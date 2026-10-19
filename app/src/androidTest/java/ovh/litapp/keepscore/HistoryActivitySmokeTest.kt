package ovh.litapp.keepscore

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryActivitySmokeTest {

    @Test
    fun timelineAndRoundHistoryLaunchWithTimestampedRounds() {
        launchHistory(game(playerCount = 3, timestamped = true))
    }

    @Test
    fun historyWithoutTimestampsLaunchesWithoutTimeline() {
        launchHistory(game(playerCount = 3, timestamped = false))
    }

    @Test
    fun historyWithMoreThanEightPlayersLaunchesWithoutRoundTable() {
        launchHistory(game(playerCount = 9, timestamped = true))
    }

    private fun launchHistory(game: Game) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, HistoryActivity::class.java)
            .putExtra(HistoryActivity.EXTRA_GAME, game)
        val scenario = ActivityScenario.launch<HistoryActivity>(intent)
        scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
        scenario.close()
    }

    private fun game(playerCount: Int, timestamped: Boolean) = Game().apply {
        id = 76
        name = "History smoke"
        dateStarted = System.currentTimeMillis()
        dateSaved = dateStarted
        playerScores = (0 until playerCount).map { index ->
            val firstTimestamp = if (timestamped) dateStarted + 5_000 else 0
            val secondTimestamp = if (timestamped) dateStarted + 10_000 else 0
            val deltas = listOf(Delta(firstTimestamp, 3), Delta(secondTimestamp, -1))
            PlayerScore().apply {
                name = "Player ${index + 1}"
                playerNumber = index
                playerColor = PlayerColor.BUILT_INS[index % PlayerColor.BUILT_INS.size]
                history = deltas
                score = 2
            }
        }
    }
}
