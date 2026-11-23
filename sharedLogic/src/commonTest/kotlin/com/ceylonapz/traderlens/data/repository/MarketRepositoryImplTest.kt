package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.KlineDto
import com.ceylonapz.traderlens.data.remote.KlineStream
import com.ceylonapz.traderlens.data.remote.KlineStreamEvent
import com.ceylonapz.traderlens.data.remote.createHttpClient
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.CandleFeedEvent
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.RetryStatus
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

class MarketRepositoryImplTest {

    private fun repository(
        vararg events: KlineStreamEvent,
        handler: MockRequestHandler,
        timeSource: TimeSource = TimeSource.Monotonic,
    ): MarketRepositoryImpl {
        val client = createHttpClient(enableLogging = false, engine = MockEngine(handler))
        val stream = object : KlineStream {
            override fun events(interval: KlineInterval): Flow<KlineStreamEvent> = flowOf(*events)
        }
        return MarketRepositoryImpl(BinanceApi(client), stream, timeSource)
    }

    private fun restKline(openTime: Long, close: String) = """[$openTime,"1","1","1","$close"]"""

    private fun update(openTime: Long, close: String) =
        KlineStreamEvent.Update(KlineDto(openTime, "1", "1", "1", close))

    private fun candle(openTime: Long, closeCents: Long) =
        Candle(openTime, PriceCents(100), PriceCents(100), PriceCents(100), PriceCents(closeCents))

    private fun json(body: String): MockRequestHandler = {
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun returnsCandlesOnSuccess() = runTest {
        val repo = repository(handler = json("""[[1795428840000,"61342.59","61399.17","61342.59","61390.00","5.0",1795428899999,"0",1,"0","0","0"]]"""))

        val result = repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1)

        val expected = Candle(
            openTimeMillis = 1795428840000,
            open = PriceCents(6_134_259),
            high = PriceCents(6_139_917),
            low = PriceCents(6_134_259),
            close = PriceCents(6_139_000),
        )
        assertEquals(listOf(expected), result.getOrThrow())
    }

