package ovh.litapp.keepscore.db;

import java.util.Collection;
import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

/**
 * Room data access for the games/player-scores tables. The queries preserve the semantics of
 * the old hand-written SQL in {@link GameDBHelper}: games are ordered by {@code dateSaved},
 * player scores by {@code playerNumber}, and inserting a new row uses SQLite's autoincrement
 * (the old code emulated this with {@code max(_id) + 1}).
 */
@Dao
public interface GameDao {

    @Query("SELECT * FROM Games ORDER BY dateSaved")
    List<GameEntity> findAllGames();

    @Query("SELECT * FROM Games WHERE _id = :gameId LIMIT 1")
    GameEntity findGameEntityById(int gameId);

    @Query("SELECT * FROM Games ORDER BY dateSaved DESC LIMIT 1")
    GameEntity findMostRecentGameEntity();

    @Query("SELECT _id FROM Games ORDER BY dateSaved DESC LIMIT 1")
    Integer findMostRecentGameId();

    @Query("SELECT _id FROM Games WHERE dateStarted = :dateStarted LIMIT 1")
    Integer findGameIdByDateStarted(long dateStarted);

    @Query("SELECT COUNT(*) FROM Games")
    int findGameCount();

    @Query("SELECT * FROM PlayerScores WHERE gameId = :gameId ORDER BY playerNumber")
    List<PlayerScoreEntity> findPlayerScoresByGameId(int gameId);

    @Query("SELECT * FROM PlayerScores ORDER BY gameId, playerNumber")
    List<PlayerScoreEntity> findAllPlayerScores();

    @Query("SELECT DISTINCT name FROM PlayerScores")
    List<String> findDistinctPlayerNames();

    @Insert
    long insertGame(GameEntity game);

    @Update
    void updateGame(GameEntity game);

    @Insert
    long insertPlayerScore(PlayerScoreEntity playerScore);

    @Update
    void updatePlayerScore(PlayerScoreEntity playerScore);

    @Query("UPDATE Games SET name = :name WHERE _id = :gameId")
    void updateGameName(int gameId, String name);

    @Query("DELETE FROM PlayerScores WHERE gameId = :gameId")
    void deletePlayerScoresByGameId(int gameId);

    @Query("DELETE FROM Games WHERE _id = :gameId")
    void deleteGameById(int gameId);

    @Query("DELETE FROM PlayerScores WHERE gameId IN (:gameIds)")
    void deletePlayerScoresByGameIds(Collection<Integer> gameIds);

    @Query("DELETE FROM Games WHERE _id IN (:gameIds)")
    void deleteGamesByIds(Collection<Integer> gameIds);
}
