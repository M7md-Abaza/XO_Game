package com.m7mdabaza.xogame.presentation.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.achievements.titleRes
import com.m7mdabaza.xogame.presentation.ads.BannerAd
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar
import com.m7mdabaza.xogame.presentation.components.formatEpochDay
import com.m7mdabaza.xogame.presentation.game.GameBoard

/** Stateful entry point used by the navigation graph. */
@Composable
fun DailyChallengeRoute(
    onBack: () -> Unit,
    viewModel: DailyChallengeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val canRequestAds = remember { ConsentManager.canRequestAds(context) }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.unlockedAchievements.collect { list ->
            list.forEach { snackbar.showSnackbar(resources.getString(R.string.achievements_unlocked_toast, resources.getString(it.titleRes()))) }
        }
    }
    DailyChallengeScreen(
        state = state,
        showBanner = canRequestAds,
        snackbar = snackbar,
        onBack = onBack,
        onCellClick = viewModel::onCellClick,
        onRetry = viewModel::onRetry,
    )
}

@Composable
fun DailyChallengeScreen(
    state: ChallengeUiState,
    showBanner: Boolean,
    onBack: () -> Unit,
    onCellClick: (Cell) -> Unit,
    onRetry: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenTopBar(stringResource(R.string.challenge_title), onBack = onBack)
        val challenge = state.challenge
        val game = state.game
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (challenge == null || game == null) {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator()
                    Text(stringResource(R.string.challenge_loading), modifier = Modifier.padding(top = 16.dp))
                    Spacer(Modifier.weight(1f))
                    return@Column
                }
                val sides = Modifier.padding(horizontal = 16.dp)
                // Kept short (one header line, the rules inside the status), so the
                // 8×8 board keeps its size at large font scales.
                Text(
                    stringResource(
                        R.string.challenge_header,
                        formatEpochDay(challenge.epochDay),
                        state.progress.currentStreak,
                        state.progress.bestStreak,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = sides,
                )
                Text(
                    statusText(state),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    // Room for two lines, so the board doesn't jump when a longer message shows.
                    minLines = 2,
                    modifier = sides
                        .padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                val win = game.result as? GameResult.Win
                // Nearly full width, so the 8×8 squares stay close to 48 dp (as on the game screen).
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    GameBoard(
                        board = game.board,
                        winningLine = win?.line,
                        winner = win?.player,
                        hint = null,
                        hintPlayer = challenge.player,
                        lastMove = game.lastMove?.cell,
                        enabled = state.boardEnabled,
                        onCellClick = onCellClick,
                        modifier = Modifier.padding(vertical = 8.dp).widthIn(max = 560.dp),
                    )
                }
                Box(sides.heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
                    when (state.phase) {
                        ChallengePhase.FAILED -> Button(onClick = onRetry, modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 220.dp)) {
                            Text(stringResource(R.string.challenge_try_again), style = MaterialTheme.typography.titleMedium)
                        }
                        ChallengePhase.SOLVED -> Button(onClick = onRetry, modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 220.dp)) {
                            Text(stringResource(R.string.challenge_play_again), style = MaterialTheme.typography.titleMedium)
                        }
                        else -> if (state.progress.solvedToday) {
                            Text(
                                stringResource(R.string.challenge_solved_before),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // Inside the content area, so a message never covers the banner below it.
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        if (showBanner && !state.adsRemoved) BannerAd(adUnitId = state.bannerAdUnitId)
    }
}

@Composable
private fun statusText(state: ChallengeUiState): String = stringResource(
    when (state.phase) {
        ChallengePhase.LOADING -> R.string.challenge_loading
        ChallengePhase.PLAYING -> if (state.movesPlayed == 0) R.string.challenge_find_move else R.string.challenge_finish
        ChallengePhase.DEFENDING -> R.string.challenge_defending
        ChallengePhase.SOLVED -> R.string.challenge_solved
        ChallengePhase.FAILED ->
            if (state.failReason == FailReason.MISSED_WIN) R.string.challenge_failed_missed else R.string.challenge_failed_no_fork
    },
)
