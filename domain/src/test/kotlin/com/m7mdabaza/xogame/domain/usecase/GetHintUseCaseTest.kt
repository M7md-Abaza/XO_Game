package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.ai.SearchAi
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import com.m7mdabaza.xogame.domain.testing.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class GetHintUseCaseTest {

    private val hint = GetHintUseCase(SearchAi(Random(3)))

    @Test
    fun `hint shows the winning cell`() {
        // X: (0,0) (0,1); O: (1,1) (2,2). X to move wins at (0,2).
        val state = newGame(config(GameMode.VS_COMPUTER), cell(0, 0), cell(1, 1), cell(0, 1), cell(2, 2))
        assertEquals(cell(0, 2), hint(state))
    }

    @Test
    fun `hint works for both players in two-player mode`() {
        // O to move must block (0,2).
        val state = newGame(config(GameMode.TWO_PLAYERS), cell(0, 0), cell(1, 1), cell(0, 1))
        assertEquals(cell(0, 2), hint(state))
    }

    @Test
    fun `hint on an empty board for every board size`() {
        BoardSize.entries.forEach { boardSize ->
            val state = start(config(GameMode.VS_COMPUTER, boardSize))
            val move = hint(state)
            assertNotNull(move)
            assertEquals(true, state.board.isEmpty(move!!))
        }
    }

    @Test
    fun `no hint while the computer is to move or after the game ended`() {
        val computerTurn = start(config(GameMode.VS_COMPUTER, humanSymbol = Player.X, firstPlayer = Player.O))
        assertNull(hint(computerTurn))
        val won = newGame(config(), cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))
        assertNull(hint(won))
    }
}
