package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents

data class Trade(
    val id: Long,
    val side: TradeSide,
    val price: PriceCents,
    val usd: UsdCents,
    val btc: Satoshis,
    val timestampMillis: Long,
)
