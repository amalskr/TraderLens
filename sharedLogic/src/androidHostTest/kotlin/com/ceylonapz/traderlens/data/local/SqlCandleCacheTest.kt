package com.ceylonapz.traderlens.data.local

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlCandleCacheTest {

    private val interval = KlineInterval.ONE_MINUTE

    private fun candle(openTime: Long, close: Long) =
        Candle(openTime, PriceCents(100), PriceCents(200), PriceCents(50), PriceCents(close))

    @Test
    fun emptyCacheLoadsNull() = runTest {
        val cache = SqlCandleCache(inMemoryDatabase(), UnconfinedTestDispatcher(testScheduler))

        assertNull(cache.load(interval, limit = 10))
    }

    @Test
    fun savedCandlesLoadOldestFirstWithTimestamp() = runTest {
        val cache = SqlCandleCache(inMemoryDatabase(), UnconfinedTestDispatcher(testScheduler))

        cache.save(interval, listOf(candle(1, 110), candle(2, 120)), keepFromOpenTime = 1, updatedAtMillis = 5_000)
        cache.save(interval, listOf(candle(2, 125)), keepFromOpenTime = 1, updatedAtMillis = 7_000)

        assertEquals(CachedCandles(listOf(candle(1, 110), candle(2, 125)), 7_000), cache.load(interval, limit = 10))
    }

    @Test
    fun candlesBeforeTheWindowAreDropped() = runTest {
        val database = inMemoryDatabase()
        val cache = SqlCandleCache(database, UnconfinedTestDispatcher(testScheduler))

        cache.save(interval, listOf(candle(1, 110), candle(2, 120)), keepFromOpenTime = 1, updatedAtMillis = 5_000)
        cache.save(interval, listOf(candle(3, 130)), keepFromOpenTime = 2, updatedAtMillis = 6_000)

        assertEquals(listOf(2L, 3L), cache.load(interval, limit = 10)?.candles?.map { it.openTimeMillis })
        assertEquals(130, database.lastPriceQueries.select().executeAsOne().price_cents)
    }
}
