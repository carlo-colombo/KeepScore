package ovh.litapp.keepscore.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.helper.ColorScheme
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.util.IntegerUtil
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val MAX_PLAYERS_FOR_ROUND_TABLE = 8
private val ZOOM_LEVELS = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.75f, 1f, 1.5f, 2f, 2.5f, 3f)
private val CHART_ITEM_WIDTH = 60.dp
private val CHART_LEGEND_WIDTH = 140.dp

private enum class HistoryView(val title: Int, val chart: Boolean) {
    TIMELINE(R.string.button_history_time_chart, true),
    ROUND_CHART(R.string.button_history_round_chart, true),
    ROUND_TABLE(R.string.button_history_round_table, false),
    PLAYER_TABLE(R.string.button_history_player_table, false),
}

/** The four history views, with legacy timestamp and player-count visibility rules. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(game: Game, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val showTimeline = remember(game) {
        game.playerScores.any { player -> player.history.orEmpty().any { it.timestamp > 0L } }
    }
    val showRoundTable = game.playerScores.size <= MAX_PLAYERS_FOR_ROUND_TABLE
    val tabs = remember(showTimeline, showRoundTable) {
        buildList {
            if (showTimeline) add(HistoryView.TIMELINE)
            add(HistoryView.ROUND_CHART)
            if (showRoundTable) add(HistoryView.ROUND_TABLE)
            add(HistoryView.PLAYER_TABLE)
        }
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    selectedTab = selectedTab.coerceIn(tabs.indices)
    var timelineZoomIndex by rememberSaveable { mutableIntStateOf(ZOOM_LEVELS.indexOf(1f)) }
    var roundZoomIndex by rememberSaveable { mutableIntStateOf(ZOOM_LEVELS.indexOf(1f)) }

    val timeline = remember(game, context) { timelineChartData(game, context) }
    val roundChart = remember(game, context) { roundChartData(game, context) }
    val playerTable = remember(game, context) { playerTableData(game, context) }
    val roundTable = remember(game, context) { roundTableData(game, context) }
    val currentView = tabs[selectedTab]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.menu_history)) },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate back"
                            },
                        ) {
                            Text(text = "←", fontSize = 24.sp)
                        }
                    },
                    actions = {
                        if (currentView.chart) {
                            val zoomIndex = if (currentView == HistoryView.TIMELINE) {
                                timelineZoomIndex
                            } else {
                                roundZoomIndex
                            }
                            IconButton(
                                onClick = {
                                    if (currentView == HistoryView.TIMELINE) {
                                        timelineZoomIndex = (timelineZoomIndex - 1).coerceAtLeast(0)
                                    } else {
                                        roundZoomIndex = (roundZoomIndex - 1).coerceAtLeast(0)
                                    }
                                },
                                enabled = zoomIndex > 0,
                                modifier = Modifier.semantics { contentDescription = "Zoom out" },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.action_zoom_out),
                                    contentDescription = null,
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (currentView == HistoryView.TIMELINE) {
                                        timelineZoomIndex = (timelineZoomIndex + 1).coerceAtMost(ZOOM_LEVELS.lastIndex)
                                    } else {
                                        roundZoomIndex = (roundZoomIndex + 1).coerceAtMost(ZOOM_LEVELS.lastIndex)
                                    }
                                },
                                enabled = zoomIndex < ZOOM_LEVELS.lastIndex,
                                modifier = Modifier.semantics { contentDescription = "Zoom in" },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.action_zoom_in),
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(stringResource(tab.title), maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (currentView) {
                HistoryView.TIMELINE -> HistoryChart(
                    data = timeline,
                    zoom = ZOOM_LEVELS[timelineZoomIndex],
                    timeline = true,
                )

                HistoryView.ROUND_CHART -> HistoryChart(
                    data = roundChart,
                    zoom = ZOOM_LEVELS[roundZoomIndex],
                    timeline = false,
                )

                HistoryView.ROUND_TABLE -> RoundHistoryTable(roundTable)
                HistoryView.PLAYER_TABLE -> PlayerHistoryTable(playerTable)
            }
        }
    }
}

@Composable
private fun HistoryChart(data: HistoryChartData, zoom: Float, timeline: Boolean) {
    val scrollState = rememberScrollState()
    val textColor = colorResource(R.color.chart_main)
    val horizontalGridColor = colorResource(R.color.chart_tertiary)
    val verticalGridColor = colorResource(R.color.chart_secondary)
    val density = LocalDensity.current
    val maxPoints = data.series.maxOfOrNull { it.points.size } ?: 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        val plotWidth = CHART_ITEM_WIDTH * zoom * (maxPoints - 1)
        val axisAndPadding = 82.dp
        val trailingLabelWidth = if (timeline) 72.dp else 12.dp
        val wantedCanvasWidth = axisAndPadding + plotWidth + trailingLabelWidth
        val canvasWidth = maxOf(maxWidth - CHART_LEGEND_WIDTH, wantedCanvasWidth)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState),
        ) {
            Column(
                modifier = Modifier
                    .width(CHART_LEGEND_WIDTH)
                    .fillMaxHeight()
                    .clipToBounds(),
                verticalArrangement = Arrangement.Center,
            ) {
                data.series.forEach { series ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(3.dp)
                                .background(Color(series.color)),
                        )
                        Text(
                            text = series.name,
                            modifier = Modifier.padding(start = 6.dp),
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .width(canvasWidth.coerceAtLeast(1.dp))
                    .fillMaxHeight()
                    .semantics(mergeDescendants = true) {
                        val summary = data.series.joinToString(separator = "; ") { series ->
                            "${series.name}: ${series.points.lastOrNull() ?: 0}"
                        }
                        contentDescription = if (timeline) {
                            "Timeline chart. Final scores: $summary"
                        } else {
                            "Round-by-round chart. Final scores: $summary"
                        }
                    },
            ) {
                if (data.series.isEmpty()) return@Canvas

                val axisLabelWidth = 54.dp.toPx()
                val left = axisLabelWidth + 12.dp.toPx()
                val top = 12.dp.toPx()
                val bottomLabelHeight = if (timeline) 32.dp.toPx() else 12.dp.toPx()
                val bottom = size.height - bottomLabelHeight
                val plotHeight = (bottom - top).coerceAtLeast(1f)
                val itemWidth = with(density) { CHART_ITEM_WIDTH.toPx() * zoom }
                val plotRight = left + (maxPoints - 1).coerceAtLeast(0) * itemWidth
                val minimum = min(0L, data.series.minOf { line -> line.points.minOrNull() ?: 0L })
                val maximum = max(0L, data.series.maxOf { line -> line.points.maxOrNull() ?: 0L })
                val adjustedMaximum = if (minimum == maximum) maximum + 1 else maximum
                val yMinimum = floor(minimum / 5.0).toLong() * 5L
                val yMaximum = (ceil(adjustedMaximum / 5.0) * 5.0).toLong().coerceAtLeast(yMinimum + 5)
                val axisTextSize = 12.sp.toPx()
                val axisPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor.toArgb()
                    textSize = axisTextSize
                    typeface = android.graphics.Typeface.MONOSPACE
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                val mainPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor.toArgb()
                    textSize = 12.sp.toPx()
                    typeface = android.graphics.Typeface.MONOSPACE
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                val plotTop = top
                val plotBottom = bottom - if (timeline) 8.dp.toPx() else 0f
                val plotAreaHeight = (plotBottom - plotTop).coerceAtLeast(1f)
                val nativeCanvas = drawContext.canvas.nativeCanvas

                // Horizontal grid, score labels, and border.
                for (tick in 0..4) {
                    val fraction = tick / 4f
                    val y = plotTop + plotAreaHeight * fraction
                    drawLine(
                        color = horizontalGridColor,
                        start = androidx.compose.ui.geometry.Offset(left, y),
                        end = androidx.compose.ui.geometry.Offset(plotRight, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                    val value = (yMaximum - ((yMaximum - yMinimum) * fraction)).toLong()
                    nativeCanvas.drawText(value.toString(), left - 7.dp.toPx(), y + axisTextSize / 3f, axisPaint)
                }
                drawLine(
                    color = textColor,
                    start = androidx.compose.ui.geometry.Offset(left, plotTop),
                    end = androidx.compose.ui.geometry.Offset(left, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = textColor,
                    start = androidx.compose.ui.geometry.Offset(left, plotTop),
                    end = androidx.compose.ui.geometry.Offset(plotRight, plotTop),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = textColor,
                    start = androidx.compose.ui.geometry.Offset(plotRight, plotTop),
                    end = androidx.compose.ui.geometry.Offset(plotRight, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = textColor,
                    start = androidx.compose.ui.geometry.Offset(plotRight, plotBottom),
                    end = androidx.compose.ui.geometry.Offset(left, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )

                for (pointIndex in 1 until maxPoints - 1) {
                    val x = left + pointIndex * itemWidth
                    drawLine(
                        color = verticalGridColor,
                        start = androidx.compose.ui.geometry.Offset(x, plotTop),
                        end = androidx.compose.ui.geometry.Offset(x, plotBottom),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                if (timeline) {
                    data.xAxisLabels.forEachIndexed { pointIndex, label ->
                        val x = left + pointIndex * itemWidth
                        nativeCanvas.drawText(label, x, size.height - 6.dp.toPx(), mainPaint)
                    }
                }

                data.series.forEach { series ->
                    if (series.points.isEmpty()) return@forEach
                    val points = series.points.mapIndexed { index, score ->
                        val fraction = (score - yMinimum).toDouble() / (yMaximum - yMinimum).toDouble()
                        androidx.compose.ui.geometry.Offset(
                            x = left + index * itemWidth,
                            y = plotBottom - (fraction * plotAreaHeight).toFloat(),
                        )
                    }
                    val color = Color(series.color)
                    val path = Path().apply {
                        points.forEachIndexed { index, point ->
                            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                        }
                    }
                    drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
                    points.forEachIndexed { index, point ->
                        if (!timeline || index == 0 || index == points.lastIndex ||
                            series.points[index] != series.points.getOrNull(index - 1) ||
                            series.points[index] != series.points.getOrNull(index + 1)
                        ) {
                            drawCircle(color = color, radius = 2.dp.toPx(), center = point)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHistoryTable(groups: List<HistoryPlayerTableGroup>) {
    val background = MaterialTheme.colorScheme.surface
    val headerBackground = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 8.dp),
    ) {
        groups.forEachIndexed { groupIndex, group ->
            item(key = "header-$groupIndex") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBackground)
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = listOfNotNull(group.leftName, group.rightName)
                                .joinToString(" and ")
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.leftName,
                        modifier = Modifier.weight(1f),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                    Text(
                        text = group.rightName ?: " ",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            items(group.rows, key = { row -> "group-$groupIndex-row-${row.stripe}" }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HistoryScoreCell(
                        cell = row.left,
                        stripe = row.stripe,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .width(2.dp)
                            .heightIn(min = 44.dp)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                    HistoryScoreCell(
                        cell = row.right,
                        stripe = row.stripe,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun RoundHistoryTable(data: HistoryRoundTableData) {
    val background = MaterialTheme.colorScheme.surface
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 8.dp),
    ) {
        val roundColumnWidth = 48.dp
        val totalColumnWidth = 72.dp
        val tableWidth = maxOf(maxWidth, roundColumnWidth + totalColumnWidth + 112.dp * data.playerNames.size)
        val playerColumnWidth = (tableWidth - roundColumnWidth - totalColumnWidth) /
            data.playerNames.size.coerceAtLeast(1)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .width(tableWidth)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(" ", modifier = Modifier.width(roundColumnWidth))
                data.playerNames.forEach { name ->
                    Text(
                        text = name,
                        modifier = Modifier.width(playerColumnWidth).padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.CONSTANT_text_epsilon),
                    modifier = Modifier.width(totalColumnWidth),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            LazyColumn(
                modifier = Modifier
                    .width(tableWidth)
                    .weight(1f),
            ) {
                items(data.rows, key = { row -> row.roundNumber ?: 0 }) { row ->
                    val rowColor = stripeColor(row.roundNumber ?: 0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowColor)
                            .semantics(mergeDescendants = true) {
                                contentDescription = if (row.isStartingScore) {
                                    "Starting scores"
                                } else {
                                    "Round ${row.roundNumber}"
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.roundNumber?.toString() ?: "",
                            modifier = Modifier.width(roundColumnWidth).padding(horizontal = 6.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                        )
                        row.cells.forEach { cell ->
                            RoundScoreCell(
                                cell = cell,
                                playerCount = data.playerNames.size,
                                stripe = row.roundNumber ?: 0,
                                modifier = Modifier.width(playerColumnWidth),
                            )
                        }
                        Text(
                            text = row.total.toString(),
                            modifier = Modifier.width(totalColumnWidth).padding(horizontal = 5.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            maxLines = 1,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RoundScoreCell(
    cell: HistoryTableCell?,
    playerCount: Int,
    stripe: Int,
    modifier: Modifier = Modifier,
) {
    val compact = playerCount > 6
    val vertical = playerCount > 4
    Column(
        modifier = modifier
            .background(stripeColor(stripe))
            .padding(horizontal = 4.dp, vertical = if (vertical) 5.dp else 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        if (cell == null) return@Column
        val delta = cell.delta
        if (delta != null) {
            DeltaText(delta, compact = compact)
        }
        Text(
            text = cell.runningTotal.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun HistoryScoreCell(cell: HistoryTableCell?, stripe: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(stripeColor(stripe))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cell == null) return@Row
        val delta = cell.delta
        if (delta != null) DeltaText(delta)
        Text(
            text = cell.runningTotal.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun DeltaText(delta: Int, compact: Boolean = false) {
    val context = LocalContext.current
    val greenPositive = remember(context) { PreferenceHelper.getGreenTextPreference(context) }
    val colorResId = when {
        delta < 0 -> ColorScheme.Light.negativeColorResId
        greenPositive -> ColorScheme.Light.greenPositiveColorResId
        else -> ColorScheme.Light.positiveColorResId
    }
    Text(
        text = IntegerUtil.toCharSequenceWithSign(delta).toString(),
        color = colorResource(colorResId),
        fontFamily = FontFamily.Monospace,
        fontSize = if (compact) 12.sp else 14.sp,
        maxLines = 1,
    )
}

@Composable
private fun stripeColor(stripe: Int): Color = if (stripe % 2 == 0) {
    MaterialTheme.colorScheme.surface
} else {
    MaterialTheme.colorScheme.surfaceVariant
}
