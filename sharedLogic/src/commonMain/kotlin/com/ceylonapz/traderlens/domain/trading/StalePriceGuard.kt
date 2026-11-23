package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.TradeRejection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
* Stop trade if not new price.
* 10:00:00 ට BTC = $90,000 (අන්තිම update එක)
* Network එක කැඩුණා
* 10:05:00 ට ඇත්ත market එකේ BTC = $88,000 (මිල වැටිලා)
* User ගේ screen එකේ තවම $90,000 පේනවා
* User Sell එබුවොත් → app එක $90,000 ට විකුණනවා
*
* solution : check
*  1. webSocket is connected or not
*  2. last price recived  before 10sec or not -? MrktRepoIMpl,_latestPrice
* */
class StalePriceGuard(private val maxAge: Duration = MAX_PRICE_AGE) {

    /** Why trading must be blocked right now, or null if the price is live. */
    fun check(connection: ConnectionState, price: LivePrice?): TradeRejection? = when {
        connection != ConnectionState.Connected -> TradeRejection.DISCONNECTED
        price == null || price.receivedAt.elapsedNow() > maxAge -> TradeRejection.STALE_PRICE
        else -> null
    }

    companion object {
        // Binance pushes 1m kline updates about every 2s, so 10s means several ticks were missed.
        val MAX_PRICE_AGE = 10.seconds
    }
}
