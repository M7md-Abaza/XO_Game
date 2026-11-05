package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.domain.testing.FakeSettingsRepository
import com.m7mdabaza.xogame.domain.testing.FakeStatsRepository
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StatsUseCasesTest {

    private val repository = FakeStatsRepository()
    private val record = RecordGameResultUseCase(repository)
    private val observe = ObserveStatsUseCase(repository)

    /** X: top row, O: middle row; X wins on move 5. */
    private val xWins = arrayOf(cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))

    /** O wins: X plays (0,0) (2,2) (2,1), O plays the middle row. */
    private val oWins = arrayOf(cell(0, 0), cell(1, 0), cell(2, 2), cell(1, 1), cell(2, 1), cell(1, 2))

    private val draw = arrayOf(
        cell(0, 0), cell(0, 1), cell(0, 2), cell(1, 1), cell(1, 0),
        cell(1, 2), cell(2, 1), cell(2, 0), cell(2, 2),
    )

    @Test
    fun `against the computer the record is the human's`() = runTest {
        val humanX = config(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY, humanSymbol = Player.X)
        record(newGame(humanX, *xWins))
        record(newGame(humanX, *oWins))
        record(newGame(humanX, *draw))

        val humanO = config(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY, humanSymbol = Player.O)
        record(newGame(humanO, *oWins)) // the human (O) wins

        assertEquals(
            LevelStats(wins = 2, losses = 1, draws = 1, currentStreak = 1, bestStreak = 1),
            observe().first()[GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY],
        )
    }

    @Test
    fun `two-player wins are X's and losses are O's`() = runTest {
        val twoPlayers = config(GameMode.TWO_PLAYERS, BoardSize.THREE)
        record(newGame(twoPlayers, *xWins))
        record(newGame(twoPlayers, *oWins))
        val stats = observe().first()[GameMode.TWO_PLAYERS, BoardSize.THREE]
        assertEquals(1, stats.wins)
        assertEquals(1, stats.losses)
        assertEquals(LevelStats(), observe().first()[GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY])
    }

    @Test
    fun `an unfinished round cannot be recorded`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { record(newGame(config(), cell(1, 1))) }
        }
    }

    @Test
    fun `reset clears everything`() = runTest {
        record(newGame(config(), *xWins))
        ResetStatsUseCase(repository)()
        assertEquals(0, observe().first().played(GameMode.TWO_PLAYERS))
    }

    @Test
    fun `settings use cases read and update the repository`() = runTest {
        val settings = FakeSettingsRepository()
        UpdateSettingsUseCase(settings).invoke { it.copy(theme = ThemeMode.DARK, soundEnabled = false) }
        assertEquals(Settings(theme = ThemeMode.DARK, soundEnabled = false), ObserveSettingsUseCase(settings)().first())
    }
}
