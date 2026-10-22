package ovh.litapp.keepscore.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [TimeUtil].
 */
class TimeUtilTest {

    @Test
    fun formatsUnderOneMinuteAsMinutesAndSeconds() {
        assertEquals("0:00", TimeUtil.formatSeconds(0))
        assertEquals("0:05", TimeUtil.formatSeconds(5))
        assertEquals("0:59", TimeUtil.formatSeconds(59))
    }

    @Test
    fun formatsWholeMinutesWithoutAnHoursPart() {
        assertEquals("1:00", TimeUtil.formatSeconds(60))
        assertEquals("1:01", TimeUtil.formatSeconds(61))
        assertEquals("59:59", TimeUtil.formatSeconds(3599))
    }

    @Test
    fun formatsHoursWithPaddedMinutesAndSeconds() {
        assertEquals("1:00:00", TimeUtil.formatSeconds(3600))
        assertEquals("1:01:01", TimeUtil.formatSeconds(3661))
        assertEquals("25:01:01", TimeUtil.formatSeconds(90061))
    }
}
