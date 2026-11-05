package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartGameUseCaseTest {

    @Test
    fun `the board follows the board size`() {
        BoardSize.entries.forEach { boardSize ->
            val state = start(config(boardSize = boardSize))
            assertEquals(boardSize.size, state.board.size)
            assertTrue(state.board.isEmpty)
            assertTrue(state.history.isEmpty())
            assertEquals(GameResult.InProgress, state.result)
        }
    }

    @Test
    fun `first player moves first`() {
        assertEquals(Player.O, start(config(firstPlayer = Player.O)).turn)
        assertEquals(Player.X, start(config(firstPlayer = Player.X)).turn)
    }

    @Test
    fun `vs computer, computer first`() {
        val state = start(config(GameMode.VS_COMPUTER, humanSymbol = Player.X, firstPlayer = Player.O))
        assertEquals(Player.O, state.config.computerSymbol)
        assertTrue(state.isComputerTurn)
        assertFalse(state.isHumanTurn)
    }

    @Test
    fun `vs computer, human plays O and starts`() {
        val state = start(config(GameMode.VS_COMPUTER, humanSymbol = Player.O, firstPlayer = Player.O))
        assertEquals(Player.X, state.config.computerSymbol)
        assertTrue(state.isHumanTurn)
        assertFalse(state.isComputerTurn)
    }

    @Test
    fun `two players, every turn is a human turn`() {
        val state = start(config(GameMode.TWO_PLAYERS, firstPlayer = Player.O))
        assertNull(state.config.computerSymbol)
        assertTrue(state.isHumanTurn)
        assertFalse(state.isComputerTurn)
    }
}
