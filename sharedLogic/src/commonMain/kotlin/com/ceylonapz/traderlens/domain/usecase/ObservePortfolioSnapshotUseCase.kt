package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.Portfolio
import com.ceylonapz.traderlens.domain.model.PortfolioSnapshot
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Portfolio valued at the last cached price, so it works without a live connection. */
class ObservePortfolioSnapshotUseCase(
    private val walletRepository: WalletRepository,
    private val marketRepository: MarketRepository,
) {

    operator fun invoke(): Flow<PortfolioSnapshot> =
        combine(walletRepository.observeWallet(), marketRepository.observeLastKnownPrice()) { wallet, last ->
            val price = last?.price
            PortfolioSnapshot(Portfolio(wallet, price, price?.let(wallet::totalValue)), last?.updatedAtMillis)
        }
}
