package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.model.money.satoshisToUsd

data class Wallet(val usd: UsdCents, val btc: Satoshis) {

    // USD + BTC value at the given price. Null if conversion fails.
    fun totalValue(price: PriceCents): UsdCents? = satoshisToUsd(btc, price)?.let { usd + it }

    /** Balances after a trade, or null if they would go negative. */
    fun afterTrade(side: TradeSide, usd: UsdCents, btc: Satoshis): Wallet? = when (side) {
        TradeSide.BUY -> if (usd > this.usd) null else Wallet(this.usd - usd, this.btc + btc)
        TradeSide.SELL -> if (btc > this.btc) null else Wallet(this.usd + usd, this.btc - btc)
    }

    companion object {
        val INITIAL = Wallet(UsdCents(1_000_000), Satoshis.ZERO)
    }
}
