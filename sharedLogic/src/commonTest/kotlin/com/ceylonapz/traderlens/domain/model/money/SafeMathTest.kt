package com.ceylonapz.traderlens.domain.model.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SafeMathTest {

    @Test
    fun mulDivFloorReturnsNullWhenDivisorIsZero() {
        assertNull(mulDivFloorOrNull(10, 5, 0))
    }

    @Test
    fun mulDivFloorReturnsNullWhenDivisorIsNegative() {
        assertNull(mulDivFloorOrNull(10, 5, -1))
    }

    @Test
    fun mulDivFloorRoundsDown() {
        assertEquals(3, mulDivFloorOrNull(7, 1, 2))
    }
}
