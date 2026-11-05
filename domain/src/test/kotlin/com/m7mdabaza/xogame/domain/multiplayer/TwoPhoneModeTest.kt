package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.ChallengeProgress
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.testing.FakeStatsRepository
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import com.m7mdabaza.xogame.domain.usecase.RecordGameResultUseCase
import com.m7mdabaza.xogame.domain.usecase.UndoMoveUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** How the two-phone mode fits the game, stats and achievements. */
class TwoPhoneModeTest {

    /** The guest's phone: this player is O, X starts. */
    private val guestConfig = config(GameMode.TWO_PHONES, humanSymbol = Player.O, firstPlayer = Player.X)

    @Test
    fun `two-phone records are kept per board, without a level`() {
        assertEquals(BoardSize.entries.map { StatsKey(GameMode.TWO_PHONES, it) }, StatsKey.all.filter { it.mode == GameMode.TWO_PHONES })
        assertThrows(IllegalArgumentException::class.java) { StatsKey(GameMode.TWO_PHONES, BoardSize.THREE, Level.EASY) }
        assertThrows(IllegalArgumentException::class.java) { GameConfig(GameMode.TWO_PHONES, BoardSize.THREE, Level.EASY) }
    }

    @Test
    fun `this phone's player moves on their own turn only, and there is no computer`() {
        val game = newGame(guestConfig)
        assertFalse(game.isHumanTurn)
        assertFalse(game.isComputerTurn)
        val afterX = newGame(guestConfig, cell(0, 0))
        assertTrue(afterX.isHumanTurn)
        assertFalse(newGame(guestConfig, cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(2, 2), cell(1, 2)).humanBeatComputer)
    }

    @Test
    fun `there is no undo between two phones`() {
        assertNull(UndoMoveUseCase()(newGame(guestConfig, cell(0, 0), cell(1, 1))))
    }

    @Test
    fun `a round counts from this phone's side, with streaks`() = runTest {
        val repository = FakeStatsRepository()
        // O completes the middle row: a win for the guest.
        val won = newGame(guestConfig, cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(2, 2), cell(1, 2))
        RecordGameResultUseCase(repository)(won)
        assertEquals(LevelStats(wins = 1, currentStreak = 1, bestStreak = 1), repository.stats.first()[GameMode.TWO_PHONES, BoardSize.THREE])
    }

    @Test
    fun `two-phone games count towards 100 games, and a win unlocks Beat a friend`() {
        val stats = Stats(mapOf(StatsKey(GameMode.TWO_PHONES, BoardSize.FOUR) to LevelStats(wins = 1, losses = 2)))
        val challenge = ChallengeProgress(emptySet(), today = 100)
        assertEquals(3, stats.playedTotal)
        assertEquals(3, Achievement.GAMES_100.progress(stats, challenge))
        assertEquals(1, Achievement.BEAT_A_FRIEND.progress(stats, challenge))
        assertEquals(0, Achievement.TWO_PLAYER_GAME.progress(stats, challenge))
        assertEquals(0, Achievement.BEAT_A_FRIEND.progress(Stats(), challenge))
    }
}
