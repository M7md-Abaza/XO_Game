package com.m7mdabaza.xogame.presentation.twophones

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.multiplayer.EndReason
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhonePhase
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneState
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar

/**
 * The two-phone game. For now (v3 milestone 3) it shows the connection: who
 * you play with, the match and your mark. Milestone 4 puts the board here.
 */
@Composable
fun TwoPhonesGameRoute(
    onExit: () -> Unit,
    viewModel: TwoPhonesGameViewModel = hiltViewModel(),
) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    // Leaving is done once the session says so; then the goodbye is on its way.
    LaunchedEffect(match?.endReason) {
        if (match?.endReason == EndReason.YOU_LEFT) onExit()
    }
    TwoPhonesGameScreen(
        match = match,
        connectionMissing = viewModel.connectionMissing,
        onLeave = viewModel::onLeave,
        onExit = onExit,
    )
}

@Composable
fun TwoPhonesGameScreen(
    match: TwoPhoneState?,
    connectionMissing: Boolean,
    onLeave: () -> Unit,
    onExit: () -> Unit,
) {
    val ended = connectionMissing || match?.isEnded == true
    BackHandler { if (ended) onExit() else onLeave() }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        ScreenTopBar(stringResource(R.string.twophones_title), onBack = { if (ended) onExit() else onLeave() })
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val friend = match?.friendName?.ifBlank { null } ?: stringResource(R.string.twophones_unnamed)
            when {
                ended -> {
                    Message(endText(match?.endReason, friend))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onExit, modifier = Modifier.heightIn(min = 52.dp)) { Text(stringResource(R.string.twophones_back_home)) }
                }
                match == null || match.phase == TwoPhonePhase.CONNECTING -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Message(stringResource(R.string.twophones_connecting))
                }
                else -> {
                    Message(stringResource(R.string.twophones_connected, friend))
                    match.setup?.let { setup ->
                        val length = if (setup.matchLength == MatchLength.SINGLE) {
                            stringResource(R.string.setup_match_single)
                        } else {
                            stringResource(R.string.setup_match_best_of, setup.matchLength.rounds)
                        }
                        Text(
                            stringResource(R.string.twophones_game_summary, stringResource(R.string.board_size, setup.boardSize.size), length),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.twophones_you_play, match.mySymbol.name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onLeave, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.twophones_leave)) }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun endText(reason: EndReason?, friend: String): String = when (reason) {
    EndReason.FRIEND_LEFT -> stringResource(R.string.twophones_end_friend_left, friend)
    EndReason.UPDATE_THIS_APP -> stringResource(R.string.twophones_end_update_this)
    EndReason.FRIEND_MUST_UPDATE -> stringResource(R.string.twophones_end_friend_update)
    EndReason.OUT_OF_SYNC -> stringResource(R.string.twophones_end_out_of_sync)
    // No connection (e.g. after the app was restarted), or it dropped.
    EndReason.CONNECTION_LOST, EndReason.YOU_LEFT, null -> stringResource(R.string.twophones_end_connection_lost)
}

