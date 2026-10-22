package ovh.litapp.keepscore.db

import android.content.Context
import androidx.room.Room
import java.util.Collections
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.util.Pair
import ovh.litapp.keepscore.util.StringUtil
import ovh.litapp.keepscore.util.UtilLogger

/**
 * Data access for saved games. This used to be a hand-written `SQLiteOpenHelper`; it now delegates to
 * Room while keeping its public API (and therefore every call site) unchanged.
 *
 * The database file name and schema are unchanged, so an existing "games.db" is upgraded by
 * [AppDatabase]'s migrations. Room normally forbids database access on the main thread, but legacy
 * call sites (notably [ovh.litapp.keepscore.GameActivity]'s foreground autosave and
 * `createExistingGameFromId`) query synchronously from the UI thread, so main-thread queries are
 * allowed to preserve the existing behavior.
 */
class GameDBHelper(context: Context) {

    private val database: AppDatabase = Room
        .databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
        .allowMainThreadQueries()
        .addMigrations(*AppDatabase.MIGRATIONS)
        .build()
    private val dao: GameDao = database.gameDao()

    fun close() {
        synchronized(GameDBHelper::class.java) {
            database.close()
        }
    }

    /** Return true if a game with that dateStarted value exists. */
    fun existsByDateStarted(dateStarted: Long): Boolean = synchronized(GameDBHelper::class.java) {
        dao.findGameIdByDateStarted(dateStarted) != null
    }

    fun findGameById(gameId: Int): Game? = synchronized(GameDBHelper::class.java) {
        val entity = dao.findGameEntityById(gameId) ?: return@synchronized null
        toGame(entity, dao.findPlayerScoresByGameId(gameId))
    }

    fun findGameCount(): Int = synchronized(GameDBHelper::class.java) {
        dao.findGameCount()
    }

    fun findMostRecentGameId(): Int = synchronized(GameDBHelper::class.java) {
        dao.findMostRecentGameId() ?: -1
    }

    fun findMostRecentGame(): Game? = synchronized(GameDBHelper::class.java) {
        val entity = dao.findMostRecentGameEntity() ?: return@synchronized null
        toGame(entity, dao.findPlayerScoresByGameId(entity.id))
    }

    /** save a game, updating its 'dateSaved' value */
    fun saveGame(game: Game) {
        saveGame(game, true)
    }

    /** save a game, optionally updating its 'dateSaved' value */
    fun saveGame(game: Game, updateDateSaved: Boolean) {
        synchronized(GameDBHelper::class.java) {
            database.runInTransaction { saveGameWithinTransaction(game, updateDateSaved) }
        }
    }

    private fun saveGameWithinTransaction(game: Game, updateDateSaved: Boolean) {
        val dateSaved = if (updateDateSaved) System.currentTimeMillis() else game.dateSaved
        game.dateSaved = dateSaved

        if (game.id != -1) {
            // game was already saved, so try to overwrite
            dao.updateGame(toGameEntity(game))
        } else {
            // else create a new row in the table
            val newGameId = dao.insertGame(toGameEntity(game)).toInt()
            game.id = newGameId
            log.d("new game id is %s", newGameId)
        }

        savePlayerScoresWithinTransaction(game.id, game.playerScores)
    }

    private fun savePlayerScoresWithinTransaction(gameId: Int, playerScores: List<PlayerScore>) {
        for (playerScore in playerScores) {
            if (playerScore.id != -1) {
                // already exists; update
                dao.updatePlayerScore(toPlayerScoreEntity(gameId, playerScore))
            } else {
                // else insert a new row in the table
                val newId = dao.insertPlayerScore(toPlayerScoreEntity(gameId, playerScore)).toInt()
                playerScore.id = newId
                log.d("new playerScore id is %s", newId)
            }
        }
    }

    fun findAllGames(): List<Game> = synchronized(GameDBHelper::class.java) {
        val gameEntities = dao.findAllGames()
        val playerScoresByGameId = groupByGameId(dao.findAllPlayerScores())

        gameEntities.map { entity -> toGame(entity, playerScoresByGameId[entity.id]) }
    }

    fun findAllGameSummaries(): List<GameSummary> = synchronized(GameDBHelper::class.java) {
        val gameEntities = dao.findAllGames()
        val playerScoresByGameId = groupByGameId(dao.findAllPlayerScores())

        gameEntities.map { entity ->
            val playerScores = playerScoresByGameId[entity.id]

            val gameSummary = GameSummary()
            gameSummary.id = entity.id
            gameSummary.name = entity.name
            gameSummary.dateSaved = entity.dateSaved

            val playerNames = ArrayList<String>()
            var numRounds = 0
            for (playerScore in playerScores.orEmpty()) {
                playerNames.add(playerScore.name)
                // Count comma-delimited rounds; an empty history has no recorded rounds.
                numRounds = maxOf(numRounds, numberOfRounds(playerScore.history))
            }
            gameSummary.playerNames = playerNames
            gameSummary.numRounds = numRounds

            gameSummary
        }
    }

