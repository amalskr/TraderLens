package com.ceylonapz.traderlens.domain.model.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyArithmeticTest {

    @Test
    fun plusAndMinusWork() {
        assertEquals(UsdCents(150), UsdCents(100) + UsdCents(50))
        assertEquals(UsdCents(50), UsdCents(100) - UsdCents(50))
        assertEquals(Satoshis(3), Satoshis(1) + Satoshis(2))
        assertEquals(PriceCents(-10), PriceCents(90) - PriceCents(100))
    }

    @Test
    fun compareToOrdersByValue() {
        assertTrue(UsdCents(1) < UsdCents(2))
        assertTrue(Satoshis(5) > Satoshis(4))
        assertTrue(PriceCents(7) >= PriceCents(7))
    }

    @Test
    fun plusThrowsOnOverflowInsteadOfWrapping() {
        assertFailsWith<ArithmeticException> { UsdCents(Long.MAX_VALUE) + UsdCents(1) }
    }

    @Test
    fun minusThrowsOnOverflowInsteadOfWrapping() {
        assertFailsWith<ArithmeticException> { Satoshis(Long.MIN_VALUE) - Satoshis(1) }
    }
}
