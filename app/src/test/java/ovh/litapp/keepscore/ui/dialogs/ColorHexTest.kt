package ovh.litapp.keepscore.ui.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests for the hex value the custom colour picker shows and accepts. */
class ColorHexTest {

    @Test
    fun hexStringIgnoresTheAlphaChannel() {
        assertEquals("#112233", hexColorString(0xFF112233.toInt()))
        assertEquals("#000000", hexColorString(0x00000000))
        assertEquals("#0A0B0C", hexColorString(0xFF0A0B0C.toInt()))
    }

    @Test
    fun parsingAcceptsSixAndEightDigitHexValues() {
        assertEquals(0xFF112233.toInt(), parseHexColor("#112233"))
        assertEquals(0xFF112233.toInt(), parseHexColor("#FF112233"))
        assertEquals(0xFF112233.toInt(), parseHexColor(" #ff112233 "))
    }

    @Test
    fun parsingRejectsEverythingThatIsNotAHexColour() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("#"))
        assertNull(parseHexColor("112233"))
        assertNull(parseHexColor("#12345"))
        assertNull(parseHexColor("#1234567"))
        assertNull(parseHexColor("#GGGGGG"))
        assertNull(parseHexColor("#+12345"))
    }

    @Test
    fun aColourSurvivesTheRoundTripThroughItsHexString() {
        listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF33B5E5.toInt(), 0xFF7A5C00.toInt()).forEach { color ->
            assertEquals(color, parseHexColor(hexColorString(color)))
        }
    }
}
