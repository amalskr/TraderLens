package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.local.inMemoryDatabase
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletRepositoryImplTest {

    private val database = inMemoryDatabase()

    @Test
    fun createIfAbsentKeepsFirstWallet() = runTest {
        val repository = WalletRepositoryImpl(database, UnconfinedTestDispatcher(testScheduler))

        repository.createIfAbsent(Wallet.INITIAL)
        repository.createIfAbsent(Wallet(UsdCents(5), Satoshis(5)))

        assertEquals(Wallet.INITIAL, repository.observeWallet().first())
    }

    @Test
    fun observeWalletEmitsUpdates() = runTest {
        val repository = WalletRepositoryImpl(database, UnconfinedTestDispatcher(testScheduler))
        val emissions = mutableListOf<Wallet>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeWallet().toList(emissions) }

        repository.createIfAbsent(Wallet.INITIAL)
        database.walletQueries.update(usd_cents = 910_000, btc_sats = 1_000_000)

        assertEquals(listOf(Wallet.INITIAL, Wallet(UsdCents(910_000), Satoshis(1_000_000))), emissions)
    }
}
