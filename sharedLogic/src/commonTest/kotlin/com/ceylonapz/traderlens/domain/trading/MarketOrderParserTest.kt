package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.AmountUnit
import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarketOrderParserTest {

    private val price = PriceCents(6_000_000)

    @Test
    fun buyInUsdKeepsAmount() {
        assertEquals(MarketOrder.Buy(UsdCents(100_000)), parseMarketOrder(TradeSide.BUY, AmountUnit.USD, "1000", price))
    }

    @Test
    fun sellInBtcKeepsAmount() {
        assertEquals(MarketOrder.Sell(Satoshis(1_000_000)), parseMarketOrder(TradeSide.SELL, AmountUnit.BTC, "0.01", price))
    }

    @Test
    fun buyInBtcConvertsToUsd() {
        assertEquals(MarketOrder.Buy(UsdCents(60_000)), parseMarketOrder(TradeSide.BUY, AmountUnit.BTC, "0.01", price))
    }

    @Test
    fun sellInUsdConvertsToSatoshisRoundingDown() {
        assertEquals(MarketOrder.Sell(Satoshis(1_666_666)), parseMarketOrder(TradeSide.SELL, AmountUnit.USD, "1000", price))
    }

    @Test
    fun crossUnitNeedsPrice() {
        assertNull(parseMarketOrder(TradeSide.BUY, AmountUnit.BTC, "0.01", null))
        assertNull(parseMarketOrder(TradeSide.SELL, AmountUnit.USD, "1000", null))
    }

    @Test
    fun sameUnitWorksWithoutPrice() {
        assertEquals(MarketOrder.Buy(UsdCents(500)), parseMarketOrder(TradeSide.BUY, AmountUnit.USD, "5", null))
    }

    @Test
    fun invalidTextReturnsNull() {
        assertNull(parseMarketOrder(TradeSide.BUY, AmountUnit.USD, "", price))
        assertNull(parseMarketOrder(TradeSide.SELL, AmountUnit.BTC, "1.2.3", price))
    }
}
