package com.m7mdabaza.xogame.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LevelStatsTest {

    private fun LevelStats.after(vararg outcomes: RoundOutcome) = outcomes.fold(this) { s, o -> s.record(o) }

    @Test
    fun `counts wins losses and draws`() {
        val s = LevelStats().after(RoundOutcome.WIN, RoundOutcome.LOSS, RoundOutcome.DRAW, RoundOutcome.WIN)
        assertEquals(2, s.wins)
        assertEquals(1, s.losses)
        assertEquals(1, s.draws)
        assertEquals(4, s.played)
        assertEquals(0.5f, s.winRate!!, 0.0001f)
    }

    @Test
    fun `no win rate before the first round`() {
        assertNull(LevelStats().winRate)
    }

    @Test
    fun `a streak counts consecutive wins and the best is kept`() {
        val s = LevelStats().after(RoundOutcome.WIN, RoundOutcome.WIN, RoundOutcome.WIN)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.bestStreak)

        val afterLoss = s.record(RoundOutcome.LOSS)
        assertEquals(0, afterLoss.currentStreak)
        assertEquals(3, afterLoss.bestStreak)

        val again = afterLoss.after(RoundOutcome.WIN, RoundOutcome.WIN)
        assertEquals(2, again.currentStreak)
        assertEquals(3, again.bestStreak)
    }

    @Test
    fun `a draw ends the streak`() {
        val s = LevelStats().after(RoundOutcome.WIN, RoundOutcome.WIN, RoundOutcome.DRAW)
        assertEquals(0, s.currentStreak)
        assertEquals(2, s.bestStreak)
    }

    @Test
    fun `stats default to empty records and sum per mode`() {
        val stats = Stats(
            mapOf(
                StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to LevelStats(wins = 2, draws = 1),
                StatsKey(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.HARD) to LevelStats(losses = 4),
                StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE) to LevelStats(wins = 1),
            ),
        )
        assertEquals(LevelStats(), stats[GameMode.TWO_PLAYERS, BoardSize.EIGHT])
        assertEquals(7, stats.played(GameMode.VS_COMPUTER))
        assertEquals(1, stats.played(GameMode.TWO_PLAYERS))
    }
}
