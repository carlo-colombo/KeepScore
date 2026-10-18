package ovh.litapp.keepscore.ui.scoring

/** The smallest text size [autosizeTextSize] shrinks to, as the legacy AutoResizeTextView did. */
const val MIN_AUTOSIZE_TEXT_SIZE_PX = 20f

/** How much [autosizeTextSize] lowers the text size per step, as AutoResizeTextView did. */
private const val AUTOSIZE_STEP_PX = 2f

/** History entries are always padded to at least this many characters. */
private const val MIN_HISTORY_CHARS = 3

/** One line of a player's history column, with its sign and padding already applied. */
data class HistoryLine(val value: Int, val text: String)

/** The lines of a history column that are drawn, and whether the last one is cut off. */
data class HistoryDisplay(val lines: List<HistoryLine>, val truncated: Boolean)

/** A value with its sign, the way the legacy scoreboard wrote deltas ("+3", "0" becomes "+0"). */
fun signedValue(value: Int): String = if (value >= 0) "+$value" else value.toString()

/**
 * The history entries the history column shows, oldest first.
 *
 * While the last entry is still being batched into (it is the badge, on top of the score) it is not
 * part of the history yet, and an entry that cannot be shown at all leaves the column empty.
 */
fun historyValuesToShow(history: List<Int>, modifiable: Boolean): List<Int> = when {
    history.isEmpty() -> emptyList()
    modifiable && history.size <= 1 -> emptyList()
    modifiable -> history.dropLast(1)
    else -> history
}

/**
 * The lines of the history column, newest first, every line padded to the widest entry so the
 * signs line up, and never shorter than [MIN_HISTORY_CHARS].
 */
fun historyLines(history: List<Int>, modifiable: Boolean): List<HistoryLine> {
    val shown = historyValuesToShow(history, modifiable)
    if (shown.isEmpty()) {
        return emptyList()
    }
    val width = shown.maxOf { signedValue(it).length }.coerceAtLeast(MIN_HISTORY_CHARS)
    return shown.asReversed().map { value -> HistoryLine(value, signedValue(value).padStart(width)) }
}

/**
 * The history lines to draw in a column that fits [maxLines], dropping the entries that do not fit
 * and marking the last drawn line as cut off so it can be faded, like the legacy AutofitTextView
 * faded the entry that was half visible.
 */
fun historyDisplay(history: List<Int>, modifiable: Boolean, maxLines: Int): HistoryDisplay {
    val lines = historyLines(history, modifiable)
    if (maxLines <= 0) {
        return HistoryDisplay(emptyList(), truncated = false)
    }
    return if (lines.size <= maxLines) {
        HistoryDisplay(lines, truncated = false)
    } else {
        HistoryDisplay(lines.take(maxLines), truncated = true)
    }
}

/**
 * The largest text size no bigger than [maxSizePx] at which text whose width is [widthAt] fits
 * [availablePx].
 *
 * This is the rule the legacy AutoResizeTextView used for the score and the player name: start at
 * the maximum size and lower it in steps until the text fits, but never below
 * [MIN_AUTOSIZE_TEXT_SIZE_PX]. A view that has not been measured yet keeps the maximum size.
 */
fun autosizeTextSize(maxSizePx: Float, availablePx: Float, widthAt: (Float) -> Float): Float {
    if (availablePx <= 0f) {
        return maxSizePx
    }
    var textSizePx = maxSizePx
    while (textSizePx > MIN_AUTOSIZE_TEXT_SIZE_PX && widthAt(textSizePx) > availablePx) {
        textSizePx -= AUTOSIZE_STEP_PX
    }
    return textSizePx
}
