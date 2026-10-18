package ovh.litapp.keepscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.ui.organize.OrganizePlayersScreen
import ovh.litapp.keepscore.ui.theme.KeepScoreTheme

/**
 * The "Edit Players" screen, rendered with Jetpack Compose.
 *
 * Replaces the DragSortListView-based screen: the players are a reorderable Compose list, and the
 * add/sort/shuffle actions moved from the options menu into the top app bar. The result contract is
 * unchanged, so [GameActivity] is unaffected: OK returns the players in their final order under
 * [EXTRA_PLAYER_SCORES], while cancelling, the up arrow and the back gesture all return
 * [RESULT_CANCELED].
 */
class OrganizePlayersActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = intent.getParcelableExtra(GameActivity.EXTRA_GAME, Game::class.java)
            ?: error("OrganizePlayersActivity needs a ${GameActivity.EXTRA_GAME} extra")

        onBackPressedDispatcher.addCallback(this) {
            cancelAndFinish()
        }

        setContent {
            KeepScoreTheme {
                OrganizePlayersScreen(
                    initialPlayers = game.playerScores,
                    onConfirm = ::setPlayersResultAndFinish,
                    onCancel = ::cancelAndFinish,
                )
            }
        }
    }

    private fun setPlayersResultAndFinish(players: List<PlayerScore>) {
        setResult(RESULT_OK, playerScoresResult(players))
        finish()
    }

    private fun cancelAndFinish() {
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {

        /**
         * More players than this cannot be laid out by the game screens at once. Read by the
         * New Game screen for its "More…" dialog.
         */
        const val MAX_NUM_PLAYERS = 30

        /** The game screens need at least this many players. */
        const val MIN_NUM_PLAYERS = 2

        /** Key of the [PlayerScore] list in the [RESULT_OK] result. */
        const val EXTRA_PLAYER_SCORES = "playerScores"
    }
}

/**
 * The [RESULT_OK] payload: the players in their final order under [EXTRA_PLAYER_SCORES], which is
 * what [GameActivity] reads back out of the result.
 */
internal fun playerScoresResult(players: List<PlayerScore>): Intent =
    Intent().putParcelableArrayListExtra(OrganizePlayersActivity.EXTRA_PLAYER_SCORES, ArrayList(players))
