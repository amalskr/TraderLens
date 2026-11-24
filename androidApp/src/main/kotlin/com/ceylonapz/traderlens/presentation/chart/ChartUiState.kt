package com.ceylonapz.traderlens.presentation.chart

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.Portfolio
import com.ceylonapz.traderlens.domain.model.RetryStatus
import com.ceylonapz.traderlens.presentation.trade.TradePanelState

data class ChartUiState(
    val portfolio: Portfolio? = null,
    val tradePanel: TradePanelState = TradePanelState(),
    val candles: List<Candle> = emptyList(),
    val connection: ConnectionState = ConnectionState.Offline,
    val retry: RetryStatus? = null,
    val lastUpdatedOpenTime: Long? = null,
    val lastAppendedOpenTime: Long? = null,
    val reconnects: Int = 0,
    val lastGapFill: GapFillStatus? = null,
)

sealed interface GapFillStatus {
    data class Filled(val added: Int) : GapFillStatus
    data object Failed : GapFillStatus
}
