package ovh.litapp.keepscore.ui.scoring

import androidx.annotation.DimenRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PlayerTextFormat
import ovh.litapp.keepscore.helper.PreferenceHelper
import ovh.litapp.keepscore.ui.dialogs.AdditionalDeltasDialog
import ovh.litapp.keepscore.ui.dialogs.HistoryAction
import ovh.litapp.keepscore.ui.dialogs.HistoryContextDialog

/** How long the badge takes to fade out, matching the legacy PlayerView's animation. */
private const val BADGE_FADE_OUT_MILLIS = 1000

/** How opaque the last history line is when the entries below it do not fit. */
private const val TRUNCATED_HISTORY_LINE_ALPHA = 0.35f

private const val MINUS_LABEL = "-"
private const val PLUS_LABEL = "+"

/** One history line of text, used to measure how tall a history line is. */
private const val HISTORY_LINE_SAMPLE = "0"

/** How many onscreen delta buttons the landscape screen shows. */
private const val ONSCREEN_DELTA_BUTTONS = 4

/** The name row is this much taller than the name it holds, like the legacy layout's name holder. */
private const val NAME_ROW_HEIGHT_RATIO = 1.4f

/** Aspect ratio of `R.drawable.tag_holo_light`, the highlight tag. */
private const val NAME_TAG_ASPECT_RATIO = 50f / 30f

/** The width of the player colour swatch of the legacy portrait layout. */
private val ColorSwatchWidth = 10.dp

/** The padding of the legacy history column: 3dp on the sides and 5dp above and below. */
private val HistoryPaddingHorizontal = 3.dp
private val HistoryPaddingVertical = 5.dp

/** The padding around the buttons, cards and rows of the legacy layout. */
private val LayoutPadding = 3.dp

/**
 * The scoreboard: one card per player with their colour, name, score, history and the buttons that
 * change the score.
 *
 * Portrait shows one player per row in a scrolling column; landscape keeps the two-column grid the
 * legacy `layout-land` screen used, up to eight players. Larger games always use the scrollable
 * column so their cards remain usable, even when the device is in landscape. A long-press on a plus
 * or minus button opens the additional deltas dialog, and a long-press on the history or the score
 * opens the history dialog; both are rendered by the player card from its own dialog state, so the
 * gestures are the ones the legacy screen had.
 */
@Composable
fun ScoringScreen(state: ScoringScreenState, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // the legacy PlayerView re-read these on every resume; refreshPreferences() bumps the version
    val preferencesVersion = state.preferencesVersion
    val colorScheme = remember(preferencesVersion) { PreferenceHelper.getColorScheme(context) }
    val showColors = remember(preferencesVersion) { PreferenceHelper.getShowColors(context) }
    val showRoundTotals = remember(preferencesVersion) { PreferenceHelper.getShowRoundTotals(context) }
    val greenText = remember(preferencesVersion) { PreferenceHelper.getGreenTextPreference(context) }
    val highlightDisabled = remember(preferencesVersion) {
        PreferenceHelper.getBooleanPreference(
            R.string.CONSTANT_pref_disable_highlight_tag,
            R.string.CONSTANT_pref_disable_highlight_tag_default,
            context,
        )
    }

    val colors = ScoringColors(
        background = colorResource(colorScheme.backgroundColorResId),
        foreground = colorResource(colorScheme.foregroundColorResId),
        divider = colorResource(colorScheme.dividerColorResId),
        positive = colorResource(
            if (greenText) colorScheme.greenPositiveColorResId else colorScheme.positiveColorResId,
        ),
        negative = colorResource(colorScheme.negativeColorResId),
    )

    val numPlayers = state.players.size
    val orientation = scoringOrientationFor(LocalConfiguration.current.orientation, numPlayers)
    val rows = playerRows(numPlayers, orientation)
    val highlightedPlayerIndex = if (highlightDisabled) -1 else state.highlightedPlayerIndex()
    val showOnscreenDeltaButtons = orientation == ScoringOrientation.LANDSCAPE &&
        numPlayers <= LocalResources.current.getInteger(R.integer.max_players_for_onscreen_delta_buttons)

    // the legacy layout left a gap above and below a game of one or two players
    val rootPadding = if (numPlayers <= 2) dimensionResource(R.dimen.game_root_padding) else 0.dp

    MaterialTheme(
        colorScheme = darkColorScheme(background = colors.background, onBackground = colors.foreground),
    ) {
        Column(modifier = modifier.fillMaxSize().background(colors.background)) {
            if (showRoundTotals) {
                RoundTotalStrip(state = state, color = colors.foreground)
            }
            Spacer(modifier = Modifier.height(rootPadding))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (orientation) {
                    ScoringOrientation.PORTRAIT -> PortraitColumn(
                        state = state,
                        rows = rows,
                        colors = colors,
                        showColors = showColors,
                        highlightedPlayerIndex = highlightedPlayerIndex,
                    )

                    ScoringOrientation.LANDSCAPE -> LandscapeGrid(
                        state = state,
                        rows = rows,
                        colors = colors,
                        showColors = showColors,
                        highlightedPlayerIndex = highlightedPlayerIndex,
                        showOnscreenDeltaButtons = showOnscreenDeltaButtons,
                    )
                }
            }
            Spacer(modifier = Modifier.height(rootPadding))
        }
    }
}

