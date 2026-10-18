package ovh.litapp.keepscore.util;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ovh.litapp.keepscore.util.CollectionUtil.Function;

/**
 * Tests for the date-bucketing functions in {@link Functions}, which produce the start of each
 * time period used to group saved games.
 */
public class FunctionsDateTest {

    // Saturday, March 24, 2012 around 7:30 pm GMT
    private static final long REFERENCE_TIME = 1332617241000L;

    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        // the date functions use the default time zone, so pin it to make the buckets predictable
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void referenceDateIsSaturdayMarch242012() {
        assertEquals("24 Mar 2012 19:27:21 GMT", formatInGmt(REFERENCE_TIME));
    }

    @Test
    public void nowIsTheReferenceDate() {
        assertEquals(REFERENCE_TIME, Functions.NOW.apply(referenceDate()).getTime());
    }

    @Test
    public void theEpochIsTheUnixEpoch() {
        assertEquals("01 Jan 1970 00:00:00 GMT", formatInGmt(Functions.THE_EPOCH.apply(referenceDate())
                .getTime()));
    }

    @Test
    public void dateBucketsStartAtTheMidnightBeforeTheReferenceDate() {
        assertBucket("24 Mar 2012 00:00:00 GMT", Functions.TODAY_START);
        assertBucket("23 Mar 2012 00:00:00 GMT", Functions.YESTERDAY_START);
        assertBucket("22 Mar 2012 00:00:00 GMT", Functions.DAY_BEFORE_YESTERDAY_START);
        assertBucket("17 Mar 2012 00:00:00 GMT", Functions.ONE_WEEK_AGO_START);
        assertBucket("23 Feb 2012 00:00:00 GMT", Functions.ONE_MONTH_AGO_START);
        // 2012 is a leap year, so a year earlier is March 25, not March 24
        assertBucket("25 Mar 2011 00:00:00 GMT", Functions.ONE_YEAR_AGO_START);
    }

    private void assertBucket(String expectedGmt, Function<Date, Date> function) {
        assertEquals(expectedGmt, formatInGmt(function.apply(referenceDate()).getTime()));
    }

    private Date referenceDate() {
        return new Date(REFERENCE_TIME);
    }

    private String formatInGmt(long time) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date(time));
    }
}
