package com.ceylonapz.traderlens.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChartViewModel(private val marketRepository: MarketRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ChartUiState>(ChartUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = marketRepository.getCandles(KlineInterval.ONE_MINUTE, CANDLE_LIMIT).fold(
                onSuccess = { ChartUiState.Success(it) },
                onFailure = { ChartUiState.Error },
            )
        }
    }

    private companion object {
        const val CANDLE_LIMIT = 100
    }
}
