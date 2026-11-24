package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.AmountUnit
import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import com.ceylonapz.traderlens.domain.model.money.satoshisToUsd
import com.ceylonapz.traderlens.domain.model.money.usdToSatoshis

/** Builds an order from typed input; an amount in the other unit is converted at [price], rounding down. */
fun parseMarketOrder(side: TradeSide, unit: AmountUnit, text: String, price: PriceCents?): MarketOrder? =
    when (side) {
        TradeSide.BUY -> when (unit) {
            AmountUnit.USD -> UsdCents.parseOrNull(text)
            AmountUnit.BTC -> Satoshis.parseOrNull(text)?.let { sats -> price?.let { satoshisToUsd(sats, it) } }
        }?.let(MarketOrder::Buy)
        TradeSide.SELL -> when (unit) {
            AmountUnit.BTC -> Satoshis.parseOrNull(text)
            AmountUnit.USD -> UsdCents.parseOrNull(text)?.let { usd -> price?.let { usdToSatoshis(usd, it) } }
        }?.let(MarketOrder::Sell)
    }
