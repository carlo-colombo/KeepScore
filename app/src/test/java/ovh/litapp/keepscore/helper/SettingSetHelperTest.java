package ovh.litapp.keepscore.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingSetHelperTest {

    private static final String SETTING_SET_NAME = "scrabble";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PreferenceHelper.resetCache();
    }

    @Test
    public void deleteRemovesSettingSetFromAvailableSetsAndDeletesItsPreferencesFile() {
        // saving a setting set creates the entry in the meta preferences and the
        // backing preferences file named after the setting set
        SettingSetHelper.saveCurrentSettingSet(context, SETTING_SET_NAME);
        assertTrue(SettingSetHelper.getAvailableSettingSets(context).contains(SETTING_SET_NAME));

        SharedPreferences settingSetPrefs = context.getSharedPreferences(SETTING_SET_NAME, Context.MODE_PRIVATE);
        settingSetPrefs.edit().putString("commit_pref_button_1", "5").commit();
        File preferencesFile = new File(context.getDataDir(), "shared_prefs/" + SETTING_SET_NAME + ".xml");
        assertTrue(preferencesFile.exists());

        SettingSetHelper.delete(context, SETTING_SET_NAME);

        assertFalse(SettingSetHelper.getAvailableSettingSets(context).contains(SETTING_SET_NAME));
        assertFalse("backing preferences file should be deleted", preferencesFile.exists());
        assertTrue(context.getSharedPreferences(SETTING_SET_NAME, Context.MODE_PRIVATE).getAll().isEmpty());
    }

    @Test
    public void savedSettingSetIsReadableRightAfterTheApply() {
        // the settings copy uses apply(), which must still update the in-memory
        // value synchronously, so a read from the same process sees it immediately
        SharedPreferences defaults = PreferenceManager.getDefaultSharedPreferences(context);
        defaults.edit().putString("pref_key", "value").apply();

        SettingSetHelper.saveCurrentSettingSet(context, SETTING_SET_NAME);
        assertEquals("value", SettingSetHelper.getSettingsSet(context, SETTING_SET_NAME).get("pref_key"));

        defaults.edit().putString("pref_key", "changed").apply();
        SettingSetHelper.loadSettingSet(context, SETTING_SET_NAME);
        assertEquals("value", defaults.getString("pref_key", null));
    }

    @Test
    public void settingSetsPreserveTheLegacyOrientationPreferenceKey() {
        SharedPreferences defaults = PreferenceManager.getDefaultSharedPreferences(context);
        String originalOrientation = defaults.getString("orientation", null);
        defaults.edit().putString("orientation", "portrait").commit();

        try {
            SettingSetHelper.saveCurrentSettingSet(context, SETTING_SET_NAME);
            assertEquals("portrait", SettingSetHelper.getSettingsSet(context, SETTING_SET_NAME).get("orientation"));

            defaults.edit().putString("orientation", "landscape").commit();
            SettingSetHelper.loadSettingSet(context, SETTING_SET_NAME);
            assertEquals("portrait", defaults.getString("orientation", null));
        } finally {
            SettingSetHelper.delete(context, SETTING_SET_NAME);
            if (originalOrientation == null) {
                defaults.edit().remove("orientation").commit();
            } else {
                defaults.edit().putString("orientation", originalOrientation).commit();
            }
            PreferenceHelper.resetCache();
        }
    }
}
