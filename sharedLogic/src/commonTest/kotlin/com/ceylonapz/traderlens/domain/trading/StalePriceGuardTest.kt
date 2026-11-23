package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class StalePriceGuardTest {

    private val timeSource = TestTimeSource()
    private val guard = StalePriceGuard(maxAge = 10.seconds)
    private val price = LivePrice(PriceCents(9_000_000), timeSource.markNow())

    @Test
    fun freshPriceWhileConnectedIsAllowed() {
        timeSource += 2.seconds
        assertNull(guard.check(ConnectionState.Connected, price))
    }

    @Test
    fun priceExactlyAtMaxAgeIsAllowed() {
        timeSource += 10.seconds
        assertNull(guard.check(ConnectionState.Connected, price))
    }

    @Test
    fun priceOlderThanMaxAgeIsStale() {
        timeSource += 10.seconds + 1.milliseconds
        assertEquals(TradeRejection.STALE_PRICE, guard.check(ConnectionState.Connected, price))
    }

    @Test
    fun missingPriceIsStale() {
        assertEquals(TradeRejection.STALE_PRICE, guard.check(ConnectionState.Connected, null))
    }

    @Test
    fun anyStateOtherThanConnectedIsBlockedEvenWithFreshPrice() {
        assertEquals(TradeRejection.DISCONNECTED, guard.check(ConnectionState.Reconnecting, price))
        assertEquals(TradeRejection.DISCONNECTED, guard.check(ConnectionState.Offline, price))
    }
}
