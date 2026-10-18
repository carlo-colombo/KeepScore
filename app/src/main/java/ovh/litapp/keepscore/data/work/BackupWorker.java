package ovh.litapp.keepscore.data.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.GameDBHelper;
import ovh.litapp.keepscore.helper.SdcardHelper;
import ovh.litapp.keepscore.serialization.GamesBackup;
import ovh.litapp.keepscore.serialization.GamesBackupSerializer;
import ovh.litapp.keepscore.util.UtilLogger;

/**
 * Writes the daily XML/GZIP backup of all saved games. Replaces the deprecated
 * {@code IntentService} that the old AlarmManager path started; WorkManager
 * survives reboots and respects Doze without an exact-alarm permission.
 */
public class BackupWorker extends Worker {

    private static final UtilLogger log = new UtilLogger(BackupWorker.class);

    public static final String UNIQUE_NAME = "keepscore-periodic-backup";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        GameDBHelper dbHelper = null;
        try {
            dbHelper = new GameDBHelper(getApplicationContext());
            List<Game> games = dbHelper.findAllGames();

            String filename = SdcardHelper.createBackupFilename(SdcardHelper.Format.GZIP);

            GamesBackup gamesBackup = new GamesBackup();
            gamesBackup.setVersion(GamesBackupSerializer.CURRENT_VERSION);
            gamesBackup.setDateSaved(System.currentTimeMillis());
            gamesBackup.setGameCount(games.size());
            gamesBackup.setAutomatic(true);
            gamesBackup.setFilename(filename);
            gamesBackup.setGames(games);

            String xmlData = GamesBackupSerializer.serialize(gamesBackup);
            boolean saved = SdcardHelper.save(filename, SdcardHelper.Format.GZIP,
                    SdcardHelper.Location.Backups, xmlData);

            log.i("Periodic backup saved=%s games=%d file=%s", saved, games.size(), filename);
            return saved ? Result.success() : Result.retry();
        } catch (Exception e) {
            log.e(e, "Periodic backup failed");
            return Result.retry();
        } finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }
    }
}
