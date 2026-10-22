package ovh.litapp.keepscore.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ovh.litapp.keepscore.R

/** What the user picked in the history dialog. */
enum class HistoryAction {
    /** Accept the history entry that is still being batched into. */
    CONFIRM,

    /** Drop the last history entry. */
    DELETE_LAST,

    /** Add a history entry of zero. */
    ADD_ZERO,
}

/**
 * The dialog behind a long-press on a player's history or score: accept the entry that is still
 * open, delete the last entry, or add a zero entry.
 *
 * @param showConfirm whether the last entry can still be accepted, i.e. whether it is still being
 *            batched into
 * @param hasHistory whether there is a last entry to delete
 * @param onResult the action the user picked; picking one dismisses the dialog
 */
@Composable
fun HistoryContextDialog(
    showConfirm: Boolean,
    hasHistory: Boolean,
    onDismissRequest: () -> Unit,
    onResult: (HistoryAction) -> Unit,
) {
    val items = buildList {
        if (showConfirm) {
            add(R.string.text_confirm to HistoryAction.CONFIRM)
        }
        // can't undo the last entry if there is no history
        if (hasHistory) {
            add(R.string.text_undo_last to HistoryAction.DELETE_LAST)
        }
        add(R.string.text_add_zero to HistoryAction.ADD_ZERO)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Column {
                items.forEach { (labelResId, action) ->
                    Text(
                        text = stringResource(labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onResult(action)
                                onDismissRequest()
                            }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                    )
                }
            }
        },
        // the legacy dialog was a plain list of items and had no buttons
        confirmButton = {},
    )
}
