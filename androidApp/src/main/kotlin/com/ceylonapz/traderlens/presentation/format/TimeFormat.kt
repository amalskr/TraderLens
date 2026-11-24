package com.ceylonapz.traderlens.presentation.format

fun formatAgo(elapsedMillis: Long): String {
    val seconds = elapsedMillis.coerceAtLeast(0) / 1_000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> plural(seconds / 60, "minute")
        seconds < 86_400 -> plural(seconds / 3_600, "hour")
        else -> plural(seconds / 86_400, "day")
    }
}

private fun plural(count: Long, unit: String) = "$count ${if (count == 1L) unit else unit + "s"} ago"
