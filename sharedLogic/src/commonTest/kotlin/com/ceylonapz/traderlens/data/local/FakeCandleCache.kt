package com.ceylonapz.traderlens.data.local

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCandleCache(var cached: CachedCandles? = null) : CandleCache {
    data class Save(val changed: List<Candle>, val keepFromOpenTime: Long, val updatedAtMillis: Long)

    val saves = mutableListOf<Save>()
    val lastPrice = MutableStateFlow<PriceSnapshot?>(null)

    override suspend fun load(interval: KlineInterval, limit: Int): CachedCandles? = cached

    override fun observeLastPrice(): Flow<PriceSnapshot?> = lastPrice

    override suspend fun save(interval: KlineInterval, changed: List<Candle>, keepFromOpenTime: Long, updatedAtMillis: Long) {
        saves += Save(changed, keepFromOpenTime, updatedAtMillis)
        lastPrice.value = PriceSnapshot(changed.maxBy { it.openTimeMillis }.close, updatedAtMillis)
    }
}
