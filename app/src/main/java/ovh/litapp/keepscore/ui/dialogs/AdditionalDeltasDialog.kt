package ovh.litapp.keepscore.ui.dialogs

import android.content.Intent
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.SettingsActivity
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.util.IntegerUtil

/** How many shortcut buttons the legacy delta popup showed. */
private const val DELTA_SHORTCUT_COUNT = 4

/** The height the delta field and its shortcut buttons share. */
private val DeltaRowHeight = 48.dp

/**
 * The additional deltas dialog of a long-pressed + or - button: a field that takes a number or an
 * expression, the four configurable shortcut buttons and the settings shortcut.
 *
 * [onConfirm] receives the value the user entered, so a caller that opened the dialog for the minus
 * button negates it. The dialog confirms and dismisses on OK, on Go and on a typed `=`; a value
 * that does not evaluate keeps it open with the toast the legacy dialog showed.
 */
@Composable
fun AdditionalDeltasDialog(
    positive: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val context = LocalContext.current
    val shortcutValues = remember(context) {
        List(DELTA_SHORTCUT_COUNT) { index -> PreferenceHelper.getPopupDeltaButtonValue(index, context) }
    }
    // the legacy field opened with "0" highlighted, so the first keystroke replaced it
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(DELTA_INITIAL_VALUE, TextRange(0, DELTA_INITIAL_VALUE.length)))
    }
    val textSize = dialogFontSize(R.dimen.delta_popup_text)
    val isExpression = isDeltaExpression(fieldValue.text)

    val confirm = {
        val result = evaluateDeltaExpression(fieldValue.text)
        if (result == null) {
            Toast.makeText(context, R.string.toast_calc_error, Toast.LENGTH_SHORT).show()
        } else {
            onConfirm(IntegerUtil.parseIntOrZero(result))
            onDismissRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(if (positive) R.string.title_add else R.string.title_subtract)) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = dimensionResource(R.dimen.delta_popup_width)),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DeltaField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        val transition = applyDeltaInput(newValue.text)
                        fieldValue = if (transition.text == newValue.text) {
                            newValue
                        } else {
                            TextFieldValue(transition.text, TextRange(transition.text.length))
                        }
                        if (transition.calculationFailed) {
                            Toast.makeText(context, R.string.toast_calc_error, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCalculate = confirm,
                    textSize = textSize,
                    modifier = Modifier.weight(1f),
                )
                // the shortcuts edit a plain number, so the legacy field hid them for expressions
                if (!isExpression) {
                    shortcutValues.forEach { shortcut ->
                        DeltaShortcutButton(
                            label = IntegerUtil.toCharSequenceWithSign(shortcut).toString(),
                            textSize = textSize,
                            onClick = {
                                val value = IntegerUtil.parseIntOrZero(fieldValue.text) + shortcut
                                val text = Integer.toString(value)
                                fieldValue = TextFieldValue(text, TextRange(text.length))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = confirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            NeutralWithDismissButton(
                neutralLabel = stringResource(R.string.button_customize),
                onNeutral = {
                    onDismissRequest()
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                            .putExtra(SettingsActivity.EXTRA_SCROLL_TO_CONFIGURATIONS, true),
                    )
                },
                dismissLabel = stringResource(android.R.string.cancel),
                onDismiss = onDismissRequest,
            )
        },
    )
}

/** The field the delta dialog evaluates, with the legacy number pad and Go shortcut. */
@Composable
private fun DeltaField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onCalculate: () -> Unit,
    textSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(DeltaRowHeight)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = textSize, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onCalculate() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One of the four configurable shortcuts, which adds its value to the field. */
@Composable
private fun DeltaShortcutButton(
    label: String,
    textSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(DeltaRowHeight),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = textSize,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A dimension that is declared in `sp`, as a Compose font size. */
@Composable
private fun dialogFontSize(@DimenRes id: Int): TextUnit =
    with(LocalDensity.current) { LocalResources.current.getDimension(id).toSp() }
