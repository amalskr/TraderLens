package com.ceylonapz.traderlens.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class CandleQueriesTest {

    private val queries = inMemoryDatabase().candleQueries

    private fun upsert(openTime: Long, close: Long = 100, interval: String = "1m") =
        queries.upsert(interval, openTime, open_cents = 100, high_cents = 100, low_cents = 100, close_cents = close)

    private fun openTimes(limit: Long = 10, interval: String = "1m") =
        queries.selectLatest(interval, limit).executeAsList().map { it.open_time_millis }

    @Test
    fun upsertWithSameKeyReplaces() {
        upsert(openTime = 1, close = 100)
        upsert(openTime = 1, close = 150)

        val candles = queries.selectLatest("1m", 10).executeAsList()

        assertEquals(1, candles.size)
        assertEquals(150, candles.single().close_cents)
    }

    @Test
    fun sameOpenTimeInAnotherIntervalIsSeparate() {
        upsert(openTime = 1, interval = "1m")
        upsert(openTime = 1, interval = "5m")

        assertEquals(listOf(1L), openTimes(interval = "1m"))
        assertEquals(listOf(1L), openTimes(interval = "5m"))
    }

    @Test
    fun selectLatestReturnsNewestNOldestFirst() {
        listOf(3L, 1L, 5L, 2L, 4L).forEach { upsert(openTime = it) }

        assertEquals(listOf(3L, 4L, 5L), openTimes(limit = 3))
    }

    @Test
    fun deleteOlderThanRemovesOnlyOlderRows() {
        (1L..5L).forEach { upsert(openTime = it) }
        upsert(openTime = 1, interval = "5m")

        queries.deleteOlderThan("1m", 3)

        assertEquals(listOf(3L, 4L, 5L), openTimes())
        assertEquals(listOf(1L), openTimes(interval = "5m"))
    }
}
