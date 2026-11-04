package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.DailyChallenge
import com.m7mdabaza.xogame.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeGeneratorTest {

    private val x = Player.X.code
    private val o = Player.O.code

    private val fallbackMoves = listOf(Cell(3, 2), Cell(2, 2), Cell(3, 3), Cell(4, 5), Cell(3, 4), Cell(6, 1), Cell(5, 5), Cell(1, 6))

    private fun boardOf(challenge: DailyChallenge): SearchBoard {
        val board = SearchBoard(8, 5)
        challenge.setupMoves.forEachIndexed { i, cell ->
            val index = cell.row * 8 + cell.col
            assertEquals("day ${challenge.epochDay}: move $i on a taken cell", EMPTY, board[index])
            board.place(index, if (i % 2 == 0) x else o)
        }
        return board
    }

    /** True if every O reply leaves X an immediate win (X has just played). */
    private fun forcesWin(board: SearchBoard): Boolean =
        (0 until board.cellCount).filter { board[it] == EMPTY }.all { reply ->
            board.place(reply, o)
            val wins = board.winningCells(x).isNotEmpty()
            board.remove(reply, o)
            wins
        }

    @Test
    fun `the same day always gives the same puzzle`() {
        assertEquals(ChallengeGenerator.generate(20_000), ChallengeGenerator.generate(20_000))
    }

    @Test
    fun `different days give different puzzles`() {
        val puzzles = (20_000L until 20_030L).map { ChallengeGenerator.generate(it).setupMoves }
        assertTrue("at least 28 distinct puzzles in 30 days", puzzles.toSet().size >= 28)
        assertNotEquals(puzzles[0], puzzles[1])
    }

    @Test
    fun `a year of puzzles are all valid win-in-2 positions`() {
        val started = System.nanoTime()
        for (day in 20_000L until 20_365L) {
            val challenge = ChallengeGenerator.generate(day)
            assertEquals(day, challenge.epochDay)
            assertEquals("even number of setup moves, so X is to move", 0, challenge.setupMoves.size % 2)
            assertNotEquals("day $day fell back to the fixed puzzle", fallbackMoves, challenge.setupMoves)

            val board = boardOf(challenge)
            assertTrue("day $day: X can already win", board.winningCells(x).isEmpty())
            assertTrue("day $day: X would have to block", board.winningCells(o).isEmpty())
            assertTrue("day $day: 1-3 solutions, was ${challenge.solutions.size}", challenge.solutions.size in 1..3)

            for (index in 0 until board.cellCount) {
                if (board[index] != EMPTY) continue
                board.place(index, x)
                val forced = forcesWin(board)
                board.remove(index, x)
                val cell = Cell(index / 8, index % 8)
                assertEquals("day $day: $cell", cell in challenge.solutions, forced)
            }
        }
        val ms = (System.nanoTime() - started) / 1_000_000
        println("365 puzzles generated and checked in $ms ms")
    }

    @Test
    fun `the fallback puzzle is valid too`() {
        // Reach the fallback through its public shape: a fixed open three on row 3.
        val board = SearchBoard(8, 5)
        fallbackMoves
            .forEachIndexed { i, c -> board.place(c.row * 8 + c.col, if (i % 2 == 0) x else o) }
        val solutions = ChallengeGenerator.solutions(board).map { Cell(it / 8, it % 8) }.toSet()
        assertEquals(setOf(Cell(3, 1), Cell(3, 5)), solutions)
    }
}
