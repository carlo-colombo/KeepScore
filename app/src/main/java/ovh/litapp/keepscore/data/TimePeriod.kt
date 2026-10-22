package ovh.litapp.keepscore.data

import java.util.Date

import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.util.CollectionUtil.Function
import ovh.litapp.keepscore.util.Functions

/**
 * Basic measures of time to use for organizing games in the LoadGameActivity,
 * e.g. "Today," "Yesterday," "Last," etc.
 *
 * @author nolan
 */
enum class TimePeriod(
    val titleResId: Int,
    val startDateFunction: Function<Date, Date>,
    val endDateFunction: Function<Date, Date>,
) {
    Today(R.string.title_today, Functions.TODAY_START, Functions.NOW),
    Yesterday(R.string.title_yesterday, Functions.YESTERDAY_START, Functions.TODAY_START),
    DayBeforeYesterday(
        R.string.title_two_days_ago,
        Functions.DAY_BEFORE_YESTERDAY_START,
        Functions.YESTERDAY_START,
    ),
    LastWeek(R.string.title_last_week, Functions.ONE_WEEK_AGO_START, Functions.DAY_BEFORE_YESTERDAY_START),
    LastMonth(R.string.title_last_month, Functions.ONE_MONTH_AGO_START, Functions.ONE_WEEK_AGO_START),
    LastYear(R.string.title_last_year, Functions.ONE_YEAR_AGO_START, Functions.ONE_MONTH_AGO_START),
    Older(R.string.title_older, Functions.THE_EPOCH, Functions.ONE_YEAR_AGO_START),
}
