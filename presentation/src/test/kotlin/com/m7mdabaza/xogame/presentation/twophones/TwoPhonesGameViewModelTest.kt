package com.m7mdabaza.xogame.presentation.twophones

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.ConnectedPeer
import com.m7mdabaza.xogame.domain.multiplayer.EndReason
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.PROTOCOL_VERSION
import com.m7mdabaza.xogame.domain.multiplayer.PeerChannel
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.PeerRole
import com.m7mdabaza.xogame.domain.multiplayer.RequestKind
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhonePhase
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneRules
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordGameResultUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.presentation.game.GamePhase
import com.m7mdabaza.xogame.presentation.testing.FakeAchievementsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeAdUnitIds
import com.m7mdabaza.xogame.presentation.testing.FakeChallengeRepository
import com.m7mdabaza.xogame.presentation.testing.FakeGameFeedback
import com.m7mdabaza.xogame.presentation.testing.FakePeerChannel
import com.m7mdabaza.xogame.presentation.testing.FakePeerLobby
import com.m7mdabaza.xogame.presentation.testing.FakePurchaseRepository
import com.m7mdabaza.xogame.presentation.testing.FakeStatsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeTime
import com.m7mdabaza.xogame.presentation.testing.PeerChannelPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/** Two real game ViewModels, one per phone, playing each other over an in-memory connection. */
@OptIn(ExperimentalCoroutinesApi::class)
class TwoPhonesGameViewModelTest {

    private val rules = TwoPhoneRules(StartGameUseCase(), PlayMoveUseCase(WinChecker()))
    private val bestOf3 = TwoPhoneSetup(BoardSize.THREE, MatchLength.BEST_OF_3, Player.X)

