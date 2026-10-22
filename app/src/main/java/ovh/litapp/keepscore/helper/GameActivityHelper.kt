package ovh.litapp.keepscore.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import ovh.litapp.keepscore.GameActivity
import ovh.litapp.keepscore.LandscapeGameActivity
import ovh.litapp.keepscore.NewGameActivity
import ovh.litapp.keepscore.PortraitGameActivity
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.GameSummary

object GameActivityHelper {

    @JvmStatic
    fun openGame(context: Context, game: GameSummary) {
        val intent = Intent(context, getGameActivityClass(game.playerNames.size))
        intent.putExtra(GameActivity.EXTRA_GAME_ID, game.id)

        context.startActivity(intent)
    }

    @JvmStatic
    fun newGame(context: Context, playerNames: Array<String>, playerColors: Array<String>) {
        val intent = Intent(context, getGameActivityClass(playerNames.size))

        intent.putExtra(GameActivity.EXTRA_PLAYER_NAMES, playerNames)
        intent.putExtra(GameActivity.EXTRA_PLAYER_COLORS, playerColors)

        context.startActivity(intent)
    }

    @JvmStatic
    fun openGameWithClearTop(context: Activity, game: Game) {
        val intent = Intent(context, getGameActivityClass(game.playerScores.size))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(GameActivity.EXTRA_GAME, game)

        context.startActivity(intent)

        CompatibilityHelper.overridePendingTransition(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out,
        )
    }

    @JvmStatic
    fun getGameActivityClass(numPlayers: Int): Class<*> =
        if (numPlayers > NewGameActivity.TYPICAL_MAX_NUMBER_OF_PLAYERS) {
            PortraitGameActivity::class.java
        } else {
            LandscapeGameActivity::class.java
        }
}
