package ovh.litapp.keepscore.serialization

import android.content.Context
import android.graphics.Color
import android.net.Uri

import androidx.test.core.app.ApplicationProvider

import java.lang.reflect.Field
import java.util.UUID

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import ovh.litapp.keepscore.KeepScoreApplication
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor
import ovh.litapp.keepscore.helper.SdcardHelper

/**
 * Round-trip tests for [GamesBackupSerializer].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = GamesBackupSerializerTest.TestKeepScoreApplication::class)
class GamesBackupSerializerTest {

    /** Sets the singleton required by SdcardHelper without scheduling unrelated background work. */
    class TestKeepScoreApplication : KeepScoreApplication() {

        override fun onCreate() {
            try {
                val instance: Field = KeepScoreApplication::class.java.getDeclaredField("instance")
                instance.isAccessible = true
                instance.set(null, this)
            } catch (e: ReflectiveOperationException) {
                throw AssertionError(e)
            }
        }
    }

    @Test
    fun roundTripPreservesGamesPlayerScoresAndHistory() {
        val gamesBackup = createGamesBackup()

        val deserialized = roundTrip(gamesBackup)

        assertGamesBackupEquals(gamesBackup, deserialized)
    }

    @Test
    fun deserializesVersionFourBackupWithoutColorsOrHistoryTimestamps() {
        val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<GamesBackup><gameCount>1</gameCount><version>4</version><automatic>false</automatic>" +
            "<backupFilename>legacy.xml</backupFilename><dateBackupSaved>1332617241000</dateBackupSaved>" +
            "<Games><Game><dateGameSaved>1332617240000</dateGameSaved>" +
            "<dateGameStarted>1332617000000</dateGameStarted><gameName>R&amp;D &lt;Friday&gt;</gameName>" +
            "<PlayerScores>" +
            "<PlayerScore><playerName>Alice &amp; Bob &lt;captain&gt;</playerName><score>12</score>" +
            "<playerNumber>4</playerNumber><history>5,7</history><lastUpdate>1332617239000</lastUpdate></PlayerScore>" +
            "<PlayerScore><playerName>Tom &amp; Jerry</playerName><score>9</score>" +
            "<playerNumber>9</playerNumber><history>-2,3,8</history><lastUpdate>1332617238000</lastUpdate></PlayerScore>" +
            "</PlayerScores></Game></Games></GamesBackup>"

        val deserialized = GamesBackupSerializer.deserialize(xml)

        assertEquals(4, deserialized.version)
        assertEquals(1, deserialized.games.size)
        val game = deserialized.games[0]
        assertEquals("R&D <Friday>", game.name)
        assertEquals(2, game.playerScores.size)

        val first = game.playerScores[0]
        assertEquals("Alice & Bob <captain>", first.name)
        assertEquals(PlayerColor.BUILT_INS[4], first.playerColor)
        assertEquals(listOf(Delta(0L, 5), Delta(0L, 7)), first.history)

        val second = game.playerScores[1]
        assertEquals("Tom & Jerry", second.name)
        assertEquals(PlayerColor.BUILT_INS[9], second.playerColor)
        assertEquals(listOf(Delta(0L, -2), Delta(0L, 3), Delta(0L, 8)), second.history)
        for (playerScore in game.playerScores) {
            for (delta in playerScore.history) {
                assertEquals(0L, delta.timestamp)
            }
        }
    }

    @Test
    fun gzipSaveAndOpenPreservesSerializedXmlThroughContentResolver() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val xml = GamesBackupSerializer.serialize(createGamesBackup())
        val filename = "serializer-round-trip-" + UUID.randomUUID() + ".xml.gz"
        val file = SdcardHelper.getFile(filename, SdcardHelper.Location.Backups)
        assertNotNull(file)

        try {
            assertTrue(
                SdcardHelper.save(filename, SdcardHelper.Format.GZIP, SdcardHelper.Location.Backups, xml),
            )
            assertTrue(file.exists())

            val readBack = SdcardHelper.open(
                Uri.fromFile(file),
                SdcardHelper.Format.GZIP,
                context.contentResolver,
            )

            assertEquals(xml, readBack)
        } finally {
            if (file.exists()) {
                assertTrue("temporary backup file should be deleted", file.delete())
            }
        }
    }

    @Test
    fun roundTripPreservesNullAndEmptyNames() {
        val deserialized = roundTrip(createGamesBackup())

        val nullGame = deserialized.games[1]
        assertNull(nullGame.name)
        assertNull(nullGame.playerScores[0].name)
        assertEquals("", nullGame.playerScores[1].name)

        val emptyNameGame = deserialized.games[2]
        assertEquals("", emptyNameGame.name)
    }

    @Test
    fun roundTripPreservesEmptyPlayerHistory() {
        val deserialized = roundTrip(createGamesBackup())

        val playerWithNoHistory = deserialized.games[1].playerScores[0]
        assertTrue(playerWithNoHistory.history.isEmpty())
    }

    @Test
    fun roundTripPreservesBuiltInAndCustomPlayerColors() {
        val deserialized = roundTrip(createGamesBackup())

        assertEquals(PlayerColor.BUILT_INS[0], deserialized.games[0].playerScores[0].playerColor)
        assertEquals(PlayerColor.BUILT_INS[7], deserialized.games[0].playerScores[1].playerColor)

        val customColor = deserialized.games[2].playerScores[0].playerColor
        assertTrue(customColor is CustomPlayerColor)
        assertEquals(Color.parseColor("#FF00FF00"), (customColor as CustomPlayerColor).color)

        val translucentCustomColor = deserialized.games[2].playerScores[1].playerColor
        assertTrue(translucentCustomColor is CustomPlayerColor)
        assertEquals(Color.parseColor("#80123456"), (translucentCustomColor as CustomPlayerColor).color)
    }

    @Test
    fun roundTripPreservesEmptyGameList() {
        val gamesBackup = createGamesBackup()
        gamesBackup.games = ArrayList()
        gamesBackup.gameCount = 0

        val deserialized = roundTrip(gamesBackup)

        assertGamesBackupEquals(gamesBackup, deserialized)
        assertTrue(deserialized.games.isEmpty())
    }

    private fun roundTrip(gamesBackup: GamesBackup): GamesBackup {
        val xmlData = GamesBackupSerializer.serialize(gamesBackup)
        return GamesBackupSerializer.deserialize(xmlData)
    }

    private fun createGamesBackup(): GamesBackup {
        val games = ArrayList<Game>()

        // fully populated game with built-in colors and score histories
        var game = createGame("Friday night", 1332617000000L, 1332617241000L)
        game.playerScores = listOf(
            createPlayerScore(
                "Alice", 0, 12L, PlayerColor.BUILT_INS[0],
                Delta(1332617000001L, 5), Delta(1332617000002L, 7),
            ),
            createPlayerScore(
                "Bob", 1, 9L, PlayerColor.BUILT_INS[7],
                Delta(1332617000003L, 4), Delta(1332617000004L, 5),
            ),
        )
        games.add(game)

        // null and empty names, a player without any history, and a never-updated player
        game = createGame(null, 1332617100000L, 1332617242000L)
        val emptyHistoryPlayer = createPlayerScore(null, 0, 0L, PlayerColor.BUILT_INS[15])
        emptyHistoryPlayer.lastUpdate = 0L
        game.playerScores = listOf(
            emptyHistoryPlayer,
            createPlayerScore("", 1, 3L, PlayerColor.BUILT_INS[1], Delta(1332617100001L, 3)),
        )
        games.add(game)

        // empty game name and custom player colors, including a translucent one
        game = createGame("", 1332617200000L, 1332617243000L)
        game.playerScores = listOf(
            createPlayerScore(
                "Carol", 0, 1L, CustomPlayerColor(Color.parseColor("#FF00FF00")),
                Delta(1332617200001L, 1),
            ),
            createPlayerScore(
                "Dave", 1, 2L, CustomPlayerColor(Color.parseColor("#80123456")),
                Delta(1332617200002L, 2),
            ),
        )
        games.add(game)

        val gamesBackup = GamesBackup()
        gamesBackup.version = GamesBackupSerializer.CURRENT_VERSION
        gamesBackup.dateSaved = BACKUP_DATE_SAVED
        gamesBackup.setAutomatic(false)
        gamesBackup.filename = "games-20120324.xml.gz"
        gamesBackup.games = games
        gamesBackup.gameCount = games.size
        return gamesBackup
    }

    private fun createGame(name: String?, dateStarted: Long, dateSaved: Long): Game {
        val game = Game()
        game.name = name
        game.dateStarted = dateStarted
        game.dateSaved = dateSaved
        return game
    }

    private fun createPlayerScore(
        name: String?,
        playerNumber: Int,
        score: Long,
        color: PlayerColor,
        vararg history: Delta,
    ): PlayerScore {
        val playerScore = PlayerScore()
        playerScore.name = name
        playerScore.playerNumber = playerNumber
        playerScore.score = score
        playerScore.playerColor = color
        playerScore.history = ArrayList(history.toList())
        playerScore.lastUpdate = 1332617000000L + playerNumber
        return playerScore
    }

    private fun assertGamesBackupEquals(expected: GamesBackup, actual: GamesBackup) {
        assertEquals(expected.version, actual.version)
        assertEquals(expected.dateSaved, actual.dateSaved)
        assertEquals(expected.gameCount, actual.gameCount)
        assertEquals(expected.isAutomatic(), actual.isAutomatic())
        assertEquals(expected.filename, actual.filename)
        assertEquals(expected.games.size, actual.games.size)

        for (i in expected.games.indices) {
            assertGameEquals(expected.games[i], actual.games[i])
        }
    }

    private fun assertGameEquals(expected: Game, actual: Game) {
        assertNullableEquals(expected.name, actual.name)
        assertEquals(expected.dateStarted, actual.dateStarted)
        assertEquals(expected.dateSaved, actual.dateSaved)
        assertEquals(expected.playerScores.size, actual.playerScores.size)

        for (i in expected.playerScores.indices) {
            assertPlayerScoreEquals(expected.playerScores[i], actual.playerScores[i])
        }
    }

    private fun assertPlayerScoreEquals(expected: PlayerScore, actual: PlayerScore) {
        assertNullableEquals(expected.name, actual.name)
        assertEquals(expected.playerNumber, actual.playerNumber)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.history, actual.history)
        assertEquals(expected.lastUpdate, actual.lastUpdate)
        assertEquals(expected.playerColor, actual.playerColor)
    }

    private fun assertNullableEquals(expected: String?, actual: String?) {
        if (expected == null) {
            assertNull(actual)
        } else {
            assertEquals(expected, actual)
        }
    }

    private companion object {
        private const val BACKUP_DATE_SAVED = 1332617241000L
    }
}
