package ovh.litapp.keepscore.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room mapping of the legacy "Games" table. The column names and nullability mirror the schema that
 * [GameDBHelper] created before the Room migration so existing databases keep working, including the
 * legacy "autosaved" column that is still NOT NULL.
 */
@Entity(
    tableName = "Games",
    indices = [Index(name = "index_date_started", value = ["dateStarted"])],
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Int = 0,

    @ColumnInfo(name = "name")
    var name: String? = null,

    /** Legacy column that predates autosave toggling; still required by the old table. */
    @ColumnInfo(name = "autosaved")
    var autosaved: Int = 0,

    @ColumnInfo(name = "dateStarted")
    var dateStarted: Long = 0,

    @ColumnInfo(name = "dateSaved")
    var dateSaved: Long = 0,
)
