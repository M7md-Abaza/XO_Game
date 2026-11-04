package com.m7mdabaza.xogame.domain.engine

import com.m7mdabaza.xogame.domain.model.Board
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameResult
import javax.inject.Inject
import kotlin.math.abs

/**
 * Finds `k` (or more) marks in a row on any N×N board: horizontally,
 * vertically or on either diagonal. Replaces the hard-coded line lists of the
 * legacy activities.
 */
class WinChecker @Inject constructor() {

    /**
     * The win that goes through [cell], or null. After a move only lines
     * through the new mark can have changed, so this is all a move needs to
     * check.
     */
    fun winThrough(board: Board, cell: Cell, winLength: Int): GameResult.Win? {
        val player = board[cell] ?: return null
        for ((dr, dc) in DIRECTIONS) {
            val start = walk(board, cell, -dr, -dc)
            val end = walk(board, cell, dr, dc)
            val length = maxOf(abs(end.row - start.row), abs(end.col - start.col)) + 1
            if (length >= winLength) {
                return GameResult.Win(player, List(length) { Cell(start.row + it * dr, start.col + it * dc) })
            }
        }
        return null
    }

    /** Any win on the board, or null. Scans every cell; use [winThrough] after a move. */
    fun findWin(board: Board, winLength: Int): GameResult.Win? =
        board.allCells.firstNotNullOfOrNull { if (board.isEmpty(it)) null else winThrough(board, it, winLength) }

    /**
     * The result of the board right after a mark was placed on [lastMove]:
     * a win through it, otherwise a draw if the board is full.
     */
    fun resultAfter(board: Board, lastMove: Cell, winLength: Int): GameResult =
        winThrough(board, lastMove, winLength)
            ?: if (board.isFull) GameResult.Draw else GameResult.InProgress

    /** The last cell reached from [from] in direction ([dr], [dc]) holding the same mark. */
    private fun walk(board: Board, from: Cell, dr: Int, dc: Int): Cell {
        val player = board[from]
        var current = from
        while (true) {
            val next = Cell(current.row + dr, current.col + dc)
            if (next !in board || board[next] != player) return current
            current = next
        }
    }

    private companion object {
        /** Right, down, down-right, down-left. Their opposites are covered by walking both ways. */
        val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
    }
}
