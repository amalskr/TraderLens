package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.LivePrice
import kotlinx.coroutines.flow.StateFlow

interface PriceFeed {
    /** Offline unless the live stream is being collected. */
    val connectionState: StateFlow<ConnectionState>

    /** Last price from a WebSocket tick; REST data never updates it. */
    val latestPrice: StateFlow<LivePrice?>
}
