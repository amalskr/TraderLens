package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.data.local.CandleCache
import com.ceylonapz.traderlens.data.local.SqlCandleCache
import com.ceylonapz.traderlens.data.local.TraderLensDatabase
import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.BinanceStream
import com.ceylonapz.traderlens.data.remote.KlineStream
import com.ceylonapz.traderlens.data.remote.createHttpClient
import com.ceylonapz.traderlens.data.repository.MarketRepositoryImpl
import com.ceylonapz.traderlens.data.repository.TradeRepositoryImpl
import com.ceylonapz.traderlens.data.repository.WalletRepositoryImpl
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import com.ceylonapz.traderlens.domain.trading.OrderValidator
import com.ceylonapz.traderlens.domain.trading.TradingEngine
import com.ceylonapz.traderlens.domain.usecase.ExecuteTradeUseCase
import com.ceylonapz.traderlens.domain.usecase.InitializeWalletUseCase
import com.ceylonapz.traderlens.domain.usecase.ObserveMarketUseCase
import com.ceylonapz.traderlens.domain.usecase.ObservePortfolioSnapshotUseCase
import com.ceylonapz.traderlens.domain.usecase.ObservePortfolioUseCase
import com.ceylonapz.traderlens.domain.usecase.ObserveTradesUseCase
import com.ceylonapz.traderlens.domain.usecase.ObserveTradingBlockUseCase
import com.ceylonapz.traderlens.domain.usecase.RefreshStalePriceUseCase
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

fun sharedModule(enableLogging: Boolean) = module {
    includes(platformModule)
    single { createHttpClient(enableLogging) }
    single { BinanceApi(get()) }
    single<KlineStream> { BinanceStream(get()) }
    single<CandleCache> { SqlCandleCache(get()) }
    single<MarketRepository> {
        MarketRepositoryImpl(get(), get(), get(), nowMillis = { Clock.System.now().toEpochMilliseconds() })
    } bind PriceFeed::class
    single { TraderLensDatabase(get()) }
    single<WalletRepository> { WalletRepositoryImpl(get()) }
    factory { InitializeWalletUseCase(get()) }
    factory { ExecuteTradeUseCase(get()) }
    factory { ObservePortfolioUseCase(get(), get()) }
    factory { ObserveTradesUseCase(get()) }
    factory { ObserveMarketUseCase(get()) }
    factory { ObserveTradingBlockUseCase(get()) }
    factory { ObservePortfolioSnapshotUseCase(get(), get()) }
    factory { RefreshStalePriceUseCase(get(), nowMillis = { Clock.System.now().toEpochMilliseconds() }) }
    single<TradeRepository> { TradeRepositoryImpl(get()) }
    // Single instance so its Mutex guards every caller.
    factory { OrderValidator() }
    single { TradingEngine(get(), get(), nowMillis = { Clock.System.now().toEpochMilliseconds() }) }
}
