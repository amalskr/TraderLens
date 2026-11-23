package com.ceylonapz.traderlens.presentation.format

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import java.util.Locale

// Integer maths only, so no Double rounding creeps into displayed balances.
fun UsdCents.formatUsd(): String = String.format(Locale.US, "%,d.%02d USD", value / 100, value % 100)

fun PriceCents.formatUsd(): String = UsdCents(value).formatUsd()

fun Satoshis.formatBtc(): String =
    String.format(Locale.US, "%d.%08d BTC", value / Satoshis.PER_BTC, value % Satoshis.PER_BTC)
