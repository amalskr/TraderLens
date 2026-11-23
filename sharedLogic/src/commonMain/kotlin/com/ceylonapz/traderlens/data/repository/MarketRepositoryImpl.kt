package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.BinanceStream
import com.ceylonapz.traderlens.data.remote.toCandle
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.coroutines.cancellation.CancellationException

//for binance websocket
class MarketRepositoryImpl(
    private val api: BinanceApi,
    private val stream: BinanceStream,
) : MarketRepository {

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

    override fun observeCandles(interval: KlineInterval): Flow<Candle> =
        stream.klines(interval).mapNotNull { it.toCandle() }
}
