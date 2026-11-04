package com.m7mdabaza.xogame.presentation.challenge

import com.m7mdabaza.xogame.domain.ai.SearchAi
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.domain.usecase.ComputerMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.GetDailyChallengeUseCase
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveChallengeProgressUseCase
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordChallengeSolvedUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordReviewRequestedUseCase
import com.m7mdabaza.xogame.domain.usecase.ShouldRequestReviewUseCase
import com.m7mdabaza.xogame.domain.usecase.StartChallengeUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.presentation.testing.FakeAchievementsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeAdUnitIds
import com.m7mdabaza.xogame.presentation.testing.FakeChallengeRepository
import com.m7mdabaza.xogame.presentation.testing.FakeGameFeedback
import com.m7mdabaza.xogame.presentation.testing.FakePromptsRepository
import com.m7mdabaza.xogame.presentation.testing.FakePurchaseRepository
import com.m7mdabaza.xogame.presentation.testing.FakeStatsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

@OptIn(ExperimentalCoroutinesApi::class)
class DailyChallengeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = 20_396L // 2025-11-04
    private val time = FakeTime(now = today * TimeProvider.MILLIS_PER_DAY + 12 * 3_600_000L)
    private val challenges = FakeChallengeRepository()
    private val achievements = FakeAchievementsRepository()
    private val feedback = FakeGameFeedback()
    private val playMove = PlayMoveUseCase(WinChecker())
    private val stats = FakeStatsRepository()
    private val prompts = FakePromptsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): DailyChallengeViewModel = DailyChallengeViewModel(
        getDailyChallenge = GetDailyChallengeUseCase(time),
        startChallenge = StartChallengeUseCase(StartGameUseCase(), playMove),
        playMove = playMove,
        computerMove = ComputerMoveUseCase(SearchAi()),
        recordSolved = RecordChallengeSolvedUseCase(challenges),
        evaluateAchievements = EvaluateAchievementsUseCase(stats, challenges, achievements, time),
        shouldRequestReview = ShouldRequestReviewUseCase(stats, prompts, time),
        recordReviewRequested = RecordReviewRequestedUseCase(prompts, time),
        observeProgress = ObserveChallengeProgressUseCase(challenges, time),
        observeAdsRemoved = ObserveAdsRemovedUseCase(FakePurchaseRepository()),
        feedback = feedback,
        adUnitIds = FakeAdUnitIds,
        computeDispatcher = dispatcher,
    ).also { advanceUntilIdle() }

    private val DailyChallengeViewModel.state get() = uiState.value
    private val DailyChallengeViewModel.challenge get() = state.challenge!!
    private val DailyChallengeViewModel.game get() = state.game!!

    /** The square where X wins at once, if any. */
    private fun winningSquare(game: GameState): Cell? = game.board.emptyCells.firstOrNull { cell ->
        ((playMove(game, cell) as MoveResult.Played).state.result as? GameResult.Win)?.player == Player.X
    }

    /** Plays a solution and lets the phone block one of the two threats. */
    private fun TestScope.playSolution(vm: DailyChallengeViewModel) {
        vm.onCellClick(vm.challenge.solutions.first())
        assertEquals(ChallengePhase.DEFENDING, vm.state.phase)
        assertFalse(vm.state.boardEnabled)
        advanceUntilIdle()
    }

    @Test
    fun `opens today's puzzle with X to move`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(ChallengePhase.PLAYING, vm.state.phase)
        assertEquals(today, vm.challenge.epochDay)
        assertEquals(Player.X, vm.game.turn)
        assertEquals(vm.challenge.setupMoves.size, vm.game.history.size)
        assertEquals("home", vm.state.bannerAdUnitId)
    }

    @Test
    fun `a solution, then taking the open threat, solves it and counts for today`() = runTest(dispatcher) {
        val vm = viewModel()
        playSolution(vm)
        assertEquals(ChallengePhase.PLAYING, vm.state.phase)
        assertEquals("the phone answered", vm.challenge.setupMoves.size + 2, vm.game.history.size)
        assertEquals(1, vm.state.movesPlayed)

        vm.onCellClick(winningSquare(vm.game)!!)
        assertEquals(ChallengePhase.SOLVED, vm.state.phase)
        advanceUntilIdle()
        assertEquals(setOf(today), challenges.solvedDays.value)
        assertTrue(vm.state.progress.solvedToday)
        assertEquals(1, vm.state.progress.currentStreak)
        assertEquals(ChallengeEvent.AchievementsUnlocked(listOf(Achievement.FIRST_CHALLENGE)), vm.events.first())
        assertEquals("end WIN", feedback.events.last())
    }

    @Test
    fun `a first move that is not a solution fails at once`() = runTest(dispatcher) {
        val vm = viewModel()
        val wrong = vm.game.board.emptyCells.first { it !in vm.challenge.solutions }
        vm.onCellClick(wrong)
        assertEquals(ChallengePhase.FAILED, vm.state.phase)
        assertEquals(FailReason.NO_FORK, vm.state.failReason)
        advanceTimeBy(5_000)
        assertEquals("the phone doesn't answer a failed attempt", vm.challenge.setupMoves.size + 1, vm.game.history.size)
        assertEquals(emptySet<Long>(), challenges.solvedDays.value)
    }

    @Test
    fun `missing the open threat on the second move fails`() = runTest(dispatcher) {
        val vm = viewModel()
        playSolution(vm)
        val win = winningSquare(vm.game)
        vm.onCellClick(vm.game.board.emptyCells.first { it != win })
        assertEquals(ChallengePhase.FAILED, vm.state.phase)
        assertEquals(FailReason.MISSED_WIN, vm.state.failReason)
        assertEquals("end LOSE", feedback.events.last())
    }

    @Test
    fun `try again restores the start position`() = runTest(dispatcher) {
        val vm = viewModel()
        val start = vm.game
        vm.onCellClick(vm.game.board.emptyCells.first { it !in vm.challenge.solutions })
        vm.onRetry()
        assertEquals(ChallengePhase.PLAYING, vm.state.phase)
        assertEquals(start, vm.game)
        assertEquals(0, vm.state.movesPlayed)
        assertNull(vm.state.failReason)
    }

    @Test
    fun `the board ignores taps while the phone defends and after the end`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCellClick(vm.challenge.solutions.first())
        val defending = vm.game
        vm.onCellClick(vm.game.board.emptyCells.first())
        assertEquals(defending, vm.game)
        advanceUntilIdle()

        vm.onCellClick(winningSquare(vm.game)!!)
        val solved = vm.game
        vm.onCellClick(vm.game.board.emptyCells.first())
        assertEquals(solved, vm.game)
    }

    @Test
    fun `solving asks for a Play review once the player has played enough`() = runTest(dispatcher) {
        stats.stats.value = Stats(mapOf((GameMode.TWO_PLAYERS to Level.EASY) to LevelStats(draws = 10)))
        val vm = viewModel()
        playSolution(vm)
        vm.onCellClick(winningSquare(vm.game)!!)
        advanceUntilIdle()
        assertEquals(ChallengeEvent.RequestReview, vm.events.take(2).toList().last())
        assertEquals(time.now, prompts.reviewRequestedAt.value)
    }

    @Test
    fun `no review request for a player with few games`() = runTest(dispatcher) {
        val vm = viewModel()
        playSolution(vm)
        vm.onCellClick(winningSquare(vm.game)!!)
        advanceUntilIdle()
        assertNull(prompts.reviewRequestedAt.value)
    }

    @Test
    fun `solving again the same day changes nothing`() = runTest(dispatcher) {
        challenges.solvedDays.value = setOf(today)
        val vm = viewModel()
        assertTrue(vm.state.progress.solvedToday)
        playSolution(vm)
        vm.onCellClick(winningSquare(vm.game)!!)
        advanceUntilIdle()
        assertEquals(setOf(today), challenges.solvedDays.value)
    }
}
