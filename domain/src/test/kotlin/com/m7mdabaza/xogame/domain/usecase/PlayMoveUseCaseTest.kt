package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.Move
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.board
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import com.m7mdabaza.xogame.domain.testing.play
import com.m7mdabaza.xogame.domain.testing.playing
import com.m7mdabaza.xogame.domain.testing.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayMoveUseCaseTest {

    @Test
    fun `a move places the current player's mark and passes the turn`() {
        val state = (play(start(config()), cell(1, 1)) as MoveResult.Played).state
        assertEquals(Player.X, state.board[cell(1, 1)])
        assertEquals(listOf(Move(cell(1, 1), Player.X)), state.history)
        assertEquals(Move(cell(1, 1), Player.X), state.lastMove)
        assertEquals(Player.O, state.turn)
        assertEquals(GameResult.InProgress, state.result)
    }

    @Test
    fun `turns alternate starting from the first player`() {
        val state = newGame(config(firstPlayer = Player.O), cell(0, 0), cell(0, 1), cell(0, 2))
        assertEquals(board("OXO/.../..."), state.board)
        assertEquals(Player.X, state.turn)
    }

    @Test
    fun `occupied cell is rejected`() {
        assertEquals(MoveResult.CellOccupied, play(newGame(config(), cell(0, 0)), cell(0, 0)))
    }

    @Test
    fun `out of bounds is rejected`() {
        val state = start(config())
        assertEquals(MoveResult.OutOfBounds, play(state, cell(3, 0)))
        assertEquals(MoveResult.OutOfBounds, play(state, cell(-1, 2)))
    }

    @Test
    fun `completing a line wins and ends the game`() {
        // X: (0,0) (0,1) (0,2); O: (1,0) (1,1)
        val state = newGame(config(), cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))
        assertEquals(GameResult.Win(Player.X, listOf(cell(0, 0), cell(0, 1), cell(0, 2))), state.result)
        assertTrue(state.isOver)
        assertFalse(state.isHumanTurn)
        assertFalse(state.isComputerTurn)
        assertEquals(MoveResult.GameOver, play(state, cell(2, 2)))
    }

    @Test
    fun `filling the board without a line is a draw`() {
        val state = newGame(
            config(),
            cell(0, 0), cell(0, 1), cell(0, 2), cell(1, 1), cell(1, 0),
            cell(1, 2), cell(2, 1), cell(2, 0), cell(2, 2),
        )
        assertEquals(board("XOX/XOO/OXX"), state.board)
        assertEquals(GameResult.Draw, state.result)
        assertEquals(MoveResult.GameOver, play(state, cell(0, 0)))
    }

    @Test
    fun `medium needs four in a row`() {
        val three = newGame(config(level = Level.MEDIUM), cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))
        assertEquals(GameResult.InProgress, three.result)
        assertEquals(Player.X, (three.playing(cell(1, 2), cell(0, 3)).result as GameResult.Win).player)
    }

    @Test
    fun `hard needs five in a row`() {
        var state = start(config(level = Level.HARD))
        repeat(4) { state = state.playing(cell(0, it), cell(7, it)) }
        assertEquals(GameResult.InProgress, state.result)
        assertEquals(GameResult.Win(Player.X, List(5) { cell(0, it) }), state.playing(cell(0, 4)).result)
    }

    @Test
    fun `hard is a draw only when all 64 cells are filled`() {
        // X where (col / 2 + row) is even: rows read XXOOXXOO (flipped on the next
        // row), columns alternate every cell and both diagonals alternate in
        // pairs, so no line is longer than 2.
        val cells = (0 until 8).flatMap { r -> (0 until 8).map { c -> cell(r, c) } }
        val (xs, os) = cells.partition { (it.col / 2 + it.row) % 2 == 0 }
        assertEquals(32, xs.size)

        var state = start(config(level = Level.HARD))
        xs.zip(os).forEach { (x, o) ->
            assertEquals(GameResult.InProgress, state.result)
            state = state.playing(x, o)
        }
        assertEquals(64, state.history.size)
        assertEquals(GameResult.Draw, state.result)
    }

    @Test
    fun `vs computer turn flags follow the moves`() {
        val state = newGame(config(GameMode.VS_COMPUTER, humanSymbol = Player.X), cell(1, 1))
        assertTrue(state.isComputerTurn)
        assertFalse(state.isHumanTurn)
        assertTrue(state.playing(cell(0, 0)).isHumanTurn)
    }

    @Test
    fun `rejected moves leave the state untouched`() {
        val state = newGame(config(), cell(0, 0))
        play(state, cell(0, 0))
        assertEquals(newGame(config(), cell(0, 0)), state)
    }
}
