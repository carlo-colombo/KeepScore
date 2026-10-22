package ovh.litapp.keepscore.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room mapping of the legacy "PlayerScores" table. History and history timestamps stay comma-joined
 * CSV strings, exactly as the old schema stored them, so the XML backup format and existing rows are
 * unaffected. The v4-to-v5 migration rebuilds the table to attach the [gameId] foreign key and keep
 * the `index_game_id` index.
 */
@Entity(
    tableName = "PlayerScores",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["_id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(name = "index_game_id", value = ["gameId"])],
)
data class PlayerScoreEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Int = 0,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "score")
    var score: Long = 0,

    @ColumnInfo(name = "playerNumber")
    var playerNumber: Int = 0,

    /** Comma-joined delta values; kept as a CSV string to preserve the legacy format. */
    @ColumnInfo(name = "history")
    var history: String? = null,

    @ColumnInfo(name = "lastUpdate", defaultValue = "0")
    var lastUpdate: Long = 0,

    /** Comma-joined delta timestamps, parallel to [history]. */
    @ColumnInfo(name = "historyTimestamps")
    var historyTimestamps: String? = null,

    @ColumnInfo(name = "color")
    var color: String? = null,

    @ColumnInfo(name = "gameId")
    var gameId: Int = 0,
)
