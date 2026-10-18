package ovh.litapp.keepscore.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link TimeUtil}.
 */
public class TimeUtilTest {

    @Test
    public void formatsUnderOneMinuteAsMinutesAndSeconds() {
        assertEquals("0:00", TimeUtil.formatSeconds(0));
        assertEquals("0:05", TimeUtil.formatSeconds(5));
        assertEquals("0:59", TimeUtil.formatSeconds(59));
    }

    @Test
    public void formatsWholeMinutesWithoutAnHoursPart() {
        assertEquals("1:00", TimeUtil.formatSeconds(60));
        assertEquals("1:01", TimeUtil.formatSeconds(61));
        assertEquals("59:59", TimeUtil.formatSeconds(3599));
    }

    @Test
    public void formatsHoursWithPaddedMinutesAndSeconds() {
        assertEquals("1:00:00", TimeUtil.formatSeconds(3600));
        assertEquals("1:01:01", TimeUtil.formatSeconds(3661));
        assertEquals("25:01:01", TimeUtil.formatSeconds(90061));
    }
}
