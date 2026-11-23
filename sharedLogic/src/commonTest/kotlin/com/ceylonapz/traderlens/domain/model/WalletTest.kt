package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletTest {

    @Test
    fun initialHasTenThousandUsdAndNoBtc() {
        assertEquals(UsdCents(1_000_000), Wallet.INITIAL.usd)
        assertEquals(Satoshis(0), Wallet.INITIAL.btc)
    }

    @Test
    fun totalValueWithNoBtcEqualsUsd() {
        val wallet = Wallet(UsdCents(123_456), Satoshis.ZERO)
        assertEquals(UsdCents(123_456), wallet.totalValue(PriceCents(9_000_000)))
    }

    @Test
    fun totalValueAddsBtcValueToUsd() {
        val wallet = Wallet(UsdCents(100_000), Satoshis(50_000_000))
        assertEquals(UsdCents(4_600_000), wallet.totalValue(PriceCents(9_000_000)))
    }

    @Test
    fun buyMovesUsdIntoBtc() {
        val wallet = Wallet(UsdCents(1_000_000), Satoshis(0))
        assertEquals(
            Wallet(UsdCents(100_000), Satoshis(1_000_000)),
            wallet.afterTrade(TradeSide.BUY, UsdCents(900_000), Satoshis(1_000_000)),
        )
    }

    @Test
    fun sellMovesBtcIntoUsd() {
        val wallet = Wallet(UsdCents(0), Satoshis(1_000_000))
        assertEquals(
            Wallet(UsdCents(900_000), Satoshis(0)),
            wallet.afterTrade(TradeSide.SELL, UsdCents(900_000), Satoshis(1_000_000)),
        )
    }

    @Test
    fun tradeThatWouldGoNegativeReturnsNull() {
        val wallet = Wallet(UsdCents(100), Satoshis(100))
        assertNull(wallet.afterTrade(TradeSide.BUY, UsdCents(101), Satoshis(1)))
        assertNull(wallet.afterTrade(TradeSide.SELL, UsdCents(1), Satoshis(101)))
    }
}
