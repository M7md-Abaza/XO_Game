package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.board
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBoardTest {

    @Test
    fun `window counts per level`() {
        assertEquals(8, SearchBoard(3, 3).windows.size) // 3 rows, 3 columns, 2 diagonals
        assertEquals(10, SearchBoard(4, 4).windows.size)
        assertEquals(96, SearchBoard(8, 5).windows.size) // 32 + 32 + 16 + 16
    }

    @Test
    fun `every window is a straight run of winLength cells`() {
        val b = SearchBoard(8, 5)
        b.windows.forEach { w ->
            assertEquals(5, w.size)
            val dr = w[1] / 8 - w[0] / 8
            val dc = w[1] % 8 - w[0] % 8
            for (i in 1 until 5) {
                assertEquals(dr, w[i] / 8 - w[i - 1] / 8)
                assertEquals(dc, w[i] % 8 - w[i - 1] % 8)
            }
        }
    }

    @Test
    fun `neighbours are the cells around a cell`() {
        val n = SearchBoard(8, 5).neighbours(2)
        assertEquals(24, n[3 * 8 + 3].size) // 5x5 square minus the cell
        assertEquals(8, n[0].size) // corner: 3x3 minus the cell
        assertTrue(n[0].contains(2 * 8 + 2))
        assertEquals(3, SearchBoard(3, 3).neighbours(1)[0].size)
    }

    @Test
    fun `place and remove keep window counts`() {
        val b = SearchBoard(3, 3)
        b.place(4, Player.X.code)
        assertEquals(1, b.filled)
        assertEquals(4, b.windowsOfCell[4].size) // the centre is in 4 lines
        b.windowsOfCell[4].forEach { assertEquals(1, b.count(Player.X.code, it)) }
        b.remove(4, Player.X.code)
        assertEquals(0, b.filled)
        b.windows.indices.forEach { assertEquals(0, b.count(Player.X.code, it)) }
    }

    @Test
    fun `winning cells are distinct and ignore blocked lines`() {
        // X: (0,0) (0,1) (1,0) (1,1). (0,2) completes the row, (2,0) the column,
        // (2,2) the diagonal, (1,2) row 1, (2,1) column 1. O at (2,2) blocks the diagonal.
        val b = SearchBoard.from(board("XX./XX./..O"), 3)
        assertArrayEquals(intArrayOf(1 * 3 + 2, 2 * 3 + 0, 2 * 3 + 1, 0 * 3 + 2).sortedArray(), b.winningCells(Player.X.code).sortedArray())
        assertEquals(0, b.winningCells(Player.O.code).size)
    }
}
