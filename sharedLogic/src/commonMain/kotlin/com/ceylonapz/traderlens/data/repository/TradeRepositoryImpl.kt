package com.ceylonapz.traderlens.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ceylonapz.traderlens.data.local.TradeEntity
import com.ceylonapz.traderlens.data.local.TraderLensDatabase
import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TradeRepositoryImpl(
    private val database: TraderLensDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TradeRepository {

    override fun observeTrades(): Flow<List<Trade>> =
        database.tradeQueries.selectAll().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toTrade() } }

    override suspend fun execute(
        side: TradeSide,
        price: PriceCents,
        usd: UsdCents,
        btc: Satoshis,
        timestampMillis: Long,
    ): Trade? = withContext(dispatcher) {
        database.transactionWithResult {
            val row = database.walletQueries.select().executeAsOne()
            val updated = Wallet(UsdCents(row.usd_cents), Satoshis(row.btc_sats)).afterTrade(side, usd, btc)
                ?: rollback(null)
            database.walletQueries.update(usd_cents = updated.usd.value, btc_sats = updated.btc.value)
            database.tradeQueries.insert(side.name, price.value, usd.value, btc.value, timestampMillis)
            Trade(database.tradeQueries.lastInsertId().executeAsOne(), side, price, usd, btc, timestampMillis)
        }
    }
}

private fun TradeEntity.toTrade() = Trade(
    id = id,
    side = TradeSide.valueOf(side),
    price = PriceCents(price_cents),
    usd = UsdCents(usd_cents),
    btc = Satoshis(btc_sats),
    timestampMillis = timestamp_millis,
)
