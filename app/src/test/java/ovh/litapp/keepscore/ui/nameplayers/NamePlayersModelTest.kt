package ovh.litapp.keepscore.ui.nameplayers

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NamePlayersModelTest {

    @Test
    fun playerCountIsBoundedToTwoThroughThirty() {
        assertEquals(NamePlayersModel.MIN_PLAYERS, NamePlayersModel.normalizePlayerCount(0))
        assertEquals(NamePlayersModel.MIN_PLAYERS, NamePlayersModel.normalizePlayerCount(2))
        assertEquals(8, NamePlayersModel.normalizePlayerCount(8))
        assertEquals(NamePlayersModel.MAX_PLAYERS, NamePlayersModel.normalizePlayerCount(30))
        assertEquals(NamePlayersModel.MAX_PLAYERS, NamePlayersModel.normalizePlayerCount(31))
    }

    @Test
    fun defaultColorsCycleThroughTheExistingBuiltInOrderForThirtyPlayers() {
        assertEquals(
            (0 until 30).map { (it % 16).toString() },
            NamePlayersModel.defaultPlayerColorSerializations(30),
        )
    }

    @Test
    fun twoPlayerDefaultsAreTheFirstTwoBuiltInColors() {
        assertEquals(
            listOf("0", "1"),
            NamePlayersModel.defaultPlayerColorSerializations(2),
        )
    }
}
