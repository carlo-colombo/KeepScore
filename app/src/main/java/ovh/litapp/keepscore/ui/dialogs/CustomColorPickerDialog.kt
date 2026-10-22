package ovh.litapp.keepscore.ui.dialogs

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ovh.litapp.keepscore.R

/** The height of the preview of the colour being picked. */
private val ColorPreviewHeight = 48.dp

/**
 * The custom colour picker that replaced AmbilWarna: the hue, saturation and brightness sliders of
 * the colour, a preview of it and the hex value, which can also be typed.
 *
 * [onConfirm] receives the picked colour as an Android `Color` int, which the caller wraps in a
 * [ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor].
 */
@Composable
fun CustomColorPickerDialog(
    initialColor: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialHsv = remember(initialColor) { hsvOf(initialColor) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    // the typed hex is the user's own text until a slider changes the colour
    var hexText by remember { mutableStateOf(hexColorString(initialColor)) }
    val color = hsvColor(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ColorPreviewHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(color)),
                )
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        hexText = hexColorString(hsvColor(it, saturation, brightness))
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Slider(
                    value = saturation,
                    onValueChange = {
                        saturation = it
                        hexText = hexColorString(hsvColor(hue, it, brightness))
                    },
                    valueRange = 0f..1f,
                )
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        hexText = hexColorString(hsvColor(hue, saturation, it))
                    },
                    valueRange = 0f..1f,
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        val parsed = parseHexColor(text)
                        if (parsed != null) {
                            val hsv = hsvOf(parsed)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    singleLine = true,
                    placeholder = { Text(hexColorString(color)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(color)
                    onDismissRequest()
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/** The hue, saturation and brightness of an Android `Color` int, as [colorToHSV] reports them. */
internal fun hsvOf(color: Int): FloatArray = FloatArray(3).also { colorToHSV(color, it) }

/** The Android `Color` int of a hue in degrees and a saturation and brightness from 0 to 1. */
internal fun hsvColor(hue: Float, saturation: Float, brightness: Float): Int =
    HSVToColor(floatArrayOf(hue, saturation, brightness))
