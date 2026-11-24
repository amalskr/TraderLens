package com.ceylonapz.traderlens.presentation.widget

import android.content.Context
import android.os.SystemClock
import androidx.glance.appwidget.updateAll
import com.ceylonapz.traderlens.domain.model.Wallet
import com.ceylonapz.traderlens.domain.usecase.ObservePortfolioSnapshotUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Pushes trades to the widget at once and live prices at most once a minute. */
class WidgetRefresher(
    private val context: Context,
    private val observeSnapshot: ObservePortfolioSnapshotUseCase,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            var lastWallet: Wallet? = null
            var lastPushAt = 0L
            observeSnapshot().collect { snapshot ->
                val now = SystemClock.elapsedRealtime()
                if (snapshot.portfolio.wallet != lastWallet || now - lastPushAt >= PRICE_PUSH_INTERVAL_MILLIS) {
                    lastWallet = snapshot.portfolio.wallet
                    lastPushAt = now
                    PortfolioWidget().updateAll(context)
                }
            }
        }
    }

    private companion object {
        const val PRICE_PUSH_INTERVAL_MILLIS = 60_000L
    }
}
