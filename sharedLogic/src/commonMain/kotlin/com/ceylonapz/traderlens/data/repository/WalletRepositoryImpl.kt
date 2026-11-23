package com.ceylonapz.traderlens.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ceylonapz.traderlens.data.local.TraderLensDatabase
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WalletRepositoryImpl(
    database: TraderLensDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WalletRepository {

    private val queries = database.walletQueries

    override suspend fun createIfAbsent(wallet: Wallet) = withContext(dispatcher) {
        queries.insertIfAbsent(usd_cents = wallet.usd.value, btc_sats = wallet.btc.value)
        Unit
    }

    override fun observeWallet(): Flow<Wallet> =
        queries.select().asFlow().mapToOneOrNull(dispatcher).filterNotNull().map {
            Wallet(UsdCents(it.usd_cents), Satoshis(it.btc_sats))
        }
}
