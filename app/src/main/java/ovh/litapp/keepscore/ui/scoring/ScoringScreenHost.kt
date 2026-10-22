package ovh.litapp.keepscore.ui.scoring

import androidx.activity.compose.setContent
import ovh.litapp.keepscore.GameActivity

/**
 * Puts the [ScoringScreen] of [state] into the given game activity.
 *
 * A composable can only be called from Kotlin, so this is the one thing [GameActivity] needs from
 * this package. The activity keeps [ScoringScreenState] for the life of the game and calls this
 * once, from `onCreate`; [menuActions] carries the commands the scoreboard menu offers back to it.
 */
fun GameActivity.setScoringScreenContent(
    state: ScoringScreenState,
    menuActions: ScoringMenuActions,
) {
    setContent {
        ScoringScreen(state, menuActions)
    }
}
