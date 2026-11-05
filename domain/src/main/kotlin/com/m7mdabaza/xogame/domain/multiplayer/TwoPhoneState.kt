package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Player

enum class TwoPhonePhase {
    /** Saying hello and agreeing on the match. */
    CONNECTING,
    PLAYING,

    /** A round finished; the next one starts once both players continue. */
    ROUND_OVER,

    /** A best-of match finished; a rematch starts once both players continue. */
    MATCH_OVER,

    /** The game is over for good; the connection closes. See [TwoPhoneState.endReason]. */
    ENDED,
}

enum class EndReason {
    YOU_LEFT,
    FRIEND_LEFT,

    /** Bluetooth went off, the friend went out of range, closed the app, ... */
    CONNECTION_LOST,

    /** The friend has a newer version of the app: update this one. */
    UPDATE_THIS_APP,

    /** The friend has an older version of the app: they need to update. */
    FRIEND_MUST_UPDATE,

    /** The phones disagreed about the game (a bug or a damaged message), so it can't go on. */
    OUT_OF_SYNC,
}

/**
 * One phone's view of a two-phone match. Created and changed only by
 * [TwoPhoneRules]; the same messages lead both phones to the same match.
 *
 * @property friendName the friend's name, once their hello arrived (may be blank).
 * @property stage raised by both phones on every board reset (see [PeerMessage]).
 * @property iContinued this player tapped "Next round" / "Rematch" and waits for the friend.
 * @property myRequest this player's restart / new-match request, waiting for the friend's answer.
 * @property friendRequest the friend's request, waiting for this player's answer.
 */
@ConsistentCopyVisibility
data class TwoPhoneState internal constructor(
    val role: PeerRole,
    val myName: String,
    val friendName: String? = null,
    val phase: TwoPhonePhase = TwoPhonePhase.CONNECTING,
    val setup: TwoPhoneSetup? = null,
    val stage: Int = 0,
    val game: GameState? = null,
    val match: MatchState? = null,
    val iContinued: Boolean = false,
    val friendContinued: Boolean = false,
    val myRequest: RequestKind? = null,
    val friendRequest: RequestKind? = null,
    val endReason: EndReason? = null,
) {
    val mySymbol: Player get() = role.symbol

    val friendSymbol: Player get() = role.symbol.opponent

    /** A round (or match) result is on the board. */
    val isBetweenRounds: Boolean get() = phase == TwoPhonePhase.ROUND_OVER || phase == TwoPhonePhase.MATCH_OVER

    /** It is this player's turn (they may still be blocked by their own request, see [canPlay]). */
    val isMyTurn: Boolean get() = phase == TwoPhonePhase.PLAYING && game?.turn == mySymbol

    /** This player may place a mark now. Not while their own request waits: the friend answers for the board as it is. */
    val canPlay: Boolean get() = isMyTurn && myRequest == null

    /** This player may tap "Next round" / "Rematch": not twice, and not while a request waits for an answer. */
    val canContinue: Boolean get() = isBetweenRounds && !iContinued && myRequest == null && friendRequest == null

    /**
     * This player may ask for [kind]: one request at a time, not after
     * continuing, and a restart only while a round is played.
     */
    fun canRequest(kind: RequestKind): Boolean =
        (phase == TwoPhonePhase.PLAYING || isBetweenRounds) &&
            myRequest == null && friendRequest == null && !iContinued &&
            (kind != RequestKind.RestartRound || phase == TwoPhonePhase.PLAYING)

    /** The friend left, the connection ended or the phones can't play together. */
    val isEnded: Boolean get() = phase == TwoPhonePhase.ENDED

    /** The current round's result from this player's side, or null while it runs. */
    val iWonRound: Boolean? get() = when (val result = game?.result) {
        is GameResult.Win -> result.player == mySymbol
        else -> null
    }
}

/** What a phone should do after a change, besides showing the new state. */
sealed interface TwoPhoneEvent {
    /** A mark was placed (for the sound). */
    data class MovePlayed(val player: Player, val byFriend: Boolean) : TwoPhoneEvent

    /** A round finished: record it in the stats (from this phone's side) and play the result sound. */
    data class RoundFinished(val game: GameState) : TwoPhoneEvent

    /** The board was reset: the match started, next round, restart, new match or rematch. */
    data object NewBoard : TwoPhoneEvent

    /** The friend said no to this player's request. */
    data class RequestDeclined(val kind: RequestKind) : TwoPhoneEvent
}

/** The result of a [TwoPhoneRules] call: the new state, the messages to send (in order) and the events. */
data class TwoPhoneStep(
    val state: TwoPhoneState,
    val send: List<PeerMessage> = emptyList(),
    val events: List<TwoPhoneEvent> = emptyList(),
)
