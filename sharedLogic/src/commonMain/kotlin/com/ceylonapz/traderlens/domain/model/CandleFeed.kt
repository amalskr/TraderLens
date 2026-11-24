package com.ceylonapz.traderlens.domain.model

data class CandleFeed(val candles: List<Candle>, val event: CandleFeedEvent, val updatedAtMillis: Long?)

sealed interface CandleFeedEvent {
    data object Cached : CandleFeedEvent
    data class Live(val candle: Candle) : CandleFeedEvent
    data class GapFill(val added: Int) : CandleFeedEvent
    data object GapFillFailed : CandleFeedEvent
}
