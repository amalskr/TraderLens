package com.ceylonapz.traderlens.domain.model.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceCentsParserTest {

    @Test
    fun parsesNormalBinanceValue() {
        assertEquals(PriceCents(9_123_456), PriceCents.parseOrNull("91234.56000000"))
    }

    @Test
    fun parsesValueWithoutDecimalPoint() {
        assertEquals(PriceCents(9_123_400), PriceCents.parseOrNull("91234"))
    }

    @Test
    fun parsesValueWithOneDecimal() {
        assertEquals(PriceCents(9_123_450), PriceCents.parseOrNull("91234.5"))
    }

    @Test
    fun truncatesManyDecimalsInsteadOfRounding() {
        assertEquals(PriceCents(9_123_456), PriceCents.parseOrNull("91234.56789"))
    }

    @Test
    fun parsesSubCentValueAsZero() {
        assertEquals(PriceCents(0), PriceCents.parseOrNull("0.00999"))
    }

    @Test
    fun returnsNullForEmptyString() {
        assertNull(PriceCents.parseOrNull(""))
    }

    @Test
    fun returnsNullForInvalidText() {
        assertNull(PriceCents.parseOrNull("abc"))
        assertNull(PriceCents.parseOrNull("12a.50"))
        assertNull(PriceCents.parseOrNull("1.2.3"))
        assertNull(PriceCents.parseOrNull(".5"))
        assertNull(PriceCents.parseOrNull("5."))
        assertNull(PriceCents.parseOrNull(" 91234.56"))
        assertNull(PriceCents.parseOrNull("1e5"))
    }

    @Test
    fun returnsNullForNegativeValue() {
        assertNull(PriceCents.parseOrNull("-91234.56"))
    }

    @Test
    fun returnsNullWhenValueOverflowsLong() {
        assertNull(PriceCents.parseOrNull("99999999999999999999"))
        assertNull(PriceCents.parseOrNull("92233720368547758.08"))
    }

    @Test
    fun parsesLargestRepresentableValue() {
        assertEquals(PriceCents(Long.MAX_VALUE), PriceCents.parseOrNull("92233720368547758.07"))
    }
}
