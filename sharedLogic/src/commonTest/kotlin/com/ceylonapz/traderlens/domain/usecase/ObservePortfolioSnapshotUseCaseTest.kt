package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.fakes.FakeMarketRepository
import com.ceylonapz.traderlens.domain.fakes.FakeWalletRepository
import com.ceylonapz.traderlens.domain.model.Portfolio
import com.ceylonapz.traderlens.domain.model.PortfolioSnapshot
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObservePortfolioSnapshotUseCaseTest {

    private val wallet = Wallet(UsdCents(100_000), Satoshis(50_000_000))
    private val walletRepository = FakeWalletRepository(wallet)
    private val marketRepository = FakeMarketRepository()

    @Test
    fun valuesWalletAtCachedPriceAndFollowsChanges() = runTest {
        val emissions = mutableListOf<PortfolioSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObservePortfolioSnapshotUseCase(walletRepository, marketRepository)().toList(emissions)
        }

        marketRepository.lastKnownPrice.value = PriceSnapshot(PriceCents(9_000_000), 5_000)
        walletRepository.wallet.value = Wallet(UsdCents(0), Satoshis(100_000_000))

        assertEquals(
            listOf(
                PortfolioSnapshot(Portfolio(wallet, null, null), null),
                PortfolioSnapshot(Portfolio(wallet, PriceCents(9_000_000), UsdCents(4_600_000)), 5_000),
                PortfolioSnapshot(
                    Portfolio(Wallet(UsdCents(0), Satoshis(100_000_000)), PriceCents(9_000_000), UsdCents(9_000_000)),
                    5_000,
                ),
            ),
            emissions,
        )
    }
}
