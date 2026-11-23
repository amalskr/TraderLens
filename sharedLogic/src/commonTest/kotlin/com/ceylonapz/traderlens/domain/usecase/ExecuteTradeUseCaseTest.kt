package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.fakes.FakePriceFeed
import com.ceylonapz.traderlens.domain.fakes.FakeTradeRepository
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.trading.TradingEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.TimeSource

class ExecuteTradeUseCaseTest {

    @Test
    fun executesOrderThroughEngineAtLivePrice() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)
        val feed = FakePriceFeed().apply {
            latestPrice.value = LivePrice(PriceCents(9_000_000), TimeSource.Monotonic.markNow())
        }
        val executeTrade = ExecuteTradeUseCase(TradingEngine(repository, feed, nowMillis = { 1L }))

        val result = executeTrade(MarketOrder.Buy(UsdCents(100_000)))

        assertEquals(PriceCents(9_000_000), assertIs<TradeResult.Success>(result).trade.price)
        assertEquals(UsdCents(900_000), repository.wallet.usd)
    }
}
