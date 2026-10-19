package ovh.litapp.keepscore.ui.nameplayers

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PlayerColor.BuiltInPlayerColor
import ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor
import ovh.litapp.keepscore.helper.PlayerNameHelper
import java.util.Locale

@Composable
fun NamePlayersScreen(
    numPlayers: Int,
    showColors: Boolean,
    onStartGame: (Array<String>, Array<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var playerNames by rememberSaveable(numPlayers) {
        mutableStateOf(List(numPlayers) { "" })
    }
    var playerColors by rememberSaveable(numPlayers) {
        mutableStateOf(NamePlayersModel.defaultPlayerColorSerializations(numPlayers))
    }
    var colorDialogPlayerIndex by rememberSaveable { mutableIntStateOf(-1) }
    var selectedColorSerialization by rememberSaveable { mutableStateOf("") }
    var showCustomColorDialog by rememberSaveable { mutableStateOf(false) }
    var customColorInput by rememberSaveable { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }

    val context = LocalContext.current
    LaunchedEffect(context) {
        suggestions = withContext(Dispatchers.IO) {
            PlayerNameHelper.getPlayerNameSuggestions(context.applicationContext)
        }
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val background = Brush.verticalGradient(
        listOf(colorResource(R.color.calc_background), colorResource(R.color.calc_surface)),
    )
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.text_player_names),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (landscape) 2 else 1),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(numPlayers, key = { index -> "player-$index" }) { index ->
                PlayerNameRow(
                    index = index,
                    isLastPlayer = index == numPlayers - 1,
                    name = playerNames[index],
                    colorSerialization = playerColors[index],
                    showColor = showColors,
                    suggestions = suggestions,
                    focusManager = focusManager,
                    onNameChanged = { updatedName ->
                        playerNames = playerNames.toMutableList().apply { this[index] = updatedName }
                    },
                    onColorClick = {
                        colorDialogPlayerIndex = index
                        selectedColorSerialization = playerColors[index]
                    },
                )
            }
        }

        Button(
            onClick = { onStartGame(playerNames.toTypedArray(), playerColors.toTypedArray()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Text(
                text = stringResource(android.R.string.ok),
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (colorDialogPlayerIndex >= 0) {
        PlayerColorDialog(
            selectedColorSerialization = selectedColorSerialization,
            onDismiss = {
                colorDialogPlayerIndex = -1
                showCustomColorDialog = false
            },
            onColorSelected = { selectedColor ->
                playerColors = playerColors.toMutableList().apply {
                    this[colorDialogPlayerIndex] = PlayerColor.serialize(selectedColor)
                }
                colorDialogPlayerIndex = -1
            },
            onCustomColorRequested = { currentColor ->
                customColorInput = formatArgbHex(currentColor, context)
                showCustomColorDialog = true
            },
        )

        if (showCustomColorDialog) {
            CustomPlayerColorDialog(
                initialValue = customColorInput,
                onValueChanged = { customColorInput = it },
                onDismiss = { showCustomColorDialog = false },
                onColorSelected = { color ->
                    selectedColorSerialization = PlayerColor.serialize(color)
                    playerColors = playerColors.toMutableList().apply {
                        this[colorDialogPlayerIndex] = selectedColorSerialization
                    }
                    showCustomColorDialog = false
                    colorDialogPlayerIndex = -1
                },
            )
        }
    }
}

@Composable
private fun PlayerNameRow(
    index: Int,
    isLastPlayer: Boolean,
    name: String,
    colorSerialization: String,
    showColor: Boolean,
    suggestions: List<String>,
    focusManager: FocusManager,
    onNameChanged: (String) -> Unit,
    onColorClick: () -> Unit,
) {
    val context = LocalContext.current
    val playerColor = remember(colorSerialization) { PlayerColor.deserialize(colorSerialization) }
    val playerLabel = "${stringResource(R.string.text_player)} ${index + 1}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showColor) {
            PlayerColorSwatch(
                color = playerColor,
                description = "$playerLabel color, ${colorDescription(playerColor)}",
                onClick = onColorClick,
            )
        }

        var hasFocus by remember { mutableStateOf(false) }
        var suggestionsDismissed by remember { mutableStateOf(false) }
        val matchingSuggestions = remember(name, suggestions) {
            if (name.isEmpty()) emptyList() else suggestions
                .asSequence()
                .filter { it.startsWith(name, ignoreCase = true) }
                .take(8)
                .toList()
        }
        val keyboardController = LocalSoftwareKeyboardController.current
        val imeAction = if (isLastPlayer) ImeAction.Done else ImeAction.Next

        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    onNameChanged(it)
                    suggestionsDismissed = false
                },
                label = { Text(playerLabel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        hasFocus = state.isFocused
                        if (state.isFocused) suggestionsDismissed = false
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    onDone = { keyboardController?.hide() },
                ),
            )

            DropdownMenu(
                expanded = hasFocus && !suggestionsDismissed && matchingSuggestions.isNotEmpty(),
                onDismissRequest = { suggestionsDismissed = true },
                modifier = Modifier.heightIn(max = 240.dp),
            ) {
                matchingSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onNameChanged(suggestion)
                            suggestionsDismissed = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerColorSwatch(
    color: PlayerColor,
    description: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = description
            selected = true
        },
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(color.toColor(context)))
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

@Composable
private fun PlayerColorDialog(
    selectedColorSerialization: String,
    onDismiss: () -> Unit,
    onColorSelected: (PlayerColor) -> Unit,
    onCustomColorRequested: (PlayerColor) -> Unit,
) {
    var pendingColorSerialization by rememberSaveable {
        mutableStateOf(selectedColorSerialization)
    }
    val context = LocalContext.current
    val colors = PlayerColor.BUILT_INS.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_choose_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 220.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(colors, key = { color -> color.getOrdinal() }) { color ->
                        val colorSerialization = PlayerColor.serialize(color)
                        val isSelected = pendingColorSerialization == colorSerialization
                        IconButton(
                            onClick = { pendingColorSerialization = colorSerialization },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = "Color ${color.getOrdinal() + 1}"
                                    selected = isSelected
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(color.toColor(context)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
                TextButton(onClick = {
                    onCustomColorRequested(PlayerColor.deserialize(pendingColorSerialization))
                }) {
                    Text(stringResource(R.string.button_custom))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(PlayerColor.deserialize(pendingColorSerialization))
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun CustomPlayerColorDialog(
    initialValue: String,
    onValueChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onColorSelected: (PlayerColor) -> Unit,
) {
    var colorText by rememberSaveable { mutableStateOf(initialValue) }
    val color = customColorFromHex(colorText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.button_custom)) },
        text = {
            Column {
                OutlinedTextField(
                    value = colorText,
                    onValueChange = {
                        colorText = it
                        onValueChanged(it)
                    },
                    label = { Text("ARGB or RGB hex color") },
                    placeholder = { Text("#FF336699") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                )
                if (colorText.isNotEmpty() && color == null) {
                    Text(
                        text = "Use #RRGGBB or #AARRGGBB",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                color?.let { customColor ->
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(customColor.getColor()))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = color != null,
                onClick = { color?.let(onColorSelected) },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun colorDescription(color: PlayerColor): String = when (color) {
    is BuiltInPlayerColor -> "palette color ${color.ordinal + 1}"
    is CustomPlayerColor -> "custom color"
    else -> "selected color"
}

private fun formatArgbHex(color: PlayerColor, context: android.content.Context): String =
    String.format(Locale.ROOT, "#%08X", color.toColor(context))

private fun customColorFromHex(value: String): CustomPlayerColor? {
    if (!value.matches(Regex("#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})"))) return null
    return try {
        CustomPlayerColor(value.toColorInt())
    } catch (_: IllegalArgumentException) {
        null
    }
}
