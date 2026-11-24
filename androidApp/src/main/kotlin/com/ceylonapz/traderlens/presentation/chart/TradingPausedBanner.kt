package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.TradeRejection

@Composable
fun TradingPausedBanner(reason: TradeRejection, connection: ConnectionState, modifier: Modifier = Modifier) {
    val cause = when {
        reason == TradeRejection.STALE_PRICE -> "Waiting for a live price."
        connection == ConnectionState.Reconnecting -> "Reconnecting to the market."
        else -> "You're offline."
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text("$cause Trading is paused.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}
