package com.ceylonapz.traderlens.data.local

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TradeQueriesTest {

    private val queries = inMemoryDatabase().tradeQueries

    @Test
    fun selectAllReturnsNewestFirst() {
        queries.insert("BUY", price_cents = 9_000_000, usd_cents = 90_000, btc_sats = 1_000_000, timestamp_millis = 1)
        queries.insert("SELL", price_cents = 9_100_000, usd_cents = 45_500, btc_sats = 500_000, timestamp_millis = 2)

        val trades = queries.selectAll().executeAsList()

        assertEquals(listOf("SELL", "BUY"), trades.map { it.side })
        assertEquals(listOf(2L, 1L), trades.map { it.timestamp_millis })
    }

    @Test
    fun zeroUsdAmountFails() {
        assertFailsWith<SQLException> {
            queries.insert("BUY", price_cents = 9_000_000, usd_cents = 0, btc_sats = 1_000_000, timestamp_millis = 1)
        }
        assertTrue(queries.selectAll().executeAsList().isEmpty())
    }

    @Test
    fun zeroBtcAmountFails() {
        assertFailsWith<SQLException> {
            queries.insert("BUY", price_cents = 9_000_000, usd_cents = 90_000, btc_sats = 0, timestamp_millis = 1)
        }
        assertTrue(queries.selectAll().executeAsList().isEmpty())
    }
}
