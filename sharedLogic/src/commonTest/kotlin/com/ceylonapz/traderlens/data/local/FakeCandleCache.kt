package com.ceylonapz.traderlens.data.local

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval

class FakeCandleCache(var cached: CachedCandles? = null) : CandleCache {
    data class Save(val changed: List<Candle>, val keepFromOpenTime: Long, val updatedAtMillis: Long)

    val saves = mutableListOf<Save>()

    override suspend fun load(interval: KlineInterval, limit: Int): CachedCandles? = cached

    override suspend fun save(interval: KlineInterval, changed: List<Candle>, keepFromOpenTime: Long, updatedAtMillis: Long) {
        saves += Save(changed, keepFromOpenTime, updatedAtMillis)
    }
}
