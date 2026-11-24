package com.ceylonapz.traderlens.domain.model.money

import kotlin.jvm.JvmInline

// USD cents per 1 BTC. Separate from UsdCents so amounts and prices cannot be swapped.
@JvmInline
value class PriceCents(val value: Long) : Comparable<PriceCents> {

    operator fun plus(other: PriceCents): PriceCents = PriceCents(addExact(value, other.value))

    operator fun minus(other: PriceCents): PriceCents = PriceCents(subtractExact(value, other.value))

    override fun compareTo(other: PriceCents): Int = value.compareTo(other.value)

    companion object {
        // Parses "91234.56000000", truncating past 2 decimals; null if invalid.
        fun parseOrNull(text: String): PriceCents? = parseFixedPointOrNull(text, scale = 2)?.let(::PriceCents)
    }
}
