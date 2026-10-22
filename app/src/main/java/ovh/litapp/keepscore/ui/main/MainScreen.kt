package ovh.litapp.keepscore.ui.main

import android.content.Intent
import android.net.Uri
import android.text.style.URLSpan
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.data.TimePeriod
import ovh.litapp.keepscore.db.GameSummary
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.SdcardHelper
import ovh.litapp.keepscore.helper.SdcardHelper.Format
import ovh.litapp.keepscore.serialization.GamesBackupSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backup chosen for restore, including its original storage URI and compression format. */
data class BackupEntry(val summary: GamesBackupSummary, val uri: Uri, val format: Format)

enum class MainMenuAction { Backup, Restore, Share, ExportCsv, Settings, About, DeleteSelected, ShareSelected }
enum class MainGameAction { Delete, Copy, Rematch, History, Rename, Share }

sealed interface MainDialog {
    data object Welcome : MainDialog
    data object InitialRestore : MainDialog
    data class ConfirmBackup(val gameIds: List<Int>) : MainDialog
    data class ConfirmShare(val gameIds: List<Int>) : MainDialog
    data class ConfirmCsv(val gameIds: List<Int>) : MainDialog
    data class Delete(val gameIds: List<Int>) : MainDialog
    data class Rename(val game: GameSummary) : MainDialog
    data class ChooseBackup(val entries: List<BackupEntry>) : MainDialog
    data class ImportBackup(val summary: GamesBackupSummary, val uri: Uri) : MainDialog
    data class Success(val titleResId: Int, val message: String) : MainDialog
    data class Progress(val titleResId: Int, val current: Int, val maximum: Int) : MainDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    games: List<GameSummary>,
    selectedIds: Set<Int>,
    isLoading: Boolean,
    dialog: MainDialog?,
    onNewGame: () -> Unit,
    onOpenGame: (GameSummary) -> Unit,
    onToggleSelection: (GameSummary) -> Unit,
    onGameAction: (GameSummary, MainGameAction) -> Unit,
    onMenuAction: (MainMenuAction) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDialogConfirmed: (MainDialog, String?) -> Unit,
    onDialogDismissed: (MainDialog) -> Unit,
    onBackupChosen: (BackupEntry) -> Unit,
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    val moreOptionsDescription = stringResource(R.string.text_more_options)
    val selectedMode = selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.CONSTANT_app_name)) },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = { overflowExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = moreOptionsDescription
                        },
                    ) {
                        Text("⋮", fontSize = 24.sp)
                    }
                    DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                        val actions = if (selectedMode) {
                            listOf(MainMenuAction.DeleteSelected, MainMenuAction.ShareSelected)
                        } else {
                            listOf(
                                MainMenuAction.Backup,
                                MainMenuAction.Restore,
                                MainMenuAction.Share,
                                MainMenuAction.ExportCsv,
                                MainMenuAction.Settings,
                                MainMenuAction.About,
                            )
                        }
                        actions.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(menuActionLabel(action)) },
                                onClick = {
                                    overflowExpanded = false
                                    onMenuAction(action)
                                },
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Button(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.button_new_game), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = stringResource(R.string.text_saved_games),
                color = colorResource(R.color.emphasis),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            when {
                isLoading && games.isEmpty() -> Text(
                    text = stringResource(R.string.text_loading_generic),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                games.isEmpty() -> Text(
                    text = stringResource(R.string.text_no_saved_games),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    val sections = organizeGamesByTimePeriod(games)
                    sections.forEach { (period, sectionGames) ->
                        item(key = "section-${period.name}") {
                            Text(
                                text = stringResource(period.titleResId),
                                color = colorResource(R.color.emphasis),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 5.dp),
                            )
                        }
                        items(sectionGames, key = { it.id }) { game ->
                            SavedGameRow(
                                game = game,
                                checked = game.id in selectedIds,
                                onOpen = { onOpenGame(game) },
                                onToggleSelection = { onToggleSelection(game) },
                                onAction = { onGameAction(game, it) },
                            )
                        }
                    }
                }
            }
            if (selectedMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.button_select_all), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = onDeselectAll, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.button_deselect_all), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (dialog != null) {
        MainDialogContent(
            dialog = dialog,
            onConfirm = { text -> onDialogConfirmed(dialog, text) },
            onDismiss = { onDialogDismissed(dialog) },
            onBackupChosen = onBackupChosen,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedGameRow(
    game: GameSummary,
    checked: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onAction: (MainGameAction) -> Unit,
) {
    var actionsExpanded by remember(game.id) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val separator = stringResource(R.string.text_game_name_separator)
    val datePattern = stringResource(R.string.date_format)
    val playerNames = game.playerNames.mapIndexed { index, name -> PlayerScore.toDisplayName(name, index, context).toString() }
    val title = buildString {
        if (!game.name.isNullOrEmpty()) {
            append(game.name)
            append(' ')
            append(separator)
            append(' ')
        }
        append(playerNames.joinToString(", "))
    }
    val dateText = remember(game.dateSaved, datePattern) {
        SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(game.dateSaved))
    }
    val roundsText = stringResource(
        if (game.numRounds == 1) R.string.text_format_rounds_singular else R.string.text_format_rounds,
        game.numRounds,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { actionsExpanded = true },
            )
            .semantics { contentDescription = title },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (checked) 3.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = game.playerNames.size.toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = roundsText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(text = dateText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.semantics { contentDescription = title },
            )
        }
        DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
            val actionLabels = listOf(
                MainGameAction.Delete to R.string.text_delete,
                MainGameAction.Copy to R.string.text_copy,
                MainGameAction.Rematch to R.string.menu_rematch,
                MainGameAction.History to R.string.menu_history,
                MainGameAction.Rename to if (game.name.isNullOrEmpty()) R.string.title_name_game else R.string.title_edit_game_name,
                MainGameAction.Share to R.string.menu_share,
            )
            actionLabels.forEach { (action, labelResId) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelResId)) },
                    onClick = {
                        actionsExpanded = false
                        onAction(action)
                    },
                )
            }
        }
    }
}

