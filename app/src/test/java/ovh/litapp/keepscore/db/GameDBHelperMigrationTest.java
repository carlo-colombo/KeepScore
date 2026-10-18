package ovh.litapp.keepscore.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import ovh.litapp.keepscore.helper.PlayerColor;
import ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor;

/**
 * Verifies that databases written by the old {@code SQLiteOpenHelper} implementation are
 * upgraded in place by Room's migrations.
 *
 * <p>Room's {@code MigrationTestHelper} is not usable here: it creates the starting database
 * from an exported schema JSON, and versions 1-4 predate the Room migration, so no such schema
 * files exist. Instead these tests build the legacy databases with the exact old SQL and then
 * let {@link GameDBHelper} (Room) open and migrate them, which runs Room's own schema
 * validation as a side effect.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameDBHelperMigrationTest {

    private static final String DB_NAME = "games.db";

    private static final String V4_GAMES =
            "create table if not exists Games (_id integer not null primary key autoincrement, "
                    + "name text, autosaved int not null, dateStarted int not null, dateSaved int not null);";
    private static final String V4_PLAYER_SCORES =
            "create table if not exists PlayerScores (_id integer not null primary key autoincrement, "
                    + "name text not null, score int not null, playerNumber int not null, history text, "
                    + "lastUpdate int not null default 0, historyTimestamps text, color string, "
                    + "gameId int not null);";

    private static final String V1_PLAYER_SCORES =
            "create table if not exists PlayerScores (_id integer not null primary key autoincrement, "
                    + "name text not null, score int not null, playerNumber int not null, history text, "
                    + "gameId int not null);";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void migratesV4DatabaseAndPreservesGamesPlayerScoresAndHistory() {
        createLegacyV4Database();

        GameDBHelper dbHelper = new GameDBHelper(context);
        try {
            assertEquals(1, dbHelper.findGameCount());

            Game game = dbHelper.findMostRecentGame();
            assertNotNull(game);
            assertEquals("Friday night", game.getName());
            assertEquals(1000L, game.getDateStarted());
            assertEquals(2000L, game.getDateSaved());

            List<PlayerScore> playerScores = game.getPlayerScores();
            assertEquals(2, playerScores.size());

            PlayerScore alice = playerScores.get(0);
            assertEquals("Alice", alice.getName());
            assertEquals(0, alice.getPlayerNumber());
            assertEquals(3L, alice.getScore());
            assertEquals(Arrays.asList(new Delta(40L, 1), new Delta(60L, 2)), alice.getHistory());
            assertEquals(50L, alice.getLastUpdate());
            assertEquals(PlayerColor.BUILT_INS[0], alice.getPlayerColor());

            PlayerScore bob = playerScores.get(1);
            assertEquals("Bob", bob.getName());
            assertEquals(7L, bob.getScore());
            assertEquals(Collections.singletonList(new Delta(50L, 7)), bob.getHistory());
            assertEquals(new CustomPlayerColor(Color.parseColor("#FF00FF00")), bob.getPlayerColor());

            List<GameSummary> summaries = dbHelper.findAllGameSummaries();
            assertEquals(1, summaries.size());
            assertEquals(Arrays.asList("Alice", "Bob"), summaries.get(0).getPlayerNames());
            assertEquals(2, summaries.get(0).getNumRounds());
        } finally {
            dbHelper.close();
        }

        assertFinalSchema();
    }

    @Test
    public void migratesV1DatabaseThroughEveryUpgradeStep() {
        createLegacyV1Database();

        GameDBHelper dbHelper = new GameDBHelper(context);
        try {
            Game game = dbHelper.findMostRecentGame();
            assertNotNull(game);
            assertEquals("Legacy", game.getName());

            List<PlayerScore> playerScores = game.getPlayerScores();
            assertEquals(1, playerScores.size());

            PlayerScore carol = playerScores.get(0);
            assertEquals("Carol", carol.getName());
            assertEquals(2, carol.getPlayerNumber());
            assertEquals(5L, carol.getScore());
            // lastUpdate is added by the v1->v2 migration with a default of 0
            assertEquals(0L, carol.getLastUpdate());
            // colors are populated from the player number by the v2->v3 migration
            assertEquals(PlayerColor.BUILT_INS[2], carol.getPlayerColor());
            // history timestamps did not exist, so deltas keep a zero timestamp
            assertEquals(Arrays.asList(new Delta(0L, 5), new Delta(0L, 3)), carol.getHistory());
        } finally {
            dbHelper.close();
        }

        assertFinalSchema();
    }

    private void assertFinalSchema() {
        SQLiteDatabase raw = SQLiteDatabase.openDatabase(
                context.getDatabasePath(DB_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);
        try {
            assertForeignKey(raw, "PlayerScores", "Games", "gameId", "_id", "CASCADE");
            assertIndexExists(raw, "PlayerScores", "index_game_id");
            assertIndexExists(raw, "Games", "index_date_started");
        } finally {
            raw.close();
        }
    }

    private void createLegacyV4Database() {
        SQLiteOpenHelper helper = new SQLiteOpenHelper(context, DB_NAME, null, 4) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(V4_GAMES);
                db.execSQL(V4_PLAYER_SCORES);
                db.execSQL("create index if not exists index_game_id on PlayerScores (gameId);");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        };

        SQLiteDatabase db = helper.getWritableDatabase();
        long gameId = db.insertOrThrow("Games", null, gameValues("Friday night", 1000L, 2000L));

        ContentValues alice = playerScoreValues("Alice", 3, 0, "1,2", gameId);
        alice.put("lastUpdate", 50);
        alice.put("historyTimestamps", "40,60");
        alice.put("color", "0");
        db.insertOrThrow("PlayerScores", null, alice);

        ContentValues bob = playerScoreValues("Bob", 7, 1, "7", gameId);
        bob.put("lastUpdate", 70);
        bob.put("historyTimestamps", "50");
        bob.put("color", "#ff00ff00");
        db.insertOrThrow("PlayerScores", null, bob);

        helper.close();
    }

    private void createLegacyV1Database() {
        SQLiteOpenHelper helper = new SQLiteOpenHelper(context, DB_NAME, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(V4_GAMES);
                db.execSQL(V1_PLAYER_SCORES);
                db.execSQL("create index if not exists index_game_id on PlayerScores (gameId);");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        };

        SQLiteDatabase db = helper.getWritableDatabase();
        long gameId = db.insertOrThrow("Games", null, gameValues("Legacy", 10L, 20L));
        db.insertOrThrow("PlayerScores", null, playerScoreValues("Carol", 5, 2, "5,3", gameId));

        helper.close();
    }

    private static ContentValues gameValues(String name, long dateStarted, long dateSaved) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("autosaved", 1);
        values.put("dateStarted", dateStarted);
        values.put("dateSaved", dateSaved);
        return values;
    }

    private static ContentValues playerScoreValues(String name, long score, int playerNumber, String history,
            long gameId) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("score", score);
        values.put("playerNumber", playerNumber);
        values.put("history", history);
        values.put("gameId", gameId);
        return values;
    }

    private static void assertForeignKey(SQLiteDatabase db, String table, String referencedTable, String from,
            String to, String onDelete) {
        try (Cursor cursor = db.rawQuery("PRAGMA foreign_key_list(" + table + ")", null)) {
            boolean found = false;
            while (cursor.moveToNext()) {
                if (referencedTable.equals(cursor.getString(cursor.getColumnIndexOrThrow("table")))
                        && from.equals(cursor.getString(cursor.getColumnIndexOrThrow("from")))
                        && to.equals(cursor.getString(cursor.getColumnIndexOrThrow("to")))
                        && onDelete.equalsIgnoreCase(cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))) {
                    found = true;
                    break;
                }
            }
            assertTrue("missing foreign key " + table + "." + from + " -> " + referencedTable + "." + to, found);
        }
    }

    private static void assertIndexExists(SQLiteDatabase db, String table, String indexName) {
        try (Cursor cursor = db.rawQuery("PRAGMA index_list(" + table + ")", null)) {
            boolean found = false;
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (indexName.equals(cursor.getString(nameIndex))) {
                    found = true;
                    break;
                }
            }
            assertTrue("missing index " + indexName + " on " + table, found);
        }
    }
}
