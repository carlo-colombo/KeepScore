package ovh.litapp.keepscore.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import androidx.room.Room;

import ovh.litapp.keepscore.helper.PlayerColor;
import ovh.litapp.keepscore.util.Pair;
import ovh.litapp.keepscore.util.StringUtil;
import ovh.litapp.keepscore.util.UtilLogger;

/**
 * Data access for saved games. This used to be a hand-written {@code SQLiteOpenHelper}; it now
 * delegates to Room while keeping its public API (and therefore every call site) unchanged.
 *
 * <p>The database file name and schema are unchanged, so an existing "games.db" is upgraded by
 * {@link AppDatabase}'s migrations. Room normally forbids database access on the main thread,
 * but legacy call sites (notably {@code GameActivity}'s foreground autosave and
 * {@code createExistingGameFromId}) query synchronously from the UI thread, so main-thread
 * queries are allowed to preserve the existing behavior.
 */
public class GameDBHelper {

    private static UtilLogger log = new UtilLogger(GameDBHelper.class);

    private static final String DB_NAME = "games.db";

    private final AppDatabase database;
    private final GameDao dao;

    public GameDBHelper(Context context) {
        database = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(AppDatabase.MIGRATIONS)
                .build();
        dao = database.gameDao();
    }

    public void close() {
        synchronized (GameDBHelper.class) {
            database.close();
        }
    }

    /**
     * Return true if a game with that dateStarted value exists.
     */
    public boolean existsByDateStarted(long dateStarted) {
        synchronized (GameDBHelper.class) {
            return dao.findGameIdByDateStarted(dateStarted) != null;
        }
    }

    public Game findGameById(int gameId) {
        synchronized (GameDBHelper.class) {
            GameEntity entity = dao.findGameEntityById(gameId);
            if (entity == null) {
                return null;
            }
            return toGame(entity, dao.findPlayerScoresByGameId(gameId));
        }
    }

    public int findGameCount() {
        synchronized (GameDBHelper.class) {
            return dao.findGameCount();
        }
    }

    public int findMostRecentGameId() {
        synchronized (GameDBHelper.class) {
            Integer gameId = dao.findMostRecentGameId();
            return gameId != null ? gameId : -1;
        }
    }

    public Game findMostRecentGame() {
        synchronized (GameDBHelper.class) {
            GameEntity entity = dao.findMostRecentGameEntity();
            if (entity == null) {
                return null;
            }
            return toGame(entity, dao.findPlayerScoresByGameId(entity.id));
        }
    }

    /**
     * save a game, updating its 'dateSaved' value
     */
    public void saveGame(Game game) {
        saveGame(game, true);
    }

    /**
     * save a game, optionally updating its 'dateSaved' value
     */
    public void saveGame(Game game, boolean updateDateSaved) {
        synchronized (GameDBHelper.class) {
            database.runInTransaction(() -> saveGameWithinTransaction(game, updateDateSaved));
        }
    }

    private void saveGameWithinTransaction(Game game, boolean updateDateSaved) {
        long dateSaved = updateDateSaved ? System.currentTimeMillis() : game.getDateSaved();
        game.setDateSaved(dateSaved);

        if (game.getId() != -1) {
            // game was already saved, so try to overwrite
            dao.updateGame(toGameEntity(game));
        } else {
            // else create a new row in the table
            int newGameId = (int) dao.insertGame(toGameEntity(game));
            game.setId(newGameId);
            log.d("new game id is %s", newGameId);
        }

        savePlayerScoresWithinTransaction(game.getId(), game.getPlayerScores());
    }

    private void savePlayerScoresWithinTransaction(int gameId, List<PlayerScore> playerScores) {
        for (PlayerScore playerScore : playerScores) {
            if (playerScore.getId() != -1) {
                // already exists; update
                dao.updatePlayerScore(toPlayerScoreEntity(gameId, playerScore));
            } else {
                // else insert a new row in the table
                int newId = (int) dao.insertPlayerScore(toPlayerScoreEntity(gameId, playerScore));
                playerScore.setId(newId);
                log.d("new playerScore id is %s", newId);
            }
        }
    }

    public List<Game> findAllGames() {
        synchronized (GameDBHelper.class) {
            List<GameEntity> gameEntities = dao.findAllGames();
            Map<Integer, List<PlayerScoreEntity>> playerScoresByGameId = groupByGameId(dao.findAllPlayerScores());

            List<Game> result = new ArrayList<Game>(gameEntities.size());
            for (GameEntity entity : gameEntities) {
                result.add(toGame(entity, playerScoresByGameId.get(entity.id)));
            }
            return result;
        }
    }

    public List<GameSummary> findAllGameSummaries() {
        synchronized (GameDBHelper.class) {
            List<GameEntity> gameEntities = dao.findAllGames();
            Map<Integer, List<PlayerScoreEntity>> playerScoresByGameId = groupByGameId(dao.findAllPlayerScores());

            List<GameSummary> result = new ArrayList<GameSummary>(gameEntities.size());
            for (GameEntity entity : gameEntities) {
                List<PlayerScoreEntity> playerScores = playerScoresByGameId.get(entity.id);

                GameSummary gameSummary = new GameSummary();
                gameSummary.setId(entity.id);
                gameSummary.setName(entity.name);
                gameSummary.setDateSaved(entity.dateSaved);

                List<String> playerNames = new ArrayList<String>();
                int numRounds = 0;
                if (playerScores != null) {
                    for (PlayerScoreEntity playerScore : playerScores) {
                        playerNames.add(playerScore.name);
                        // replicate the old SQL: number of commas in the history string, plus one
                        // (a NULL history contributes nothing to the max)
                        numRounds = Math.max(numRounds, numberOfRounds(playerScore.history));
                    }
                }
                gameSummary.setPlayerNames(playerNames);
                gameSummary.setNumRounds(numRounds);

                result.add(gameSummary);
            }
            return result;
        }
    }

