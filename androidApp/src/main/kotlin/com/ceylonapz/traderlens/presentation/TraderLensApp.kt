package com.ceylonapz.traderlens.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ceylonapz.traderlens.presentation.chart.ChartScreen

@Composable
fun TraderLensApp() {
    Scaffold { innerPadding ->
        ChartScreen(modifier = Modifier.padding(innerPadding))
    }
}
