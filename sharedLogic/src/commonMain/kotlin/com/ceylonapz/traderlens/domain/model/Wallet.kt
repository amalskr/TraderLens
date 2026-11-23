package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.model.money.satoshisToUsd

data class Wallet(val usd: UsdCents, val btc: Satoshis) {

    // USD + BTC value at the given price. Null if conversion fails.
    fun totalValue(price: PriceCents): UsdCents? = satoshisToUsd(btc, price)?.let { usd + it }

    companion object {
        val INITIAL = Wallet(UsdCents(1_000_000), Satoshis.ZERO)
    }
}
