package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.DailyChallenge
import com.m7mdabaza.xogame.domain.model.Player
import kotlin.random.Random

/**
 * Builds the daily "win in 2" puzzle from the date alone, so every phone gets
 * the same one: a seeded random game is played until a position appears
 * where X (to move) has one to three moves that make two winning threats at
 * once. Neither side may already have a winning move in the position.
 *
 * Changing this algorithm changes future puzzles (past days are not stored).
 */
internal object ChallengeGenerator {

    private const val SIZE = 8
    private const val WIN_LENGTH = 5
    private const val MAX_ATTEMPTS = 500

    private val X = Player.X.code
    private val O = Player.O.code

    fun generate(epochDay: Long): DailyChallenge {
        val random = Random(epochDay * 1_000_003L + 7)
        repeat(MAX_ATTEMPTS) {
            attempt(random, epochDay)?.let { return it }
        }
        return fallback(epochDay)
    }

    /** The first moves that force a win in 2 for X (who moves next) in [board]. */
    fun solutions(board: SearchBoard): List<Int> =
        (0 until board.cellCount).filter { move ->
            board[move] == EMPTY && run {
                board.place(move, X)
                val forks = board.winningCells(X).size >= 2
                board.remove(move, X)
                forks
            }
        }

    private fun attempt(random: Random, epochDay: Long): DailyChallenge? {
        val board = SearchBoard(SIZE, WIN_LENGTH)
        val near = board.neighbours(2)
        val moves = ArrayList<Int>()
        // An even number of moves, so X is to move: 8 to 14 marks on the board.
        val setupMoves = 2 * random.nextInt(4, 8)
        var player = X
        repeat(setupMoves) {
            val move = pickMove(board, near, player, random) ?: return null
            board.place(move, player)
            moves += move
            player = opponentOf(player)
        }
        // A clean puzzle: nobody can win at once, and nobody has to block.
        if (board.winningCells(X).isNotEmpty() || board.winningCells(O).isNotEmpty()) return null
        val solutions = solutions(board)
        if (solutions.size !in 1..3) return null
        return DailyChallenge(epochDay, moves.map(::cellOf), solutions.map(::cellOf).toSet())
    }

    /**
     * X builds: one of its four best attacking cells. O plays more loosely
     * (often a random nearby cell), so X's shapes survive long enough to make
     * a puzzle. Moves that would win at once are never played.
     */
    private fun pickMove(board: SearchBoard, near: Array<IntArray>, player: Int, random: Random): Int? {
        val winning = board.winningCells(player).toSet()
        val candidates = (0 until board.cellCount).filter { cell ->
            board[cell] == EMPTY && cell !in winning && (board.filled == 0 && isCentral(cell) || near[cell].any { board[it] != EMPTY })
        }
        if (candidates.isEmpty()) return null
        if (player == O && random.nextInt(100) < 55) return candidates[random.nextInt(candidates.size)]
        val ranked = candidates.sortedByDescending { score(board, it, player) }
        return ranked[random.nextInt(minOf(4, ranked.size))]
    }

    /** How many open lines [cell] extends for [player], weighted by length. */
    private fun score(board: SearchBoard, cell: Int, player: Int): Int {
        val opponent = opponentOf(player)
        var score = 0
        for (w in board.windowsOfCell[cell]) {
            if (board.count(opponent, w) == 0) {
                val mine = board.count(player, w)
                score += 1 shl (3 * mine)
            }
        }
        return score
    }

    private fun isCentral(cell: Int): Boolean = cell / SIZE in 2..5 && cell % SIZE in 2..5

    private fun cellOf(index: Int) = Cell(index / SIZE, index % SIZE)

    /** A fixed, known-good puzzle, used only if no position turns up (practically never). */
    private fun fallback(epochDay: Long): DailyChallenge {
        // X: an open three on row 3 plus one more mark; O: scattered marks.
        val moves = listOf(Cell(3, 2), Cell(2, 2), Cell(3, 3), Cell(4, 5), Cell(3, 4), Cell(6, 1), Cell(5, 5), Cell(1, 6))
        val board = SearchBoard(SIZE, WIN_LENGTH)
        moves.forEachIndexed { i, cell -> board.place(cell.row * SIZE + cell.col, if (i % 2 == 0) X else O) }
        return DailyChallenge(epochDay, moves, solutions(board).map(::cellOf).toSet())
    }
}
