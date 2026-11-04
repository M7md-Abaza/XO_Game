package com.m7mdabaza.xogame.domain.model

import com.m7mdabaza.xogame.domain.testing.board
import com.m7mdabaza.xogame.domain.testing.cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    @Test
    fun `empty board has no marks`() {
        val b = Board.empty(4)
        assertEquals(16, b.emptyCells.size)
        assertTrue(b.isEmpty)
        assertFalse(b.isFull)
        assertNull(b[cell(3, 3)])
    }

    @Test
    fun `with returns a new board and leaves the original unchanged`() {
        val original = Board.empty(3)
        val changed = original.with(cell(1, 2), Player.O)
        assertNull(original[cell(1, 2)])
        assertEquals(Player.O, changed[1, 2])
        assertEquals(8, changed.emptyCells.size)
    }

    @Test
    fun `placing on a taken cell is not allowed`() {
        val b = board("X../.../...")
        assertThrows(IllegalArgumentException::class.java) { b.with(cell(0, 0), Player.O) }
    }

    @Test
    fun `cells outside the board are rejected`() {
        val b = Board.empty(3)
        assertFalse(cell(3, 0) in b)
        assertFalse(cell(0, -1) in b)
        assertThrows(IllegalArgumentException::class.java) { b[cell(3, 0)] }
    }

    @Test
    fun `without clears a cell`() {
        assertEquals(board("X../.../..."), board("X../.O./...").without(cell(1, 1)))
    }

    @Test
    fun `full board`() {
        assertTrue(board("XOX/XOO/OXX").isFull)
    }

    @Test
    fun `boards with the same marks are equal and print as rows`() {
        val b = board("X.O/.X./..O")
        assertEquals(board("X.O/.X./..O"), b)
        assertEquals(board("X.O/.X./..O").hashCode(), b.hashCode())
        assertEquals("X.O/.X./..O", b.toString())
    }

    @Test
    fun `player opponent`() {
        assertEquals(Player.O, Player.X.opponent)
        assertEquals(Player.X, Player.O.opponent)
    }
}
