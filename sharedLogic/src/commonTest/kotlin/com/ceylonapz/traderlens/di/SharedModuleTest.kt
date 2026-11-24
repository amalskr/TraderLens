package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.data.local.CandleCache
import com.ceylonapz.traderlens.data.local.FakeCandleCache
import com.ceylonapz.traderlens.data.repository.MarketRepositoryImpl
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertIs

class SharedModuleTest {

    @Test
    fun resolvesMarketRepository() {
        val koin = koinApplication { modules(sharedModule(enableLogging = false), module { single<CandleCache> { FakeCandleCache() } }) }.koin

        assertIs<MarketRepositoryImpl>(koin.get<MarketRepository>())
    }
}
