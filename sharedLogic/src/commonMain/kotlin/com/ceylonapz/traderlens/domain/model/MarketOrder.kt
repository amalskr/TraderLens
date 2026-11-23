package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents

sealed interface MarketOrder {
    data class Buy(val usd: UsdCents) : MarketOrder
    data class Sell(val btc: Satoshis) : MarketOrder
}
