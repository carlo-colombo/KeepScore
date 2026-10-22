package ovh.litapp.keepscore.ui.scoring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ovh.litapp.keepscore.data.RecordedChange
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.game.GameScoringModel
import ovh.litapp.keepscore.game.PlayerScoreModel
import ovh.litapp.keepscore.game.RoundTotal

/**
 * One player's row of the scoreboard.
 *
 * [playerScore] is the instance the game hands in, so saving the game saves exactly what the user
 * did here. Every mutation goes through [scoreModel] and is then republished as the Compose state
 * below, because a [PlayerScore] is plain Java that Compose cannot observe.
 */
class ScoringPlayer internal constructor(
    val playerScore: PlayerScore,
    private val scoreModel: PlayerScoreModel,
) {

    var score by mutableLongStateOf(playerScore.score)
        private set

    /** The player's history, oldest entry first. */
    var history by mutableStateOf(playerScore.history.orEmpty().map { it.value })
        private set

    /** When the current history entry was last incremented into, or 0 when it is closed. */
    var lastIncremented by mutableLongStateOf(scoreModel.lastIncremented)
        private set

    /** When the player's score was last changed, which decides who wears the highlight tag. */
    var lastUpdate by mutableLongStateOf(playerScore.lastUpdate)
        private set

    internal fun applyDelta(delta: Int, currentTime: Long, updateDelayMs: Long): RecordedChange {
        val recordedChange = scoreModel.applyDelta(delta, currentTime, updateDelayMs)
        refresh()
        return recordedChange
    }

    internal fun addZero(currentTime: Long): RecordedChange {
        val recordedChange = scoreModel.addZero(currentTime)
        refresh()
        return recordedChange
    }

    internal fun deleteLast(): RecordedChange? {
        val recordedChange = scoreModel.deleteLast()
        refresh()
        return recordedChange
    }

    /** Accepts the current history entry as final, so the badge turns into a history line. */
    internal fun confirm() {
        scoreModel.resetLastIncremented()
        refresh()
    }

    internal fun reset(initialScore: Long) {
        scoreModel.reset(initialScore)
        refresh()
    }

    internal fun revertChange(recordedChange: RecordedChange) {
        scoreModel.revertChange(recordedChange)
        refresh()
    }

    internal fun reexecuteChange(recordedChange: RecordedChange) {
        scoreModel.reexecuteChange(recordedChange)
        refresh()
    }

    internal fun resetLastIncremented() {
        scoreModel.resetLastIncremented()
        refresh()
    }

    /** Marks the player as the most recently updated one, as finishing an update used to do. */
    internal fun markUpdated(currentTime: Long) {
        playerScore.lastUpdate = currentTime
        lastUpdate = currentTime
    }

    /** Republishes the score, history, batch window and update time of the underlying player. */
    internal fun refresh() {
        score = playerScore.score
        history = playerScore.history.orEmpty().map { it.value }
        lastIncremented = scoreModel.lastIncremented
        lastUpdate = playerScore.lastUpdate
    }
}

/**
 * The Compose state of the scoring screen.
 *
 * It owns the players, the per-player scoring rules ([PlayerScoreModel]) and the undo/redo stack
 * ([GameScoringModel]), and it is the only place the screen mutates a score. The host activity
 * builds one for a game, and another one if the game is replaced by a rematch; it calls
 * [refreshPreferences] when it is resumed, in case the user changed a preference while the screen
 * was in the background.
 *
 * The two preferences scoring itself depends on are handed in as [ScoringPreferences], so this
 * class needs no Android context and the scoring rules can be exercised without one.
 */
