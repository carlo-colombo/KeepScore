package ovh.litapp.keepscore.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
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
 * Robolectric tests for the SQLite layer in {@link GameDBHelper}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameDBHelperTest {

    // GameDBHelper.DB_NAME is private, and the helper always opens this file
    private static final String DATABASE_NAME = "games.db";

    private GameDBHelper dbHelper;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DATABASE_NAME);
        dbHelper = new GameDBHelper(context);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void saveGamePersistsGamePlayerScoresAndHistory() {
        Game game = createGame("Friday night", 1000L, 2000L,
                createPlayerScore("Bob", 1, 7L, PlayerColor.BUILT_INS[1], new Delta(50L, 7)),
                createPlayerScore("Alice", 0, 3L, new CustomPlayerColor(Color.parseColor("#FF00FF00")),
                        new Delta(40L, 1), new Delta(60L, 2)));
        long beforeSave = System.currentTimeMillis();
        dbHelper.saveGame(game);

        assertTrue(game.getId() >= 0);
        for (PlayerScore playerScore : game.getPlayerScores()) {
            assertTrue(playerScore.getId() >= 0);
        }

        Game found = dbHelper.findGameById(game.getId());
        assertNotNull(found);
        assertEquals(game.getId(), found.getId());
        assertEquals("Friday night", found.getName());
        assertEquals(1000L, found.getDateStarted());
        // saveGame() stamps the game with the current time, rather than retaining its old value.
        assertTrue(game.getDateSaved() >= beforeSave);
        assertEquals(game.getDateSaved(), found.getDateSaved());

        // player scores come back sorted by player number, not in insertion order
        assertEquals(2, found.getPlayerScores().size());

        PlayerScore alice = found.getPlayerScores().get(0);
        assertEquals("Alice", alice.getName());
        assertEquals(0, alice.getPlayerNumber());
        assertEquals(3L, alice.getScore());
        assertEquals(Arrays.asList(new Delta(40L, 1), new Delta(60L, 2)), alice.getHistory());
        assertEquals(new CustomPlayerColor(Color.parseColor("#FF00FF00")), alice.getPlayerColor());

        PlayerScore bob = found.getPlayerScores().get(1);
        assertEquals("Bob", bob.getName());
        assertEquals(1, bob.getPlayerNumber());
        assertEquals(7L, bob.getScore());
        assertEquals(Collections.singletonList(new Delta(50L, 7)), bob.getHistory());
        assertEquals(PlayerColor.BUILT_INS[1], bob.getPlayerColor());

        assertEquals(1, dbHelper.findGameCount());
        assertEquals(game.getId(), dbHelper.findMostRecentGameId());
        assertEquals(1, dbHelper.findAllGames().size());
    }

    @Test
    public void deleteGameRemovesGameAndItsPlayerScores() {
        Game doomed = createGame("doomed", 1000L, 1000L,
                createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]));
        Game survivor = createGame("survivor", 2000L, 2000L,
                createPlayerScore("Bob", 0, 2L, PlayerColor.BUILT_INS[1]));
        dbHelper.saveGame(doomed, false);
        dbHelper.saveGame(survivor, false);

        dbHelper.deleteGame(doomed);

        assertNull(dbHelper.findGameById(doomed.getId()));
        assertFalse(dbHelper.existsByDateStarted(1000L));
        assertEquals(1, dbHelper.findGameCount());

        Game foundSurvivor = dbHelper.findGameById(survivor.getId());
        assertNotNull(foundSurvivor);
        assertEquals("survivor", foundSurvivor.getName());
        assertEquals(Collections.singletonList("Bob"), dbHelper.findDistinctPlayerNames());
    }

    @Test
    public void saveGameUpdatePersistsChangesWithoutChangingIdsOrAddingRows() {
        Game game = createGame("before", 1000L, 2000L,
                createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0], new Delta(10L, 1)));
        dbHelper.saveGame(game, false);
        int gameId = game.getId();
        int playerScoreId = game.getPlayerScores().get(0).getId();

        game.setName("after");
        PlayerScore playerScore = game.getPlayerScores().get(0);
        playerScore.setScore(3L);
        playerScore.setHistory(new ArrayList<Delta>(Arrays.asList(new Delta(10L, 1), new Delta(20L, 2))));
        dbHelper.saveGame(game, false);

        Game found = dbHelper.findGameById(gameId);
        assertNotNull(found);
        assertEquals(1, dbHelper.findGameCount());
        assertEquals(gameId, game.getId());
        assertEquals(gameId, found.getId());
        assertEquals("after", found.getName());
        assertEquals(1, found.getPlayerScores().size());
        assertEquals(playerScoreId, playerScore.getId());
        assertEquals(playerScoreId, found.getPlayerScores().get(0).getId());
        assertEquals(3L, found.getPlayerScores().get(0).getScore());
        assertEquals(Arrays.asList(new Delta(10L, 1), new Delta(20L, 2)),
                found.getPlayerScores().get(0).getHistory());
        assertEquals(2000L, found.getDateSaved());
    }

    @Test
    public void saveGameCanPreserveDateSavedWhenUpdateIsDisabled() {
        Game game = createGame("unchanged date", 1000L, 2000L,
                createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]));
        dbHelper.saveGame(game, false);

        assertEquals(2000L, game.getDateSaved());
        assertEquals(2000L, dbHelper.findGameById(game.getId()).getDateSaved());
    }

    @Test
    public void findAllGameSummariesReportsZeroRoundsForEmptyHistories() {
        Game game = createGame("new game", 1000L, 2000L,
                createPlayerScore("Alice", 0, 0L, PlayerColor.BUILT_INS[0]));
        GameSummary inMemorySummary = GameSummary.fromGame(game);
        dbHelper.saveGame(game, false);

        GameSummary databaseSummary = dbHelper.findAllGameSummaries().get(0);

        assertEquals(0, inMemorySummary.getNumRounds());
        assertEquals(0, databaseSummary.getNumRounds());
        assertEquals(inMemorySummary.getNumRounds(), databaseSummary.getNumRounds());
    }

    @Test
    public void findAllGameSummariesGroupsPlayerNamesAndRoundCounts() {
        dbHelper.saveGame(createGame("middle", 2000L, 2000L,
                createPlayerScore("Zoe", 1, 5L, PlayerColor.BUILT_INS[1], new Delta(1L, 5)),
                createPlayerScore("Alice", 0, 4L, PlayerColor.BUILT_INS[0], new Delta(1L, 1),
                        new Delta(2L, 3))), false);
        dbHelper.saveGame(createGame("newest", 3000L, 3000L,
                createPlayerScore("Carol", 0, 1L, PlayerColor.BUILT_INS[2], new Delta(1L, 1))), false);
        dbHelper.saveGame(createGame("oldest", 1000L, 1000L,
                createPlayerScore("Dave", 0, 2L, PlayerColor.BUILT_INS[3], new Delta(1L, 2),
                        new Delta(2L, 4), new Delta(3L, 6))), false);

        List<GameSummary> summaries = dbHelper.findAllGameSummaries();

        assertEquals(3, summaries.size());

        GameSummary middle = summaryNamed(summaries, "middle");
        assertEquals(2000L, middle.getDateSaved());
        // player names are ordered by player number, whatever order group_concat produces
        assertEquals(Arrays.asList("Alice", "Zoe"), middle.getPlayerNames());
        assertEquals(2, middle.getNumRounds());

        GameSummary newest = summaryNamed(summaries, "newest");
        assertEquals(Arrays.asList("Carol"), newest.getPlayerNames());
        assertEquals(1, newest.getNumRounds());

        GameSummary oldest = summaryNamed(summaries, "oldest");
        assertEquals(Arrays.asList("Dave"), oldest.getPlayerNames());
        assertEquals(3, oldest.getNumRounds());
    }

    @Test
    public void updateNamesAndBulkDeleteGames() {
        Game keep = createGame("keep", 1000L, 1000L,
                createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]));
        Game delete = createGame("delete", 2000L, 2000L,
                createPlayerScore("Bob", 0, 2L, PlayerColor.BUILT_INS[1]));
        dbHelper.saveGame(keep, false);
        dbHelper.saveGame(delete, false);

        assertTrue(dbHelper.existsByDateStarted(1000L));
        dbHelper.updateGameName(keep.getId(), "renamed");
        assertEquals("renamed", dbHelper.findGameById(keep.getId()).getName());
        dbHelper.deleteGames(Arrays.asList(delete.getId()));

        assertNull(dbHelper.findGameById(delete.getId()));
        assertFalse(dbHelper.existsByDateStarted(2000L));
        assertEquals(1, dbHelper.findGameCount());
        assertEquals(1, dbHelper.findGameById(keep.getId()).getPlayerScores().size());
    }

    @Test
    public void findMostRecentGameIdIsMinusOneWhenThereAreNoGames() {
        assertEquals(-1, dbHelper.findMostRecentGameId());
    }

    @Test
    public void findMostRecentGameIsTheOneSavedLast() {
        Game first = createGame("first", 1000L, 1000L,
                createPlayerScore("Alice", 0, 1L, PlayerColor.BUILT_INS[0]));
        Game last = createGame("last", 2000L, 2000L,
                createPlayerScore("Bob", 0, 2L, PlayerColor.BUILT_INS[1]));
        dbHelper.saveGame(first, false);
        dbHelper.saveGame(last, false);

        assertEquals(last.getId(), dbHelper.findMostRecentGameId());
        assertEquals("last", dbHelper.findMostRecentGame().getName());
    }

    private GameSummary summaryNamed(List<GameSummary> summaries, String name) {
        for (GameSummary summary : summaries) {
            if (name.equals(summary.getName())) {
                return summary;
            }
        }
        throw new AssertionError("no summary named " + name + " in " + summaries);
    }

    private Game createGame(String name, long dateStarted, long dateSaved, PlayerScore... playerScores) {
        Game game = new Game();
        game.setName(name);
        game.setDateStarted(dateStarted);
        game.setDateSaved(dateSaved);
        game.setPlayerScores(new ArrayList<PlayerScore>(Arrays.asList(playerScores)));
        return game;
    }

    private PlayerScore createPlayerScore(String name, int playerNumber, long score, PlayerColor color,
            Delta... history) {
        PlayerScore playerScore = new PlayerScore();
        playerScore.setName(name);
        playerScore.setPlayerNumber(playerNumber);
        playerScore.setScore(score);
        playerScore.setPlayerColor(color);
        playerScore.setHistory(new ArrayList<Delta>(Arrays.asList(history)));
        return playerScore;
    }
}
