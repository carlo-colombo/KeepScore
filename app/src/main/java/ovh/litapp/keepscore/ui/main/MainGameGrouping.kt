package ovh.litapp.keepscore.ui.main

import ovh.litapp.keepscore.data.TimePeriod
import ovh.litapp.keepscore.db.GameSummary
import java.util.Date

/** Organizes saved games by their recent-save period, retaining newest-first game order. */
internal fun organizeGamesByTimePeriod(
    games: List<GameSummary>,
    now: Date = Date(),
): List<Pair<TimePeriod, List<GameSummary>>> {
    val grouped = TimePeriod.values().associateWith { mutableListOf<GameSummary>() }
    games.sortedWith(GameSummary.byRecentlySaved()).forEach { game ->
        val period = TimePeriod.values().firstOrNull { candidate ->
            game.dateSaved >= candidate.startDateFunction.apply(now).time &&
                game.dateSaved < candidate.endDateFunction.apply(now).time
        } ?: TimePeriod.Older
        grouped.getValue(period).add(game)
    }
    return TimePeriod.values().mapNotNull { period ->
        grouped.getValue(period).takeIf { it.isNotEmpty() }?.let { period to it.toList() }
    }
}
