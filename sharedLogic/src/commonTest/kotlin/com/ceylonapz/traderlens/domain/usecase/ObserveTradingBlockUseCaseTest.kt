package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.fakes.FakePriceFeed
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.trading.StalePriceGuard
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class ObserveTradingBlockUseCaseTest {

    private val timeSource = TestTimeSource()
    private val feed = FakePriceFeed()
    private val useCase = ObserveTradingBlockUseCase(feed, StalePriceGuard(maxAge = 10.seconds), 1.seconds)

    private fun freshPrice() = LivePrice(PriceCents(9_000_000), timeSource.markNow())

    @Test
    fun blockedUntilFirstLivePrice() = runTest {
        val emissions = collect()

        feed.latestPrice.value = freshPrice()

        assertEquals(listOf(TradeRejection.STALE_PRICE, null), emissions)
    }

    @Test
    fun disconnectBlocksAndReconnectWithFreshPriceUnblocks() = runTest {
        feed.latestPrice.value = freshPrice()
        val emissions = collect()

        feed.connectionState.value = ConnectionState.Reconnecting
        feed.connectionState.value = ConnectionState.Offline
        feed.latestPrice.value = freshPrice()
        feed.connectionState.value = ConnectionState.Connected

        assertEquals(listOf(null, TradeRejection.DISCONNECTED, null), emissions)
    }

    @Test
    fun priceGoesStaleWithoutAnyNewEmission() = runTest {
        feed.latestPrice.value = freshPrice()
        val emissions = collect()

        timeSource += 11.seconds
        advanceTimeBy(1.seconds + 1.seconds / 10)

        assertEquals(listOf(null, TradeRejection.STALE_PRICE), emissions)
    }

    private fun TestScope.collect(): MutableList<TradeRejection?> {
        val emissions = mutableListOf<TradeRejection?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(emissions) }
        return emissions
    }
}
