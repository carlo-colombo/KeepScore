package ovh.litapp.keepscore.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.litapp.keepscore.data.RecordedChange
import ovh.litapp.keepscore.data.RecordedChange.Type
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore

/** Tests for the undo/redo stacks and the derived scoreboard values in [GameScoringModel]. */
class GameScoringModelTest {

    @Test
    fun clearUndoRedoClearsBothStacks() {
        val model = GameScoringModel(ArrayList())
        model.recordChange(change(0, Type.AddNew, 5, 1000L))
        assertTrue(model.canUndo())

        model.recordChange(change(0, Type.AddNew, 6, 2000L))
        model.undoBatch()
        assertTrue(model.canUndo())
        assertTrue(model.canRedo())

        model.clearUndoRedo()

        assertFalse(model.canUndo())
        assertFalse(model.canRedo())
    }

    @Test
    fun recordChangeEnablesUndoAndClearsRedo() {
        val model = GameScoringModel(ArrayList())
        assertFalse(model.canUndo())
        assertFalse(model.canRedo())

        model.recordChange(change(0, Type.AddNew, 5, 1000L))
        assertTrue(model.canUndo())
        assertFalse(model.canRedo())

        model.undoBatch()
        assertFalse(model.canUndo())
        assertTrue(model.canRedo())

        // a new change wipes out the redo stack
        model.recordChange(change(0, Type.AddNew, 5, 1000L))
        assertTrue(model.canUndo())
        assertFalse(model.canRedo())
    }

    @Test
    fun undoBatchPopsABatchedActionMostRecentFirst() {
        val model = GameScoringModel(ArrayList())
        // one + tap followed by two quick taps, as PlayerScoreModel records them
        model.recordChange(change(0, Type.AddNew, 1, 1000L))
        model.recordChange(change(0, Type.ModifyLast, 2, 2000L))
        model.recordChange(change(0, Type.ModifyLast, 3, 3000L))

        val undo = model.undoBatch()

        assertEquals(3, undo.size)
        assertEquals(3000L, undo[0].delta.timestamp)
        assertEquals(2000L, undo[1].delta.timestamp)
        assertEquals(1000L, undo[2].delta.timestamp)
        assertFalse(model.canUndo())
        assertTrue(model.canRedo())

        val redo = model.redoBatch()

        assertEquals(3, redo.size)
        assertEquals(1000L, redo[0].delta.timestamp)
        assertEquals(2000L, redo[1].delta.timestamp)
        assertEquals(3000L, redo[2].delta.timestamp)
        assertTrue(model.canUndo())
        assertFalse(model.canRedo())
    }

    @Test
    fun undoBatchStopsAtUnacceptableTransition() {
        val model = GameScoringModel(ArrayList())
        // two separate batches; undoing must not merge them into one action
        model.recordChange(change(0, Type.AddNew, 1, 1000L))
        model.recordChange(change(0, Type.AddNew, 2, 2000L))

        val undo = model.undoBatch()

        assertEquals(1, undo.size)
        assertEquals(2000L, undo[0].delta.timestamp)
        assertTrue(model.canUndo())
    }

    @Test
    fun undoBatchStopsAtDifferentPlayer() {
        val model = GameScoringModel(ArrayList())
        model.recordChange(change(0, Type.AddNew, 1, 1000L))
        model.recordChange(change(1, Type.AddNew, 2, 2000L))

        val undo = model.undoBatch()

        assertEquals(1, undo.size)
        assertEquals(1, undo[0].playerNumber)
        assertTrue(model.canUndo())
    }

    @Test
    fun undoStackExpiresAtCapacity() {
        val model = GameScoringModel(ArrayList())
        for (i in 0..GameScoringModel.UNDO_STACK_SIZE) {
            // ModifyLast -> ModifyLast is acceptable, so a full stack pops in one undo
            model.recordChange(change(0, Type.ModifyLast, 1, i.toLong()))
        }

        val undo = model.undoBatch()

        assertEquals(GameScoringModel.UNDO_STACK_SIZE, undo.size)
        assertFalse(model.canUndo())
    }

