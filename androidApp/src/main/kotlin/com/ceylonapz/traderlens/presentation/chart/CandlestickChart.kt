package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.presentation.format.formatUsd
import com.ceylonapz.traderlens.presentation.theme.DownColor
import com.ceylonapz.traderlens.presentation.theme.UpColor
import kotlin.math.abs
import kotlin.math.max

private val LabelStyle = TextStyle(color = Color.White, fontSize = 11.sp)

@Composable
fun CandlestickChart(candles: List<Candle>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val lastPrice = remember { LastPriceAnimation() }
    val last = candles.lastOrNull()
    if (last != null) LaunchedEffect(last) { lastPrice.update(last) }

    Canvas(modifier = modifier) {
        if (last == null) return@Canvas

        val price = lastPrice.price
        val priceLabelTxt = textMeasurer.measure(price.formatUsd(), LabelStyle)
        val labelPadding = 4.dp.toPx()
        val plotWidth = size.width - priceLabelTxt.size.width - labelPadding * 2

        val maxPrice = candles.maxOf { it.high.value }
        val minPrice = candles.minOf { it.low.value }
        val range = max(maxPrice - minPrice, 1L).toFloat()
        fun y(price: Long): Float = (maxPrice - price) / range * size.height

        val slotWidth = plotWidth / candles.size
        candles.forEachIndexed { index, candle ->
            val shown = if (index == candles.lastIndex) candle.copy(close = price) else candle
            drawCandle(shown, centerX = slotWidth * (index + 0.5f), bodyWidth = slotWidth * 0.7f, y = ::y)
        }

        //drawing price label and line live
        val priceY = y(price.value)
        val lineColor = if (lastPrice.rising) UpColor else DownColor
        drawLine(
            color = lineColor,
            start = Offset(0f, priceY),
            end = Offset(plotWidth, priceY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
        drawPriceLabel(priceLabelTxt, lineColor, lastPrice.flashAlpha, x = plotWidth, centerY = priceY)
    }
}

private fun DrawScope.drawCandle(candle: Candle, centerX: Float, bodyWidth: Float, y: (Long) -> Float) {
    val color = if (candle.close >= candle.open) UpColor else DownColor
    val wickWidth = 1.dp.toPx()
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

private fun DrawScope.drawPriceLabel(
    text: TextLayoutResult,
    color: Color,
    flashAlpha: Float,
    x: Float,
    centerY: Float,
) {
    val padding = 8.dp.toPx()
    val boxSize = Size(text.size.width + padding * 2, text.size.height + padding)
    val top = (centerY - boxSize.height / 2).coerceIn(0f, max(size.height - boxSize.height, 0f))
    drawRect(color = lerp(color, Color.White, flashAlpha * 0.5f), topLeft = Offset(x, top), size = boxSize)
    drawText(text, topLeft = Offset(x + padding, top + padding / 2))
}
