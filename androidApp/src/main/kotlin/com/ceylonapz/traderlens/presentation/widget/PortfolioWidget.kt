package com.ceylonapz.traderlens.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ceylonapz.traderlens.MainActivity
import com.ceylonapz.traderlens.domain.model.PortfolioSnapshot
import com.ceylonapz.traderlens.domain.usecase.InitializeWalletUseCase
import com.ceylonapz.traderlens.domain.usecase.ObservePortfolioSnapshotUseCase
import com.ceylonapz.traderlens.domain.usecase.RefreshStalePriceUseCase
import com.ceylonapz.traderlens.presentation.format.formatBasisPointsPercent
import com.ceylonapz.traderlens.presentation.format.formatSignedUsd
import com.ceylonapz.traderlens.presentation.format.formatUsd
import com.ceylonapz.traderlens.presentation.theme.DownColor
import com.ceylonapz.traderlens.presentation.theme.UpColor
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

class PortfolioWidget : GlanceAppWidget(), KoinComponent {

    private val initializeWallet: InitializeWalletUseCase by inject()
    private val refreshStalePrice: RefreshStalePriceUseCase by inject()
    private val observeSnapshot: ObservePortfolioSnapshotUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        initializeWallet()
        // Glance gives each update a short window, so a slow network must not block showing the cached values.
        withTimeoutOrNull(REFRESH_TIMEOUT) { refreshStalePrice() }
        val snapshots = observeSnapshot()
        val initial = snapshots.first()
        provideContent {
            val snapshot by snapshots.collectAsState(initial)
            GlanceTheme { WidgetContent(snapshot) }
        }
    }

    private companion object {
        val REFRESH_TIMEOUT = 10.seconds
    }
}

@Composable
private fun WidgetContent(snapshot: PortfolioSnapshot) {
    val portfolio = snapshot.portfolio
    val secondary = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text("BTC/USD", style = secondary)
        Text(
            portfolio.price?.formatUsd() ?: "Price unavailable",
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text("Portfolio", style = secondary)
        Text(
            portfolio.totalValue?.formatUsd() ?: portfolio.wallet.usd.formatUsd(),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium),
        )
        ProfitLoss(snapshot)
        snapshot.priceUpdatedAtMillis?.let {
            Text("as of ${TimeFormat.format(Instant.ofEpochMilli(it))}", style = secondary)
        }
    }
}

@Composable
private fun ProfitLoss(snapshot: PortfolioSnapshot) {
    val pnl = snapshot.portfolio.profitLoss ?: return
    val bps = snapshot.portfolio.profitLossBasisPoints ?: return
    val color = when {
        pnl.value > 0 -> ColorProvider(UpColor)
        pnl.value < 0 -> ColorProvider(DownColor)
        else -> GlanceTheme.colors.onSurfaceVariant
    }
    Text("${pnl.formatSignedUsd()} (${formatBasisPointsPercent(bps)})", style = TextStyle(color = color, fontSize = 12.sp))
}
