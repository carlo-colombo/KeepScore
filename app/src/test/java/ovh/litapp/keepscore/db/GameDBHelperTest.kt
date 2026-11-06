package ovh.litapp.keepscore.db

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import java.util.Collections
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor

/** Robolectric tests for the SQLite layer in [GameDBHelper]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameDBHelperTest {

    private lateinit var dbHelper: GameDBHelper

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        dbHelper = GameDBHelper(context)
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun saveGamePersistsGamePlayerScoresAndHistory() {
        val game = createGame(
            "Friday night",
            1000L,
            2000L,
            createPlayerScore("Bob", 1, 7L, PlayerColor.BUILT_INS[1], Delta(50L, 7)),
            createPlayerScore(
                "Alice",
                0,
                3L,
                CustomPlayerColor(Color.parseColor("#FF00FF00")),
                Delta(40L, 1),
                Delta(60L, 2),
            ),
        )
        val beforeSave = System.currentTimeMillis()
        dbHelper.saveGame(game)

        assertTrue(game.id >= 0)
        for (playerScore in game.playerScores) {
            assertTrue(playerScore.id >= 0)
        }

        val found = dbHelper.findGameById(game.id)
        assertNotNull(found)
        assertEquals(game.id, found!!.id)
        assertEquals("Friday night", found.name)
        assertEquals(1000L, found.dateStarted)
        // saveGame() stamps the game with the current time, rather than retaining its old value.
        assertTrue(game.dateSaved >= beforeSave)
        assertEquals(game.dateSaved, found.dateSaved)

        // player scores come back sorted by player number, not in insertion order
        assertEquals(2, found.playerScores.size)

        val alice = found.playerScores[0]
        assertEquals("Alice", alice.name)
        assertEquals(0, alice.playerNumber)
        assertEquals(3L, alice.score)
        assertEquals(listOf(Delta(40L, 1), Delta(60L, 2)), alice.history)
        assertEquals(CustomPlayerColor(Color.parseColor("#FF00FF00")), alice.playerColor)

        val bob = found.playerScores[1]
        assertEquals("Bob", bob.name)
        assertEquals(1, bob.playerNumber)
        assertEquals(7L, bob.score)
        assertEquals(Collections.singletonList(Delta(50L, 7)), bob.history)
        assertEquals(PlayerColor.BUILT_INS[1], bob.playerColor)

        assertEquals(1, dbHelper.findGameCount())
        assertEquals(game.id, dbHelper.findMostRecentGameId())
        assertEquals(1, dbHelper.findAllGames().size)
    }

    @Test
    fun deleteGameRemovesGameAndItsPlayerScores() {
        val doomed = createGame(
            "doomed",
            1000L,
            1000L,
            createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]),
        )
        val survivor = createGame(
            "survivor",
            2000L,
            2000L,
            createPlayerScore("Bob", 0, 2L, PlayerColor.BUILT_INS[1]),
        )
        dbHelper.saveGame(doomed, false)
        dbHelper.saveGame(survivor, false)

        dbHelper.deleteGame(doomed)

        assertNull(dbHelper.findGameById(doomed.id))
        assertFalse(dbHelper.existsByDateStarted(1000L))
        assertEquals(1, dbHelper.findGameCount())

        val foundSurvivor = dbHelper.findGameById(survivor.id)
        assertNotNull(foundSurvivor)
        assertEquals("survivor", foundSurvivor!!.name)
        assertEquals(Collections.singletonList("Bob"), dbHelper.findDistinctPlayerNames())
    }

    @Test
    fun saveGameUpdatePersistsChangesWithoutChangingIdsOrAddingRows() {
        val game = createGame(
            "before",
            1000L,
            2000L,
            createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0], Delta(10L, 1)),
        )
        dbHelper.saveGame(game, false)
        val gameId = game.id
        val playerScoreId = game.playerScores[0].id

        game.name = "after"
        val playerScore = game.playerScores[0]
        playerScore.score = 3L
        playerScore.history = arrayListOf(Delta(10L, 1), Delta(20L, 2))
        dbHelper.saveGame(game, false)

        val found = dbHelper.findGameById(gameId)
        assertNotNull(found)
        assertEquals(1, dbHelper.findGameCount())
        assertEquals(gameId, game.id)
        assertEquals(gameId, found!!.id)
        assertEquals("after", found.name)
        assertEquals(1, found.playerScores.size)
        assertEquals(playerScoreId, playerScore.id)
        assertEquals(playerScoreId, found.playerScores[0].id)
        assertEquals(3L, found.playerScores[0].score)
        assertEquals(listOf(Delta(10L, 1), Delta(20L, 2)), found.playerScores[0].history)
        assertEquals(2000L, found.dateSaved)
    }

    @Test
    fun saveGameCanPreserveDateSavedWhenUpdateIsDisabled() {
        val game = createGame(
            "unchanged date",
            1000L,
            2000L,
            createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]),
        )
        dbHelper.saveGame(game, false)

        assertEquals(2000L, game.dateSaved)
        assertEquals(2000L, dbHelper.findGameById(game.id)!!.dateSaved)
    }

    @Test
    fun findAllGameSummariesReportsZeroRoundsForEmptyHistories() {
        val game = createGame(
            "new game",
            1000L,
            2000L,
            createPlayerScore("Alice", 0, 0L, PlayerColor.BUILT_INS[0]),
        )
        val inMemorySummary = GameSummary.fromGame(game)
        dbHelper.saveGame(game, false)

        val databaseSummary = dbHelper.findAllGameSummaries()[0]

        assertEquals(0, inMemorySummary.numRounds)
        assertEquals(0, databaseSummary.numRounds)
        assertEquals(inMemorySummary.numRounds, databaseSummary.numRounds)
    }

    @Test
    fun findAllGameSummariesGroupsPlayerNamesAndRoundCounts() {
        dbHelper.saveGame(
            createGame(
                "middle",
                2000L,
                2000L,
                createPlayerScore("Zoe", 1, 5L, PlayerColor.BUILT_INS[1], Delta(1L, 5)),
                createPlayerScore(
                    "Alice",
                    0,
                    4L,
                    PlayerColor.BUILT_INS[0],
                    Delta(1L, 1),
                    Delta(2L, 3),
                ),
            ),
            false,
        )
        dbHelper.saveGame(
            createGame(
                "newest",
                3000L,
                3000L,
                createPlayerScore("Carol", 0, 1L, PlayerColor.BUILT_INS[2], Delta(1L, 1)),
            ),
            false,
        )
        dbHelper.saveGame(
            createGame(
                "oldest",
                1000L,
                1000L,
                createPlayerScore(
                    "Dave",
                    0,
                    2L,
                    PlayerColor.BUILT_INS[3],
                    Delta(1L, 2),
                    Delta(2L, 4),
                    Delta(3L, 6),
                ),
            ),
            false,
        )

        val summaries = dbHelper.findAllGameSummaries()

        assertEquals(3, summaries.size)

        val middle = summaryNamed(summaries, "middle")
        assertEquals(2000L, middle.dateSaved)
        // player names are ordered by player number, whatever order group_concat produces
        assertEquals(listOf("Alice", "Zoe"), middle.playerNames)
        assertEquals(2, middle.numRounds)

        val newest = summaryNamed(summaries, "newest")
        assertEquals(listOf("Carol"), newest.playerNames)
        assertEquals(1, newest.numRounds)

        val oldest = summaryNamed(summaries, "oldest")
        assertEquals(listOf("Dave"), oldest.playerNames)
        assertEquals(3, oldest.numRounds)
    }

    @Test
    fun updateNamesAndBulkDeleteGames() {
        val keep = createGame(
            "keep",
            1000L,
            1000L,
            createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]),
        )
        val delete = createGame(
            "delete",
            2000L,
            2000L,
            createPlayerScore("Bob", 0, 2L, PlayerColor.BUILT_INS[1]),
        )
        dbHelper.saveGame(keep, false)
        dbHelper.saveGame(delete, false)

        assertTrue(dbHelper.existsByDateStarted(1000L))
        dbHelper.updateGameName(keep.id, "renamed")
        assertEquals("renamed", dbHelper.findGameById(keep.id)!!.name)
        dbHelper.deleteGames(listOf(delete.id))

        assertNull(dbHelper.findGameById(delete.id))
        assertFalse(dbHelper.existsByDateStarted(2000L))
        assertEquals(1, dbHelper.findGameCount())
        assertEquals(1, dbHelper.findGameById(keep.id)!!.playerScores.size)
    }

    @Test
    fun findMostRecentGameIdIsMinusOneWhenThereAreNoGames() {
        assertEquals(-1, dbHelper.findMostRecentGameId())
    }

    @Test
    fun findMostRecentGameIsTheOneSavedLast() {
        val first = createGame(
            "first",
            1000L,
            1000L,
            createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]),
        )
        val last = createGame(
            "last",
            2000L,
            2000L,
            createPlayerScore("Bob", 0, 2L, PlayerColor.BUILT_INS[1]),
        )
        dbHelper.saveGame(first, false)
        dbHelper.saveGame(last, false)

        assertEquals(last.id, dbHelper.findMostRecentGameId())
        assertEquals("last", dbHelper.findMostRecentGame()!!.name)
    }

    private fun summaryNamed(summaries: List<GameSummary>, name: String): GameSummary =
        summaries.firstOrNull { it.name == name }
            ?: throw AssertionError("no summary named $name in $summaries")

    private fun createGame(
        name: String,
        dateStarted: Long,
        dateSaved: Long,
        vararg playerScores: PlayerScore,
    ): Game = Game().apply {
        this.name = name
        this.dateStarted = dateStarted
        this.dateSaved = dateSaved
        this.playerScores = ArrayList(playerScores.toList())
    }

    private fun createPlayerScore(
        name: String?,
        playerNumber: Int,
        score: Long,
        color: PlayerColor,
        vararg history: Delta,
    ): PlayerScore = PlayerScore().apply {
        this.name = name
        this.playerNumber = playerNumber
        this.score = score
        this.playerColor = color
        this.history = ArrayList(history.toList())
    }

    private companion object {
        // GameDBHelper.DB_NAME is private, and the helper always opens this file
        private const val DATABASE_NAME = "games.db"
    }
}
