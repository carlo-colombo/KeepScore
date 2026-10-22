package ovh.litapp.keepscore.game

import java.util.concurrent.atomic.AtomicLong
import ovh.litapp.keepscore.data.RecordedChange
import ovh.litapp.keepscore.data.RecordedChange.Type
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore

/**
 * The scoring state for a single player in a game.
 *
 * Owns every mutation of a [PlayerScore]'s score and history, plus the timestamp of the last
 * increment that decides whether a new + or - tap batches into the current history entry or starts
 * a new one. The batching rules live here rather than in the view so they can be exercised by plain
 * JUnit tests; callers are responsible for refreshing the UI and for passing the returned
 * [RecordedChange] to the undo recorder.
 */
class PlayerScoreModel(val playerScore: PlayerScore) {

    // Timestamps are written on the increment background thread while the UI thread reads them, so
    // keep the same atomic access the legacy PlayerView used.
    private val lastIncrementedAtomic = AtomicLong(0)

    /**
     * Timestamp of the last increment, or 0 when the current history entry is no longer being
     * batched into.
     */
    val lastIncremented: Long
        get() = lastIncrementedAtomic.get()

    /** Marks the current history entry as no longer being batched into, e.g. after "confirm". */
    fun resetLastIncremented() {
        lastIncrementedAtomic.set(0)
    }

    /**
     * Applies [value] to the player's score. When the previous increment was within [updateDelayMs]
     * of [currentTime], the value is batched into the last history entry (dropping that entry if the
     * batch reaches zero); otherwise a new history entry is appended. Returns the change to record
     * for undo/redo.
     */
    fun applyDelta(value: Int, currentTime: Long, updateDelayMs: Long): RecordedChange {
        val previousIncrement = lastIncrementedAtomic.getAndSet(currentTime)
        val history = playerScore.history
        val recordedChange: RecordedChange

        if (currentTime - previousIncrement > updateDelayMs || history.isEmpty()) {
            val delta = Delta(currentTime, value)
            history.add(delta)
            recordedChange = RecordedChange(playerScore.playerNumber, Type.AddNew, delta)
        } else {
            val lastIndex = history.size - 1
            val newValue = history[lastIndex].value + value
            if (newValue == 0) {
                // don't add "0" to the list; just delete the last history item
                val deletedDelta = history.removeAt(lastIndex)
                // reset the batch time so we don't update the previous value later
                lastIncrementedAtomic.set(0)
                recordedChange = RecordedChange(
                    playerScore.playerNumber,
                    Type.DeleteLastZero,
                    deletedDelta,
                )
            } else {
                history[lastIndex] = Delta(currentTime, newValue)
                recordedChange = RecordedChange(
                    playerScore.playerNumber,
                    Type.ModifyLast,
                    Delta(currentTime, value),
                )
            }
        }

        playerScore.score = playerScore.score + value
        return recordedChange
    }

    /**
     * Adds a zero-valued history entry, e.g. for a round in which the player scored nothing. Returns
     * the change to record for undo/redo.
     */
    fun addZero(currentTime: Long): RecordedChange {
        val delta = Delta(currentTime, 0)
        playerScore.history.add(delta)
        lastIncrementedAtomic.set(0)
        return RecordedChange(playerScore.playerNumber, Type.AddNew, delta)
    }

    /**
     * Removes the last history entry and subtracts its value from the score. Returns the change to
     * record for undo/redo, or `null` when the history is already empty.
     */
    fun deleteLast(): RecordedChange? {
        val history = playerScore.history
        var recordedChange: RecordedChange? = null
        if (history.isNotEmpty()) {
            val removed = history.removeAt(history.size - 1)
            playerScore.score = playerScore.score - removed.value
            recordedChange = RecordedChange(playerScore.playerNumber, Type.DeleteLast, removed)
        }
        lastIncrementedAtomic.set(0)
        return recordedChange
    }

    /** Resets the score to [initialScore] and empties the history, e.g. for a rematch. */
    fun reset(initialScore: Long) {
        playerScore.score = initialScore
        playerScore.history = ArrayList()
        lastIncrementedAtomic.set(0)
    }

    /** Undoes a change recorded by [applyDelta], [addZero] or [deleteLast]. */
    fun revertChange(recordedChange: RecordedChange) {
        val changeDelta = recordedChange.delta
        when (recordedChange.type) {
            Type.AddNew -> {
                playerScore.history.removeAt(playerScore.history.size - 1)
                playerScore.score = playerScore.score - changeDelta.value
            }

            Type.DeleteLast, Type.DeleteLastZero -> {
                playerScore.history.add(changeDelta)
                playerScore.score = playerScore.score + changeDelta.value
            }

            Type.ModifyLast -> {
                val lastDelta = playerScore.history[playerScore.history.size - 1]
                lastDelta.value = lastDelta.value - changeDelta.value
                playerScore.score = playerScore.score - changeDelta.value
            }
        }
    }

    /** Redoes a change previously undone by [revertChange]. */
    fun reexecuteChange(recordedChange: RecordedChange) {
        val changeDelta = recordedChange.delta
        when (recordedChange.type) {
            Type.AddNew -> {
                playerScore.history.add(changeDelta)
                playerScore.score = playerScore.score + changeDelta.value
            }

            Type.DeleteLast, Type.DeleteLastZero -> {
                playerScore.history.removeAt(playerScore.history.size - 1)
                playerScore.score = playerScore.score - changeDelta.value
            }

            Type.ModifyLast -> {
                val lastDelta = playerScore.history[playerScore.history.size - 1]
                lastDelta.value = lastDelta.value + changeDelta.value
                playerScore.score = playerScore.score + changeDelta.value
            }
        }
    }
}
