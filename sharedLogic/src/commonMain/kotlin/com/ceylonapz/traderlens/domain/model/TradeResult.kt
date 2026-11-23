package com.ceylonapz.traderlens.domain.model

sealed interface TradeResult {
    data class Success(val trade: Trade) : TradeResult
    data class Rejected(val reason: TradeRejection) : TradeResult
}

enum class TradeRejection {
    INVALID_PRICE,
    INVALID_AMOUNT,
    BELOW_MINIMUM,
    INSUFFICIENT_FUNDS,
}
