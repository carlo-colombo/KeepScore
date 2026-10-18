package ovh.litapp.keepscore;

import ovh.litapp.keepscore.android.EdgeToEdge;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBar.Tab;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.PlayerScore;
import ovh.litapp.keepscore.fragment.HistoryPlayerTableFragment;
import ovh.litapp.keepscore.fragment.HistoryRoundChartFragment;
import ovh.litapp.keepscore.fragment.HistoryRoundTableFragment;
import ovh.litapp.keepscore.fragment.HistoryTimelineFragment;
import ovh.litapp.keepscore.util.UtilLogger;

/**
 * Activity for displaying the entire history of a game
 * 
 * @author nolan
 * 
 */
public class HistoryActivity extends AppCompatActivity implements ActionBar.TabListener, OnPageChangeListener {

    private static final UtilLogger log = new UtilLogger(HistoryActivity.class);

    // Public service announcement: You just lost the
    public static final String EXTRA_GAME = "game";
    private static final int MAX_PLAYERS_FOR_ROUND_TABLE = 8;
    
    private AppSectionsPagerAdapter appSectionsPagerAdapter;
    private ViewPager viewPager;
    private ActionBar actionBar;
    
    private Game game;
    private boolean showTimeline;
    private boolean showRoundTable;
    
    private static enum TabDef {
        
        ChartByTime(R.string.button_history_time_chart),
        ChartByRound(R.string.button_history_round_chart),
        TableByRound(R.string.button_history_round_table),
        TableByPlayer(R.string.button_history_player_table);
        
        private int titleResId;
        
        private TabDef(int titleResId) {
            this.titleResId = titleResId;
        }

        public int getTitleResId() {
            return titleResId;
        }
    }
    
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.history);
        EdgeToEdge.apply(this);
        
        game = getIntent().getParcelableExtra(EXTRA_GAME);
        showTimeline = determineIfShouldShowTimeline();
        showRoundTable = determineIfShouldShowRoundTable();

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        appSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        actionBar = getSupportActionBar();
        
        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.addOnPageChangeListener(this);
        
        // home button goes back
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        for (int i = 0; i < TabDef.values().length; i++) {
            TabDef tab = TabDef.values()[i];
            if (!showTimeline && tab == TabDef.ChartByTime) {
                continue;
            } else if (!showRoundTable && tab == TabDef.TableByRound) {
                continue;
            }
            actionBar.addTab(actionBar.newTab().setText(tab.getTitleResId()).setTag(tab).setTabListener(this));
        }

        // Set the adapter only after the tabs exist: the adapter's item count is
        // actionBar.getTabCount(), and ViewPager throws if that count changes
        // without notifyDataSetChanged.
        viewPager.setAdapter(appSectionsPagerAdapter);

        log.d("intent is %s", getIntent());
        log.d("game is %s", game);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // go back on pressing home in the action bar
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }
    
    private boolean determineIfShouldShowRoundTable() {
        // this table looks scrunched up if there are too many players.  It's not useful.
        return game.getPlayerScores().size() <= MAX_PLAYERS_FOR_ROUND_TABLE;
    }

    private boolean determineIfShouldShowTimeline() {
        
        // on older versions of keepscore, we didn't log the timestamps of deltas.  So return true
        // if any deltas have a timestamp on them
        for (PlayerScore playerScore : game.getPlayerScores()) {
            for (Delta delta : playerScore.getHistory()) {
                if (delta.getTimestamp() > 0L) {
                    return true;
                }
            }
        }
        return false;
        
        
    }

    private class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            
            TabDef tabDef = (TabDef)actionBar.getTabAt(i).getTag();
            
            Fragment fragment = createFragment(tabDef);
            Bundle args = new Bundle();
            args.putParcelable(GameActivity.EXTRA_GAME, game);
            fragment.setArguments(args);
            return fragment;
        }
        
        private Fragment createFragment(TabDef tabDef) {
            switch (tabDef){
                case ChartByTime:
                    return new HistoryTimelineFragment();
                case ChartByRound:
                    return new HistoryRoundChartFragment();
                case TableByRound:
                    return new HistoryRoundTableFragment();
                case TableByPlayer:
                default:
                    return new HistoryPlayerTableFragment();
            }            
        }

        @Override
        public int getCount() {
            return actionBar.getTabCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return actionBar.getTabAt(position).getText();
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPageSelected(int position) {
        // When swiping between different app sections, select the corresponding tab.
        actionBar.setSelectedNavigationItem(position);
        

        /*
         * 
         * When paging between tabs, normally the dropdown list (spinnerAdapter) won't change properly.  This
         * occurs on small devices in landscape mode, i.e. when the tab labels are reduced to a dropdown to save space.
         * 
         * Thanks to this StackOverflow discussion for figuring out how to solve this problem:
         * http://stackoverflow.com/questions/15409330/action-bar-selecttab-and-setselectednavigationitem-not-working
         */
        
        try {
            //now use reflection to select the correct Spinner if
            // the bar's tabs have been reduced to a Spinner
            Activity activity = HistoryActivity.this;
            int actionBarResId = getResources().getIdentifier("action_bar", "id", "android");
            if (actionBarResId == 0) { // not found, use the AppCompat action bar
                actionBarResId = androidx.appcompat.R.id.action_bar;
            }
            View action_bar_view = activity.findViewById(actionBarResId);
            Class<?> action_bar_class = action_bar_view.getClass();
            Field tab_scroll_view_prop = action_bar_class.getDeclaredField("mTabScrollView");
            tab_scroll_view_prop.setAccessible(true);
            //get the value of mTabScrollView in our action bar
            Object tab_scroll_view = tab_scroll_view_prop.get(action_bar_view);
            if (tab_scroll_view == null) return;
            Field spinner_prop = tab_scroll_view.getClass().getDeclaredField("mTabSpinner");
            spinner_prop.setAccessible(true);
            //get the value of mTabSpinner in our scroll view
            Object tab_spinner = spinner_prop.get(tab_scroll_view);
            if (tab_spinner == null) return;
            Method set_selection_method = tab_spinner.getClass().getSuperclass().getDeclaredMethod("setSelection", Integer.TYPE, Boolean.TYPE);
            set_selection_method.invoke(tab_spinner, position, true);
        
        } catch (NoSuchFieldException ignore) {
            log.d(ignore, "got exception");
        } catch (InvocationTargetException ignore) {
            log.d(ignore, "got exception");
        } catch (IllegalAccessException ignore) {
            log.d(ignore, "got exception");
        } catch (NoSuchMethodException ignore) {
            log.d(ignore, "got exception");
        }
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
        invalidateOptionsMenu();
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }
     
}