@Composable
private fun menuActionLabel(action: MainMenuAction): String = when (action) {
    MainMenuAction.Backup -> stringResource(R.string.menu_save_backup)
    MainMenuAction.Restore -> stringResource(R.string.menu_load_backup)
    MainMenuAction.Share, MainMenuAction.ShareSelected -> stringResource(R.string.menu_share)
    MainMenuAction.ExportCsv -> stringResource(R.string.menu_export_to_spreadsheet)
    MainMenuAction.Settings -> stringResource(R.string.menu_settings)
    MainMenuAction.About -> stringResource(R.string.menu_about)
    MainMenuAction.DeleteSelected -> stringResource(R.string.text_delete)
}

@Composable
private fun MainDialogContent(
    dialog: MainDialog,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
    onBackupChosen: (BackupEntry) -> Unit,
) {
    when (dialog) {
        MainDialog.Welcome -> {
            val html = stringResource(
                R.string.text_welcome_message,
                stringResource(R.string.CONSTANT_link_donate),
                stringResource(R.string.CONSTANT_link_translate),
            )
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.text_welcome_title)) },
                text = { WelcomeHtmlText(html) },
                confirmButton = {
                    TextButton(onClick = { onConfirm(null) }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {},
            )
        }
        MainDialog.InitialRestore -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.menu_load_backup)) },
            text = { Text(stringResource(R.string.text_restore_initial)) },
            confirmButton = { TextButton(onClick = { onConfirm(null) }) { Text(stringResource(android.R.string.ok)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        )
        is MainDialog.ConfirmBackup -> ConfirmationDialog(
            title = stringResource(R.string.menu_save_backup),
            message = pluralStringResource(R.plurals.text_save_backup, dialog.gameIds.size, dialog.gameIds.size),
            onConfirm = { onConfirm(null) },
            onDismiss = onDismiss,
        )
        is MainDialog.ConfirmShare -> ConfirmationDialog(
            title = stringResource(R.string.title_confirm),
            message = pluralStringResource(R.plurals.text_share_with_friend, dialog.gameIds.size, dialog.gameIds.size),
            onConfirm = { onConfirm(null) },
            onDismiss = onDismiss,
        )
        is MainDialog.ConfirmCsv -> ConfirmationDialog(
            title = stringResource(R.string.title_confirm),
            message = pluralStringResource(
                R.plurals.text_share_spreadsheet_confirm,
                dialog.gameIds.size,
                dialog.gameIds.size,
            ),
            onConfirm = { onConfirm(null) },
            onDismiss = onDismiss,
        )
        is MainDialog.Delete -> {
            val message = if (dialog.gameIds.size == 1) stringResource(R.string.text_game_will_be_deleted)
            else stringResource(R.string.text_games_will_be_deleted, dialog.gameIds.size)
            ConfirmationDialog(
                title = stringResource(R.string.title_confirm_delete),
                message = message,
                onConfirm = { onConfirm(null) },
                onDismiss = onDismiss,
            )
        }
        is MainDialog.Rename -> {
            var name by remember(dialog.game.id) { mutableStateOf(dialog.game.name.orEmpty()) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(if (dialog.game.name.isNullOrEmpty()) R.string.title_name_game else R.string.title_edit_game_name)) },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.hint_game_name)) },
                        singleLine = true,
                    )
                },
                confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text(stringResource(android.R.string.ok)) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
            )
        }
        is MainDialog.ChooseBackup -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.title_choose_backup)) },
            text = {
                LazyColumn {
                    items(dialog.entries) { entry ->
                        BackupSummaryRow(entry.summary, onClick = { onBackupChosen(entry) })
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        )
        is MainDialog.ImportBackup -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.title_choose_backup)) },
            text = { BackupSummaryRow(dialog.summary, onClick = { onConfirm(null) }) },
            confirmButton = { TextButton(onClick = { onConfirm(null) }) { Text(stringResource(android.R.string.ok)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        )
        is MainDialog.Success -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(dialog.titleResId)) },
            text = { Text(dialog.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        )
        is MainDialog.Progress -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(dialog.titleResId)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (dialog.maximum <= 0) 0f else dialog.current.toFloat() / dialog.maximum },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${dialog.current} / ${dialog.maximum}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun WelcomeHtmlText(html: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val text = remember(html, linkColor) {
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val links = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        buildAnnotatedString {
            append(spanned.toString())
            links.forEach { link ->
                val start = spanned.getSpanStart(link)
                val end = spanned.getSpanEnd(link)
                addStringAnnotation("url", link.url, start, end)
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start,
                    end,
                )
            }
        }
    }
    ClickableText(text = text, onClick = { offset ->
        text.getStringAnnotations("url", offset, offset + 1).firstOrNull()?.let { annotation ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
        }
    })
}

@Composable
private fun ConfirmationDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun BackupSummaryRow(summary: GamesBackupSummary, onClick: () -> Unit) {
    val datePattern = stringResource(R.string.text_backup_date_format)
    val date = remember(summary.dateSaved, datePattern) {
        SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(summary.dateSaved))
    }
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(if (summary.isAutomatic) R.string.text_backup_automatic else R.string.text_backup_manual),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(text = date, color = MaterialTheme.colorScheme.onSurface)
            Text(text = summary.filename.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = summary.gameCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

