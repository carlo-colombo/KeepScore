package ovh.litapp.keepscore;

import java.util.List;

import android.app.IntentService;
import android.content.Intent;

import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.GameDBHelper;
import ovh.litapp.keepscore.helper.SdcardHelper;
import ovh.litapp.keepscore.helper.SdcardHelper.Format;
import ovh.litapp.keepscore.helper.SdcardHelper.Location;
import ovh.litapp.keepscore.serialization.GamesBackup;
import ovh.litapp.keepscore.serialization.GamesBackupSerializer;
import ovh.litapp.keepscore.util.UtilLogger;

/**
 * Task to run occasionally in order to automatically back up saved games to an
 * XML file.
 * 
 * @author nolan
 * 
 */
public class PeriodicAutomaticBackupService extends IntentService {

    UtilLogger log = new UtilLogger(PeriodicAutomaticBackupService.class);
    
    /** action to use to call this receiver */
    public static final String INTENT_ACTION = "ovh.litapp.keepscore.periodicbackup";
    
    /** random int */
    public static final int INTENT_REQUEST_CODE = 458010910;
    
    public static final String SERVICE_NAME = PeriodicAutomaticBackupService.class.getSimpleName();
    
    public PeriodicAutomaticBackupService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String filename = SdcardHelper.createBackupFilename(Format.GZIP);
        GameDBHelper dbHelper =  null;
        try {
            dbHelper = new GameDBHelper(this);
            List<Game> games = dbHelper.findAllGames();
            
            log.i("Beginning periodic automatic backup of %d saved KeepScore games...", games.size());
            
            GamesBackup gamesBackup = new GamesBackup();
            
            gamesBackup.setVersion(GamesBackupSerializer.CURRENT_VERSION);
            gamesBackup.setDateSaved(System.currentTimeMillis());
            gamesBackup.setGameCount(games.size());
            gamesBackup.setAutomatic(true);
            gamesBackup.setFilename(filename);
            gamesBackup.setGames(games);
            
            String xmlData = GamesBackupSerializer.serialize(gamesBackup);
            SdcardHelper.save(filename, Format.GZIP, Location.Backups, xmlData);
            
            log.i("KeepScore backed up %d games to \"%s\".", games.size(), filename);
        } finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }
    }

    
}
