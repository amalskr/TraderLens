package com.ceylonapz.traderlens.domain.model.money

// Parses "123.45" as an integer of 10^-scale units without floating point; extra decimals are truncated.
internal fun parseFixedPointOrNull(text: String, scale: Int): Long? {
    val dot = text.indexOf('.')
    val wholePart = if (dot < 0) text else text.substring(0, dot)
    val fractionPart = if (dot < 0) "" else text.substring(dot + 1)

    if (wholePart.isEmpty() || !wholePart.isAllDigits()) return null
    if (dot >= 0 && (fractionPart.isEmpty() || !fractionPart.isAllDigits())) return null

    var whole = 0L
    for (c in wholePart) {
        val digit = (c - '0').toLong()
        if (whole > (Long.MAX_VALUE - digit) / 10) return null
        whole = whole * 10 + digit
    }

    var unitsPerWhole = 1L
    repeat(scale) { unitsPerWhole *= 10 }
    val fromWhole = multiplyOrNull(whole, unitsPerWhole) ?: return null
    val fraction = fractionPart.padEnd(scale, '0').take(scale).ifEmpty { "0" }.toLong()
    return addOrNull(fromWhole, fraction)
}

private fun String.isAllDigits(): Boolean = all { it in '0'..'9' }