    @Test
    fun returnsFailureOnServerError() = runTest {
        val repo = repository(handler = { respondError(HttpStatusCode.InternalServerError) })

        assertTrue(repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1).isFailure)
    }

    @Test
    fun returnsFailureOnInvalidPrice() = runTest {
        val repo = repository(handler = json("""[[1795428840000,"abc","1","1","1"]]"""))

        assertTrue(repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1).isFailure)
    }

    @Test
    fun rethrowsCancellation() = runTest {
        val repo = repository(handler = { throw CancellationException("cancelled") })

        assertFailsWith<CancellationException> { repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1) }
    }

    @Test
    fun connectFillsFromRestThenAppliesLiveUpdates() = runTest {
        val repo = repository(
            KlineStreamEvent.Connected,
            update(2, "3.00"),
            update(3, "4.00"),
            handler = json("[${restKline(1, "1.00")},${restKline(2, "2.00")}]"),
        )

        val emissions = repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList()

        assertEquals(
            listOf(
                listOf(candle(1, 100), candle(2, 200)),
                listOf(candle(1, 100), candle(2, 300)),
                listOf(candle(1, 100), candle(2, 300), candle(3, 400)),
            ),
            emissions.map { it.candles },
        )
        assertEquals(
            listOf(
                CandleFeedEvent.GapFill(added = 2),
                CandleFeedEvent.Live(candle(2, 300)),
                CandleFeedEvent.Live(candle(3, 400)),
            ),
            emissions.map { it.event },
        )
    }

    @Test
    fun reconnectFillsMissedCandles() = runTest {
        var restCalls = 0
        val repo = repository(
            KlineStreamEvent.Connected,
            update(1, "1.00"),
            KlineStreamEvent.Connected,
            handler = {
                restCalls++
                val body = if (restCalls == 1) "[${restKline(1, "1.00")}]"
                else "[${restKline(2, "2.00")},${restKline(3, "3.00")}]"
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        )

        val last = repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList().last()

        assertEquals(listOf(candle(1, 100), candle(2, 200), candle(3, 300)), last.candles)
        assertEquals(CandleFeedEvent.GapFill(added = 2), last.event)
    }

    @Test
    fun failedGapFillKeepsLiveUpdates() = runTest {
        val repo = repository(
            KlineStreamEvent.Connected,
            update(1, "1.00"),
            handler = { respondError(HttpStatusCode.InternalServerError) },
        )

        val emissions = repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList()

        assertEquals(listOf(emptyList(), listOf(candle(1, 100))), emissions.map { it.candles })
        assertEquals(CandleFeedEvent.GapFillFailed, emissions.first().event)
    }

    @Test
    fun keepsOnlyLatestLimitCandles() = runTest {
        val repo = repository(
            KlineStreamEvent.Connected,
            update(3, "3.00"),
            handler = json("[${restKline(1, "1.00")},${restKline(2, "2.00")}]"),
        )

        val last = repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 2).toList().last()

        assertEquals(listOf(candle(2, 200), candle(3, 300)), last.candles)
    }

    @Test
    fun connectionStateFollowsStreamEvents() = runTest {
        val states = mutableListOf<ConnectionState>()
        val retries = mutableListOf<RetryStatus?>()
        lateinit var repo: MarketRepositoryImpl
        val stream = object : KlineStream {
            override fun events(interval: KlineInterval): Flow<KlineStreamEvent> = flow {
                states += repo.connectionState.value
                listOf(
                    KlineStreamEvent.Connected,
                    KlineStreamEvent.Disconnected(failures = 1, retryInMillis = 700),
                    KlineStreamEvent.Disconnected(failures = 2, retryInMillis = 1_500),
                    KlineStreamEvent.Disconnected(failures = 3, retryInMillis = 3_000),
                    KlineStreamEvent.Connected,
                ).forEach {
                    emit(it)
                    states += repo.connectionState.value
                    retries += repo.retryStatus.value
                }
            }
        }
        val client = createHttpClient(enableLogging = false, engine = MockEngine(json("[]")))
        repo = MarketRepositoryImpl(BinanceApi(client), stream)

        assertEquals(ConnectionState.Offline, repo.connectionState.value)
        repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList()

        assertEquals(
            listOf(
                ConnectionState.Reconnecting,
                ConnectionState.Connected,
                ConnectionState.Reconnecting,
                ConnectionState.Reconnecting,
                ConnectionState.Offline,
                ConnectionState.Connected,
            ),
            states,
        )
        assertEquals(
            listOf(null, RetryStatus(1, 700), RetryStatus(2, 1_500), RetryStatus(3, 3_000), null),
            retries,
        )
        assertEquals(ConnectionState.Offline, repo.connectionState.value)
        assertEquals(null, repo.retryStatus.value)
    }

    @Test
    fun restGapFillDoesNotSetLivePrice() = runTest {
        val repo = repository(KlineStreamEvent.Connected, handler = json("[${restKline(1, "1.00")}]"))

        repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList()

        assertNull(repo.latestPrice.value)
    }

    @Test
    fun liveUpdateSetsLatestPriceWithReceiveTime() = runTest {
        val timeSource = TestTimeSource()
        val repo = repository(
            KlineStreamEvent.Connected,
            update(1, "1.00"),
            update(1, "1.50"),
            handler = json("[]"),
            timeSource = timeSource,
        )

        repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList()
        timeSource += 3.seconds

        val latest = repo.latestPrice.value
        assertEquals(PriceCents(150), latest?.price)
        assertEquals(3.seconds, latest?.receivedAt?.elapsedNow())
    }

    @Test
    fun reconnectClearsPriceFromPreviousConnection() = runTest {
        val repo = repository(
            KlineStreamEvent.Connected,
            update(1, "1.00"),
            KlineStreamEvent.Disconnected(failures = 1, retryInMillis = 500),
            KlineStreamEvent.Connected,
            handler = json("[]"),
        )

        repo.observeCandles(KlineInterval.ONE_MINUTE, limit = 10).toList()

        assertNull(repo.latestPrice.value)
    }
}
