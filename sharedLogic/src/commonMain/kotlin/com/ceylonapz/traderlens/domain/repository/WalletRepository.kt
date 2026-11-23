package com.ceylonapz.traderlens.domain.repository

import com.ceylonapz.traderlens.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    suspend fun createIfAbsent(wallet: Wallet)

    /** Emits only once the wallet exists. */
    fun observeWallet(): Flow<Wallet>
}
