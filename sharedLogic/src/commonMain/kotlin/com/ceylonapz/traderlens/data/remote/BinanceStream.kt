package com.ceylonapz.traderlens.data.remote

import com.ceylonapz.traderlens.domain.model.KlineInterval
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BinanceStream(
    private val client: HttpClient,
    private val backoff: ExponentialBackoff = ExponentialBackoff(), //reconnect kline stream
) {

    fun klines(interval: KlineInterval): Flow<KlineDto> = flow {
        client.webSocket("$BASE_URL/ws/${SYMBOL}@kline_${interval.code}") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    emit(binanceJson.decodeFromString<KlineEventDto>(frame.readText()).toKlineDto())
                }
            }
        }
        // Binance drops connections (e.g. every 24h); a clean close must reconnect too.
        error("Kline stream closed")
    }.retryWithBackoff(backoff)

    private companion object {
        const val BASE_URL = "wss://stream.binance.com:9443"
        const val SYMBOL = "btcusdt"
    }
}
