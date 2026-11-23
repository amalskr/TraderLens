package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InitializeWalletUseCaseTest {

    @Test
    fun createsInitialWalletIfAbsent() = runTest {
        val created = mutableListOf<Wallet>()
        val repository = object : WalletRepository {
            override suspend fun createIfAbsent(wallet: Wallet) {
                created += wallet
            }

            override fun observeWallet(): Flow<Wallet> = emptyFlow()
        }

        InitializeWalletUseCase(repository)()

        assertEquals(listOf(Wallet.INITIAL), created)
    }
}
