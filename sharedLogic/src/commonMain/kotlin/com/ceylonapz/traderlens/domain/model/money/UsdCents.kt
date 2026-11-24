package com.ceylonapz.traderlens.domain.model.money

import kotlin.jvm.JvmInline

// USD amount in cents. $10,000.00 -> 1_000_000
@JvmInline
value class UsdCents(val value: Long) : Comparable<UsdCents> {

    operator fun plus(other: UsdCents): UsdCents = UsdCents(addExact(value, other.value))

    operator fun minus(other: UsdCents): UsdCents = UsdCents(subtractExact(value, other.value))

    override fun compareTo(other: UsdCents): Int = value.compareTo(other.value)

    companion object {
        val ZERO = UsdCents(0)

        fun parseOrNull(text: String): UsdCents? = parseFixedPointOrNull(text, scale = 2)?.let(::UsdCents)
    }
}
