package ovh.litapp.keepscore.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link CSVUtil}, which follows http://en.wikipedia.org/wiki/Comma-separated_values
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CSVUtilTest {

    @Test
    public void quotesValuesAndTerminatesLineWithCrLf() {
        assertEquals("\"Alice\",\"Bob\"\r\n", convert("Alice", "Bob"));
    }

    @Test
    public void nullAndEmptyValuesBecomeEmptyFields() {
        assertEquals("\"Alice\",,,\"Bob\"\r\n", convert("Alice", null, "", "Bob"));
    }

    @Test
    public void emptyLineForNoValues() {
        assertEquals("\r\n", CSVUtil.convertToLine(Collections.<String>emptyList()));
    }

    @Test
    public void embeddedCommasStayInsideQuotes() {
        assertEquals("\"Smith, John\",\"1,234\"\r\n", convert("Smith, John", "1,234"));
    }

    @Test
    public void embeddedQuotesAreDoubled() {
        assertEquals("\"he said \"\"hi\"\"\"\r\n", convert("he said \"hi\""));
        assertEquals("\"\"\"\"\"\"\r\n", convert("\"\"")); // two quotes become four
    }

    @Test
    public void embeddedNewlinesStayInsideQuotes() {
        assertEquals("\"line one\nline two\"\r\n", convert("line one\nline two"));
    }

    @Test
    public void carriageReturnsAreStripped() {
        assertEquals("\"line one\nline two\"\r\n", convert("line one\r\nline two"));
        // a value consisting only of a carriage return keeps its quotes, but loses the character
        assertEquals("\"\"\r\n", convert("\r"));
    }

    private String convert(String... values) {
        List<String> valueList = Arrays.asList(values);
        return CSVUtil.convertToLine(valueList);
    }
}
