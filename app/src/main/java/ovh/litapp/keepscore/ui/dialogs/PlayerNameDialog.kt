package ovh.litapp.keepscore.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PlayerNameHelper
import ovh.litapp.keepscore.util.StringUtil

/** The tallest the suggestion list gets before it scrolls. */
private val SuggestionListMaxHeight = 160.dp

/**
 * The dialog that names a player, for a new player and for renaming one.
 *
 * The field offers the names of players from previous games, filtered by what has been typed from
 * the first character on, the way the legacy `AutoCompleteTextView` did. [onConfirm] receives the
 * name, trimmed of nothing, since renaming and adding each decide what to do with blank input.
 *
 * @param newPlayerNumber the zero-based position the player will have, shown as the hint
 */
@Composable
fun PlayerNameDialog(
    @StringRes titleResId: Int,
    startingValue: String,
    newPlayerNumber: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(StringUtil.nullToEmpty(startingValue)) }
    // the legacy dialog fetched the suggestions in the background to avoid jankiness
    val suggestions by produceState(initialValue = emptyList<String>(), context) {
        value = withContext(Dispatchers.IO) { PlayerNameHelper.getPlayerNameSuggestions(context) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(titleResId)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("${stringResource(R.string.text_player)} ${newPlayerNumber + 1}") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                val matches = suggestionMatches(suggestions, name)
                if (matches.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = SuggestionListMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        matches.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { name = suggestion }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(StringUtil.nullToEmpty(name))
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

/**
 * The suggestions the legacy field offered for [text]: the previous players' names that start with
 * what has been typed, ignoring case, and none before the first character.
 */
internal fun suggestionMatches(suggestions: List<String>, text: String): List<String> {
    if (text.isEmpty()) {
        return emptyList()
    }
    return suggestions.filter { it.startsWith(text, ignoreCase = true) }
}
