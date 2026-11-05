package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import javax.inject.Inject

/**
 * The rules of a two-phone match, as a pure state machine that runs on both
 * phones. Each call takes the current [TwoPhoneState] and returns a
 * [TwoPhoneStep]: the new state, the messages to send to the friend, and
 * events (sounds, stats). Both phones apply the same messages in the same
 * order, so they stay on the same board.
 *
 * The flow:
 * - Both phones say [PeerMessage.Hello]; versions must match. The host also
 *   sends the [PeerMessage.Setup] and starts when the guest is [PeerMessage.Ready].
 *   The host plays X.
 * - Players take turns placing marks ([PeerMessage.Move]). Both phones check
 *   every move with the shared game rules.
 * - After a round both tap "Next round" ([PeerMessage.Continue]); the next
 *   round is started by the player who didn't start this one. After a best-of
 *   match the same gives a rematch.
 * - Either player may ask to restart the round or start a new match
 *   ([PeerMessage.Request]); the friend answers. While a request waits, its
 *   sender can't move or continue and the friend must answer before
 *   continuing, so both phones apply it to the same board. If both ask at the
 *   same moment, the host's request wins and the guest's is dropped. A
 *   restart request lapses when the round ends first.
 * - Anything that breaks these rules ends the game as [EndReason.OUT_OF_SYNC].
 *
 * Actions that aren't allowed right now (see [TwoPhoneState.canPlay] etc.)
 * change nothing.
 */
