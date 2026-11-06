package com.m7mdabaza.xogame.presentation.twophones

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.EndReason
import com.m7mdabaza.xogame.domain.multiplayer.RequestKind
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneState
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.achievements.titleRes
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar
import com.m7mdabaza.xogame.presentation.components.TextChoices
import com.m7mdabaza.xogame.presentation.game.GameActions
import com.m7mdabaza.xogame.presentation.game.GameScreen
import com.m7mdabaza.xogame.presentation.game.isolated
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsDialogHost
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsViewModel
import kotlinx.coroutines.launch

/** Stateful entry point used by the navigation graph. */
@Composable
fun TwoPhonesGameRoute(
    onExit: () -> Unit,
    viewModel: TwoPhonesGameViewModel = hiltViewModel(),
    removeAdsViewModel: RemoveAdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val removeAds by removeAdsViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current
    val canRequestAds = remember { ConsentManager.canRequestAds(context) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val friend by rememberUpdatedState(state.match?.let { friendName(it) }.orEmpty())

    // Leaving is done once the session says so; then the goodbye is on its way.
    LaunchedEffect(state.match?.endReason) {
        if (state.match?.endReason == EndReason.YOU_LEFT) onExit()
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            // Each message on its own, so a snackbar never holds up the next event.
            when (event) {
                is TwoPhonesGameEvent.RequestDeclined -> {
                    val text = resources.getString(R.string.twophones_request_declined, friend)
                    scope.launch { snackbar.showSnackbar(text) }
                }
                is TwoPhonesGameEvent.AchievementsUnlocked -> event.achievements.forEach {
                    val text = resources.getString(R.string.achievements_unlocked_toast, resources.getString(it.titleRes()))
                    scope.launch { snackbar.showSnackbar(text) }
                }
            }
        }
    }

    RemoveAdsDialogHost(removeAds, removeAdsViewModel, snackbar)

    TwoPhonesGameScreen(
        state = state,
        snackbar = snackbar,
        showRemoveAdsBadge = removeAds.showBadge,
        showBanner = canRequestAds,
        actions = TwoPhonesGameActions(
            onCellClick = viewModel::onCellClick,
            onContinue = viewModel::onContinue,
            onRestartRound = viewModel::onRestartRound,
            onRequestNewMatch = viewModel::onRequestNewMatch,
            onAnswer = viewModel::onAnswer,
            onLeave = viewModel::onLeave,
            onExit = onExit,
            onRemoveAds = removeAdsViewModel::openDialog,
        ),
    )
}

/** Everything the two-phone game screen can ask for. */
data class TwoPhonesGameActions(
    val onCellClick: (Cell) -> Unit = {},
    /** Next round / Rematch. */
    val onContinue: () -> Unit = {},
    /** Asks the friend to restart the round. */
    val onRestartRound: () -> Unit = {},
    /** Asks the friend for a new match; [Starter.PLAYER_ONE] is this player. */
    val onRequestNewMatch: (BoardSize, MatchLength, Starter) -> Unit = { _, _, _ -> },
    /** Answers the friend's request. */
    val onAnswer: (Boolean) -> Unit = {},
    /** Says goodbye to the friend (after "Leave the game?"). */
    val onLeave: () -> Unit = {},
    /** Back to Home once the game is over. */
    val onExit: () -> Unit = {},
    val onRemoveAds: () -> Unit = {},
)

/**
 * The two-phone game: the shared game screen (board, score, turns) plus what
 * two phones need: the friend's requests, "Leave the game?", the new-match
 * request and why the game ended. Before the match starts (or without a
 * connection) it shows only a message.
 */
@Composable
fun TwoPhonesGameScreen(
    state: TwoPhonesUiState,
    actions: TwoPhonesGameActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    showRemoveAdsBadge: Boolean = false,
    // UMP allows ad requests; the banner also needs `GameUiState.adsRemoved` to be false.
    showBanner: Boolean = false,
) {
    val match = state.match
    val game = state.game
    if (match == null || game == null) {
        NoBoard(match, actions.onExit, actions.onLeave)
        return
    }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var choosingNewMatch by rememberSaveable { mutableStateOf(false) }
    val canNewMatch = game.twoPhones?.canNewMatch == true
    // The friend asked first, or the game ended: this player can't ask now.
    LaunchedEffect(canNewMatch) { if (!canNewMatch) choosingNewMatch = false }

    GameScreen(
        state = game,
        showBanner = showBanner,
        snackbar = snackbar,
        showRemoveAdsBadge = showRemoveAdsBadge,
        actions = GameActions(
            onBack = { if (match.isEnded) actions.onExit() else confirmLeave = true },
            onCellClick = actions.onCellClick,
            onRestartRound = actions.onRestartRound,
            onNextRound = actions.onContinue,
            onOpenSetup = { choosingNewMatch = true },
            onLeave = { confirmLeave = true },
            onRemoveAds = actions.onRemoveAds,
        ),
    )

    val friend = friendName(match)
    val friendRequest = match.friendRequest
    val setup = match.setup
    when {
        match.isEnded -> EndedDialog(endText(match.endReason, friend), actions.onExit)
        // The friend's request comes first: nothing else goes on until it's answered.
        friendRequest != null -> FriendRequestDialog(friendRequest, match, friend, actions.onAnswer)
        confirmLeave -> LeaveDialog(friend, onLeave = { confirmLeave = false; actions.onLeave() }, onStay = { confirmLeave = false })
        choosingNewMatch && setup != null -> NewMatchDialog(
            current = setup,
            iStart = setup.firstPlayer == match.mySymbol,
            friend = friend,
            onAsk = { boardSize, length, starter ->
                choosingNewMatch = false
                actions.onRequestNewMatch(boardSize, length, starter)
            },
            onDismiss = { choosingNewMatch = false },
        )
    }
}

