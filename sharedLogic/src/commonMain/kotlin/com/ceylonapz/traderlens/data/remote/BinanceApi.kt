package com.ceylonapz.traderlens.data.remote

import com.ceylonapz.traderlens.domain.model.KlineInterval
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonArray

class BinanceApi(private val client: HttpClient) {

    suspend fun getKlines(interval: KlineInterval, limit: Int): List<KlineDto> {
        val rows: List<JsonArray> = client.get("$BASE_URL/api/v3/klines") {
            parameter("symbol", SYMBOL)
            parameter("interval", interval.code)
            parameter("limit", limit)
        }.body()
        return rows.map { it.toKlineDto() }
    }

    private companion object {
        const val BASE_URL = "https://api.binance.com"
        const val SYMBOL = "BTCUSDT"
    }
}
