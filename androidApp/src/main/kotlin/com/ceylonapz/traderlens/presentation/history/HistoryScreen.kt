package com.ceylonapz.traderlens.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ceylonapz.traderlens.domain.model.Trade
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.presentation.format.formatBtc
import com.ceylonapz.traderlens.presentation.format.formatUsd
import com.ceylonapz.traderlens.presentation.theme.DownColor
import com.ceylonapz.traderlens.presentation.theme.UpColor
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
fun HistoryScreen(modifier: Modifier = Modifier, viewModel: HistoryViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState, modifier)
}

@Composable
private fun HistoryContent(uiState: HistoryUiState, modifier: Modifier = Modifier) {
    when {
        uiState.loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.trades.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No trades yet", style = MaterialTheme.typography.bodyLarge)
        }
        else -> LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(uiState.trades, key = { it.id }) { trade ->
                TradeRow(trade)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TradeRow(trade: Trade) {
    val (label, color) = when (trade.side) {
        TradeSide.BUY -> "Buy" to UpColor
        TradeSide.SELL -> "Sell" to DownColor
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(
                DateTimeFormat.format(Instant.ofEpochMilli(trade.timestampMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(trade.btc.formatBtc(), style = MaterialTheme.typography.bodyMedium)
            Text(trade.usd.formatUsd(), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "@ ${trade.price.formatUsd()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
