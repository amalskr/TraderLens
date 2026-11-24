package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.CandleFeedEvent
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
import com.ceylonapz.traderlens.domain.model.RetryStatus
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ObserveMarketUseCaseTest {

    private val feed = CandleFeed(emptyList(), CandleFeedEvent.GapFill(added = 0), updatedAtMillis = null)

    private val repository = object : MarketRepository {
        var requested: Pair<KlineInterval, Int>? = null
        override val connectionState = MutableStateFlow(ConnectionState.Connected)
        override val latestPrice = MutableStateFlow<LivePrice?>(null)
        override val retryStatus = MutableStateFlow<RetryStatus?>(null)
        override suspend fun getCandles(interval: KlineInterval, limit: Int) = Result.success(emptyList<Candle>())
        override fun observeCandles(interval: KlineInterval, limit: Int): Flow<CandleFeed> {
            requested = interval to limit
            return flowOf(feed)
        }
        override fun observeLastKnownPrice() = flowOf<PriceSnapshot?>(null)
        override suspend fun refreshCandles(interval: KlineInterval, limit: Int) = true
    }

    @Test
    fun forwardsIntervalAndLimitToRepository() = runTest {
        val emissions = ObserveMarketUseCase(repository)(KlineInterval.ONE_MINUTE, 100).toList()

        assertEquals(KlineInterval.ONE_MINUTE to 100, repository.requested)
        assertEquals(listOf(feed), emissions)
    }

    @Test
    fun exposesRepositoryConnectionAndRetryState() {
        val useCase = ObserveMarketUseCase(repository)

        assertSame(repository.connectionState, useCase.connectionState)
        assertSame(repository.retryStatus, useCase.retryStatus)
    }
}
