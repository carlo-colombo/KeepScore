package ovh.litapp.keepscore.ui.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryDataTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun roundChartUsesHistoryItemsForStartingAndRunningScores() {
        val game = game(
            player("Ada", 0, 100, PlayerColor.BUILT_INS[0], Delta(102_000, 10), Delta(112_000, -3)),
            player("Ben", 1, 20, PlayerColor.BUILT_INS[1], Delta(106_000, 5), Delta(118_000, 3)),
        )

        val chart = roundChartData(game, context)

        assertEquals(listOf("Ada", "Ben"), chart.series.map { it.name })
        assertEquals(listOf(100L, 110L, 107L), chart.series[0].points)
        assertEquals(listOf(20L, 25L, 28L), chart.series[1].points)
        assertEquals(PlayerColor.BUILT_INS[0].toChartColor(context), chart.series[0].color)
        assertEquals(emptyList<String>(), chart.xAxisLabels)
    }

    @Test
    fun timelineGroupsFiveSecondBucketsAndCarriesUnchangedScores() {
        val game = game(
            player("Ada", 0, 100, PlayerColor.BUILT_INS[0], Delta(102_000, 10), Delta(112_000, -3)),
            player("Ben", 1, 20, PlayerColor.BUILT_INS[1], Delta(106_000, 5), Delta(118_000, 3)),
        ).apply { dateStarted = 100_000 }

        val chart = timelineChartData(game, context)

        assertEquals(listOf("0:00", "0:05", "0:10", "0:15"), chart.xAxisLabels)
        assertEquals(listOf(100L, 110L, 107L, 107L), chart.series[0].points)
        assertEquals(listOf(20L, 25L, 25L, 28L), chart.series[1].points)
    }

    @Test
    fun tableMappersAlignRoundsAndPadThePlayerTable() {
        val ada = player("Ada", 0, 100, PlayerColor.BUILT_INS[0], Delta(10, 10), Delta(20, -3))
        val ben = player("Ben", 1, 20, PlayerColor.BUILT_INS[1], Delta(10, 5))
        val cy = player("Cy", 2, -7, PlayerColor.BUILT_INS[2])
        val game = game(ada, ben, cy)

        val rounds = roundTableData(game, context)
        assertEquals(listOf("Ada", "Ben", "Cy"), rounds.playerNames)
        assertEquals(3, rounds.rows.size)
        assertEquals(null, rounds.rows[0].roundNumber)
        assertEquals(113L, rounds.rows[0].total)
        assertEquals(listOf(null, null, null), rounds.rows[0].cells.map { it?.delta })
        assertEquals(1, rounds.rows[1].roundNumber)
        assertEquals(listOf(10, 5, null), rounds.rows[1].cells.map { it?.delta })
        assertEquals(15L, rounds.rows[1].total)
        assertEquals(listOf(-3, null, null), rounds.rows[2].cells.map { it?.delta })
        assertEquals(-3L, rounds.rows[2].total)

        val players = playerTableData(game, context)
        assertEquals(2, players.size)
        assertEquals("Ada", players[0].leftName)
        assertEquals("Ben", players[0].rightName)
        assertEquals(3, players[0].rows.size)
        assertEquals(100L, players[0].rows[0].left?.runningTotal)
        assertEquals(null, players[0].rows[0].left?.delta)
        assertEquals(null, players[0].rows[2].right)
        assertEquals("Cy", players[1].leftName)
        assertEquals(null, players[1].rightName)
        assertEquals(1, players[1].rows.size)
        assertEquals(-7L, players[1].rows[0].left?.runningTotal)
    }

    private fun game(vararg players: PlayerScore) = Game().apply {
        dateStarted = 100_000
        playerScores = players.toList()
    }

    private fun player(
        name: String,
        number: Int,
        startingScore: Long,
        color: PlayerColor,
        vararg deltas: Delta,
    ) = PlayerScore().apply {
        this.name = name
        playerNumber = number
        playerColor = color
        history = deltas.toList()
        score = startingScore + deltas.sumOf { it.value.toLong() }
    }
}
