package com.m7mdabaza.xogame.domain.model

import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    /** X takes the top row; O plays the middle row. */
    private val xWins = arrayOf(cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))

    @Test
    fun `the human beat the computer only when the human's symbol won`() {
        assertTrue(newGame(config(GameMode.VS_COMPUTER, humanSymbol = Player.X), *xWins).humanBeatComputer)
        assertFalse(newGame(config(GameMode.VS_COMPUTER, humanSymbol = Player.O), *xWins).humanBeatComputer)
    }

    @Test
    fun `no win against the computer in two-player games, unfinished games or draws`() {
        assertFalse(newGame(config(GameMode.TWO_PLAYERS), *xWins).humanBeatComputer)
        assertFalse(newGame(config(GameMode.VS_COMPUTER), cell(0, 0)).humanBeatComputer)
        val draw = arrayOf(
            cell(0, 0), cell(0, 1), cell(0, 2), cell(1, 1), cell(1, 0),
            cell(1, 2), cell(2, 1), cell(2, 0), cell(2, 2),
        )
        val drawn = newGame(config(GameMode.VS_COMPUTER), *draw)
        assertTrue(drawn.isOver)
        assertFalse(drawn.humanBeatComputer)
    }
}
