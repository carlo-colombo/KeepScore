package ovh.litapp.keepscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import ovh.litapp.keepscore.android.EdgeToEdge
import ovh.litapp.keepscore.ui.newgame.NewGameScreen

class NewGameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                NewGameScreen(onNumPlayersSelected = ::startNamePlayersActivity)
            }
        }

        EdgeToEdge.apply(this)
    }

    private fun startNamePlayersActivity(numPlayers: Int) {
        val intent = Intent(this, NamePlayersActivity::class.java)
        intent.putExtra(NamePlayersActivity.EXTRA_NUM_PLAYERS, numPlayers)
        startActivity(intent)
    }

    companion object {
        /**
         * Counts above this go through the "More…" button; the game screens cannot lay out more
         * players at once. Java callers (e.g. GameActivityHelper) read this as a static field.
         */
        const val TYPICAL_MAX_NUMBER_OF_PLAYERS = 8
    }
}
