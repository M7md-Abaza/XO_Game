package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.Board
import com.m7mdabaza.xogame.domain.model.Player

/** Player codes used inside the search: 0 is an empty cell. */
internal const val EMPTY = 0

internal val Player.code: Int get() = ordinal + 1

internal fun opponentOf(code: Int): Int = 3 - code

/**
 * A mutable board for the AI search. Cells are indexes `row * size + col`.
 *
 * Every run of [winLength] cells that can hold a win (a "window") is listed
 * once, and the board keeps how many marks each player has in each window.
 * Placing or removing a mark only updates the windows through that cell, so
 * win detection, threat detection and evaluation are cheap.
 */
internal class SearchBoard(val size: Int, val winLength: Int) {

    val cellCount = size * size
    private val cells = IntArray(cellCount)
    var filled = 0
        private set

    /** Cell indexes of every window. */
    val windows: Array<IntArray>

    /** For each cell, the indexes of the windows that contain it. */
    val windowsOfCell: Array<IntArray>

    /** `counts[playerCode][window]`: how many marks the player has in the window. */
    private val counts: Array<IntArray>

    init {
        val found = ArrayList<IntArray>()
        for (r in 0 until size) for (c in 0 until size) for ((dr, dc) in DIRECTIONS) {
            val endRow = r + dr * (winLength - 1)
            val endCol = c + dc * (winLength - 1)
            if (endRow in 0 until size && endCol in 0 until size) {
                found += IntArray(winLength) { (r + dr * it) * size + (c + dc * it) }
            }
        }
        windows = found.toTypedArray()
        val perCell = Array(cellCount) { ArrayList<Int>() }
        windows.forEachIndexed { w, windowCells -> windowCells.forEach { perCell[it] += w } }
        windowsOfCell = Array(cellCount) { perCell[it].toIntArray() }
        counts = Array(3) { IntArray(windows.size) }
    }

    val isFull: Boolean get() = filled == cellCount

    operator fun get(index: Int): Int = cells[index]

    fun count(player: Int, window: Int): Int = counts[player][window]

    fun place(index: Int, player: Int) {
        check(cells[index] == EMPTY) { "Cell $index is taken" }
        cells[index] = player
        filled++
        for (w in windowsOfCell[index]) counts[player][w]++
    }

    fun remove(index: Int, player: Int) {
        check(cells[index] == player) { "Cell $index does not hold $player" }
        cells[index] = EMPTY
        filled--
        for (w in windowsOfCell[index]) counts[player][w]--
    }

    /** The distinct empty cells where [player] would complete a window, i.e. win at once. */
    fun winningCells(player: Int): IntArray {
        val opponent = opponentOf(player)
        var result = IntArray(0)
        for (w in windows.indices) {
            if (counts[player][w] == winLength - 1 && counts[opponent][w] == 0) {
                val cell = windows[w].first { cells[it] == EMPTY }
                if (cell !in result) result += cell
            }
        }
        return result
    }

    /** For each cell, the cells within Chebyshev distance [distance] (excluding itself). */
    fun neighbours(distance: Int): Array<IntArray> = Array(cellCount) { i ->
        // Plain loops, not buildList: inside buildList `size` would mean the list's size.
        val row = i / size
        val col = i % size
        val result = ArrayList<Int>()
        for (r in maxOf(0, row - distance)..minOf(size - 1, row + distance)) {
            for (c in maxOf(0, col - distance)..minOf(size - 1, col + distance)) {
                if (r != row || c != col) result += r * size + c
            }
        }
        result.toIntArray()
    }

    companion object {
        private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

        fun from(board: Board, winLength: Int): SearchBoard =
            SearchBoard(board.size, winLength).apply {
                board.allCells.forEach { cell ->
                    board[cell]?.let { place(cell.row * size + cell.col, it.code) }
                }
            }
    }
}
