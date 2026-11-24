package com.ceylonapz.traderlens.domain.model.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountParserTest {

    @Test
    fun parsesUsdToCents() {
        assertEquals(UsdCents(100_050), UsdCents.parseOrNull("1000.50"))
        assertEquals(UsdCents(100_000), UsdCents.parseOrNull("1000"))
    }

    @Test
    fun truncatesUsdPastCents() {
        assertEquals(UsdCents(1_099), UsdCents.parseOrNull("10.999"))
    }

    @Test
    fun parsesBtcToSatoshis() {
        assertEquals(Satoshis(1), Satoshis.parseOrNull("0.00000001"))
        assertEquals(Satoshis(150_000_000), Satoshis.parseOrNull("1.5"))
    }

    @Test
    fun truncatesBtcPastSatoshis() {
        assertEquals(Satoshis(1), Satoshis.parseOrNull("0.000000019"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(UsdCents.parseOrNull(""))
        assertNull(UsdCents.parseOrNull("-5"))
        assertNull(Satoshis.parseOrNull("1."))
        assertNull(Satoshis.parseOrNull("abc"))
    }

    @Test
    fun rejectsSatoshiOverflow() {
        assertNull(Satoshis.parseOrNull("100000000000"))
    }
}
