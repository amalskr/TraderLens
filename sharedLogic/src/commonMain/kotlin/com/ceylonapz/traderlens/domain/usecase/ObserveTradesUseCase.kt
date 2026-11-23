package com.ceylonapz.traderlens.domain.usecase

import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.repository.TradeRepository
import kotlinx.coroutines.flow.Flow

class ObserveTradesUseCase(private val tradeRepository: TradeRepository) {

    operator fun invoke(): Flow<List<Trade>> = tradeRepository.observeTrades()
}
