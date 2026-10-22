package ovh.litapp.keepscore.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Room data access for the games/player-scores tables. The queries preserve the semantics of the old
 * hand-written SQL in [GameDBHelper]: games are ordered by `dateSaved`, player scores by
 * `playerNumber`, and inserting a new row uses SQLite's autoincrement (the old code emulated this
 * with `max(_id) + 1`).
 */
@Dao
interface GameDao {

    @Query("SELECT * FROM Games ORDER BY dateSaved")
    fun findAllGames(): List<GameEntity>

    @Query("SELECT * FROM Games WHERE _id = :gameId LIMIT 1")
    fun findGameEntityById(gameId: Int): GameEntity?

    @Query("SELECT * FROM Games ORDER BY dateSaved DESC LIMIT 1")
    fun findMostRecentGameEntity(): GameEntity?

    @Query("SELECT _id FROM Games ORDER BY dateSaved DESC LIMIT 1")
    fun findMostRecentGameId(): Int?

    @Query("SELECT _id FROM Games WHERE dateStarted = :dateStarted LIMIT 1")
    fun findGameIdByDateStarted(dateStarted: Long): Int?

    @Query("SELECT COUNT(*) FROM Games")
    fun findGameCount(): Int

    @Query("SELECT * FROM PlayerScores WHERE gameId = :gameId ORDER BY playerNumber")
    fun findPlayerScoresByGameId(gameId: Int): List<PlayerScoreEntity>

    @Query("SELECT * FROM PlayerScores ORDER BY gameId, playerNumber")
    fun findAllPlayerScores(): List<PlayerScoreEntity>

    @Query("SELECT DISTINCT name FROM PlayerScores")
    fun findDistinctPlayerNames(): List<String>

    @Insert
    fun insertGame(game: GameEntity): Long

    @Update
    fun updateGame(game: GameEntity)

    @Insert
    fun insertPlayerScore(playerScore: PlayerScoreEntity): Long

    @Update
    fun updatePlayerScore(playerScore: PlayerScoreEntity)

    @Query("UPDATE Games SET name = :name WHERE _id = :gameId")
    fun updateGameName(gameId: Int, name: String?)

    @Query("DELETE FROM PlayerScores WHERE gameId = :gameId")
    fun deletePlayerScoresByGameId(gameId: Int)

    @Query("DELETE FROM Games WHERE _id = :gameId")
    fun deleteGameById(gameId: Int)

    @Query("DELETE FROM PlayerScores WHERE gameId IN (:gameIds)")
    fun deletePlayerScoresByGameIds(gameIds: Collection<Int>)

    @Query("DELETE FROM Games WHERE _id IN (:gameIds)")
    fun deleteGamesByIds(gameIds: Collection<Int>)
}
