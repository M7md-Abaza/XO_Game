package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameState
import javax.inject.Inject
import kotlin.random.Random

/**
 * The game AI, for every board size: an alpha-beta (negamax) search over the
 * "windows" of [SearchBoard].
 *
 * - It always takes a win in one and always blocks the opponent's win in one.
 * - It recognises a double threat (two winning cells for the opponent) as lost.
 * - Small boards: every empty cell is searched. 3×3 is searched to the end,
 *   so it plays perfectly.
 * - 8×8: only the [Difficulty.candidateLimit] best cells near existing marks,
 *   ranked by how many open lines they extend or block, are searched.
 * - Positions it cannot search to the end are scored by counting each
 *   player's marks in every window still open to them.
 * - Among equally good moves it picks at random, so games vary.
 *
 * It is stateless and thread-safe. Run it off the main thread on 8×8.
 */
class SearchAi internal constructor(private val random: Random) {

    @Inject
    constructor() : this(Random.Default)

    /** The move for the player whose turn it is in [state]. */
    fun chooseMove(state: GameState, difficulty: Difficulty): Cell {
        require(!state.isOver) { "The game is over" }
        val search = Search(state, difficulty)
        val index = search.forcedMoves()?.random(random)
            ?: if (difficulty.mistakeChance > 0 && random.nextDouble() < difficulty.mistakeChance) {
                search.candidates(state.turn.code, limit = Int.MAX_VALUE).random(random)
            } else {
                search.bestMoves().random(random)
            }
        return search.cellOf(index)
    }

    /** Every move the search rates best (no mistakes, no random pick). For tests. */
    internal fun bestMoves(state: GameState, difficulty: Difficulty): List<Cell> {
        require(!state.isOver) { "The game is over" }
        val search = Search(state, difficulty)
        return (search.forcedMoves() ?: search.bestMoves()).map(search::cellOf)
    }
}

private class Search(state: GameState, private val difficulty: Difficulty) {

    private val board = SearchBoard.from(state.board, state.config.level.winLength)
    private val me = state.turn.code
    private val winLength = board.winLength
    private val neighbours = difficulty.neighbourhood?.let(board::neighbours)

    /** `weight[c]`: value of a window holding c of one player's marks and none of the other's. */
    private val weight = IntArray(winLength + 1) { c -> if (c == 0) 0 else pow10(c - 1) }

    fun cellOf(index: Int) = Cell(index / board.size, index % board.size)

    /** Winning cells if there are any, otherwise the cells that block an immediate loss, otherwise null. */
    fun forcedMoves(): IntArray? =
        board.winningCells(me).takeIf { it.isNotEmpty() }
            ?: board.winningCells(opponentOf(me)).takeIf { it.isNotEmpty() }

    /** All root moves with the best search score. */
    fun bestMoves(): IntArray {
        val opponent = opponentOf(me)
        var best = -INFINITY
        var bestMoves = IntArray(0)
        for (move in candidates(me, difficulty.candidateLimit)) {
            board.place(move, me)
            // A window of (best - 1, ∞) gives an exact score for every move
            // at least as good as the best so far, so ties are all found.
            val score = -negamax(opponent, difficulty.searchDepth - 1, ply = 1, alpha = -INFINITY, beta = 1 - best)
            board.remove(move, me)
            when {
                score > best -> { best = score; bestMoves = intArrayOf(move) }
                score == best -> bestMoves += move
            }
        }
        return bestMoves
    }

    /**
     * The score of the position for [player], who is to move, looking
     * [depth] more moves ahead. The previous move did not win.
     */
    private fun negamax(player: Int, depth: Int, ply: Int, alpha: Int, beta: Int): Int {
        if (board.isFull) return 0
        if (board.winningCells(player).isNotEmpty()) return WIN - ply
        val opponent = opponentOf(player)
        val threats = board.winningCells(opponent)
        // We can block only one; the opponent completes the other next move.
        if (threats.size >= 2) return -(WIN - ply - 1)
        if (depth <= 0) return evaluate(player)

        val moves = if (threats.size == 1) threats else candidates(player, difficulty.candidateLimit)
        var a = alpha
        var best = -INFINITY
        for (move in moves) {
            board.place(move, player)
            val score = -negamax(opponent, depth - 1, ply + 1, -beta, -a)
            board.remove(move, player)
            if (score > best) {
                best = score
                if (score > a) a = score
                if (a >= beta) break
            }
        }
        return best
    }

    /** Static score for [player] (to move): open windows count for whoever owns them. */
    private fun evaluate(player: Int): Int {
        val opponent = opponentOf(player)
        var score = 0
        for (w in board.windows.indices) {
            val mine = board.count(player, w)
            val theirs = board.count(opponent, w)
            if (theirs == 0) score += weight[mine] else if (mine == 0) score -= weight[theirs]
        }
        return score
    }

    /**
     * Empty cells worth considering for [player], most promising first, at
     * most [limit] of them. A cell scores for every open window it extends
     * for [player] (attack) or for the opponent (defence).
     */
    fun candidates(player: Int, limit: Int): IntArray {
        val opponent = opponentOf(player)
        val cells = IntArray(board.cellCount)
        val scores = IntArray(board.cellCount)
        var count = 0
        for (i in 0 until board.cellCount) {
            if (board[i] != EMPTY || !isNearMarks(i)) continue
            var score = 0
            for (w in board.windowsOfCell[i]) {
                val mine = board.count(player, w)
                val theirs = board.count(opponent, w)
                if (theirs == 0) score += weight[mine + 1]
                if (mine == 0) score += weight[theirs + 1] * 4 / 5
            }
            // Insertion sort, highest score first (at most 64 cells).
            var j = count++
            while (j > 0 && scores[j - 1] < score) {
                cells[j] = cells[j - 1]
                scores[j] = scores[j - 1]
                j--
            }
            cells[j] = i
            scores[j] = score
        }
        return cells.copyOf(minOf(count, limit))
    }

    private fun isNearMarks(index: Int): Boolean {
        val near = neighbours ?: return true
        if (board.filled == 0) return true
        return near[index].any { board[it] != EMPTY }
    }

    private companion object {
        const val WIN = 1_000_000
        const val INFINITY = 1_000_000_000

        fun pow10(n: Int): Int = (1..n).fold(1) { acc, _ -> acc * 10 }
    }
}
