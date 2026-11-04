package com.m7mdabaza.xogame.presentation.game

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.text.BidiFormatter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.AssistAccess
import com.m7mdabaza.xogame.domain.model.AssistKind
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.review.rememberInAppReview
import com.m7mdabaza.xogame.presentation.achievements.titleRes
import com.m7mdabaza.xogame.presentation.ads.BannerAd
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import com.m7mdabaza.xogame.presentation.ads.InterstitialAdManager
import com.m7mdabaza.xogame.presentation.ads.RewardedAdManager
import com.m7mdabaza.xogame.presentation.components.OMark
import com.m7mdabaza.xogame.presentation.components.XMark
import com.m7mdabaza.xogame.presentation.home.titleRes
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import kotlinx.coroutines.launch

/** Stateful entry point used by the navigation graph. */
@Composable
fun GameRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val canRequestAds = remember { ConsentManager.canRequestAds(context) }
    val snackbar = remember { SnackbarHostState() }
    val adNotReady = stringResource(R.string.ads_not_ready)

    // Full-screen ads, loaded ahead of time. Interstitials only while ads aren't removed.
    val interstitial = remember(activity, state.interstitialAdUnitId) {
        activity?.let { InterstitialAdManager(it, state.interstitialAdUnitId) }
    }
    val rewarded = remember(activity, state.rewardedAdUnitId) {
        activity?.let { RewardedAdManager(it, state.rewardedAdUnitId) }
    }
    LaunchedEffect(interstitial, state.adsRemoved) { if (!state.adsRemoved) interstitial?.load() }
    LaunchedEffect(rewarded) { rewarded?.load() }

    val requestReview = rememberInAppReview()
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GameEvent.ShowInterstitial ->
                    interstitial?.show(viewModel::onInterstitialFinished) ?: viewModel.onInterstitialFinished(false)
                is GameEvent.ShowRewarded -> {
                    val shown = rewarded?.show { earned -> viewModel.onRewardedFinished(event.kind, earned) } ?: false
                    if (!shown) viewModel.onRewardedUnavailable()
                }
                // Messages and the review sheet run on their own, so they never hold up the next event
                // (a snackbar waits several seconds; the host queues them).
                GameEvent.AdNotReady -> scope.launch { snackbar.showSnackbar(adNotReady) }
                is GameEvent.AchievementsUnlocked -> event.achievements.forEach {
                    val text = resources.getString(R.string.achievements_unlocked_toast, resources.getString(it.titleRes()))
                    scope.launch { snackbar.showSnackbar(text) }
                }
                GameEvent.RequestReview -> scope.launch { requestReview() }
            }
        }
    }

    GameScreen(
        state = state,
        showBanner = canRequestAds,
        snackbar = snackbar,
        actions = GameActions(
            onBack = onBack,
            onCellClick = viewModel::onCellClick,
            onUndo = viewModel::onUndo,
            onHint = viewModel::onHint,
            onRestartRound = viewModel::onRestartRound,
            onNextRound = viewModel::onNextRound,
            onOpenSetup = viewModel::onOpenSetup,
            onStartMatch = viewModel::onStartMatch,
            // Before the first match there is nothing to go back to.
            onDismissSetup = { if (state.hasStarted) viewModel.onDismissSetup() else onBack() },
            onAssistOfferAccepted = viewModel::onAssistOfferAccepted,
            onAssistOfferDismissed = viewModel::onAssistOfferDismissed,
            onRemoveAds = {
                viewModel.onAssistOfferDismissed()
                onOpenSettings()
            },
        ),
    )
}

/** Everything the game screen can ask for. */
data class GameActions(
    val onBack: () -> Unit = {},
    val onCellClick: (Cell) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onHint: () -> Unit = {},
    val onRestartRound: () -> Unit = {},
    val onNextRound: () -> Unit = {},
    val onOpenSetup: () -> Unit = {},
    val onStartMatch: (MatchSettings) -> Unit = {},
    val onDismissSetup: () -> Unit = {},
    val onAssistOfferAccepted: () -> Unit = {},
    val onAssistOfferDismissed: () -> Unit = {},
    /** Go to the Remove-ads offer (in Settings). */
    val onRemoveAds: () -> Unit = {},
)

