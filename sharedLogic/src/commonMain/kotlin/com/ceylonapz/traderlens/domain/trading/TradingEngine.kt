package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TradingEngine(
    private val tradeRepository: TradeRepository,
    private val priceFeed: PriceFeed,
    private val nowMillis: () -> Long,
    private val validator: OrderValidator = OrderValidator(),
    private val priceGuard: StalePriceGuard = StalePriceGuard(),
) {

    // Serializes trades so a double tap can't spend the same balance twice.
    private val mutex = Mutex()

    /** Fills [order] at the latest WebSocket price; never hits the network. */
    suspend fun entry(order: MarketOrder): TradeResult = mutex.withLock {
        // Read inside the lock: a queued tap must not fill at the price from when it was tapped.
        val livePrice = priceFeed.latestPrice.value
        priceGuard.check(priceFeed.connectionState.value, livePrice)?.let { return TradeResult.Rejected(it) }
        val price = livePrice?.price ?: return TradeResult.Rejected(TradeRejection.STALE_PRICE)

        val valid = when (val validation = validator.validate(order, price)) {
            is OrderValidation.Invalid -> return TradeResult.Rejected(validation.reason)
            is OrderValidation.Valid -> validation
        }
        val trade = tradeRepository.execute(valid.side, price, valid.usd, valid.btc, nowMillis())
            ?: return TradeResult.Rejected(TradeRejection.INSUFFICIENT_FUNDS)
        TradeResult.Success(trade)
    }
}
