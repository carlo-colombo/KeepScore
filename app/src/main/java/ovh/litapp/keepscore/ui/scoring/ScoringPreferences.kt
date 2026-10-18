package ovh.litapp.keepscore.ui.scoring

import android.content.Context
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PreferenceHelper

/**
 * The preferences the scoreboard needs for scoring itself: how long a tap keeps batching into the
 * same history entry, and the score a rematch starts from.
 *
 * The screen reads the rest of the preferences it draws with (colours, round totals, button values)
 * straight from [PreferenceHelper] while it composes.
 */
interface ScoringPreferences {

    /** How many milliseconds a + or - tap keeps batching into the same history entry. */
    val updateDelayMs: Int

    /** The score every player starts a rematch from. */
    val initialScore: Int

    companion object {
        /** The preferences the user picked in the settings. */
        @JvmStatic
        fun from(context: Context): ScoringPreferences = PreferenceHelperScoringPreferences(context)
    }
}

/** Reads [ScoringPreferences] from the settings, the way the legacy scoreboard did. */
private class PreferenceHelperScoringPreferences(private val context: Context) : ScoringPreferences {

    override val updateDelayMs: Int
        get() = PreferenceHelper.getUpdateDelay(context) * 1000

    override val initialScore: Int
        get() = PreferenceHelper.getIntPreference(
            R.string.CONSTANT_pref_initial_score,
            R.string.CONSTANT_pref_initial_score_default,
            context,
        )
}
