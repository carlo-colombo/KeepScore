package ovh.litapp.keepscore.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [CSVUtil], which follows http://en.wikipedia.org/wiki/Comma-separated_values
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CSVUtilTest {

    @Test
    fun quotesValuesAndTerminatesLineWithCrLf() {
        assertEquals("\"Alice\",\"Bob\"\r\n", convert("Alice", "Bob"))
    }

    @Test
    fun nullAndEmptyValuesBecomeEmptyFields() {
        assertEquals("\"Alice\",,,\"Bob\"\r\n", convert("Alice", null, "", "Bob"))
    }

    @Test
    fun emptyLineForNoValues() {
        assertEquals("\r\n", CSVUtil.convertToLine(emptyList()))
    }

    @Test
    fun embeddedCommasStayInsideQuotes() {
        assertEquals("\"Smith, John\",\"1,234\"\r\n", convert("Smith, John", "1,234"))
    }

    @Test
    fun embeddedQuotesAreDoubled() {
        assertEquals("\"he said \"\"hi\"\"\"\r\n", convert("he said \"hi\""))
        assertEquals("\"\"\"\"\"\"\r\n", convert("\"\"")) // two quotes become four
    }

    @Test
    fun embeddedNewlinesStayInsideQuotes() {
        assertEquals("\"line one\nline two\"\r\n", convert("line one\nline two"))
    }

    @Test
    fun carriageReturnsAreStripped() {
        assertEquals("\"line one\nline two\"\r\n", convert("line one\r\nline two"))
        // a value consisting only of a carriage return keeps its quotes, but loses the character
        assertEquals("\"\"\r\n", convert("\r"))
    }

    private fun convert(vararg values: String?): String = CSVUtil.convertToLine(values.toList())
}