/**
 * The round and the total of the round in progress, drawn above the scoreboard when the user asked
 * for it in the settings.
 */
@Composable
private fun RoundTotalStrip(state: ScoringScreenState, color: Color, modifier: Modifier = Modifier) {
    val roundTotal = state.roundTotal()
    Text(
        text = stringResource(R.string.text_round_total, maxOf(roundTotal.round, 1), roundTotal.total),
        color = color,
        fontSize = fontResource(R.dimen.round_total_text_size),
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** One player per row, however many there are, the way the portrait screen always laid them out. */
@Composable
private fun PortraitColumn(
    state: ScoringScreenState,
    rows: List<List<Int?>>,
    colors: ScoringColors,
    showColors: Boolean,
    highlightedPlayerIndex: Int,
) {
    val playerHeight = dimensionResource(R.dimen.portrait_max_player_score_size)
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        rows.forEach { row ->
            val playerIndex = row.first() ?: return@forEach
            PlayerCard(
                state = state,
                playerIndex = playerIndex,
                colors = colors,
                showColors = showColors,
                highlighted = playerIndex == highlightedPlayerIndex,
                showOnscreenDeltaButtons = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playerHeight)
                    .padding(LayoutPadding),
            )
        }
    }
}

/** The two-column grid for landscape games of at most eight players. */
@Composable
private fun LandscapeGrid(
    state: ScoringScreenState,
    rows: List<List<Int?>>,
    colors: ScoringColors,
    showColors: Boolean,
    highlightedPlayerIndex: Int,
    showOnscreenDeltaButtons: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { playerIndex ->
                    if (playerIndex == null) {
                        // an empty cell, so the last player of a half-filled row keeps its column
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        PlayerCard(
                            state = state,
                            playerIndex = playerIndex,
                            colors = colors,
                            showColors = showColors,
                            highlighted = playerIndex == highlightedPlayerIndex,
                            showOnscreenDeltaButtons = showOnscreenDeltaButtons,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(LayoutPadding),
                        )
                    }
                }
            }
        }
    }
}

