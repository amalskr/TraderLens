package com.ceylonapz.traderlens.presentation.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ceylonapz.traderlens.domain.model.AmountUnit
import com.ceylonapz.traderlens.domain.model.TradeRejection
import com.ceylonapz.traderlens.domain.model.TradeResult
import com.ceylonapz.traderlens.domain.model.TradeSide
import com.ceylonapz.traderlens.domain.trading.OrderValidator
import com.ceylonapz.traderlens.presentation.format.formatBtc
import com.ceylonapz.traderlens.presentation.format.formatUsd

class TradePanelActions(
    val onSideChange: (TradeSide) -> Unit,
    val onUnitChange: (AmountUnit) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onMax: () -> Unit,
    val onReview: () -> Unit,
    val onDismissConfirm: () -> Unit,
    val onConfirm: () -> Unit,
)

@Composable
fun TradePanel(state: TradePanelState, actions: TradePanelActions, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(TradeSide.entries, state.side, { if (it == TradeSide.BUY) "Buy" else "Sell" }, actions.onSideChange)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = actions.onAmountChange,
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = state.blockedBy == null,
                    modifier = Modifier.weight(1f),
                )
                Choice(AmountUnit.entries, state.unit, { it.name }, actions.onUnitChange, Modifier.width(120.dp))
                TextButton(onClick = actions.onMax, enabled = state.blockedBy == null) { Text("Max") }
            }
            PreviewLine(state)
            Button(
                onClick = actions.onReview,
                enabled = state.preview is OrderPreview.Ready && !state.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.side == TradeSide.BUY) "Review buy" else "Review sell")
            }
            state.lastResult?.let { Text(it.label(), style = MaterialTheme.typography.bodyMedium) }
        }
    }
    val preview = state.preview
    if (state.confirming && preview is OrderPreview.Ready) ConfirmDialog(state, preview, actions)
}

@Composable
private fun <T> Choice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun PreviewLine(state: TradePanelState) {
    when (val preview = state.preview) {
        null -> Text(" ", style = MaterialTheme.typography.bodySmall)
        is OrderPreview.Ready -> Text(
            "≈ ${preview.btc.formatBtc()} for ${preview.usd.formatUsd()} at ${preview.price.formatUsd()}",
            style = MaterialTheme.typography.bodySmall,
        )
        is OrderPreview.Blocked -> Text(
            preview.reason.message(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ConfirmDialog(state: TradePanelState, preview: OrderPreview.Ready, actions: TradePanelActions) {
    val buy = state.side == TradeSide.BUY
    AlertDialog(
        onDismissRequest = actions.onDismissConfirm,
        title = { Text(if (buy) "Confirm buy" else "Confirm sell") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (buy) "Spend ${preview.usd.formatUsd()}" else "Sell ${preview.btc.formatBtc()}")
                Text(if (buy) "Receive ≈ ${preview.btc.formatBtc()}" else "Receive ≈ ${preview.usd.formatUsd()}")
                Text("Price ≈ ${preview.price.formatUsd()}")
                Text("Fills at the live price when you confirm.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = actions.onConfirm, enabled = !state.submitting) { Text(if (buy) "Buy" else "Sell") }
        },
        dismissButton = { TextButton(onClick = actions.onDismissConfirm) { Text("Cancel") } },
    )
}

private fun TradeResult.label(): String = when (this) {
    is TradeResult.Success -> {
        val verb = if (trade.side == TradeSide.BUY) "Bought" else "Sold"
        "#${trade.id} $verb ${trade.btc.formatBtc()} for ${trade.usd.formatUsd()} at ${trade.price.formatUsd()}"
    }
    is TradeResult.Rejected -> "Rejected: ${reason.message()}"
}

private fun TradeRejection.message(): String = when (this) {
    TradeRejection.INSUFFICIENT_FUNDS -> "Insufficient funds"
    TradeRejection.DISCONNECTED -> "Not connected"
    TradeRejection.STALE_PRICE -> "Waiting for a live price"
    TradeRejection.INVALID_PRICE -> "Invalid price"
    TradeRejection.INVALID_AMOUNT -> "Invalid amount"
    TradeRejection.BELOW_MINIMUM -> "Minimum order is ${OrderValidator.MINIMUM_ORDER_USD.formatUsd()}"
}
