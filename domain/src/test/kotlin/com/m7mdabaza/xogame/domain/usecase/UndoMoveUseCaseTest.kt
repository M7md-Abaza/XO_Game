package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.board
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import com.m7mdabaza.xogame.domain.testing.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoMoveUseCaseTest {

    private val undo = UndoMoveUseCase()

    private val vsComputer = config(GameMode.VS_COMPUTER, humanSymbol = Player.X, firstPlayer = Player.X)

    @Test
    fun `nothing to undo at the start`() {
        assertNull(undo(start(config())))
        assertNull(undo(start(vsComputer)))
    }

    @Test
    fun `two players undo exactly one move`() {
        val undone = undo(newGame(config(), cell(0, 0), cell(1, 1)))!!
        assertEquals(board("X../.../..."), undone.board)
        assertEquals(Player.O, undone.turn)
        assertEquals(newGame(config(), cell(0, 0)), undone)
    }

    @Test
    fun `vs computer undo removes the computer reply and the human move`() {
        val undone = undo(newGame(vsComputer, cell(1, 1), cell(0, 0), cell(2, 2), cell(0, 2)))!!
        assertEquals(newGame(vsComputer, cell(1, 1), cell(0, 0)), undone)
        assertTrue(undone.isHumanTurn)
    }

    @Test
    fun `vs computer undo before the computer replied removes only the human move`() {
        val state = newGame(vsComputer, cell(1, 1), cell(0, 0), cell(2, 2))
        assertEquals(newGame(vsComputer, cell(1, 1), cell(0, 0)), undo(state))
    }

    @Test
    fun `vs computer when the computer opened, its opening move is kept`() {
        val computerFirst = config(GameMode.VS_COMPUTER, humanSymbol = Player.X, firstPlayer = Player.O)
        assertNull(undo(newGame(computerFirst, cell(1, 1))))

        val undone = undo(newGame(computerFirst, cell(1, 1), cell(0, 0), cell(2, 2)))!!
        assertEquals(newGame(computerFirst, cell(1, 1)), undone)
        assertTrue(undone.isHumanTurn)
    }

    @Test
    fun `vs computer with the human playing O`() {
        val humanO = config(GameMode.VS_COMPUTER, humanSymbol = Player.O, firstPlayer = Player.X)
        val state = newGame(humanO, cell(1, 1), cell(0, 0), cell(2, 2))
        assertEquals(newGame(humanO, cell(1, 1)), undo(state))
    }

    @Test
    fun `undo after a win reopens the game`() {
        val won = newGame(config(), cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(0, 2))
        assertTrue(won.result is GameResult.Win)
        val undone = undo(won)!!
        assertEquals(GameResult.InProgress, undone.result)
        assertEquals(Player.X, undone.turn)
    }

    @Test
    fun `vs computer undo after the computer won`() {
        // Human X: (1,0) (2,0) (2,2); computer O: (0,0) (0,1) (0,2) wins.
        val lost = newGame(vsComputer, cell(1, 0), cell(0, 0), cell(2, 0), cell(0, 1), cell(2, 2), cell(0, 2))
        assertEquals(Player.O, (lost.result as GameResult.Win).player)
        assertEquals(newGame(vsComputer, cell(1, 0), cell(0, 0), cell(2, 0), cell(0, 1)), undo(lost))
    }

    @Test
    fun `undo after a draw reopens the game`() {
        val drawn = newGame(
            config(),
            cell(0, 0), cell(0, 1), cell(0, 2), cell(1, 1), cell(1, 0),
            cell(1, 2), cell(2, 1), cell(2, 0), cell(2, 2),
        )
        assertEquals(GameResult.Draw, drawn.result)
        assertEquals(GameResult.InProgress, undo(drawn)!!.result)
    }

    @Test
    fun `undo all the way back gives the starting state`() {
        var state = newGame(config(), cell(0, 0), cell(1, 1), cell(2, 2))
        repeat(3) { state = undo(state)!! }
        assertEquals(start(config()), state)
        assertNull(undo(state))
    }
}
