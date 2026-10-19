package ovh.litapp.keepscore.ui.main

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.data.TimePeriod
import ovh.litapp.keepscore.db.GameSummary

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainGameGroupingTest {
    private var originalTimeZone: TimeZone? = null

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun groupsRecentGamesInRecentPeriodOrderAndSortsWithinEachPeriod() {
        val today = midnight(2025, Calendar.MARCH, 10) + 60_000
        val yesterday = midnight(2025, Calendar.MARCH, 9) + 60_000
        val earlierToday = game(1, today)
        val laterToday = game(2, today + 1_000)
        val yesterdayGame = game(3, yesterday)
        val oldGame = game(4, 0)

        val sections = organizeGamesByTimePeriod(
            listOf(earlierToday, oldGame, yesterdayGame, laterToday),
            Date(midnight(2025, Calendar.MARCH, 10) + 12 * 60 * 60 * 1000L),
        )

        assertEquals(listOf(TimePeriod.Today, TimePeriod.Yesterday, TimePeriod.Older), sections.map { it.first })
        assertSame(laterToday, sections[0].second[0])
        assertSame(earlierToday, sections[0].second[1])
        assertSame(yesterdayGame, sections[1].second[0])
        assertSame(oldGame, sections[2].second[0])
    }

    private fun game(id: Int, savedAt: Long) = GameSummary().apply {
        this.id = id
        dateSaved = savedAt
    }

    private fun midnight(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("GMT")).apply {
            clear()
            set(year, month, day)
        }.timeInMillis
}
