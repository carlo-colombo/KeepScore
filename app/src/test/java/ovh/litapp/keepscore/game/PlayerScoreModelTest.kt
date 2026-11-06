package ovh.litapp.keepscore.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.litapp.keepscore.data.RecordedChange
import ovh.litapp.keepscore.data.RecordedChange.Type
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore

/** Tests for the score/history mutation and batching rules in [PlayerScoreModel]. */
class PlayerScoreModelTest {

    @Test
    fun firstDeltaAppendsHistoryEntry() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        val change = model.applyDelta(5, 1000L, UPDATE_DELAY_MS)

        assertEquals(Type.AddNew, change.type)
        assertEquals(5, change.delta.value)
        assertEquals(1000L, change.delta.timestamp)
        assertEquals(1, change.playerNumber)
        assertEquals(listOf(Delta(1000L, 5)), playerScore.history)
        assertEquals(5L, playerScore.score)
        assertEquals(1000L, model.lastIncremented)
    }

    @Test
    fun deltaAtDelayBoundaryBatchesIntoLastEntry() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.applyDelta(2, 1000L + UPDATE_DELAY_MS, UPDATE_DELAY_MS)

        assertEquals(Type.ModifyLast, change.type)
        assertEquals(Delta(11000L, 2), change.delta)
        assertEquals(listOf(Delta(11000L, 7)), playerScore.history)
        assertEquals(7L, playerScore.score)
        assertEquals(11000L, model.lastIncremented)
    }

    @Test
    fun deltaWithinDelayBatchesIntoLastEntry() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.applyDelta(2, 2000L, UPDATE_DELAY_MS)

        assertEquals(Type.ModifyLast, change.type)
        assertEquals(2, change.delta.value)
        assertEquals(2000L, change.delta.timestamp)
        // the stored entry holds the batch total and its timestamp moves to the latest delta
        assertEquals(listOf(Delta(2000L, 7)), playerScore.history)
        assertEquals(7L, playerScore.score)
        assertEquals(2000L, model.lastIncremented)
    }

    @Test
    fun deltaAfterDelayStartsNewEntry() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.applyDelta(2, 1000L + UPDATE_DELAY_MS + 1, UPDATE_DELAY_MS)

        assertEquals(Type.AddNew, change.type)
        assertEquals(Delta(11001L, 2), change.delta)
        assertEquals(listOf(Delta(1000L, 5), Delta(11001L, 2)), playerScore.history)
        assertEquals(7L, playerScore.score)
        assertEquals(11001L, model.lastIncremented)
    }

    @Test
    fun deltaThatBatchesToZeroDeletesLastEntry() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.applyDelta(-5, 2000L, UPDATE_DELAY_MS)

        assertEquals(Type.DeleteLastZero, change.type)
        assertEquals(5, change.delta.value)
        assertTrue(playerScore.history.isEmpty())
        assertEquals(0L, playerScore.score)
        // the batch ends, so the next delta starts a new entry
        assertEquals(0L, model.lastIncremented)
    }

    @Test
    fun addZeroAppendsZeroEntryAndEndsBatch() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        val change = model.addZero(1000L)

        assertEquals(Type.AddNew, change.type)
        assertEquals(0, change.delta.value)
        assertEquals(listOf(Delta(1000L, 0)), playerScore.history)
        assertEquals(0L, playerScore.score)
        assertEquals(0L, model.lastIncremented)

        val next = model.applyDelta(3, UPDATE_DELAY_MS + 1, UPDATE_DELAY_MS)
        assertEquals(Type.AddNew, next.type)
        assertEquals(2, playerScore.history.size)
    }

    @Test
    fun deleteLastRemovesEntryAndScore() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.deleteLast()

        assertEquals(Type.DeleteLast, change?.type)
        assertEquals(5, change?.delta?.value)
        assertTrue(playerScore.history.isEmpty())
        assertEquals(0L, playerScore.score)
        assertEquals(0L, model.lastIncremented)
    }

    @Test
    fun deleteLastOnEmptyHistoryReturnsNull() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        assertNull(model.deleteLast())
        assertTrue(playerScore.history.isEmpty())
    }

    @Test
    fun revertThenReexecuteAddNewRestoresState() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        val change = model.applyDelta(5, 1000L, UPDATE_DELAY_MS)

        model.revertChange(change)
        assertTrue(playerScore.history.isEmpty())
        assertEquals(0L, playerScore.score)

        model.reexecuteChange(change)
        assertEquals(listOf(Delta(1000L, 5)), playerScore.history)
        assertEquals(5L, playerScore.score)
    }

    @Test
    fun revertThenReexecuteModifyLastRestoresState() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.applyDelta(2, 2000L, UPDATE_DELAY_MS)

        model.revertChange(change)
        assertEquals(listOf(Delta(2000L, 5)), playerScore.history)
        assertEquals(5L, playerScore.score)

        model.reexecuteChange(change)
        assertEquals(listOf(Delta(2000L, 7)), playerScore.history)
        assertEquals(7L, playerScore.score)
    }

    @Test
    fun revertThenReexecuteDeleteLastRestoresState() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = requireNotNull(model.deleteLast())

        model.revertChange(change)
        assertEquals(listOf(Delta(1000L, 5)), playerScore.history)
        assertEquals(5L, playerScore.score)

        model.reexecuteChange(change)
        assertTrue(playerScore.history.isEmpty())
        assertEquals(0L, playerScore.score)
    }

    @Test
    fun revertThenReexecuteDeleteLastZeroRestoresState() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        val change = model.applyDelta(-5, 2000L, UPDATE_DELAY_MS)

        model.revertChange(change)
        assertEquals(listOf(Delta(1000L, 5)), playerScore.history)
        assertEquals(5L, playerScore.score)

        model.reexecuteChange(change)
        assertTrue(playerScore.history.isEmpty())
        assertEquals(0L, playerScore.score)
    }

    @Test
    fun resetLastIncrementedEndsTheCurrentBatch() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 100000L, UPDATE_DELAY_MS)
        assertEquals(100000L, model.lastIncremented)

        model.resetLastIncremented()

        assertEquals(0L, model.lastIncremented)
        val next = model.applyDelta(2, 100001L, UPDATE_DELAY_MS)
        assertEquals(Type.AddNew, next.type)
        assertEquals(listOf(Delta(100000L, 5), Delta(100001L, 2)), playerScore.history)
        assertEquals(7L, playerScore.score)
    }

    @Test
    fun resetClearsHistoryAndRestoresInitialScore() {
        val playerScore = newPlayerScore()
        val model = PlayerScoreModel(playerScore)

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS)
        model.reset(10L)

        assertEquals(10L, playerScore.score)
        assertTrue(playerScore.history.isEmpty())
        assertEquals(0L, model.lastIncremented)
    }

    private fun newPlayerScore(): PlayerScore = PlayerScore().apply {
        playerNumber = 1
        score = 0L
        history = ArrayList()
    }

    private companion object {
        private const val UPDATE_DELAY_MS = 10000L
    }
}
