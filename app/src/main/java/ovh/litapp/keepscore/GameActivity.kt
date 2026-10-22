package ovh.litapp.keepscore

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit
import ovh.litapp.keepscore.android.EdgeToEdge
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.GameDBHelper
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.GameActivityHelper
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.ui.scoring.ScoringMenuActions
import ovh.litapp.keepscore.ui.scoring.ScoringPreferences
import ovh.litapp.keepscore.ui.scoring.ScoringScreenState
import ovh.litapp.keepscore.ui.scoring.setScoringScreenContent
import ovh.litapp.keepscore.util.StopWatch
import ovh.litapp.keepscore.util.UtilLogger

/**
 * Main activity when the user is playing a game.
 *
 * The scoreboard itself is the Compose screen in `ui/scoring`, which owns every score mutation, the
 * undo/redo stack and the round totals; this activity owns the game object, the autosave and the
 * commands the scoreboard menu offers, and hides the action bar while a game is on screen.
 *
 * @author nolan
 */
abstract class GameActivity : AppCompatActivity() {

    protected var game: Game? = null
    protected var playerScores: MutableList<PlayerScore>? = null

    private lateinit var scoringScreenState: ScoringScreenState

    private val handler = Handler(Looper.getMainLooper())
    private var paused = true
    private var dbHelper: GameDBHelper? = null
    private var savedGameBeforeExit = false

    private val menuActions: ScoringMenuActions = object : ScoringMenuActions {

        override fun showHistory() {
            val historyIntent = Intent(this@GameActivity, HistoryActivity::class.java)
            historyIntent.putExtra(HistoryActivity.EXTRA_GAME, requireGame())
            startActivity(historyIntent)
        }

        override fun editPlayers() {
            startOrganizePlayersActivity()
        }

        override fun rematch() {
            showRematchDialogue()
        }

        override fun undo() {
            scoringScreenState.undoOrRedo(true)
        }

        override fun redo() {
            scoringScreenState.undoOrRedo(false)
        }

        override fun openSettings() {
            startActivity(Intent(this@GameActivity, SettingsActivity::class.java))
        }
    }

