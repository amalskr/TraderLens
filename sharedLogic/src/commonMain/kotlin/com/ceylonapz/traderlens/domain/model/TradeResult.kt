package com.ceylonapz.traderlens.domain.model

sealed interface TradeResult {
    data class Success(val trade: Trade) : TradeResult
    data class Rejected(val reason: TradeRejection) : TradeResult
}

enum class TradeRejection {
    AMOUNT_TOO_SMALL,
    INSUFFICIENT_FUNDS,
}
