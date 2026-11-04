package com.m7mdabaza.xogame.presentation.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.ChallengeProgress
import com.m7mdabaza.xogame.domain.model.DailyChallenge
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.usecase.ComputerMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.GetDailyChallengeUseCase
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveChallengeProgressUseCase
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordChallengeSolvedUseCase
import com.m7mdabaza.xogame.domain.usecase.StartChallengeUseCase
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import com.m7mdabaza.xogame.presentation.di.DefaultDispatcher
import com.m7mdabaza.xogame.presentation.game.GameFeedback
import com.m7mdabaza.xogame.presentation.game.RoundSound
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ChallengePhase {
    LOADING,

    /** The player's turn (first or second move). */
    PLAYING,

    /** The phone is answering the first move. */
    DEFENDING,
    SOLVED,
    FAILED,
}

enum class FailReason {
    /** The first move didn't make two threats, so the phone could block. */
    NO_FORK,

    /** The second move didn't take the remaining winning square. */
    MISSED_WIN,
}

data class ChallengeUiState(
    val phase: ChallengePhase = ChallengePhase.LOADING,
    val challenge: DailyChallenge? = null,
    val game: GameState? = null,
    /** The player's moves so far in this attempt (0, 1 or 2). */
    val movesPlayed: Int = 0,
    val failReason: FailReason? = null,
    val progress: ChallengeProgress = ChallengeProgress(emptySet(), today = 0),
    val bannerAdUnitId: String,
    val adsRemoved: Boolean = false,
) {
    val boardEnabled: Boolean get() = phase == ChallengePhase.PLAYING
}

/**
 * Today's "win in 2" puzzle. A first move that isn't a solution fails at once
 * (the phone could simply block it); after a solution the phone blocks one of
 * the two threats and the player has to take the other. Retries are
 * unlimited; the first solve of the day counts for the streak.
 */
@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val getDailyChallenge: GetDailyChallengeUseCase,
    private val startChallenge: StartChallengeUseCase,
    private val playMove: PlayMoveUseCase,
    private val computerMove: ComputerMoveUseCase,
    private val recordSolved: RecordChallengeSolvedUseCase,
    private val evaluateAchievements: EvaluateAchievementsUseCase,
    observeProgress: ObserveChallengeProgressUseCase,
    observeAdsRemoved: ObserveAdsRemovedUseCase,
    private val feedback: GameFeedback,
    adUnitIds: AdUnitIds,
    @param:DefaultDispatcher private val computeDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState(bannerAdUnitId = adUnitIds.homeBanner))
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val _unlocked = Channel<List<Achievement>>(Channel.BUFFERED)

    /** Achievements unlocked by solving, to announce once. */
    val unlockedAchievements: Flow<List<Achievement>> = _unlocked.receiveAsFlow()

    private var defenderJob: Job? = null

    init {
        viewModelScope.launch {
            val challenge = withContext(computeDispatcher) { getDailyChallenge() }
            val start = startChallenge(challenge)
            _uiState.update { it.copy(phase = ChallengePhase.PLAYING, challenge = challenge, game = start) }
        }
        viewModelScope.launch { observeProgress().collect { p -> _uiState.update { it.copy(progress = p) } } }
        viewModelScope.launch { observeAdsRemoved().collect { r -> _uiState.update { it.copy(adsRemoved = r) } } }
    }

    fun onCellClick(cell: Cell) {
        val state = _uiState.value
        val challenge = state.challenge ?: return
        val game = state.game ?: return
        if (!state.boardEnabled) return
        val played = (playMove(game, cell) as? MoveResult.Played)?.state ?: return
        feedback.move(game.turn, byComputer = false)
        val moves = state.movesPlayed + 1
        when {
            (played.result as? GameResult.Win)?.player == challenge.player -> solved(played, challenge)
            moves == 1 && cell !in challenge.solutions -> failed(played, FailReason.NO_FORK)
            moves >= challenge.movesToWin -> failed(played, FailReason.MISSED_WIN)
            else -> defend(played, moves)
        }
    }

    /** Back to the start position for another attempt. */
    fun onRetry() {
        val challenge = _uiState.value.challenge ?: return
        defenderJob?.cancel()
        feedback.newRound()
        _uiState.update {
            it.copy(phase = ChallengePhase.PLAYING, game = startChallenge(challenge), movesPlayed = 0, failReason = null)
        }
    }

    private fun defend(game: GameState, moves: Int) {
        _uiState.update { it.copy(phase = ChallengePhase.DEFENDING, game = game, movesPlayed = moves) }
        defenderJob = viewModelScope.launch {
            delay(DEFENDER_DELAY_MS)
            val reply = withContext(computeDispatcher) { computerMove(game) }
            val answered = (playMove(game, reply) as MoveResult.Played).state
            feedback.move(game.turn, byComputer = true)
            _uiState.update { it.copy(phase = ChallengePhase.PLAYING, game = answered) }
        }
    }

    private fun solved(game: GameState, challenge: DailyChallenge) {
        _uiState.update { it.copy(phase = ChallengePhase.SOLVED, game = game, movesPlayed = it.movesPlayed + 1) }
        feedback.roundEnded(RoundSound.WIN)
        viewModelScope.launch {
            val unlocked = withContext(NonCancellable) {
                recordSolved(challenge.epochDay)
                evaluateAchievements()
            }
            if (unlocked.isNotEmpty()) _unlocked.send(unlocked)
        }
    }

    private fun failed(game: GameState, reason: FailReason) {
        _uiState.update { it.copy(phase = ChallengePhase.FAILED, game = game, movesPlayed = it.movesPlayed + 1, failReason = reason) }
        feedback.roundEnded(RoundSound.LOSE)
    }

    companion object {
        internal const val DEFENDER_DELAY_MS = 500L
    }
}
