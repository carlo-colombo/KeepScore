package ovh.litapp.keepscore.db

import java.util.Comparator
import ovh.litapp.keepscore.util.CollectionUtil
import ovh.litapp.keepscore.util.Functions
import ovh.litapp.keepscore.util.LongUtil

/**
 * Smaller POJO representing a "Game" object. Designed so that the main page, with its list of all
 * games, can be very fast.
 *
 * @author nolan
 */
data class GameSummary(
    var id: Int = -1,
    var name: String? = null,
    var playerNames: List<String> = emptyList(),
    var numRounds: Int = 0,
    var dateSaved: Long = 0,
) {

    companion object {

        @JvmStatic
        fun byRecentlySaved(): Comparator<GameSummary> =
            Comparator { object1, object2 -> LongUtil.compare(object2.dateSaved, object1.dateSaved) }

        @JvmStatic
        fun fromGame(game: Game): GameSummary {
            val result = GameSummary()
            result.id = game.id
            result.name = game.name
            result.dateSaved = game.dateSaved
            result.playerNames = CollectionUtil.transform(
                game.playerScores.orEmpty(),
                CollectionUtil.Function { obj: PlayerScore -> obj.name.orEmpty() },
            )
            result.numRounds = CollectionUtil.max(
                game.playerScores.orEmpty(),
                Functions.PLAYER_SCORE_TO_HISTORY_SIZE,
            ) ?: 0
            return result
        }

        @JvmField
        val GET_ID: CollectionUtil.Function<GameSummary, Int> =
            CollectionUtil.Function { obj -> obj.id }
    }
}
