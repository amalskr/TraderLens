package com.ceylonapz.traderlens.data.remote

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExponentialBackoffTest {

    private val noJitter = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    @Test
    fun delayDoublesPerAttempt() {
        val backoff = ExponentialBackoff(baseMillis = 1_000, maxMillis = 30_000, random = noJitter)
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L), (0..3).map(backoff::delayMillis))
    }

    @Test
    fun delayIsCappedAtMax() {
        val backoff = ExponentialBackoff(baseMillis = 1_000, maxMillis = 30_000, random = noJitter)
        assertEquals(15_000L, backoff.delayMillis(10))
        assertEquals(15_000L, backoff.delayMillis(Int.MAX_VALUE))
    }

    @Test
    fun jitteredDelayStaysWithinHalfCapAndCap() {
        val backoff = ExponentialBackoff(baseMillis = 1_000, maxMillis = 30_000, random = Random(42))
        repeat(1_000) {
            val attempt = it % 8
            val cap = minOf(1_000L shl attempt, 30_000L)
            val delay = backoff.delayMillis(attempt)
            assertTrue(delay in cap / 2..cap, "attempt $attempt delay $delay")
        }
    }

    @Test
    fun retriesFailuresWithGrowingDelay() = runTest {
        var calls = 0
        val source = flow {
            calls++
            if (calls < 3) error("down")
            emit(calls)
        }

        val values = source.retryWithBackoff(ExponentialBackoff(random = noJitter)).toList()

        assertEquals(listOf(3), values)
        assertEquals(500L + 1_000L, currentTime)
    }

    @Test
    fun attemptResetsAfterValueArrives() = runTest {
        var calls = 0
        val source = flow {
            calls++
            emit(calls)
            if (calls < 3) error("dropped")
        }

        val values = source.retryWithBackoff(ExponentialBackoff(random = noJitter)).toList()

        assertEquals(listOf(1, 2, 3), values)
        assertEquals(500L + 500L, currentTime)
    }
}
