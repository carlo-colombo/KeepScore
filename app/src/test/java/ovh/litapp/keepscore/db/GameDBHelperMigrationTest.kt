package ovh.litapp.keepscore.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import java.util.Collections
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor

/**
 * Verifies that databases written by the old `SQLiteOpenHelper` implementation are upgraded in place
 * by Room's migrations.
 *
 * Room's `MigrationTestHelper` is not usable here: it creates the starting database from an exported
 * schema JSON, and versions 1-4 predate the Room migration, so no such schema files exist. Instead
 * these tests build the legacy databases with the exact old SQL and then let [GameDBHelper] (Room)
 * open and migrate them, which runs Room's own schema validation as a side effect.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameDBHelperMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun migratesV4DatabaseAndPreservesGamesPlayerScoresAndHistory() {
        createLegacyV4Database()

        val dbHelper = GameDBHelper(context)
        try {
            assertEquals(1, dbHelper.findGameCount())

            val game = dbHelper.findMostRecentGame()
            assertNotNull(game)
            assertEquals("Friday night", game!!.name)
            assertEquals(1000L, game.dateStarted)
            assertEquals(2000L, game.dateSaved)

            val playerScores = game.playerScores
            assertEquals(2, playerScores.size)

            val alice = playerScores[0]
            assertEquals("Alice", alice.name)
            assertEquals(0, alice.playerNumber)
            assertEquals(3L, alice.score)
            assertEquals(listOf(Delta(40L, 1), Delta(60L, 2)), alice.history)
            assertEquals(50L, alice.lastUpdate)
            assertEquals(PlayerColor.BUILT_INS[0], alice.playerColor)

            val bob = playerScores[1]
            assertEquals("Bob", bob.name)
            assertEquals(7L, bob.score)
            assertEquals(Collections.singletonList(Delta(50L, 7)), bob.history)
            assertEquals(CustomPlayerColor(Color.parseColor("#FF00FF00")), bob.playerColor)

            val summaries = dbHelper.findAllGameSummaries()
            assertEquals(1, summaries.size)
            assertEquals(listOf("Alice", "Bob"), summaries[0].playerNames)
            assertEquals(2, summaries[0].numRounds)
        } finally {
            dbHelper.close()
        }

        assertFinalSchema()
    }

    @Test
    fun migratesV1DatabaseThroughEveryUpgradeStep() {
        createLegacyV1Database()

        val dbHelper = GameDBHelper(context)
        try {
            val game = dbHelper.findMostRecentGame()
            assertNotNull(game)
            assertEquals("Legacy", game!!.name)

            val playerScores = game.playerScores
            assertEquals(1, playerScores.size)

            val carol = playerScores[0]
            assertEquals("Carol", carol.name)
            assertEquals(2, carol.playerNumber)
            assertEquals(5L, carol.score)
            // lastUpdate is added by the v1->v2 migration with a default of 0
            assertEquals(0L, carol.lastUpdate)
            // colors are populated from the player number by the v2->v3 migration
            assertEquals(PlayerColor.BUILT_INS[2], carol.playerColor)
            // history timestamps did not exist, so deltas keep a zero timestamp
            assertEquals(listOf(Delta(0L, 5), Delta(0L, 3)), carol.history)
        } finally {
            dbHelper.close()
        }

        assertFinalSchema()
    }

    private fun assertFinalSchema() {
        val raw = SQLiteDatabase.openDatabase(
            context.getDatabasePath(DB_NAME).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        try {
            assertForeignKey(raw, "PlayerScores", "Games", "gameId", "_id", "CASCADE")
            assertIndexExists(raw, "PlayerScores", "index_game_id")
            assertIndexExists(raw, "Games", "index_date_started")
        } finally {
            raw.close()
        }
    }

    private fun createLegacyV4Database() {
        val helper = legacyHelper(4) {
            it.execSQL(V4_GAMES)
            it.execSQL(V4_PLAYER_SCORES)
            it.execSQL("create index if not exists index_game_id on PlayerScores (gameId);")
        }

        val db = helper.writableDatabase
        val gameId = db.insertOrThrow("Games", null, gameValues("Friday night", 1000L, 2000L))

        val alice = playerScoreValues("Alice", 3, 0, "1,2", gameId)
        alice.put("lastUpdate", 50)
        alice.put("historyTimestamps", "40,60")
        alice.put("color", "0")
        db.insertOrThrow("PlayerScores", null, alice)

        val bob = playerScoreValues("Bob", 7, 1, "7", gameId)
        bob.put("lastUpdate", 70)
        bob.put("historyTimestamps", "50")
        bob.put("color", "#ff00ff00")
        db.insertOrThrow("PlayerScores", null, bob)

        helper.close()
    }

    private fun createLegacyV1Database() {
        val helper = legacyHelper(1) {
            it.execSQL(V4_GAMES)
            it.execSQL(V1_PLAYER_SCORES)
            it.execSQL("create index if not exists index_game_id on PlayerScores (gameId);")
        }

        val db = helper.writableDatabase
        val gameId = db.insertOrThrow("Games", null, gameValues("Legacy", 10L, 20L))
        db.insertOrThrow("PlayerScores", null, playerScoreValues("Carol", 5, 2, "5,3", gameId))

        helper.close()
    }

    /** The pre-Room helper: it creates the legacy tables and never upgrades them itself. */
    private fun legacyHelper(version: Int, onCreate: (SQLiteDatabase) -> Unit): SQLiteOpenHelper =
        object : SQLiteOpenHelper(context, DB_NAME, null, version) {
            override fun onCreate(db: SQLiteDatabase) {
                onCreate(db)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // no-op: the migrations under test are Room's
            }
        }

    private fun assertForeignKey(
        db: SQLiteDatabase,
        table: String,
        referencedTable: String,
        from: String,
        to: String,
        onDelete: String,
    ) {
        db.rawQuery("PRAGMA foreign_key_list($table)", null).use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (referencedTable == cursor.getString(cursor.getColumnIndexOrThrow("table")) &&
                    from == cursor.getString(cursor.getColumnIndexOrThrow("from")) &&
                    to == cursor.getString(cursor.getColumnIndexOrThrow("to")) &&
                    onDelete.equals(
                        cursor.getString(cursor.getColumnIndexOrThrow("on_delete")),
                        ignoreCase = true,
                    )
                ) {
                    found = true
                    break
                }
            }
            assertTrue(
                "missing foreign key $table.$from -> $referencedTable.$to",
                found,
            )
        }
    }

    private fun assertIndexExists(db: SQLiteDatabase, table: String, indexName: String) {
        db.rawQuery("PRAGMA index_list($table)", null).use { cursor: Cursor ->
            var found = false
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (indexName == cursor.getString(nameIndex)) {
                    found = true
                    break
                }
            }
            assertTrue("missing index $indexName on $table", found)
        }
    }

    private companion object {
        private const val DB_NAME = "games.db"

        private const val V4_GAMES =
            "create table if not exists Games (_id integer not null primary key autoincrement, " +
                "name text, autosaved int not null, dateStarted int not null, dateSaved int not null);"

        private const val V4_PLAYER_SCORES =
            "create table if not exists PlayerScores (_id integer not null primary key autoincrement, " +
                "name text not null, score int not null, playerNumber int not null, history text, " +
                "lastUpdate int not null default 0, historyTimestamps text, color string, " +
                "gameId int not null);"

        private const val V1_PLAYER_SCORES =
            "create table if not exists PlayerScores (_id integer not null primary key autoincrement, " +
                "name text not null, score int not null, playerNumber int not null, history text, " +
                "gameId int not null);"

        private fun gameValues(name: String, dateStarted: Long, dateSaved: Long): ContentValues =
            ContentValues().apply {
                put("name", name)
                put("autosaved", 1)
                put("dateStarted", dateStarted)
                put("dateSaved", dateSaved)
            }

        private fun playerScoreValues(
            name: String,
            score: Long,
            playerNumber: Int,
            history: String,
            gameId: Long,
        ): ContentValues = ContentValues().apply {
            put("name", name)
            put("score", score)
            put("playerNumber", playerNumber)
            put("history", history)
            put("gameId", gameId)
        }
    }
}
