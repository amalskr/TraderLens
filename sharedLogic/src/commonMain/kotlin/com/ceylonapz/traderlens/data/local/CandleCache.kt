package com.ceylonapz.traderlens.data.local

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
import kotlinx.coroutines.flow.Flow

data class CachedCandles(val candles: List<Candle>, val updatedAtMillis: Long)

interface CandleCache {
    suspend fun load(interval: KlineInterval, limit: Int): CachedCandles?

    fun observeLastPrice(): Flow<PriceSnapshot?>

    /** Upserts [changed], drops candles older than [keepFromOpenTime] and records the newest close as the last price. */
    suspend fun save(interval: KlineInterval, changed: List<Candle>, keepFromOpenTime: Long, updatedAtMillis: Long)
}
