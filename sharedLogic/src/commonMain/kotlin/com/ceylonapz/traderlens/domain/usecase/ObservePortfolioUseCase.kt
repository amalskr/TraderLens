package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.Portfolio
import com.ceylonapz.traderlens.domain.repository.PriceFeed
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePortfolioUseCase(
    private val walletRepository: WalletRepository,
    private val priceFeed: PriceFeed,
) {

    operator fun invoke(): Flow<Portfolio> =
        combine(walletRepository.observeWallet(), priceFeed.latestPrice) { wallet, livePrice ->
            val price = livePrice?.price
            Portfolio(wallet, price, price?.let(wallet::totalValue))
        }
}
