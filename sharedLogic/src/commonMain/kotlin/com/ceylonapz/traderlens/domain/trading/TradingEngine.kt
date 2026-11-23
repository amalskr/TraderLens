package com.ceylonapz.traderlens.domain.trading

import com.ceylonapz.traderlens.domain.model.MarketOrder
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.satoshisToUsd
import com.ceylonapz.traderlens.domain.model.money.usdToSatoshis
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
    private val nowMillis: () -> Long,
) {

    // Serializes trades so a double tap can't spend the same balance twice.
    private val mutex = Mutex()

    /** Fills [order] at [price], which the caller takes from the live stream; never hits the network. */
    suspend fun entry(order: MarketOrder, price: PriceCents): TradeResult = mutex.withLock {
        val (side, usd, btc) = when (order) {
            is MarketOrder.Buy -> Triple(TradeSide.BUY, order.usd, usdToSatoshis(order.usd, price))
            is MarketOrder.Sell -> Triple(TradeSide.SELL, satoshisToUsd(order.btc, price), order.btc)
        }
        if (usd == null || btc == null || usd.value <= 0 || btc.value <= 0) {
            return TradeResult.Rejected(TradeRejection.AMOUNT_TOO_SMALL)
        }
        val trade = tradeRepository.execute(side, price, usd, btc, nowMillis())
            ?: return TradeResult.Rejected(TradeRejection.INSUFFICIENT_FUNDS)
        TradeResult.Success(trade)
    }
}
