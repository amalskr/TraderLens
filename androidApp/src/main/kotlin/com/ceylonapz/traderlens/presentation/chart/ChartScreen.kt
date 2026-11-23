package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChartScreen(modifier: Modifier = Modifier, viewModel: ChartViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChartContent(uiState, modifier)
}

@Composable
private fun ChartContent(uiState: ChartUiState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            is ChartUiState.Loading -> CircularProgressIndicator()
            is ChartUiState.Success -> CandlestickChart(
                candles = uiState.candles,
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
            is ChartUiState.Error -> Text("Couldn't load the chart")
        }
    }
}
