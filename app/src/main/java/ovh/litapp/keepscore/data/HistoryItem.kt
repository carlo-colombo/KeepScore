package ovh.litapp.keepscore.data

import android.content.Context

import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore

/**
 * Simple class for showing the full history of a user's score
 *
 * @author nolan
 */
class HistoryItem(var delta: Int, var runningTotal: Long, var isHideDelta: Boolean) {

    companion object {

        /**
         * Create a list of displayable history items given a PlayerScore.  Entries should be listed from past to future.
         */
        @JvmStatic
        fun createFromPlayerScore(playerScore: PlayerScore, context: Context): List<HistoryItem> {

            val history = playerScore.history
            var runningScore = getStartingScore(playerScore)

            val historyItems = ArrayList<HistoryItem>()

            // add an initial one to just show the starting score
            historyItems.add(HistoryItem(0, runningScore, true))

            for (historyDelta in history) {
                runningScore += historyDelta.value

                historyItems.add(HistoryItem(historyDelta.value, runningScore, false))
            }

            return historyItems
        }

        private fun getStartingScore(playerScore: PlayerScore): Long {
            // figure out what the starting score was by just subtracting everything
            var result = playerScore.score
            for (delta in playerScore.history) {
                result -= delta.value
            }
            return result
        }
    }
}
