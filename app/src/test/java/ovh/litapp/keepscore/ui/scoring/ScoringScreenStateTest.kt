package ovh.litapp.keepscore.ui.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor

/**
 * Tests for the scoreboard state: how a tap changes a score and its history, what undo and redo put
 * back, when the game needs saving, and what the round total strip and the highlight show.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScoringScreenStateTest {

    /** The preferences the scoreboard is built with, without going through the settings. */
    private class FakeScoringPreferences(
        override var updateDelayMs: Int = 10_000,
        override var initialScore: Int = 0,
    ) : ScoringPreferences

    private val preferences = FakeScoringPreferences()

    private fun player(name: String, number: Int, score: Long = 0) = PlayerScore().apply {
        this.name = name
        this.score = score
        playerNumber = number
        playerColor = PlayerColor.BUILT_INS[0]
        history = arrayListOf()
    }

    private fun stateOf(vararg players: PlayerScore) = ScoringScreenState(players.toList(), preferences)

    /** Sets how long a tap keeps batching into the same history entry, in seconds. */
    private fun setUpdateDelay(seconds: Int) {
        preferences.updateDelayMs = seconds * 1000
    }

    @Test
    fun anIncrementChangesTheScoreAndItsHistory() {
        val state = stateOf(player("a", 0))

        state.increment(0, 5)

        assertEquals(5L, state.players[0].score)
        assertEquals(listOf(5), state.players[0].history)
        assertTrue(state.canUndo())
        assertFalse(state.canRedo())
    }

    @Test
    fun anIncrementInsideTheUpdateDelayBatchesIntoTheOpenEntry() {
        setUpdateDelay(10)
        val state = stateOf(player("a", 0))

        state.increment(0, 5)
        state.increment(0, 5)

        assertEquals(10L, state.players[0].score)
        assertEquals(listOf(10), state.players[0].history)
    }

    @Test
    fun anIncrementAfterTheOpenEntryIsClosedStartsANewEntry() {
        setUpdateDelay(10)
        val state = stateOf(player("a", 0))

        state.increment(0, 5)
        state.confirm(0)
        state.increment(0, 5)

        assertEquals(10L, state.players[0].score)
        assertEquals(listOf(5, 5), state.players[0].history)
    }

    @Test
    fun undoingForgetsTheOpenEntryAndRedoingPutsItBack() {
        setUpdateDelay(10)
        val state = stateOf(player("a", 0))
        state.increment(0, 5)
        state.increment(0, 3)

        state.undoOrRedo(undo = true)

        assertEquals(0L, state.players[0].score)
        assertEquals(emptyList<Int>(), state.players[0].history)
        assertTrue(state.canRedo())

        state.undoOrRedo(undo = false)

        assertEquals(8L, state.players[0].score)
        assertEquals(listOf(8), state.players[0].history)
    }

    @Test
    fun confirmingClosesTheOpenEntryWithoutChangingTheScore() {
        setUpdateDelay(10)
        val state = stateOf(player("a", 0))
        state.increment(0, 5)

        state.confirm(0)

        assertEquals(0L, state.players[0].lastIncremented)
        assertEquals(5L, state.players[0].score)
        assertEquals(listOf(5), state.players[0].history)
    }

    @Test
    fun addingAZeroEntryKeepsTheScoreAndDeletingTheLastEntryTakesItBack() {
        val state = stateOf(player("a", 0, score = 7))

        state.addZero(0)

        assertEquals(7L, state.players[0].score)
        assertEquals(listOf(0), state.players[0].history)

        state.deleteLast(0)

        assertEquals(7L, state.players[0].score)
        assertEquals(emptyList<Int>(), state.players[0].history)
    }

    @Test
    fun deletingTheLastEntrySubtractsItFromTheScore() {
        val state = stateOf(player("a", 0))
        state.increment(0, 5)

        state.deleteLast(0)

        assertEquals(0L, state.players[0].score)
        assertEquals(emptyList<Int>(), state.players[0].history)
    }

    @Test
    fun aChangeMarksTheGameForSavingAndSavingClearsIt() {
        val state = stateOf(player("a", 0))
        assertFalse(state.shouldAutosave())

        state.increment(0, 5)
        assertTrue(state.shouldAutosave())

        state.markSaved()
        assertFalse(state.shouldAutosave())
    }

    @Test
    fun undoAndRedoDoNotMarkTheGameForSaving() {
        val state = stateOf(player("a", 0))
        state.increment(0, 5)
        state.markSaved()

        state.undoOrRedo(undo = true)

        assertFalse(state.shouldAutosave())
    }

    @Test
    fun anUnsavedGameWithAPlayerNameIsWorthSaving() {
        val state = stateOf(player("a", 0))

        state.markDirtyForUnsavedNewGame(gameId = -1)

        assertTrue(state.shouldAutosave())
    }

    @Test
    fun anAlreadySavedGameIsNotMarkedForSaving() {
        val state = stateOf(player("a", 0))

        state.markDirtyForUnsavedNewGame(gameId = 7)

        assertFalse(state.shouldAutosave())
    }

    @Test
    fun anUnsavedGameWithoutPlayerNamesIsNotMarkedForSaving() {
        val state = stateOf(player("", 0))

        state.markDirtyForUnsavedNewGame(gameId = -1)

        assertFalse(state.shouldAutosave())
    }

    @Test
    fun theHighlightFollowsTheScoreThatChangedLast() {
        val state = stateOf(player("a", 0), player("b", 1))
        assertEquals(-1, state.highlightedPlayerIndex())

        state.increment(1, 5)
        assertEquals(1, state.highlightedPlayerIndex())

        // the legacy screen left the highlight where it was when a change was undone
        state.undoOrRedo(undo = true)
        assertEquals(1, state.highlightedPlayerIndex())
    }

    @Test
    fun theRoundTotalAddsUpTheLongestHistory() {
        val state = stateOf(
            player("a", 0).apply {
                score = 3
                history = arrayListOf(Delta(0L, 1), Delta(0L, 2))
            },
            player("b", 1).apply {
                score = 5
                history = arrayListOf(Delta(0L, 5))
            },
        )

        val roundTotal = state.roundTotal()

        assertEquals(2, roundTotal.round)
        // the second player has no entry for the current round, so counts as zero
        assertEquals(2, roundTotal.total)
    }

    @Test
    fun aRoundWithoutHistoryHasNoTotal() {
        val roundTotal = stateOf(player("a", 0), player("b", 1)).roundTotal()

        assertEquals(0, roundTotal.round)
        assertEquals(0, roundTotal.total)
    }

    @Test
    fun aRematchPutsEveryPlayerBackToTheInitialScoreAndForgetsTheUndoStack() {
        val state = stateOf(player("a", 0, score = 12), player("b", 1, score = 4))
        state.increment(0, 5)

        state.resetForRematch()

        assertEquals(0L, state.players[0].score)
        assertEquals(0L, state.players[1].score)
        assertEquals(emptyList<Int>(), state.players[0].history)
        assertEquals(0L, state.players[0].lastIncremented)
        assertFalse(state.canUndo())
        assertFalse(state.canRedo())
        assertTrue(state.shouldAutosave())
    }

    @Test
    fun aRematchStartsFromTheConfiguredInitialScore() {
        preferences.initialScore = 20
        val state = stateOf(player("a", 0, score = 12))

        state.resetForRematch()

        assertEquals(20L, state.players[0].score)
    }

    @Test
    fun resumingReReadsTheUpdateDelay() {
        setUpdateDelay(10)
        val state = stateOf(player("a", 0))
        assertEquals(10_000, state.updateDelayMs)

        setUpdateDelay(0)
        state.refreshPreferences()

        assertEquals(0, state.updateDelayMs)
    }
}
