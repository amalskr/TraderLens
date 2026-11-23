package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.RetryStatus
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Connection and retry stay separate StateFlows: combining them with the feed would replay feed events.
class ObserveMarketUseCase(private val marketRepository: MarketRepository) {

    val connectionState: StateFlow<ConnectionState> get() = marketRepository.connectionState

    val retryStatus: StateFlow<RetryStatus?> get() = marketRepository.retryStatus

    operator fun invoke(interval: KlineInterval, limit: Int): Flow<CandleFeed> =
        marketRepository.observeCandles(interval, limit)
}
