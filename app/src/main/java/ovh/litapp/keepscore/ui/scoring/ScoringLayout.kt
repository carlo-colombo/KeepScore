package ovh.litapp.keepscore.ui.scoring

import android.content.res.Configuration

/** The two scoreboard layouts: a single column of players, or the landscape grid. */
enum class ScoringOrientation {
    PORTRAIT,
    LANDSCAPE,
}

/** How many players sit side by side in a landscape row. */
private const val LANDSCAPE_COLUMNS = 2

/**
 * The layout to use for a device orientation, given as
 * [android.content.res.Configuration.orientation].
 *
 * The two game activities are locked to their orientation by the manifest, so this agrees with
 * which of them is running.
 */
fun scoringOrientationFor(configurationOrientation: Int): ScoringOrientation =
    if (configurationOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        ScoringOrientation.LANDSCAPE
    } else {
        ScoringOrientation.PORTRAIT
    }

/**
 * The players of each row of the scoreboard, one list per row, in display order, for a game of
 * [numPlayers] players laid out in [orientation].
 *
 * Portrait stacks one player per row, however many there are. Landscape fills [LANDSCAPE_COLUMNS]
 * players per row, which is the two-column grid `layout-land/player_score.xml` drew: a landscape
 * game never has more than eight players, because a bigger game is always opened in portrait.
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
