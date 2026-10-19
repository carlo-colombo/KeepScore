package ovh.litapp.keepscore.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import ovh.litapp.keepscore.KeepScoreApplication;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.PlayerScore;
import ovh.litapp.keepscore.helper.PlayerColor;
import ovh.litapp.keepscore.helper.SdcardHelper;
import ovh.litapp.keepscore.helper.PlayerColor.CustomPlayerColor;

/**
 * Round-trip tests for {@link GamesBackupSerializer}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = GamesBackupSerializerTest.TestKeepScoreApplication.class)
public class GamesBackupSerializerTest {

    /** Sets the singleton required by SdcardHelper without scheduling unrelated background work. */
    public static class TestKeepScoreApplication extends KeepScoreApplication {
        @Override
        public void onCreate() {
            try {
                Field instance = KeepScoreApplication.class.getDeclaredField("instance");
                instance.setAccessible(true);
                instance.set(null, this);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static final long BACKUP_DATE_SAVED = 1332617241000L;

    @Test
    public void roundTripPreservesGamesPlayerScoresAndHistory() {
        GamesBackup gamesBackup = createGamesBackup();

        GamesBackup deserialized = roundTrip(gamesBackup);

        assertGamesBackupEquals(gamesBackup, deserialized);
    }

    @Test
    public void deserializesVersionFourBackupWithoutColorsOrHistoryTimestamps() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GamesBackup><gameCount>1</gameCount><version>4</version><automatic>false</automatic>"
                + "<backupFilename>legacy.xml</backupFilename><dateBackupSaved>1332617241000</dateBackupSaved>"
                + "<Games><Game><dateGameSaved>1332617240000</dateGameSaved>"
                + "<dateGameStarted>1332617000000</dateGameStarted><gameName>R&amp;D &lt;Friday&gt;</gameName>"
                + "<PlayerScores>"
                + "<PlayerScore><playerName>Alice &amp; Bob &lt;captain&gt;</playerName><score>12</score>"
                + "<playerNumber>4</playerNumber><history>5,7</history><lastUpdate>1332617239000</lastUpdate></PlayerScore>"
                + "<PlayerScore><playerName>Tom &amp; Jerry</playerName><score>9</score>"
                + "<playerNumber>9</playerNumber><history>-2,3,8</history><lastUpdate>1332617238000</lastUpdate></PlayerScore>"
                + "</PlayerScores></Game></Games></GamesBackup>";

        GamesBackup deserialized = GamesBackupSerializer.deserialize(xml);

        assertEquals(4, deserialized.getVersion());
        assertEquals(1, deserialized.getGames().size());
        Game game = deserialized.getGames().get(0);
        assertEquals("R&D <Friday>", game.getName());
        assertEquals(2, game.getPlayerScores().size());

        PlayerScore first = game.getPlayerScores().get(0);
        assertEquals("Alice & Bob <captain>", first.getName());
        assertEquals(PlayerColor.BUILT_INS[4], first.getPlayerColor());
        assertEquals(Arrays.asList(new Delta(0L, 5), new Delta(0L, 7)), first.getHistory());

        PlayerScore second = game.getPlayerScores().get(1);
        assertEquals("Tom & Jerry", second.getName());
        assertEquals(PlayerColor.BUILT_INS[9], second.getPlayerColor());
        assertEquals(Arrays.asList(new Delta(0L, -2), new Delta(0L, 3), new Delta(0L, 8)), second.getHistory());
        for (PlayerScore playerScore : game.getPlayerScores()) {
            for (Delta delta : playerScore.getHistory()) {
                assertEquals(0L, delta.getTimestamp());
            }
        }
    }

    @Test
    public void gzipSaveAndOpenPreservesSerializedXmlThroughContentResolver() {
        Context context = ApplicationProvider.getApplicationContext();
        String xml = GamesBackupSerializer.serialize(createGamesBackup());
        String filename = "serializer-round-trip-" + UUID.randomUUID() + ".xml.gz";
        File file = SdcardHelper.getFile(filename, SdcardHelper.Location.Backups);
        assertNotNull(file);

        try {
            assertTrue(SdcardHelper.save(filename, SdcardHelper.Format.GZIP,
                    SdcardHelper.Location.Backups, xml));
            assertTrue(file.exists());

            String readBack = SdcardHelper.open(Uri.fromFile(file), SdcardHelper.Format.GZIP,
                    context.getContentResolver());

            assertEquals(xml, readBack);
        } finally {
            if (file.exists()) {
                assertTrue("temporary backup file should be deleted", file.delete());
            }
        }
    }

    @Test
    public void roundTripPreservesNullAndEmptyNames() {
        GamesBackup deserialized = roundTrip(createGamesBackup());

        Game nullGame = deserialized.getGames().get(1);
        assertNull(nullGame.getName());
        assertNull(nullGame.getPlayerScores().get(0).getName());
        assertEquals("", nullGame.getPlayerScores().get(1).getName());

        Game emptyNameGame = deserialized.getGames().get(2);
        assertEquals("", emptyNameGame.getName());
    }

    @Test
    public void roundTripPreservesEmptyPlayerHistory() {
        GamesBackup deserialized = roundTrip(createGamesBackup());

        PlayerScore playerWithNoHistory = deserialized.getGames().get(1).getPlayerScores().get(0);
        assertTrue(playerWithNoHistory.getHistory().isEmpty());
    }

    @Test
    public void roundTripPreservesBuiltInAndCustomPlayerColors() {
        GamesBackup deserialized = roundTrip(createGamesBackup());

        assertEquals(PlayerColor.BUILT_INS[0], deserialized.getGames().get(0).getPlayerScores().get(0)
                .getPlayerColor());
        assertEquals(PlayerColor.BUILT_INS[7], deserialized.getGames().get(0).getPlayerScores().get(1)
                .getPlayerColor());

        PlayerColor customColor = deserialized.getGames().get(2).getPlayerScores().get(0).getPlayerColor();
        assertTrue(customColor instanceof CustomPlayerColor);
        assertEquals(Color.parseColor("#FF00FF00"), ((CustomPlayerColor) customColor).getColor());

        PlayerColor translucentCustomColor = deserialized.getGames().get(2).getPlayerScores().get(1)
                .getPlayerColor();
        assertTrue(translucentCustomColor instanceof CustomPlayerColor);
        assertEquals(Color.parseColor("#80123456"), ((CustomPlayerColor) translucentCustomColor).getColor());
    }

    @Test
    public void roundTripPreservesEmptyGameList() {
        GamesBackup gamesBackup = createGamesBackup();
        gamesBackup.setGames(Collections.<Game>emptyList());
        gamesBackup.setGameCount(0);

        GamesBackup deserialized = roundTrip(gamesBackup);

        assertGamesBackupEquals(gamesBackup, deserialized);
        assertTrue(deserialized.getGames().isEmpty());
    }

    private GamesBackup roundTrip(GamesBackup gamesBackup) {
        String xmlData = GamesBackupSerializer.serialize(gamesBackup);
        return GamesBackupSerializer.deserialize(xmlData);
    }

    private GamesBackup createGamesBackup() {
        List<Game> games = new ArrayList<Game>();

        // fully populated game with built-in colors and score histories
        Game game = createGame("Friday night", 1332617000000L, 1332617241000L);
        game.setPlayerScores(Arrays.asList(
                createPlayerScore("Alice", 0, 12L, PlayerColor.BUILT_INS[0],
                        new Delta(1332617000001L, 5), new Delta(1332617000002L, 7)),
                createPlayerScore("Bob", 1, 9L, PlayerColor.BUILT_INS[7],
                        new Delta(1332617000003L, 4), new Delta(1332617000004L, 5))));
        games.add(game);

        // null and empty names, a player without any history, and a never-updated player
        game = createGame(null, 1332617100000L, 1332617242000L);
        PlayerScore emptyHistoryPlayer = createPlayerScore(null, 0, 0L, PlayerColor.BUILT_INS[15]);
        emptyHistoryPlayer.setLastUpdate(0L);
        game.setPlayerScores(Arrays.asList(emptyHistoryPlayer,
                createPlayerScore("", 1, 3L, PlayerColor.BUILT_INS[1], new Delta(1332617100001L, 3))));
        games.add(game);

        // empty game name and custom player colors, including a translucent one
        game = createGame("", 1332617200000L, 1332617243000L);
        game.setPlayerScores(Arrays.asList(
                createPlayerScore("Carol", 0, 1L, new CustomPlayerColor(Color.parseColor("#FF00FF00")),
                        new Delta(1332617200001L, 1)),
                createPlayerScore("Dave", 1, 2L, new CustomPlayerColor(Color.parseColor("#80123456")),
                        new Delta(1332617200002L, 2))));
        games.add(game);

        GamesBackup gamesBackup = new GamesBackup();
        gamesBackup.setVersion(GamesBackupSerializer.CURRENT_VERSION);
        gamesBackup.setDateSaved(BACKUP_DATE_SAVED);
        gamesBackup.setAutomatic(false);
        gamesBackup.setFilename("games-20120324.xml.gz");
        gamesBackup.setGames(games);
        gamesBackup.setGameCount(games.size());
        return gamesBackup;
    }

    private Game createGame(String name, long dateStarted, long dateSaved) {
        Game game = new Game();
        game.setName(name);
        game.setDateStarted(dateStarted);
        game.setDateSaved(dateSaved);
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
        playerScore.setLastUpdate(1332617000000L + playerNumber);
        return playerScore;
    }

    private void assertGamesBackupEquals(GamesBackup expected, GamesBackup actual) {
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getDateSaved(), actual.getDateSaved());
        assertEquals(expected.getGameCount(), actual.getGameCount());
        assertEquals(expected.isAutomatic(), actual.isAutomatic());
        assertEquals(expected.getFilename(), actual.getFilename());
        assertEquals(expected.getGames().size(), actual.getGames().size());

        for (int i = 0; i < expected.getGames().size(); i++) {
            assertGameEquals(expected.getGames().get(i), actual.getGames().get(i));
        }
    }

    private void assertGameEquals(Game expected, Game actual) {
        assertNullableEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDateStarted(), actual.getDateStarted());
        assertEquals(expected.getDateSaved(), actual.getDateSaved());
        assertEquals(expected.getPlayerScores().size(), actual.getPlayerScores().size());

        for (int i = 0; i < expected.getPlayerScores().size(); i++) {
            assertPlayerScoreEquals(expected.getPlayerScores().get(i), actual.getPlayerScores().get(i));
        }
    }

    private void assertPlayerScoreEquals(PlayerScore expected, PlayerScore actual) {
        assertNullableEquals(expected.getName(), actual.getName());
        assertEquals(expected.getPlayerNumber(), actual.getPlayerNumber());
        assertEquals(expected.getScore(), actual.getScore());
        assertEquals(expected.getHistory(), actual.getHistory());
        assertEquals(expected.getLastUpdate(), actual.getLastUpdate());
        assertEquals(expected.getPlayerColor(), actual.getPlayerColor());
    }

    private void assertNullableEquals(String expected, String actual) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(expected, actual);
        }
    }
}
