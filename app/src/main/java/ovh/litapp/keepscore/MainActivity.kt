package ovh.litapp.keepscore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import ovh.litapp.keepscore.android.EdgeToEdge
import ovh.litapp.keepscore.data.LoadGamesBackupResult
import ovh.litapp.keepscore.data.TimePeriod
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.GameDBHelper
import ovh.litapp.keepscore.db.GameSummary
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.GameActivityHelper
import ovh.litapp.keepscore.helper.MailHelper
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.helper.SdcardHelper
import ovh.litapp.keepscore.helper.SdcardHelper.Format
import ovh.litapp.keepscore.helper.SdcardHelper.Location
import ovh.litapp.keepscore.helper.ToastHelper
import ovh.litapp.keepscore.serialization.GamesBackup
import ovh.litapp.keepscore.serialization.GamesBackupSerializer
import ovh.litapp.keepscore.serialization.GamesBackupSummary
import ovh.litapp.keepscore.ui.main.BackupEntry
import ovh.litapp.keepscore.ui.main.MainDialog
import ovh.litapp.keepscore.ui.main.MainGameAction
import ovh.litapp.keepscore.ui.main.MainMenuAction
import ovh.litapp.keepscore.ui.main.MainScreen
import ovh.litapp.keepscore.ui.theme.KeepScoreTheme
import ovh.litapp.keepscore.util.UtilLogger
import java.io.File
import java.util.Date
import java.util.concurrent.Executors

/** Compose host for the saved-games home screen. */
class MainActivity : AppCompatActivity() {
    private val log = UtilLogger(MainActivity::class.java)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var games by mutableStateOf<List<GameSummary>>(emptyList())
    private var selectedIds by mutableStateOf<Set<Int>>(emptySet())
    private var isLoading by mutableStateOf(true)
    private var dialog by mutableStateOf<MainDialog?>(null)
    private var gamesLoaded = false
    private var restoreCheckPending = false
    private var gamesLoadId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        EdgeToEdge.apply(this, false)
        savedInstanceState?.getIntArray(STATE_SELECTED_IDS)?.let { ids -> selectedIds = ids.toSet() }

        setContent {
            KeepScoreTheme {
                MainScreen(
                    games = games,
                    selectedIds = selectedIds,
                    isLoading = isLoading,
                    dialog = dialog,
                    onNewGame = { startActivity(Intent(this, NewGameActivity::class.java)) },
                    onOpenGame = { GameActivityHelper.openGame(this, it) },
                    onToggleSelection = ::toggleSelection,
                    onGameAction = ::onGameAction,
                    onMenuAction = ::onMenuAction,
                    onSelectAll = { selectedIds = games.map { game -> game.id }.toSet() },
                    onDeselectAll = { selectedIds = emptySet() },
                    onDialogConfirmed = ::onDialogConfirmed,
                    onDialogDismissed = ::onDialogDismissed,
                    onBackupChosen = ::onDialogBackupChosen,
                )
            }
        }

