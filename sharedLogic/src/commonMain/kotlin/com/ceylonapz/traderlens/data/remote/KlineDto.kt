package com.ceylonapz.traderlens.data.remote

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class KlineDto(
    val openTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
)

// Binance sends each kline as a positional array: [openTime, open, high, low, close, ...].
fun JsonArray.toKlineDto(): KlineDto = KlineDto(
    openTime = this[0].jsonPrimitive.long,
    open = this[1].jsonPrimitive.content,
    high = this[2].jsonPrimitive.content,
    low = this[3].jsonPrimitive.content,
    close = this[4].jsonPrimitive.content,
)

fun KlineDto.toCandle(): Candle? {
    return Candle(
        openTimeMillis = openTime,
        open = PriceCents.parseOrNull(open) ?: return null,
        high = PriceCents.parseOrNull(high) ?: return null,
        low = PriceCents.parseOrNull(low) ?: return null,
        close = PriceCents.parseOrNull(close) ?: return null,
    )
}
