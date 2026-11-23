package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
* User ළඟ $10,000 සහ BTC 0 තියෙනවා. BTC මිල $90,000. User "$1,000 ට Buy" එබුවා:
* Lock the Engine
* calculate : $1,000 ÷ $90,000 = 0.01111111 BTC (round down).
* Check 1: Amount එක ඉතා කුඩාද? 0.00000000 BTC වගේ නම් → "Amount too small" කියලා reject.
* save in DB transaction
*  Wallet එක කියවනවා: $10,000
*  Check 2: සල්ලි ඇතිද? $10,000 ≥ $1,000 [ok] (නැත්නම් → "Insufficient funds", කිසිවක් වෙනස් වෙන්නේ නැහැ)
*  Wallet එක update: $9,000 සහ 0.01111111 BTC
*  Ledger එකට trade එක ලියනවා: "BUY 0.01111111 BTC at $90,000 for $1,000"
*UnLock Engine, ප්‍රතිඵලය return කරනවා: Success (trade එක එක්ක) හෝ Rejected (හේතුව එක්ක).
*
* when Sell එකත් එහෙමමයි, ප්‍රතිලෝමව
* "0.005 BTC Sell" → 0.005 × $90,000 = $450 → BTC අඩු වෙනවා, USD වැඩි වෙනවා.
* */
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