class ScoringScreenState(
    playerScores: List<PlayerScore>,
    private val preferences: ScoringPreferences,
) {

    val players = mutableStateListOf<ScoringPlayer>()

    private val scoringModel = GameScoringModel(playerScores)

    // written from the background thread of saveWithNewPlayerScores, like the legacy
    // per-player AtomicBoolean flags were
    @Volatile
    private var dirty = false

    /** Bumped by [refreshPreferences] to make the screen re-read the preferences. */
    var preferencesVersion by mutableIntStateOf(0)
        private set

    /** How long a + or - tap keeps batching into the same history entry. */
    var updateDelayMs by mutableIntStateOf(preferences.updateDelayMs)
        private set

    /** Bumped when the batch window of the last increment has elapsed. */
    var clockVersion by mutableIntStateOf(0)
        private set

    init {
        playerScores.forEach { playerScore ->
            players.add(ScoringPlayer(playerScore, PlayerScoreModel(playerScore)))
        }
    }

    /** Re-reads the preferences the screen draws with, e.g. after the user visited the settings. */
    fun refreshPreferences() {
        preferencesVersion++
        updateDelayMs = preferences.updateDelayMs
    }

    /** Tells the screen that a pending batch window has elapsed, so the badge fades out. */
    fun onUpdateDelayElapsed() {
        clockVersion++
    }

    fun increment(playerIndex: Int, delta: Int) {
        val player = players[playerIndex]
        val currentTime = System.currentTimeMillis()
        scoringModel.recordChange(player.applyDelta(delta, currentTime, updateDelayMs.toLong()))
        // only the + and - taps move the highlight, as they did in the legacy PlayerView
        player.markUpdated(currentTime)
        markDirty()
    }

    /** Adds a zero entry, for a round in which the player scored nothing. */
    fun addZero(playerIndex: Int) {
        scoringModel.recordChange(players[playerIndex].addZero(System.currentTimeMillis()))
        markDirty()
    }

    /** Drops the player's last history entry and subtracts it from the score. */
    fun deleteLast(playerIndex: Int) {
        players[playerIndex].deleteLast()?.let { scoringModel.recordChange(it) }
        markDirty()
    }

    /** Accepts the entry that is still being batched into as final. */
    fun confirm(playerIndex: Int) {
        players[playerIndex].confirm()
    }

    /** Sends every player back to the initial score for a rematch, as the legacy screen did. */
    fun resetForRematch() {
        val initialScore = preferences.initialScore.toLong()
        players.forEach { it.reset(initialScore) }
        scoringModel.clearUndoRedo()
        markDirty()
    }

    fun canUndo(): Boolean = scoringModel.canUndo()

    fun canRedo(): Boolean = scoringModel.canRedo()

    /**
     * Reverts (or re-executes) the next batch of changes, most recent first. Like the legacy
     * screen, undo and redo leave the highlight where it was and do not mark the game dirty.
     */
    fun undoOrRedo(undo: Boolean) {
        val recordedChanges = if (undo) scoringModel.undoBatch() else scoringModel.redoBatch()

        var lastPlayerNumber = -1
        for (recordedChange in recordedChanges) {
            val player = players[recordedChange.playerNumber]
            if (undo) {
                player.revertChange(recordedChange)
            } else {
                player.reexecuteChange(recordedChange)
            }
            lastPlayerNumber = recordedChange.playerNumber
        }

        if (lastPlayerNumber != -1) {
            // resetting the batch window keeps the badge from showing after the score was put back
            players[lastPlayerNumber].resetLastIncremented()
        }
    }

    /** Whether the game changed since it was last saved. */
    fun shouldAutosave(): Boolean = dirty

    /** The legacy screen saved a game as soon as it was created with a player name in it. */
    fun markDirtyForUnsavedNewGame(gameId: Int) {
        if (gameId == -1 && players.any { !it.playerScore.name.isNullOrEmpty() }) {
            markDirty()
        }
    }

    /** Called when the game has been written to the database. */
    fun markSaved() {
        dirty = false
    }

    /** The current round and the sum of the players' scores in it. */
    fun roundTotal(): RoundTotal {
        // read the observable history of every player, then let the model compute the round
        players.forEach { it.history }
        return scoringModel.computeRoundTotal()
    }

    /** The player whose score changed last, or -1 when no score has changed yet. */
    fun highlightedPlayerIndex(): Int {
        // read the observable update time of every player, then let the model pick the latest
        players.forEach { it.lastUpdate }
        return scoringModel.highlightedPlayerIndex()
    }

    private fun markDirty() {
        dirty = true
    }
}
