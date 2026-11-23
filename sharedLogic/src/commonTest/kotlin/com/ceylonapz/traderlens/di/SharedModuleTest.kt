package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.data.repository.MarketRepositoryImpl
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs

class SharedModuleTest {

    @Test
    fun resolvesMarketRepository() {
        val koin = koinApplication { modules(sharedModule(enableLogging = false)) }.koin

        assertIs<MarketRepositoryImpl>(koin.get<MarketRepository>())
    }
}
