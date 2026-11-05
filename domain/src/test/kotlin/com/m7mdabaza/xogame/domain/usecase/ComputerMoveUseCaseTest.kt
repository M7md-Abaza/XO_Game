package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.ai.SearchAi
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import com.m7mdabaza.xogame.domain.testing.playing
import com.m7mdabaza.xogame.domain.testing.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComputerMoveUseCaseTest {

    private val computerMove = ComputerMoveUseCase(SearchAi(Random(7)))

    @Test
    fun `plays an empty cell on every board and level, as X or O`() {
        for (boardSize in BoardSize.entries) for (level in Level.entries) for (human in Player.entries) {
            val state = start(config(GameMode.VS_COMPUTER, boardSize, level, humanSymbol = human, firstPlayer = human.opponent))
            val move = computerMove(state)
            assertTrue(move in state.board && state.board.isEmpty(move))
            assertEquals(human.opponent, state.playing(move).board[move])
        }
    }

    @Test
    fun `computer blocks the human`() {
        // Human X: (0,0) (0,1); computer O: (1,1). O must block (0,2).
        val state = newGame(config(GameMode.VS_COMPUTER), cell(0, 0), cell(1, 1), cell(0, 1))
        repeat(20) { assertEquals(cell(0, 2), ComputerMoveUseCase(SearchAi(Random(it)))(state)) }
    }

    @Test
    fun `refuses to move on the human's turn or after the game ended`() {
        assertThrows(IllegalArgumentException::class.java) { computerMove(start(config(GameMode.VS_COMPUTER))) }
        assertThrows(IllegalArgumentException::class.java) { computerMove(start(config(GameMode.TWO_PLAYERS))) }
        val won = newGame(config(GameMode.VS_COMPUTER), cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))
        assertThrows(IllegalArgumentException::class.java) { computerMove(won) }
    }
}
