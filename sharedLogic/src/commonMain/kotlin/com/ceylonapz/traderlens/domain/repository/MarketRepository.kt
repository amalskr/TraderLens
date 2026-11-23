package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.RetryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

//for binance websocket
interface MarketRepository : PriceFeed {
    /** The pending reconnect attempt, or null while connected or idle. */
    val retryStatus: StateFlow<RetryStatus?>

    suspend fun getCandles(interval: KlineInterval, limit: Int): Result<List<Candle>>

    /** Latest [limit] candles, refetched over REST on every (re)connect to fill gaps, then updated live. */
    fun observeCandles(interval: KlineInterval, limit: Int): Flow<CandleFeed>
}
