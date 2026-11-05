package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.ChallengeProgress
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.domain.testing.FakeAchievementsRepository
import com.m7mdabaza.xogame.domain.testing.FakeChallengeRepository
import com.m7mdabaza.xogame.domain.testing.FakeStatsRepository
import com.m7mdabaza.xogame.domain.testing.FakeTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ProgressUseCasesTest {

    // --- Challenge streaks -------------------------------------------------------------

    @Test
    fun `streak counts back from today, or from yesterday while today is open`() {
        assertEquals(3, ChallengeProgress(setOf(8, 9, 10), today = 10).currentStreak)
        assertEquals(3, ChallengeProgress(setOf(7, 8, 9), today = 10).currentStreak)
        assertEquals(0, ChallengeProgress(setOf(7, 8), today = 10).currentStreak)
        assertTrue(ChallengeProgress(setOf(10), today = 10).solvedToday)
        assertFalse(ChallengeProgress(setOf(9), today = 10).solvedToday)
    }

    @Test
    fun `best streak is the longest run ever`() {
        val progress = ChallengeProgress(setOf(1, 2, 3, 4, 10, 11, 20), today = 30)
        assertEquals(4, progress.bestStreak)
        assertEquals(0, progress.currentStreak)
        assertEquals(7, progress.totalSolved)
        assertEquals(0, ChallengeProgress(emptySet(), today = 5).bestStreak)
    }

    @Test
    fun `today follows the device time zone`() {
        val utc = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh")) // UTC+3
            val lateEveningUtc = TimeProvider { 10 * TimeProvider.MILLIS_PER_DAY + 22 * 3_600_000L }
            assertEquals("22:00 UTC is already the next day in Riyadh", 11, lateEveningUtc.todayEpochDay())
        } finally {
            TimeZone.setDefault(utc)
        }
    }

    @Test
    fun `progress use case and recording a solved day`() = runTest {
        val repo = FakeChallengeRepository()
        val time = FakeTime(now = 100 * TimeProvider.MILLIS_PER_DAY + 1)
        RecordChallengeSolvedUseCase(repo)(time.todayEpochDay())
        val progress = ObserveChallengeProgressUseCase(repo, time)().first()
        assertTrue(progress.solvedToday)
        assertEquals(1, progress.currentStreak)
    }

    // --- Daily challenge -------------------------------------------------------------------

    @Test
    fun `the challenge starts with X to move on the 8x8 board`() {
        val challenge = GetDailyChallengeUseCase(FakeTime(now = 20_123 * TimeProvider.MILLIS_PER_DAY))()
        val state = StartChallengeUseCase(StartGameUseCase(), PlayMoveUseCase(WinChecker()))(challenge)
        assertEquals(8, state.board.size)
        assertEquals(challenge.setupMoves.size, state.history.size)
        assertEquals(Player.X, state.turn)
        assertTrue(state.isHumanTurn)
        assertFalse(state.isOver)
    }

    // --- Achievements ----------------------------------------------------------------------

    private val stats = FakeStatsRepository()
    private val challenges = FakeChallengeRepository()
    private val achievements = FakeAchievementsRepository()
    private val time = FakeTime(now = 50 * TimeProvider.MILLIS_PER_DAY)
    private val evaluate = EvaluateAchievementsUseCase(stats, challenges, achievements, time)
    private val observe = ObserveAchievementsUseCase(stats, challenges, achievements, time)

    private fun stats(vararg entries: Pair<StatsKey, LevelStats>) = Stats(entries.toMap())

    @Test
    fun `nothing is unlocked at the start`() = runTest {
        assertEquals(emptyList<Achievement>(), evaluate())
        assertTrue(observe().first().none { it.isUnlocked })
    }

    @Test
    fun `achievements unlock once their target is reached, and only once`() = runTest {
        stats.stats.value = stats(StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to LevelStats(wins = 1, currentStreak = 1, bestStreak = 1))
        assertEquals(listOf(Achievement.FIRST_WIN), evaluate())
        assertEquals(emptyList<Achievement>(), evaluate())

        stats.stats.value = stats(
            StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to LevelStats(wins = 5, currentStreak = 5, bestStreak = 5),
            StatsKey(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.HARD) to LevelStats(wins = 1),
            StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE) to LevelStats(draws = 1),
        )
        assertEquals(listOf(Achievement.BEAT_HARD, Achievement.WIN_STREAK_5, Achievement.TWO_PLAYER_GAME), evaluate())

        challenges.solvedDays.value = (44L..50L).toSet()
        assertEquals(listOf(Achievement.FIRST_CHALLENGE, Achievement.CHALLENGE_STREAK_7), evaluate())
    }

    @Test
    fun `unlocks are kept after the stats are reset, and show progress while locked`() = runTest {
        stats.stats.value = stats(StatsKey(GameMode.VS_COMPUTER, BoardSize.FOUR, Level.MEDIUM) to LevelStats(wins = 1, losses = 36))
        evaluate()
        stats.stats.value = Stats()

        val list = observe().first()
        assertEquals(Achievement.entries, list.map { it.achievement })
        assertTrue(list.first { it.achievement == Achievement.BEAT_MEDIUM }.isUnlocked)
        assertEquals(time.now, list.first { it.achievement == Achievement.FIRST_WIN }.unlockedAtMillis)

        stats.stats.value = stats(StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE) to LevelStats(wins = 20, losses = 17))
        val games = observe().first().first { it.achievement == Achievement.GAMES_100 }
        assertEquals(37, games.progress)
        assertFalse(games.isUnlocked)
    }

    @Test
    fun `progress is capped at the target`() {
        val progress = Achievement.GAMES_100.progress(
            stats(StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to LevelStats(wins = 500)),
            ChallengeProgress(emptySet(), today = 0),
        )
        assertEquals(100, progress)
    }
}
