package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlin.test.Test
import kotlin.test.assertEquals

class CandleMergeTest {

    private fun candle(openTime: Long, close: Long = 100) =
        Candle(openTime, PriceCents(100), PriceCents(close), PriceCents(close), PriceCents(close))

    @Test
    fun mergeIntoEmptyListAddsCandle() {
        assertEquals(listOf(candle(1)), emptyList<Candle>().merge(candle(1)))
    }

    @Test
    fun sameOpenTimeAsLastReplacesIt() {
        val candles = listOf(candle(1), candle(2))
        assertEquals(listOf(candle(1), candle(2, close = 150)), candles.merge(candle(2, close = 150)))
    }

    @Test
    fun newerOpenTimeAppends() {
        val candles = listOf(candle(1), candle(2))
        assertEquals(listOf(candle(1), candle(2), candle(3)), candles.merge(candle(3)))
    }

    @Test
    fun olderExistingOpenTimeReplacesInPlace() {
        val candles = listOf(candle(1), candle(2), candle(3))
        assertEquals(listOf(candle(1), candle(2, close = 90), candle(3)), candles.merge(candle(2, close = 90)))
    }

    @Test
    fun missingOpenTimeInsertsInOrder() {
        val candles = listOf(candle(1), candle(3))
        assertEquals(listOf(candle(1), candle(2), candle(3)), candles.merge(candle(2)))
    }

    @Test
    fun doesNotMutateOriginalList() {
        val candles = listOf(candle(1))
        candles.merge(candle(1, close = 200))
        assertEquals(listOf(candle(1)), candles)
    }
}
