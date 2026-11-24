package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.UsdCents

// totalValue is null until a live price arrives.
data class Portfolio(val wallet: Wallet, val price: PriceCents?, val totalValue: UsdCents?) {

    // Measured against the starting balance; there are no deposits or withdrawals to adjust for.
    val profitLoss: UsdCents? get() = totalValue?.let { it - Wallet.INITIAL.usd }

    /** Profit or loss in basis points (1 bp = 0.01%), truncated toward zero. */
    val profitLossBasisPoints: Long? get() = profitLoss?.let { it.value * BASIS_POINTS / Wallet.INITIAL.usd.value }

    private companion object {
        const val BASIS_POINTS = 10_000L
    }
}
