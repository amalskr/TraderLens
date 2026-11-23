package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.repository.WalletRepository

class InitializeWalletUseCase(private val walletRepository: WalletRepository) {

    suspend operator fun invoke() = walletRepository.createIfAbsent(Wallet.INITIAL)
}