/** A single player: the name row, the score row and, in landscape, the onscreen delta buttons. */
@Composable
private fun PlayerCard(
    state: ScoringScreenState,
    playerIndex: Int,
    colors: ScoringColors,
    showColors: Boolean,
    highlighted: Boolean,
    showOnscreenDeltaButtons: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = state.players[playerIndex]
    val textFormat = PlayerTextFormat.forNumPlayers(state.players.size)

    // recomposes when a pending batch window elapses, which is when the badge fades out
    val clockVersion = state.clockVersion
    val now = remember(clockVersion) { System.currentTimeMillis() }
    val modifiable = player.lastIncremented > 0 && now < player.lastIncremented + state.updateDelayMs

    // the long-press dialogs of this player: the value null means the dialog is closed
    var deltaDialogPositive by remember { mutableStateOf<Boolean?>(null) }
    var historyDialog by remember { mutableStateOf<HistoryDialogRequest?>(null) }

    LaunchedEffect(player.lastIncremented, player.history.size, state.updateDelayMs) {
        val remainingMs = player.lastIncremented + state.updateDelayMs - System.currentTimeMillis()
        if (player.lastIncremented > 0 && remainingMs > 0) {
            delay(remainingMs)
            state.onUpdateDelayElapsed()
        }
    }

    Column(
        modifier = modifier
            .border(1.dp, colors.border, RoundedCornerShape(7.dp))
            .padding(1.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                PlayerNameRow(
                    name = player.playerScore.toDisplayName(context).toString(),
                    playerColor = player.playerScore.playerColor ?: PlayerColor.BUILT_INS[0],
                    showColors = showColors,
                    highlighted = highlighted,
                    colors = colors,
                    textFormat = textFormat,
                )
                ScoreRow(
                    score = player.score,
                    modifiable = modifiable,
                    badgeValue = player.history.lastOrNull(),
                    colors = colors,
                    textFormat = textFormat,
                    minusValue = PreferenceHelper.getMinusButtonValue(context),
                    plusValue = PreferenceHelper.getPlusButtonValue(context),
                    onDelta = { delta -> state.increment(playerIndex, delta) },
                    onConfirm = { state.confirm(playerIndex) },
                    onOpenDeltaDialog = { positive -> deltaDialogPositive = positive },
                    onOpenHistoryDialog = {
                        // the legacy dialog captured these when it opened
                        historyDialog = HistoryDialogRequest(modifiable, player.history.isNotEmpty())
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(colors.divider))
            HistoryColumn(
                history = player.history,
                modifiable = modifiable,
                colors = colors,
                onOpenHistoryDialog = {
                    historyDialog = HistoryDialogRequest(modifiable, player.history.isNotEmpty())
                },
                modifier = Modifier
                    .width(dimensionResource(R.dimen.history_width))
                    .fillMaxHeight(),
            )
        }
        if (showOnscreenDeltaButtons) {
            OnscreenDeltaButtons(
                onDelta = { value -> state.increment(playerIndex, value) },
                colors = colors,
                textFormat = textFormat,
            )
        }
    }

    deltaDialogPositive?.let { positive ->
        AdditionalDeltasDialog(
            positive = positive,
            onDismissRequest = { deltaDialogPositive = null },
            onConfirm = { delta ->
                // the minus dialog reports the value the user entered, which the legacy screen negated
                when {
                    delta == 0 -> state.addZero(playerIndex)
                    positive -> state.increment(playerIndex, delta)
                    else -> state.increment(playerIndex, -delta)
                }
            },
        )
    }
    historyDialog?.let { request ->
        HistoryContextDialog(
            showConfirm = request.showConfirm,
            hasHistory = request.hasHistory,
            onDismissRequest = { historyDialog = null },
            onResult = { action ->
                when (action) {
                    HistoryAction.CONFIRM -> state.confirm(playerIndex)
                    HistoryAction.DELETE_LAST -> state.deleteLast(playerIndex)
                    HistoryAction.ADD_ZERO -> state.addZero(playerIndex)
                }
            },
        )
    }
}

/**
 * The state the history dialog opened with: whether the open entry can be confirmed and whether
 * there is an entry to delete, captured when the long-press happened like the legacy dialog did.
 */
private class HistoryDialogRequest(val showConfirm: Boolean, val hasHistory: Boolean)

/** The colour swatch, the player's name and the tag that marks the player who changed last. */
@Composable
private fun PlayerNameRow(
    name: String,
    playerColor: PlayerColor,
    showColors: Boolean,
    highlighted: Boolean,
    colors: ScoringColors,
    textFormat: PlayerTextFormat,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val nameSize = fontResource(textFormat.playerNameTextSize)
    val nameHeight = with(LocalDensity.current) { nameSize.toPx().toDp() }
    val nameRowHeight = nameHeight * NAME_ROW_HEIGHT_RATIO

    Row(
        modifier = modifier.fillMaxWidth().height(nameRowHeight).padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showColors) {
            Box(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .width(ColorSwatchWidth)
                    .fillMaxHeight()
                    .background(Color(playerColor.toColor(context))),
            )
        }
        AutoSizedText(
            text = name,
            maxSize = nameSize,
            color = colors.foreground,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.tag_holo_light),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // invisible rather than absent, so the name does not shift as the tag appears
            modifier = Modifier
                .height(nameHeight)
                .aspectRatio(NAME_TAG_ASPECT_RATIO)
                .alpha(if (highlighted) 1f else 0f),
        )
    }
}

