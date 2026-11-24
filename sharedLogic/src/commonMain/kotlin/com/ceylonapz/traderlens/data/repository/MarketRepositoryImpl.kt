package com.ceylonapz.traderlens.data.repository

import com.ceylonapz.traderlens.data.local.CandleCache
import com.ceylonapz.traderlens.data.remote.BinanceApi
import com.ceylonapz.traderlens.data.remote.KlineStream
import com.ceylonapz.traderlens.data.remote.KlineStreamEvent
import com.ceylonapz.traderlens.data.remote.toCandle
import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.CandleFeed
import com.ceylonapz.traderlens.domain.model.CandleFeedEvent
import com.ceylonapz.traderlens.domain.model.ConnectionState
import com.ceylonapz.traderlens.domain.model.KlineInterval
import com.ceylonapz.traderlens.domain.model.LivePrice
import com.ceylonapz.traderlens.domain.model.PriceSnapshot
import com.ceylonapz.traderlens.domain.model.RetryStatus
import com.ceylonapz.traderlens.domain.model.merge
import com.ceylonapz.traderlens.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

class MarketRepositoryImpl(
    private val api: BinanceApi,
    private val stream: KlineStream,
    private val cache: CandleCache,
    private val nowMillis: () -> Long,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : MarketRepository {

    private val _connectionState = MutableStateFlow(ConnectionState.Offline)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latestPrice = MutableStateFlow<LivePrice?>(null)
    override val latestPrice: StateFlow<LivePrice?> = _latestPrice.asStateFlow()

    private val _retryStatus = MutableStateFlow<RetryStatus?>(null)
    override val retryStatus: StateFlow<RetryStatus?> = _retryStatus.asStateFlow()

    //BinanceApi to load initial candles
    override suspend fun getCandles(interval: KlineInterval, limit: Int): Result<List<Candle>> =
        try {
            val candles = api.getKlines(interval, limit).map { dto ->
                dto.toCandle() ?: error("Invalid price in kline ${dto.openTime}")
            }
            Result.success(candles)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun observeLastKnownPrice(): Flow<PriceSnapshot?> = cache.observeLastPrice()

    override suspend fun refreshCandles(interval: KlineInterval, limit: Int): Boolean =
        getCandles(interval, limit).fold(
            onSuccess = { candles ->
                if (candles.isNotEmpty()) cache.save(interval, candles, candles.first().openTimeMillis, nowMillis())
                true
            },
            onFailure = { false },
        )

    //BinanceStream,
    override fun observeCandles(interval: KlineInterval, limit: Int): Flow<CandleFeed> = flow {
        var candles = emptyList<Candle>()
        var updatedAt: Long? = null
        _connectionState.value = ConnectionState.Reconnecting
        cache.load(interval, limit)?.let { cached ->
            candles = cached.candles
            updatedAt = cached.updatedAtMillis
            emit(CandleFeed(candles, CandleFeedEvent.Cached, updatedAt))
        }
        try {
            stream.events(interval).collect { event ->
                when (event) {
                    KlineStreamEvent.Connected -> { //when connect or reconnect
                        // A tick from before the gap must not be traded on after reconnecting.
                        _latestPrice.value = null
                        _connectionState.value = ConnectionState.Connected
                        _retryStatus.value = null
                        // A failed gap fill keeps the live stream going; the next reconnect retries it.
                        val fill = getCandles(interval, limit).fold(
                            onSuccess = { fetched ->
                                val known = candles.mapTo(HashSet()) { it.openTimeMillis }
                                candles = candles.merge(fetched).takeLast(limit)
                                val now = nowMillis()
                                updatedAt = now
                                if (candles.isNotEmpty()) cache.save(interval, candles, candles.first().openTimeMillis, now)
                                CandleFeedEvent.GapFill(added = fetched.count { it.openTimeMillis !in known })
                            },
                            onFailure = { CandleFeedEvent.GapFillFailed },
                        )
                        emit(CandleFeed(candles, fill, updatedAt))
                    }
                    is KlineStreamEvent.Update -> { // load every 2 sec max 200
                        val candle = event.kline.toCandle() ?: return@collect
                        _latestPrice.value = LivePrice(candle.close, timeSource.markNow())
                        candles = candles.merge(candle).takeLast(limit)
                        val now = nowMillis()
                        updatedAt = now
                        cache.save(interval, listOf(candle), candles.first().openTimeMillis, now)
                        emit(CandleFeed(candles, CandleFeedEvent.Live(candle), updatedAt))
                    }
                    is KlineStreamEvent.Disconnected -> {
                        _retryStatus.value = RetryStatus(event.failures, event.retryInMillis)
                        _connectionState.value =
                            if (event.failures < OFFLINE_AFTER_FAILURES) ConnectionState.Reconnecting else ConnectionState.Offline
                    }
                }
            }
        } finally {
            _connectionState.value = ConnectionState.Offline
            _retryStatus.value = null
        }
    }

    private companion object {
        // Backoff waits about 0.5s, 1s, 2s first, so a short blip shows Reconnecting, not Offline.
        const val OFFLINE_AFTER_FAILURES = 3
    }
}
