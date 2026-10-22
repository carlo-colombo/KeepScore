package ovh.litapp.keepscore.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.TextUtils
import ovh.litapp.keepscore.util.StringUtil

/**
 * Helper for switching between different groups of settings. Useful for users who want different
 * increment amounts for scrabble, carcassonne, etc.
 *
 * @author nolan
 */
object SettingSetHelper {

    // reserved settings for figuring out what the current settings are
    const val META_SETTINGS = "meta_keepscore_settings"

    private const val AVAILABLE_SETTINGS_SET = "available"

    @JvmStatic
    fun isValidSettingSetName(name: String?): Boolean =
        !StringUtil.isEmptyOrWhitespace(name) &&
            name!!.length <= 50 && // come on, dude - too long
            !name.contains("/") && // file path separator
            !name.contains(",") // used for comma-separated list

    /**
     * Save the current setting set, overwriting anything with the previous name.
     */
    @JvmStatic
    fun saveCurrentSettingSet(context: Context, settingSetName: String) {
        copySettings(defaultPreferences(context), settingSetPreferences(context, settingSetName))

        // save a new 'available' settings set to the meta settings
        val availableSet = getAvailableSettingSets(context)
        availableSet.add(settingSetName)
        metaPreferences(context).edit()
            .putString(AVAILABLE_SETTINGS_SET, TextUtils.join(",", availableSet)) // comma-separated
            .apply()
    }

    /**
     * Load a saved setting set into the current settings.
     */
    @JvmStatic
    fun loadSettingSet(context: Context, settingSetName: String) {
        copySettings(settingSetPreferences(context, settingSetName), defaultPreferences(context))
    }

    @JvmStatic
    fun getAvailableSettingSets(context: Context): MutableSet<String> {
        val available = metaPreferences(context).getString(AVAILABLE_SETTINGS_SET, "")
        return StringUtil.split(available.orEmpty(), ',').toMutableSet()
    }

    @JvmStatic
    fun getSettingsSet(context: Context, settingSetName: String): Map<String, *> =
        settingSetPreferences(context, settingSetName).all

    @JvmStatic
    fun delete(context: Context, settingSetName: String) {
        val availableSets = getAvailableSettingSets(context)
        availableSets.remove(settingSetName)

        metaPreferences(context).edit()
            .putString(AVAILABLE_SETTINGS_SET, TextUtils.join(",", availableSets))
            .apply()

        // the settings set is backed by its own preferences file - remove it too, otherwise it
        // lingers on disk after being dropped from the available list
        context.deleteSharedPreferences(settingSetName)
    }

    private fun copySettings(source: SharedPreferences, target: SharedPreferences) {
        val editor = target.edit()
        editor.clear()
        for ((key, value) in source.all) {
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.apply()
    }

    private fun metaPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(META_SETTINGS, Context.MODE_PRIVATE)

    private fun defaultPreferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun settingSetPreferences(context: Context, settingsName: String): SharedPreferences =
        context.getSharedPreferences(settingsName, Context.MODE_PRIVATE)
}
