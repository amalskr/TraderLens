package com.ceylonapz.traderlens.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ceylonapz.traderlens.R
import com.ceylonapz.traderlens.presentation.chart.ChartScreen
import com.ceylonapz.traderlens.presentation.history.HistoryScreen
import com.ceylonapz.traderlens.presentation.navigation.HistoryRoute
import com.ceylonapz.traderlens.presentation.navigation.TradeRoute

@Composable
fun TraderLensApp() {
    val backStack = rememberNavBackStack(TradeRoute)
    Scaffold(
        bottomBar = { BottomBar(current = backStack.last(), onSelect = backStack::selectTab) },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TradeRoute> { ChartScreen() }
                entry<HistoryRoute> { HistoryScreen() }
            },
        )
    }
}

// Trade is always the root, so back from History returns to it and the chart ViewModel stays alive.
private fun NavBackStack<NavKey>.selectTab(key: NavKey) {
    if (key == TradeRoute) {
        while (size > 1) removeAt(lastIndex)
    } else if (last() != key) {
        add(key)
    }
}

@Composable
private fun BottomBar(current: NavKey, onSelect: (NavKey) -> Unit) {
    NavigationBar {
        TabItem(TradeRoute, "Trade", R.drawable.ic_chart, current, onSelect)
        TabItem(HistoryRoute, "History", R.drawable.ic_history, current, onSelect)
    }
}

@Composable
private fun RowScope.TabItem(
    key: NavKey,
    label: String,
    @DrawableRes icon: Int,
    current: NavKey,
    onSelect: (NavKey) -> Unit,
) {
    NavigationBarItem(
        selected = current == key,
        onClick = { onSelect(key) },
        icon = { Icon(painterResource(icon), contentDescription = null) },
        label = { Text(label) },
    )
}
