package ovh.litapp.keepscore.ui.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * The neutral button of the legacy dialogs, next to the negative one.
 *
 * Material3's `AlertDialog` has a single dismiss slot, while the legacy delta and colour dialogs
 * had a neutral button (`Customize…`, `Custom…`) and a Cancel button; this lays the two out in the
 * legacy order, to the left of OK.
 */
@Composable
internal fun NeutralWithDismissButton(
    neutralLabel: String,
    onNeutral: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
) {
    Row {
        TextButton(onClick = onNeutral) {
            Text(neutralLabel)
        }
        TextButton(onClick = onDismiss) {
            Text(dismissLabel)
        }
    }
}
