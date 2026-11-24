package com.ceylonapz.traderlens.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceylonapz.traderlens.domain.usecase.ObserveTradesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(observeTrades: ObserveTradesUseCase) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeTrades()
        .map { HistoryUiState(loading = false, trades = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
