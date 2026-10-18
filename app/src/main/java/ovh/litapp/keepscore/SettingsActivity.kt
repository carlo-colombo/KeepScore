package ovh.litapp.keepscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import ovh.litapp.keepscore.helper.Orientation
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.ui.settings.SettingsScreen

/**
 * Settings screen, rendered with Jetpack Compose.
 *
 * Replaces the deprecated PreferenceActivity that inflated res/xml/settings.xml; the preferences it
 * reads and writes are unchanged, so the rest of the app is unaffected.
 */
class SettingsActivity : ComponentActivity() {

    private var startingOrientation: Orientation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preserve the baseline across recreate() (used by load/reset); otherwise the
        // recreated activity would compare the new orientation against itself.
        startingOrientation = intent?.getSerializableExtra(EXTRA_STARTING_ORIENTATION) as? Orientation
            ?: PreferenceHelper.getOrientation(this).also {
                intent?.putExtra(EXTRA_STARTING_ORIENTATION, it)
            }

        // The hardware/gesture back button tells the caller whether the preferred orientation
        // changed, so the game screens can restart in the new orientation. The toolbar's up arrow
        // only finishes, matching the old ActionBar home behaviour.
        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_OK, orientationChangedResult())
            finish()
        }

        val scrollToConfigurations =
            intent?.getBooleanExtra(EXTRA_SCROLL_TO_CONFIGURATIONS, false) == true

        setContent {
            MaterialTheme {
                SettingsScreen(
                    scrollToConfigurations = scrollToConfigurations,
                    onNavigateToAbout = {
                        startActivity(Intent(this, AboutActivity::class.java))
                    },
                    onReload = ::recreate,
                    onBack = ::finish,
                )
            }
        }
    }

    private fun orientationChangedResult(): Intent = Intent().putExtra(
        EXTRA_ORIENTATION_CHANGED,
        startingOrientation != PreferenceHelper.getOrientation(this),
    )

    companion object {
        const val EXTRA_SCROLL_TO_CONFIGURATIONS = "scrollToConfigurations"
        const val EXTRA_ORIENTATION_CHANGED = "orientationChanged"
        const val EXTRA_STARTING_ORIENTATION = "startingOrientation"
    }
}
