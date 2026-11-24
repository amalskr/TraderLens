package com.ceylonapz.traderlens.presentation.portfolio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ceylonapz.traderlens.domain.model.Portfolio
import com.ceylonapz.traderlens.presentation.format.formatBtc
import com.ceylonapz.traderlens.presentation.format.formatUsd

@Composable
fun PortfolioCard(portfolio: Portfolio, modifier: Modifier = Modifier) {
    val wallet = portfolio.wallet
    val btcValue = portfolio.totalValue?.minus(wallet.usd)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Total value", style = MaterialTheme.typography.labelLarge)
            Text(portfolio.totalValue?.formatUsd() ?: "Waiting for price", style = MaterialTheme.typography.headlineSmall)
            BalanceRow("USD", wallet.usd.formatUsd())
            BalanceRow("BTC", wallet.btc.formatBtc() + (btcValue?.let { " ≈ ${it.formatUsd()}" } ?: ""))
        }
    }
}

@Composable
private fun BalanceRow(title: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