    @Test
    fun transitionRulesMatchTheLegacySets() {
        val types = Type.entries.toTypedArray()
        val expectedUndo = arrayOf(
            booleanArrayOf(false, false, false, false),
            booleanArrayOf(true, true, false, true),
            booleanArrayOf(false, false, false, false),
            booleanArrayOf(true, true, false, true),
        )
        val expectedRedo = arrayOf(
            booleanArrayOf(false, true, false, true),
            booleanArrayOf(false, true, false, true),
            booleanArrayOf(false, false, false, false),
            booleanArrayOf(false, true, false, true),
        )

        for (lastIndex in types.indices) {
            for (nextIndex in types.indices) {
                assertEquals(
                    "undo ${types[lastIndex]} -> ${types[nextIndex]}",
                    expectedUndo[lastIndex][nextIndex],
                    GameScoringModel.isAcceptableUndoOrRedoTransition(
                        true,
                        types[lastIndex],
                        types[nextIndex],
                    ),
                )
                assertEquals(
                    "redo ${types[lastIndex]} -> ${types[nextIndex]}",
                    expectedRedo[lastIndex][nextIndex],
                    GameScoringModel.isAcceptableUndoOrRedoTransition(
                        false,
                        types[lastIndex],
                        types[nextIndex],
                    ),
                )
            }
        }
    }

    @Test
    fun computeRoundTotalUsesTheLongestHistory() {
        val alice = newPlayerScore(0)
        alice.history = arrayListOf(Delta(1L, 5), Delta(2L, 3))
        val bob = newPlayerScore(1)
        bob.history = arrayListOf(Delta(1L, 2))

        val roundTotal = GameScoringModel(listOf(alice, bob)).computeRoundTotal()

        // round 2; Bob has no entry for it, so it counts as zero
        assertEquals(2, roundTotal.round)
        assertEquals(3, roundTotal.total)
    }

    @Test
    fun computeRoundTotalSumsShortHistoriesForTheFirstRound() {
        val alice = newPlayerScore(0)
        alice.history = arrayListOf(Delta(1L, 1))
        val bob = newPlayerScore(1)
        bob.history = arrayListOf(Delta(1L, 2))

        val roundTotal = GameScoringModel(listOf(alice, bob)).computeRoundTotal()

        assertEquals(1, roundTotal.round)
        assertEquals(3, roundTotal.total)
    }

    @Test
    fun computeRoundTotalIsZeroWithoutHistory() {
        val roundTotal = GameScoringModel(listOf(newPlayerScore(0), newPlayerScore(1)))
            .computeRoundTotal()

        assertEquals(0, roundTotal.round)
        assertEquals(0, roundTotal.total)
    }

    @Test
    fun highlightedPlayerIsTheMostRecentlyUpdated() {
        val alice = newPlayerScore(0)
        val bob = newPlayerScore(1)
        bob.lastUpdate = 500L

        assertEquals(1, GameScoringModel(listOf(alice, bob)).highlightedPlayerIndex())
    }

    @Test
    fun highlightedPlayerIsTheEarliestOnATie() {
        val alice = newPlayerScore(0)
        val bob = newPlayerScore(1)
        alice.lastUpdate = 500L
        bob.lastUpdate = 500L

        assertEquals(0, GameScoringModel(listOf(alice, bob)).highlightedPlayerIndex())
    }

    @Test
    fun highlightedPlayerIsNoneWhenNoPlayerWasUpdated() {
        assertEquals(
            -1,
            GameScoringModel(listOf(newPlayerScore(0), newPlayerScore(1))).highlightedPlayerIndex(),
        )
    }

    @Test
    fun undoAndRedoRestoreScoresThroughTheModels() {
        val alice = newPlayerScore(0)
        val bob = newPlayerScore(1)
        val aliceModel = PlayerScoreModel(alice)
        val bobModel = PlayerScoreModel(bob)
        val game = GameScoringModel(listOf(alice, bob))

        game.recordChange(aliceModel.applyDelta(5, 1000L, UPDATE_DELAY_MS))
        game.recordChange(bobModel.applyDelta(3, 1000L, UPDATE_DELAY_MS))

        // undo only touches the most recently changed player
        val undo = game.undoBatch()
        assertEquals(1, undo.size)
        bobModel.revertChange(undo[0])
        assertEquals(5L, alice.score)
        assertEquals(0L, bob.score)

        val redo = game.redoBatch()
        assertEquals(1, redo.size)
        bobModel.reexecuteChange(redo[0])
        assertEquals(5L, alice.score)
        assertEquals(3L, bob.score)
    }

    private fun newPlayerScore(playerNumber: Int): PlayerScore = PlayerScore().apply {
        this.playerNumber = playerNumber
        score = 0L
        history = ArrayList()
    }

    private fun change(
        playerNumber: Int,
        type: Type,
        value: Int,
        timestamp: Long,
    ): RecordedChange = RecordedChange(playerNumber, type, Delta(timestamp, value))

    private companion object {
        private const val UPDATE_DELAY_MS = 10000L
    }
}
