package com.ceylonapz.traderlens.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class RetryBackoff(
    private val baseMillis: Long = 1_000,
    private val maxMillis: Long = 30_000,
    private val random: Random = Random.Default,
) {

    // Equal jitter: spreads clients out but never retries instantly.
    fun delayMillis(attempt: Int): Long {
        val cap = (baseMillis shl attempt.coerceIn(0, MAX_SHIFT)).coerceAtMost(maxMillis)
        return cap / 2 + random.nextLong(cap / 2 + 1)
    }

    private companion object {
        const val MAX_SHIFT = 20
    }
}

// Retries forever; the attempt count resets once a value arrives, so a healthy reconnect starts fast again.
fun <T> Flow<T>.retryWithBackoff(
    backoff: RetryBackoff,
    onRetry: suspend FlowCollector<T>.(failures: Int, delayMillis: Long) -> Unit = { _, _ -> },
): Flow<T> = flow {
    var attempt = 0
    emitAll(
        this@retryWithBackoff
            .onEach { attempt = 0 }
            .retryWhen { _, _ ->
                val delayMillis = backoff.delayMillis(attempt++)
                onRetry(attempt, delayMillis)
                delay(delayMillis.milliseconds)
                true
            }
    )
}
