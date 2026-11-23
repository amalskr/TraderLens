package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents

data class Candle(
    val openTimeMillis: Long,
    val open: PriceCents,
    val high: PriceCents,
    val low: PriceCents,
    val close: PriceCents,
)
