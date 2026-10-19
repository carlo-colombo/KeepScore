package ovh.litapp.keepscore

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AboutActivityTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun pageRetainsVersionCreditsLinksTranslationsAndChangelog() {
        val html = buildAboutPageHtml(resources, "1.5.1", donateInstalled = false)

        assertTrue(html.contains("Version 1.5.1"))
        assertTrue(html.contains("Nolan Lawson"))
        assertTrue(html.contains("nolanlawson.com/apps/#keepscore"))
        assertTrue(html.contains("github.com/nolanlawson/KeepScore"))
        assertTrue(html.contains("play.google.com/store/apps/details?id=ovh.litapp.keepscore.donate"))
        assertTrue(html.contains("Action Bar Sherlock"))
        assertTrue(html.contains("Thomas Meyer"))
        assertTrue(html.contains("Changelog"))
        assertFalse(html.contains("Version 1.5.1 (Donate)"))
    }

    @Test
    fun donateEditionAddsItsVersionSuffixAndMessage() {
        val html = buildAboutPageHtml(resources, "1.5.1", donateInstalled = true)

        assertTrue(html.contains("Version 1.5.1 (Donate)"))
        assertTrue(html.contains("Thank you so much for donating!"))
    }
}
