package com.ceylonapz.traderlens.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastPriceQueriesTest {

    private val queries = inMemoryDatabase().lastPriceQueries

    @Test
    fun selectIsEmptyBeforeFirstUpsert() {
        assertNull(queries.select().executeAsOneOrNull())
    }

    @Test
    fun upsertThenSelectReturnsNewValueAndTimestamp() {
        queries.upsert(price_cents = 9_000_000, timestamp_millis = 1_000)
        queries.upsert(price_cents = 9_100_000, timestamp_millis = 2_000)

        val lastPrice = queries.select().executeAsOne()

        assertEquals(9_100_000, lastPrice.price_cents)
        assertEquals(2_000, lastPrice.timestamp_millis)
    }
}
