package com.ceylonapz.traderlens.data.remote

import com.ceylonapz.traderlens.domain.model.KlineInterval
import kotlinx.coroutines.flow.Flow

/*
* After every connect or reconnect, the app now refetches the latest
* candles over REST and merges them in before applying live updates.
* That fills any gap from a disconnect.
* */

interface KlineStream {
    fun events(interval: KlineInterval): Flow<KlineStreamEvent>
}

sealed interface KlineStreamEvent {
    data object Connected : KlineStreamEvent
    data class Update(val kline: KlineDto) : KlineStreamEvent
    data class Disconnected(val failures: Int, val retryInMillis: Long) : KlineStreamEvent
}
