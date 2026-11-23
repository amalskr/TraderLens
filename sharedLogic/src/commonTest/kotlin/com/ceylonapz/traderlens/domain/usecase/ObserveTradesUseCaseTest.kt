package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.fakes.FakeTradeRepository
import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import com.ceylonapz.traderlens.domain.model.money.Satoshis
import com.ceylonapz.traderlens.domain.model.money.UsdCents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveTradesUseCaseTest {

    @Test
    fun emitsTradesFromRepository() = runTest {
        val repository = FakeTradeRepository(Wallet.INITIAL)
        val trade = Trade(1, TradeSide.BUY, PriceCents(9_000_000), UsdCents(90_000), Satoshis(1_000_000), 1)
        repository.trades.value = listOf(trade)

        assertEquals(listOf(trade), ObserveTradesUseCase(repository)().first())
    }
}
