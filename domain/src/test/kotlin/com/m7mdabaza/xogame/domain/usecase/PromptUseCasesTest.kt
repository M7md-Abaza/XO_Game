package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.domain.testing.FakePromptsRepository
import com.m7mdabaza.xogame.domain.testing.FakeStatsRepository
import com.m7mdabaza.xogame.domain.testing.FakeTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptUseCasesTest {

    private val stats = FakeStatsRepository()
    private val prompts = FakePromptsRepository()
    private val time = FakeTime(now = 1_000 * TimeProvider.MILLIS_PER_DAY)
    private val shouldRequestReview = ShouldRequestReviewUseCase(stats, prompts, time)
    private val recordReview = RecordReviewRequestedUseCase(prompts, time)
    private val shouldPromptForUpdate = ShouldPromptForUpdateUseCase(prompts, time)
    private val recordUpdate = RecordUpdatePromptedUseCase(prompts, time)

    private fun played(vsComputer: Int, twoPlayers: Int = 0) {
        stats.stats.value = Stats(
            mapOf(
                StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to LevelStats(wins = vsComputer),
                StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE) to LevelStats(draws = twoPlayers),
            ),
        )
    }

    @Test
    fun `no review request before enough games, in any mode`() = runTest {
        played(vsComputer = 2, twoPlayers = 2)
        assertFalse(shouldRequestReview())
        played(vsComputer = 3, twoPlayers = 2)
        assertTrue(shouldRequestReview())
    }

    @Test
    fun `a review is requested at most once every 90 days`() = runTest {
        played(vsComputer = 20)
        recordReview()
        assertEquals(time.now, prompts.reviewRequestedAt.value)
        time.now += 89 * TimeProvider.MILLIS_PER_DAY
        assertFalse(shouldRequestReview())
        time.now += TimeProvider.MILLIS_PER_DAY
        assertTrue(shouldRequestReview())
    }

    @Test
    fun `an update is offered at most once every 3 days`() = runTest {
        assertTrue(shouldPromptForUpdate())
        recordUpdate()
        time.now += 3 * TimeProvider.MILLIS_PER_DAY - 1
        assertFalse(shouldPromptForUpdate())
        time.now += 1
        assertTrue(shouldPromptForUpdate())
    }
}
