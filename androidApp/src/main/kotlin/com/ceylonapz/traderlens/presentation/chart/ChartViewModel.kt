package com.ceylonapz.traderlens.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.CandleFeedEvent
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import com.ceylonapz.traderlens.domain.usecase.InitializeWalletUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChartViewModel(
    private val marketRepository: MarketRepository,
    private val walletRepository: WalletRepository,
    private val initializeWallet: InitializeWalletUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            initializeWallet()
            walletRepository.observeWallet().collect { wallet ->
                _uiState.update { it.copy(wallet = wallet) }
            }
        }
        viewModelScope.launch {
            marketRepository.connectionState.collect { connection ->
                _uiState.update { it.copy(connection = connection) }
            }
        }
        viewModelScope.launch {
            marketRepository.retryStatus.collect { retry ->
                _uiState.update { it.copy(retry = retry) }
            }
        }
        viewModelScope.launch {
            marketRepository.observeCandles(KlineInterval.ONE_MINUTE, CANDLE_LIMIT).collect { feed ->
                _uiState.update { it.apply(feed) }
            }
        }
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
