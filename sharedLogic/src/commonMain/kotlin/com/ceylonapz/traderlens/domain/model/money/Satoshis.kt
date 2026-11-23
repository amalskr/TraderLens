package com.ceylonapz.traderlens.domain.model.money

import kotlin.jvm.JvmInline

@JvmInline
value class Satoshis(val value: Long) : Comparable<Satoshis> {

    operator fun plus(other: Satoshis): Satoshis = Satoshis(addExact(value, other.value))

    operator fun minus(other: Satoshis): Satoshis = Satoshis(subtractExact(value, other.value))

    override fun compareTo(other: Satoshis): Int = value.compareTo(other.value)

    companion object {
        const val PER_BTC: Long = 100_000_000L
        val ZERO = Satoshis(0)
    }
}
