package com.ceylonapz.traderlens.presentation.format

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import java.util.Locale
import kotlin.math.abs

// Integer maths only, so no Double rounding creeps into displayed balances.
fun UsdCents.formatUsd(): String = String.format(Locale.US, "%,d.%02d USD", value / 100, value % 100)

fun PriceCents.formatUsd(): String = UsdCents(value).formatUsd()

fun Satoshis.formatBtc(): String =
    String.format(Locale.US, "%d.%08d BTC", value / Satoshis.PER_BTC, value % Satoshis.PER_BTC)

// Unformatted values for editable text fields.
fun UsdCents.toPlainAmount(): String = String.format(Locale.US, "%d.%02d", value / 100, value % 100)

fun Satoshis.toPlainAmount(): String =
    String.format(Locale.US, "%d.%08d", value / Satoshis.PER_BTC, value % Satoshis.PER_BTC)

fun UsdCents.formatSignedUsd(): String = sign(value) + UsdCents(abs(value)).formatUsd()

fun formatBasisPointsPercent(bps: Long): String =
    sign(bps) + String.format(Locale.US, "%d.%02d%%", abs(bps) / 100, abs(bps) % 100)

private fun sign(value: Long): String = when {
    value > 0 -> "+"
    value < 0 -> "-"
    else -> ""
}
