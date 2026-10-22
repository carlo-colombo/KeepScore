package ovh.litapp.keepscore.ui.dialogs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the player-name suggestions the legacy `AutoCompleteTextView` filtered: the previous
 * players' names that start with what has been typed, ignoring case.
 */
class PlayerNameSuggestionsTest {

    private val previousPlayers = listOf("Alice", "albert", "Bob", "Carol")

    @Test
    fun suggestionsAreTheNamesThatStartWithTheTypedText() {
        assertEquals(listOf("Alice", "albert"), suggestionMatches(previousPlayers, "al"))
        assertEquals(listOf("Alice", "albert"), suggestionMatches(previousPlayers, "AL"))
        assertEquals(listOf("Bob"), suggestionMatches(previousPlayers, "B"))
    }

    @Test
    fun suggestionsMatchFromTheFirstCharacterOnly() {
        assertEquals(emptyList<String>(), suggestionMatches(previousPlayers, ""))
        assertEquals(emptyList<String>(), suggestionMatches(previousPlayers, "lice"))
        assertEquals(emptyList<String>(), suggestionMatches(previousPlayers, "zz"))
    }
}
