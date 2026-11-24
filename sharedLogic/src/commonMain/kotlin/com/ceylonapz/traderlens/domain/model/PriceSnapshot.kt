package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents

data class PriceSnapshot(val price: PriceCents, val updatedAtMillis: Long)
