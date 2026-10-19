package ovh.litapp.keepscore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ovh.litapp.keepscore.android.EdgeToEdge
import ovh.litapp.keepscore.helper.PackageHelper
import ovh.litapp.keepscore.ui.about.AboutScreen
import ovh.litapp.keepscore.ui.theme.KeepScoreTheme
import java.util.Locale

class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)

        val html = buildAboutPageHtml(
            resources = resources,
            versionName = PackageHelper.getVersionName(this),
            donateInstalled = PackageHelper.isDonateVersionInstalled(this),
        )

        setContent {
            KeepScoreTheme {
                AboutScreen(
                    title = getString(R.string.text_about),
                    pageHtml = html,
                    onOpenLink = ::openExternalLink,
                    onDone = ::finish,
                )
            }
        }

        EdgeToEdge.apply(this)
    }

    private fun openExternalLink(uri: Uri) {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/**
 * Builds the legacy localized About page content from its raw HTML resources. Those resources
 * carry translated descriptions, contributors, changelog markup and links; the screen layout itself
 * is Compose, while this content remains in a WebView so none of the translations are discarded.
 */
internal fun buildAboutPageHtml(
    resources: android.content.res.Resources,
    versionName: String,
    donateInstalled: Boolean,
): String {
    val donateSuffix = if (donateInstalled) {
        resources.getString(R.string.CONSTANT_text_donate_suffix)
    } else {
        ""
    }
    val donateMessage = if (donateInstalled) {
        "<p/>${resources.getString(R.string.text_donate_message)}"
    } else {
        ""
    }
    val textColor = String.format(
        Locale.ROOT,
        "#%06X",
        resources.getColor(R.color.calc_text_primary, null) and 0x00ff_ffff,
    )
    val styles = "<link rel='stylesheet' type='text/css' href='file:///android_asset/about_keepscore.css' />" +
        "<style>body { color: $textColor; font-family: sans-serif; }</style>"

    val content = readRawText(resources, R.raw.version_and_credits) +
        styles +
        readRawText(resources, R.raw.third_party_credits) +
        readRawText(resources, R.raw.translations) +
        readRawText(resources, R.raw.changelog)

    return String.format(
        Locale.ROOT,
        content,
        versionName,
        donateSuffix,
        donateMessage,
        resources.getString(R.string.html_third_party),
        resources.getString(R.string.html_translations),
    )
}

private fun readRawText(resources: android.content.res.Resources, resourceId: Int): String =
    resources.openRawResource(resourceId).bufferedReader(Charsets.UTF_8).use { it.readText() }
