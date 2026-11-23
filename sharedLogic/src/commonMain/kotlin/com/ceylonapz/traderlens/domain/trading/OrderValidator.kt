package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.model.money.satoshisToUsd
import com.ceylonapz.traderlens.domain.model.money.usdToSatoshis

sealed interface OrderValidation {
    data class Valid(val side: TradeSide, val usd: UsdCents, val btc: Satoshis) : OrderValidation
    data class Invalid(val reason: TradeRejection) : OrderValidation
}

// Balance is not checked here: it is only reliable inside the trade transaction.
class OrderValidator(private val minimumUsd: UsdCents = MINIMUM_ORDER_USD) {

    fun validate(order: MarketOrder, price: PriceCents): OrderValidation {
        if (price.value <= 0) return OrderValidation.Invalid(TradeRejection.INVALID_PRICE)

        val (side, usd, btc) = when (order) {
            is MarketOrder.Buy -> {
                if (order.usd.value <= 0) return OrderValidation.Invalid(TradeRejection.INVALID_AMOUNT)
                Triple(TradeSide.BUY, order.usd, usdToSatoshis(order.usd, price))
            }
            is MarketOrder.Sell -> {
                if (order.btc.value <= 0) return OrderValidation.Invalid(TradeRejection.INVALID_AMOUNT)
                Triple(TradeSide.SELL, satoshisToUsd(order.btc, price), order.btc)
            }
        }
        if (usd == null || btc == null) return OrderValidation.Invalid(TradeRejection.INVALID_AMOUNT)
        if (usd < minimumUsd || btc.value == 0L) return OrderValidation.Invalid(TradeRejection.BELOW_MINIMUM)
        return OrderValidation.Valid(side, usd, btc)
    }

    companion object {
        // Binance's minimum notional for BTCUSDT.
        val MINIMUM_ORDER_USD = UsdCents(500)
    }
}