/** The minus button, the score with its badge, and the plus button. */
@Composable
private fun ScoreRow(
    score: Long,
    modifiable: Boolean,
    badgeValue: Int?,
    colors: ScoringColors,
    textFormat: PlayerTextFormat,
    minusValue: Int,
    plusValue: Int,
    onDelta: (Int) -> Unit,
    onConfirm: () -> Unit,
    onOpenDeltaDialog: (positive: Boolean) -> Unit,
    onOpenHistoryDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonTextSize = fontResource(textFormat.plusMinusTextSize)
    val buttonWidth = dimensionResource(R.dimen.delta_button_width)

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScoreButton(
            label = MINUS_LABEL,
            textSize = buttonTextSize,
            colors = colors,
            onClick = { onDelta(-minusValue) },
            onLongClick = { onOpenDeltaDialog(false) },
            modifier = Modifier.width(buttonWidth).fillMaxHeight().padding(end = LayoutPadding),
        )
        AutoSizedText(
            text = score.toString(),
            maxSize = fontResource(textFormat.playerScoreTextSize),
            color = colors.foreground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .combinedClickable(
                    onClick = {},
                    onLongClick = onOpenHistoryDialog,
                ),
        ) {
            if (badgeValue != null) {
                DeltaBadge(
                    value = badgeValue,
                    visible = modifiable,
                    colors = colors,
                    textFormat = textFormat,
                    onClick = onConfirm,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = dimensionResource(textFormat.badgeOffset),
                            end = dimensionResource(textFormat.badgeOffset),
                        ),
                )
            }
        }
        ScoreButton(
            label = PLUS_LABEL,
            textSize = buttonTextSize,
            colors = colors,
            onClick = { onDelta(plusValue) },
            onLongClick = { onOpenDeltaDialog(true) },
            modifier = Modifier.width(buttonWidth).fillMaxHeight().padding(start = LayoutPadding),
        )
    }
}

/**
 * The right-hand history column: the player's deltas, newest first, in the colours the positive and
 * negative colours preference picked.
 */
@Composable
private fun HistoryColumn(
    history: List<Int>,
    modifiable: Boolean,
    colors: ScoringColors,
    onOpenHistoryDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontSize = fontResource(R.dimen.player_score_history)
    val measurer = rememberTextMeasurer()
    val lineHeightPx = remember(fontSize) {
        measurer.measure(AnnotatedString(HISTORY_LINE_SAMPLE), TextStyle(fontSize = fontSize))
            .size.height.toFloat()
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val availableHeightPx = with(density) { (maxHeight - HistoryPaddingVertical * 2).toPx() }
        val display = historyDisplay(history, modifiable, maxHistoryLines(availableHeightPx, lineHeightPx))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HistoryPaddingHorizontal, vertical = HistoryPaddingVertical)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onOpenHistoryDialog,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            display.lines.forEachIndexed { index, line ->
                val faded = display.truncated && index == display.lines.lastIndex
                Text(
                    text = line.text,
                    color = if (line.value >= 0) colors.positive else colors.negative,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alpha(if (faded) TRUNCATED_HISTORY_LINE_ALPHA else 1f),
                )
            }
        }
    }
}

