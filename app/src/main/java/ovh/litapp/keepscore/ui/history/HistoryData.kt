package ovh.litapp.keepscore.ui.history

import android.content.Context
import ovh.litapp.keepscore.data.HistoryItem
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.util.TimeUtil
import java.util.TreeMap
import kotlin.math.floor

/** One player's plotted score history, in oldest-to-newest order. */
data class HistorySeries(
    val name: String,
    val color: Int,
    val points: List<Long>,
)

/** Values shared by the timeline and round-by-round charts. */
data class HistoryChartData(
    val xAxisLabels: List<String>,
    val series: List<HistorySeries>,
)

/** A displayed delta and its running score; a null delta is the initial score entry. */
data class HistoryTableCell(
    val delta: Int?,
    val runningTotal: Long,
)

data class HistoryPlayerTableRow(
    val left: HistoryTableCell?,
    val right: HistoryTableCell?,
    val stripe: Int,
)

data class HistoryPlayerTableGroup(
    val leftName: String,
    val rightName: String?,
    val rows: List<HistoryPlayerTableRow>,
)

data class HistoryRoundTableRow(
    /** Null for the starting-score row, otherwise the one-based round number. */
    val roundNumber: Int?,
    val cells: List<HistoryTableCell?>,
    val total: Long,
    val isStartingScore: Boolean,
)

data class HistoryRoundTableData(
    val playerNames: List<String>,
    val rows: List<HistoryRoundTableRow>,
)

/** The round chart's points come directly from HistoryItem's production running-total mapping. */
fun roundChartData(game: Game, context: Context): HistoryChartData = HistoryChartData(
    xAxisLabels = emptyList(),
    series = game.playerScores.map { player ->
        HistorySeries(
            name = player.toDisplayName(context).toString(),
            color = player.playerColor.toChartColor(context),
            points = HistoryItem.createFromPlayerScore(player, context).map { it.runningTotal },
        )
    },
)

/**
 * Builds the timeline by grouping deltas into five-second buckets, carrying forward unchanged
 * scores, and preserving the legacy rule that the first point is at the game start.
 */
fun timelineChartData(game: Game, context: Context): HistoryChartData {
    val playerScores = game.playerScores
    val timeline = TreeMap<Int, MutableMap<Int, Long>>()
    val roundedStartTimeMs = floor(game.dateStarted.toDouble() / TIMELINE_BUCKET_MS).toLong() *
        TIMELINE_BUCKET_MS

    playerScores.forEachIndexed { playerIndex, player ->
        val historyItems = HistoryItem.createFromPlayerScore(player, context)
        timeline.getOrPut(0) { mutableMapOf() }[playerIndex] = historyItems.firstOrNull()?.runningTotal ?: 0L

        player.history.orEmpty().forEachIndexed { deltaIndex, delta ->
            val runningTotal = historyItems.getOrNull(deltaIndex + 1)?.runningTotal ?: return@forEachIndexed
            var secondsSinceStart = floor(
                (delta.timestamp - roundedStartTimeMs).toDouble() / TIMELINE_BUCKET_MS,
            ).toInt() * TIMELINE_BUCKET_SECONDS
            if (secondsSinceStart == 0) {
                secondsSinceStart = TIMELINE_BUCKET_SECONDS
            }
            // Several changes by one player in the same bucket are represented by the last score.
            timeline.getOrPut(secondsSinceStart) { mutableMapOf() }[playerIndex] = runningTotal
        }
    }

    val runningScores = LongArray(playerScores.size)
    val labels = mutableListOf<String>()
    val points = List(playerScores.size) { mutableListOf<Long>() }
    timeline.forEach { (seconds, scores) ->
        labels += TimeUtil.formatSeconds(seconds)
        scores.forEach { (playerIndex, score) -> runningScores[playerIndex] = score }
        runningScores.forEachIndexed { playerIndex, score -> points[playerIndex] += score }
    }

    return HistoryChartData(
        xAxisLabels = labels,
        series = playerScores.mapIndexed { index, player ->
            HistorySeries(
                name = player.toDisplayName(context).toString(),
                color = player.playerColor.toChartColor(context),
                points = points[index].toList(),
            )
        },
    )
}

/** The player table displays two players side by side, padding the shorter history with blanks. */
fun playerTableData(game: Game, context: Context): List<HistoryPlayerTableGroup> {
    var stripe = 0
    return game.playerScores.chunked(2).map { players ->
        val leftHistory = historyCells(players[0], context)
        val rightHistory = players.getOrNull(1)?.let { historyCells(it, context) }.orEmpty()
        val rowCount = maxOf(leftHistory.size, rightHistory.size)
        val rows = (0 until rowCount).map { rowIndex ->
            HistoryPlayerTableRow(
                left = leftHistory.getOrNull(rowIndex),
                right = rightHistory.getOrNull(rowIndex),
                stripe = stripe++,
            )
        }
        HistoryPlayerTableGroup(
            leftName = players[0].toDisplayName(context).toString(),
            rightName = players.getOrNull(1)?.toDisplayName(context)?.toString(),
            rows = rows,
        )
    }
}

/** The round table aligns each player's score history by round and totals each row. */
fun roundTableData(game: Game, context: Context): HistoryRoundTableData {
    val histories = game.playerScores.map { historyCells(it, context) }
    val rowCount = histories.maxOfOrNull { it.size } ?: 0
    val rows = (0 until rowCount).map { rowIndex ->
        val cells = histories.map { it.getOrNull(rowIndex) }
        val startingScore = rowIndex == 0
        val rowTotal = if (startingScore) {
            cells.sumOf { it?.runningTotal ?: 0L }
        } else {
            cells.sumOf { it?.delta?.toLong() ?: 0L }
        }
        HistoryRoundTableRow(
            roundNumber = if (startingScore) null else rowIndex,
            cells = cells,
            total = rowTotal,
            isStartingScore = startingScore,
        )
    }
    return HistoryRoundTableData(
        playerNames = game.playerScores.map { it.toDisplayName(context).toString() },
        rows = rows,
    )
}

private fun historyCells(player: PlayerScore, context: Context): List<HistoryTableCell> =
    HistoryItem.createFromPlayerScore(player, context).map { historyItem ->
        HistoryTableCell(
            delta = if (historyItem.isHideDelta) null else historyItem.delta,
            runningTotal = historyItem.runningTotal,
        )
    }

private const val TIMELINE_BUCKET_SECONDS = 5
private const val TIMELINE_BUCKET_MS = 5_000L
