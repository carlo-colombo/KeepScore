package ovh.litapp.keepscore.ui.settings

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PackageHelper
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.helper.SettingSetHelper

/**
 * How an integer preference validates the text the user types. Mirrors the three
 * OnPreferenceChangeListeners that the legacy SettingsActivity registered.
 */
internal enum class IntegerValidation { NON_ZERO, ANY_INTEGER, UPDATE_DELAY }

/** A preference that stores an integer as a String, exactly like the legacy EditTextPreference. */
internal class IntegerPreference(
    val key: String,
    val titleRes: Int,
    val defaultValue: String,
    val allowNegative: Boolean,
    val validation: IntegerValidation,
    val summaryRes: Int?,
)

/** A preference that stores a Boolean, exactly like the legacy CheckBoxPreference. */
internal class TogglePreference(
    val key: String,
    val titleRes: Int,
    val summaryRes: Int?,
    val defaultValue: Boolean,
)

/** A preference that stores one of a fixed set of Strings, exactly like the legacy ListPreference. */
internal class ChoicePreference(
    val key: String,
    val titleRes: Int,
    val entriesRes: Int,
    val entryValuesRes: Int,
    val defaultValue: String,
)

/** One row of the settings screen, in the order declared by res/xml/settings.xml. */
internal sealed interface SettingsRow {
    data class Category(val titleRes: Int) : SettingsRow
    data class Toggle(val spec: TogglePreference) : SettingsRow
    data class Integer(val spec: IntegerPreference) : SettingsRow
    data class Choice(val spec: ChoicePreference) : SettingsRow
    data class Action(
        val action: SettingsAction,
        val key: String,
        val titleRes: Int,
        val summaryRes: Int? = null,
        val summaryText: String? = null,
    ) : SettingsRow
}

/**
 * The preference key of each row, matching the keys the legacy PreferenceScreen declared. Used as
 * the LazyColumn item key and to keep the legacy CONSTANT_pref_* keys referenced.
 */
internal val SettingsRow.stableKey: Any
    get() = when (this) {
        is SettingsRow.Category -> titleRes
        is SettingsRow.Toggle -> spec.key
        is SettingsRow.Integer -> spec.key
        is SettingsRow.Choice -> spec.key
        is SettingsRow.Action -> key
    }

internal enum class SettingsAction { RESET, SAVE_SETTINGS_SET, LOAD_SETTINGS_SET, ABOUT }

/** Which preferences the setting-set backup helpers should include, keyed by title. */
private class PersistentEntry(val key: String, val titleRes: Int)

/**
 * Reads and writes the preferences shown on the settings screen.
 *
 * The keys, defaults and value types are the same as the ones the legacy PreferenceActivity loaded
 * from res/xml/settings.xml, so anything that reads the shared preferences (GameActivity,
 * MainActivity, SettingSetHelper) keeps working unchanged.
 */
