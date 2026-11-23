package com.ceylonapz.traderlens.data.remote

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KlineEventDtoTest {

    private val message = """
        {"e":"kline","E":1795429061234,"s":"BTCUSDT","k":{"t":1795429020000,"T":1795429079999,
        "s":"BTCUSDT","i":"1m","f":100,"L":200,"o":"61422.79000000","c":"61388.00000000",
        "h":"61422.79000000","l":"61380.10000000","v":"3.10000000","n":101,"x":false,
        "q":"190000.00000000","V":"1.50000000","Q":"92000.00000000","B":"0"}}
    """.trimIndent()

    @Test
    fun parsesStreamMessageToKlineDto() {
        val dto = binanceJson.decodeFromString<KlineEventDto>(message).toKlineDto()

        val expected = KlineDto(
            openTime = 1795429020000,
            open = "61422.79000000",
            high = "61422.79000000",
            low = "61380.10000000",
            close = "61388.00000000",
        )
        assertEquals(expected, dto)
    }

    @Test
    fun messageWithoutKlineThrows() {
        assertFailsWith<SerializationException> {
            binanceJson.decodeFromString<KlineEventDto>("""{"result":null,"id":1}""")
        }
    }
}
