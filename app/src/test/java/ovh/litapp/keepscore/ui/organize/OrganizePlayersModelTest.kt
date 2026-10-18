package ovh.litapp.keepscore.ui.organize

import java.util.ArrayList
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.OrganizePlayersActivity
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor

/**
 * Tests for the ordering rules of the "Edit Players" screen: every operation that changes the order
 * renumbers the players, because the game screens address them by player number.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrganizePlayersModelTest {

    private fun player(
        name: String,
        score: Long,
        number: Int = 0,
        withHistory: Boolean = false,
    ) = PlayerScore().apply {
        this.name = name
        this.score = score
        playerNumber = number
        playerColor = PlayerColor.BUILT_INS[0]
        history = if (withHistory) arrayListOf(Delta(0L, 5)) else ArrayList()
    }

    private fun OrganizePlayersModel.names() = players.map { it.name }

    private fun OrganizePlayersModel.numbers() = players.map { it.playerNumber }

    @Test
    fun playersStartInTheGivenOrderWithTheirOwnNumbers() {
        val model = OrganizePlayersModel(
            listOf(player("a", 30, number = 0), player("b", 10, number = 1), player("c", 20, number = 2)),
        )

        assertEquals(listOf("a", "b", "c"), model.names())
        assertEquals(listOf(0, 1, 2), model.numbers())
    }

    @Test
    fun movingAPlayerRenumbersTheRest() {
        val model = OrganizePlayersModel(
            listOf(player("a", 30, number = 0), player("b", 10, number = 1), player("c", 20, number = 2)),
        )

        model.movePlayer(0, 2)

        assertEquals(listOf("b", "c", "a"), model.names())
        assertEquals(listOf(0, 1, 2), model.numbers())
        assertEquals(2, model.players[2].playerScore.playerNumber)
    }

    @Test
    fun sortByScoreAlternatesBetweenAscendingAndDescending() {
        val model = OrganizePlayersModel(
            listOf(player("a", 30, number = 0), player("b", 10, number = 1), player("c", 20, number = 2)),
        )

        model.sortByScore()
        assertEquals(listOf("b", "c", "a"), model.names())
        assertEquals(listOf(0, 1, 2), model.numbers())

        model.sortByScore()
        assertEquals(listOf("a", "c", "b"), model.names())
        assertEquals(listOf(0, 1, 2), model.numbers())
    }

    @Test
    fun shufflingKeepsEveryPlayerAndRenumbersThem() {
        val model = OrganizePlayersModel(
            listOf(player("a", 30, number = 0), player("b", 10, number = 1), player("c", 20, number = 2)),
        )

        model.shuffle(Random(1))

        assertEquals(listOf("a", "b", "c"), model.names().sorted())
        assertEquals(listOf(0, 1, 2), model.numbers())
    }

    @Test
    fun addingAPlayerAppendsItWithTheNextNumberAndBuiltInColour() {
        val model = OrganizePlayersModel(listOf(player("a", 30, number = 0), player("b", 10, number = 1)))

        model.addPlayer("c", 100)

        assertEquals(listOf("a", "b", "c"), model.names())
        val added = model.players.last()
        assertEquals(2, added.playerNumber)
        assertEquals(PlayerColor.BUILT_INS[2], added.playerColor)
        assertEquals(100L, added.playerScore.score)
        assertTrue(added.playerScore.history.isEmpty())
    }

    @Test
    fun addingIsNotOfferedOnceTheMaximumNumberOfPlayersIsReached() {
        val playersAtMaximum = (1..OrganizePlayersActivity.MAX_NUM_PLAYERS).map { player("p$it", 0) }

        assertFalse(OrganizePlayersModel(playersAtMaximum).canAddPlayer)
        assertTrue(OrganizePlayersModel(playersAtMaximum.drop(1)).canAddPlayer)
    }

    @Test
    fun deletingAPlayerRenumbersTheRest() {
        val model = OrganizePlayersModel(
            listOf(player("a", 30, number = 0), player("b", 10, number = 1), player("c", 20, number = 2)),
        )

        model.deletePlayer(model.players[1])

        assertEquals(listOf("a", "c"), model.names())
        assertEquals(listOf(0, 1), model.numbers())
        assertTrue(model.deletedPlayersWithHistory.isEmpty())
    }

    @Test
    fun deletingAPlayerWithAScoreHistoryIsRememberedForTheWarning() {
        val model = OrganizePlayersModel(
            listOf(
                player("a", 30, number = 0, withHistory = true),
                player("b", 10, number = 1),
                player("c", 20, number = 2),
            ),
        )

        model.deletePlayer(model.players[0])

        assertEquals(listOf("b", "c"), model.names())
        assertEquals(listOf("a"), model.deletedPlayersWithHistory.map { it.name })
    }

    @Test
    fun theLastTwoPlayersCannotBeDeleted() {
        val model = OrganizePlayersModel(listOf(player("a", 30, number = 0), player("b", 10, number = 1)))

        assertFalse(model.canDeletePlayer)
        model.deletePlayer(model.players[0])

        assertEquals(listOf("a", "b"), model.names())
    }

    @Test
    fun theResultIsTheSamePlayersInDisplayOrder() {
        val first = player("a", 30, number = 0)
        val second = player("b", 10, number = 1)
        val model = OrganizePlayersModel(listOf(first, second))

        model.movePlayer(0, 1)

        val result = model.toResultList()
        assertEquals(listOf(second, first), result)
        assertSame(second, result[0])
    }

    @Test
    fun renamingAndRecolouringAreWrittenToThePlayerScore() {
        val original = player("a", 30, number = 0)
        val model = OrganizePlayersModel(listOf(original))

        model.players[0].rename("b")
        model.players[0].recolor(PlayerColor.BUILT_INS[3])

        assertEquals("b", original.name)
        assertEquals(PlayerColor.BUILT_INS[3], original.playerColor)
    }
}
