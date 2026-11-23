package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderValidatorTest {

    private val validator = OrderValidator()
    private val price = PriceCents(9_000_000)

    private fun invalid(reason: TradeRejection) = OrderValidation.Invalid(reason)

    @Test
    fun buyAtMinimumIsValidAndRoundsBtcDown() {
        assertEquals(
            OrderValidation.Valid(TradeSide.BUY, UsdCents(500), Satoshis(5_555)),
            validator.validate(MarketOrder.Buy(UsdCents(500)), price),
        )
    }

    @Test
    fun buyBelowMinimumIsRejected() {
        assertEquals(invalid(TradeRejection.BELOW_MINIMUM), validator.validate(MarketOrder.Buy(UsdCents(499)), price))
    }

    @Test
    fun sellIsValidAndRoundsUsdDown() {
        assertEquals(
            OrderValidation.Valid(TradeSide.SELL, UsdCents(900), Satoshis(10_001)),
            validator.validate(MarketOrder.Sell(Satoshis(10_001)), price),
        )
    }

    @Test
    fun sellWorthLessThanMinimumIsRejected() {
        // 5_555 sats at $90,000 is $4.99 after rounding down.
        assertEquals(invalid(TradeRejection.BELOW_MINIMUM), validator.validate(MarketOrder.Sell(Satoshis(5_555)), price))
    }

    @Test
    fun zeroAmountIsRejected() {
        assertEquals(invalid(TradeRejection.INVALID_AMOUNT), validator.validate(MarketOrder.Buy(UsdCents(0)), price))
        assertEquals(invalid(TradeRejection.INVALID_AMOUNT), validator.validate(MarketOrder.Sell(Satoshis(0)), price))
    }

    @Test
    fun negativeAmountIsRejected() {
        assertEquals(invalid(TradeRejection.INVALID_AMOUNT), validator.validate(MarketOrder.Buy(UsdCents(-500)), price))
        assertEquals(invalid(TradeRejection.INVALID_AMOUNT), validator.validate(MarketOrder.Sell(Satoshis(-1)), price))
    }

    @Test
    fun amountThatOverflowsIsRejected() {
        assertEquals(
            invalid(TradeRejection.INVALID_AMOUNT),
            validator.validate(MarketOrder.Sell(Satoshis(Long.MAX_VALUE)), PriceCents(1_000_000_000_000)),
        )
    }

    @Test
    fun nonPositivePriceIsRejected() {
        assertEquals(invalid(TradeRejection.INVALID_PRICE), validator.validate(MarketOrder.Buy(UsdCents(500)), PriceCents(0)))
        assertEquals(invalid(TradeRejection.INVALID_PRICE), validator.validate(MarketOrder.Buy(UsdCents(500)), PriceCents(-1)))
    }

    @Test
    fun buyThatRoundsToZeroBtcIsRejected() {
        val absurdPrice = PriceCents(Long.MAX_VALUE)
        assertEquals(invalid(TradeRejection.BELOW_MINIMUM), validator.validate(MarketOrder.Buy(UsdCents(500)), absurdPrice))
    }

    @Test
    fun minimumIsConfigurable() {
        val strict = OrderValidator(minimumUsd = UsdCents(1_000))
        assertEquals(invalid(TradeRejection.BELOW_MINIMUM), strict.validate(MarketOrder.Buy(UsdCents(999)), price))
    }
}
