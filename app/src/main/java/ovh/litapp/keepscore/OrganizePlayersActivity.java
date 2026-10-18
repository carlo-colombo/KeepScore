package ovh.litapp.keepscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import ovh.litapp.keepscore.android.AppCompatListActivity;
import ovh.litapp.keepscore.data.EditablePlayerAdapter;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.PlayerScore;
import ovh.litapp.keepscore.helper.DialogHelper;
import ovh.litapp.keepscore.helper.PlayerColor;
import ovh.litapp.keepscore.helper.PreferenceHelper;
import ovh.litapp.keepscore.util.Callback;
import ovh.litapp.keepscore.util.StringUtil;
import ovh.litapp.keepscore.widget.dragndrop.DragSortListView;

public class OrganizePlayersActivity extends AppCompatListActivity implements OnClickListener {

    public static final int MAX_NUM_PLAYERS = 30;
    public static final int MIN_NUM_PLAYERS = 2;

    public static final String EXTRA_PLAYER_SCORES = "playerScores";

    private Button okButton, cancelButton;
    
    private EditablePlayerAdapter adapter;
    private Game game;

    private List<String> deletedPlayersToWarnAbout = new ArrayList<String>();
    
    private boolean menuButtonSortsDescending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        game = getIntent().getParcelableExtra(GameActivity.EXTRA_GAME);
        
        setContentView(R.layout.organize_players_dialog);
        
        setUpWidgets();
        
        setUpActionBar();
    }
    
    private void setUpActionBar() {
        // home button goes back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setUpWidgets() {
        
        okButton = (Button) findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
        cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        
        adapter = new EditablePlayerAdapter(this, game.getPlayerScores());

        setListAdapter(adapter);
        
        adapter.setOnChangeListener(new Runnable() {

            @Override
            public void run() {
                // update the Add Player button if necessary
                invalidateOptionsMenu();
            }
        });

        adapter.setOnDeleteListener(new Callback<PlayerScore>() {

            @Override
            public void onCallback(PlayerScore playerScore) {
                // warn about deleted player scores if they actually have a
                // history,
                // i.e. there's something the user might regret deleting
                if (playerScore.getHistory() != null && playerScore.getHistory().size() > 0) {
                    deletedPlayersToWarnAbout.add(playerScore.toDisplayName(OrganizePlayersActivity.this).toString());
                }
            }
        });

        ((DragSortListView) getListView()).setDropListener(adapter);

        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.organize_players_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem addPlayerItem = menu.findItem(R.id.menu_add_player);
        boolean enabled = adapter.getCount() < MAX_NUM_PLAYERS;
        addPlayerItem.setEnabled(enabled);
        addPlayerItem.setVisible(enabled);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // go back on pressing home in the action bar
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else if (itemId == R.id.menu_add_player) {
            showAddPlayerDialog();
            return true;
        } else if (itemId == R.id.menu_randomize) {
            randomizePlayers();
            return true;
        } else if (itemId == R.id.menu_sort_players) {
            sortPlayers();
            return true;
        }
        return false;
    }

    private void onOkButtonClicked() {
        if (deletedPlayersToWarnAbout.isEmpty()) {
            setDataResultAndExit();
        } else {
            // warn the player just in case they don't want to delete these
            // players
            String deletePlayerText = getResources().getQuantityString(
                    R.plurals.text_confirm_delete_player, deletedPlayersToWarnAbout.size());
            new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.title_confirm_delete)
                    .setMessage(String.format(deletePlayerText, TextUtils.join(", ", deletedPlayersToWarnAbout)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setDataResultAndExit();
                        }
                    }).show();

        }

    }

    private void setDataResultAndExit() {

        Intent data = new Intent();
        data.putParcelableArrayListExtra(EXTRA_PLAYER_SCORES, new ArrayList<PlayerScore>(adapter.getItems()));
        setResult(RESULT_OK, data);
        finish();
    }

    private void showAddPlayerDialog() {

        DialogHelper.showPlayerNameDialog(this, R.string.title_add_player, "", adapter.getCount(),
                new Callback<String>() {

                    @Override
                    public void onCallback(String input) {
                        addNewPlayer(input.trim());
                    }
                });
    }

    private void addNewPlayer(CharSequence name) {
        PlayerScore playerScore = new PlayerScore();

        playerScore.setId(-1);
        playerScore.setName(StringUtil.nullToEmpty(name));
        playerScore.setPlayerNumber(adapter.getCount());
        playerScore.setScore(PreferenceHelper.getIntPreference(R.string.CONSTANT_pref_initial_score,
                R.string.CONSTANT_pref_initial_score_default, this));
        playerScore.setHistory(new ArrayList<Delta>());
        playerScore.setPlayerColor(PlayerColor.BUILT_INS[playerScore.getPlayerNumber() % PlayerColor.BUILT_INS.length]);

        adapter.add(playerScore);
        adapter.notifyDataSetChanged();
    }

    
    private void sortPlayers() {
        
        // alternate between ascending and descending order
        Comparator<PlayerScore> comparator = menuButtonSortsDescending 
            ? Collections.reverseOrder(PlayerScore.sortByScore())
            : PlayerScore.sortByScore();
        
        adapter.sortAndRefreshView(comparator);
        
        menuButtonSortsDescending = !menuButtonSortsDescending;
    }
    
    private void randomizePlayers() {
        adapter.shuffleAndRefreshView();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ok) {
            onOkButtonClicked();
        } else if (id == R.id.button_cancel) {
            setResult(RESULT_CANCELED);
            finish();
        }
        
    }

}
