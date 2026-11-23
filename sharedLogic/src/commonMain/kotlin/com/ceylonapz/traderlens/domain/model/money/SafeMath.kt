package com.ceylonapz.traderlens.domain.model.money

// Silent overflow would corrupt balances: plus/minus throw, conversions return null.

// Sign-bit overflow checks (Hacker's Delight).
internal fun addExact(a: Long, b: Long): Long {
    val result = a + b
    if ((a xor result) and (b xor result) < 0) throw ArithmeticException("Long overflow: $a + $b")
    return result
}

internal fun subtractExact(a: Long, b: Long): Long {
    val result = a - b
    if ((a xor b) and (a xor result) < 0) throw ArithmeticException("Long overflow: $a - $b")
    return result
}

// The OrNull helpers assume non-negative inputs.
internal fun multiplyOrNull(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    if (a > Long.MAX_VALUE / b) return null
    return a * b
}

internal fun addOrNull(a: Long, b: Long): Long? {
    if (a > Long.MAX_VALUE - b) return null
    return a + b
}

/**
 * floor(a * b / c) computed as q * b + floor(r * b / c) with a = q * c + r,
 * so the intermediate product is r * b instead of a * b.
 */
internal fun mulDivFloorOrNull(a: Long, b: Long, c: Long): Long? {
    if (c <= 0) return null
    val q = a / c
    val r = a % c
    val whole = multiplyOrNull(q, b) ?: return null
    val partProduct = multiplyOrNull(r, b) ?: return null
    return addOrNull(whole, partProduct / c)
}
