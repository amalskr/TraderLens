package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.UsdCents

// totalValue is null until a live price arrives.
data class Portfolio(val wallet: Wallet, val price: PriceCents?, val totalValue: UsdCents?)