/** The four delta buttons of the landscape screen, shown for a game of two players. */
@Composable
private fun OnscreenDeltaButtons(
    onDelta: (Int) -> Unit,
    colors: ScoringColors,
    textFormat: PlayerTextFormat,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val values = (0 until ONSCREEN_DELTA_BUTTONS).map { index ->
        PreferenceHelper.getTwoPlayerDeltaButtonValue(index, context)
    }
    val buttonTextSize = fontResource(textFormat.onscreenDeltaButtonTextSize)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(textFormat.onscreenDeltaButtonHeight))
                .padding(LayoutPadding),
            horizontalArrangement = Arrangement.spacedBy(LayoutPadding),
        ) {
            values.forEach { value ->
                ScoreButton(
                    label = signedValue(value),
                    textSize = buttonTextSize,
                    colors = colors,
                    onClick = { onDelta(value) },
                    onLongClick = {},
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

/** A + or - style button of a player's card. */
@Composable
private fun ScoreButton(
    label: String,
    textSize: TextUnit,
    colors: ScoringColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(colors.button)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = colors.foreground,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The badge on top of the score: the delta of the history entry that is still being batched into.
 * It fades out when the entry is closed, like the legacy badge did, and a tap accepts the entry.
 */
@Composable
private fun DeltaBadge(
    value: Int,
    visible: Boolean,
    colors: ScoringColors,
    textFormat: PlayerTextFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = fadeOut(animationSpec = tween(durationMillis = BADGE_FADE_OUT_MILLIS)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .background(if (value >= 0) colors.positive else colors.negative, RoundedCornerShape(50))
                .combinedClickable(onClick = onClick, onLongClick = {}),
        ) {
            Text(
                text = signedValue(value),
                color = Color.White,
                fontSize = fontResource(textFormat.badgeTextSize),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(textFormat.badgePaddingLeftRight),
                    vertical = dimensionResource(textFormat.badgePaddingTopBottom),
                ),
            )
        }
    }
}

/**
 * [text] centred in its box, shrunk until it fits the width of the box and never enlarged, the way
 * the legacy AutoResizeTextView shrank the score and the player name. [overlay] draws on top of the
 * text, e.g. the delta badge.
 */
@Composable
private fun AutoSizedText(
    text: String,
    maxSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val maxSizePx = with(density) { maxSize.toPx() }
        val availablePx = with(density) { maxWidth.toPx() }
        val style = remember(maxSize, fontWeight) {
            TextStyle(fontSize = maxSize, fontWeight = fontWeight)
        }
        // text width grows with the text size, so one measurement gives the width per size unit
        val widthPerSizeUnit = remember(text, style, maxSizePx) {
            if (maxSizePx <= 0f) {
                0f
            } else {
                measurer.measure(AnnotatedString(text), style).size.width / maxSizePx
            }
        }
        val textSizePx = autosizeTextSize(maxSizePx, availablePx) { sizePx -> widthPerSizeUnit * sizePx }

        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            fontSize = with(density) { textSizePx.toSp() },
        )
        overlay()
    }
}

/** A resource dimension in pixels, i.e. what the legacy `getDimension` returned for it. */
@Composable
private fun dimensionPx(@DimenRes id: Int): Float = LocalResources.current.getDimension(id)

/**
 * A resource dimension as a Compose font size. The dimension is read in pixels first, so an `sp`
 * dimension scales with the user's font size once and not twice.
 */
@Composable
private fun fontResource(@DimenRes id: Int): TextUnit =
    with(LocalDensity.current) { dimensionPx(id).toSp() }

/**
 * The colours the scoreboard draws with, derived from the user's colour-scheme preference. The
 * buttons and the card borders are tints of the two colours the scheme defines, so every scheme
 * keeps readable buttons.
 */
@Immutable
private class ScoringColors(
    val background: Color,
    val foreground: Color,
    val divider: Color,
    val positive: Color,
    val negative: Color,
) {
    val button: Color = lerp(background, foreground, 0.12f)
    val border: Color = lerp(background, foreground, 0.35f)
}
