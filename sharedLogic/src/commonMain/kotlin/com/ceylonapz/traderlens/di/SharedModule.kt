package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.data.local.TraderLensDatabase
import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.BinanceStream
import com.ceylonapz.traderlens.data.remote.KlineStream
import com.ceylonapz.traderlens.data.remote.createHttpClient
import com.ceylonapz.traderlens.data.repository.MarketRepositoryImpl
import com.ceylonapz.traderlens.data.repository.WalletRepositoryImpl
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import com.ceylonapz.traderlens.domain.usecase.InitializeWalletUseCase
import org.koin.dsl.module

fun sharedModule(enableLogging: Boolean) = module {
    includes(platformModule)
    single { createHttpClient(enableLogging) }
    single { BinanceApi(get()) }
    single<KlineStream> { BinanceStream(get()) }
    single<MarketRepository> { MarketRepositoryImpl(get(), get()) }
    single { TraderLensDatabase(get()) }
    single<WalletRepository> { WalletRepositoryImpl(get()) }
    factory { InitializeWalletUseCase(get()) }
}
