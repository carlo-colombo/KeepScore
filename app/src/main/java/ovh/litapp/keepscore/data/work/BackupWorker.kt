package ovh.litapp.keepscore.data.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ovh.litapp.keepscore.db.GameDBHelper
import ovh.litapp.keepscore.helper.SdcardHelper
import ovh.litapp.keepscore.serialization.GamesBackup
import ovh.litapp.keepscore.serialization.GamesBackupSerializer
import ovh.litapp.keepscore.util.UtilLogger

/**
 * Writes the daily XML/GZIP backup of all saved games. Replaces the deprecated `IntentService` that
 * the old AlarmManager path started; WorkManager survives reboots and respects Doze without an
 * exact-alarm permission.
 */
class BackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        var dbHelper: GameDBHelper? = null
        try {
            dbHelper = GameDBHelper(applicationContext)
            val games = dbHelper.findAllGames()

            val filename = SdcardHelper.createBackupFilename(SdcardHelper.Format.GZIP)

            val gamesBackup = GamesBackup()
            gamesBackup.version = GamesBackupSerializer.CURRENT_VERSION
            gamesBackup.dateSaved = System.currentTimeMillis()
            gamesBackup.gameCount = games.size
            gamesBackup.setAutomatic(true)
            gamesBackup.filename = filename
            gamesBackup.games = games.toMutableList()

            val xmlData = GamesBackupSerializer.serialize(gamesBackup)
            val saved = SdcardHelper.save(
                filename,
                SdcardHelper.Format.GZIP,
                SdcardHelper.Location.Backups,
                xmlData,
            )

            log.i("Periodic backup saved=%s games=%d file=%s", saved, games.size, filename)
            return if (saved) Result.success() else Result.retry()
        } catch (e: Exception) {
            log.e(e, "Periodic backup failed")
            return Result.retry()
        } finally {
            dbHelper?.close()
        }
    }

    companion object {

        const val UNIQUE_NAME = "keepscore-periodic-backup"

        private val log = UtilLogger(BackupWorker::class.java)
    }
}
