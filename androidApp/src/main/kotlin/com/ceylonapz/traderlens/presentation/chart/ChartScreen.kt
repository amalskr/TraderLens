package com.ceylonapz.traderlens.presentation.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ceylonapz.traderlens.R
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.RetryStatus
import com.ceylonapz.traderlens.presentation.portfolio.PortfolioCard
import com.ceylonapz.traderlens.presentation.theme.DownColor
import com.ceylonapz.traderlens.presentation.theme.UpColor
import com.ceylonapz.traderlens.presentation.trade.TradePanel
import com.ceylonapz.traderlens.presentation.trade.TradePanelActions
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PORTRAIT_CHART_HEIGHT = 320.dp
private val FAB_SIZE = 56.dp
private val FAB_MARGIN = 16.dp

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun ChartScreen(modifier: Modifier = Modifier, viewModel: ChartViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tradeActions = remember(viewModel) {
        TradePanelActions(
            onSideChange = viewModel::onSideChange,
            onUnitChange = viewModel::onUnitChange,
            onAmountChange = viewModel::onAmountChange,
            onMax = viewModel::onMax,
            onReview = viewModel::onReview,
            onDismissConfirm = viewModel::onDismissConfirm,
            onConfirm = viewModel::onConfirm,
        )
    }
    ChartContent(
        uiState = uiState,
        tradeActions = tradeActions,
        onIntervalChange = viewModel::onIntervalChange,
        onOpenTradeSheet = viewModel::onOpenTradeSheet,
        onCloseTradeSheet = viewModel::onCloseTradeSheet,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartContent(
    uiState: ChartUiState,
    tradeActions: TradePanelActions,
    onIntervalChange: (KlineInterval) -> Unit,
    onOpenTradeSheet: () -> Unit,
    onCloseTradeSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Box(modifier.fillMaxSize()) {
        if (wide) { // for fordable screens
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Chart(uiState, onIntervalChange, Modifier.weight(1f).fillMaxHeight())
                // The row already pads the bottom by FAB_MARGIN, so the column stops right at the button's top edge.
                Column(Modifier.width(400.dp).fillMaxHeight().padding(bottom = FAB_SIZE).verticalScroll(rememberScrollState())) {
                    TradeSection(uiState)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = FAB_SIZE + FAB_MARGIN)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                TradeSection(uiState)
                Spacer(Modifier.size(16.dp))
                Chart(uiState, onIntervalChange, Modifier.fillMaxWidth().height(PORTRAIT_CHART_HEIGHT))
            }
        }
        FloatingActionButton(
            onClick = onOpenTradeSheet,
            modifier = Modifier.align(Alignment.BottomEnd).padding(FAB_MARGIN),
        ) {
            Icon(painterResource(R.drawable.ic_trade), contentDescription = "Trade")
        }
    }
    if (uiState.showTradeSheet) {
        ModalBottomSheet(onDismissRequest = onCloseTradeSheet) {
            TradePanel(
                uiState.tradePanel,
                tradeActions,
                Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun TradeSection(uiState: ChartUiState) {
    uiState.tradePanel.blockedBy?.let {
        TradingPausedBanner(it, uiState.connection, uiState.updatedAtMillis)
        Spacer(Modifier.size(12.dp))
    }
    uiState.portfolio?.let { PortfolioCard(it) }
    Spacer(Modifier.size(12.dp))
    StreamStatus(uiState)
}

@Composable
private fun Chart(uiState: ChartUiState, onIntervalChange: (KlineInterval) -> Unit, modifier: Modifier) {
    Column(modifier) {
        IntervalSelector(uiState.interval, onIntervalChange)
        Spacer(Modifier.size(8.dp))
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
private fun IntervalSelector(selected: KlineInterval, onSelect: (KlineInterval) -> Unit) {
    val options = KlineInterval.entries
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, interval ->
            SegmentedButton(
                selected = interval == selected,
                onClick = { onSelect(interval) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(interval.code) },
            )
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
        ConnectionState.Connected -> UpColor
        ConnectionState.Reconnecting -> Color(0xFFFFA726)
        ConnectionState.Offline -> DownColor
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
