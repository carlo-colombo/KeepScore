package ovh.litapp.keepscore.ui.scoring

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PreferenceHelper

/**
 * Clicks the real scoreboard buttons.
 *
 * The minus button once negated the preference that already stores a negative number, so it added
 * points instead of removing them. That mapping lives in the composable, not in the state, so only a
 * test that actually presses the button can catch it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScoreboardButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val preferences = object : ScoringPreferences {
        override var updateDelayMs: Int = 10_000
        override var initialScore: Int = 0
    }

    private val menuActions = object : ScoringMenuActions {
        override fun showHistory() = Unit

        override fun editPlayers() = Unit

        override fun rematch() = Unit

        override fun undo() = Unit

        override fun redo() = Unit

        override fun openSettings() = Unit
    }

    private fun player(number: Int) = PlayerScore().apply {
        name = "Player ${number + 1}"
        score = 0
        playerNumber = number
        playerColor = PlayerColor.BUILT_INS[number]
        history = arrayListOf()
    }

    private fun showScoreboard(): ScoringScreenState {
        val state = ScoringScreenState(listOf(player(0), player(1)), preferences)
        composeRule.setContent { ScoringScreen(state, menuActions) }
        return state
    }

    @Test
    fun minusButtonSubtractsTheConfiguredValue() {
        val minusValue = PreferenceHelper.getMinusButtonValue(ApplicationProvider.getApplicationContext())
        // the preference is stored signed, which is what the button must apply unchanged
        assertTrue("the minus preference must stay negative, was $minusValue", minusValue < 0)

        val state = showScoreboard()
        state.increment(0, 20)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("-").onFirst().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(20L + minusValue, state.players[0].score)
    }

    @Test
    fun plusButtonAddsTheConfiguredValue() {
        val plusValue = PreferenceHelper.getPlusButtonValue(ApplicationProvider.getApplicationContext())
        assertTrue("the plus preference must be positive, was $plusValue", plusValue > 0)

        val state = showScoreboard()

        composeRule.onAllNodesWithText("+").onFirst().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(plusValue.toLong(), state.players[0].score)
    }
}
