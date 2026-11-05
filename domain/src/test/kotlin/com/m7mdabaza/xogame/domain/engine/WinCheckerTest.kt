package com.m7mdabaza.xogame.domain.engine

import com.m7mdabaza.xogame.domain.model.Board
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.board
import com.m7mdabaza.xogame.domain.testing.cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WinCheckerTest {

    private val checker = WinChecker()

    private fun win(player: Player, vararg cells: Cell) = GameResult.Win(player, cells.toList())

    private fun Board.withAll(cells: List<Cell>, player: Player) = cells.fold(this) { b, c -> b.with(c, player) }

    // --- 3×3, 3 in a row ---------------------------------------------------

    @Test
    fun `3x3 every row, column and diagonal wins`() {
        val n = 3
        val lines = buildList {
            for (i in 0 until n) {
                add(List(n) { cell(i, it) })
                add(List(n) { cell(it, i) })
            }
            add(List(n) { cell(it, it) })
            add(List(n) { cell(it, n - 1 - it) })
        }
        for (line in lines) {
            val b = Board.empty(n).withAll(line, Player.O)
            val found = checker.findWin(b, 3)
            assertEquals("line $line", Player.O, found?.player)
            assertEquals("line $line", line.toSet(), found?.line?.toSet())
            // Checking through any cell of the line finds the same win.
            line.forEach { assertEquals(line.toSet(), checker.winThrough(b, it, 3)?.line?.toSet()) }
        }
    }

    @Test
    fun `winning line is ordered from one end to the other`() {
        assertEquals(
            win(Player.X, cell(0, 2), cell(1, 1), cell(2, 0)),
            checker.winThrough(board("..X/.X./X.."), cell(1, 1), 3),
        )
        assertEquals(
            win(Player.X, cell(0, 0), cell(0, 1), cell(0, 2)),
            checker.findWin(board("XXX/OO./..."), 3),
        )
    }

    @Test
    fun `no win on a drawn 3x3 board`() {
        val b = board("XOX/XOO/OXX")
        assertNull(checker.findWin(b, 3))
        assertEquals(GameResult.Draw, checker.resultAfter(b, cell(2, 2), 3))
    }

    @Test
    fun `two in a row is not a win`() {
        val b = board("XX./OO./...")
        assertNull(checker.findWin(b, 3))
        assertEquals(GameResult.InProgress, checker.resultAfter(b, cell(0, 1), 3))
    }

    @Test
    fun `mixed marks never form a line`() {
        assertNull(checker.findWin(board("XXO/.../..."), 3))
    }

    @Test
    fun `win on the last cell beats draw`() {
        val result = checker.resultAfter(board("XOX/OXO/OXX"), cell(2, 2), 3)
        assertEquals(Player.X, (result as GameResult.Win).player)
    }

    @Test
    fun `winThrough an empty cell or a cell off the line is null`() {
        val b = board("XXX/OO./...")
        assertNull(checker.winThrough(b, cell(2, 2), 3))
        assertNull(checker.winThrough(b, cell(1, 0), 3))
    }

    // --- 4×4, 4 in a row ---------------------------------------------------

    @Test
    fun `4x4 needs four in a row`() {
        assertNull(checker.findWin(board("XXX./OOO./..../...."), 4))
        assertEquals(Player.O, checker.findWin(board("XXX./OOOO/X.../...."), 4)?.player)
    }

    @Test
    fun `4x4 anti-diagonal`() {
        assertEquals(
            win(Player.O, cell(0, 3), cell(1, 2), cell(2, 1), cell(3, 0)),
            checker.winThrough(board("...O/..O./.O../O..."), cell(3, 0), 4),
        )
    }

    // --- 8×8, 5 in a row ---------------------------------------------------

    private val empty8 = Board.empty(BoardSize.EIGHT.size)

    @Test
    fun `8x8 four in a row is not enough`() {
        assertNull(checker.findWin(empty8.withAll(List(4) { cell(3, it + 2) }, Player.X), BoardSize.EIGHT.winLength))
    }

    @Test
    fun `8x8 five in a row anywhere wins, including at the edges`() {
        val cases = listOf(
            List(5) { cell(0, it + 3) }, // top edge, touching the right edge
            List(5) { cell(it + 3, 7) }, // right edge, touching the bottom
            List(5) { cell(it, it) }, // main diagonal from the top-left corner
            List(5) { cell(it + 3, 7 - it) }, // anti-diagonal down to the bottom edge
            List(5) { cell(7 - it, it) }, // anti-diagonal from the bottom-left corner
        )
        for (line in cases) {
            val b = empty8.withAll(line, Player.O)
            assertEquals("line $line", line.toSet(), checker.findWin(b, 5)?.line?.toSet())
            assertEquals("line $line", Player.O, checker.winThrough(b, line[2], 5)?.player)
        }
    }

    @Test
    fun `8x8 a broken line is not a win`() {
        val b = empty8.withAll(listOf(0, 1, 3, 4, 5).map { cell(4, it) }, Player.X).with(cell(4, 2), Player.O)
        assertNull(checker.findWin(b, 5))
    }

    @Test
    fun `8x8 six in a row wins and returns the whole run`() {
        val line = List(6) { cell(it + 1, it + 1) }
        assertEquals(
            win(Player.X, *line.toTypedArray()),
            checker.winThrough(empty8.withAll(line, Player.X), cell(4, 4), 5),
        )
    }

    @Test
    fun `lines do not wrap around the edge`() {
        // The end of row 0 plus the start of row 1 are consecutive in row-major
        // order, but they are not a line.
        val cells = listOf(cell(0, 5), cell(0, 6), cell(0, 7), cell(1, 0), cell(1, 1))
        assertNull(checker.findWin(empty8.withAll(cells, Player.X), 5))
    }
}
