package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Answer
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Continue
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Hello
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Move
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Ready
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Request
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage.Setup
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneEvent.MovePlayed
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneEvent.NewBoard
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneEvent.RequestDeclined
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneEvent.RoundFinished
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TwoPhoneRulesTest {

    private val rules = twoPhoneRules
    private val bigMatch = TwoPhoneSetup(BoardSize.EIGHT, MatchLength.BEST_OF_5, Player.O)

    /** The second mover wins the top row on 3×3: 1,0 / 0,0 / 2,2 / 0,1 / 2,1 / 0,2. */
    private val secondMoverWins = arrayOf(Cell(1, 0), Cell(0, 0), Cell(2, 2), Cell(0, 1), Cell(2, 1), Cell(0, 2))

    private fun started(setup: TwoPhoneSetup = TwoPhoneSetup(BoardSize.THREE, MatchLength.BEST_OF_3, Player.X)) =
        TwoPhones(setup).apply { settle() }

    // --- Connecting ------------------------------------------------------------------------

    @Test
    fun `after the hellos both phones start the host's match, the host as X and the guest as O`() {
        val phones = TwoPhones()
        assertEquals(listOf(Hello(PROTOCOL_VERSION, "Ahmed"), Setup(phones.setup)), phones.sentByHost)
        assertEquals(TwoPhonePhase.CONNECTING, phones.host.phase)

        phones.settle()
        phones.assertInSync()
        with(phones) {
            assertEquals(listOf(Hello(PROTOCOL_VERSION, "Sara"), Ready), sentByGuest)
            assertEquals(TwoPhonePhase.PLAYING, host.phase)
            assertEquals("Sara", host.friendName)
            assertEquals("Ahmed", guest.friendName)
            assertEquals(Player.X, host.game?.config?.humanSymbol)
            assertEquals(Player.O, guest.game?.config?.humanSymbol)
            assertEquals(GameMode.TWO_PHONES, guest.game?.config?.mode)
            assertEquals(MatchState(MatchLength.BEST_OF_3), guest.match)
            assertEquals(listOf(NewBoard), hostEvents)
            assertEquals(listOf(NewBoard), guestEvents)
        }
    }

    @Test
    fun `the host starts only when the guest is ready`() {
        val phones = TwoPhones()
        phones.deliver(Side.HOST) // the guest's hello
        assertEquals(TwoPhonePhase.CONNECTING, phones.host.phase)
        phones.deliver(Side.GUEST) // the host's hello and setup
        assertEquals(TwoPhonePhase.PLAYING, phones.guest.phase)
        assertEquals(TwoPhonePhase.CONNECTING, phones.host.phase)
        phones.deliver(Side.HOST) // ready
        assertEquals(TwoPhonePhase.PLAYING, phones.host.phase)
    }

    @Test
    fun `names are trimmed and shortened`() {
        val phones = TwoPhones(hostName = "  A very long name that goes on and on  ", guestName = " ")
        phones.settle()
        assertEquals("A very long name tha", phones.guest.friendName)
        assertEquals("", phones.host.friendName)
    }

    @Test
    fun `phones with different versions can't play, and each says which one to update`() {
        val guest = rules.start(PeerRole.GUEST, "Sara", null).state
        assertEquals(EndReason.UPDATE_THIS_APP, rules.receive(guest, Hello(PROTOCOL_VERSION + 1, "Ahmed")).state.endReason)
        assertEquals(EndReason.FRIEND_MUST_UPDATE, rules.receive(guest, Hello(PROTOCOL_VERSION - 1, "Ahmed")).state.endReason)
    }

    @Test
    fun `only the host sends the setup and only the guest gets ready, after the hello`() {
        val host = rules.start(PeerRole.HOST, "Ahmed", TwoPhoneSetup(BoardSize.THREE, MatchLength.SINGLE, Player.X)).state
        val guest = rules.start(PeerRole.GUEST, "Sara", null).state
        val setup = Setup(TwoPhoneSetup(BoardSize.THREE, MatchLength.SINGLE, Player.X))
        assertEquals(EndReason.OUT_OF_SYNC, rules.receive(guest, setup).state.endReason) // before the hello
        val greetedHost = rules.receive(host, Hello(PROTOCOL_VERSION, "Sara")).state
        val greetedGuest = rules.receive(guest, Hello(PROTOCOL_VERSION, "Ahmed")).state
        assertEquals(EndReason.OUT_OF_SYNC, rules.receive(greetedHost, setup).state.endReason)
        assertEquals(EndReason.OUT_OF_SYNC, rules.receive(greetedGuest, Ready).state.endReason)
        assertEquals(EndReason.OUT_OF_SYNC, rules.receive(greetedGuest, Hello(PROTOCOL_VERSION, "Ahmed")).state.endReason)
    }

    // --- Playing ---------------------------------------------------------------------------

    @Test
    fun `players take turns, and only the player on turn can move`() {
        val phones = started()
        phones.play(Side.GUEST, 0, 0) // X starts: not the guest's turn
        assertTrue(phones.toHost.isEmpty())
        assertNull(phones.guest.game?.lastMove)

        phones.play(Side.HOST, 1, 1)
        assertEquals(listOf(Move(0, 0, Cell(1, 1))), phones.toGuest.toList())
        assertFalse(phones.host.isMyTurn)
        phones.settle()
        assertTrue(phones.guest.isMyTurn)
        phones.assertInSync()
        assertEquals(listOf(NewBoard, MovePlayed(Player.X, byFriend = false)), phones.hostEvents)
        assertEquals(listOf(NewBoard, MovePlayed(Player.X, byFriend = true)), phones.guestEvents)

        phones.play(Side.GUEST, 1, 1) // taken
        assertTrue(phones.toHost.isEmpty())
        phones.play(Side.GUEST, 0, 0)
        assertEquals(listOf(Move(0, 1, Cell(0, 0))), phones.toHost.toList())
    }

    @Test
    fun `a finished round is scored on both phones, each from its own side`() {
        val phones = started()
        phones.playRound(*firstMoverWins)
        phones.assertInSync()
        with(phones) {
            assertEquals(TwoPhonePhase.ROUND_OVER, host.phase)
            assertEquals(MatchState(MatchLength.BEST_OF_3, xWins = 1), host.match)
            assertEquals(true, host.iWonRound)
            assertEquals(false, guest.iWonRound)
            assertEquals(Player.X, hostEvents.filterIsInstance<RoundFinished>().single().game.config.humanSymbol)
            assertEquals(Player.O, guestEvents.filterIsInstance<RoundFinished>().single().game.config.humanSymbol)
        }
    }

    // --- Next round and rematch ------------------------------------------------------------

    @Test
    fun `the next round starts once both continue, started by the other player`() {
        val phones = started()
        phones.playRound(*firstMoverWins)
        phones.continueAfterRound(Side.HOST)
        phones.settle()
        assertTrue(phones.host.iContinued)
        assertTrue(phones.guest.friendContinued)
        assertEquals(TwoPhonePhase.ROUND_OVER, phones.guest.phase)
        assertFalse(phones.host.canContinue)
        phones.continueAfterRound(Side.HOST) // twice: nothing
        assertTrue(phones.toGuest.isEmpty())

        phones.continueAfterRound(Side.GUEST)
        phones.settle()
        phones.assertInSync()
        assertEquals(TwoPhonePhase.PLAYING, phones.host.phase)
        assertEquals(1, phones.host.stage)
        assertEquals(Player.O, phones.host.game?.config?.firstPlayer)
        assertEquals(MatchState(MatchLength.BEST_OF_3, xWins = 1), phones.host.match)
        assertTrue(phones.guest.isMyTurn)
    }

    @Test
    fun `continuing at the same moment works too`() {
        val phones = started()
        phones.playRound(*drawRound)
        phones.continueAfterRound(Side.HOST)
        phones.continueAfterRound(Side.GUEST)
        phones.settle()
        phones.assertInSync()
        assertEquals(TwoPhonePhase.PLAYING, phones.host.phase)
        assertEquals(MatchState(MatchLength.BEST_OF_3, draws = 1), phones.host.match)
    }

    @Test
    fun `after a best-of match both continue for a rematch with a fresh score`() {
        val phones = started()
        phones.playRound(*firstMoverWins) // X started and won
        phones.continueAfterRound(Side.HOST)
        phones.continueAfterRound(Side.GUEST)
        phones.settle()
        phones.playRound(*secondMoverWins) // O started, X won: 2-0
        assertEquals(TwoPhonePhase.MATCH_OVER, phones.guest.phase)
        assertEquals(Player.X, phones.guest.match?.winner)

        phones.continueAfterRound(Side.GUEST)
        phones.continueAfterRound(Side.HOST)
        phones.settle()
        phones.assertInSync()
        assertEquals(TwoPhonePhase.PLAYING, phones.host.phase)
        assertEquals(MatchState(MatchLength.BEST_OF_3), phones.host.match)
        assertEquals(Player.X, phones.host.game?.config?.firstPlayer)
    }

    @Test
    fun `single rounds never end the match, and the starter alternates`() {
        val phones = started(TwoPhoneSetup(BoardSize.THREE, MatchLength.SINGLE, Player.O))
        val starters = mutableListOf<Player?>()
        repeat(3) {
            starters += phones.host.game?.config?.firstPlayer
            phones.playRound(*firstMoverWins)
            assertEquals(TwoPhonePhase.ROUND_OVER, phones.host.phase)
            phones.continueAfterRound(Side.HOST)
            phones.continueAfterRound(Side.GUEST)
            phones.settle()
        }
        assertEquals(listOf(Player.O, Player.X, Player.O), starters)
        assertEquals(MatchState(MatchLength.SINGLE, xWins = 1, oWins = 2), phones.host.match)
    }

    // --- Requests --------------------------------------------------------------------------

    @Test
    fun `an accepted restart clears the board on both phones, with the same starter and score`() {
        val phones = started()
        phones.play(Side.HOST, 0, 0)
        phones.settle()
        phones.request(Side.GUEST, RequestKind.RestartRound)
        phones.settle()
        assertEquals(RequestKind.RestartRound, phones.host.friendRequest)
        assertFalse(phones.guest.canPlay) // the guest's turn, but its request waits

        phones.answer(Side.HOST, accept = true)
        phones.settle()
        phones.assertInSync()
        assertEquals(1, phones.host.stage)
        assertEquals(emptyList<Any>(), phones.host.game?.history)
        assertEquals(Player.X, phones.host.game?.config?.firstPlayer)
        assertEquals(MatchState(MatchLength.BEST_OF_3), phones.host.match)
        assertEquals(NewBoard, phones.guestEvents.last())
    }

    @Test
    fun `a declined request changes nothing, and the asker hears no`() {
        val phones = started()
        val kind = RequestKind.NewMatch(bigMatch)
        phones.request(Side.GUEST, kind)
        phones.settle()
        phones.answer(Side.HOST, accept = false)
        phones.settle()
        phones.assertInSync()
        assertEquals(0, phones.host.stage)
        assertEquals(RequestDeclined(kind), phones.guestEvents.last())
        assertTrue(phones.guest.canRequest(kind))
    }

    @Test
    fun `while a request waits its sender can't move or continue, and nobody can ask again`() {
        val phones = started()
        phones.request(Side.HOST, RequestKind.RestartRound)
        assertFalse(phones.host.canPlay)
        phones.play(Side.HOST, 0, 0)
        assertEquals(1, phones.toGuest.size) // just the request
        assertFalse(phones.host.canRequest(RequestKind.NewMatch(bigMatch)))
        phones.settle()
        assertFalse(phones.guest.canRequest(RequestKind.NewMatch(bigMatch)))
    }

    @Test
    fun `the friend may still move while deciding, and the answer applies to the board as it is then`() {
        val phones = started()
        phones.play(Side.HOST, 0, 0)
        phones.settle()
        phones.request(Side.HOST, RequestKind.NewMatch(bigMatch))
        phones.settle()
        phones.play(Side.GUEST, 1, 1)
        phones.answer(Side.GUEST, accept = true)
        phones.settle()
        phones.assertInSync()
        assertEquals(bigMatch, phones.host.setup)
        assertEquals(BoardSize.EIGHT, phones.guest.game?.config?.boardSize)
        assertEquals(Player.O, phones.guest.game?.config?.firstPlayer)
        assertEquals(MatchState(MatchLength.BEST_OF_5), phones.guest.match)
    }

    @Test
    fun `a new match can be asked for between rounds, by the guest too`() {
        val phones = started()
        phones.playRound(*firstMoverWins)
        phones.request(Side.GUEST, RequestKind.NewMatch(bigMatch))
        phones.settle()
        phones.answer(Side.HOST, accept = true)
        phones.settle()
        phones.assertInSync()
        assertEquals(TwoPhonePhase.PLAYING, phones.host.phase)
        assertEquals(MatchState(MatchLength.BEST_OF_5), phones.host.match)
    }

    @Test
    fun `a restart can't be asked for between rounds`() {
        val phones = started()
        phones.playRound(*firstMoverWins)
        phones.request(Side.HOST, RequestKind.RestartRound)
        assertTrue(phones.toGuest.isEmpty())
        assertNull(phones.host.myRequest)
    }

    @Test
    fun `when both ask at the same moment the host's request wins`() {
        val phones = started()
        val hostWants = RequestKind.NewMatch(bigMatch)
        phones.request(Side.HOST, hostWants)
        phones.request(Side.GUEST, RequestKind.RestartRound)
        phones.settle()
        phones.assertInSync()
        assertNull(phones.guest.myRequest)
        assertEquals(hostWants, phones.guest.friendRequest)

        phones.answer(Side.GUEST, accept = true)
        phones.settle()
        phones.assertInSync()
        assertEquals(bigMatch, phones.guest.setup)
    }

    @Test
    fun `a restart that crosses the round's last move lapses on both phones`() {
        val phones = started()
        phones.playRound(*firstMoverWins.copyOfRange(0, 4))
        phones.request(Side.GUEST, RequestKind.RestartRound)
        phones.play(Side.HOST, 0, 2) // wins before the request arrives
        phones.settle()
        phones.assertInSync()
        assertEquals(TwoPhonePhase.ROUND_OVER, phones.host.phase)
        assertNull(phones.host.friendRequest)
        assertNull(phones.guest.myRequest)
        assertTrue(phones.host.canContinue && phones.guest.canContinue)
    }

    @Test
    fun `a new-match request that crosses the round's last move still stands`() {
        val phones = started()
        phones.playRound(*firstMoverWins.copyOfRange(0, 4))
        phones.request(Side.GUEST, RequestKind.NewMatch(bigMatch))
        phones.play(Side.HOST, 0, 2)
        phones.settle()
        phones.assertInSync()
        assertEquals(RequestKind.NewMatch(bigMatch), phones.host.friendRequest)
        phones.answer(Side.HOST, accept = true)
        phones.settle()
        phones.assertInSync()
        assertEquals(bigMatch, phones.host.setup)
        // The finished round still counted on both phones.
        assertEquals(1, phones.hostEvents.filterIsInstance<RoundFinished>().size)
        assertEquals(1, phones.guestEvents.filterIsInstance<RoundFinished>().size)
    }

    @Test
    fun `a request that crosses a continue is answered first`() {
        for (accept in listOf(true, false)) {
            val phones = started()
            phones.playRound(*firstMoverWins)
            phones.continueAfterRound(Side.HOST)
            phones.request(Side.GUEST, RequestKind.NewMatch(bigMatch))
            phones.settle()
            phones.assertInSync()
            assertFalse(phones.guest.canContinue)

            phones.answer(Side.HOST, accept)
            phones.settle()
            phones.assertInSync()
            if (accept) {
                assertEquals(bigMatch, phones.host.setup)
            } else {
                // The host's "next round" still counts; the guest's makes it happen.
                assertTrue(phones.guest.friendContinued)
                phones.continueAfterRound(Side.GUEST)
                phones.settle()
                phones.assertInSync()
                assertEquals(TwoPhonePhase.PLAYING, phones.host.phase)
                assertEquals(1, phones.host.stage)
            }
        }
    }

    // --- Ending ----------------------------------------------------------------------------

    @Test
    fun `leaving says goodbye, and the friend sees it`() {
        val phones = started()
        phones.act(Side.HOST) { leave(it) }
        assertEquals(EndReason.YOU_LEFT, phones.host.endReason)
        assertEquals(listOf(PeerMessage.Leave), phones.toGuest.toList())
        phones.settle()
        assertEquals(EndReason.FRIEND_LEFT, phones.guest.endReason)
    }

    @Test
    fun `a lost connection ends the game`() {
        val phones = started()
        phones.lose(Side.GUEST)
        assertEquals(TwoPhonePhase.ENDED, phones.guest.phase)
        assertEquals(EndReason.CONNECTION_LOST, phones.guest.endReason)
    }

    @Test
    fun `messages that break the rules end the game as out of sync`() {
        val phones = started() // X (the host) to move
        val guest = phones.guest
        val broken = listOf(
            Move(stage = 1, index = 0, cell = Cell(0, 0)), // wrong board
            Move(stage = 0, index = 1, cell = Cell(0, 0)), // a move went missing
            Move(stage = 0, index = 0, cell = Cell(5, 5)), // off the board
            Continue(0), // the round isn't over
            Answer(0, accepted = true), // nothing was asked
            Request(3, RequestKind.RestartRound), // wrong board
        )
        for (message in broken) {
            assertEquals("for $message", EndReason.OUT_OF_SYNC, rules.receive(guest, message).state.endReason)
        }
        // The friend can't move while their own request waits.
        phones.request(Side.HOST, RequestKind.RestartRound)
        phones.settle()
        assertEquals(EndReason.OUT_OF_SYNC, rules.receive(phones.guest, Move(0, 0, Cell(0, 0))).state.endReason)
    }

    @Test
    fun `once the game is over nothing changes any more`() {
        val phones = started()
        phones.act(Side.HOST) { leave(it) }
        val ended = phones.host
        for (step in listOf(
            rules.play(ended, Cell(0, 0)),
            rules.request(ended, RequestKind.RestartRound),
            rules.receive(ended, Move(0, 0, Cell(0, 0))),
            rules.connectionLost(ended),
            rules.leave(ended),
        )) {
            assertEquals(ended, step.state)
            assertEquals(emptyList<PeerMessage>(), step.send)
        }
    }

    // --- Anything goes ---------------------------------------------------------------------

    @Test
    fun `random play with random delivery keeps both phones on the same board`() {
        val kinds = listOf(
            RequestKind.RestartRound,
            RequestKind.NewMatch(TwoPhoneSetup(BoardSize.THREE, MatchLength.SINGLE, Player.O)),
            RequestKind.NewMatch(TwoPhoneSetup(BoardSize.FOUR, MatchLength.BEST_OF_3, Player.X)),
        )
        repeat(3_000) { seed ->
            val random = Random(seed)
            val length = if (random.nextBoolean()) MatchLength.BEST_OF_3 else MatchLength.SINGLE
            val phones = TwoPhones(TwoPhoneSetup(BoardSize.THREE, length, Player.entries.random(random)))
            try {
                repeat(300) {
                    val side = Side.entries.random(random)
                    when (random.nextInt(50)) {
                        in 0..24 -> phones.state(side).game?.let { game ->
                            val n = game.config.boardSize.size
                            val empty = (0 until n).flatMap { r -> (0 until n).map { c -> Cell(r, c) } }.filter { game.board.isEmpty(it) }
                            if (empty.isNotEmpty()) phones.act(side) { play(it, empty.random(random)) }
                        }
                        in 25..29 -> phones.continueAfterRound(side)
                        30 -> phones.request(side, kinds.random(random))
                        in 31..34 -> phones.answer(side, random.nextBoolean())
                        else -> phones.deliver(side, 1 + random.nextInt(2))
                    }
                    assertFalse(phones.host.isEnded || phones.guest.isEnded)
                    if (phones.toHost.isEmpty() && phones.toGuest.isEmpty()) phones.assertInSync()
                }
                phones.settle()
                phones.assertInSync()
            } catch (e: AssertionError) {
                throw AssertionError("seed $seed: ${e.message} (host ${phones.host.endReason}, guest ${phones.guest.endReason})", e)
            }
            rounds += phones.hostEvents.count { it is RoundFinished }
            resets += phones.hostEvents.count { it == NewBoard } - 1
            declined += (phones.hostEvents + phones.guestEvents).count { it is RequestDeclined }
            granted += (phones.sentByHost + phones.sentByGuest).count { it is Answer && it.accepted }
            dropped += phones.droppedGuestRequests
            lapsed += phones.lapsedRestarts
        }
        // The random games really went through the interesting cases.
        val counts = "rounds $rounds, resets $resets, granted $granted, declined $declined, dropped $dropped, lapsed $lapsed"
        assertTrue(counts, rounds > 2_000 && resets > 3_000 && granted > 1_000 && declined > 1_000 && dropped > 200 && lapsed > 50)
    }

    private var rounds = 0
    private var resets = 0
    private var declined = 0
    private var granted = 0
    private var dropped = 0
    private var lapsed = 0
}