@Composable
fun GameScreen(
    state: GameUiState,
    showBanner: Boolean,
    actions: GameActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackHandler(onBack = actions.onBack)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
        ) {
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (maxWidth > maxHeight * 1.2f) {
                    // Wide window (the app is locked to portrait, but split screen, desktop
                    // windowing and large screens on Android 16+ can still be wide): the board
                    // gets the full height on the start side; the top bar and everything else
                    // go beside it.
                    Row(Modifier.fillMaxSize().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Board(state, actions, Modifier.fillMaxHeight().padding(vertical = 8.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            TopBar(state, actions)
                            Column(
                                Modifier.weight(1f).padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Controls(state, actions)
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        TopBar(state, actions)
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val sides = Modifier.padding(horizontal = 16.dp)
                            Scoreboard(state, sides)
                            StatusLine(state, sides.padding(top = 12.dp))
                            // The 8×8 board gets almost the full width, so its squares
                            // stay close to the 48 dp touch-target guideline on phones.
                            val boardSides = if (state.level.boardSize > 4) 4.dp else 16.dp
                            Box(
                                Modifier.weight(1f).fillMaxWidth().padding(horizontal = boardSides),
                                contentAlignment = Alignment.Center,
                            ) {
                                Board(state, actions, Modifier.padding(vertical = 8.dp))
                            }
                            Box(sides) { BottomPanel(state, actions) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                // Inside the content area, so a message never covers the banner below it.
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
            if (showBanner && !state.adsRemoved) BannerAd(adUnitId = state.bannerAdUnitId)
        }
    }

    if (state.showSetup) {
        MatchSetupDialog(
            mode = state.mode,
            initial = state.settings,
            onStart = actions.onStartMatch,
            onDismiss = actions.onDismissSetup,
        )
    }
    MatchOverDialog(state, actions)
    AssistOfferDialog(state, actions)
}

@Composable
private fun ColumnScope.Controls(state: GameUiState, actions: GameActions) {
    Scoreboard(state)
    StatusLine(state, Modifier.padding(vertical = 16.dp))
    BottomPanel(state, actions)
}

@Composable
private fun Board(state: GameUiState, actions: GameActions, modifier: Modifier = Modifier) {
    val win = state.game.result as? GameResult.Win
    GameBoard(
        board = state.game.board,
        winningLine = win?.line,
        winner = win?.player,
        hint = state.hint,
        hintPlayer = state.game.turn,
        lastMove = state.game.lastMove?.cell,
        enabled = state.boardEnabled,
        onCellClick = actions.onCellClick,
        modifier = modifier.widthIn(max = 560.dp),
    )
}

@Composable
private fun TopBar(state: GameUiState, actions: GameActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.game_back))
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                stringResource(state.level.titleRes()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(
                    if (state.mode == GameMode.VS_COMPUTER) R.string.game_mode_vs_computer else R.string.game_mode_two_players,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = actions.onOpenSetup, enabled = state.hasStarted) {
            Icon(painterResource(R.drawable.ic_tune), contentDescription = stringResource(R.string.game_new_match))
        }
    }
}

/** The display name of whoever plays [player] in this match. */
@Composable
private fun nameOf(state: GameUiState, player: Player): String = when (state.mode) {
    GameMode.VS_COMPUTER ->
        stringResource(if (player == state.settings.humanSymbol) R.string.game_you else R.string.game_phone)
    GameMode.TWO_PLAYERS -> {
        val name = if (player == Player.X) state.settings.xName else state.settings.oName
        name.ifBlank { stringResource(R.string.game_player_default, player.name) }
    }
}

/**
 * Prepares a player name to go into a sentence: an Arabic name in English
 * text (or a Latin one in Arabic) keeps its own direction and doesn't reorder
 * the words around it. Names that already match the layout direction are
 * returned unchanged.
 */
@Composable
private fun isolated(name: String): String {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return BidiFormatter.getInstance(rtl).unicodeWrap(name)
}

@Composable
private fun Scoreboard(state: GameUiState, modifier: Modifier = Modifier) {
    val active = if (state.phase == GamePhase.PLAYING) state.game.turn else null
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerChip(Player.X, nameOf(state, Player.X), state.match.xWins, active == Player.X, Modifier.weight(1f))
            Text(
                ":",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            PlayerChip(Player.O, nameOf(state, Player.O), state.match.oWins, active == Player.O, Modifier.weight(1f))
        }
        val details = buildList {
            if (state.match.length != MatchLength.SINGLE) {
                val round = minOf(state.match.currentRound, state.match.length.rounds)
                add(stringResource(R.string.game_round_of, if (state.game.isOver) state.match.roundsPlayed else round, state.match.length.rounds))
            }
            if (state.match.draws > 0) add(stringResource(R.string.game_draws, state.match.draws))
        }
        if (details.isNotEmpty()) {
            Text(
                details.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PlayerChip(player: Player, name: String, score: Int, active: Boolean, modifier: Modifier = Modifier) {
    // One screen-reader item per chip: "You (X): 2" instead of loose fragments.
    val description = stringResource(R.string.game_score_description, name, player.name, score)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (player == Player.X) XMark(Modifier.size(28.dp)) else OMark(Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(score.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusLine(state: GameUiState, modifier: Modifier = Modifier) {
    val text = when (state.phase) {
        GamePhase.SETUP -> ""
        GamePhase.PLAYING -> when {
            state.computerThinking -> stringResource(R.string.game_thinking)
            state.hintLoading -> stringResource(R.string.game_hint_loading)
            state.hint != null -> stringResource(R.string.game_hint_shown)
            state.mode == GameMode.VS_COMPUTER -> stringResource(R.string.game_turn_you)
            else -> stringResource(R.string.game_turn_player, isolated(nameOf(state, state.game.turn)))
        }
        GamePhase.ROUND_OVER, GamePhase.MATCH_OVER -> roundResultText(state)
    }
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = modifier
            .heightIn(min = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun roundResultText(state: GameUiState): String = when (val result = state.game.result) {
    is GameResult.Win -> when {
        state.mode == GameMode.TWO_PLAYERS -> stringResource(R.string.game_result_player_wins, isolated(nameOf(state, result.player)))
        result.player == state.settings.humanSymbol -> stringResource(R.string.game_result_you_win)
        else -> stringResource(R.string.game_result_phone_wins)
    }
    else -> stringResource(R.string.game_result_draw)
}

@Composable
private fun matchResultText(state: GameUiState): String {
    val winner = state.match.winner
    return when {
        winner == null -> stringResource(R.string.game_match_draw)
        state.mode == GameMode.TWO_PLAYERS -> stringResource(R.string.game_match_player_wins, isolated(nameOf(state, winner)))
        winner == state.settings.humanSymbol -> stringResource(R.string.game_match_you_win)
        else -> stringResource(R.string.game_match_phone_wins)
    }
}

/** Undo / hint / restart while playing; the next-round button once a round is over. */
@Composable
private fun BottomPanel(state: GameUiState, actions: GameActions) {
    AnimatedContent(
        targetState = state.phase == GamePhase.PLAYING || state.phase == GamePhase.SETUP,
        // Same height for both states, so the board doesn't jump when a round ends
        // (only very large font sizes can make the buttons taller than this).
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        contentAlignment = Alignment.Center,
        label = "bottomPanel",
    ) { playing ->
        if (playing) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionButton(
                    R.drawable.ic_undo,
                    stringResource(R.string.game_undo),
                    state.canUndo,
                    actions.onUndo,
                    needsAd = state.undoAccess == AssistAccess.REWARDED_AD,
                )
                ActionButton(
                    R.drawable.ic_lightbulb,
                    stringResource(R.string.game_hint),
                    state.canHint,
                    actions.onHint,
                    loading = state.hintLoading,
                    needsAd = state.hintAccess == AssistAccess.REWARDED_AD,
                )
                ActionButton(
                    R.drawable.ic_refresh,
                    stringResource(R.string.game_restart),
                    state.phase == GamePhase.PLAYING && state.game.history.isNotEmpty(),
                    actions.onRestartRound,
                )
            }
        } else {
            Button(onClick = actions.onNextRound, modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 220.dp)) {
                Text(
                    stringResource(
                        when {
                            state.phase == GamePhase.MATCH_OVER -> R.string.game_new_match
                            state.match.length == MatchLength.SINGLE -> R.string.game_play_again
                            else -> R.string.game_next_round
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * An equal-width tile with the icon above the label, so the three actions fit
 * side by side at any font size and in any language.
 */
@Composable
private fun RowScope.ActionButton(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    loading: Boolean = false,
    /** Shows a small ad marker: using it costs a rewarded ad. */
    needsAd: Boolean = false,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = Modifier.weight(1f).heightIn(min = 64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
                }
                if (needsAd) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painterResource(R.drawable.ic_ad_video),
                        contentDescription = stringResource(R.string.ads_needs_ad),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Shown once when a best-of match ends; the bottom panel still offers a new match after closing it. */
@Composable
private fun MatchOverDialog(state: GameUiState, actions: GameActions) {
    var dismissed by rememberSaveable(state.match) { mutableStateOf(false) }
    if (state.phase != GamePhase.MATCH_OVER || dismissed) return
    val winner = state.match.winner
    AlertDialog(
        onDismissRequest = { dismissed = true },
        icon = {
            when (winner) {
                Player.X -> XMark(Modifier.size(56.dp))
                Player.O -> OMark(Modifier.size(56.dp))
                null -> Row {
                    XMark(Modifier.size(40.dp))
                    Spacer(Modifier.width(8.dp))
                    OMark(Modifier.size(40.dp))
                }
            }
        },
        title = { Text(matchResultText(state), textAlign = TextAlign.Center) },
        text = {
            Text(
                stringResource(R.string.game_match_score, state.match.xWins, state.match.oWins),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = actions.onNextRound) { Text(stringResource(R.string.game_new_match)) }
        },
        dismissButton = {
            TextButton(
                onClick = { dismissed = true; actions.onOpenSetup() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) {
                Text(stringResource(R.string.game_settings))
            }
        },
    )
}

/** "Watch a short ad for a hint/undo?" — rewarded ads are always the player's choice. */
@Composable
private fun AssistOfferDialog(state: GameUiState, actions: GameActions) {
    val kind = state.assistOffer ?: return
    AlertDialog(
        onDismissRequest = actions.onAssistOfferDismissed,
        icon = {
            Icon(
                painterResource(if (kind == AssistKind.HINT) R.drawable.ic_lightbulb else R.drawable.ic_undo),
                contentDescription = null,
            )
        },
        title = {
            Text(stringResource(if (kind == AssistKind.HINT) R.string.ads_offer_hint_title else R.string.ads_offer_undo_title))
        },
        text = {
            Column {
                Text(stringResource(if (state.adsRemoved) R.string.ads_offer_message_free_used else R.string.ads_offer_message))
                if (!state.adsRemoved) {
                    TextButton(
                        onClick = actions.onRemoveAds,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.ads_offer_remove_ads))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = actions.onAssistOfferAccepted) {
                Icon(painterResource(R.drawable.ic_ad_video), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ads_offer_watch))
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onAssistOfferDismissed,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) { Text(stringResource(R.string.setup_cancel)) }
        },
    )
}

@Preview(showSystemUi = true)
@Composable
private fun GameScreenPreview() {
    val config = com.m7mdabaza.xogame.domain.model.GameConfig(GameMode.VS_COMPUTER, com.m7mdabaza.xogame.domain.model.Level.EASY)
    val game = com.m7mdabaza.xogame.domain.usecase.StartGameUseCase()(config)
    XoTheme {
        GameScreen(
            state = GameUiState(
                mode = GameMode.VS_COMPUTER,
                level = com.m7mdabaza.xogame.domain.model.Level.EASY,
                settings = MatchSettings(length = MatchLength.BEST_OF_3),
                game = game,
                match = com.m7mdabaza.xogame.domain.model.MatchState(MatchLength.BEST_OF_3, xWins = 1),
                showSetup = false,
                hasStarted = true,
                bannerAdUnitId = "",
            ),
            showBanner = true,
            actions = GameActions(),
        )
    }
}
