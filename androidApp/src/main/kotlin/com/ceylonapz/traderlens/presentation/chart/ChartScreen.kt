package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.RetryStatus
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun ChartScreen(modifier: Modifier = Modifier, viewModel: ChartViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChartContent(uiState, modifier)
}

@Composable
private fun ChartContent(uiState: ChartUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        StreamStatus(uiState)
        Spacer(Modifier.size(16.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                uiState.candles.isNotEmpty() -> CandlestickChart(uiState.candles, Modifier.fillMaxSize())
                uiState.connection == ConnectionState.Offline -> Text("Couldn't load the chart")
                else -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun StreamStatus(uiState: ChartUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusRow("Connection") { ConnectionLabel(uiState.connection) }
            StatusRow("Live merge") { Text(liveMergeLabel(uiState)) }
            StatusRow("Retry") { RetryLabel(uiState.retry) }
            StatusRow("Reconnects") { Text(uiState.reconnects.toString()) }
            StatusRow("Gap fill") { Text(uiState.lastGapFill.label()) }
        }
    }
}

@Composable
private fun StatusRow(title: String, value: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(110.dp))
        value()
    }
}

@Composable
private fun ConnectionLabel(connection: ConnectionState) {
    val color = when (connection) {
        ConnectionState.Connected -> Color(0xFF26A69A)
        ConnectionState.Reconnecting -> Color(0xFFFFA726)
        ConnectionState.Offline -> Color(0xFFEF5350)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(connection.name)
    }
}

// Ticks hit the new candle within seconds, so "appended" is kept separately or it would never be visible.
@Composable
private fun RetryLabel(retry: RetryStatus?) {
    if (retry == null) {
        Text("Idle")
        return
    }
    val remainingMillis by produceState(retry.delayMillis, retry) {
        val endMillis = System.currentTimeMillis() + retry.delayMillis
        while (value > 0) {
            delay(100)
            value = (endMillis - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }
    val next = if (remainingMillis > 0) "next try in %.1fs".format(remainingMillis / 1000f) else "trying now"
    Text("Failed ${retry.failures}x · $next")
}

private fun liveMergeLabel(uiState: ChartUiState): String {
    val updated = uiState.lastUpdatedOpenTime ?: return "Waiting for first tick"
    val appended = uiState.lastAppendedOpenTime?.let(::formatTime) ?: "none yet"
    return "Updated ${formatTime(updated)} · Appended $appended"
}

private fun formatTime(millis: Long): String = TimeFormat.format(Instant.ofEpochMilli(millis))

private fun GapFillStatus?.label(): String = when (this) {
    null -> "Not run yet"
    is GapFillStatus.Filled -> "+$added ${if (added == 1) "candle" else "candles"} from REST"
    GapFillStatus.Failed -> "Failed, retries on next reconnect"
}
