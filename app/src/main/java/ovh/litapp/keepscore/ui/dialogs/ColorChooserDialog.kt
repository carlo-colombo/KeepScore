package ovh.litapp.keepscore.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PlayerColor

/** How many colours the legacy chooser showed per row. */
private const val COLORS_PER_ROW = 4

/**
 * The dialog that picks a player's colour: the built-in palette plus, through the neutral button,
 * a custom colour.
 *
 * Tapping a swatch only marks it as the colour being browsed, the way the legacy grid's selection
 * did; [onColorSelected] receives the swatch the user confirmed with OK, or the custom colour,
 * after which the dialog is dismissed.
 *
 * @param selectedColor the colour the player has now, which starts out selected
 */
@Composable
fun ColorChooserDialog(
    selectedColor: PlayerColor,
    onDismissRequest: () -> Unit,
    onColorSelected: (PlayerColor) -> Unit,
) {
    val context = LocalContext.current
    var browsedColor by remember(selectedColor) { mutableStateOf(selectedColor) }
    // the legacy custom picker opened over the grid and came back to it on cancel
    var customColorPickerVisible by remember { mutableStateOf(false) }

    if (customColorPickerVisible) {
        CustomColorPickerDialog(
            // the legacy picker started from the colour the player had, not the one being browsed
            initialColor = selectedColor.toColor(context),
            onDismissRequest = { customColorPickerVisible = false },
            onConfirm = { color -> onColorSelected(PlayerColor.CustomPlayerColor(color)) },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.title_choose_color)) },
            text = {
                Column {
                    PlayerColor.BUILT_INS.toList().chunked(COLORS_PER_ROW).forEach { rowColors ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowColors.forEach { color ->
                                ColorSwatch(
                                    color = color,
                                    selected = color == browsedColor,
                                    onClick = { browsedColor = color },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .aspectRatio(1f),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onColorSelected(browsedColor) }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                NeutralWithDismissButton(
                    neutralLabel = stringResource(R.string.button_custom),
                    onNeutral = { customColorPickerVisible = true },
                    dismissLabel = stringResource(android.R.string.cancel),
                    onDismiss = onDismissRequest,
                )
            },
        )
    }
}

/** One colour of the palette, drawn with the border the legacy selector drew when selected. */
@Composable
private fun ColorSwatch(
    color: PlayerColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(color.toColor(context)))
            .then(
                if (selected) {
                    Modifier.border(
                        dimensionResource(R.dimen.color_selector_edge),
                        colorResource(R.color.player_color_selector_edge),
                        shape,
                    )
                } else {
                    Modifier
                },
            )
            .selectable(selected = selected, onClick = onClick),
    )
}
