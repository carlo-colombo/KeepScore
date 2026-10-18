package ovh.litapp.keepscore.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room mapping of the legacy "PlayerScores" table. History and history timestamps stay
 * comma-joined CSV strings, exactly as the old schema stored them, so the XML backup format
 * and existing rows are unaffected. The v4-to-v5 migration rebuilds the table to attach the
 * {@code gameId} foreign key and keep the {@code index_game_id} index.
 */
@Entity(
        tableName = "PlayerScores",
        foreignKeys = @ForeignKey(
                entity = GameEntity.class,
                parentColumns = "_id",
                childColumns = "gameId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index(name = "index_game_id", value = {"gameId"})
        })
public class PlayerScoreEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public int id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "score")
    public long score;

    @ColumnInfo(name = "playerNumber")
    public int playerNumber;

    /** Comma-joined delta values; kept as a CSV string to preserve the legacy format. */
    @ColumnInfo(name = "history")
    public String history;

    @ColumnInfo(name = "lastUpdate", defaultValue = "0")
    public long lastUpdate;

    /** Comma-joined delta timestamps, parallel to {@link #history}. */
    @ColumnInfo(name = "historyTimestamps")
    public String historyTimestamps;

    @ColumnInfo(name = "color")
    public String color;

    @ColumnInfo(name = "gameId")
    public int gameId;
}
