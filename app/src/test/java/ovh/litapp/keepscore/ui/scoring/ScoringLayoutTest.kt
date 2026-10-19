package ovh.litapp.keepscore.ui.scoring

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the responsive rules of the scoreboard: which layout an orientation gets, how the
 * players are distributed over the rows, and how many history lines a column of a given height
 * holds.
 */
class ScoringLayoutTest {

    @Test
    fun aLandscapeConfigurationGetsTheGrid() {
        assertEquals(
            ScoringOrientation.LANDSCAPE,
            scoringOrientationFor(Configuration.ORIENTATION_LANDSCAPE, numPlayers = 8),
        )
    }

    @Test
    fun aPortraitOrUndefinedConfigurationStacksThePlayers() {
        assertEquals(
            ScoringOrientation.PORTRAIT,
            scoringOrientationFor(Configuration.ORIENTATION_PORTRAIT, numPlayers = 2),
        )
        assertEquals(
            ScoringOrientation.PORTRAIT,
            scoringOrientationFor(Configuration.ORIENTATION_UNDEFINED, numPlayers = 2),
        )
    }

    @Test
    fun landscapeKeepsEightPlayersInTheGridButStacksNineAndThirty() {
        assertEquals(ScoringOrientation.LANDSCAPE, scoringOrientationFor(Configuration.ORIENTATION_LANDSCAPE, 8))
        assertEquals(
            listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6, 7)),
            playerRows(8, scoringOrientationFor(Configuration.ORIENTATION_LANDSCAPE, 8)),
        )

        val ninePlayerOrientation = scoringOrientationFor(Configuration.ORIENTATION_LANDSCAPE, 9)
        assertEquals(ScoringOrientation.PORTRAIT, ninePlayerOrientation)
        assertEquals(9, playerRows(9, ninePlayerOrientation).size)
        assertEquals((0 until 9).toList(), playerRows(9, ninePlayerOrientation).flatten().filterNotNull())

        val thirtyPlayerOrientation = scoringOrientationFor(Configuration.ORIENTATION_LANDSCAPE, 30)
        assertEquals(ScoringOrientation.PORTRAIT, thirtyPlayerOrientation)
        assertEquals(30, playerRows(30, thirtyPlayerOrientation).size)
        assertEquals((0 until 30).toList(), playerRows(30, thirtyPlayerOrientation).flatten().filterNotNull())
    }

    @Test
    fun portraitGivesEveryPlayerARowOfTheirOwn() {
        val rows = playerRows(30, ScoringOrientation.PORTRAIT)

        assertEquals(30, rows.size)
        assertEquals(listOf(0), rows.first())
        assertEquals(listOf(29), rows.last())
        assertEquals((0 until 30).toList(), rows.flatten().filterNotNull())
    }

    @Test
    fun landscapeFillsTwoPlayersPerRow() {
        val rows = playerRows(8, ScoringOrientation.LANDSCAPE)

        assertEquals(
            listOf(
                listOf(0, 1),
                listOf(2, 3),
                listOf(4, 5),
                listOf(6, 7),
            ),
            rows,
        )
    }

    @Test
    fun landscapeLeavesTheOddCellOfAHalfFilledRowEmpty() {
        assertEquals(listOf(listOf(0, 1), listOf(2, null)), playerRows(3, ScoringOrientation.LANDSCAPE))
        assertEquals(listOf(listOf(0, null)), playerRows(1, ScoringOrientation.LANDSCAPE))
        assertEquals(
            listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6, null)),
            playerRows(7, ScoringOrientation.LANDSCAPE),
        )
    }

    @Test
    fun aGameWithoutPlayersHasNoRows() {
        assertEquals(emptyList<List<Int?>>(), playerRows(0, ScoringOrientation.PORTRAIT))
        assertEquals(emptyList<List<Int?>>(), playerRows(0, ScoringOrientation.LANDSCAPE))
    }

    @Test
    fun aColumnHoldsAsManyLinesAsItsHeightAllows() {
        assertEquals(5, maxHistoryLines(availableHeightPx = 100f, lineHeightPx = 20f))
        assertEquals(0, maxHistoryLines(availableHeightPx = 19f, lineHeightPx = 20f))
    }

    @Test
    fun aColumnWithoutRoomHoldsNoLines() {
        assertEquals(0, maxHistoryLines(availableHeightPx = 0f, lineHeightPx = 20f))
        assertEquals(0, maxHistoryLines(availableHeightPx = 100f, lineHeightPx = 0f))
    }
}
