package ovh.litapp.keepscore.util

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import ovh.litapp.keepscore.util.CollectionUtil.Function

/**
 * Tests for [StringUtil]. Robolectric is needed because several helpers delegate to `android.text`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringUtilTest {

    @Test
    fun isEmptyOrWhitespaceTreatsNullAndBlankAsEmpty() {
        assertTrue(StringUtil.isEmptyOrWhitespace(null))
        assertTrue(StringUtil.isEmptyOrWhitespace(""))
        assertTrue(StringUtil.isEmptyOrWhitespace(" \t\n"))
        assertFalse(StringUtil.isEmptyOrWhitespace(" a "))
    }

    @Test
    fun splitByCharKeepsEmptyFields() {
        assertEquals(listOf("1", "", "3"), StringUtil.split("1,,3", ','))
        assertEquals(listOf("1", "2"), StringUtil.split("1,2", ','))
        assertEquals(listOf("1", "", ""), StringUtil.split("1,,", ','))
    }

    @Test
    fun splitByStringUsesTheWholeDelimiter() {
        assertEquals(listOf("a", "b", "c"), StringUtil.split("a::b::c", "::"))
        assertEquals(emptyList<String>(), StringUtil.split("", "::"))
        assertEquals(emptyList<String>(), StringUtil.split(null, ','))
    }

    @Test
    fun nullToEmptyAndEmptyToNullRoundTrip() {
        assertEquals("", StringUtil.nullToEmpty(null))
        assertEquals("abc", StringUtil.nullToEmpty("abc"))
        assertNull(StringUtil.emptyToNull(""))
        assertNull(StringUtil.emptyToNull(null))
        assertEquals("abc", StringUtil.emptyToNull("abc"))
    }

    @Test
    fun getNthIndexOfCountsOccurrences() {
        assertEquals(3, StringUtil.getNthIndexOf('a', "banana", 2))
        assertEquals(-1, StringUtil.getNthIndexOf('a', "banana", 4))
    }

    @Test
    fun padLeftPadsToTheRequestedLength() {
        assertEquals("0012", StringUtil.padLeft("12", '0', 4).toString())
        assertEquals("12", StringUtil.padLeft("12", '0', 2).toString())

        val builder = StringBuilder("12")
        assertTrue(StringUtil.padLeft(builder, '0', 4) === builder)
        assertEquals("0012", builder.toString())
    }

    @Test
    fun countCountsNonOverlappingOccurrences() {
        assertEquals(2, StringUtil.count("a,b,c", ","))
        assertEquals(0, StringUtil.count("abc", ","))
    }

    @Test
    fun lengthFunctionReadsTheLength() {
        val lengthFunction: Function<String, Int> = StringUtil.lengthFunction()

        assertEquals(3, lengthFunction.apply("abc"))
    }

    @Test
    fun joinSpannablesKeepsSpansWhenAnyInputIsSpanned() {
        val spanned = SpannableString("ab")
        spanned.setSpan(StyleSpan(Typeface.BOLD), 0, spanned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val joined = StringUtil.joinSpannables(" - ", "x", spanned)

        assertEquals("x - ab", joined.toString())
        assertTrue(joined is Spanned)
        assertEquals(1, (joined as Spanned).getSpans(0, joined.length, StyleSpan::class.java).size)
    }

    @Test
    fun joinSpannablesReturnsPlainTextWithoutSpannedInput() {
        assertEquals("a - b", StringUtil.joinSpannables(" - ", "a", "b").toString())
        assertEquals("only", StringUtil.joinSpannables(" - ", "only").toString())
        assertEquals("", StringUtil.joinSpannables(" - ").toString())
    }
}
