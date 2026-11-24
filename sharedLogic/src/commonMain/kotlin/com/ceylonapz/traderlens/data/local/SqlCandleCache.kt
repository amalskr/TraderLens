package com.ceylonapz.traderlens.data.local

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SqlCandleCache(
    private val database: TraderLensDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CandleCache {

    override suspend fun load(interval: KlineInterval, limit: Int): CachedCandles? = withContext(dispatcher) {
        val candles = database.candleQueries.selectLatest(interval.code, limit.toLong()).executeAsList().map {
            Candle(it.open_time_millis, PriceCents(it.open_cents), PriceCents(it.high_cents), PriceCents(it.low_cents), PriceCents(it.close_cents))
        }
        val lastPrice = database.lastPriceQueries.select().executeAsOneOrNull()
        if (candles.isEmpty() || lastPrice == null) null else CachedCandles(candles, lastPrice.timestamp_millis)
    }

    override suspend fun save(
        interval: KlineInterval,
        changed: List<Candle>,
        keepFromOpenTime: Long,
        updatedAtMillis: Long,
    ) = withContext(dispatcher) {
        val newest = changed.maxByOrNull { it.openTimeMillis } ?: return@withContext
        database.transaction {
            changed.forEach {
                database.candleQueries.upsert(interval.code, it.openTimeMillis, it.open.value, it.high.value, it.low.value, it.close.value)
            }
            database.candleQueries.deleteOlderThan(interval.code, keepFromOpenTime)
            database.lastPriceQueries.upsert(newest.close.value, updatedAtMillis)
        }
    }
}
