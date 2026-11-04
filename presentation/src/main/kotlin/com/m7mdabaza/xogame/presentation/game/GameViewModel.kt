package com.m7mdabaza.xogame.presentation.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.AssistAccess
import com.m7mdabaza.xogame.domain.model.AssistKind
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.usecase.ComputerMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.GetAssistAccessUseCase
import com.m7mdabaza.xogame.domain.usecase.GetHintUseCase
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordGameResultUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordInterstitialShownUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordReviewRequestedUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordRoundForAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.ShouldRequestReviewUseCase
import com.m7mdabaza.xogame.domain.usecase.ShouldShowInterstitialUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.domain.usecase.UndoMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import com.m7mdabaza.xogame.presentation.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

/**
 * One game screen for every mode and level (replaces the six legacy
 * activities). Owns the match: setup, rounds, the computer's moves, undo,
 * hints and the score. Finished rounds go into the stats, and the setup
 * choices are remembered in the settings as next time's defaults. The whole
 * session is saved in [SavedStateHandle] after every change, so it survives
 * rotation and process death.
 *
 * Ads: an interstitial may show between rounds (see
 * [ShouldShowInterstitialUseCase]); hints and undos are free or cost a
 * rewarded ad (see [GetAssistAccessUseCase]). The screen shows the ads when
 * asked through [events] and reports back.
 *
 * The navigation route supplies the `mode` and `level` arguments (enum names).
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val startGame: StartGameUseCase,
    private val playMove: PlayMoveUseCase,
    private val undoMove: UndoMoveUseCase,
    private val computerMove: ComputerMoveUseCase,
    private val getHint: GetHintUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val recordGameResult: RecordGameResultUseCase,
    private val observeAdsRemoved: ObserveAdsRemovedUseCase,
    private val getAssistAccess: GetAssistAccessUseCase,
    private val shouldShowInterstitial: ShouldShowInterstitialUseCase,
    private val recordInterstitialShown: RecordInterstitialShownUseCase,
    private val recordRoundForAds: RecordRoundForAdsUseCase,
    private val evaluateAchievements: EvaluateAchievementsUseCase,
    private val shouldRequestReview: ShouldRequestReviewUseCase,
    private val recordReviewRequested: RecordReviewRequestedUseCase,
    private val feedback: GameFeedback,
    adUnitIds: AdUnitIds,
    @param:DefaultDispatcher private val computeDispatcher: CoroutineDispatcher,
    private val random: Random,
) : ViewModel() {

    private val mode = GameMode.valueOf(checkNotNull(savedStateHandle.get<String>(ARG_MODE)) { "Missing $ARG_MODE" })
    private val level = Level.valueOf(checkNotNull(savedStateHandle.get<String>(ARG_LEVEL)) { "Missing $ARG_LEVEL" })

    /** First player of round 1 of the current match; best-of rounds alternate from it. */
    private var matchFirstPlayer = Player.X

    private var computerJob: Job? = null
    private var hintJob: Job? = null

    /** Worked out when a round ends, so "next round" can decide right away. */
    private var interstitialDue = false

    private val _events = Channel<GameEvent>(Channel.BUFFERED)
    val events: Flow<GameEvent> = _events.receiveAsFlow()

    private val _uiState: MutableStateFlow<GameUiState>
    val uiState: StateFlow<GameUiState>

    init {
        val banner = adUnitIds.gameBanner(mode, level)
        val restored = SavedSession.decode(savedStateHandle[KEY_SESSION])?.let { restore(it, banner) }
        val initial = (
            restored ?: GameUiState(
                mode = mode,
                level = level,
                settings = MatchSettings(),
                game = startGame(configFor(MatchSettings(), Player.X)),
                match = MatchState(MatchLength.SINGLE),
                showSetup = true,
                hasStarted = false,
                bannerAdUnitId = banner,
            )
            ).copy(interstitialAdUnitId = adUnitIds.interstitial(mode, level), rewardedAdUnitId = adUnitIds.rewarded)
        _uiState = MutableStateFlow(withDerived(initial))
        uiState = _uiState.asStateFlow()
        save()
        if (restored == null) {
            // Fill the setup with the choices remembered from the last match.
            viewModelScope.launch {
                val defaults = observeSettings().first().toMatchSettings()
                update { if (it.hasStarted) it else it.copy(settings = defaults) }
            }
        }
        viewModelScope.launch {
            observeAdsRemoved().collect { removed -> update { it.copy(adsRemoved = removed) } }
        }
        if (_uiState.value.phase == GamePhase.PLAYING && _uiState.value.game.isComputerTurn) startComputerTurn()
    }

    // --- Actions ---------------------------------------------------------------

    fun onCellClick(cell: Cell) {
        if (!_uiState.value.boardEnabled) return
        place(cell, byComputer = false)
    }

    /**
     * Only while a round is being played: a finished round (won, lost or
     * drawn) can't be undone. Free or after a rewarded ad (see [GameUiState.undoAccess]).
     */
    fun onUndo() {
        val state = _uiState.value
        if (!state.canUndo) return
        when (state.undoAccess) {
            AssistAccess.FREE -> {
                // In two-player mode undo is always free; only a buyer's free undo is "used".
                val usesFreeTurn = mode != GameMode.TWO_PLAYERS
                undo(markFreeUse = usesFreeTurn)
            }
            AssistAccess.REWARDED_AD -> update { it.copy(assistOffer = AssistKind.UNDO) }
        }
    }

    /** Free once per round for Remove-ads buyers, otherwise after a rewarded ad (see [GameUiState.hintAccess]). */
    fun onHint() {
        val state = _uiState.value
        if (!state.canHint) return
        when (state.hintAccess) {
            AssistAccess.FREE -> showHint(markFreeUse = true)
            AssistAccess.REWARDED_AD -> update { it.copy(assistOffer = AssistKind.HINT) }
        }
    }

    /** The player agreed to watch an ad for the offered hint/undo. */
    fun onAssistOfferAccepted() {
        val kind = _uiState.value.assistOffer ?: return
        update { it.copy(assistOffer = null) }
        _events.trySend(GameEvent.ShowRewarded(kind))
    }

    fun onAssistOfferDismissed() {
        update { it.copy(assistOffer = null) }
    }

    /** The rewarded ad closed; the assist is given only if the reward was earned. */
    fun onRewardedFinished(kind: AssistKind, rewarded: Boolean) {
        if (!rewarded) return
        when (kind) {
            AssistKind.HINT -> if (_uiState.value.canHint) showHint(markFreeUse = false)
            AssistKind.UNDO -> if (_uiState.value.canUndo) undo(markFreeUse = false)
        }
    }

    fun onRewardedUnavailable() {
        _events.trySend(GameEvent.AdNotReady)
    }

    /** Starts the current round again from an empty board, without scoring it (free uses stay used). */
    fun onRestartRound() {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYING) return
        feedback.newRound()
        startRound(state.settings, state.match, state.game.config.firstPlayer, resetFreeUses = false)
    }

    /**
     * After a round: the next round, or a new match with the same settings
     * when the match is over. An interstitial may come first.
     */
    fun onNextRound() {
        val phase = _uiState.value.phase
        if (phase != GamePhase.ROUND_OVER && phase != GamePhase.MATCH_OVER) return
        if (interstitialDue && !_uiState.value.adsRemoved) {
            interstitialDue = false
            _events.trySend(GameEvent.ShowInterstitial)
        } else {
            goToNextRound()
        }
    }

    /** The interstitial closed (or none was ready); continue to the next round. */
    fun onInterstitialFinished(shown: Boolean) {
        if (shown) viewModelScope.launch { withContext(NonCancellable) { recordInterstitialShown() } }
        goToNextRound()
    }

    fun onOpenSetup() {
        cancelJobs()
        update { it.copy(showSetup = true, computerThinking = false, hintLoading = false) }
    }

    /** Closes the setup without changes (only once a match has started; otherwise leave the screen). */
    fun onDismissSetup() {
        val state = _uiState.value
        if (!state.showSetup || !state.hasStarted) return
        update { it.copy(showSetup = false) }
        if (_uiState.value.phase == GamePhase.PLAYING && _uiState.value.game.isComputerTurn) startComputerTurn()
    }

    fun onStartMatch(settings: MatchSettings) {
        val cleaned = settings.copy(
            xName = settings.xName.trim().take(MAX_NAME_LENGTH),
            oName = settings.oName.trim().take(MAX_NAME_LENGTH),
        )
        startMatch(cleaned)
        feedback.newRound()
        // Next time the setup opens with these choices.
        viewModelScope.launch { updateSettings { it.withSetupDefaults(cleaned) } }
    }

    // --- Game flow ------------------------------------------------------------

    private fun goToNextRound() {
        val state = _uiState.value
        when (state.phase) {
            GamePhase.ROUND_OVER -> startRound(state.settings, state.match, firstPlayerOfNextRound(state))
            GamePhase.MATCH_OVER -> startMatch(state.settings)
            else -> return
        }
        feedback.newRound()
    }

    private fun startMatch(settings: MatchSettings) {
        matchFirstPlayer = settings.resolveStarter(mode, random)
        startRound(settings, MatchState(settings.length), matchFirstPlayer)
    }

    private fun startRound(settings: MatchSettings, match: MatchState, firstPlayer: Player, resetFreeUses: Boolean = true) {
        cancelJobs()
        update {
            it.copy(
                settings = settings,
                match = match,
                game = startGame(configFor(settings, firstPlayer)),
                showSetup = false,
                hasStarted = true,
                computerThinking = false,
                hintLoading = false,
                hint = null,
                assistOffer = null,
                freeHintUsed = if (resetFreeUses) false else it.freeHintUsed,
                freeUndoUsed = if (resetFreeUses) false else it.freeUndoUsed,
            )
        }
        if (_uiState.value.game.isComputerTurn) startComputerTurn()
    }

    /**
     * Single rounds keep the chosen starter (rolling again for Random). In a
     * best-of match the players take turns to start.
     */
    private fun firstPlayerOfNextRound(state: GameUiState): Player =
        if (state.match.length == MatchLength.SINGLE) {
            state.settings.resolveStarter(mode, random)
        } else if (state.match.roundsPlayed % 2 == 0) {
            matchFirstPlayer
        } else {
            matchFirstPlayer.opponent
        }

    private fun undo(markFreeUse: Boolean) {
        val undone = undoMove(_uiState.value.game) ?: return
        cancelJobs()
        update {
            it.copy(
                game = undone,
                computerThinking = false,
                hintLoading = false,
                hint = null,
                freeUndoUsed = it.freeUndoUsed || (markFreeUse && it.adsRemoved),
            )
        }
    }

    private fun showHint(markFreeUse: Boolean) {
        val game = _uiState.value.game
        update { it.copy(hintLoading = true, freeHintUsed = it.freeHintUsed || (markFreeUse && it.adsRemoved)) }
        hintJob = viewModelScope.launch {
            val cell = withContext(computeDispatcher) { getHint(game) }
            update { it.copy(hintLoading = false, hint = cell) }
        }
    }

    private fun place(cell: Cell, byComputer: Boolean) {
        val state = _uiState.value
        val played = playMove(state.game, cell) as? MoveResult.Played ?: return
        hintJob?.cancel()
        feedback.move(state.game.turn, byComputer)
        update { it.copy(game = played.state, hint = null, hintLoading = false) }
        afterMove()
    }

    private fun afterMove() {
        val state = _uiState.value
        val game = state.game
        when {
            game.isOver -> {
                update { it.copy(match = it.match.record(game.result), computerThinking = false, assistOffer = null) }
                feedback.roundEnded(soundFor(game.result, state))
                viewModelScope.launch {
                    // Finish the writes even if the user leaves the screen right away.
                    val unlocked = withContext(NonCancellable) {
                        recordGameResult(game)
                        recordRoundForAds()
                        evaluateAchievements()
                    }
                    if (unlocked.isNotEmpty()) _events.trySend(GameEvent.AchievementsUnlocked(unlocked))
                    // A happy moment: the player just beat the phone.
                    if (game.humanBeatComputer && shouldRequestReview()) {
                        recordReviewRequested()
                        _events.trySend(GameEvent.RequestReview)
                    }
                    interstitialDue = shouldShowInterstitial()
                }
            }
            game.isComputerTurn -> startComputerTurn()
        }
    }

    private fun startComputerTurn() {
        computerJob?.cancel()
        update { it.copy(computerThinking = true) }
        computerJob = viewModelScope.launch {
            delay(COMPUTER_DELAY_MS)
            val game = _uiState.value.game
            val cell = withContext(computeDispatcher) { computerMove(game) }
            update { it.copy(computerThinking = false) }
            place(cell, byComputer = true)
        }
    }

    private fun soundFor(result: GameResult, state: GameUiState): RoundSound = when (result) {
        is GameResult.Win ->
            if (mode == GameMode.VS_COMPUTER && result.player != state.settings.humanSymbol) RoundSound.LOSE else RoundSound.WIN
        else -> RoundSound.DRAW
    }

    private fun cancelJobs() {
        computerJob?.cancel()
        hintJob?.cancel()
    }

    private fun configFor(settings: MatchSettings, firstPlayer: Player) =
        GameConfig(mode, level, humanSymbol = settings.playerOne(mode), firstPlayer = firstPlayer)

    // --- State and saving -----------------------------------------------------

    private fun update(transform: (GameUiState) -> GameUiState) {
        _uiState.value = withDerived(transform(_uiState.value))
        save()
    }

    private fun withDerived(state: GameUiState): GameUiState = state.copy(
        canUndo = state.phase == GamePhase.PLAYING && undoMove(state.game) != null,
        hintAccess = getAssistAccess(AssistKind.HINT, mode, state.adsRemoved, state.freeHintUsed),
        undoAccess = getAssistAccess(AssistKind.UNDO, mode, state.adsRemoved, state.freeUndoUsed),
    )

    private fun save() {
        val state = _uiState.value
        val size = state.game.board.size
        savedStateHandle[KEY_SESSION] = SavedSession(
            humanSymbol = state.settings.humanSymbol.name,
            starter = state.settings.starter.name,
            length = state.settings.length.name,
            xName = state.settings.xName,
            oName = state.settings.oName,
            showSetup = state.showSetup,
            hasStarted = state.hasStarted,
            matchFirstPlayer = matchFirstPlayer.name,
            roundFirstPlayer = state.game.config.firstPlayer.name,
            moves = state.game.history.map { it.cell.row * size + it.cell.col },
            xWins = state.match.xWins,
            oWins = state.match.oWins,
            draws = state.match.draws,
            freeHintUsed = state.freeHintUsed,
            freeUndoUsed = state.freeUndoUsed,
        ).encode()
    }

    /** Rebuilds the screen state from [saved], or null if it doesn't fit this level any more. */
    private fun restore(saved: SavedSession, banner: String): GameUiState? = runCatching {
        val settings = MatchSettings(
            humanSymbol = Player.valueOf(saved.humanSymbol),
            starter = Starter.valueOf(saved.starter),
            length = MatchLength.valueOf(saved.length),
            xName = saved.xName,
            oName = saved.oName,
        )
        matchFirstPlayer = Player.valueOf(saved.matchFirstPlayer)
        var game: GameState = startGame(configFor(settings, Player.valueOf(saved.roundFirstPlayer)))
        for (index in saved.moves) {
            val size = game.board.size
            game = (playMove(game, Cell(index / size, index % size)) as MoveResult.Played).state
        }
        GameUiState(
            mode = mode,
            level = level,
            settings = settings,
            game = game,
            match = MatchState(settings.length, saved.xWins, saved.oWins, saved.draws),
            showSetup = saved.showSetup,
            hasStarted = saved.hasStarted,
            bannerAdUnitId = banner,
            freeHintUsed = saved.freeHintUsed,
            freeUndoUsed = saved.freeUndoUsed,
        )
    }.getOrNull()

    companion object {
        /** Navigation argument names; they must match the `Game` route's properties. */
        const val ARG_MODE = "mode"
        const val ARG_LEVEL = "level"

        internal const val KEY_SESSION = "game_session"

        /** A short pause so the computer's reply doesn't feel instant. */
        internal const val COMPUTER_DELAY_MS = 450L

        const val MAX_NAME_LENGTH = 16
    }
}
