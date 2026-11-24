package com.ceylonapz.traderlens.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceylonapz.traderlens.domain.model.AmountUnit
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.CandleFeedEvent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.ceylonapz.traderlens.presentation.format.toPlainAmount
import com.ceylonapz.traderlens.presentation.trade.OrderPreview
import com.ceylonapz.traderlens.presentation.trade.TradePanelState
import kotlinx.coroutines.launch

class ChartViewModel(
    private val initializeWallet: InitializeWalletUseCase,
    private val observeMarket: ObserveMarketUseCase,
    private val observePortfolio: ObservePortfolioUseCase,
    private val executeTrade: ExecuteTradeUseCase,
    private val orderValidator: OrderValidator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            initializeWallet()
            observePortfolio().collect { portfolio ->
                _uiState.update { it.copy(portfolio = portfolio).withPreview() }
            }
        }
        viewModelScope.launch {
            observeMarket.connectionState.collect { connection ->
                _uiState.update { it.copy(connection = connection) }
            }
        }
        viewModelScope.launch {
            observeMarket.retryStatus.collect { retry ->
                _uiState.update { it.copy(retry = retry) }
            }
        }
        viewModelScope.launch {
            observeMarket(KlineInterval.ONE_MINUTE, CANDLE_LIMIT).collect { feed ->
                _uiState.update { it.apply(feed) }
            }
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

    private fun updatePanel(transform: (TradePanelState) -> TradePanelState) {
        _uiState.update { it.copy(tradePanel = transform(it.tradePanel)).withPreview() }
    }

    private fun ChartUiState.withPreview(): ChartUiState = copy(tradePanel = tradePanel.copy(preview = preview()))

    private fun ChartUiState.preview(): OrderPreview? {
        val panel = tradePanel
        if (panel.amount.isEmpty()) return null
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
    }
}
