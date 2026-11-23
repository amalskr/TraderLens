package com.ceylonapz.traderlens.domain.model

import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlin.time.TimeMark

// Monotonic mark, so a wall clock change can't make an old price look fresh.
data class LivePrice(val price: PriceCents, val receivedAt: TimeMark)
