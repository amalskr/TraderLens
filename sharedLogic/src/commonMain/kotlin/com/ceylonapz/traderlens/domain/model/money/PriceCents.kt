package com.ceylonapz.traderlens.domain.model.money

import kotlin.jvm.JvmInline

// USD cents per 1 BTC. Separate from UsdCents so amounts and prices cannot be swapped.
@JvmInline
value class PriceCents(val value: Long) : Comparable<PriceCents> {

    operator fun plus(other: PriceCents): PriceCents = PriceCents(addExact(value, other.value))

    operator fun minus(other: PriceCents): PriceCents = PriceCents(subtractExact(value, other.value))

    override fun compareTo(other: PriceCents): Int = value.compareTo(other.value)

    companion object {
        private const val CENT_DIGITS = 2

        // Parses "91234.56000000" without floating point, truncating past 2 decimals; null if invalid.
        fun parseOrNull(text: String): PriceCents? {
            val dot = text.indexOf('.')
            val wholePart = if (dot < 0) text else text.substring(0, dot)
            val fractionPart = if (dot < 0) "" else text.substring(dot + 1)

            if (wholePart.isEmpty() || !wholePart.isAllDigits()) return null
            if (dot >= 0 && (fractionPart.isEmpty() || !fractionPart.isAllDigits())) return null

            var dollars = 0L
            for (c in wholePart) {
                val digit = (c - '0').toLong()
                if (dollars > (Long.MAX_VALUE - digit) / 10) return null
                dollars = dollars * 10 + digit
            }

            val centsFromDollars = multiplyOrNull(dollars, 100) ?: return null
            val extraCents = fractionPart.padEnd(CENT_DIGITS, '0').take(CENT_DIGITS).toLong()
            val total = addOrNull(centsFromDollars, extraCents) ?: return null
            return PriceCents(total)
        }

        private fun String.isAllDigits(): Boolean = all { it in '0'..'9' }
    }
}
