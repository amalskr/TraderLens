package com.ceylonapz.traderlens.data.remote

import com.ceylonapz.traderlens.domain.model.Candle
import com.ceylonapz.traderlens.domain.model.money.PriceCents
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class KlineDtoTest {

    private val row = """
        [1795428840000,"61342.59000000","61399.17000000","61342.59000000","61390.00000000",
         "5.05580000",1795428899999,"310311.23481230",3947,"2.56609000","157487.73612850","0"]
    """.toJsonArray()

    private val dto = KlineDto(
        openTime = 1795428840000,
        open = "61342.59000000",
        high = "61399.17000000",
        low = "61342.59000000",
        close = "61390.00000000",
    )

    @Test
    fun mapsBinanceRowToDto() {
        assertEquals(dto, row.toKlineDto())
    }

    @Test
    fun malformedRowThrows() {
        assertFails { """[1795428840000,"61342.59"]""".toJsonArray().toKlineDto() }
        assertFails { """["notATime","1","1","1","1"]""".toJsonArray().toKlineDto() }
    }

    @Test
    fun mapsDtoToCandleInCents() {
        val expected = Candle(
            openTimeMillis = 1795428840000,
            open = PriceCents(6_134_259),
            high = PriceCents(6_139_917),
            low = PriceCents(6_134_259),
            close = PriceCents(6_139_000),
        )
        assertEquals(expected, dto.toCandle())
    }

    @Test
    fun invalidPriceMapsToNull() {
        assertNull(dto.copy(close = "abc").toCandle())
    }

    private fun String.toJsonArray(): JsonArray = Json.parseToJsonElement(this).jsonArray
}
