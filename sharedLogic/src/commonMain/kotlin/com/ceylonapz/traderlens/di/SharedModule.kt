package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.BinanceStream
import com.ceylonapz.traderlens.data.remote.createHttpClient
import com.ceylonapz.traderlens.data.repository.MarketRepositoryImpl
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import org.koin.dsl.module

fun sharedModule(enableLogging: Boolean) = module {
    single { createHttpClient(enableLogging) }
    single { BinanceApi(get()) }
    single { BinanceStream(get()) }
    single<MarketRepository> { MarketRepositoryImpl(get(), get()) }
}
