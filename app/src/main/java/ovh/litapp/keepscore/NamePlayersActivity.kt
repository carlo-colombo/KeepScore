package ovh.litapp.keepscore

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ovh.litapp.keepscore.android.EdgeToEdge
import ovh.litapp.keepscore.helper.GameActivityHelper
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.ui.nameplayers.NamePlayersModel
import ovh.litapp.keepscore.ui.nameplayers.NamePlayersScreen
import ovh.litapp.keepscore.ui.theme.KeepScoreTheme

class NamePlayersActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val numPlayers = NamePlayersModel.normalizePlayerCount(
            intent.getIntExtra(EXTRA_NUM_PLAYERS, NamePlayersModel.MIN_PLAYERS),
        )
        setContent {
            KeepScoreTheme {
                NamePlayersScreen(
                    numPlayers = numPlayers,
                    showColors = PreferenceHelper.getShowColors(this),
                    onStartGame = { names, colors ->
                        GameActivityHelper.newGame(this, names, colors)
                    },
                )
            }
        }

        EdgeToEdge.apply(this)
    }

    companion object {
        const val EXTRA_NUM_PLAYERS = "numPlayers"
    }
}
