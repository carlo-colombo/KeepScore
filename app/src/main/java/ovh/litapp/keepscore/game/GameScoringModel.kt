package ovh.litapp.keepscore.game

import ovh.litapp.keepscore.data.RecordedChange
import ovh.litapp.keepscore.data.RecordedChange.Type
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.util.DataExpiringStack
import ovh.litapp.keepscore.util.Pair

/**
 * The game-wide scoring state: the undo/redo history and the values derived from the players' score
 * histories that the scoreboard shows (the current round total and which player was updated last).
 *
 * Undo/redo only decides *which* changes to revert or re-execute; applying them to a player is the
 * caller's job, which keeps the stack logic free of any view dependency.
 */
class GameScoringModel(private val playerScores: List<PlayerScore>) {

    private val undoStack = DataExpiringStack<RecordedChange>(UNDO_STACK_SIZE)
    private val redoStack = DataExpiringStack<RecordedChange>(UNDO_STACK_SIZE)

    /** Records a change so it can be undone, invalidating any redo history. */
    fun recordChange(recordedChange: RecordedChange) {
        undoStack.pop(recordedChange)
        redoStack.clear()
    }

    fun canUndo(): Boolean = !undoStack.isEmpty()

    fun canRedo(): Boolean = !redoStack.isEmpty()

    fun clearUndoRedo() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Pops and returns the changes an undo should revert, most recently recorded first. Stops at the
     * first change that targets a different player or that is not an acceptable transition from the
     * previous one, because consecutive edits to one player form a single user action.
     */
    fun undoBatch(): List<RecordedChange> =
        pollBatch(undoStack, redoStack, ACCEPTABLE_UNDO_TRANSITIONS)

    /** Like [undoBatch], but for redoing changes that were previously undone. */
    fun redoBatch(): List<RecordedChange> =
        pollBatch(redoStack, undoStack, ACCEPTABLE_REDO_TRANSITIONS)

    private fun pollBatch(
        stackToPoll: DataExpiringStack<RecordedChange>,
        stackToPush: DataExpiringStack<RecordedChange>,
        acceptableTransitions: Set<Pair<Type, Type>>,
    ): List<RecordedChange> {
        val batch = ArrayList<RecordedChange>()

        var lastPlayerNumber = -1
        var lastType: Type? = null
        while (true) {
            val peeked = stackToPoll.peek() ?: break
            val samePlayer = lastPlayerNumber == -1 || lastPlayerNumber == peeked.playerNumber
            // only apply multiple changes to the same PlayerScore
            val acceptable = lastType == null ||
                acceptableTransitions.contains(Pair.create(lastType, peeked.type))
            if (!samePlayer || !acceptable) {
                break
            }

            val recordedChange = stackToPoll.poll() ?: break
            batch.add(recordedChange)
            stackToPush.pop(recordedChange)

            lastPlayerNumber = recordedChange.playerNumber
            lastType = recordedChange.type
        }

        return batch
    }

    /**
     * The round the scoreboard is on (the longest player history) and the sum of every player's
     * score for that round. A player whose history is shorter counts as zero for the round.
     */
    fun computeRoundTotal(): RoundTotal {
        var round = 0
        for (playerScore in playerScores) {
            round = maxOf(round, playerScore.history.size)
        }

        if (round == 0) {
            return RoundTotal(0, 0)
        }

        var total = 0
        for (playerScore in playerScores) {
            val history = playerScore.history
            if (history.size >= round) {
                total += history[round - 1].value
            }
        }
        return RoundTotal(round, total)
    }

    /**
     * The index of the player whose score was updated most recently, or -1 when no player has been
     * updated, e.g. a game saved before updates were tracked. The earliest player wins ties.
     */
    fun highlightedPlayerIndex(): Int {
        var maxLastUpdate = 0L
        var maxLastUpdateIdx = -1

        for (i in playerScores.indices) {
            val playerScore = playerScores[i]
            if (playerScore.lastUpdate > maxLastUpdate) {
                maxLastUpdate = playerScore.lastUpdate
                maxLastUpdateIdx = i
            }
        }

        return maxLastUpdateIdx
    }

    companion object {

        /** How many changes to keep in memory. */
        const val UNDO_STACK_SIZE = 500

        private val ACCEPTABLE_UNDO_TRANSITIONS: Set<Pair<Type, Type>> = hashSetOf(
            Pair.create(Type.ModifyLast, Type.ModifyLast),
            Pair.create(Type.ModifyLast, Type.AddNew),
            Pair.create(Type.DeleteLastZero, Type.AddNew),
            Pair.create(Type.DeleteLastZero, Type.ModifyLast),
            Pair.create(Type.ModifyLast, Type.DeleteLastZero),
            Pair.create(Type.DeleteLastZero, Type.DeleteLastZero),
        )

        private val ACCEPTABLE_REDO_TRANSITIONS: Set<Pair<Type, Type>> = hashSetOf(
            Pair.create(Type.ModifyLast, Type.ModifyLast),
            Pair.create(Type.AddNew, Type.ModifyLast),
            Pair.create(Type.AddNew, Type.DeleteLastZero),
            Pair.create(Type.DeleteLastZero, Type.ModifyLast),
            Pair.create(Type.ModifyLast, Type.DeleteLastZero),
            Pair.create(Type.DeleteLastZero, Type.DeleteLastZero),
        )

        /**
         * Whether a change of [type] may follow a change of [lastType] in a single undo or redo.
         * "Modify last" edits are treated as part of the action they were batched into.
         */
        @JvmStatic
        fun isAcceptableUndoOrRedoTransition(undo: Boolean, lastType: Type, type: Type): Boolean =
            if (undo) {
                ACCEPTABLE_UNDO_TRANSITIONS.contains(Pair.create(lastType, type))
            } else {
                ACCEPTABLE_REDO_TRANSITIONS.contains(Pair.create(lastType, type))
            }
    }
}
