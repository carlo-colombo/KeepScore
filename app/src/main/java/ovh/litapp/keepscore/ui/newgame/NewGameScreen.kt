package ovh.litapp.keepscore.ui.newgame

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovh.litapp.keepscore.NewGameActivity.Companion.TYPICAL_MAX_NUMBER_OF_PLAYERS
import ovh.litapp.keepscore.OrganizePlayersActivity
import ovh.litapp.keepscore.R

/**
 * Button texts for the player counts shown directly on the screen.
 *
 * Index 0 is two players, index 1 is three players, and so on, so the player count of an entry is
 * its index plus two.
 */
private val typicalNumPlayersLabels = listOf(
    R.string.CONSTANT_button_2_players,
    R.string.CONSTANT_button_3_players,
    R.string.CONSTANT_button_4_players,
    R.string.CONSTANT_button_5_players,
    R.string.CONSTANT_button_6_players,
    R.string.CONSTANT_button_7_players,
    R.string.CONSTANT_button_8_players,
)

/**
 * "New Game" screen: pick the number of players, then go on to name them.
 *
 * Counts up to [TYPICAL_MAX_NUMBER_OF_PLAYERS] have their own button; the "More…" button opens a
 * dialog with every count up to [OrganizePlayersActivity.MAX_NUM_PLAYERS].
 */
@Composable
fun NewGameScreen(
    onNumPlayersSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMorePlayersDialog by rememberSaveable { mutableStateOf(false) }
    val background = Brush.verticalGradient(
        listOf(colorResource(R.color.calc_background), colorResource(R.color.calc_surface)),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(15.dp))
        Text(
            text = stringResource(R.string.text_num_players),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            typicalNumPlayersLabels.forEachIndexed { index, label ->
                NumPlayersButton(label = label, onClick = { onNumPlayersSelected(index + 2) })
            }
            NumPlayersButton(label = R.string.button_more, onClick = { showMorePlayersDialog = true })
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (showMorePlayersDialog) {
        MorePlayersDialog(
            onDismiss = { showMorePlayersDialog = false },
            onNumPlayersSelected = { numPlayers ->
                showMorePlayersDialog = false
                onNumPlayersSelected(numPlayers)
            },
        )
    }
}

@Composable
private fun NumPlayersButton(@StringRes label: Int, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(label), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MorePlayersDialog(
    onDismiss: () -> Unit,
    onNumPlayersSelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.text_num_players)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (numPlayers in TYPICAL_MAX_NUMBER_OF_PLAYERS + 1..OrganizePlayersActivity.MAX_NUM_PLAYERS) {
                    TextButton(
                        onClick = { onNumPlayersSelected(numPlayers) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = numPlayers.toString(), fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {},
    )
}
