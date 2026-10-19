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

import ovh.litapp.keepscore.R;
import ovh.litapp.keepscore.ui.settings.SettingsController;

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
    public void isValidSettingSetNameRejectsNamesThatBreakTheAvailableListOrFilePath() {
        assertTrue(SettingSetHelper.isValidSettingSetName("ordinary game name"));
        assertFalse(SettingSetHelper.isValidSettingSetName("game,with,commas"));
        assertFalse(SettingSetHelper.isValidSettingSetName("game/name"));
        assertFalse(SettingSetHelper.isValidSettingSetName(""));
        assertFalse(SettingSetHelper.isValidSettingSetName("x".repeat(51)));
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
    public void deletingOneSettingSetLeavesAnotherListedAndLoadable() {
        String firstName = "settings_first";
        String secondName = "settings_second";
        String preferenceKey = "setting_set_test_value";
        SharedPreferences defaults = PreferenceManager.getDefaultSharedPreferences(context);
        SettingSetHelper.delete(context, firstName);
        SettingSetHelper.delete(context, secondName);
        File firstPreferencesFile = new File(
                context.getDataDir(), "shared_prefs/" + firstName + ".xml");

        try {
            defaults.edit().putString(preferenceKey, "first").commit();
            SettingSetHelper.saveCurrentSettingSet(context, firstName);
            defaults.edit().putString(preferenceKey, "second").commit();
            SettingSetHelper.saveCurrentSettingSet(context, secondName);

            assertTrue(firstPreferencesFile.exists());
            assertTrue(SettingSetHelper.getAvailableSettingSets(context).contains(firstName));
            assertTrue(SettingSetHelper.getAvailableSettingSets(context).contains(secondName));

            SettingSetHelper.delete(context, firstName);

            assertFalse(firstPreferencesFile.exists());
            assertFalse(SettingSetHelper.getAvailableSettingSets(context).contains(firstName));
            assertTrue(SettingSetHelper.getAvailableSettingSets(context).contains(secondName));
            assertEquals("second", SettingSetHelper.getSettingsSet(context, secondName).get(preferenceKey));

            defaults.edit().putString(preferenceKey, "changed").commit();
            SettingSetHelper.loadSettingSet(context, secondName);
            assertEquals("second", defaults.getString(preferenceKey, null));
        } finally {
            SettingSetHelper.delete(context, firstName);
            SettingSetHelper.delete(context, secondName);
            defaults.edit().remove(preferenceKey).commit();
        }
    }

    @Test
    public void savingOverAnExistingNameUpdatesValuesWithoutAddingAnotherAvailableEntry() {
        String settingSetName = "settings_overwrite";
        String preferenceKey = "setting_set_test_value";
        SharedPreferences defaults = PreferenceManager.getDefaultSharedPreferences(context);
        SettingSetHelper.delete(context, settingSetName);
        SharedPreferences meta = context.getSharedPreferences(
                SettingSetHelper.META_SETTINGS, Context.MODE_PRIVATE);

        try {
            defaults.edit().putString(preferenceKey, "first value").commit();
            SettingSetHelper.saveCurrentSettingSet(context, settingSetName);
            defaults.edit().putString(preferenceKey, "updated value").commit();
            SettingSetHelper.saveCurrentSettingSet(context, settingSetName);

            assertEquals("updated value",
                    SettingSetHelper.getSettingsSet(context, settingSetName).get(preferenceKey));
            assertTrue(SettingSetHelper.getAvailableSettingSets(context).contains(settingSetName));

            int matchingEntries = 0;
            for (String availableName : meta.getString("available", "").split(",", -1)) {
                if (settingSetName.equals(availableName)) {
                    matchingEntries++;
                }
            }
            assertEquals("saving the same name must not duplicate its list entry", 1, matchingEntries);
        } finally {
            SettingSetHelper.delete(context, settingSetName);
            defaults.edit().remove(preferenceKey).commit();
        }
    }

    @Test
    public void loadingSettingSetThroughSettingsControllerInvalidatesPreferenceHelperCache() {
        String settingSetName = "settings_plus_button_alternate";
        String plusButtonKey = context.getString(R.string.CONSTANT_pref_plus_button);
        SharedPreferences defaults = PreferenceManager.getDefaultSharedPreferences(context);
        String originalPlusButton = defaults.getString(plusButtonKey, null);
        SettingSetHelper.delete(context, settingSetName);

        try {
            defaults.edit().putString(plusButtonKey, "2").commit();
            PreferenceHelper.resetCache();
            assertEquals(2, PreferenceHelper.getPlusButtonValue(context));

            defaults.edit().putString(plusButtonKey, "5").commit();
            SettingSetHelper.saveCurrentSettingSet(context, settingSetName);
            defaults.edit().putString(plusButtonKey, "3").commit();

            new SettingsController(context).loadSettingSet(settingSetName);

            assertEquals(5, PreferenceHelper.getPlusButtonValue(context));
        } finally {
            SettingSetHelper.delete(context, settingSetName);
            if (originalPlusButton == null) {
                defaults.edit().remove(plusButtonKey).commit();
            } else {
                defaults.edit().putString(plusButtonKey, originalPlusButton).commit();
            }
            PreferenceHelper.resetCache();
        }
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