/** The friend's name, ready to go into a sentence ("Player O" if they left it blank). */
@Composable
private fun friendName(match: TwoPhoneState): String =
    isolated(match.friendName?.ifBlank { null } ?: stringResource(R.string.game_player_default, match.friendSymbol.name))

/** Before the board: connecting, no connection, or the game ended before it started (another app version). */
@Composable
private fun NoBoard(match: TwoPhoneState?, onExit: () -> Unit, onLeave: () -> Unit) {
    val ended = match?.isEnded ?: true
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
            if (ended) {
                Message(endText(match?.endReason, match?.let { friendName(it) }.orEmpty()))
                Spacer(Modifier.height(24.dp))
                Button(onClick = onExit, modifier = Modifier.heightIn(min = 52.dp)) { Text(stringResource(R.string.twophones_back_home)) }
            } else {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Message(stringResource(R.string.twophones_connecting))
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

/** The game is over for good; the final board stays visible behind it. */
@Composable
private fun EndedDialog(text: String, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onExit,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(text, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) },
        confirmButton = {
            Button(onClick = onExit) { Text(stringResource(R.string.twophones_back_home)) }
        },
    )
}

/** "Sara wants to restart the round" / "Sara wants a new match: ...". */
@Composable
private fun FriendRequestDialog(kind: RequestKind, match: TwoPhoneState, friend: String, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        // Back declines; a tap outside does nothing, so nobody answers by accident.
        onDismissRequest = { onAnswer(false) },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(
                when (kind) {
                    RequestKind.RestartRound -> stringResource(R.string.twophones_request_restart_title, friend)
                    is RequestKind.NewMatch -> stringResource(R.string.twophones_request_new_match_title, friend)
                },
            )
        },
        text = {
            Column {
                when (kind) {
                    RequestKind.RestartRound -> Text(stringResource(R.string.twophones_request_restart_text))
                    is RequestKind.NewMatch -> {
                        Text(
                            gameSummary(kind.setup.boardSize, kind.setup.matchLength),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (kind.setup.firstPlayer == match.mySymbol) {
                                stringResource(R.string.twophones_new_match_you_start)
                            } else {
                                stringResource(R.string.twophones_new_match_friend_starts, friend)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.twophones_request_new_match_text))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAnswer(true) }) { Text(stringResource(R.string.twophones_accept)) }
        },
        dismissButton = {
            TextButton(
                onClick = { onAnswer(false) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) { Text(stringResource(R.string.twophones_decline)) }
        },
    )
}

@Composable
private fun LeaveDialog(friend: String, onLeave: () -> Unit, onStay: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(stringResource(R.string.twophones_leave_title)) },
        text = { Text(stringResource(R.string.twophones_leave_text, friend)) },
        confirmButton = {
            Button(
                onClick = onLeave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(R.string.twophones_leave)) }
        },
        dismissButton = {
            TextButton(
                onClick = onStay,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) { Text(stringResource(R.string.twophones_stay)) }
        },
    )
}

/**
 * Chooses a new match to ask the friend for; it starts if they accept. Wider
 * than an AlertDialog (like the match setup), so the three-way choices keep
 * their whole labels at large font sizes ("Best of 3" vs "Best of 5").
 */
@Composable
private fun NewMatchDialog(
    current: TwoPhoneSetup,
    iStart: Boolean,
    friend: String,
    onAsk: (BoardSize, MatchLength, Starter) -> Unit,
    onDismiss: () -> Unit,
) {
    var boardSize by rememberSaveable { mutableStateOf(current.boardSize) }
    var matchLength by rememberSaveable { mutableStateOf(current.matchLength) }
    var starter by rememberSaveable { mutableStateOf(if (iStart) Starter.PLAYER_ONE else Starter.PLAYER_TWO) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 480.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    stringResource(R.string.game_new_match),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Label(stringResource(R.string.picker_board))
                TextChoices(BoardSize.entries, boardSize, { boardSize = it }) { stringResource(R.string.board_size, it.size) }
                Label(stringResource(R.string.setup_match))
                TextChoices(MatchLength.entries, matchLength, { matchLength = it }) { matchLabel(it) }
                Label(stringResource(R.string.setup_who_starts))
                TextChoices(Starter.entries, starter, { starter = it }) { option ->
                    stringResource(
                        when (option) {
                            Starter.PLAYER_ONE -> R.string.twophones_starter_me
                            Starter.PLAYER_TWO -> R.string.twophones_starter_friend
                            Starter.RANDOM -> R.string.setup_starter_random
                        },
                    )
                }
                Text(
                    stringResource(R.string.twophones_new_match_note, friend),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    ) { Text(stringResource(R.string.setup_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onAsk(boardSize, matchLength, starter) }) { Text(stringResource(R.string.twophones_ask)) }
                }
            }
        }
    }
}
