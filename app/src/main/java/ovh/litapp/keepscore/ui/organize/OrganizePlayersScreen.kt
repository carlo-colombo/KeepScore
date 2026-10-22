package ovh.litapp.keepscore.ui.organize

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.ui.dialogs.ColorChooserDialog
import ovh.litapp.keepscore.ui.dialogs.PlayerNameDialog
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * The "Edit Players" screen.
 *
 * Shows the game's players in display order and lets the user rename them, pick their colour,
 * delete them, add players, sort by score, shuffle them and drag them into a new order. [onConfirm]
 * receives the players in their final order, which the host puts into the activity result;
 * [onCancel] is the cancel/back path and returns nothing.
 *
 * The name and colour dialogs are the shared Compose dialogs, so the player-name suggestions and
 * the 16-colour grid with its custom-colour picker behave exactly as they did before the migration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizePlayersScreen(
    initialPlayers: List<PlayerScore>,
    onConfirm: (List<PlayerScore>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val model = remember { OrganizePlayersModel(initialPlayers) }
    val showColors = remember { PreferenceHelper.getShowColors(context) }
    val initialScore = remember {
        PreferenceHelper.getIntPreference(
            R.string.CONSTANT_pref_initial_score,
            R.string.CONSTANT_pref_initial_score_default,
            context,
        ).toLong()
    }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        model.movePlayer(from.index, to.index)
    }
    var deletedNamesToWarnAbout by remember { mutableStateOf<List<String>?>(null) }
    var nameDialog by remember { mutableStateOf<PlayerNameRequest?>(null) }
    var colorDialogPlayer by remember { mutableStateOf<OrganizePlayer?>(null) }

    val confirm = {
        val deletedNames = model.deletedPlayersWithHistory.map { it.toDisplayName(context).toString() }
        if (deletedNames.isEmpty()) {
            onConfirm(model.toResultList())
        } else {
            deletedNamesToWarnAbout = deletedNames
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_add_edit_players)) },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) {
                        Text(text = "\u2190", fontSize = 24.sp)
                    }
                },
                actions = {
                    if (model.canAddPlayer) {
                        IconButton(
                            onClick = {
                                nameDialog = PlayerNameRequest(
                                    titleResId = R.string.title_add_player,
                                    startingValue = "",
                                    playerNumber = model.players.size,
                                    onConfirm = { name -> model.addPlayer(name.trim(), initialScore) },
                                )
                            },
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_action_user_add),
                                contentDescription = stringResource(R.string.menu_add_player),
                            )
                        }
                    }
                    IconButton(onClick = model::sortByScore) {
                        Image(
                            painter = painterResource(R.drawable.action_sort_by_size),
                            contentDescription = stringResource(R.string.menu_sort_players),
                        )
                    }
                    IconButton(onClick = { model.shuffle() }) {
                        Image(
                            painter = painterResource(R.drawable.ic_action_shuffle),
                            contentDescription = stringResource(R.string.menu_randomize),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()) {
                Button(onClick = confirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(android.R.string.ok))
                }
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(model.players, key = { it.key }) { player ->
                ReorderableItem(state = reorderableState, key = player.key) { isDragging ->
                    PlayerRow(
                        player = player,
                        showColor = showColors,
                        canDelete = model.canDeletePlayer,
                        isDragging = isDragging,
                        onRename = {
                            nameDialog = PlayerNameRequest(
                                titleResId = R.string.title_change_name,
                                startingValue = player.name,
                                playerNumber = player.playerNumber,
                                onConfirm = player::rename,
                            )
                        },
                        onChooseColor = { colorDialogPlayer = player },
                        onDelete = { model.deletePlayer(player) },
                    )
                }
            }
        }
    }

    deletedNamesToWarnAbout?.let { deletedNames ->
        DeletePlayersDialog(
            deletedNames = deletedNames,
            onConfirm = { onConfirm(model.toResultList()) },
            onDismiss = { deletedNamesToWarnAbout = null },
        )
    }

    nameDialog?.let { request ->
        PlayerNameDialog(
            titleResId = request.titleResId,
            startingValue = request.startingValue,
            newPlayerNumber = request.playerNumber,
            onDismissRequest = { nameDialog = null },
            onConfirm = { name ->
                request.onConfirm(name)
                nameDialog = null
            },
        )
    }
    colorDialogPlayer?.let { player ->
        ColorChooserDialog(
            selectedColor = player.playerColor,
            onDismissRequest = { colorDialogPlayer = null },
            onColorSelected = { color ->
                player.recolor(color)
                colorDialogPlayer = null
            },
        )
    }
}

@Composable
private fun ReorderableCollectionItemScope.PlayerRow(
    player: OrganizePlayer,
    showColor: Boolean,
    canDelete: Boolean,
    isDragging: Boolean,
    onRename: () -> Unit,
    onChooseColor: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isDragging) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .draggableHandle(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.icmpmove),
                contentDescription = null,
            )
        }
        Text(
            text = "#${player.playerNumber + 1}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (showColor) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(player.playerColor.toColor(context)))
                    .clickable(onClick = onChooseColor),
            )
        }
        Text(
            text = PlayerScore.toDisplayName(player.name, player.playerNumber, context).toString(),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onRename)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = player.playerScore.score.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = onRename) {
            Image(
                painter = painterResource(R.drawable.ic_menu_edit),
                contentDescription = stringResource(R.string.text_edit_name),
            )
        }
        IconButton(onClick = onDelete, enabled = canDelete) {
            Image(
                painter = painterResource(R.drawable.ic_menu_delete),
                contentDescription = stringResource(R.string.text_delete),
                modifier = Modifier.alpha(if (canDelete) 1f else 0.3f),
            )
        }
    }
}

@Composable
private fun DeletePlayersDialog(
    deletedNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        // The legacy warning could not be dismissed without choosing a button.
        onDismissRequest = {},
        title = { Text(stringResource(R.string.title_confirm_delete)) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.text_confirm_delete_player,
                    deletedNames.size,
                    deletedNames.joinToString(", "),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

/**
 * The name dialog an add or rename action opened, so the screen renders it from its own state
 * rather than building an `AlertDialog` on the spot; [onConfirm] applies the name to the player.
 */
private class PlayerNameRequest(
    @StringRes val titleResId: Int,
    val startingValue: String,
    val playerNumber: Int,
    val onConfirm: (String) -> Unit,
)
