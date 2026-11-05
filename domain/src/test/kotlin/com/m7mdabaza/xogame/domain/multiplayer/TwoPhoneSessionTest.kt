package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One end of an in-memory connection; messages go to [other] in order. */
private class MemoryChannel : PeerChannel {
    lateinit var other: MemoryChannel
    private val inbox = Channel<PeerEvent>(Channel.UNLIMITED)
    var closed = false
        private set
    val sent = mutableListOf<PeerMessage>()

    override val events: Flow<PeerEvent> = inbox.receiveAsFlow()

    override suspend fun send(message: PeerMessage) {
        if (closed) return
        sent += message
        other.inbox.send(PeerEvent.Received(message))
    }

    override fun close() {
        if (closed) return
        closed = true
        inbox.close()
        other.inbox.trySend(PeerEvent.Disconnected)
    }

    companion object {
        fun pair(): Pair<MemoryChannel, MemoryChannel> {
            val a = MemoryChannel()
            val b = MemoryChannel()
            a.other = b
            b.other = a
            return a to b
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TwoPhoneSessionTest {

    private val setup = TwoPhoneSetup(BoardSize.THREE, MatchLength.BEST_OF_3, Player.X)

    private class Phones(val host: TwoPhoneSession, val guest: TwoPhoneSession, val hostChannel: MemoryChannel, val guestChannel: MemoryChannel)

    private fun TestScope.connect(scope: CoroutineScope = backgroundScope): Phones {
        val (hostChannel, guestChannel) = MemoryChannel.pair()
        val host = TwoPhoneSession(hostChannel, twoPhoneRules, scope, PeerRole.HOST, "Ahmed", setup)
        val guest = TwoPhoneSession(guestChannel, twoPhoneRules, scope, PeerRole.GUEST, "Sara", null)
        runCurrent()
        return Phones(host, guest, hostChannel, guestChannel)
    }

    @Test
    fun `two sessions connect and play a round together`() = runTest {
        val phones = connect()
        val hostEvents = mutableListOf<TwoPhoneEvent>()
        val guestEvents = mutableListOf<TwoPhoneEvent>()
        backgroundScope.launch { phones.host.events.collect { hostEvents += it } }
        backgroundScope.launch { phones.guest.events.collect { guestEvents += it } }
        assertEquals(TwoPhonePhase.PLAYING, phones.host.state.value.phase)
        assertEquals("Sara", phones.host.state.value.friendName)

        for (cell in listOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(0, 2))) {
            val mover = if (phones.host.state.value.isMyTurn) phones.host else phones.guest
            mover.play(cell)
            runCurrent()
        }

        assertEquals(TwoPhonePhase.ROUND_OVER, phones.guest.state.value.phase)
        assertEquals(MatchState(MatchLength.BEST_OF_3, xWins = 1), phones.guest.state.value.match)
        assertEquals(phones.host.state.value.game?.board, phones.guest.state.value.game?.board)
        assertEquals(1, hostEvents.filterIsInstance<TwoPhoneEvent.RoundFinished>().size)
        assertEquals(1, guestEvents.filterIsInstance<TwoPhoneEvent.RoundFinished>().size)

        phones.host.continueAfterRound()
        phones.guest.continueAfterRound()
        runCurrent()
        assertEquals(1, phones.guest.state.value.stage)
        assertEquals(Player.O, phones.host.state.value.game?.config?.firstPlayer)
    }

    @Test
    fun `requests go back and forth`() = runTest {
        val phones = connect()
        phones.guest.request(RequestKind.RestartRound)
        runCurrent()
        assertEquals(RequestKind.RestartRound, phones.host.state.value.friendRequest)
        phones.host.answer(accept = true)
        runCurrent()
        assertEquals(1, phones.host.state.value.stage)
        assertEquals(1, phones.guest.state.value.stage)
    }

    @Test
    fun `leaving says goodbye and closes both ends`() = runTest {
        val phones = connect()
        phones.host.leave()
        runCurrent()
        assertEquals(EndReason.YOU_LEFT, phones.host.state.value.endReason)
        assertEquals(EndReason.FRIEND_LEFT, phones.guest.state.value.endReason)
        assertEquals(PeerMessage.Leave, phones.hostChannel.sent.last())
        assertTrue(phones.hostChannel.closed)
        assertTrue(phones.guestChannel.closed)
    }

    @Test
    fun `closing without a goodbye looks like a lost connection to the friend`() = runTest {
        val phones = connect()
        phones.guest.close()
        runCurrent()
        assertEquals(EndReason.CONNECTION_LOST, phones.host.state.value.endReason)
        assertTrue(phones.hostChannel.closed)
    }

    @Test
    fun `events are delivered once each, in order`() = runTest {
        val phones = connect()
        phones.host.play(Cell(1, 1))
        runCurrent()
        phones.host.leave()
        runCurrent()
        val events = mutableListOf<TwoPhoneEvent>()
        val job = launch { phones.host.events.collect { events += it } }
        runCurrent()
        job.cancel()
        assertEquals(listOf(TwoPhoneEvent.NewBoard, TwoPhoneEvent.MovePlayed(Player.X, byFriend = false)), events)
    }
}
