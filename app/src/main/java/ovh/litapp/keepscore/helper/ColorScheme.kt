package ovh.litapp.keepscore.helper

import android.content.Context
import android.graphics.Typeface
import ovh.litapp.keepscore.R

/**
 * Defines a color scheme that can be chosen by the user from the Settings.
 *
 * @author nolan
 */
enum class ColorScheme(
    val backgroundColorResId: Int,
    val foregroundColorResId: Int,
    val positiveColorResId: Int,
    val greenPositiveColorResId: Int,
    val negativeColorResId: Int,
    val buttonBackgroundDrawableResId: Int,
    val dividerColorResId: Int,
    val borderDrawableResId: Int,
    val highlightedPlayerNameBackgroundDrawableResId: Int,
    val highlightedPlayerNameTextColorResId: Int,
    val playerNameTypeface: Typeface,
) {
    Light(
        R.color.card_background,
        R.color.card_text_color,
        R.color.blue_1,
        R.color.green,
        R.color.red,
        R.drawable.btn_default_holo_light,
        R.color.card_divider,
        R.drawable.card_shape,
        R.drawable.bubbly_background_1,
        android.R.color.primary_text_dark_nodisable,
        Typeface.DEFAULT,
    ),
    Dark(
        android.R.color.background_dark,
        android.R.color.secondary_text_dark_nodisable,
        R.color.blue_4,
        R.color.green_4,
        R.color.red_4,
        R.drawable.btn_default_transparent,
        R.color.light_blue_3,
        R.drawable.blue_border_shape_no_gradient,
        R.drawable.bubbly_background_1,
        android.R.color.primary_text_dark_nodisable,
        Typeface.DEFAULT_BOLD,
    ),
    Android(
        R.color.android_color,
        android.R.color.primary_text_light_nodisable,
        R.color.blue_5,
        R.color.green_5,
        R.color.red,
        android.R.drawable.btn_default,
        R.color.light_blue_2,
        R.drawable.blue_border_shape_no_gradient,
        R.drawable.bubbly_background_1,
        android.R.color.primary_text_dark_nodisable,
        Typeface.DEFAULT_BOLD,
    ),
    ClassicLight(
        android.R.color.background_light,
        android.R.color.primary_text_light_nodisable,
        R.color.blue_1,
        R.color.green,
        R.color.red,
        android.R.drawable.btn_default,
        R.color.light_blue_2,
        R.drawable.blue_border_shape_with_gradient,
        R.drawable.bubbly_background_1,
        android.R.color.primary_text_dark_nodisable,
        Typeface.DEFAULT_BOLD,
    ),
    ;

    companion object {

        @JvmStatic
        fun findByPreference(preference: String, context: Context): ColorScheme =
            when (preference) {
                context.getString(R.string.CONSTANT_pref_color_scheme_choice_light) -> Light
                context.getString(R.string.CONSTANT_pref_color_scheme_choice_dark) -> Dark
                context.getString(R.string.CONSTANT_pref_color_scheme_choice_android) -> Android
                else -> ClassicLight
            }
    }
}
