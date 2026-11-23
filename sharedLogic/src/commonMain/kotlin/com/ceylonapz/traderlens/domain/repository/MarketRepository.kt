package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import kotlinx.coroutines.flow.Flow

//for binance websocket
interface MarketRepository {
    suspend fun getCandles(interval: KlineInterval, limit: Int): Result<List<Candle>>

    /** Latest [limit] candles, refetched over REST on every (re)connect to fill gaps, then updated live. */
    fun observeCandles(interval: KlineInterval, limit: Int): Flow<List<Candle>>
}
