package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class RefreshStalePriceUseCase(
    private val marketRepository: MarketRepository,
    private val nowMillis: () -> Long,
    private val maxAge: Duration = 5.minutes,
) {

    /** Refetches candles over REST when the cached price is missing or older than [maxAge]; false if that fetch fails. */
    suspend operator fun invoke(): Boolean {
        val last = marketRepository.observeLastKnownPrice().first()
        if (last != null && nowMillis() - last.updatedAtMillis <= maxAge.inWholeMilliseconds) return true
        return marketRepository.refreshCandles(KlineInterval.ONE_MINUTE, CANDLE_LIMIT)
    }

    private companion object {
        const val CANDLE_LIMIT = 100
    }
}
