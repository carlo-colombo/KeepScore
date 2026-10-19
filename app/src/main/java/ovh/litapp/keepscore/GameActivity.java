package ovh.litapp.keepscore;

import ovh.litapp.keepscore.android.EdgeToEdge;
import ovh.litapp.keepscore.ui.scoring.ScoringPreferences;
import ovh.litapp.keepscore.ui.scoring.ScoringScreenHostKt;
import ovh.litapp.keepscore.ui.scoring.ScoringScreenState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.GameDBHelper;
import ovh.litapp.keepscore.db.PlayerScore;
import ovh.litapp.keepscore.helper.GameActivityHelper;
import ovh.litapp.keepscore.helper.PlayerColor;
import ovh.litapp.keepscore.helper.PreferenceHelper;
import ovh.litapp.keepscore.util.StopWatch;
import ovh.litapp.keepscore.util.UtilLogger;

/**
 * Main activity when the user is playing a game.
 *
 * The scoreboard itself is the Compose screen in {@code ui/scoring}, which owns every score
 * mutation, the undo/redo stack and the round totals; this activity owns the game object, the
 * autosave and the action bar menu, and hides the action bar while a game is on screen.
 *
 * @author nolan
 * 
 */
public abstract class GameActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAMES = "playerNames";
    public static final String EXTRA_PLAYER_COLORS = "playerColors";
    public static final String EXTRA_GAME_ID = "gameId";
    public static final String EXTRA_GAME = "game";

    private static final long PERIODIC_SAVE_PERIOD = TimeUnit.SECONDS.toMillis(30);

    private static final UtilLogger log = new UtilLogger(GameActivity.class);

    private Game game;
    protected List<PlayerScore> playerScores;

    private ScoringScreenState scoringScreenState;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean paused = true;
    private GameDBHelper dbHelper;
    private boolean savedGameBeforeExit;

    private final ActivityResultLauncher<Intent> organizePlayersActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    final List<PlayerScore> newPlayerScores = result.getData()
                            .getParcelableArrayListExtra(OrganizePlayersActivity.EXTRA_PLAYER_SCORES);
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            saveWithNewPlayerScores(newPlayerScores);
                        }
                    });
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createGame();

        setUpScoringScreen();
        // This activity hides the action bar in onResume(), after edge-to-edge insets are applied.
        EdgeToEdge.apply(this, false);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                if (gameWasSaved() || shouldAutosave()) {
                    // back button pressed
                    // if the game has been modified at all, skip showing the "enter player names"
                    // screen and just go directly to the main screen

                    Intent intent = new Intent(GameActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    // no saved game, so fall back to the default behavior of finishing this activity
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.d("onDestroy()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.d("onPause()");

        paused = true;

        if (shouldAutosave()) {
            saveGame(game, false, null);
        }

        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }

        if (savedGameBeforeExit) { // if nothing was changed in the game, don't
            // show this message
            Toast.makeText(this, R.string.toast_game_saved, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean useWakeLock = PreferenceHelper.getBooleanPreference(R.string.CONSTANT_pref_use_wake_lock,
                R.string.CONSTANT_pref_use_wake_lock_default, this);
        if (useWakeLock) {
            // FLAG_KEEP_SCREEN_ON replaces the deprecated FULL_WAKE_LOCK; the
            // window flag is scoped to this window and needs no permission.
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        startPeriodicSave();

        // the scoreboard draws itself from the preferences, so re-read them here: the user may
        // have changed the colour scheme, the button values or the update delay in the settings
        scoringScreenState.refreshPreferences();

        paused = false;
        savedGameBeforeExit = false;

        getSupportActionBar().hide();
    }

    private GameDBHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper = new GameDBHelper(this);
        }
        return dbHelper;
    }

    private void startPeriodicSave() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (shouldAutosave()) {
                    saveGame(game, true, new Runnable() {

                        @Override
                        public void run() {
                            if (!paused) {
                                startPeriodicSave();
                            }
                        }
                    });
                } else {
                    log.d("no need to do periodic save");
                    if (!paused) {
                        startPeriodicSave();
                    }
                }
            }
        }, PERIODIC_SAVE_PERIOD);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // show/hide undo/redo menu items
        MenuItem undoMenuItem = menu.findItem(R.id.menu_undo);
        MenuItem redoMenuItem = menu.findItem(R.id.menu_redo);

        boolean showUndo = scoringScreenState.canUndo();
        boolean showRedo = scoringScreenState.canRedo();

        undoMenuItem.setEnabled(showUndo);
        redoMenuItem.setEnabled(showRedo);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_history) {
            Intent historyIntent = new Intent(this, HistoryActivity.class);
            historyIntent.putExtra(HistoryActivity.EXTRA_GAME, game);
            startActivity(historyIntent);
        } else if (itemId == R.id.menu_settings) {
            Intent settingsIntent = new Intent(GameActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (itemId == R.id.menu_add_edit_players) {
            startOrganizePlayersActivity();
        } else if (itemId == R.id.menu_rematch) {
            showRematchDialogue();
        } else if (itemId == R.id.menu_undo) {
            scoringScreenState.undoOrRedo(true);
        } else if (itemId == R.id.menu_redo) {
            scoringScreenState.undoOrRedo(false);
        }
        return false;
    }

    private void startOrganizePlayersActivity() {

        Intent intent = new Intent(this, OrganizePlayersActivity.class);
        intent.putExtra(EXTRA_GAME, game);
        organizePlayersActivityLauncher.launch(intent);
    }

    private void showRematchDialogue() {
        new AlertDialog.Builder(this).setCancelable(true).setTitle(R.string.text_confirm)
                .setMessage(R.string.text_confirm_rematch).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createRematchGame();
                    }
                }).show();

    }

    private void createRematchGame() {

        saveGame(game, true, null); // save the original game

        game = game.makeCleanCopy();
        playerScores = game.getPlayerScores();

        // a rematch is a new game: give it a scoreboard of its own, with an empty undo stack
        setUpScoringScreen();
        scoringScreenState.resetForRematch();
        saveGame(game, true, null); // save the new game

        Toast.makeText(this, R.string.toast_rematch_created, Toast.LENGTH_SHORT).show();
    }

    private boolean gameWasSaved() {
        return game != null && game.getId() != -1;
    }

    private boolean shouldAutosave() {
        // only autosave if the user has changed SOMETHING, i.e. the scores
        // aren't all just zero
        return scoringScreenState.shouldAutosave();
    }

    private void createGame() {

        if (getIntent().hasExtra(EXTRA_PLAYER_NAMES)) {
            // starting a new game
            createNewGame();
        } else if (getIntent().hasExtra(EXTRA_GAME_ID)) {
            // loading an existing game
            createExistingGameFromId();
        } else {
            // game object parceled into intent
            game = getIntent().getParcelableExtra(EXTRA_GAME);
            playerScores = game.getPlayerScores();
            log.d("unparceled game is: %s", game);
        }

        log.d("loaded game: %s", game);
        log.d("loaded playerScores: %s", playerScores);
    }

    private void createExistingGameFromId() {
        int gameId = getIntent().getIntExtra(EXTRA_GAME_ID, 0);

        game = getDbHelper().findGameById(gameId);
        playerScores = game.getPlayerScores();
    }

    private void createNewGame() {

        String[] playerNames = getIntent().getStringArrayExtra(EXTRA_PLAYER_NAMES);
        String[] playerColors = getIntent().getStringArrayExtra(EXTRA_PLAYER_COLORS);

        game = new Game();

        playerScores = new ArrayList<PlayerScore>();

        game.setDateStarted(System.currentTimeMillis());
        game.setPlayerScores(playerScores);

        for (int i = 0; i < playerNames.length; i++) {

            PlayerScore playerScore = new PlayerScore();

            playerScore.setName(playerNames[i]);
            playerScore.setPlayerColor(PlayerColor.deserialize(playerColors[i]));
            playerScore.setPlayerNumber(i);
            playerScore.setHistory(new ArrayList<Delta>());
            playerScore.setScore(PreferenceHelper.getIntPreference(R.string.CONSTANT_pref_initial_score,
                    R.string.CONSTANT_pref_initial_score_default, GameActivity.this));

            playerScores.add(playerScore);
        }

        log.d("created new game: %s", game);
        log.d("created new playerScores: %s", playerScores);
    }

    /**
     * Puts the scoreboard of the current {@link #playerScores} on screen, replacing the one of the
     * previous game if there was one.
     */
    private void setUpScoringScreen() {
        scoringScreenState = new ScoringScreenState(playerScores, ScoringPreferences.from(this));

        // set to autosave if the player names are filled in. This feels
        // intuitive to me. There's no point
        // in saving an empty game, but if the player names are included,
        // the game feels non-empty and therefore
        // worth saving. This only applies for newly created games.
        scoringScreenState.markDirtyForUnsavedNewGame(game.getId());

        ScoringScreenHostKt.setScoringScreenContent(this, scoringScreenState);
    }

    private synchronized void saveGame(final Game gameToSave, boolean inBackground, final Runnable onFinished) {

        scoringScreenState.markSaved();

        if (inBackground) {
            // do in the background to avoid jankiness
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {

                    saveGame(gameToSave);

                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    if (onFinished != null) {
                        onFinished.run();
                    }
                }

            }.execute((Void) null);
        } else {
            // do in foreground to ensure the game gets saved before the
            // activity finishes
            saveGame(gameToSave);
            if (onFinished != null) {
                onFinished.run();
            }
        }
    }

    private synchronized void saveGame(Game gameToSave) {
        StopWatch stopWatch = new StopWatch("saveGame()");

        getDbHelper().saveGame(gameToSave);
        log.d("saved game: %s", gameToSave);
        savedGameBeforeExit = true;

        stopWatch.log(log);
    }

    private void saveWithNewPlayerScores(final List<PlayerScore> newPlayerScores) {

        // delete the game and recreate it with the new data

        // do in the background to avoid jank
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                // delete the old game before starting new one
                getDbHelper().deleteGame(game);
                // after this, because the id is not -1, only UPDATEs will be
                // performed,
                // so the delete is clean even if the background saver keeps
                // running

                final Game newGame = (Game) game.clone();
                newGame.setId(-1);
                newGame.setPlayerScores(newPlayerScores);
                for (PlayerScore playerScore : newGame.getPlayerScores()) {
                    playerScore.setId(-1);
                }

                Runnable onFinished = new Runnable() {

                    @Override
                    public void run() {
                        log.d("game to parcel is: %s", game);

                        // start a new activity so that the layout can refresh
                        // correctly
                        // TODO: don't start a new activity; just refresh the
                        // layout

                        GameActivityHelper.openGameWithClearTop(GameActivity.this, newGame);
                    }

                };
                saveGame(newGame, true, onFinished); // automatically save the
                                                     // game

                return null;
            }
        }.execute((Void) null);

    }

}
