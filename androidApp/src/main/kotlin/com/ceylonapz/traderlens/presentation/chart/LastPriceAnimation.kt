package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/*
* Animate 1 : Price slide - price = from + (to - from) × progress
* Animate 2 : Up/down flash - rising (price up|down)
* */
@Stable
class LastPriceAnimation {
    private val progress = Animatable(1f)
    private val flash = Animatable(0f)
    private var from by mutableLongStateOf(0L)
    private var to by mutableLongStateOf(0L)
    private var openTime: Long? = null

    var rising by mutableStateOf(true)
        private set

    val price: PriceCents get() = PriceCents(from + ((to - from) * progress.value).roundToLong())

    val flashAlpha: Float get() = flash.value

    suspend fun update(candle: Candle) = coroutineScope {
        val target = candle.close.value
        val previous = to
        val hasPrevious = openTime != null
        // A new candle opens at its own price, so only slide within the same candle.
        from = if (candle.openTimeMillis == openTime) price.value else target
        to = target
        openTime = candle.openTimeMillis

        launch {
            //price animate
            progress.snapTo(0f)
            progress.animateTo(1f, tween(SLIDE_MILLIS))
        }
        if (hasPrevious && target != previous) {
            //flash animate
            rising = target > previous
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(FLASH_MILLIS))
            }
        }
    }

    private companion object {
        const val SLIDE_MILLIS = 300
        const val FLASH_MILLIS = 600
    }
}
