package com.ceylonapz.traderlens.domain.model.money

// Rounds down so a buy never spends more than [usd]; null on invalid input or overflow.
//$45,000.00	4_500_000 -> 50_000_000	0.5 BTC
fun usdToSatoshis(usd: UsdCents, price: PriceCents): Satoshis? {
    if (usd.value < 0 || price.value <= 0) return null
    return mulDivFloorOrNull(usd.value, Satoshis.PER_BTC, price.value)?.let(::Satoshis)
}

// Rounds down so a sell never credits more than [sats] is worth; null on invalid input or overflow.
fun satoshisToUsd(sats: Satoshis, price: PriceCents): UsdCents? {
    if (sats.value < 0 || price.value <= 0) return null
    return mulDivFloorOrNull(sats.value, price.value, Satoshis.PER_BTC)?.let(::UsdCents)
}
