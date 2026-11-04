package com.m7mdabaza.xogame.domain.model

/**
 * Difficulty level. Each level is a board size plus how many marks in a row
 * win. Against the computer it also selects the AI strength.
 */
enum class Level(val boardSize: Int, val winLength: Int) {
    EASY(boardSize = 3, winLength = 3),
    MEDIUM(boardSize = 4, winLength = 4),
    HARD(boardSize = 8, winLength = 5);

    val cellCount: Int get() = boardSize * boardSize
}
