package ovh.litapp.keepscore.ui.scoring

import android.content.res.Configuration
import ovh.litapp.keepscore.NewGameActivity.Companion.TYPICAL_MAX_NUMBER_OF_PLAYERS

/** The two scoreboard layouts: a single column of players, or the landscape grid. */
enum class ScoringOrientation {
    PORTRAIT,
    LANDSCAPE,
}

/** How many players sit side by side in a landscape row. */
private const val LANDSCAPE_COLUMNS = 2

/**
 * The layout to use for a device orientation, given as
 * [android.content.res.Configuration.orientation]. Games with more than
 * [TYPICAL_MAX_NUMBER_OF_PLAYERS] always use the scrollable column, even in landscape, so their
 * player cards remain usable.
 */
fun scoringOrientationFor(configurationOrientation: Int, numPlayers: Int): ScoringOrientation =
    if (configurationOrientation == Configuration.ORIENTATION_LANDSCAPE
        && numPlayers <= TYPICAL_MAX_NUMBER_OF_PLAYERS
    ) {
        ScoringOrientation.LANDSCAPE
    } else {
        ScoringOrientation.PORTRAIT
    }

/**
 * The players of each row of the scoreboard, one list per row, in display order, for a game of
 * [numPlayers] players laid out in [orientation].
 *
 * Portrait stacks one player per row, however many there are. Landscape fills [LANDSCAPE_COLUMNS]
 * players per row, which is the two-column grid `layout-land/player_score.xml` drew; the caller
 * switches larger games to portrait before building rows.
 *
 * A `null` is an empty cell. The legacy grid left the odd cell of a half-filled row invisible, and
 * the empty cell keeps the lone player of such a row in its column instead of stretching it across
 * the row.
 */
fun playerRows(numPlayers: Int, orientation: ScoringOrientation): List<List<Int?>> =
    when (orientation) {
        ScoringOrientation.PORTRAIT -> (0 until numPlayers).map { playerIndex -> listOf(playerIndex) }
        ScoringOrientation.LANDSCAPE -> (0 until numPlayers)
            .chunked(LANDSCAPE_COLUMNS)
            .map { row -> List(LANDSCAPE_COLUMNS) { column -> row.getOrNull(column) } }
    }

/**
 * How many history lines fit in a [availableHeightPx] tall column whose lines are
 * [lineHeightPx] high.
 */
fun maxHistoryLines(availableHeightPx: Float, lineHeightPx: Float): Int {
    if (availableHeightPx <= 0f || lineHeightPx <= 0f) {
        return 0
    }
    return (availableHeightPx / lineHeightPx).toInt()
}
