package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class TradingEngineTest {

    // Suspends between reading and writing the wallet, so unguarded concurrent trades would double spend.
    private class FakeTradeRepository(var wallet: Wallet) : TradeRepository {
        val trades = mutableListOf<Trade>()

        override suspend fun execute(
            side: TradeSide,
            price: PriceCents,
            usd: UsdCents,
            btc: Satoshis,
            timestampMillis: Long,
        ): Trade? {
            val current = wallet
            delay(10)
            wallet = current.afterTrade(side, usd, btc) ?: return null
            return Trade(trades.size + 1L, side, price, usd, btc, timestampMillis).also { trades += it }
        }
    }

    private class FakePriceFeed : PriceFeed {
        override val connectionState = MutableStateFlow(ConnectionState.Connected)
        override val latestPrice = MutableStateFlow<LivePrice?>(null)
    }

    private val price = PriceCents(9_000_000)
    private val timeSource = TestTimeSource()
    private val feed = FakePriceFeed().apply { latestPrice.value = LivePrice(price, timeSource.markNow()) }

    private fun engine(repository: TradeRepository) = TradingEngine(repository, feed, nowMillis = { 1_234L })

    @Test
    fun buyConvertsUsdToSatoshisRoundedDown() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)

        val result = engine(repository).entry(MarketOrder.Buy(UsdCents(1_000_000)))

        val trade = assertIs<TradeResult.Success>(result).trade
        assertEquals(Trade(1, TradeSide.BUY, price, UsdCents(1_000_000), Satoshis(11_111_111), 1_234), trade)
        assertEquals(Wallet(UsdCents(0), Satoshis(11_111_111)), repository.wallet)
    }

    @Test
    fun sellConvertsSatoshisToUsdRoundedDown() = runTest {
        val repository = FakeTradeRepository(Wallet(UsdCents(0), Satoshis(11_111_111)))

        val result = engine(repository).entry(MarketOrder.Sell(Satoshis(11_111_111)))

        val trade = assertIs<TradeResult.Success>(result).trade
        assertEquals(UsdCents(999_999), trade.usd)
        assertEquals(Wallet(UsdCents(999_999), Satoshis(0)), repository.wallet)
    }

    @Test
    fun invalidOrderIsRejectedWithoutTouchingRepository() = runTest {
        val repository = FakeTradeRepository(Wallet(UsdCents(0), Satoshis(1)))

        val result = engine(repository).entry(MarketOrder.Sell(Satoshis(1)))

        assertEquals(TradeResult.Rejected(TradeRejection.BELOW_MINIMUM), result)
        assertEquals(emptyList(), repository.trades)
    }

    @Test
    fun insufficientFundsIsRejected() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)

        val result = engine(repository).entry(MarketOrder.Buy(UsdCents(1_000_001)))

        assertEquals(TradeResult.Rejected(TradeRejection.INSUFFICIENT_FUNDS), result)
        assertEquals(Wallet.INITIAL, repository.wallet)
    }

    @Test
    fun concurrentTradesCannotDoubleSpend() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)
        val engine = engine(repository)

        val results = List(2) { async { engine.entry(MarketOrder.Buy(UsdCents(600_000))) } }.awaitAll()

        assertEquals(1, results.count { it is TradeResult.Success })
        assertEquals(1, results.count { it == TradeResult.Rejected(TradeRejection.INSUFFICIENT_FUNDS) })
        assertEquals(UsdCents(400_000), repository.wallet.usd)
    }

    @Test
    fun disconnectedIsRejectedWithoutTouchingRepository() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)
        feed.connectionState.value = ConnectionState.Reconnecting

        val result = engine(repository).entry(MarketOrder.Buy(UsdCents(100_000)))

        assertEquals(TradeResult.Rejected(TradeRejection.DISCONNECTED), result)
        assertEquals(emptyList(), repository.trades)
    }

    @Test
    fun stalePriceIsRejected() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)
        timeSource += 11.seconds

        val result = engine(repository).entry(MarketOrder.Buy(UsdCents(100_000)))

        assertEquals(TradeResult.Rejected(TradeRejection.STALE_PRICE), result)
        assertEquals(emptyList(), repository.trades)
    }

    @Test
    fun noLivePriceYetIsRejected() = runTest {
        feed.latestPrice.value = null

        val result = engine(FakeTradeRepository(Wallet.INITIAL)).entry(MarketOrder.Buy(UsdCents(100_000)))

        assertEquals(TradeResult.Rejected(TradeRejection.STALE_PRICE), result)
    }

    @Test
    fun queuedOrderFillsAtPriceWhenItRuns() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)
        val engine = engine(repository)
        val newPrice = PriceCents(10_000_000)

        val first = async { engine.entry(MarketOrder.Buy(UsdCents(100_000))) }
        val second = async { engine.entry(MarketOrder.Buy(UsdCents(100_000))) }
        testScheduler.runCurrent()
        feed.latestPrice.value = LivePrice(newPrice, timeSource.markNow())

        assertEquals(price, assertIs<TradeResult.Success>(first.await()).trade.price)
        assertEquals(newPrice, assertIs<TradeResult.Success>(second.await()).trade.price)
    }
}
