package ovh.litapp.keepscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ovh.litapp.keepscore.ui.settings.SettingsScreen
import ovh.litapp.keepscore.ui.theme.KeepScoreTheme

/**
 * Settings screen, rendered with Jetpack Compose.
 *
 * Replaces the deprecated PreferenceActivity that inflated res/xml/settings.xml; the preferences it
 * reads and writes are unchanged, so the rest of the app is unaffected.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollToConfigurations =
            intent?.getBooleanExtra(EXTRA_SCROLL_TO_CONFIGURATIONS, false) == true

        setContent {
            KeepScoreTheme {
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

    companion object {
        const val EXTRA_SCROLL_TO_CONFIGURATIONS = "scrollToConfigurations"
    }
}
