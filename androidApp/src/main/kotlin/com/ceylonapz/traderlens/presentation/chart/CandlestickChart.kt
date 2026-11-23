package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ceylonapz.traderlens.domain.model.Candle
import kotlin.math.abs
import kotlin.math.max

private val UpColor = Color(0xFF26A69A)
private val DownColor = Color(0xFFEF5350)

@Composable
fun CandlestickChart(candles: List<Candle>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (candles.isEmpty()) return@Canvas

        val maxPrice = candles.maxOf { it.high.value }
        val minPrice = candles.minOf { it.low.value }
        val range = max(maxPrice - minPrice, 1L).toFloat()
        fun y(price: Long): Float = (maxPrice - price) / range * size.height

        val slotWidth = size.width / candles.size
        val bodyWidth = slotWidth * 0.7f
        val wickWidth = 1.dp.toPx()

        candles.forEachIndexed { index, candle ->
            val color = if (candle.close >= candle.open) UpColor else DownColor
            val centerX = slotWidth * (index + 0.5f)

            drawLine(
                color = color,
                start = Offset(centerX, y(candle.high.value)),
                end = Offset(centerX, y(candle.low.value)),
                strokeWidth = wickWidth,
            )

            val bodyTop = y(max(candle.open.value, candle.close.value))
            val bodyHeight = max(abs(y(candle.open.value) - y(candle.close.value)), wickWidth)
            drawRect(
                color = color,
                topLeft = Offset(centerX - bodyWidth / 2, bodyTop),
                size = Size(bodyWidth, bodyHeight),
            )
        }
    }
}
