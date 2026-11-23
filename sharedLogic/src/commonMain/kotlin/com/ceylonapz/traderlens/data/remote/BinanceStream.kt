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
    private val backoff: RetryBackoff = RetryBackoff(), //reconnect kline stream
) : KlineStream {

    override fun events(interval: KlineInterval): Flow<KlineStreamEvent> = flow {
        client.webSocket("$BASE_URL/ws/${SYMBOL}@kline_${interval.code}") {
            // Frames buffer in `incoming` while the collector handles Connected, so a REST gap fill misses nothing.
            emit(KlineStreamEvent.Connected)
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val kline = binanceJson.decodeFromString<KlineEventDto>(frame.readText()).toKlineDto()
                    emit(KlineStreamEvent.Update(kline))
                }
            }
        }
        // Binance drops connections (e.g. every 24h); a clean close must reconnect too.
        error("Kline stream closed")
    }.retryWithBackoff(backoff) { failures, delayMillis ->
        emit(KlineStreamEvent.Disconnected(failures, delayMillis))
    }

    private companion object {
        const val BASE_URL = "wss://stream.binance.com:9443"
        const val SYMBOL = "btcusdt"
    }
}