        if (PreferenceHelper.getBooleanPreference(
                R.string.CONSTANT_pref_initial_message,
                R.string.CONSTANT_pref_initial_message_default,
                this,
            )
        ) {
            dialog = MainDialog.Welcome
        }
        loadBackupFileFromShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadBackupFileFromShare(intent)
    }

    override fun onResume() {
        super.onResume()
        loadGamesInBackground()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray(STATE_SELECTED_IDS, selectedIds.toIntArray())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun loadGamesInBackground() {
        val loadId = ++gamesLoadId
        isLoading = true
        runBackground(
            work = {
                val helper = GameDBHelper(this)
                try {
                    helper.findAllGameSummaries().sortedWith(GameSummary.byRecentlySaved())
                } finally {
                    helper.close()
                }
            },
            onResult = { loadedGames ->
                if (loadId != gamesLoadId) return@runBackground
                games = loadedGames
                selectedIds = selectedIds.intersect(loadedGames.map { it.id }.toSet())
                isLoading = false
                gamesLoaded = true
                if (restoreCheckPending) {
                    restoreCheckPending = false
                    showLoadBackupDialogIfEmpty()
                }
            },
            onError = {
                if (loadId == gamesLoadId) isLoading = false
                log.e(it as? Exception ?: RuntimeException(it), "Unable to load saved games")
            },
        )
    }

    private fun toggleSelection(game: GameSummary) {
        selectedIds = if (game.id in selectedIds) selectedIds - game.id else selectedIds + game.id
    }

    private fun onMenuAction(action: MainMenuAction) {
        when (action) {
            MainMenuAction.Backup -> {
                if (games.isEmpty()) {
                    ToastHelper.showShort(this, R.string.toast_no_saved_games)
                } else {
                    dialog = MainDialog.ConfirmBackup(games.map { it.id })
                }
            }
            MainMenuAction.Restore -> showLoadBackupDialog()
            MainMenuAction.Share -> dialog = MainDialog.ConfirmShare(games.map { it.id })
            MainMenuAction.ExportCsv -> dialog = MainDialog.ConfirmCsv(games.map { it.id })
            MainMenuAction.Settings -> startActivity(Intent(this, SettingsActivity::class.java))
            MainMenuAction.About -> startActivity(Intent(this, AboutActivity::class.java))
            MainMenuAction.DeleteSelected -> dialog = MainDialog.Delete(selectedGames().map { it.id })
            MainMenuAction.ShareSelected -> dialog = MainDialog.ConfirmShare(selectedGames().map { it.id })
        }
    }

    private fun onGameAction(game: GameSummary, action: MainGameAction) {
        when (action) {
            MainGameAction.Delete -> dialog = MainDialog.Delete(listOf(game.id))
            MainGameAction.Copy -> copyGame(game, resetScores = false)
            MainGameAction.Rematch -> copyGame(game, resetScores = true)
            MainGameAction.History -> findGame(game) { loaded ->
                val historyIntent = Intent(this, HistoryActivity::class.java)
                historyIntent.putExtra(HistoryActivity.EXTRA_GAME, loaded)
                startActivity(historyIntent)
            }
            MainGameAction.Rename -> dialog = MainDialog.Rename(game)
            MainGameAction.Share -> dialog = MainDialog.ConfirmShare(listOf(game.id))
        }
    }

    private fun onDialogConfirmed(confirmed: MainDialog, text: String?) {
        dialog = null
        when (confirmed) {
            MainDialog.Welcome -> {
                PreferenceHelper.setBooleanPreference(R.string.CONSTANT_pref_initial_message, false, this)
                if (gamesLoaded) showLoadBackupDialogIfEmpty() else restoreCheckPending = true
            }
            MainDialog.InitialRestore -> showLoadBackupDialog()
            is MainDialog.ConfirmBackup -> saveBackup(Format.GZIP, Location.Backups, confirmed.gameIds, false)
            is MainDialog.ConfirmShare -> saveBackup(Format.XML, Location.Shares, confirmed.gameIds, true)
            is MainDialog.ConfirmCsv -> exportToSpreadsheet(confirmed.gameIds)
            is MainDialog.Delete -> deleteGames(confirmed.gameIds)
            is MainDialog.Rename -> renameGame(confirmed.game, text.orEmpty())
            is MainDialog.ChooseBackup -> Unit
            is MainDialog.ImportBackup -> {
                confirmedUriIntents += confirmed.uri.toString()
                loadBackup(confirmed.summary, confirmed.uri, Format.XML)
            }
            is MainDialog.Success, is MainDialog.Progress -> Unit
        }
    }

    private fun onDialogDismissed(dismissed: MainDialog) {
        dialog = null
        if (dismissed is MainDialog.ImportBackup) confirmedUriIntents += dismissed.uri.toString()
    }

    private fun showLoadBackupDialogIfEmpty() {
        if (games.isNotEmpty()) return
        runBackground(
            work = { SdcardHelper.isAvailable() && SdcardHelper.list(Location.Backups).isNotEmpty() },
            onResult = { hasBackup -> if (hasBackup && games.isEmpty()) dialog = MainDialog.InitialRestore },
        )
    }

    private fun onDialogBackupChosen(entry: BackupEntry) {
        loadBackup(entry.summary, entry.uri, entry.format)
    }

    private fun showLoadBackupDialog() {
        runBackground(
            work = {
                val available = SdcardHelper.isAvailable()
                if (!available) return@runBackground Pair(false, emptyList<BackupEntry>())
                val backups = SdcardHelper.list(Location.Backups)
                val entries = backups.mapNotNull { filename ->
                    runCatching {
                        val file = SdcardHelper.getFile(filename, Location.Backups)
                        val format = if (filename.endsWith(".gz")) Format.GZIP else Format.XML
                        val summary = GamesBackupSerializer.readGamesBackupSummary(
                            Uri.fromFile(file), format, contentResolver,
                        )
                        BackupEntry(summary, Uri.fromFile(file), format)
                    }.onFailure { log.e(it as? Exception ?: RuntimeException(it), "Unable to read backup summary") }.getOrNull()
                }.sortedByDescending { it.summary.dateSaved }
                Pair(true, entries)
            },
            onResult = { (available, entries) ->
                when {
                    !available -> ToastHelper.showLong(this, R.string.toast_no_sdcard)
                    entries.isEmpty() -> ToastHelper.showShort(this, R.string.toast_no_backups)
                    else -> dialog = MainDialog.ChooseBackup(entries)
                }
            },
            onError = {
                log.e(it as? Exception ?: RuntimeException(it), "Unable to list backups")
                ToastHelper.showLong(this, R.string.toast_error_with_backup)
            },
        )
    }

    private fun loadBackupFileFromShare(intent: Intent?) {
        val uri = intent?.data ?: return
        val uriKey = intent.dataString ?: uri.toString()
        if (uriKey in confirmedUriIntents) return
        runBackground(
            work = { GamesBackupSerializer.readGamesBackupSummary(uri, Format.XML, contentResolver) },
            onResult = { summary -> dialog = MainDialog.ImportBackup(summary, uri) },
            onError = {
                log.e(it as? Exception ?: RuntimeException(it), "Unable to read incoming backup %s", uri)
                ToastHelper.showLong(this, R.string.toast_error_with_backup)
            },
        )
    }

    private fun saveBackup(format: Format, location: Location, gameIds: List<Int>, share: Boolean) {
        if (gameIds.isEmpty()) return
        dialog = MainDialog.Progress(R.string.text_saving, 0, gameIds.size)
        runBackgroundWithProgress(
            work = { reportProgress ->
                if (!SdcardHelper.isAvailable()) return@runBackgroundWithProgress BackupResult(false, null, null)
                val backupFilename = SdcardHelper.createBackupFilename(format)
                val savedGames = ArrayList<Game>()
                val helper = GameDBHelper(this)
                try {
                    gameIds.forEach { id ->
                        savedGames.add(helper.findGameById(id))
                        reportProgress(savedGames.size)
                    }
                } finally {
                    helper.close()
                }
                val backup = GamesBackup().apply {
                    version = GamesBackupSerializer.CURRENT_VERSION
                    dateSaved = System.currentTimeMillis()
                    setAutomatic(false)
                    gameCount = savedGames.size
                    games = savedGames
                    filename = backupFilename
                }
                val saved = SdcardHelper.save(backupFilename, format, location, GamesBackupSerializer.serialize(backup))
                BackupResult(true, if (saved) backupFilename else null, if (saved) SdcardHelper.getFile(backupFilename, location) else null)
            },
            onProgress = { progress -> dialog = MainDialog.Progress(R.string.text_saving, progress, gameIds.size) },
            onResult = { result ->
                dialog = null
                val filename = result.filename
                if (!result.available) {
                    ToastHelper.showLong(this, R.string.toast_no_sdcard)
                } else if (filename == null) {
                    ToastHelper.showLong(this, R.string.toast_save_backup_failed)
                } else if (share) {
                    val subject = resources.getQuantityString(R.plurals.text_share_mail_subject, gameIds.size, gameIds.size)
                    val uri = sharedUri(checkNotNull(result.file))
                    MailHelper.sendAsAttachment(this, uri, "text/xml", subject, getString(R.string.text_share_mail_body))
                } else {
                    val message = resources.getQuantityString(
                        R.plurals.text_save_backup_succeeded, gameIds.size, gameIds.size, filename,
                    )
                    dialog = MainDialog.Success(R.string.title_success, message)
                }
            },
            onError = {
                log.e(it as? Exception ?: RuntimeException(it), "Unable to save backup")
                dialog = null
                ToastHelper.showLong(this, R.string.toast_save_backup_failed)
            },
        )
    }

    private fun exportToSpreadsheet(gameIds: List<Int>) {
        if (gameIds.isEmpty()) return
        dialog = MainDialog.Progress(R.string.text_loading_generic, 0, gameIds.size + 1)
        runBackgroundWithProgress(
            work = { reportProgress ->
                val savedGames = ArrayList<Game>()
                val helper = GameDBHelper(this)
                try {
                    gameIds.forEach { id ->
                        savedGames.add(helper.findGameById(id))
                        reportProgress(savedGames.size)
                    }
                } finally {
                    helper.close()
                }
                val filename = SdcardHelper.createSpreadsheetFilename()
                SdcardHelper.saveSpreadsheet(filename, savedGames, this)
                reportProgress(gameIds.size + 1)
                filename
            },
            onProgress = { progress -> dialog = MainDialog.Progress(R.string.text_loading_generic, progress, gameIds.size + 1) },
            onResult = { filename ->
                dialog = null
                val subject = resources.getQuantityString(
                    R.plurals.text_share_spreadsheet_subject, gameIds.size, gameIds.size,
                )
                val uri = sharedUri(SdcardHelper.getFile(filename, Location.Spreadsheets))
                MailHelper.sendAsAttachment(this, uri, "text/csv", subject, getString(R.string.text_share_spreadsheet_body))
            },
            onError = {
                dialog = null
                log.e(it as? Exception ?: RuntimeException(it), "Unable to export spreadsheet")
                ToastHelper.showLong(this, R.string.toast_save_backup_failed)
            },
        )
    }

    private fun loadBackup(summary: GamesBackupSummary, uri: Uri, format: Format) {
        dialog = MainDialog.Progress(R.string.text_loading, 0, summary.gameCount)
        runBackgroundWithProgress(
            work = { reportProgress -> loadBackupInBackground(uri, format) { reportProgress(it) } },
            onProgress = { progress -> dialog = MainDialog.Progress(R.string.text_loading, progress, summary.gameCount) },
            onResult = { result ->
                dialog = null
                if (result == null) {
                    ToastHelper.showLong(this, R.string.toast_error_with_backup)
                } else {
                    games = (games + result.loadedGames.map(GameSummary::fromGame))
                        .sortedWith(GameSummary.byRecentlySaved())
                    val message = getString(
                        R.string.text_load_backup,
                        result.filename,
                        result.numFound,
                        result.loadedGames.size,
                        result.numDuplicates,
                    )
                    dialog = MainDialog.Success(R.string.title_success, message)
                }
            },
            onError = {
                dialog = null
                log.e(it as? Exception ?: RuntimeException(it), "Unable to import backup")
                ToastHelper.showLong(this, R.string.toast_error_with_backup)
            },
        )
    }

    private fun loadBackupInBackground(uri: Uri, format: Format, onProgress: (Int) -> Unit): LoadGamesBackupResult? {
        val backup = try {
            GamesBackupSerializer.deserialize(SdcardHelper.open(uri, format, contentResolver))
        } catch (exception: Exception) {
            log.e(exception, "Unable to deserialize backup")
            return null
        }
        val loadedGames = ArrayList<Game>()
        var found = 0
        var duplicates = 0
        val helper = GameDBHelper(this)
        try {
            backup.games.forEach { game ->
                found++
                if (helper.existsByDateStarted(game.dateStarted)) {
                    duplicates++
                } else {
                    helper.saveGame(game, false)
                    loadedGames.add(game)
                }
                onProgress(found)
            }
        } finally {
            helper.close()
        }
        return LoadGamesBackupResult().apply {
            filename = backup.filename ?: uri.lastPathSegment
            numFound = found
            numDuplicates = duplicates
            this.loadedGames = loadedGames
        }
    }

    private fun deleteGames(ids: List<Int>) {
        if (ids.isEmpty()) return
        runBackground(
            work = {
                val helper = GameDBHelper(this)
                try {
                    helper.deleteGames(ids)
                } finally {
                    helper.close()
                }
            },
            onResult = {
                games = games.filterNot { it.id in ids }
                selectedIds = selectedIds - ids.toSet()
                Toast.makeText(this, if (ids.size == 1) R.string.toast_deleted else R.string.toast_multiple_deleted, Toast.LENGTH_SHORT).show()
            },
            onError = {
                log.e(it as? Exception ?: RuntimeException(it), "Unable to delete games")
                ToastHelper.showLong(this, R.string.toast_error_with_backup)
            },
        )
    }

    private fun copyGame(summary: GameSummary, resetScores: Boolean) {
        findGame(summary) { original ->
            val copy = original.makeCleanCopy()
            if (resetScores) {
                val initialScore = PreferenceHelper.getIntPreference(
                    R.string.CONSTANT_pref_initial_score,
                    R.string.CONSTANT_pref_initial_score_default,
                    this,
                )
                copy.playerScores.forEach { player ->
                    player.score = initialScore.toLong()
                    player.history = ArrayList<Delta>()
                }
            }
            runBackground(
                work = {
                    val helper = GameDBHelper(this)
                    try {
                        helper.saveGame(copy)
                    } finally {
                        helper.close()
                    }
                },
                onResult = {
                    games = (games + GameSummary.fromGame(copy)).sortedWith(GameSummary.byRecentlySaved())
                    ToastHelper.showShort(this, if (resetScores) R.string.toast_rematch_created else R.string.toast_game_copied)
                },
            )
        }
    }

    private fun renameGame(game: GameSummary, newName: String) {
        val trimmedName = newName.trim()
        runBackground(
            work = {
                val helper = GameDBHelper(this)
                try {
                    helper.updateGameName(game.id, trimmedName)
                } finally {
                    helper.close()
                }
            },
            onResult = {
                game.name = trimmedName
                games = games.toList()
            },
            onError = {
                log.e(it as? Exception ?: RuntimeException(it), "Unable to rename game")
                ToastHelper.showLong(this, R.string.toast_error_with_backup)
            },
        )
    }

    private fun findGame(summary: GameSummary, onResult: (Game) -> Unit) {
        runBackground(
            work = {
                val helper = GameDBHelper(this)
                try {
                    helper.findGameById(summary.id)
                } finally {
                    helper.close()
                }
            },
            onResult = onResult,
            onError = {
                log.e(it as? Exception ?: RuntimeException(it), "Unable to load game")
                ToastHelper.showLong(this, R.string.toast_error_with_backup)
            },
        )
    }

    private fun selectedGames(): List<GameSummary> = games.filter { it.id in selectedIds }

    private fun sharedUri(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun <T> runBackground(
        work: () -> T,
        onResult: (T) -> Unit,
        onError: (Throwable) -> Unit = { log.e(it as? Exception ?: RuntimeException(it), "Background task failed") },
    ) {
        runBackgroundWithProgress(work = { _ -> work() }, onResult = onResult, onError = onError)
    }

    private fun <T> runBackgroundWithProgress(
        work: (reportProgress: (Int) -> Unit) -> T,
        onResult: (T) -> Unit,
        onProgress: (Int) -> Unit = {},
        onError: (Throwable) -> Unit = { log.e(it as? Exception ?: RuntimeException(it), "Background task failed") },
    ) {
        executor.execute {
            try {
                val value = work { progress ->
                    mainHandler.post {
                        if (!isFinishing && !isDestroyed) onProgress(progress)
                    }
                }
                mainHandler.post {
                    if (!isFinishing && !isDestroyed) onResult(value)
                }
            } catch (error: Throwable) {
                mainHandler.post {
                    if (!isFinishing && !isDestroyed) onError(error)
                }
            }
        }
    }

    private data class BackupResult(val available: Boolean, val filename: String?, val file: File?)

    companion object {
        private const val STATE_SELECTED_IDS = "selected_game_ids"
        private val confirmedUriIntents = mutableSetOf<String>()
    }
}
