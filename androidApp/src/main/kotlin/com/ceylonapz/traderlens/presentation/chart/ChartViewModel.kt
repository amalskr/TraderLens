package com.ceylonapz.traderlens.presentation.chart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceylonapz.traderlens.domain.model.AmountUnit
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.CandleFeedEvent
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.trading.OrderValidation
import com.ceylonapz.traderlens.domain.trading.OrderValidator
import com.ceylonapz.traderlens.domain.trading.parseMarketOrder
import com.ceylonapz.traderlens.domain.usecase.ExecuteTradeUseCase
import com.ceylonapz.traderlens.domain.usecase.InitializeWalletUseCase
import com.ceylonapz.traderlens.domain.usecase.ObserveMarketUseCase
import com.ceylonapz.traderlens.domain.usecase.ObservePortfolioUseCase
import com.ceylonapz.traderlens.domain.usecase.ObserveTradingBlockUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.ceylonapz.traderlens.presentation.format.toPlainAmount
import com.ceylonapz.traderlens.presentation.trade.OrderPreview
import com.ceylonapz.traderlens.presentation.trade.TradePanelState
import kotlinx.coroutines.launch

class ChartViewModel(
    private val initializeWallet: InitializeWalletUseCase,
    private val observeMarket: ObserveMarketUseCase,
    private val observePortfolio: ObservePortfolioUseCase,
    private val observeTradingBlock: ObserveTradingBlockUseCase,
    private val executeTrade: ExecuteTradeUseCase,
    private val orderValidator: OrderValidator,
    private val savedState: SavedStateHandle, //  process death
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChartUiState(
            tradePanel = restoredPanel(),
            interval = savedState.get<String>(KEY_INTERVAL)?.let(KlineInterval::valueOf) ?: KlineInterval.ONE_MINUTE,
        ),
    )

    // Stop live market when BG and reconnect in foreground
    val uiState: StateFlow<ChartUiState> = flow {
        coroutineScope {
            launch { observeLiveMarket() }
            emitAll(_uiState)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), _uiState.value)

    init {
        viewModelScope.launch {
            initializeWallet()
            observePortfolio().collect { portfolio ->
                _uiState.update { it.copy(portfolio = portfolio).withPreview() }
            }
        }
    }

    fun onIntervalChange(interval: KlineInterval) {
        if (interval == _uiState.value.interval) return
        savedState[KEY_INTERVAL] = interval.name
        _uiState.update {
            it.copy(
                interval = interval,
                candles = emptyList(),
                updatedAtMillis = null,
                lastUpdatedOpenTime = null,
                lastAppendedOpenTime = null,
                reconnects = 0,
                lastGapFill = null,
            )
        }
    }

    fun onSideChange(side: TradeSide) = updatePanel { it.copy(side = side, amount = "") }

    fun onUnitChange(unit: AmountUnit) = updatePanel { it.copy(unit = unit, amount = "") }

    fun onAmountChange(text: String) {
        val decimals = if (_uiState.value.tradePanel.unit == AmountUnit.USD) 2 else 8
        if (!Regex("""\d*(\.\d{0,$decimals})?""").matches(text)) return
        updatePanel { it.copy(amount = text) }
    }

    // Max uses the side's own unit so the whole balance is used without a lossy conversion.
    fun onMax() {
        val wallet = _uiState.value.portfolio?.wallet ?: return
        updatePanel {
            when (it.side) {
                TradeSide.BUY -> it.copy(unit = AmountUnit.USD, amount = wallet.usd.toPlainAmount())
                TradeSide.SELL -> it.copy(unit = AmountUnit.BTC, amount = wallet.btc.toPlainAmount())
            }
        }
    }

    fun onReview() {
        if (_uiState.value.tradePanel.preview is OrderPreview.Ready) updatePanel { it.copy(confirming = true) }
    }

    fun onDismissConfirm() = updatePanel { it.copy(confirming = false) }

    fun onConfirm() {
        val panel = _uiState.value.tradePanel
        val preview = panel.preview as? OrderPreview.Ready ?: return
        if (panel.submitting) return
        updatePanel { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = executeTrade(preview.order)
            updatePanel {
                val cleared = if (result is TradeResult.Success) it.copy(amount = "") else it
                cleared.copy(confirming = false, submitting = false, lastResult = result)
            }
        }
    }

    private suspend fun observeLiveMarket() {
        try {
            coroutineScope {
                launch {
                    observeMarket.connectionState.collect { connection ->
                        _uiState.update { it.copy(connection = connection) }
                    }
                }
                launch {
                    observeMarket.retryStatus.collect { retry ->
                        _uiState.update { it.copy(retry = retry) }
                    }
                }
                launch {
                    observeTradingBlock().collect { block ->
                        updatePanel { it.copy(blockedBy = block, confirming = it.confirming && block == null) }
                    }
                }
                _uiState.map { it.interval }.distinctUntilChanged().collectLatest { interval ->
                    observeMarket(interval, CANDLE_LIMIT).collect { feed ->
                        // A feed for the previous interval can still land before collectLatest cancels it.
                        _uiState.update {
                            if (it.interval != interval) it else it.apply(feed).copy(updatedAtMillis = feed.updatedAtMillis)
                        }
                    }
                }
            }
        } finally {
            _uiState.update { it.copy(connection = ConnectionState.Offline, retry = null) }
            updatePanel { it.copy(blockedBy = TradeRejection.DISCONNECTED, confirming = false) }
        }
    }

    private fun updatePanel(transform: (TradePanelState) -> TradePanelState) {
        _uiState.update { it.copy(tradePanel = transform(it.tradePanel)).withPreview() }
        val panel = _uiState.value.tradePanel
        savedState[KEY_SIDE] = panel.side.name
        savedState[KEY_UNIT] = panel.unit.name
        savedState[KEY_AMOUNT] = panel.amount
    }

    // Only the form survives process death; a confirmation is never restored because the price has moved since.
    private fun restoredPanel() = TradePanelState(
        side = savedState.get<String>(KEY_SIDE)?.let(TradeSide::valueOf) ?: TradeSide.BUY,
        unit = savedState.get<String>(KEY_UNIT)?.let(AmountUnit::valueOf) ?: AmountUnit.USD,
        amount = savedState[KEY_AMOUNT] ?: "",
    )

    private fun ChartUiState.withPreview(): ChartUiState = copy(tradePanel = tradePanel.copy(preview = preview()))

    private fun ChartUiState.preview(): OrderPreview? {
        val panel = tradePanel
        if (panel.amount.isEmpty()) return null
        panel.blockedBy?.let { return OrderPreview.Blocked(it) }
        val price = portfolio?.price ?: return OrderPreview.Blocked(TradeRejection.STALE_PRICE)
        val order = parseMarketOrder(panel.side, panel.unit, panel.amount, price)
            ?: return OrderPreview.Blocked(TradeRejection.INVALID_AMOUNT)
        val valid = when (val validation = orderValidator.validate(order, price)) {
            is OrderValidation.Invalid -> return OrderPreview.Blocked(validation.reason)
            is OrderValidation.Valid -> validation
        }
        if (portfolio.wallet.afterTrade(valid.side, valid.usd, valid.btc) == null) {
            return OrderPreview.Blocked(TradeRejection.INSUFFICIENT_FUNDS)
        }
        return OrderPreview.Ready(order, valid.usd, valid.btc, price)
    }

    private fun ChartUiState.apply(feed: CandleFeed): ChartUiState = when (val event = feed.event) {
        CandleFeedEvent.Cached -> copy(candles = feed.candles)
        is CandleFeedEvent.Live -> {
            val openTime = event.candle.openTimeMillis
            copy(
                candles = feed.candles,
                lastUpdatedOpenTime = openTime,
                lastAppendedOpenTime = if (candles.none { it.openTimeMillis == openTime }) openTime else lastAppendedOpenTime,
            )
        }
        // Every connect starts with a gap fill, so each one after the first is a reconnect.
        is CandleFeedEvent.GapFill -> copy(
            candles = feed.candles,
            lastGapFill = GapFillStatus.Filled(event.added),
            reconnects = if (lastGapFill == null) reconnects else reconnects + 1,
        )
        CandleFeedEvent.GapFillFailed -> copy(
            candles = feed.candles,
            lastGapFill = GapFillStatus.Failed,
            reconnects = if (lastGapFill == null) reconnects else reconnects + 1,
        )
    }

    private companion object {
        const val CANDLE_LIMIT = 100
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val KEY_SIDE = "side"
        const val KEY_UNIT = "unit"
        const val KEY_AMOUNT = "amount"
        const val KEY_INTERVAL = "interval"
    }
}
