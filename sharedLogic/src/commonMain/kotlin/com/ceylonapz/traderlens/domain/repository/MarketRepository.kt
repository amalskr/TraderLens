package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.KlineInterval

interface MarketRepository {
    suspend fun getCandles(interval: KlineInterval, limit: Int): Result<List<Candle>>
}
