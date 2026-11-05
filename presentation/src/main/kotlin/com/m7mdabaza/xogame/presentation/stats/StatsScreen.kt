package com.m7mdabaza.xogame.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.usecase.ObserveStatsUseCase
import com.m7mdabaza.xogame.domain.usecase.ResetStatsUseCase
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar
import com.m7mdabaza.xogame.presentation.components.TextChoices
import com.m7mdabaza.xogame.presentation.home.label
import com.m7mdabaza.xogame.presentation.home.titleRes
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class StatsViewModel @Inject constructor(
    observeStats: ObserveStatsUseCase,
    private val resetStats: ResetStatsUseCase,
) : ViewModel() {

    val stats: StateFlow<Stats> = observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    fun reset() {
        viewModelScope.launch { resetStats() }
    }
}

/** Stateful entry point used by the navigation graph. */
@Composable
fun StatsRoute(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    StatsScreen(stats = stats, onBack = onBack, onReset = viewModel::reset)
}

@Composable
fun StatsScreen(
    stats: Stats,
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(GameMode.VS_COMPUTER) }
    var boardSize by rememberSaveable { mutableStateOf(BoardSize.THREE) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        ScreenTopBar(stringResource(R.string.stats_title), onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            // The two-phone records get their tab with the two-phone game (v3 milestone 4).
            TextChoices(listOf(GameMode.VS_COMPUTER, GameMode.TWO_PLAYERS), mode, { mode = it }) { option ->
                stringResource(
                    if (option == GameMode.VS_COMPUTER) R.string.stats_tab_vs_computer else R.string.stats_tab_two_players,
                )
            }
            when (mode) {
                // One board at a time, with a card per level.
                GameMode.VS_COMPUTER -> {
                    Spacer(Modifier.height(12.dp))
                    TextChoices(BoardSize.entries, boardSize, { boardSize = it }) { it.label() }
                    Text(
                        stringResource(R.string.board_win_rule, boardSize.winLength),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally),
                    )
                    Level.entries.forEach { level ->
                        Spacer(Modifier.height(16.dp))
                        RecordCard(
                            title = stringResource(level.titleRes()),
                            subtitle = null,
                            vsComputer = true,
                            stats = stats[mode, boardSize, level],
                        )
                    }
                }
                // No levels without the computer: a card per board.
                GameMode.TWO_PLAYERS -> BoardSize.entries.forEach { board ->
                    Spacer(Modifier.height(16.dp))
                    RecordCard(
                        title = stringResource(R.string.board_title, board.size),
                        subtitle = stringResource(R.string.board_win_rule, board.winLength),
                        vsComputer = false,
                        stats = stats[mode, board],
                    )
                }
                GameMode.TWO_PHONES -> Unit
            }
            Spacer(Modifier.height(24.dp))
            if (stats.playedTotal > 0) {
                TextButton(
                    onClick = { confirmReset = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.stats_reset)) }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.stats_reset_title)) },
            text = { Text(stringResource(R.string.stats_reset_message)) },
            confirmButton = {
                Button(
                    onClick = { confirmReset = false; onReset() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(stringResource(R.string.stats_reset_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmReset = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) { Text(stringResource(R.string.setup_cancel)) }
            },
        )
    }
}

/** One record: the human's results against the phone (with streaks), or X's and O's wins. */
@Composable
private fun RecordCard(title: String, subtitle: String?, vsComputer: Boolean, stats: LevelStats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Title over its rules, so long texts and large fonts wrap cleanly.
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (stats.played == 0) {
                Text(
                    stringResource(R.string.stats_no_games),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (vsComputer) {
                StatRow(
                    R.string.stats_played to stats.played.toString(),
                    R.string.stats_wins to stats.wins.toString(),
                    R.string.stats_losses to stats.losses.toString(),
                    R.string.stats_draws to stats.draws.toString(),
                )
                Spacer(Modifier.height(12.dp))
                StatRow(
                    R.string.stats_win_rate to stringResource(R.string.stats_percent, ((stats.winRate ?: 0f) * 100).roundToInt()),
                    R.string.stats_streak to stats.currentStreak.toString(),
                    R.string.stats_best_streak to stats.bestStreak.toString(),
                )
            } else {
                StatRow(
                    R.string.stats_played to stats.played.toString(),
                    R.string.stats_x_wins to stats.wins.toString(),
                    R.string.stats_o_wins to stats.losses.toString(),
                    R.string.stats_draws to stats.draws.toString(),
                )
            }
        }
    }
}

/** Equal-width cells of a big number over a small label. */
@Composable
private fun StatRow(vararg items: Pair<Int, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            val labelText = stringResource(label)
            Column(
                Modifier
                    .weight(1f)
                    // Read as "Wins: 3" instead of two separate items.
                    .clearAndSetSemantics { contentDescription = "$labelText: $value" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    labelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun StatsScreenPreview() {
    XoTheme {
        StatsScreen(
            stats = Stats(
                mapOf(
                    StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to
                        LevelStats(wins = 7, losses = 2, draws = 11, currentStreak = 2, bestStreak = 4),
                ),
            ),
            onBack = {},
            onReset = {},
        )
    }
}
