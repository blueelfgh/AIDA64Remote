package com.example.aida64remote.data

import com.example.aida64remote.model.SensorItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorParserTest {
    @Test
    fun parseRealSseSample() {
        val line =
            "data: Page0|{|}Simple3|49°{|}SIV4|5487{|}Bar4p|100|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV5|10{|}Gph24p|0|{|}Simple26|温度: 30°C{|}"
        val sensors = SensorParser.parse(line)
        val map = SensorParser.toMap(sensors)
        assertEquals("49°", map["Simple3"])
        assertEquals("5487", map["SIV4"])
        assertEquals("100", map["Bar4p"])
        assertEquals("10", map["SIV5"])
        assertEquals("0", map["Gph24p"])
        assertEquals("温度: 30°C", map["Simple26"])
        assertTrue(sensors.none { it.id.startsWith("Page") })
        assertEquals(
            SensorItem("Simple3", "49°"),
            sensors.first { it.id == "Simple3" },
        )
    }
}
