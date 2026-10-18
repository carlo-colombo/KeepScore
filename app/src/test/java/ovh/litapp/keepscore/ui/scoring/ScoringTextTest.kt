package ovh.litapp.keepscore.ui.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the text rules of a player's card: how the history is written and how big the score and
 * the name get.
 */
class ScoringTextTest {

    /** Every character is this wide, like a monospace font at a fixed text size. */
    private fun textWidth(numChars: Int): (Float) -> Float = { textSizePx -> textSizePx * 0.6f * numChars }

    @Test
    fun everyDeltaIsWrittenWithItsSign() {
        assertEquals("+3", signedValue(3))
        assertEquals("+0", signedValue(0))
        assertEquals("-4", signedValue(-4))
    }

    @Test
    fun theOpenEntryIsNotPartOfTheHistoryYet() {
        assertEquals(emptyList<Int>(), historyValuesToShow(listOf(5), modifiable = true))
        assertEquals(listOf(5, 3), historyValuesToShow(listOf(5, 3), modifiable = false))
        assertEquals(listOf(5), historyValuesToShow(listOf(5, 3), modifiable = true))
        assertEquals(emptyList<Int>(), historyValuesToShow(emptyList(), modifiable = false))
    }

    @Test
    fun theHistoryIsNewestFirstAndPaddedToTheWidestEntry() {
        val lines = historyLines(listOf(5, -10, 2), modifiable = false)

        assertEquals(listOf(2, -10, 5), lines.map { it.value })
        assertEquals(listOf(" +2", "-10", " +5"), lines.map { it.text })
    }

    @Test
    fun shortEntriesArePaddedToThreeCharacters() {
        assertEquals(" +1", historyLines(listOf(1), modifiable = false).single().text)
        assertEquals(" -1", historyLines(listOf(-1), modifiable = false).single().text)
        assertEquals(
            listOf(" -1", " +1"),
            historyLines(listOf(1, -1), modifiable = false).map { it.text },
        )
    }

    @Test
    fun aColumnThatFitsEverythingShowsEveryEntry() {
        val display = historyDisplay(listOf(1, 2, 3), modifiable = false, maxLines = 3)

        assertEquals(listOf(3, 2, 1), display.lines.map { it.value })
        assertFalse(display.truncated)
    }

    @Test
    fun aColumnThatCannotFitEverythingDropsTheOldestEntriesAndFadesTheLast() {
        val display = historyDisplay(listOf(1, 2, 3, 4), modifiable = false, maxLines = 2)

        assertEquals(listOf(4, 3), display.lines.map { it.value })
        assertTrue(display.truncated)
    }

    @Test
    fun aColumnWithoutRoomShowsNothing() {
        val display = historyDisplay(listOf(1, 2), modifiable = false, maxLines = 0)

        assertEquals(emptyList<HistoryLine>(), display.lines)
        assertFalse(display.truncated)
    }

    @Test
    fun textThatFitsKeepsTheMaximumSize() {
        assertEquals(48f, autosizeTextSize(48f, 100f, textWidth(numChars = 3)), 0f)
    }

    @Test
    fun textThatDoesNotFitShrinksInStepsOfTwoPixels() {
        // 48 * 2.4 = 115.2px is too wide, 42 * 2.4 = 100.8px still is, 40 * 2.4 = 96px is not
        assertEquals(40f, autosizeTextSize(48f, 100f, textWidth(numChars = 4)), 0f)
    }

    @Test
    fun textNeverShrinksBelowTheMinimumSize() {
        assertEquals(
            MIN_AUTOSIZE_TEXT_SIZE_PX,
            autosizeTextSize(48f, 1f, textWidth(numChars = 10)),
            0f,
        )
    }

    @Test
    fun anUnmeasuredBoxKeepsTheMaximumSize() {
        assertEquals(48f, autosizeTextSize(48f, 0f) { 1000f }, 0f)
    }
}
