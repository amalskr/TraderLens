package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
