package com.m7mdabaza.xogame.domain.model

/**
 * The board a game is played on: its side length and how many marks in a row
 * win. Chosen separately from the computer's [Level].
 */
enum class BoardSize(val size: Int, val winLength: Int) {
    THREE(size = 3, winLength = 3),
    FOUR(size = 4, winLength = 4),
    EIGHT(size = 8, winLength = 5);

    val cellCount: Int get() = size * size
}
