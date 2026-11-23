package com.ceylonapz.traderlens.data.local

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletQueriesTest {

    private val queries = inMemoryDatabase().walletQueries

    @Test
    fun insertIfAbsentRunsOnceOnly() {
        queries.insertIfAbsent(usd_cents = 1_000_000, btc_sats = 0)
        queries.insertIfAbsent(usd_cents = 5, btc_sats = 5)

        val wallet = queries.select().executeAsOne()
        assertEquals(1_000_000, wallet.usd_cents)
        assertEquals(0, wallet.btc_sats)
    }

    @Test
    fun negativeUsdUpdateFails() {
        queries.insertIfAbsent(usd_cents = 1_000_000, btc_sats = 0)

        assertFailsWith<SQLException> { queries.update(usd_cents = -1, btc_sats = 0) }
        assertEquals(1_000_000, queries.select().executeAsOne().usd_cents)
    }

    @Test
    fun negativeBtcUpdateFails() {
        queries.insertIfAbsent(usd_cents = 1_000_000, btc_sats = 0)

        assertFailsWith<SQLException> { queries.update(usd_cents = 0, btc_sats = -1) }
        assertEquals(0, queries.select().executeAsOne().btc_sats)
    }
}
