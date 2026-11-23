package com.ceylonapz.traderlens.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KlineEventDto(@SerialName("k") val kline: Kline) {

    @Serializable
    data class Kline(
        @SerialName("t") val openTime: Long,
        @SerialName("o") val open: String,
        @SerialName("h") val high: String,
        @SerialName("l") val low: String,
        @SerialName("c") val close: String,
    )
}

fun KlineEventDto.toKlineDto(): KlineDto = KlineDto(
    openTime = kline.openTime,
    open = kline.open,
    high = kline.high,
    low = kline.low,
    close = kline.close,
)
