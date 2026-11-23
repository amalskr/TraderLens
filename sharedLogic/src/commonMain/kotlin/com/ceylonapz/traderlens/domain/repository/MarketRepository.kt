package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import kotlinx.coroutines.flow.Flow

//for binance websocket
interface MarketRepository {
    suspend fun getCandles(interval: KlineInterval, limit: Int): Result<List<Candle>>

    fun observeCandles(interval: KlineInterval): Flow<Candle>
}
