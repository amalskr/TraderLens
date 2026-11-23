package com.ceylonapz.traderlens.domain.model.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyConversionsTest {

    private val price50k = PriceCents(5_000_000)
    private val price91k = PriceCents(9_123_456)

    @Test
    fun usdToSatoshisExactValue() {
        assertEquals(Satoshis(1_000_000), usdToSatoshis(UsdCents(50_000), price50k))
    }

    @Test
    fun satoshisToUsdExactValue() {
        assertEquals(UsdCents(50_000), satoshisToUsd(Satoshis(1_000_000), price50k))
    }

    @Test
    fun usdToSatoshisRoundsDown() {
        assertEquals(Satoshis(1_096), usdToSatoshis(UsdCents(100), price91k))
    }

    @Test
    fun satoshisToUsdRoundsDown() {
        assertEquals(UsdCents(99), satoshisToUsd(Satoshis(1_096), price91k))
    }

    @Test
    fun zeroAmountConvertsToZero() {
        assertEquals(Satoshis.ZERO, usdToSatoshis(UsdCents.ZERO, price91k))
        assertEquals(UsdCents.ZERO, satoshisToUsd(Satoshis.ZERO, price91k))
    }

    @Test
    fun largeRealisticValuesDoNotOverflow() {
        val allBitcoin = Satoshis(21_000_000L * Satoshis.PER_BTC)
        val price1m = PriceCents(100_000_000)
        assertEquals(UsdCents(2_100_000_000_000_000), satoshisToUsd(allBitcoin, price1m))
    }

    @Test
    fun returnsNullOnOverflow() {
        assertNull(usdToSatoshis(UsdCents(Long.MAX_VALUE), PriceCents(1)))
        assertNull(satoshisToUsd(Satoshis(Long.MAX_VALUE), PriceCents(Long.MAX_VALUE)))
    }

    @Test
    fun returnsNullForInvalidInput() {
        assertNull(usdToSatoshis(UsdCents(100), PriceCents(0)))
        assertNull(usdToSatoshis(UsdCents(-1), price91k))
        assertNull(satoshisToUsd(Satoshis(100), PriceCents(-5)))
        assertNull(satoshisToUsd(Satoshis(-1), price91k))
    }
}
