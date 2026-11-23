package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.KlineStream
import com.ceylonapz.traderlens.data.remote.KlineStreamEvent
import com.ceylonapz.traderlens.data.remote.toCandle
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.merge
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

class MarketRepositoryImpl(
    private val api: BinanceApi,
    private val stream: KlineStream,
) : MarketRepository {

    //get from BinanceApi
    override suspend fun getCandles(interval: KlineInterval, limit: Int): Result<List<Candle>> =
        try {
            val candles = api.getKlines(interval, limit).map { dto ->
                dto.toCandle() ?: error("Invalid price in kline ${dto.openTime}")
            }
            Result.success(candles)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    //get from BinanceStream
    override fun observeCandles(interval: KlineInterval, limit: Int): Flow<List<Candle>> = flow {
        var candles = emptyList<Candle>()
        stream.events(interval).collect { event ->
            candles = when (event) {
                // A failed gap fill keeps the live stream going; the next reconnect retries it.
                KlineStreamEvent.Connected -> getCandles(interval, limit).fold(candles::merge) { candles }
                is KlineStreamEvent.Update -> event.kline.toCandle()?.let(candles::merge) ?: candles
            }.takeLast(limit)
            emit(candles)
        }
    }
}
