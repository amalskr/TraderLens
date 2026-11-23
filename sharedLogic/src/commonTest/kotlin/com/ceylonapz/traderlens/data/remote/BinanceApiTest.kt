package com.ceylonapz.traderlens.data.remote

import com.ceylonapz.traderlens.domain.model.KlineInterval
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BinanceApiTest {

    private val body = """
        [[1795428840000,"61342.59000000","61399.17000000","61342.59000000","61390.00000000","5.05580000",1795428899999,"310311.23481230",3947,"2.56609000","157487.73612850","0"],
         [1795428900000,"61390.00000000","61402.01000000","61378.00000000","61398.00000000","2.01567000",1795428959999,"123739.34094740",1663,"1.39199000","85449.36284610","0"]]
    """.trimIndent()

    private fun apiReturning(handler: (HttpRequestData) -> Unit = {}): BinanceApi {
        val engine = MockEngine { request ->
            handler(request)
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return BinanceApi(createHttpClient(enableLogging = false, engine = engine))
    }

    @Test
    fun requestsBtcUsdtKlinesWithIntervalAndLimit() = runTest {
        lateinit var request: HttpRequestData
        apiReturning { request = it }.getKlines(KlineInterval.ONE_MINUTE, limit = 2)

        assertEquals("api.binance.com", request.url.host)
        assertEquals("/api/v3/klines", request.url.encodedPath)
        assertEquals("BTCUSDT", request.url.parameters["symbol"])
        assertEquals("1m", request.url.parameters["interval"])
        assertEquals("2", request.url.parameters["limit"])
    }

    @Test
    fun parsesResponseOldestFirst() = runTest {
        val klines = apiReturning().getKlines(KlineInterval.ONE_MINUTE, limit = 2)

        assertEquals(listOf(1795428840000, 1795428900000), klines.map { it.openTime })
        assertEquals("61398.00000000", klines[1].close)
    }

    @Test
    fun serverErrorThrows() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = BinanceApi(createHttpClient(enableLogging = false, engine = engine))

        assertFailsWith<ServerResponseException> { api.getKlines(KlineInterval.ONE_MINUTE, limit = 2) }
    }
}