    public void deleteGame(Game game) {
        synchronized (GameDBHelper.class) {
            final int id = game.getId();
            database.runInTransaction(() -> {
                dao.deletePlayerScoresByGameId(id);
                dao.deleteGameById(id);
            });
        }
    }

    public void updateGameName(int gameId, String newName) {
        synchronized (GameDBHelper.class) {
            dao.updateGameName(gameId, newName);
        }
    }

    public List<String> findDistinctPlayerNames() {
        synchronized (GameDBHelper.class) {
            return new ArrayList<String>(dao.findDistinctPlayerNames());
        }
    }

    public void deleteGames(Collection<Integer> gameIds) {
        synchronized (GameDBHelper.class) {
            if (gameIds.isEmpty()) {
                return;
            }
            database.runInTransaction(() -> {
                dao.deletePlayerScoresByGameIds(gameIds);
                dao.deleteGamesByIds(gameIds);
            });
        }
    }

    private static Map<Integer, List<PlayerScoreEntity>> groupByGameId(List<PlayerScoreEntity> playerScores) {
        Map<Integer, List<PlayerScoreEntity>> result = new HashMap<Integer, List<PlayerScoreEntity>>();
        for (PlayerScoreEntity playerScore : playerScores) {
            List<PlayerScoreEntity> scoresForGame = result.get(playerScore.gameId);
            if (scoresForGame == null) {
                scoresForGame = new ArrayList<PlayerScoreEntity>();
                result.put(playerScore.gameId, scoresForGame);
            }
            scoresForGame.add(playerScore);
        }
        return result;
    }

    private static int numberOfRounds(String history) {
        if (history == null) {
            return 0;
        }
        int commas = 0;
        for (int i = 0, len = history.length(); i < len; i++) {
            if (history.charAt(i) == ',') {
                commas++;
            }
        }
        return commas + 1;
    }

    private static GameEntity toGameEntity(Game game) {
        GameEntity entity = new GameEntity();
        entity.id = game.getId() == -1 ? 0 : game.getId();
        entity.name = game.getName();
        entity.autosaved = 1; // legacy column that must be specified
        entity.dateStarted = game.getDateStarted();
        entity.dateSaved = game.getDateSaved();
        return entity;
    }

    private static PlayerScoreEntity toPlayerScoreEntity(int gameId, PlayerScore playerScore) {
        Pair<String, String> historyAsStrings = Delta.toJoinedStrings(playerScore.getHistory());

        PlayerScoreEntity entity = new PlayerScoreEntity();
        entity.id = playerScore.getId() == -1 ? 0 : playerScore.getId();
        entity.name = StringUtil.nullToEmpty(playerScore.getName());
        entity.score = playerScore.getScore();
        entity.playerNumber = playerScore.getPlayerNumber();
        entity.history = historyAsStrings.getFirst();
        entity.historyTimestamps = historyAsStrings.getSecond();
        entity.lastUpdate = playerScore.getLastUpdate();
        entity.color = PlayerColor.serialize(playerScore.getPlayerColor());
        entity.gameId = gameId;
        return entity;
    }

    private static Game toGame(GameEntity entity, List<PlayerScoreEntity> playerScoreEntities) {
        Game game = new Game();
        game.setId(entity.id);
        game.setName(entity.name);
        game.setDateStarted(entity.dateStarted);
        game.setDateSaved(entity.dateSaved);

        List<PlayerScore> playerScores = new ArrayList<PlayerScore>(
                playerScoreEntities != null ? playerScoreEntities.size() : 0);
        if (playerScoreEntities != null) {
            for (PlayerScoreEntity playerScoreEntity : playerScoreEntities) {
                playerScores.add(toPlayerScore(playerScoreEntity));
            }
        }
        Collections.sort(playerScores, PlayerScore.sortByPlayerNumber());
        game.setPlayerScores(playerScores);
        return game;
    }

    private static PlayerScore toPlayerScore(PlayerScoreEntity entity) {
        PlayerScore playerScore = new PlayerScore();
        playerScore.setId(entity.id);
        playerScore.setName(entity.name);
        playerScore.setScore(entity.score);
        playerScore.setPlayerNumber(entity.playerNumber);
        playerScore.setHistory(Delta.fromJoinedStrings(
                StringUtil.nullToEmpty(entity.history),
                StringUtil.nullToEmpty(entity.historyTimestamps)));
        playerScore.setLastUpdate(entity.lastUpdate);
        playerScore.setPlayerColor(entity.color == null
                ? PlayerColor.BUILT_INS[entity.playerNumber]
                : PlayerColor.deserialize(entity.color));
        return playerScore;
    }
}
