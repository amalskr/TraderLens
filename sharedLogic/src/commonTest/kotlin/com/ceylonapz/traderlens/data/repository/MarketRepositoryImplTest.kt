package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.BinanceStream
import com.ceylonapz.traderlens.data.remote.createHttpClient
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MarketRepositoryImplTest {

    private fun repository(handler: MockRequestHandler): MarketRepositoryImpl {
        val client = createHttpClient(enableLogging = false, engine = MockEngine(handler))
        return MarketRepositoryImpl(BinanceApi(client), BinanceStream(client))
    }

    private fun json(body: String): MockRequestHandler = {
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun returnsCandlesOnSuccess() = runTest {
        val repo = repository(json("""[[1795428840000,"61342.59","61399.17","61342.59","61390.00","5.0",1795428899999,"0",1,"0","0","0"]]"""))

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
        val repo = repository { respondError(HttpStatusCode.InternalServerError) }

        assertTrue(repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1).isFailure)
    }

    @Test
    fun returnsFailureOnInvalidPrice() = runTest {
        val repo = repository(json("""[[1795428840000,"abc","1","1","1"]]"""))

        assertTrue(repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1).isFailure)
    }

    @Test
    fun rethrowsCancellation() = runTest {
        val repo = repository { throw CancellationException("cancelled") }

        assertFailsWith<CancellationException> { repo.getCandles(KlineInterval.ONE_MINUTE, limit = 1) }
    }
}
