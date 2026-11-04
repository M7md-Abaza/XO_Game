package com.m7mdabaza.xogame.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelTest {

    @Test
    fun `levels keep the original board sizes and win lengths`() {
        assertEquals(3 to 3, Level.EASY.boardSize to Level.EASY.winLength)
        assertEquals(4 to 4, Level.MEDIUM.boardSize to Level.MEDIUM.winLength)
        assertEquals(8 to 5, Level.HARD.boardSize to Level.HARD.winLength)
    }

    @Test
    fun `a win always fits on the board`() {
        Level.entries.forEach { assertTrue(it.winLength <= it.boardSize) }
    }

    @Test
    fun `cell count is the board area`() {
        assertEquals(64, Level.HARD.cellCount)
    }
}
