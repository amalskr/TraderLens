package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
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

    /** Last price saved to the local cache by the live stream or a REST refresh. */
    fun observeLastKnownPrice(): Flow<PriceSnapshot?>

    /** Fetches the latest [limit] candles over REST into the local cache; false if the request fails. */
    suspend fun refreshCandles(interval: KlineInterval, limit: Int): Boolean
}
