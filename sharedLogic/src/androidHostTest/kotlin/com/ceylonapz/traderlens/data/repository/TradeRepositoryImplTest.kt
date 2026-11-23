package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.local.inMemoryDatabase
import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradeRepositoryImplTest {

    private val database = inMemoryDatabase().also { it.walletQueries.insertIfAbsent(1_000_000, 0) }
    private val price = PriceCents(9_000_000)

    private fun walletRow() = database.walletQueries.select().executeAsOne().let { it.usd_cents to it.btc_sats }

    private fun tradeCount() = database.tradeQueries.selectAll().executeAsList().size

    @Test
    fun buyUpdatesWalletAndRecordsTrade() = runTest {
        val repository = TradeRepositoryImpl(database, UnconfinedTestDispatcher(testScheduler))

        val trade = repository.execute(TradeSide.BUY, price, UsdCents(900_000), Satoshis(10_000_000), 1_000)

        assertEquals(Trade(1, TradeSide.BUY, price, UsdCents(900_000), Satoshis(10_000_000), 1_000), trade)
        assertEquals(100_000L to 10_000_000L, walletRow())
        assertEquals(1, tradeCount())
    }

    @Test
    fun sellAfterBuyGetsNextId() = runTest {
        val repository = TradeRepositoryImpl(database, UnconfinedTestDispatcher(testScheduler))
        repository.execute(TradeSide.BUY, price, UsdCents(900_000), Satoshis(10_000_000), 1_000)

        val trade = repository.execute(TradeSide.SELL, price, UsdCents(450_000), Satoshis(5_000_000), 2_000)

        assertEquals(2L, trade?.id)
        assertEquals(550_000L to 5_000_000L, walletRow())
    }

    @Test
    fun insufficientFundsRollsBack() = runTest {
        val repository = TradeRepositoryImpl(database, UnconfinedTestDispatcher(testScheduler))

        assertNull(repository.execute(TradeSide.BUY, price, UsdCents(1_000_001), Satoshis(1), 1_000))
        assertEquals(1_000_000L to 0L, walletRow())
        assertEquals(0, tradeCount())
    }

    @Test
    fun failedTradeInsertRollsBackWalletUpdate() = runTest {
        val repository = TradeRepositoryImpl(database, UnconfinedTestDispatcher(testScheduler))

        assertFailsWith<SQLException> {
            repository.execute(TradeSide.BUY, price, UsdCents(900_000), Satoshis(0), 1_000)
        }
        assertEquals(1_000_000L to 0L, walletRow())
        assertTrue(database.tradeQueries.selectAll().executeAsList().isEmpty())
    }
}
