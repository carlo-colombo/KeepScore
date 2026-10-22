package ovh.litapp.keepscore.ui.scoring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovh.litapp.keepscore.R

/**
 * Everything the scoreboard menu can do, as the game activity implements it.
 *
 * The scoreboard hides the activity's action bar, so its menu is drawn in Compose instead: without
 * it the only way to reach the history, the players, a rematch, undo/redo and the settings was the
 * hardware menu key, which most devices no longer have.
 */
interface ScoringMenuActions {
    fun showHistory()

    fun editPlayers()

    fun rematch()

    fun undo()

    fun redo()

    fun openSettings()
}

/**
 * The overflow menu of the scoreboard, in the top-right corner above the player cards.
 *
 * Undo and redo are disabled rather than hidden, so the menu does not change size while the user
 * is looking at it; the state is read when the menu opens, which is the only time it is visible.
 */
@Composable
fun ScoreboardMenu(
    state: ScoringScreenState,
    actions: ScoringMenuActions,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val moreOptions = stringResource(R.string.text_more_options)

    Box(modifier = modifier, contentAlignment = Alignment.TopEnd) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = moreOptions },
        ) {
            Text("\u22ee", fontSize = 24.sp, color = color)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_history)) },
                onClick = {
                    expanded = false
                    actions.showHistory()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_add_edit_players)) },
                onClick = {
                    expanded = false
                    actions.editPlayers()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_rematch)) },
                onClick = {
                    expanded = false
                    actions.rematch()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_undo)) },
                enabled = state.canUndo(),
                onClick = {
                    expanded = false
                    actions.undo()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_redo)) },
                enabled = state.canRedo(),
                onClick = {
                    expanded = false
                    actions.redo()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_settings)) },
                onClick = {
                    expanded = false
                    actions.openSettings()
                },
            )
        }
    }
}
