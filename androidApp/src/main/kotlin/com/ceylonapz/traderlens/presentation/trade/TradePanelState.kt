package com.ceylonapz.traderlens.presentation.trade

import com.ceylonapz.traderlens.domain.model.AmountUnit
import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents

data class TradePanelState(
    val side: TradeSide = TradeSide.BUY,
    val unit: AmountUnit = AmountUnit.USD,
    val amount: String = "",
    val preview: OrderPreview? = null,
    val confirming: Boolean = false,
    val submitting: Boolean = false,
    val lastResult: TradeResult? = null,
)

sealed interface OrderPreview {
    data class Ready(val order: MarketOrder, val usd: UsdCents, val btc: Satoshis, val price: PriceCents) : OrderPreview
    data class Blocked(val reason: TradeRejection) : OrderPreview
}
