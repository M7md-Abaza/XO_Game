package com.m7mdabaza.xogame.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardSizeTest {

    @Test
    fun `boards keep the original sizes and win lengths`() {
        assertEquals(3 to 3, BoardSize.THREE.size to BoardSize.THREE.winLength)
        assertEquals(4 to 4, BoardSize.FOUR.size to BoardSize.FOUR.winLength)
        assertEquals(8 to 5, BoardSize.EIGHT.size to BoardSize.EIGHT.winLength)
    }

    @Test
    fun `a win always fits on the board`() {
        BoardSize.entries.forEach { assertTrue(it.winLength <= it.size) }
    }

    @Test
    fun `cell count is the board area`() {
        assertEquals(64, BoardSize.EIGHT.cellCount)
    }

    @Test
    fun `a level is needed against the computer and only there`() {
        GameConfig(GameMode.VS_COMPUTER, BoardSize.FOUR, Level.HARD)
        GameConfig(GameMode.TWO_PLAYERS, BoardSize.FOUR, null)
        assertThrows(IllegalArgumentException::class.java) { GameConfig(GameMode.VS_COMPUTER, BoardSize.FOUR, null) }
        assertThrows(IllegalArgumentException::class.java) { GameConfig(GameMode.TWO_PLAYERS, BoardSize.FOUR, Level.EASY) }
        assertThrows(IllegalArgumentException::class.java) { StatsKey(GameMode.VS_COMPUTER, BoardSize.FOUR) }
    }

    @Test
    fun `every board and level has its own record, two-player boards one each`() {
        assertEquals(9 + 3, StatsKey.all.size)
        assertEquals(StatsKey.all.size, StatsKey.all.toSet().size)
        assertEquals(StatsKey(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.MEDIUM), GameConfig(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.MEDIUM).statsKey)
        assertEquals(StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE), GameConfig(GameMode.TWO_PLAYERS, BoardSize.THREE, null).statsKey)
    }
}
