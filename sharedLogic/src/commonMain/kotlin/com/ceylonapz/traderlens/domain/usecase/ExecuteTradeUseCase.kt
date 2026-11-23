package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.trading.TradingEngine

class ExecuteTradeUseCase(private val tradingEngine: TradingEngine) {

    suspend operator fun invoke(order: MarketOrder): TradeResult = tradingEngine.entry(order)
}
