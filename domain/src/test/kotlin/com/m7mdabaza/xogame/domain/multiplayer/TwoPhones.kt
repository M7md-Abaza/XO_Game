package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

val twoPhoneRules = TwoPhoneRules(StartGameUseCase(), PlayMoveUseCase(WinChecker()))

enum class Side { HOST, GUEST }

/**
 * Two phones running [TwoPhoneRules]. The messages in flight between them
 * wait in a queue per direction until the test delivers them, so tests
 * choose the interleaving (messages in one direction keep their order, as
 * on a real connection).
 */
class TwoPhones(
    val setup: TwoPhoneSetup = TwoPhoneSetup(BoardSize.THREE, MatchLength.BEST_OF_3, Player.X),
    hostName: String = "Ahmed",
    guestName: String = "Sara",
    private val rules: TwoPhoneRules = twoPhoneRules,
) {
    lateinit var host: TwoPhoneState
    lateinit var guest: TwoPhoneState
    val toHost = ArrayDeque<PeerMessage>()
    val toGuest = ArrayDeque<PeerMessage>()
    val hostEvents = mutableListOf<TwoPhoneEvent>()
    val guestEvents = mutableListOf<TwoPhoneEvent>()

    /** Every message sent so far, by sender. */
    val sentByHost = mutableListOf<PeerMessage>()
    val sentByGuest = mutableListOf<PeerMessage>()

    init {
        apply(Side.HOST, rules.start(PeerRole.HOST, hostName, setup))
        apply(Side.GUEST, rules.start(PeerRole.GUEST, guestName, null))
    }

    fun state(side: Side) = if (side == Side.HOST) host else guest

    fun events(side: Side) = if (side == Side.HOST) hostEvents else guestEvents

    /** This side's player does something. */
    fun act(side: Side, action: TwoPhoneRules.(TwoPhoneState) -> TwoPhoneStep) = apply(side, rules.action(state(side)))

    fun play(side: Side, row: Int, col: Int) = act(side) { play(it, Cell(row, col)) }

    fun continueAfterRound(side: Side) = act(side) { continueAfterRound(it) }

    fun request(side: Side, kind: RequestKind) = act(side) { request(it, kind) }

    fun answer(side: Side, accept: Boolean) = act(side) { answer(it, accept) }

    /** Guest requests dropped because the host asked at the same moment. */
    var droppedGuestRequests = 0
        private set

    /** Restart requests that lapsed because the round ended first (counted on the asking phone). */
    var lapsedRestarts = 0
        private set

    /** Delivers up to [count] waiting messages to [to], oldest first. */
    fun deliver(to: Side, count: Int = Int.MAX_VALUE) {
        val inbox = if (to == Side.HOST) toHost else toGuest
        repeat(minOf(count, inbox.size)) {
            val message = inbox.removeFirst()
            val before = state(to)
            apply(to, rules.receive(before, message))
            val after = state(to)
            if (before.myRequest != null && after.myRequest == null && !after.isEnded) {
                when (message) {
                    is PeerMessage.Request -> droppedGuestRequests++
                    is PeerMessage.Move -> lapsedRestarts++
                    else -> Unit
                }
            }
        }
    }

    /** Delivers everything, alternating directions, until nothing is in flight. */
    fun settle() {
        while (toHost.isNotEmpty() || toGuest.isNotEmpty()) {
            deliver(Side.HOST, 1)
            deliver(Side.GUEST, 1)
        }
    }

    /** Plays [cells] alternately from whoever's turn it is, delivering each move at once. */
    fun playRound(vararg cells: Cell) {
        for (cell in cells) {
            val side = if (host.isMyTurn) Side.HOST else Side.GUEST
            act(side) { play(it, cell) }
            settle()
        }
    }

    /** This side's connection drops. */
    fun lose(side: Side) = apply(side, rules.connectionLost(state(side)))

    private fun apply(side: Side, step: TwoPhoneStep) {
        if (side == Side.HOST) host = step.state else guest = step.state
        events(side) += step.events
        if (side == Side.HOST) {
            sentByHost += step.send
            toGuest += step.send
        } else {
            sentByGuest += step.send
            toHost += step.send
        }
    }

    /** With nothing in flight, both phones agree on everything that matters. */
    fun assertInSync() {
        check(toHost.isEmpty() && toGuest.isEmpty()) { "Messages still in flight" }
        assertFalse("host ended: ${host.endReason}", host.isEnded)
        assertFalse("guest ended: ${guest.endReason}", guest.isEnded)
        assertEquals("phase", host.phase, guest.phase)
        assertEquals("stage", host.stage, guest.stage)
        assertEquals("setup", host.setup, guest.setup)
        assertEquals("match", host.match, guest.match)
        assertEquals("board", host.game?.board, guest.game?.board)
        assertEquals("moves", host.game?.history, guest.game?.history)
        assertEquals("result", host.game?.result, guest.game?.result)
        assertEquals("first player", host.game?.config?.firstPlayer, guest.game?.config?.firstPlayer)
        assertEquals("host's request", host.myRequest, guest.friendRequest)
        assertEquals("guest's request", guest.myRequest, host.friendRequest)
        assertEquals("host continued", host.iContinued, guest.friendContinued)
        assertEquals("guest continued", guest.iContinued, host.friendContinued)
    }
}

/** The first mover wins the top row on 3×3: 0,0 / 1,0 / 0,1 / 1,1 / 0,2. */
val firstMoverWins = arrayOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(0, 2))

/** A 3×3 draw from the first mover: X O X / X O O / O X X. */
val drawRound = arrayOf(
    Cell(0, 0), Cell(0, 1), Cell(0, 2),
    Cell(1, 1), Cell(1, 0), Cell(1, 2),
    Cell(2, 1), Cell(2, 0), Cell(2, 2),
)
