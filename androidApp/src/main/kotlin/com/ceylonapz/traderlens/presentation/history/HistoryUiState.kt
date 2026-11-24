package com.ceylonapz.traderlens.presentation.history

import com.ceylonapz.traderlens.domain.model.Trade

data class HistoryUiState(
    val loading: Boolean = true,
    val trades: List<Trade> = emptyList(),
)
