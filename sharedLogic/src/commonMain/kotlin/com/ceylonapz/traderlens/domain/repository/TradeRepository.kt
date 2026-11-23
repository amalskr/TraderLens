package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlinx.coroutines.flow.Flow

interface TradeRepository {
    /** Newest first. */
    fun observeTrades(): Flow<List<Trade>>

    /** Updates the wallet and records the trade in one transaction; null if the wallet can't cover it. */
    suspend fun execute(
        side: TradeSide,
        price: PriceCents,
        usd: UsdCents,
        btc: Satoshis,
        timestampMillis: Long,
    ): Trade?
}
