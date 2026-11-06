package com.m7mdabaza.xogame.presentation.twophones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.PeerLobby
import com.m7mdabaza.xogame.domain.multiplayer.RequestKind
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneEvent
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneRules
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSession
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneState
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordGameResultUseCase
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import com.m7mdabaza.xogame.presentation.game.GameFeedback
import com.m7mdabaza.xogame.presentation.game.GameUiState
import com.m7mdabaza.xogame.presentation.game.MatchSettings
import com.m7mdabaza.xogame.presentation.game.RoundSound
import com.m7mdabaza.xogame.presentation.game.TwoPhonesInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

/**
 * @property match the live match; null without a connection (the app was restarted, so it's gone).
 * @property game the board, score and names as the shared game screen shows them, once the match started.
 */
data class TwoPhonesUiState(
    val match: TwoPhoneState? = null,
    val game: GameUiState? = null,
) {
    val connectionMissing: Boolean get() = match == null

    /** Nothing more can happen: the game ended, or there is no connection. */
    val isEnded: Boolean get() = match?.isEnded ?: true
}

/** One-off things the two-phone game screen shows. */
sealed interface TwoPhonesGameEvent {
    /** The friend said no to this player's restart or new match. */
    data class RequestDeclined(val kind: RequestKind) : TwoPhonesGameEvent

    data class AchievementsUnlocked(val achievements: List<Achievement>) : TwoPhonesGameEvent
}

/**
 * A two-phone game over the connection the lobby made: the match runs in a
 * [TwoPhoneSession] (the same rules on both phones), this phone's results go
 * into the stats, and it plays the sounds. A banner (until Remove ads), but
 * no interstitials: an ad on one phone would keep the friend waiting. No
 * undo or hints. Without a connection (the app was restarted)
 * [TwoPhonesUiState.match] stays null.
 */
@HiltViewModel
class TwoPhonesGameViewModel @Inject constructor(
    lobby: PeerLobby,
    rules: TwoPhoneRules,
    private val recordGameResult: RecordGameResultUseCase,
    private val evaluateAchievements: EvaluateAchievementsUseCase,
    private val feedback: GameFeedback,
    private val random: Random,
    adUnitIds: AdUnitIds,
    observeAdsRemoved: ObserveAdsRemovedUseCase,
) : ViewModel() {

    private val session: TwoPhoneSession? = (lobby.state.value as? LobbyState.Connected)?.peer?.let { peer ->
        TwoPhoneSession(peer.channel, rules, viewModelScope, peer.role, peer.myName, peer.setup)
    }

    private val banner = adUnitIds.twoPhonesBanner

    val uiState: StateFlow<TwoPhonesUiState> = session?.state
        ?.combine(observeAdsRemoved()) { match, adsRemoved -> match.toUiState(banner, adsRemoved) }
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, session.state.value.toUiState(banner, adsRemoved = false))
        ?: MutableStateFlow(TwoPhonesUiState()).asStateFlow()

    private val _events = Channel<TwoPhonesGameEvent>(Channel.BUFFERED)
    val events: Flow<TwoPhonesGameEvent> = _events.receiveAsFlow()

    init {
        // One at a time, in order: a round's result is saved before the next one's.
        session?.let { viewModelScope.launch { it.events.collect(::onSessionEvent) } }
    }

    fun onCellClick(cell: Cell) {
        session?.play(cell)
    }

    /** "Next round" / "Rematch"; the board resets once the friend taps it too. */
    fun onContinue() {
        session?.continueAfterRound()
    }

    /** Asks the friend to restart the round. */
    fun onRestartRound() {
        session?.request(RequestKind.RestartRound)
    }

    /** Asks the friend for a new match. [starter] is [Starter.PLAYER_ONE] for this player, [Starter.PLAYER_TWO] for the friend. */
    fun onRequestNewMatch(boardSize: BoardSize, matchLength: MatchLength, starter: Starter) {
        val state = session?.state?.value ?: return
        val firstPlayer = when (starter) {
            Starter.PLAYER_ONE -> state.mySymbol
            Starter.PLAYER_TWO -> state.friendSymbol
            Starter.RANDOM -> if (random.nextBoolean()) Player.X else Player.O
        }
        session.request(RequestKind.NewMatch(TwoPhoneSetup(boardSize, matchLength, firstPlayer)))
    }

    /** Answers the friend's restart / new-match request. */
    fun onAnswer(accept: Boolean) {
        session?.answer(accept)
    }

    /** Says goodbye to the friend (after the screen asked "Leave the game?"). */
    fun onLeave() {
        session?.leave()
    }

    override fun onCleared() {
        // Gone without a goodbye (e.g. the task was removed): the friend sees the connection lost.
        session?.close()
    }

    private suspend fun onSessionEvent(event: TwoPhoneEvent) {
        when (event) {
            // The friend's moves make a sound but don't vibrate, like the computer's.
            is TwoPhoneEvent.MovePlayed -> feedback.move(event.player, byComputer = event.byFriend)
            TwoPhoneEvent.NewBoard -> feedback.newRound()
            is TwoPhoneEvent.RequestDeclined -> _events.trySend(TwoPhonesGameEvent.RequestDeclined(event.kind))
            is TwoPhoneEvent.RoundFinished -> {
                feedback.roundEnded(soundFor(event.game))
                // Finish the writes even if the player leaves the screen right away.
                val unlocked = withContext(NonCancellable) {
                    recordGameResult(event.game)
                    evaluateAchievements()
                }
                if (unlocked.isNotEmpty()) _events.trySend(TwoPhonesGameEvent.AchievementsUnlocked(unlocked))
            }
        }
    }

    private fun soundFor(game: GameState): RoundSound = when (val result = game.result) {
        is GameResult.Win -> if (result.player == game.config.humanSymbol) RoundSound.WIN else RoundSound.LOSE
        else -> RoundSound.DRAW
    }
}

/**
 * This phone's view of the match, as the screen shows it; the board appears
 * once the match started. [bannerAdUnitId] shows until [adsRemoved].
 */
internal fun TwoPhoneState.toUiState(bannerAdUnitId: String = "", adsRemoved: Boolean = false): TwoPhonesUiState {
    val setup = setup
    val game = game
    val match = match
    if (setup == null || game == null || match == null) return TwoPhonesUiState(this)
    val friend = friendName.orEmpty()
    val gameUi = GameUiState(
        mode = GameMode.TWO_PHONES,
        boardSize = game.config.boardSize,
        level = null,
        settings = MatchSettings(
            humanSymbol = mySymbol,
            length = match.length,
            xName = if (mySymbol == Player.X) myName else friend,
            oName = if (mySymbol == Player.O) myName else friend,
        ),
        game = game,
        match = match,
        showSetup = false,
        hasStarted = true,
        bannerAdUnitId = bannerAdUnitId,
        adsRemoved = adsRemoved,
        twoPhones = TwoPhonesInfo(
            canPlay = canPlay,
            waitingForAnswer = myRequest != null,
            waitingForFriend = iContinued,
            friendReady = friendContinued && !iContinued,
            canContinue = canContinue,
            canRestart = canRequest(RequestKind.RestartRound),
            canNewMatch = canRequest(RequestKind.NewMatch(setup)),
            ended = isEnded,
        ),
    )
    return TwoPhonesUiState(this, gameUi)
}
