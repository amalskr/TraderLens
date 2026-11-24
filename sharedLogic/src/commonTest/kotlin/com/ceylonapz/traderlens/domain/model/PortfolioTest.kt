package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortfolioTest {

    private fun portfolio(total: UsdCents?) = Portfolio(Wallet.INITIAL, PriceCents(6_000_000), total)

    @Test
    fun startingBalanceHasNoProfitOrLoss() {
        val p = portfolio(UsdCents(1_000_000))
        assertEquals(UsdCents(0), p.profitLoss)
        assertEquals(0L, p.profitLossBasisPoints)
    }

    @Test
    fun profitIsPositive() {
        val p = portfolio(UsdCents(1_012_345))
        assertEquals(UsdCents(12_345), p.profitLoss)
        assertEquals(123L, p.profitLossBasisPoints)
    }

    @Test
    fun lossIsNegative() {
        val p = portfolio(UsdCents(950_000))
        assertEquals(UsdCents(-50_000), p.profitLoss)
        assertEquals(-500L, p.profitLossBasisPoints)
    }

    @Test
    fun smallLossTruncatesBasisPointsToZero() {
        assertEquals(0L, portfolio(UsdCents(999_934)).profitLossBasisPoints)
    }

    @Test
    fun unknownWithoutPrice() {
        val p = Portfolio(Wallet(UsdCents(900_000), Satoshis(1)), price = null, totalValue = null)
        assertNull(p.profitLoss)
        assertNull(p.profitLossBasisPoints)
    }
}