    private val organizePlayersActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newPlayerScores = result.data
                ?.getParcelableArrayListExtra<PlayerScore>(OrganizePlayersActivity.EXTRA_PLAYER_SCORES)
            if (newPlayerScores != null) {
                handler.post { saveWithNewPlayerScores(newPlayerScores) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createGame()

        setUpScoringScreen()
        // This activity hides the action bar in onResume(), after edge-to-edge insets are applied.
        EdgeToEdge.apply(this, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (gameWasSaved() || shouldAutosave()) {
                    // back button pressed
                    // if the game has been modified at all, skip showing the "enter player names"
                    // screen and just go directly to the main screen
                    val intent = Intent(this@GameActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                } else {
                    // no saved game, so fall back to the default behavior of finishing this activity
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        log.d("onDestroy()")
    }

    override fun onPause() {
        super.onPause()
        log.d("onPause()")

        paused = true

        if (shouldAutosave()) {
            saveGame(requireGame(), false, null)
        }

        dbHelper?.close()
        dbHelper = null

        if (savedGameBeforeExit) { // if nothing was changed in the game, don't show this message
            Toast.makeText(this, R.string.toast_game_saved, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        val useWakeLock = PreferenceHelper.getBooleanPreference(
            R.string.CONSTANT_pref_use_wake_lock,
            R.string.CONSTANT_pref_use_wake_lock_default,
            this,
        )
        if (useWakeLock) {
            // FLAG_KEEP_SCREEN_ON replaces the deprecated FULL_WAKE_LOCK; the window flag is scoped
            // to this window and needs no permission.
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        startPeriodicSave()

        // the scoreboard draws itself from the preferences, so re-read them here: the user may have
        // changed the colour scheme, the button values or the update delay in the settings
        scoringScreenState.refreshPreferences()

        paused = false
        savedGameBeforeExit = false

        supportActionBar?.hide()
    }

    private fun requireGame(): Game = game ?: error("the game was not created")

    private fun getDbHelper(): GameDBHelper {
        if (dbHelper == null) {
            dbHelper = GameDBHelper(this)
        }
        return dbHelper!!
    }

    private fun startPeriodicSave() {
        handler.postDelayed(
            {
                if (shouldAutosave()) {
                    saveGame(requireGame(), true) {
                        if (!paused) {
                            startPeriodicSave()
                        }
                    }
                } else {
                    log.d("no need to do periodic save")
                    if (!paused) {
                        startPeriodicSave()
                    }
                }
            },
            PERIODIC_SAVE_PERIOD,
        )
    }

    private fun startOrganizePlayersActivity() {
        val intent = Intent(this, OrganizePlayersActivity::class.java)
        intent.putExtra(EXTRA_GAME, requireGame())
        organizePlayersActivityLauncher.launch(intent)
    }

    private fun showRematchDialogue() {
        AlertDialog.Builder(this).setCancelable(true).setTitle(R.string.text_confirm)
            .setMessage(R.string.text_confirm_rematch)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> createRematchGame() }
            .show()
    }

    private fun createRematchGame() {
        saveGame(requireGame(), true, null) // save the original game

        val rematch = requireGame().makeCleanCopy()
        game = rematch
        playerScores = rematch.playerScores

        // a rematch is a new game: give it a scoreboard of its own, with an empty undo stack
        setUpScoringScreen()
        scoringScreenState.resetForRematch()
        saveGame(rematch, true, null) // save the new game

        Toast.makeText(this, R.string.toast_rematch_created, Toast.LENGTH_SHORT).show()
    }

    private fun gameWasSaved(): Boolean = game != null && requireGame().id != -1

    private fun shouldAutosave(): Boolean =
        // only autosave if the user has changed SOMETHING, i.e. the scores aren't all just zero
        scoringScreenState.shouldAutosave()

    private fun createGame() {
        if (intent.hasExtra(EXTRA_PLAYER_NAMES)) {
            // starting a new game
            createNewGame()
        } else if (intent.hasExtra(EXTRA_GAME_ID)) {
            createExistingGameFromId()
        } else {
            val extraGame = intent.getParcelableExtra<Game>(EXTRA_GAME)
            requireNotNull(extraGame) { "no game to play" }
            game = extraGame
            playerScores = extraGame.playerScores
        }
    }

    private fun createExistingGameFromId() {
        val gameId = intent.getIntExtra(EXTRA_GAME_ID, 0)

        val existingGame = requireNotNull(getDbHelper().findGameById(gameId)) {
            "no saved game with id $gameId"
        }
        game = existingGame
        playerScores = existingGame.playerScores
    }

    private fun createNewGame() {
        val playerNames = requireNotNull(intent.getStringArrayExtra(EXTRA_PLAYER_NAMES))
        val playerColors = requireNotNull(intent.getStringArrayExtra(EXTRA_PLAYER_COLORS))

        val newGame = Game()
        val scores = ArrayList<PlayerScore>()

        newGame.dateStarted = System.currentTimeMillis()
        newGame.playerScores = scores
        game = newGame
        playerScores = scores

        for (i in playerNames.indices) {
            val playerScore = PlayerScore()

            playerScore.name = playerNames[i]
            playerScore.playerColor = PlayerColor.deserialize(playerColors[i])
            playerScore.playerNumber = i
            playerScore.history = ArrayList<Delta>()
            playerScore.score = PreferenceHelper.getIntPreference(
                R.string.CONSTANT_pref_initial_score,
                R.string.CONSTANT_pref_initial_score_default,
                this,
            ).toLong()

            scores.add(playerScore)
        }

        log.d("created new game: %s", newGame)
        log.d("created new playerScores: %s", scores)
    }

    /**
     * Puts the scoreboard of the current [playerScores] on screen, replacing the one of the previous
     * game if there was one.
     */
    private fun setUpScoringScreen() {
        scoringScreenState = ScoringScreenState(
            requireNotNull(playerScores) { "no players to score" },
            ScoringPreferences.from(this),
        )

        // set to autosave if the player names are filled in. This feels intuitive to me. There's no
        // point in saving an empty game, but if the player names are included, the game feels
        // non-empty and therefore worth saving. This only applies for newly created games.
        scoringScreenState.markDirtyForUnsavedNewGame(requireGame().id)

        setScoringScreenContent(scoringScreenState, menuActions)
    }

    @Synchronized
    private fun saveGame(gameToSave: Game, inBackground: Boolean, onFinished: (() -> Unit)?) {
        scoringScreenState.markSaved()

        if (inBackground) {
            // do in the background to avoid jankiness
            object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg params: Void): Void? {
                    saveGame(gameToSave)
                    return null
                }

                override fun onPostExecute(result: Void?) {
                    super.onPostExecute(result)
                    onFinished?.invoke()
                }
            }.execute()
        } else {
            // do in foreground to ensure the game gets saved before the activity finishes
            saveGame(gameToSave)
            onFinished?.invoke()
        }
    }

    @Synchronized
    private fun saveGame(gameToSave: Game) {
        val stopWatch = StopWatch("saveGame()")

        getDbHelper().saveGame(gameToSave)
        log.d("saved game: %s", gameToSave)
        savedGameBeforeExit = true

        stopWatch.log(log)
    }

    private fun saveWithNewPlayerScores(newPlayerScores: List<PlayerScore>) {
        // delete the game and recreate it with the new data

        // do in the background to avoid jank
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                // delete the old game before starting new one
                getDbHelper().deleteGame(requireGame())
                // after this, because the id is not -1, only UPDATEs will be performed,
                // so the delete is clean even if the background saver keeps running

                val newGame = requireGame().clone()
                newGame.id = -1
                newGame.playerScores = ArrayList(newPlayerScores)
                for (playerScore in newGame.playerScores) {
                    playerScore.id = -1
                }

                val onFinished = {
                    log.d("game to parcel is: %s", requireGame())

                    // start a new activity so that the layout can refresh correctly
                    // TODO: don't start a new activity; just refresh the layout
                    GameActivityHelper.openGameWithClearTop(this@GameActivity, newGame)
                }
                saveGame(newGame, true, onFinished) // automatically save the game

                return null
            }
        }.execute()
    }

    companion object {

        const val EXTRA_PLAYER_NAMES = "playerNames"
        const val EXTRA_PLAYER_COLORS = "playerColors"
        const val EXTRA_GAME_ID = "gameId"
        const val EXTRA_GAME = "game"

        private val PERIODIC_SAVE_PERIOD = TimeUnit.SECONDS.toMillis(30)

        private val log = UtilLogger(GameActivity::class.java)
    }
}
