package com.ceylonapz.traderlens.domain.model

/*
* A live update for the current candle replaces the last one
* A new candle is added at the end
* It returns a new list and leaves the original unchanged,
* help for auto reconnect as well
* */

fun List<Candle>.merge(candle: Candle): List<Candle> {
    val index = binarySearch { it.openTimeMillis.compareTo(candle.openTimeMillis) }
    return toMutableList().apply {
        if (index >= 0) set(index, candle) else add(-index - 1, candle)
    }
}
