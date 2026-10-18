package ovh.litapp.keepscore.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room database for saved games. The name ("games.db") and version (5) are chosen so that an
 * existing database created by the old {@code SQLiteOpenHelper}-based
 * {@link GameDBHelper} is upgraded by the migrations below rather than rebuilt.
 */
@Database(entities = {GameEntity.class, PlayerScoreEntity.class}, version = 5, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public abstract GameDao gameDao();

    /**
     * Old databases at version 1 lacked the {@code dateStarted} index and the {@code lastUpdate}
     * column. The old {@code onUpgrade} applied both when jumping from version 1.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_date_started ON Games(dateStarted)");
            db.execSQL("ALTER TABLE PlayerScores ADD COLUMN lastUpdate INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * Old databases at version 2 lacked history timestamps and player colors. The old
     * {@code onUpgrade} populated colors from the player number, which is safe because the
     * legacy app only ever used the 16 built-in colors.
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE PlayerScores ADD COLUMN historyTimestamps TEXT");
            db.execSQL("ALTER TABLE PlayerScores ADD COLUMN color TEXT");
            db.execSQL("UPDATE PlayerScores SET color = playerNumber");
        }
    };

    /** The old app bumped the database to version 4 without any schema change. */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // no-op: version 4 was a bookkeeping bump in the old helper
        }
    };

    /**
     * Rebuilds PlayerScores so it carries the {@code gameId} foreign key and the color column
     * gains TEXT affinity (the legacy table declared it as the non-standard {@code string}
     * type). Data is copied verbatim, and the indexes Room expects are (re)created.
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS PlayerScores_new ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "name TEXT NOT NULL, "
                    + "score INTEGER NOT NULL, "
                    + "playerNumber INTEGER NOT NULL, "
                    + "history TEXT, "
                    + "lastUpdate INTEGER NOT NULL DEFAULT 0, "
                    + "historyTimestamps TEXT, "
                    + "color TEXT, "
                    + "gameId INTEGER NOT NULL, "
                    + "FOREIGN KEY(gameId) REFERENCES Games(_id) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT INTO PlayerScores_new "
                    + "(_id, name, score, playerNumber, history, lastUpdate, historyTimestamps, color, gameId) "
                    + "SELECT _id, name, score, playerNumber, history, lastUpdate, historyTimestamps, color, gameId "
                    + "FROM PlayerScores");
            db.execSQL("DROP TABLE PlayerScores");
            db.execSQL("ALTER TABLE PlayerScores_new RENAME TO PlayerScores");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_game_id ON PlayerScores(gameId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_date_started ON Games(dateStarted)");
        }
    };

    static final Migration[] MIGRATIONS = {
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
    };
}