    /** Collects the screens' one-off events; Main is set before any phone exists. */
    private val collectors = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        collectors.cancel()
        Dispatchers.resetMain()
    }

    /** One phone: its own lobby, stats, sounds and game ViewModel. */
    private inner class Phone(channel: PeerChannel, role: PeerRole, name: String, setup: TwoPhoneSetup?, random: Random = Random(0)) {
        val lobby = FakePeerLobby().apply {
            state.value = LobbyState.Connected(ConnectedPeer(channel, role, myName = name, friendName = "", setup = setup))
        }
        val stats = FakeStatsRepository()
        val achievements = FakeAchievementsRepository()
        val feedback = FakeGameFeedback()
        val purchases = FakePurchaseRepository()
        val store = ViewModelStore()
        val vm: TwoPhonesGameViewModel = ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer {
                    TwoPhonesGameViewModel(
                        lobby,
                        rules,
                        RecordGameResultUseCase(stats),
                        EvaluateAchievementsUseCase(stats, FakeChallengeRepository(), achievements, FakeTime()),
                        feedback,
                        random,
                        FakeAdUnitIds,
                        ObserveAdsRemovedUseCase(purchases),
                    )
                }
            },
        )[TwoPhonesGameViewModel::class]
        val events = mutableListOf<TwoPhonesGameEvent>()

        init {
            collectors.launch { vm.events.collect { events += it } }
        }

        val ui get() = vm.uiState.value
        val game get() = checkNotNull(ui.game) { "No board yet" }
        val info get() = checkNotNull(game.twoPhones)
        val record: LevelStats get() = stats.stats.value[GameMode.TWO_PHONES, game.boardSize]

        fun tap(row: Int, col: Int) = vm.onCellClick(Cell(row, col))
    }

    /** Ahmed hosts [setup] (X), Sara joins (O); they're playing once this returns. */
    private fun connect(setup: TwoPhoneSetup = bestOf3, guestRandom: Random = Random(0)): Pair<Phone, Phone> {
        val pair = PeerChannelPair()
        val host = Phone(pair.host, PeerRole.HOST, "Ahmed", setup)
        val guest = Phone(pair.guest, PeerRole.GUEST, "Sara", null, guestRandom)
        return host to guest
    }

    /** X takes the top row while O plays the middle row; X wins on the fifth move. */
    private fun xWinsTopRow(host: Phone, guest: Phone) {
        host.tap(0, 0)
        guest.tap(1, 0)
        host.tap(0, 1)
        guest.tap(1, 1)
        host.tap(0, 2)
    }

    // --- Starting --------------------------------------------------------------------------

    @Test
    fun `both phones start the host's match, the host on X with the first move`() {
        val (host, guest) = connect()
        for (phone in listOf(host, guest)) {
            assertEquals(GameMode.TWO_PHONES, phone.game.mode)
            assertEquals(BoardSize.THREE, phone.game.boardSize)
            assertEquals(MatchLength.BEST_OF_3, phone.game.match.length)
            assertEquals(GamePhase.PLAYING, phone.game.phase)
            assertEquals(listOf("new round"), phone.feedback.events)
        }
        assertEquals(Player.X, host.game.settings.humanSymbol)
        assertEquals(Player.O, guest.game.settings.humanSymbol)
        // Each phone shows the friend's name; its own player is "You" on screen.
        assertEquals("Sara", host.game.settings.oName)
        assertEquals("Ahmed", guest.game.settings.xName)
        assertTrue(host.game.boardEnabled)
        assertFalse(guest.game.boardEnabled)
    }

    @Test
    fun `before the friend is ready there is no board, only the connection`() {
        val channel = FakePeerChannel()
        val host = Phone(channel, PeerRole.HOST, "Ahmed", bestOf3)
        assertEquals(listOf(PeerMessage.Hello(PROTOCOL_VERSION, "Ahmed"), PeerMessage.Setup(bestOf3)), channel.sent)
        assertEquals(TwoPhonePhase.CONNECTING, host.ui.match?.phase)
        assertNull(host.ui.game)
        assertFalse(host.ui.isEnded)

        channel.receive(PeerMessage.Hello(PROTOCOL_VERSION, "Sara"))
        channel.receive(PeerMessage.Ready)
        assertEquals("Sara", host.ui.match?.friendName)
        assertEquals(GamePhase.PLAYING, host.game.phase)
    }

    @Test
    fun `without a connection the game says it's gone`() {
        val vm = TwoPhonesGameViewModel(
            FakePeerLobby(),
            rules,
            RecordGameResultUseCase(FakeStatsRepository()),
            EvaluateAchievementsUseCase(FakeStatsRepository(), FakeChallengeRepository(), FakeAchievementsRepository(), FakeTime()),
            FakeGameFeedback(),
            Random(0),
            FakeAdUnitIds,
            ObserveAdsRemovedUseCase(FakePurchaseRepository()),
        )
        assertTrue(vm.uiState.value.connectionMissing)
        assertTrue(vm.uiState.value.isEnded)
        assertNull(vm.uiState.value.game)
    }

    @Test
    fun `the game has the two-phone banner until this phone buys Remove ads`() {
        val (host, guest) = connect()
        assertEquals(FakeAdUnitIds.twoPhonesBanner, host.game.bannerAdUnitId)
        assertFalse(host.game.adsRemoved)

        host.purchases.adsRemoved.value = true
        assertTrue(host.game.adsRemoved)
        // Each phone has its own purchase: the friend still sees the banner.
        assertFalse(guest.game.adsRemoved)
        assertEquals(GamePhase.PLAYING, host.game.phase)
    }

    // --- Playing ---------------------------------------------------------------------------

    @Test
    fun `the board only opens on your turn`() {
        val (host, guest) = connect()
        guest.tap(1, 1)
        assertTrue(host.game.game.history.isEmpty())

        host.tap(0, 0)
        host.tap(0, 1) // not Ahmed's turn any more
        assertEquals(1, guest.game.game.history.size)
        assertFalse(host.game.boardEnabled)
        assertTrue(guest.game.boardEnabled)
    }

    @Test
    fun `a round counts on both phones, each from its own side, with the sounds`() {
        val (host, guest) = connect()
        xWinsTopRow(host, guest)

        assertEquals(GamePhase.ROUND_OVER, host.game.phase)
        assertEquals(GamePhase.ROUND_OVER, guest.game.phase)
        assertEquals(1, host.game.match.xWins)
        assertEquals(1, guest.game.match.xWins)
        assertEquals(LevelStats(wins = 1, currentStreak = 1, bestStreak = 1), host.record)
        assertEquals(LevelStats(losses = 1), guest.record)
        // The friend's moves make a sound without vibrating (like the computer's).
        assertEquals(
            listOf("new round", "move X", "move O computer", "move X", "move O computer", "move X", "end WIN"),
            host.feedback.events,
        )
        assertEquals("end LOSE", guest.feedback.events.last())
    }

    @Test
    fun `beating the friend unlocks its achievement on the winner's phone`() {
        val (host, guest) = connect()
        xWinsTopRow(host, guest)
        assertTrue(Achievement.BEAT_A_FRIEND in host.achievements.unlocked.value)
        assertTrue(host.events.any { it is TwoPhonesGameEvent.AchievementsUnlocked && Achievement.BEAT_A_FRIEND in it.achievements })
        assertFalse(Achievement.BEAT_A_FRIEND in guest.achievements.unlocked.value)
    }

    @Test
    fun `the next round starts once both tap it, and the other player starts it`() {
        val (host, guest) = connect()
        xWinsTopRow(host, guest)

        host.vm.onContinue()
        assertTrue(host.info.waitingForFriend)
        assertFalse(host.info.canContinue)
        assertTrue(guest.info.friendReady)
        assertEquals(GamePhase.ROUND_OVER, guest.game.phase)

        guest.vm.onContinue()
        for (phone in listOf(host, guest)) {
            assertEquals(GamePhase.PLAYING, phone.game.phase)
            assertTrue(phone.game.game.history.isEmpty())
            assertEquals(Player.O, phone.game.game.turn)
            assertFalse(phone.info.waitingForFriend)
            assertFalse(phone.info.friendReady)
        }
        assertTrue(guest.game.boardEnabled)
        assertEquals(1, host.game.match.xWins) // the score stays
    }

    // --- Requests --------------------------------------------------------------------------

    @Test
    fun `a restart waits for the friend's OK and keeps the starter and the score`() {
        val (host, guest) = connect()
        xWinsTopRow(host, guest)
        host.vm.onContinue()
        guest.vm.onContinue() // round 2: Sara (O) starts
        guest.tap(1, 1)
        host.tap(0, 0)

        guest.vm.onRestartRound()
        assertTrue(guest.info.waitingForAnswer)
        assertFalse(guest.game.boardEnabled) // Sara's turn, but her request waits
        assertFalse(guest.info.canRestart)
        assertEquals(RequestKind.RestartRound, host.ui.match?.friendRequest)

        host.vm.onAnswer(true)
        for (phone in listOf(host, guest)) {
            assertTrue(phone.game.game.history.isEmpty())
            assertEquals(Player.O, phone.game.game.turn)
            assertEquals(1, phone.game.match.xWins)
            assertFalse(phone.info.waitingForAnswer)
        }
        assertNull(host.ui.match?.friendRequest)
    }

    @Test
    fun `a declined request leaves the board and tells the asker`() {
        val (host, guest) = connect()
        host.tap(0, 0)
        host.vm.onRestartRound()
        guest.vm.onAnswer(false)

        assertEquals(listOf(TwoPhonesGameEvent.RequestDeclined(RequestKind.RestartRound)), host.events)
        assertFalse(host.info.waitingForAnswer)
        assertEquals(1, host.game.game.history.size)
        assertTrue(guest.game.boardEnabled)
    }

    @Test
    fun `a new match carries the asker's choices, with "me" meaning the asker`() {
        val (host, guest) = connect()
        host.tap(0, 0)
        guest.vm.onRequestNewMatch(BoardSize.FOUR, MatchLength.SINGLE, Starter.PLAYER_ONE)
        assertEquals(RequestKind.NewMatch(TwoPhoneSetup(BoardSize.FOUR, MatchLength.SINGLE, Player.O)), host.ui.match?.friendRequest)

        host.vm.onAnswer(true)
        for (phone in listOf(host, guest)) {
            assertEquals(BoardSize.FOUR, phone.game.boardSize)
            assertEquals(MatchLength.SINGLE, phone.game.match.length)
            assertEquals(Player.O, phone.game.game.turn)
            assertTrue(phone.game.game.history.isEmpty())
        }
    }

    @Test
    fun `"my friend starts" and random pick the starter for the new match`() {
        val (host, guest) = connect()
        host.vm.onRequestNewMatch(BoardSize.EIGHT, MatchLength.BEST_OF_5, Starter.PLAYER_TWO)
        assertEquals(Player.O, (guest.ui.match?.friendRequest as RequestKind.NewMatch).setup.firstPlayer)
        guest.vm.onAnswer(false)

        val starters = (0 until 20).map { seed ->
            val (_, randomGuest) = connect(guestRandom = Random(seed))
            randomGuest.vm.onRequestNewMatch(BoardSize.THREE, MatchLength.SINGLE, Starter.RANDOM)
            randomGuest.ui.match?.myRequest.let { (it as RequestKind.NewMatch).setup.firstPlayer }
        }
        assertEquals(setOf(Player.X, Player.O), starters.toSet())
    }

    @Test
    fun `while a request waits nobody can ask again or start a new match`() {
        val (host, guest) = connect()
        host.tap(0, 0)
        guest.vm.onRestartRound()
        assertFalse(guest.info.canNewMatch)
        assertFalse(host.info.canNewMatch)
        assertFalse(host.info.canRestart)
    }

    // --- Ending ----------------------------------------------------------------------------

    @Test
    fun `leaving ends the game on both phones`() {
        val (host, guest) = connect()
        host.tap(0, 0)
        guest.vm.onLeave()

        assertEquals(EndReason.YOU_LEFT, guest.ui.match?.endReason)
        assertEquals(EndReason.FRIEND_LEFT, host.ui.match?.endReason)
        assertTrue(host.ui.isEnded)
        // The last board stays visible, but nothing can be played on it.
        assertEquals(1, host.game.game.history.size)
        assertFalse(guest.game.boardEnabled)
        assertFalse(host.info.canRestart)
        assertTrue(host.info.ended)
    }

    @Test
    fun `a phone closing without a goodbye shows the connection lost, and an unfinished round doesn't count`() {
        val (host, guest) = connect()
        host.tap(0, 0)
        guest.store.clear() // the guest's game screen is gone (e.g. the app was swiped away)

        assertEquals(EndReason.CONNECTION_LOST, host.ui.match?.endReason)
        assertEquals(LevelStats(), host.record)
        assertEquals(LevelStats(), guest.record)
    }
}
