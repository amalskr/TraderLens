package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.trading.StalePriceGuard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Why trading is blocked right now (disconnected or stale price),
 * or null when it is allowed. */
class ObserveTradingBlockUseCase(
    private val priceFeed: PriceFeed,
    private val priceGuard: StalePriceGuard = StalePriceGuard(),
    private val recheckInterval: Duration = 1.seconds,
) {

    //check current trade can run or not at this time (Binance connected or disconnect)
    /*
    * Connection/price change all time
    * → old loop cancel, then new loop
    * → every recheckInterval want to guard check
    * → if changed emit it
    *
    * */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<TradeRejection?> =
        combine(priceFeed.connectionState, priceFeed.latestPrice, ::Pair)
            .transformLatest { (connection, price) ->
                while (true) {
                    emit(priceGuard.check(connection, price))
                    delay(recheckInterval)
                }
            }
            .distinctUntilChanged()
}
