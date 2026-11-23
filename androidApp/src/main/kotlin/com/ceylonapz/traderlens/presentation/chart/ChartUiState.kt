package com.ceylonapz.traderlens.presentation.chart

import com.ceylonapz.traderlens.domain.model.Candle

sealed interface ChartUiState {
    data object Loading : ChartUiState
    data class Success(val candles: List<Candle>) : ChartUiState
    data object Error : ChartUiState
}
