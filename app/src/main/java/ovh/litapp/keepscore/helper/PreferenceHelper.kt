package ovh.litapp.keepscore.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import ovh.litapp.keepscore.R

/**
 * The app's settings, read through [PreferenceManager.getDefaultSharedPreferences].
 *
 * The frequently read values are cached because the scoreboard reads them for every player; call
 * [resetCache] when the user changes a setting.
 */
object PreferenceHelper {

    // cache some of the frequently-accessed preferences for performance
    private var updateDelay = -1
    private var colorScheme: ColorScheme? = null
    private var greenText: Boolean? = null
    private var showRoundTotals: Boolean? = null
    private var showColors: Boolean? = null
    private var plusButton: Int? = null
    private var minusButton: Int? = null
    private var popupButtons: IntArray? = null
    private var twoPlayerOnscreenButtons: IntArray? = null

    @JvmStatic
    fun resetCache() {
        updateDelay = -1
        colorScheme = null
        greenText = null
        showRoundTotals = null
        showColors = null
        plusButton = null
        minusButton = null
        popupButtons = null
        twoPlayerOnscreenButtons = null
    }

    @JvmStatic
    fun getShowColors(context: Context): Boolean {
        var value = showColors
        if (value == null) {
            value = getBooleanPreference(
                R.string.CONSTANT_pref_show_colors,
                R.string.CONSTANT_pref_show_colors_default,
                context,
            )
            showColors = value
        }
        return value
    }

    @JvmStatic
    fun getPlusButtonValue(context: Context): Int {
        var value = plusButton
        if (value == null) {
            value = getIntPreference(
                R.string.CONSTANT_pref_plus_button,
                R.string.CONSTANT_pref_plus_button_default,
                context,
            )
            plusButton = value
        }
        return value
    }

    @JvmStatic
    fun getMinusButtonValue(context: Context): Int {
        var value = minusButton
        if (value == null) {
            value = getIntPreference(
                R.string.CONSTANT_pref_minus_button,
                R.string.CONSTANT_pref_minus_button_default,
                context,
            )
            minusButton = value
        }
        return value
    }

    @JvmStatic
    fun getPopupDeltaButtonValue(index: Int, context: Context): Int {
        var buttons = popupButtons
        if (buttons == null) {
            buttons = intArrayOf(
                getIntPreference(
                    R.string.CONSTANT_pref_button_1,
                    R.string.CONSTANT_pref_button_1_default,
                    context,
                ),
                getIntPreference(
                    R.string.CONSTANT_pref_button_2,
                    R.string.CONSTANT_pref_button_2_default,
                    context,
                ),
                getIntPreference(
                    R.string.CONSTANT_pref_button_3,
                    R.string.CONSTANT_pref_button_3_default,
                    context,
                ),
                getIntPreference(
                    R.string.CONSTANT_pref_button_4,
                    R.string.CONSTANT_pref_button_4_default,
                    context,
                ),
            )
            popupButtons = buttons
        }
        return buttons[index]
    }

    @JvmStatic
    fun getTwoPlayerDeltaButtonValue(index: Int, context: Context): Int {
        var buttons = twoPlayerOnscreenButtons
        if (buttons == null) {
            buttons = intArrayOf(
                getIntPreference(
                    R.string.CONSTANT_pref_2p_button_1,
                    R.string.CONSTANT_pref_2p_button_1_default,
                    context,
                ),
                getIntPreference(
                    R.string.CONSTANT_pref_2p_button_2,
                    R.string.CONSTANT_pref_2p_button_2_default,
                    context,
                ),
                getIntPreference(
                    R.string.CONSTANT_pref_2p_button_3,
                    R.string.CONSTANT_pref_2p_button_3_default,
                    context,
                ),
                getIntPreference(
                    R.string.CONSTANT_pref_2p_button_4,
                    R.string.CONSTANT_pref_2p_button_4_default,
                    context,
                ),
            )
            twoPlayerOnscreenButtons = buttons
        }
        return buttons[index]
    }

    @JvmStatic
    fun getShowRoundTotals(context: Context): Boolean {
        var value = showRoundTotals
        if (value == null) {
            value = getBooleanPreference(
                R.string.CONSTANT_pref_show_round_totals,
                R.string.CONSTANT_pref_show_round_totals_default,
                context,
            )
            showRoundTotals = value
        }
        return value
    }

    @JvmStatic
    fun getColorScheme(context: Context): ColorScheme {
        var value = colorScheme
        if (value == null) {
            val pref = getStringPreference(
                R.string.CONSTANT_pref_color_scheme,
                R.string.CONSTANT_pref_color_scheme_default,
                context,
            )
            value = ColorScheme.findByPreference(pref, context)
            colorScheme = value
        }
        return value
    }

    @JvmStatic
    fun getUpdateDelay(context: Context): Int {
        if (updateDelay == -1) {
            updateDelay = getIntPreference(
                R.string.CONSTANT_pref_update_delay,
                R.string.CONSTANT_pref_update_delay_default,
                context,
            )
        }
        return updateDelay
    }

    @JvmStatic
    fun getGreenTextPreference(context: Context): Boolean {
        var value = greenText
        if (value == null) {
            value = getBooleanPreference(
                R.string.CONSTANT_pref_green_text,
                R.string.CONSTANT_pref_green_text_default,
                context,
            )
            greenText = value
        }
        return value
    }

    @JvmStatic
    fun setBooleanPreference(resId: Int, value: Boolean, context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().putBoolean(context.getString(resId), value).apply()
    }

    @JvmStatic
    fun setBooleanPreference(resId: Int, valueResId: Int, context: Context) {
        setBooleanPreference(resId, java.lang.Boolean.parseBoolean(context.getString(valueResId)), context)
    }

    @JvmStatic
    fun setStringPreference(resId: Int, valueResId: Int, context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().putString(context.getString(resId), context.getString(valueResId)).apply()
    }

    @JvmStatic
    fun setIntPreference(resId: Int, valueResId: Int, context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().putString(context.getString(resId), context.getString(valueResId)).apply()
    }

    @JvmStatic
    fun getIntPreference(resId: Int, defaultValueResId: Int, context: Context): Int {
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultValue = context.getString(defaultValueResId)
        return sharedPrefs.getString(context.getString(resId), defaultValue)!!.toInt()
    }

    @JvmStatic
    fun getBooleanPreference(resId: Int, defaultValueResId: Int, context: Context): Boolean {
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultValue = context.getString(defaultValueResId).toBoolean()
        return sharedPrefs.getBoolean(context.getString(resId), defaultValue)
    }

    @JvmStatic
    fun getStringPreference(resId: Int, defaultValueResId: Int, context: Context): String {
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultValue = context.getString(defaultValueResId)
        return sharedPrefs.getString(context.getString(resId), defaultValue)!!
    }
}