@Stable
internal class SettingsController(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

    val versionSummary: String =
        appContext.getString(R.string.text_version_number, PackageHelper.getVersionName(appContext))

    val colorScheme = ChoicePreference(
        key = string(R.string.CONSTANT_pref_color_scheme),
        titleRes = R.string.pref_color_scheme_name,
        entriesRes = R.array.pref_color_scheme_entries,
        entryValuesRes = R.array.pref_color_scheme_entry_values,
        defaultValue = string(R.string.CONSTANT_pref_color_scheme_default),
    )
    val orientation = ChoicePreference(
        key = string(R.string.CONSTANT_pref_orientation),
        titleRes = R.string.pref_orientation_name,
        entriesRes = R.array.pref_orientation_entries,
        entryValuesRes = R.array.pref_orientation_entry_values,
        defaultValue = string(R.string.CONSTANT_pref_orientation_default),
    )

    val greenText = TogglePreference(
        key = string(R.string.CONSTANT_pref_green_text),
        titleRes = R.string.pref_green_text_name,
        summaryRes = R.string.pref_green_text_summary,
        defaultValue = string(R.string.CONSTANT_pref_green_text_default).toBoolean(),
    )
    val showRoundTotals = TogglePreference(
        key = string(R.string.CONSTANT_pref_show_round_totals),
        titleRes = R.string.pref_show_round_totals_name,
        summaryRes = null,
        defaultValue = string(R.string.CONSTANT_pref_show_round_totals_default).toBoolean(),
    )
    val showColors = TogglePreference(
        key = string(R.string.CONSTANT_pref_show_colors),
        titleRes = R.string.pref_show_colors_name,
        summaryRes = null,
        defaultValue = string(R.string.CONSTANT_pref_show_colors_default).toBoolean(),
    )
    val disableHighlightTag = TogglePreference(
        key = string(R.string.CONSTANT_pref_disable_highlight_tag),
        titleRes = R.string.pref_disable_highlight_tag_name,
        summaryRes = R.string.pref_disable_highlight_tag_summary,
        defaultValue = string(R.string.CONSTANT_pref_disable_highlight_tag_default).toBoolean(),
    )
    val useWakeLock = TogglePreference(
        key = string(R.string.CONSTANT_pref_use_wake_lock),
        titleRes = R.string.pref_use_wake_lock_name,
        summaryRes = R.string.pref_use_wake_lock_summary,
        defaultValue = string(R.string.CONSTANT_pref_use_wake_lock_default).toBoolean(),
    )
    val initialMessage = TogglePreference(
        key = string(R.string.CONSTANT_pref_initial_message),
        titleRes = R.string.pref_initial_message_name,
        summaryRes = null,
        defaultValue = string(R.string.CONSTANT_pref_initial_message_default).toBoolean(),
    )

    val initialScore = integer(
        R.string.CONSTANT_pref_initial_score,
        R.string.pref_initial_score_name,
        R.string.CONSTANT_pref_initial_score_default,
        allowNegative = true,
        validation = IntegerValidation.ANY_INTEGER,
    )
    val plusButton = integer(
        R.string.CONSTANT_pref_plus_button,
        R.string.pref_plus_button_name,
        R.string.CONSTANT_pref_plus_button_default,
    )
    val minusButton = integer(
        R.string.CONSTANT_pref_minus_button,
        R.string.pref_minus_button_name,
        R.string.CONSTANT_pref_minus_button_default,
    )
    val button1 = integer(
        R.string.CONSTANT_pref_button_1,
        R.string.pref_button_1_name,
        R.string.CONSTANT_pref_button_1_default,
    )
    val button2 = integer(
        R.string.CONSTANT_pref_button_2,
        R.string.pref_button_2_name,
        R.string.CONSTANT_pref_button_2_default,
    )
    val button3 = integer(
        R.string.CONSTANT_pref_button_3,
        R.string.pref_button_3_name,
        R.string.CONSTANT_pref_button_3_default,
    )
    val button4 = integer(
        R.string.CONSTANT_pref_button_4,
        R.string.pref_button_4_name,
        R.string.CONSTANT_pref_button_4_default,
    )
    val twoPlayerButton1 = integer(
        R.string.CONSTANT_pref_2p_button_1,
        R.string.pref_2p_button_1_name,
        R.string.CONSTANT_pref_2p_button_1_default,
    )
    val twoPlayerButton2 = integer(
        R.string.CONSTANT_pref_2p_button_2,
        R.string.pref_2p_button_2_name,
        R.string.CONSTANT_pref_2p_button_2_default,
    )
    val twoPlayerButton3 = integer(
        R.string.CONSTANT_pref_2p_button_3,
        R.string.pref_2p_button_3_name,
        R.string.CONSTANT_pref_2p_button_3_default,
    )
    val twoPlayerButton4 = integer(
        R.string.CONSTANT_pref_2p_button_4,
        R.string.pref_2p_button_4_name,
        R.string.CONSTANT_pref_2p_button_4_default,
    )
    val updateDelay = integer(
        R.string.CONSTANT_pref_update_delay,
        R.string.pref_update_delay_name,
        R.string.CONSTANT_pref_update_delay_default,
        allowNegative = false,
        validation = IntegerValidation.UPDATE_DELAY,
        summaryRes = R.string.pref_update_delay_summary,
    )

    /** The rows in display order, matching the old PreferenceScreen layout. */
    val rows: List<SettingsRow> = listOf(
        SettingsRow.Category(R.string.pref_cat_appearance),
        SettingsRow.Choice(colorScheme),
        SettingsRow.Choice(orientation),
        SettingsRow.Toggle(greenText),
        SettingsRow.Toggle(showRoundTotals),
        SettingsRow.Toggle(showColors),
        SettingsRow.Toggle(disableHighlightTag),
        SettingsRow.Category(R.string.pref_cat_config),
        SettingsRow.Integer(initialScore),
        SettingsRow.Integer(plusButton),
        SettingsRow.Integer(minusButton),
        SettingsRow.Integer(button1),
        SettingsRow.Integer(button2),
        SettingsRow.Integer(button3),
        SettingsRow.Integer(button4),
        SettingsRow.Integer(twoPlayerButton1),
        SettingsRow.Integer(twoPlayerButton2),
        SettingsRow.Integer(twoPlayerButton3),
        SettingsRow.Integer(twoPlayerButton4),
        SettingsRow.Category(R.string.pref_cat_advanced),
        SettingsRow.Integer(updateDelay),
        SettingsRow.Toggle(useWakeLock),
        SettingsRow.Category(R.string.pref_cat_manage),
        SettingsRow.Action(
            SettingsAction.RESET,
            key = string(R.string.CONSTANT_pref_reset),
            titleRes = R.string.pref_reset_name,
        ),
        SettingsRow.Action(
            SettingsAction.SAVE_SETTINGS_SET,
            key = string(R.string.CONSTANT_pref_save_settings),
            titleRes = R.string.pref_save_settings_name,
            summaryRes = R.string.pref_save_settings_summary,
        ),
        SettingsRow.Action(
            SettingsAction.LOAD_SETTINGS_SET,
            key = string(R.string.CONSTANT_pref_load_settings),
            titleRes = R.string.pref_load_settings_name,
            summaryRes = R.string.pref_load_settings_summary,
        ),
        SettingsRow.Category(R.string.pref_cat_about),
        SettingsRow.Toggle(initialMessage),
        SettingsRow.Action(
            SettingsAction.ABOUT,
            key = string(R.string.CONSTANT_pref_about),
            titleRes = R.string.text_about,
            summaryText = versionSummary,
        ),
    )

    private val persistentEntries: List<PersistentEntry> = buildList {
        rows.forEach { row ->
            when (row) {
                is SettingsRow.Toggle -> add(PersistentEntry(row.spec.key, row.spec.titleRes))
                is SettingsRow.Integer -> add(PersistentEntry(row.spec.key, row.spec.titleRes))
                is SettingsRow.Choice -> add(PersistentEntry(row.spec.key, row.spec.titleRes))
                is SettingsRow.Category, is SettingsRow.Action -> Unit
            }
        }
    }

    val integerValues = mutableStateMapOf<String, String>()
    val booleanValues = mutableStateMapOf<String, Boolean>()
    val listValues = mutableStateMapOf<String, String>()

    init {
        // Cache the current values, persisting each default on first read the way
        // androidx.preference.Preference did when it inflated the XML screen.
        rows.forEach { row ->
            when (row) {
                is SettingsRow.Toggle -> booleanValues[row.spec.key] = readBoolean(row.spec)
                is SettingsRow.Integer -> integerValues[row.spec.key] = readInteger(row.spec)
                is SettingsRow.Choice -> listValues[row.spec.key] = readChoice(row.spec)
                is SettingsRow.Category, is SettingsRow.Action -> Unit
            }
        }
    }

    fun setInteger(spec: IntegerPreference, value: String) {
        prefs.edit { putString(spec.key, value) }
        integerValues[spec.key] = value
        PreferenceHelper.resetCache()
    }

    fun setBoolean(spec: TogglePreference, value: Boolean) {
        prefs.edit { putBoolean(spec.key, value) }
        booleanValues[spec.key] = value
        PreferenceHelper.resetCache()
    }

    fun setChoice(spec: ChoicePreference, value: String) {
        prefs.edit { putString(spec.key, value) }
        listValues[spec.key] = value
        PreferenceHelper.resetCache()
    }

    fun resetAll() {
        prefs.edit { clear() }
        PreferenceHelper.resetCache()
    }

    fun availableSettingSets(): List<String> =
        SettingSetHelper.getAvailableSettingSets(appContext).sorted()

    fun saveSettingSet(name: String) {
        SettingSetHelper.saveCurrentSettingSet(appContext, name)
    }

    fun loadSettingSet(name: String) {
        SettingSetHelper.loadSettingSet(appContext, name)
        PreferenceHelper.resetCache()
    }

    fun deleteSettingSet(name: String) {
        SettingSetHelper.delete(appContext, name)
    }

    /**
     * The settings contained in [settingSetName], labelled with the same user-friendly titles and
     * in the same order the legacy screen listed them.
     */
    fun settingSetContents(settingSetName: String): List<Pair<String, String>> {
        val stored = SettingSetHelper.getSettingsSet(appContext, settingSetName)
        return persistentEntries.map { entry ->
            val value = stored[entry.key] ?: false // Android stores unset booleans as absent
            appContext.getString(entry.titleRes) to value.toString()
        }
    }

    private fun integer(
        keyRes: Int,
        titleRes: Int,
        defaultRes: Int,
        allowNegative: Boolean = true,
        validation: IntegerValidation = IntegerValidation.NON_ZERO,
        summaryRes: Int? = null,
    ) = IntegerPreference(
        key = string(keyRes),
        titleRes = titleRes,
        defaultValue = string(defaultRes),
        allowNegative = allowNegative,
        validation = validation,
        summaryRes = summaryRes,
    )

    private fun string(resId: Int): String = appContext.getString(resId)

    private fun readInteger(spec: IntegerPreference): String {
        val stored = prefs.getString(spec.key, null)
        if (stored != null) return stored
        prefs.edit { putString(spec.key, spec.defaultValue) }
        return spec.defaultValue
    }

    private fun readBoolean(spec: TogglePreference): Boolean {
        if (prefs.contains(spec.key)) return prefs.getBoolean(spec.key, spec.defaultValue)
        prefs.edit { putBoolean(spec.key, spec.defaultValue) }
        return spec.defaultValue
    }

    private fun readChoice(spec: ChoicePreference): String {
        val stored = prefs.getString(spec.key, null)
        if (stored != null) return stored
        prefs.edit { putString(spec.key, spec.defaultValue) }
        return spec.defaultValue
    }
}