class TwoPhoneRules @Inject constructor(
    private val startGame: StartGameUseCase,
    private val playMove: PlayMoveUseCase,
) {

    /** Opens the match on this phone: the host passes the [setup] it chose, the guest gets it from the host. */
    fun start(role: PeerRole, myName: String, setup: TwoPhoneSetup?): TwoPhoneStep {
        require((role == PeerRole.HOST) == (setup != null)) { "Only the host chooses the setup" }
        val name = cleanName(myName)
        val state = TwoPhoneState(role = role, myName = name, setup = setup)
        val hello = PeerMessage.Hello(PROTOCOL_VERSION, name)
        return TwoPhoneStep(state, if (setup != null) listOf(hello, PeerMessage.Setup(setup)) else listOf(hello))
    }

    // --- This player's actions ------------------------------------------------------------

    fun play(state: TwoPhoneState, cell: Cell): TwoPhoneStep {
        if (!state.canPlay) return TwoPhoneStep(state)
        val game = checkNotNull(state.game)
        val played = playMove(game, cell) as? MoveResult.Played ?: return TwoPhoneStep(state)
        val message = PeerMessage.Move(state.stage, game.history.size, cell)
        return afterMove(state, played.state, byFriend = false).let { it.copy(send = listOf(message) + it.send) }
    }

    /** "Next round" after a round, "Rematch" after a match. */
    fun continueAfterRound(state: TwoPhoneState): TwoPhoneStep {
        if (!state.canContinue) return TwoPhoneStep(state)
        val message = PeerMessage.Continue(state.stage)
        val continued = state.copy(iContinued = true)
        if (!continued.friendContinued) return TwoPhoneStep(continued, listOf(message))
        return nextBoard(continued).let { it.copy(send = listOf(message) + it.send) }
    }

    fun request(state: TwoPhoneState, kind: RequestKind): TwoPhoneStep {
        if (!state.canRequest(kind)) return TwoPhoneStep(state)
        return TwoPhoneStep(state.copy(myRequest = kind), listOf(PeerMessage.Request(state.stage, kind)))
    }

    /** Answers the friend's request. */
    fun answer(state: TwoPhoneState, accept: Boolean): TwoPhoneStep {
        val kind = state.friendRequest ?: return TwoPhoneStep(state)
        val message = PeerMessage.Answer(state.stage, accept)
        if (!accept) return TwoPhoneStep(state.copy(friendRequest = null), listOf(message))
        return grant(state, kind).let { it.copy(send = listOf(message) + it.send) }
    }

    fun leave(state: TwoPhoneState): TwoPhoneStep {
        if (state.isEnded) return TwoPhoneStep(state)
        return TwoPhoneStep(end(state, EndReason.YOU_LEFT), listOf(PeerMessage.Leave))
    }

    /** The connection ended without a goodbye. */
    fun connectionLost(state: TwoPhoneState): TwoPhoneStep {
        if (state.isEnded) return TwoPhoneStep(state)
        return TwoPhoneStep(end(state, EndReason.CONNECTION_LOST))
    }

    // --- The friend's messages ------------------------------------------------------------

    fun receive(state: TwoPhoneState, message: PeerMessage): TwoPhoneStep {
        if (state.isEnded) return TwoPhoneStep(state)
        if (message == PeerMessage.Leave) return TwoPhoneStep(end(state, EndReason.FRIEND_LEFT))
        if (message is PeerMessage.Hello) return receiveHello(state, message)
        // Nothing but the hello may come before it.
        if (state.friendName == null) return outOfSync(state)
        return when (message) {
            is PeerMessage.Setup -> receiveSetup(state, message)
            PeerMessage.Ready -> receiveReady(state)
            is PeerMessage.Move -> receiveMove(state, message)
            is PeerMessage.Continue -> receiveContinue(state, message)
            is PeerMessage.Request -> receiveRequest(state, message)
            is PeerMessage.Answer -> receiveAnswer(state, message)
            is PeerMessage.Hello, PeerMessage.Leave -> error("Handled above")
        }
    }

    private fun receiveHello(state: TwoPhoneState, hello: PeerMessage.Hello): TwoPhoneStep = when {
        state.friendName != null -> outOfSync(state)
        hello.protocolVersion > PROTOCOL_VERSION -> TwoPhoneStep(end(state, EndReason.UPDATE_THIS_APP))
        hello.protocolVersion < PROTOCOL_VERSION -> TwoPhoneStep(end(state, EndReason.FRIEND_MUST_UPDATE))
        else -> TwoPhoneStep(state.copy(friendName = cleanName(hello.name)))
    }

    private fun receiveSetup(state: TwoPhoneState, message: PeerMessage.Setup): TwoPhoneStep {
        if (state.role != PeerRole.GUEST || state.phase != TwoPhonePhase.CONNECTING || state.setup != null) return outOfSync(state)
        val started = newMatch(state, message.setup, nextStage = 0)
        return started.copy(send = listOf(PeerMessage.Ready))
    }

    private fun receiveReady(state: TwoPhoneState): TwoPhoneStep {
        if (state.role != PeerRole.HOST || state.phase != TwoPhonePhase.CONNECTING) return outOfSync(state)
        return newMatch(state, checkNotNull(state.setup), nextStage = 0)
    }

    private fun receiveMove(state: TwoPhoneState, move: PeerMessage.Move): TwoPhoneStep {
        val game = state.game
        // The friend can't move while their own request waits.
        if (state.phase != TwoPhonePhase.PLAYING || game == null || move.stage != state.stage ||
            move.index != game.history.size || game.turn != state.friendSymbol || state.friendRequest != null
        ) {
            return outOfSync(state)
        }
        val played = playMove(game, move.cell) as? MoveResult.Played ?: return outOfSync(state)
        return afterMove(state, played.state, byFriend = true)
    }

    private fun receiveContinue(state: TwoPhoneState, message: PeerMessage.Continue): TwoPhoneStep {
        // The friend can't continue twice or while their own request waits. (They may have continued
        // before this player's request reached them.)
        if (!state.isBetweenRounds || message.stage != state.stage || state.friendContinued || state.friendRequest != null) {
            return outOfSync(state)
        }
        val continued = state.copy(friendContinued = true)
        return if (continued.iContinued) nextBoard(continued) else TwoPhoneStep(continued)
    }

    private fun receiveRequest(state: TwoPhoneState, message: PeerMessage.Request): TwoPhoneStep {
        val kind = message.kind
        if (message.stage != state.stage || state.friendRequest != null || state.friendContinued ||
            !(state.phase == TwoPhonePhase.PLAYING || state.isBetweenRounds)
        ) {
            return outOfSync(state)
        }
        // A restart that crossed the round's last move: the round is over, so it lapses on both phones.
        if (kind == RequestKind.RestartRound && state.phase != TwoPhonePhase.PLAYING) return TwoPhoneStep(state)
        if (state.myRequest != null) {
            // Both asked at once: the host's request wins, the guest's is dropped on both phones.
            return if (state.role == PeerRole.HOST) TwoPhoneStep(state) else TwoPhoneStep(state.copy(myRequest = null, friendRequest = kind))
        }
        return TwoPhoneStep(state.copy(friendRequest = kind))
    }

    private fun receiveAnswer(state: TwoPhoneState, message: PeerMessage.Answer): TwoPhoneStep {
        val kind = state.myRequest
        if (kind == null || message.stage != state.stage) return outOfSync(state)
        if (!message.accepted) return TwoPhoneStep(state.copy(myRequest = null), events = listOf(TwoPhoneEvent.RequestDeclined(kind)))
        return grant(state, kind)
    }

    // --- Changes --------------------------------------------------------------------------

    private fun afterMove(state: TwoPhoneState, game: GameState, byFriend: Boolean): TwoPhoneStep {
        val moved = TwoPhoneEvent.MovePlayed(checkNotNull(game.lastMove).player, byFriend)
        if (!game.isOver) return TwoPhoneStep(state.copy(game = game), events = listOf(moved))
        val match = checkNotNull(state.match).record(game.result)
        val finished = state.copy(
            game = game,
            match = match,
            phase = if (match.isOver) TwoPhonePhase.MATCH_OVER else TwoPhonePhase.ROUND_OVER,
            // A restart asked for this round lapses with it (on both phones, at the same move).
            myRequest = state.myRequest.takeUnless { it == RequestKind.RestartRound },
            friendRequest = state.friendRequest.takeUnless { it == RequestKind.RestartRound },
        )
        return TwoPhoneStep(finished, events = listOf(moved, TwoPhoneEvent.RoundFinished(game)))
    }

    /** Both continued: the next round, or a rematch with the same setup after a match. */
    private fun nextBoard(state: TwoPhoneState): TwoPhoneStep {
        val starter = checkNotNull(state.game).config.firstPlayer.opponent
        val setup = checkNotNull(state.setup)
        val match = if (state.phase == TwoPhonePhase.MATCH_OVER) MatchState(setup.matchLength) else checkNotNull(state.match)
        return newBoard(state, setup, match, starter, state.stage + 1)
    }

    /** Applies an accepted request, on the phone that asked and on the one that answered. */
    private fun grant(state: TwoPhoneState, kind: RequestKind): TwoPhoneStep = when (kind) {
        RequestKind.RestartRound ->
            newBoard(state, checkNotNull(state.setup), checkNotNull(state.match), checkNotNull(state.game).config.firstPlayer, state.stage + 1)
        is RequestKind.NewMatch -> newMatch(state, kind.setup, state.stage + 1)
    }

    private fun newMatch(state: TwoPhoneState, setup: TwoPhoneSetup, nextStage: Int): TwoPhoneStep =
        newBoard(state, setup, MatchState(setup.matchLength), setup.firstPlayer, nextStage)

    private fun newBoard(state: TwoPhoneState, setup: TwoPhoneSetup, match: MatchState, firstPlayer: Player, nextStage: Int): TwoPhoneStep {
        val config = GameConfig(GameMode.TWO_PHONES, setup.boardSize, level = null, humanSymbol = state.mySymbol, firstPlayer = firstPlayer)
        val reset = state.copy(
            phase = TwoPhonePhase.PLAYING,
            setup = setup,
            stage = nextStage,
            game = startGame(config),
            match = match,
            iContinued = false,
            friendContinued = false,
            myRequest = null,
            friendRequest = null,
        )
        return TwoPhoneStep(reset, events = listOf(TwoPhoneEvent.NewBoard))
    }

    private fun end(state: TwoPhoneState, reason: EndReason) =
        state.copy(phase = TwoPhonePhase.ENDED, endReason = reason, myRequest = null, friendRequest = null)

    private fun outOfSync(state: TwoPhoneState) = TwoPhoneStep(end(state, EndReason.OUT_OF_SYNC))

    private fun cleanName(name: String) = name.trim().take(MAX_PEER_NAME_LENGTH)
}
