package ovh.litapp.keepscore.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferencesTest {

    @Test
    fun preferredOrientationIsHiddenButRemainsInPersistentPreferenceRows() {
        val orientation = ChoicePreference(
            key = "orientation",
            titleRes = 1,
            entriesRes = 2,
            entryValuesRes = 3,
            defaultValue = "landscape",
        )
        val colorScheme = ChoicePreference(
            key = "color_scheme",
            titleRes = 4,
            entriesRes = 5,
            entryValuesRes = 6,
            defaultValue = "default",
        )
        val rows = listOf(
            SettingsRow.Category(titleRes = 7),
            SettingsRow.Choice(colorScheme),
            SettingsRow.Choice(orientation),
        )

        val visibleRows = visibleSettingsRows(rows, orientation)
        val persistentRows = persistentPreferenceRows(rows)

        assertTrue(visibleRows.contains(SettingsRow.Choice(colorScheme)))
        assertFalse(visibleRows.contains(SettingsRow.Choice(orientation)))
        assertTrue(persistentRows.contains(SettingsRow.Choice(orientation)))
    }
}
