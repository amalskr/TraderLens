package com.ceylonapz.traderlens.domain.fakes

import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class FakePriceFeed : PriceFeed {
    override val connectionState = MutableStateFlow(ConnectionState.Connected)
    override val latestPrice = MutableStateFlow<LivePrice?>(null)
}

class FakeWalletRepository(initial: Wallet? = null) : WalletRepository {
    val wallet = MutableStateFlow(initial)

    override suspend fun createIfAbsent(wallet: Wallet) {
        if (this.wallet.value == null) this.wallet.value = wallet
    }

    override fun observeWallet(): Flow<Wallet> = wallet.filterNotNull()
}

// Suspends between reading and writing the wallet, so unguarded concurrent trades would double spend.
class FakeTradeRepository(var wallet: Wallet) : TradeRepository {
    val trades = MutableStateFlow<List<Trade>>(emptyList())
    var failNext = false

    override fun observeTrades(): Flow<List<Trade>> = trades

    override suspend fun execute(
        side: TradeSide,
        price: PriceCents,
        usd: UsdCents,
        btc: Satoshis,
        timestampMillis: Long,
    ): Trade? {
        val current = wallet
        delay(10)
        if (failNext) {
            failNext = false
            error("Database unavailable")
        }
        wallet = current.afterTrade(side, usd, btc) ?: return null
        return Trade(trades.value.size + 1L, side, price, usd, btc, timestampMillis).also {
            trades.value = listOf(it) + trades.value
        }
    }
}
