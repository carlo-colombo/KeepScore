package ovh.litapp.keepscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ovh.litapp.keepscore.android.EdgeToEdge
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.ui.history.HistoryScreen
import ovh.litapp.keepscore.ui.theme.KeepScoreTheme

/** Activity host for the Compose history views; the caller still passes the game parcel. */
class HistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = intent.getParcelableExtra(EXTRA_GAME, Game::class.java)
            ?: error("HistoryActivity requires a $EXTRA_GAME extra")

        setContent {
            KeepScoreTheme {
                HistoryScreen(game = game, onBack = ::finish)
            }
        }
        EdgeToEdge.apply(this)
    }

    companion object {
        /** Intent key used by MainActivity and GameActivity to hand history its game parcel. */
        const val EXTRA_GAME = "game"
    }
}
