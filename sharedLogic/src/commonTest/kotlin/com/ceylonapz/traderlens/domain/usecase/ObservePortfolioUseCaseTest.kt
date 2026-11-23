package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.fakes.FakePriceFeed
import com.ceylonapz.traderlens.domain.fakes.FakeWalletRepository
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.Portfolio
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
import kotlin.time.TimeSource

class ObservePortfolioUseCaseTest {

    private val wallet = Wallet(UsdCents(100_000), Satoshis(50_000_000))
    private val walletRepository = FakeWalletRepository(wallet)
    private val feed = FakePriceFeed()

    private fun live(cents: Long) = LivePrice(PriceCents(cents), TimeSource.Monotonic.markNow())

    @Test
    fun totalIsUnknownUntilLivePriceArrives() = runTest {
        val emissions = mutableListOf<Portfolio>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObservePortfolioUseCase(walletRepository, feed)().toList(emissions)
        }

        feed.latestPrice.value = live(9_000_000)

        assertEquals(
            listOf(
                Portfolio(wallet, price = null, totalValue = null),
                Portfolio(wallet, PriceCents(9_000_000), UsdCents(4_600_000)),
            ),
            emissions,
        )
    }

    @Test
    fun totalFollowsPriceAndWalletChanges() = runTest {
        feed.latestPrice.value = live(9_000_000)
        val emissions = mutableListOf<Portfolio>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObservePortfolioUseCase(walletRepository, feed)().toList(emissions)
        }

        feed.latestPrice.value = live(10_000_000)
        walletRepository.wallet.value = Wallet(UsdCents(0), Satoshis(100_000_000))

        assertEquals(
            listOf(UsdCents(4_600_000), UsdCents(5_100_000), UsdCents(10_000_000)),
            emissions.map { it.totalValue },
        )
    }
}
