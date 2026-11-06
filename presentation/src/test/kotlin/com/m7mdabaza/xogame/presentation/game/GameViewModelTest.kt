package com.m7mdabaza.xogame.presentation.game

import androidx.lifecycle.SavedStateHandle
import com.m7mdabaza.xogame.domain.ai.SearchAi
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.AssistAccess
import com.m7mdabaza.xogame.domain.model.AssistKind
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.usecase.ComputerMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.GetAssistAccessUseCase
import com.m7mdabaza.xogame.domain.usecase.GetHintUseCase
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
import com.m7mdabaza.xogame.presentation.testing.FakeAchievementsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeAdPolicyRepository
import com.m7mdabaza.xogame.presentation.testing.FakeAdUnitIds
import com.m7mdabaza.xogame.presentation.testing.FakeChallengeRepository
import com.m7mdabaza.xogame.presentation.testing.FakeGameFeedback
import com.m7mdabaza.xogame.presentation.testing.FakePromptsRepository
import com.m7mdabaza.xogame.presentation.testing.FakePurchaseRepository
import com.m7mdabaza.xogame.presentation.testing.FakeSettingsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeStatsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val feedback = FakeGameFeedback()
    private val settingsRepository = FakeSettingsRepository()
    private val statsRepository = FakeStatsRepository()
    private val adPolicy = FakeAdPolicyRepository()
    private val purchases = FakePurchaseRepository()
    private val time = FakeTime()
    private val challengeRepository = FakeChallengeRepository()
    private val achievementsRepository = FakeAchievementsRepository()
    private val prompts = FakePromptsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** The route's arguments; like the navigation graph, a level only against the computer. */
    private fun handle(mode: GameMode, boardSize: BoardSize = BoardSize.THREE, level: Level = Level.EASY) =
        SavedStateHandle(
            buildMap {
                put(GameViewModel.ARG_MODE, mode.name)
                put(GameViewModel.ARG_BOARD_SIZE, boardSize.name)
                if (mode == GameMode.VS_COMPUTER) put(GameViewModel.ARG_LEVEL, level.name)
            },
        )

    private fun viewModel(handle: SavedStateHandle, random: Random = Random(1)) = GameViewModel(
        savedStateHandle = handle,
        startGame = StartGameUseCase(),
        playMove = PlayMoveUseCase(WinChecker()),
        undoMove = UndoMoveUseCase(),
        computerMove = ComputerMoveUseCase(SearchAi()),
        getHint = GetHintUseCase(SearchAi()),
        observeSettings = ObserveSettingsUseCase(settingsRepository),
        updateSettings = UpdateSettingsUseCase(settingsRepository),
        recordGameResult = RecordGameResultUseCase(statsRepository),
        observeAdsRemoved = ObserveAdsRemovedUseCase(purchases),
        getAssistAccess = GetAssistAccessUseCase(),
        shouldShowInterstitial = ShouldShowInterstitialUseCase(adPolicy, purchases, time),
        recordInterstitialShown = RecordInterstitialShownUseCase(adPolicy, time),
        recordRoundForAds = RecordRoundForAdsUseCase(adPolicy),
        evaluateAchievements = EvaluateAchievementsUseCase(statsRepository, challengeRepository, achievementsRepository, time),
        shouldRequestReview = ShouldRequestReviewUseCase(statsRepository, prompts, time),
        recordReviewRequested = RecordReviewRequestedUseCase(prompts, time),
        feedback = feedback,
        adUnitIds = FakeAdUnitIds,
        computeDispatcher = dispatcher,
        random = random,
    )

    private fun viewModel(mode: GameMode, boardSize: BoardSize = BoardSize.THREE, level: Level = Level.EASY) =
        viewModel(handle(mode, boardSize, level))

    private val GameViewModel.state get() = uiState.value

    private fun GameViewModel.tap(vararg cells: Cell) = cells.forEach { onCellClick(it) }

    /** Asks for [kind], accepts the ad offer and pretends the rewarded ad was watched to the end. */
    private fun GameViewModel.withAd(kind: AssistKind) {
        if (kind == AssistKind.HINT) onHint() else onUndo()
        assertEquals(kind, state.assistOffer)
        onAssistOfferAccepted()
        onRewardedFinished(kind, rewarded = true)
    }

    /** X: top row; O: middle row. X wins on the fifth move. */
    private val xWinsTopRow = arrayOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(0, 2))

    // --- Setup -----------------------------------------------------------------

    @Test
    fun `opens with the setup and an empty board`() {
        val vm = viewModel(GameMode.TWO_PLAYERS, BoardSize.FOUR)
        assertEquals(GamePhase.SETUP, vm.state.phase)
        assertFalse(vm.state.hasStarted)
        assertFalse(vm.state.boardEnabled)
        assertEquals(4, vm.state.game.board.size)
        assertEquals("banner-game", vm.state.bannerAdUnitId)
        assertEquals("interstitial-between-rounds", vm.state.interstitialAdUnitId)
        assertNull("no level without the computer", vm.state.level)
        vm.onCellClick(Cell(0, 0))
        assertTrue(vm.state.game.history.isEmpty())
    }

    @Test
    fun `against the computer the board and level come from the route`() {
        val vm = viewModel(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.MEDIUM)
        assertEquals(BoardSize.EIGHT, vm.state.boardSize)
        assertEquals(Level.MEDIUM, vm.state.level)
        assertEquals(8, vm.state.game.board.size)
        assertEquals(Level.MEDIUM, vm.state.game.config.level)
        // One game banner for every mode, board and level.
        assertEquals("banner-game", vm.state.bannerAdUnitId)
    }

    @Test
    fun `a level passed for two players is ignored`() {
        val handle = SavedStateHandle(
            mapOf(
                GameViewModel.ARG_MODE to GameMode.TWO_PLAYERS.name,
                GameViewModel.ARG_BOARD_SIZE to BoardSize.THREE.name,
                GameViewModel.ARG_LEVEL to Level.HARD.name,
            ),
        )
        assertNull(viewModel(handle).state.game.config.level)
    }

    @Test
    fun `dismissing the setup before the first match does nothing (the screen closes)`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onDismissSetup()
        assertEquals(GamePhase.SETUP, vm.state.phase)
    }

    @Test
    fun `names are trimmed and shortened`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings(xName = "  Mohamed  ", oName = "A".repeat(40)))
        assertEquals("Mohamed", vm.state.settings.xName)
        assertEquals(GameViewModel.MAX_NAME_LENGTH, vm.state.settings.oName.length)
        assertEquals(listOf("new round"), feedback.events)
    }

    // --- Two players -------------------------------------------------------------

    @Test
    fun `two players alternate and a line wins the round`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        assertEquals(GamePhase.PLAYING, vm.state.phase)
        assertEquals(Player.X, vm.state.game.turn)

        vm.tap(*xWinsTopRow)
        assertEquals(GamePhase.ROUND_OVER, vm.state.phase)
        assertEquals(Player.X, (vm.state.game.result as GameResult.Win).player)
        assertEquals(1, vm.state.match.xWins)
        assertFalse(vm.state.boardEnabled)
        assertFalse(vm.state.canUndo)
        assertEquals("end WIN", feedback.events.last())
        assertEquals(5, feedback.events.count { it.startsWith("move") })
    }

    @Test
    fun `tapping a taken square changes nothing`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(Cell(1, 1), Cell(1, 1))
        assertEquals(1, vm.state.game.history.size)
        assertEquals(Player.O, vm.state.game.turn)
    }

    @Test
    fun `player two can start`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings(starter = Starter.PLAYER_TWO))
        assertEquals(Player.O, vm.state.game.turn)
    }

    @Test
    fun `undo in two-player mode takes back one move`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        assertFalse(vm.state.canUndo)
        vm.tap(Cell(0, 0), Cell(1, 1))
        assertTrue(vm.state.canUndo)
        vm.onUndo()
        assertEquals(listOf(Cell(0, 0)), vm.state.game.history.map { it.cell })
        assertEquals(Player.O, vm.state.game.turn)
    }

    @Test
    fun `no undo once a round has ended, whoever won`() = runTest(dispatcher) {
        // Two players: the winner can't take the winning move back.
        val twoPlayers = viewModel(GameMode.TWO_PLAYERS)
        twoPlayers.onStartMatch(MatchSettings())
        twoPlayers.tap(*xWinsTopRow)
        assertFalse(twoPlayers.state.canUndo)
        twoPlayers.onUndo()
        assertEquals(5, twoPlayers.state.game.history.size)
        assertEquals(GamePhase.ROUND_OVER, twoPlayers.state.phase)
        assertEquals(1, twoPlayers.state.match.xWins)

        // Against the computer: whatever the result, the finished round stays finished.
        val vsComputer = viewModel(GameMode.VS_COMPUTER)
        vsComputer.onStartMatch(MatchSettings())
        while (vsComputer.state.phase == GamePhase.PLAYING) {
            vsComputer.onCellClick(vsComputer.state.game.board.emptyCells.last())
            advanceUntilIdle()
        }
        val finished = vsComputer.state
        assertFalse(finished.canUndo)
        vsComputer.onUndo()
        assertEquals(finished.game, vsComputer.state.game)
        assertEquals(finished.match, vsComputer.state.match)
    }

    @Test
    fun `hint shows the winning square and the next move clears it`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1)) // X to move, wins at (0,2)
        assertTrue(vm.state.canHint)
        vm.withAd(AssistKind.HINT)
        assertTrue(vm.state.hintLoading)
        advanceUntilIdle()
        assertEquals(Cell(0, 2), vm.state.hint)
        assertFalse(vm.state.canHint)
        vm.onCellClick(Cell(2, 2))
        assertNull(vm.state.hint)
    }

    @Test
    fun `restart clears the board without scoring`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(Cell(0, 0), Cell(1, 1))
        vm.onRestartRound()
        assertTrue(vm.state.game.history.isEmpty())
        assertEquals(0, vm.state.match.roundsPlayed)
        assertEquals(Player.X, vm.state.game.turn)
    }

    // --- Matches -----------------------------------------------------------------

    @Test
    fun `single rounds keep a running score and the chosen starter`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings(length = MatchLength.SINGLE))
        vm.tap(*xWinsTopRow)
        vm.onNextRound()
        assertEquals(GamePhase.PLAYING, vm.state.phase)
        assertEquals(Player.X, vm.state.game.turn)
        vm.tap(*xWinsTopRow)
        assertEquals(2, vm.state.match.xWins)
        assertEquals(GamePhase.ROUND_OVER, vm.state.phase)
    }

    @Test
    fun `best of 3 alternates the starter and ends with a match winner`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings(length = MatchLength.BEST_OF_3))
        vm.tap(*xWinsTopRow)
        assertEquals(GamePhase.ROUND_OVER, vm.state.phase)

        vm.onNextRound()
        assertEquals(Player.O, vm.state.game.turn) // round 2: O starts
        // O plays the "X" moves of the pattern, so O completes the top row.
        vm.tap(*xWinsTopRow)
        assertEquals(1, vm.state.match.oWins)

        vm.onNextRound()
        assertEquals(Player.X, vm.state.game.turn) // round 3: X again
        vm.tap(*xWinsTopRow)
        assertEquals(GamePhase.MATCH_OVER, vm.state.phase)
        assertEquals(Player.X, vm.state.match.winner)

        vm.onNextRound() // new match, same settings
        assertEquals(GamePhase.PLAYING, vm.state.phase)
        assertEquals(0, vm.state.match.roundsPlayed)
        assertEquals(MatchLength.BEST_OF_3, vm.state.match.length)
    }

    @Test
    fun `opening the setup mid-game and closing it resumes the round`() {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(Cell(0, 0))
        vm.onOpenSetup()
        assertEquals(GamePhase.SETUP, vm.state.phase)
        vm.onDismissSetup()
        assertEquals(GamePhase.PLAYING, vm.state.phase)
        assertEquals(1, vm.state.game.history.size)
    }

    // --- Against the computer ------------------------------------------------------

    @Test
    fun `the computer replies after a short pause`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings())
        vm.onCellClick(Cell(1, 1))
        assertTrue(vm.state.computerThinking)
        assertFalse(vm.state.boardEnabled)
        vm.onCellClick(Cell(0, 0)) // ignored while the phone thinks
        assertEquals(1, vm.state.game.history.size)

        advanceTimeBy(GameViewModel.COMPUTER_DELAY_MS - 1)
        assertEquals(1, vm.state.game.history.size)
        advanceUntilIdle()
        assertEquals(2, vm.state.game.history.size)
        assertEquals(Player.O, vm.state.game.lastMove?.player)
        assertFalse(vm.state.computerThinking)
        assertTrue(vm.state.boardEnabled)
        assertEquals("move O computer", feedback.events.last())
    }

    @Test
    fun `the computer opens when it starts`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings(humanSymbol = Player.O, starter = Starter.PLAYER_TWO))
        assertTrue(vm.state.computerThinking)
        advanceUntilIdle()
        assertEquals(Player.X, vm.state.game.lastMove?.player)
        assertTrue(vm.state.game.isHumanTurn)
    }

    @Test
    fun `undo against the computer removes its reply and the human move`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings())
        vm.onCellClick(Cell(1, 1))
        advanceUntilIdle()
        vm.withAd(AssistKind.UNDO)
        assertTrue(vm.state.game.history.isEmpty())
        assertTrue(vm.state.boardEnabled)
    }

    @Test
    fun `undo while the computer thinks cancels its move`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings())
        vm.onCellClick(Cell(1, 1))
        assertTrue(vm.state.canUndo)
        vm.withAd(AssistKind.UNDO)
        advanceUntilIdle()
        assertTrue(vm.state.game.history.isEmpty())
        assertFalse(vm.state.computerThinking)
    }

    @Test
    fun `losing to the computer plays the losing sound`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings())
        // Play badly until the round ends: always the last empty square.
        while (vm.state.phase == GamePhase.PLAYING) {
            vm.onCellClick(vm.state.game.board.emptyCells.last())
            advanceUntilIdle()
        }
        val result = vm.state.game.result
        val expected = when {
            result is GameResult.Win && result.player == Player.O -> "end LOSE"
            result is GameResult.Win -> "end WIN"
            else -> "end DRAW"
        }
        assertEquals(expected, feedback.events.last())
    }

    // --- Saving --------------------------------------------------------------------

    @Test
    fun `the session survives process death`() {
        val handle = handle(GameMode.TWO_PLAYERS, BoardSize.FOUR)
        val vm = viewModel(handle)
        vm.onStartMatch(MatchSettings(length = MatchLength.BEST_OF_5, xName = "Sara", starter = Starter.PLAYER_TWO))
        vm.tap(Cell(0, 0), Cell(1, 1), Cell(2, 2))

        val restored = viewModel(handle)
        assertEquals(vm.state.game.board, restored.state.game.board)
        assertEquals(vm.state.game.history, restored.state.game.history)
        assertEquals(vm.state.settings, restored.state.settings)
        assertEquals(vm.state.match, restored.state.match)
        assertEquals(GamePhase.PLAYING, restored.state.phase)
        assertEquals(Player.X, restored.state.game.turn)
    }

    @Test
    fun `a pending computer move resumes after restore`() = runTest(dispatcher) {
        val handle = handle(GameMode.VS_COMPUTER)
        val vm = viewModel(handle)
        vm.onStartMatch(MatchSettings())
        vm.onCellClick(Cell(1, 1)) // process dies while the phone is thinking

        val restored = viewModel(handle)
        assertTrue(restored.state.computerThinking)
        advanceUntilIdle()
        assertEquals(2, restored.state.game.history.size)
    }

    @Test
    fun `a finished round is restored without scoring it twice`() {
        val handle = handle(GameMode.TWO_PLAYERS)
        val vm = viewModel(handle)
        vm.onStartMatch(MatchSettings(length = MatchLength.BEST_OF_3))
        vm.tap(*xWinsTopRow)

        val restored = viewModel(handle)
        assertEquals(GamePhase.ROUND_OVER, restored.state.phase)
        assertEquals(1, restored.state.match.xWins)
        restored.onNextRound()
        assertEquals(Player.O, restored.state.game.turn) // alternation survives too
    }

    @Test
    fun `unreadable saved data falls back to a fresh setup`() {
        val handle = handle(GameMode.TWO_PLAYERS)
        handle[GameViewModel.KEY_SESSION] = "not json"
        assertEquals(GamePhase.SETUP, viewModel(handle).state.phase)
    }

    // --- Settings and stats ----------------------------------------------------------

    @Test
    fun `the setup opens with the choices of the last match`() = runTest(dispatcher) {
        settingsRepository.settings.value = Settings(
            matchLength = MatchLength.BEST_OF_5,
            humanSymbol = Player.O,
            starter = Starter.RANDOM,
            xName = "Sara",
            oName = "Omar",
        )
        val vm = viewModel(GameMode.TWO_PLAYERS)
        advanceUntilIdle()
        assertEquals(
            MatchSettings(humanSymbol = Player.O, starter = Starter.RANDOM, length = MatchLength.BEST_OF_5, xName = "Sara", oName = "Omar"),
            vm.state.settings,
        )
    }

    @Test
    fun `starting a match remembers its choices and keeps the other settings`() = runTest(dispatcher) {
        settingsRepository.settings.value = Settings(soundEnabled = false)
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings(humanSymbol = Player.O, starter = Starter.PLAYER_TWO, length = MatchLength.BEST_OF_3))
        advanceUntilIdle()
        assertEquals(
            Settings(soundEnabled = false, matchLength = MatchLength.BEST_OF_3, humanSymbol = Player.O, starter = Starter.PLAYER_TWO),
            settingsRepository.settings.value,
        )
    }

    @Test
    fun `finished rounds are recorded in the stats of their mode and board`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(*xWinsTopRow)
        advanceUntilIdle()
        vm.onNextRound()
        vm.tap(*xWinsTopRow) // X again (single rounds keep the starter)
        advanceUntilIdle()
        assertEquals(LevelStats(wins = 2, currentStreak = 2, bestStreak = 2), statsRepository.stats.value[GameMode.TWO_PLAYERS, BoardSize.THREE])
    }

    @Test
    fun `losing to the computer counts as a loss for the human`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER)
        vm.onStartMatch(MatchSettings())
        while (vm.state.phase == GamePhase.PLAYING) {
            vm.onCellClick(vm.state.game.board.emptyCells.last())
            advanceUntilIdle()
        }
        val stats = statsRepository.stats.value[GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY]
        assertEquals(1, stats.played)
        val result = vm.state.game.result
        if (result is GameResult.Win && result.player == Player.O) assertEquals(1, stats.losses)
    }

    @Test
    fun `games against the computer are recorded per board and level`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.VS_COMPUTER, BoardSize.FOUR, Level.HARD)
        vm.onStartMatch(MatchSettings())
        while (vm.state.phase == GamePhase.PLAYING) {
            vm.onCellClick(vm.state.game.board.emptyCells.last())
            advanceUntilIdle()
        }
        val stats = statsRepository.stats.value
        assertEquals(1, stats[GameMode.VS_COMPUTER, BoardSize.FOUR, Level.HARD].played)
        assertEquals(1, stats.played(GameMode.VS_COMPUTER))
    }

    @Test
    fun `a finished round announces newly unlocked achievements once`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(*xWinsTopRow)
        advanceUntilIdle()
        assertEquals(GameEvent.AchievementsUnlocked(listOf(Achievement.TWO_PLAYER_GAME)), vm.events.first())
        assertEquals(setOf(Achievement.TWO_PLAYER_GAME), achievementsRepository.unlocked.value.keys)

        vm.onNextRound()
        vm.tap(*xWinsTopRow)
        advanceUntilIdle()
        assertEquals("unlocked only once", 1, achievementsRepository.unlocked.value.size)
    }

    @Test
    fun `a two-player win never asks for a Play review`() = runTest(dispatcher) {
        statsRepository.stats.value = Stats(mapOf(StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE) to LevelStats(draws = 20)))
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(*xWinsTopRow)
        advanceUntilIdle()
        assertNull(prompts.reviewRequestedAt.value)
    }

    @Test
    fun `restarted rounds are not recorded`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(Cell(0, 0), Cell(1, 1))
        vm.onRestartRound()
        advanceUntilIdle()
        assertEquals(0, statsRepository.stats.value.played(GameMode.TWO_PLAYERS))
    }

    @Test
    fun `a round restored after process death is not recorded again`() = runTest(dispatcher) {
        val handle = handle(GameMode.TWO_PLAYERS)
        val vm = viewModel(handle)
        vm.onStartMatch(MatchSettings())
        vm.tap(*xWinsTopRow)
        advanceUntilIdle()

        viewModel(handle)
        advanceUntilIdle()
        assertEquals(1, statsRepository.stats.value.played(GameMode.TWO_PLAYERS))
    }

    // --- Rewarded hint / undo ----------------------------------------------------------

    @Test
    fun `without Remove ads a hint asks for an ad first`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        vm.tap(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1)) // X to move, wins at (0,2)
        assertEquals(AssistAccess.REWARDED_AD, vm.state.hintAccess)

        vm.onHint()
        assertEquals(AssistKind.HINT, vm.state.assistOffer)
        vm.onAssistOfferDismissed()
        assertNull(vm.state.assistOffer)
        assertNull(vm.state.hint)

        vm.onHint()
        vm.onAssistOfferAccepted()
        assertEquals(GameEvent.ShowRewarded(AssistKind.HINT), vm.events.first())

        vm.onRewardedFinished(AssistKind.HINT, rewarded = false) // closed early
        advanceUntilIdle()
        assertNull(vm.state.hint)

        vm.onHint()
        vm.onAssistOfferAccepted()
        vm.onRewardedFinished(AssistKind.HINT, rewarded = true)
        advanceUntilIdle()
        assertEquals(Cell(0, 2), vm.state.hint)
    }

    @Test
    fun `no rewarded ad ready tells the player`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onRewardedUnavailable()
        assertEquals(GameEvent.AdNotReady, vm.events.first())
    }

    @Test
    fun `undo against the computer costs an ad, but is always free for two players`() = runTest(dispatcher) {
        val vsComputer = viewModel(GameMode.VS_COMPUTER)
        vsComputer.onStartMatch(MatchSettings())
        vsComputer.onCellClick(Cell(1, 1))
        advanceUntilIdle()
        assertEquals(AssistAccess.REWARDED_AD, vsComputer.state.undoAccess)
        vsComputer.onUndo()
        assertEquals(AssistKind.UNDO, vsComputer.state.assistOffer)
        assertEquals(2, vsComputer.state.game.history.size)

        val twoPlayers = viewModel(GameMode.TWO_PLAYERS)
        twoPlayers.onStartMatch(MatchSettings())
        assertEquals(AssistAccess.FREE, twoPlayers.state.undoAccess)
    }

    @Test
    fun `buyers get one free hint and one free undo per round`() = runTest(dispatcher) {
        purchases.adsRemoved.value = true
        val vm = viewModel(GameMode.VS_COMPUTER)
        advanceUntilIdle()
        assertTrue(vm.state.adsRemoved)
        vm.onStartMatch(MatchSettings())

        assertEquals(AssistAccess.FREE, vm.state.hintAccess)
        vm.onHint()
        assertNull("no ad offer for the free hint", vm.state.assistOffer)
        advanceUntilIdle()
        assertTrue(vm.state.freeHintUsed)
        assertEquals(AssistAccess.REWARDED_AD, vm.state.hintAccess)

        vm.onCellClick(Cell(1, 1))
        advanceUntilIdle()
        assertEquals(AssistAccess.FREE, vm.state.undoAccess)
        vm.onUndo()
        assertTrue(vm.state.game.history.isEmpty())
        assertEquals(AssistAccess.REWARDED_AD, vm.state.undoAccess)

        // Restarting doesn't hand out new free uses ...
        vm.onCellClick(Cell(0, 0))
        advanceUntilIdle()
        vm.onRestartRound()
        assertEquals(AssistAccess.REWARDED_AD, vm.state.hintAccess)

        // ... a new round does.
        while (vm.state.phase == GamePhase.PLAYING) {
            vm.onCellClick(vm.state.game.board.emptyCells.last())
            advanceUntilIdle()
        }
        vm.onNextRound()
        advanceUntilIdle()
        assertEquals(AssistAccess.FREE, vm.state.hintAccess)
        assertEquals(AssistAccess.FREE, vm.state.undoAccess)
    }

    @Test
    fun `used free assists survive process death`() = runTest(dispatcher) {
        purchases.adsRemoved.value = true
        val handle = handle(GameMode.TWO_PLAYERS)
        val vm = viewModel(handle)
        advanceUntilIdle()
        vm.onStartMatch(MatchSettings())
        vm.onHint()
        advanceUntilIdle()

        val restored = viewModel(handle)
        advanceUntilIdle()
        assertTrue(restored.state.freeHintUsed)
        assertEquals(AssistAccess.REWARDED_AD, restored.state.hintAccess)
    }

    // --- Interstitials -----------------------------------------------------------------

    /** Plays two-player rounds that X wins, moving to the next round in between. */
    private fun kotlinx.coroutines.test.TestScope.finishRound(vm: GameViewModel) {
        vm.tap(*xWinsTopRow)
        advanceUntilIdle()
    }

    @Test
    fun `an interstitial shows between rounds only from the second round of a session`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        finishRound(vm)
        vm.onNextRound()
        assertEquals("no ad after the first round", GamePhase.PLAYING, vm.state.phase)

        finishRound(vm)
        vm.onNextRound()
        assertEquals(GameEvent.ShowInterstitial, vm.events.first { it !is GameEvent.AchievementsUnlocked })
        assertEquals("waits for the ad to close", GamePhase.ROUND_OVER, vm.state.phase)

        vm.onInterstitialFinished(shown = true)
        advanceUntilIdle()
        assertEquals(GamePhase.PLAYING, vm.state.phase)
        assertEquals(1, adPolicy.interstitialsShown)

        // Two more rounds, but within 3 minutes: no ad.
        finishRound(vm)
        vm.onNextRound()
        finishRound(vm)
        vm.onNextRound()
        assertEquals(GamePhase.PLAYING, vm.state.phase)
    }

    @Test
    fun `no ad was ready, so the round just goes on and nothing is recorded`() = runTest(dispatcher) {
        val vm = viewModel(GameMode.TWO_PLAYERS)
        vm.onStartMatch(MatchSettings())
        repeat(2) {
            finishRound(vm)
            if (it == 0) vm.onNextRound()
        }
        vm.onNextRound()
        vm.onInterstitialFinished(shown = false)
        advanceUntilIdle()
        assertEquals(GamePhase.PLAYING, vm.state.phase)
        assertEquals(0, adPolicy.interstitialsShown)
    }

    @Test
    fun `no interstitials and no banner after Remove ads`() = runTest(dispatcher) {
        purchases.adsRemoved.value = true
        val vm = viewModel(GameMode.TWO_PLAYERS)
        advanceUntilIdle()
        assertTrue(vm.state.adsRemoved)
        vm.onStartMatch(MatchSettings())
        repeat(3) {
            finishRound(vm)
            vm.onNextRound()
            assertEquals(GamePhase.PLAYING, vm.state.phase)
        }
        assertEquals(0, adPolicy.interstitialsShown)
    }
}
