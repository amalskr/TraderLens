package com.ceylonapz.traderlens.domain.model

data class CandleFeed(val candles: List<Candle>, val event: CandleFeedEvent)

sealed interface CandleFeedEvent {
    data class Live(val candle: Candle) : CandleFeedEvent
    data class GapFill(val added: Int) : CandleFeedEvent
    data object GapFillFailed : CandleFeedEvent
}
