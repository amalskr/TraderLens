package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.fakes.FakeMarketRepository
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class RefreshStalePriceUseCaseTest {

    private val now = 10_000_000L
    private val repository = FakeMarketRepository(refreshPrice = PriceSnapshot(PriceCents(9_100_000), now))
    private val useCase = RefreshStalePriceUseCase(repository, nowMillis = { now }, maxAge = 5.minutes)

    @Test
    fun freshPriceSkipsNetwork() = runTest {
        repository.lastKnownPrice.value = PriceSnapshot(PriceCents(9_000_000), now - 5.minutes.inWholeMilliseconds)

        assertTrue(useCase())
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun stalePriceIsRefreshed() = runTest {
        repository.lastKnownPrice.value = PriceSnapshot(PriceCents(9_000_000), now - 5.minutes.inWholeMilliseconds - 1)

        assertTrue(useCase())
        assertEquals(1, repository.refreshCalls)
        assertEquals(PriceCents(9_100_000), repository.lastKnownPrice.value?.price)
    }

    @Test
    fun missingPriceIsRefreshed() = runTest {
        assertTrue(useCase())
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun failedRefreshKeepsOldPrice() = runTest {
        val old = PriceSnapshot(PriceCents(9_000_000), 0)
        repository.lastKnownPrice.value = old
        repository.refreshPrice = null

        assertFalse(useCase())
        assertEquals(old, repository.lastKnownPrice.value)
    }
}
