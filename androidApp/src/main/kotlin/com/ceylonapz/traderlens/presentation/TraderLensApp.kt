package com.ceylonapz.traderlens.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ceylonapz.traderlens.presentation.theme.TraderLensTheme

/**
 * Root composable. Navigation 3 (Trade and History screens) will be hosted here.
 */
@Composable
fun TraderLensApp() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "TraderLens",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview
@Composable
private fun TraderLensAppPreview() {
    TraderLensTheme {
        TraderLensApp()
    }
}
