package com.ceylonapz.traderlens.di

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
import com.ceylonapz.traderlens.domain.trading.TradingEngine
import com.ceylonapz.traderlens.domain.usecase.InitializeWalletUseCase
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

fun sharedModule(enableLogging: Boolean) = module {
    includes(platformModule)
    single { createHttpClient(enableLogging) }
    single { BinanceApi(get()) }
    single<KlineStream> { BinanceStream(get()) }
    single<MarketRepository> { MarketRepositoryImpl(get(), get()) } bind PriceFeed::class
    single { TraderLensDatabase(get()) }
    single<WalletRepository> { WalletRepositoryImpl(get()) }
    factory { InitializeWalletUseCase(get()) }
    single<TradeRepository> { TradeRepositoryImpl(get()) }
    // Single instance so its Mutex guards every caller.
    single { TradingEngine(get(), get(), nowMillis = { Clock.System.now().toEpochMilliseconds() }) }
}
