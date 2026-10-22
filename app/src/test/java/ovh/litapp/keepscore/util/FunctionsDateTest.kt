package ovh.litapp.keepscore.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

import ovh.litapp.keepscore.util.CollectionUtil.Function

/**
 * Tests for the date-bucketing functions in [Functions], which produce the start of each
 * time period used to group saved games.
 */
class FunctionsDateTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        // the date functions use the default time zone, so pin it to make the buckets predictable
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun referenceDateIsSaturdayMarch242012() {
        assertEquals("24 Mar 2012 19:27:21 GMT", formatInGmt(REFERENCE_TIME))
    }

    @Test
    fun nowIsTheReferenceDate() {
        assertEquals(REFERENCE_TIME, Functions.NOW.apply(referenceDate()).time)
    }

    @Test
    fun theEpochIsTheUnixEpoch() {
        assertEquals("01 Jan 1970 00:00:00 GMT", formatInGmt(Functions.THE_EPOCH.apply(referenceDate()).time))
    }

    @Test
    fun dateBucketsStartAtTheMidnightBeforeTheReferenceDate() {
        assertBucket("24 Mar 2012 00:00:00 GMT", Functions.TODAY_START)
        assertBucket("23 Mar 2012 00:00:00 GMT", Functions.YESTERDAY_START)
        assertBucket("22 Mar 2012 00:00:00 GMT", Functions.DAY_BEFORE_YESTERDAY_START)
        assertBucket("17 Mar 2012 00:00:00 GMT", Functions.ONE_WEEK_AGO_START)
        assertBucket("23 Feb 2012 00:00:00 GMT", Functions.ONE_MONTH_AGO_START)
        // 2012 is a leap year, so a year earlier is March 25, not March 24
        assertBucket("25 Mar 2011 00:00:00 GMT", Functions.ONE_YEAR_AGO_START)
    }

    private fun assertBucket(expectedGmt: String, function: Function<Date, Date>) {
        assertEquals(expectedGmt, formatInGmt(function.apply(referenceDate()).time))
    }

    private fun referenceDate(): Date = Date(REFERENCE_TIME)

    private fun formatInGmt(time: Long): String {
        val format = SimpleDateFormat("dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        return format.format(Date(time))
    }

    private companion object {
        // Saturday, March 24, 2012 around 7:30 pm GMT
        private const val REFERENCE_TIME = 1332617241000L
    }
}
