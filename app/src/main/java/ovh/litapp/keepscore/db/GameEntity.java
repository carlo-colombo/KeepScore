package ovh.litapp.keepscore.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room mapping of the legacy "Games" table. The column names and nullability mirror the
 * schema that {@link GameDBHelper} created before the Room migration so existing databases
 * keep working, including the legacy "autosaved" column that is still NOT NULL.
 */
@Entity(tableName = "Games", indices = {
        @Index(name = "index_date_started", value = {"dateStarted"})
})
public class GameEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    /** Legacy column that predates autosave toggling; still required by the old table. */
    @ColumnInfo(name = "autosaved")
    public int autosaved;

    @ColumnInfo(name = "dateStarted")
    public long dateStarted;

    @ColumnInfo(name = "dateSaved")
    public long dateSaved;
}