    fun deleteGame(game: Game) {
        synchronized(GameDBHelper::class.java) {
            val id = game.id
            database.runInTransaction {
                dao.deletePlayerScoresByGameId(id)
                dao.deleteGameById(id)
            }
        }
    }

    fun updateGameName(gameId: Int, newName: String?) {
        synchronized(GameDBHelper::class.java) {
            dao.updateGameName(gameId, newName)
        }
    }

    fun findDistinctPlayerNames(): List<String> = synchronized(GameDBHelper::class.java) {
        ArrayList(dao.findDistinctPlayerNames())
    }

    fun deleteGames(gameIds: Collection<Int>) {
        synchronized(GameDBHelper::class.java) {
            if (gameIds.isEmpty()) {
                return
            }
            database.runInTransaction {
                dao.deletePlayerScoresByGameIds(gameIds)
                dao.deleteGamesByIds(gameIds)
            }
        }
    }

    companion object {

        private const val DB_NAME = "games.db"

        private val log = UtilLogger(GameDBHelper::class.java)

        private fun groupByGameId(
            playerScores: List<PlayerScoreEntity>,
        ): Map<Int, MutableList<PlayerScoreEntity>> {
            val result = HashMap<Int, MutableList<PlayerScoreEntity>>()
            for (playerScore in playerScores) {
                result.getOrPut(playerScore.gameId) { ArrayList() }.add(playerScore)
            }
            return result
        }

        private fun numberOfRounds(history: String?): Int {
            if (history.isNullOrEmpty()) {
                return 0
            }
            var commas = 0
            for (element in history) {
                if (element == ',') {
                    commas++
                }
            }
            return commas + 1
        }

        private fun toGameEntity(game: Game): GameEntity {
            val entity = GameEntity()
            entity.id = if (game.id == -1) 0 else game.id
            entity.name = game.name
            entity.autosaved = 1 // legacy column that must be specified
            entity.dateStarted = game.dateStarted
            entity.dateSaved = game.dateSaved
            return entity
        }

        private fun toPlayerScoreEntity(gameId: Int, playerScore: PlayerScore): PlayerScoreEntity {
            val historyAsStrings: Pair<String, String> = Delta.toJoinedStrings(playerScore.history)

            val entity = PlayerScoreEntity()
            entity.id = if (playerScore.id == -1) 0 else playerScore.id
            entity.name = StringUtil.nullToEmpty(playerScore.name)
            entity.score = playerScore.score
            entity.playerNumber = playerScore.playerNumber
            entity.history = historyAsStrings.first
            entity.historyTimestamps = historyAsStrings.second
            entity.lastUpdate = playerScore.lastUpdate
            entity.color = PlayerColor.serialize(playerScore.playerColor)
            entity.gameId = gameId
            return entity
        }

        private fun toGame(entity: GameEntity, playerScoreEntities: List<PlayerScoreEntity>?): Game {
            val game = Game()
            game.id = entity.id
            game.name = entity.name
            game.dateStarted = entity.dateStarted
            game.dateSaved = entity.dateSaved

            val playerScores = ArrayList<PlayerScore>(playerScoreEntities?.size ?: 0)
            if (playerScoreEntities != null) {
                for (playerScoreEntity in playerScoreEntities) {
                    playerScores.add(toPlayerScore(playerScoreEntity))
                }
            }
            Collections.sort(playerScores, PlayerScore.sortByPlayerNumber())
            game.playerScores = playerScores
            return game
        }

        private fun toPlayerScore(entity: PlayerScoreEntity): PlayerScore {
            val playerScore = PlayerScore()
            playerScore.id = entity.id
            playerScore.name = entity.name
            playerScore.score = entity.score
            playerScore.playerNumber = entity.playerNumber
            playerScore.history = Delta.fromJoinedStrings(
                StringUtil.nullToEmpty(entity.history),
                StringUtil.nullToEmpty(entity.historyTimestamps),
            )
            playerScore.lastUpdate = entity.lastUpdate
            val color = entity.color
            playerScore.playerColor = if (color == null) {
                PlayerColor.BUILT_INS[entity.playerNumber]
            } else {
                PlayerColor.deserialize(color)
            }
            return playerScore
        }
    }
}
