package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Level

/**
 * How hard the AI tries.
 *
 * @property searchDepth how many moves ahead it looks, counting its own move.
 *   A depth of at least the number of empty cells solves the position exactly.
 * @property candidateLimit how many of the most promising cells it searches
 *   at each step (all of them by default).
 * @property neighbourhood when set, only empty cells within this distance of
 *   an existing mark are considered (big boards only; distant cells are
 *   almost never good moves).
 * @property mistakeChance per move, the chance it plays a random candidate
 *   instead of searching. Winning moves and blocks of an immediate loss are
 *   always played, whatever this is.
 * @property missedBlockChance when the opponent threatens to win next move,
 *   the chance it overlooks that and plays elsewhere. It never misses its
 *   own winning move.
 */
data class Difficulty(
    val searchDepth: Int,
    val candidateLimit: Int = Int.MAX_VALUE,
    val neighbourhood: Int? = null,
    val mistakeChance: Double = 0.0,
    val missedBlockChance: Double = 0.0,
) {
    init {
        require(searchDepth >= 1) { "searchDepth must be at least 1" }
        require(candidateLimit >= 1) { "candidateLimit must be at least 1" }
        require(mistakeChance in 0.0..1.0) { "mistakeChance must be a probability" }
        require(missedBlockChance in 0.0..1.0) { "missedBlockChance must be a probability" }
    }
}

/**
 * The AI strength for each board and [Level] (the computer opponent), and
 * for each board (the hint helper). Calibrated head to head in milestone 10
 * (see `SearchAiTest`).
 */
object DifficultyPolicy {

    /**
     * The computer opponent. Easy and Medium are beatable on every board,
     * Medium clearly less often than Easy.
     *
     * - 3×3: random mistakes only; perfect play beats Easy in about 1 game of
     *   5 and Medium in about 1 of 12. Hard is a perfect player and never loses.
     * - 4×4: four in a row there is a draw with sensible play; a computer that
     *   contests every line and blocks every threat almost never loses (about
     *   1 game in 100). So Easy and Medium also make more random moves and
     *   sometimes overlook a threat: a strong player beats Easy in about 1
     *   game of 4 and Medium in about 1 of 12. Hard never misses a block.
     * - 8×8: shallower searches and more mistakes for the easier levels; each
     *   level clearly beats the one below. Hard looks 5 moves ahead (odd
     *   depths play better on 8×8, see [hint]), over slightly fewer candidates
     *   than hints so hints stay a little stronger.
     */
    fun computer(boardSize: BoardSize, level: Level): Difficulty = when (boardSize) {
        BoardSize.THREE -> when (level) {
            Level.EASY -> Difficulty(searchDepth = 9, mistakeChance = 0.35)
            Level.MEDIUM -> Difficulty(searchDepth = 9, mistakeChance = 0.15)
            Level.HARD -> Difficulty(searchDepth = 9)
        }
        BoardSize.FOUR -> when (level) {
            Level.EASY -> Difficulty(searchDepth = 1, mistakeChance = 0.5, missedBlockChance = 0.5)
            Level.MEDIUM -> Difficulty(searchDepth = 4, mistakeChance = 0.2, missedBlockChance = 0.2)
            Level.HARD -> Difficulty(searchDepth = 6, mistakeChance = 0.05)
        }
        BoardSize.EIGHT -> when (level) {
            Level.EASY -> Difficulty(searchDepth = 1, candidateLimit = 8, neighbourhood = 2, mistakeChance = 0.35)
            Level.MEDIUM -> Difficulty(searchDepth = 3, candidateLimit = 8, neighbourhood = 2, mistakeChance = 0.15)
            Level.HARD -> Difficulty(searchDepth = 5, candidateLimit = 10, neighbourhood = 2, mistakeChance = 0.05)
        }
    }

    /**
     * Hints: full strength, no mistakes, whatever the computer's level.
     * Perfect on 3×3.
     *
     * 8×8 was calibrated head to head (randomised openings): odd depths beat
     * the even depth above them, and depth 6 gains nothing over 5 at 2–5× the
     * time. Depth 5 beats the Hard computer clearly and takes a few ms per
     * move on a desktop JVM.
     */
    fun hint(boardSize: BoardSize): Difficulty = when (boardSize) {
        BoardSize.THREE -> Difficulty(searchDepth = 9)
        BoardSize.FOUR -> Difficulty(searchDepth = 6)
        BoardSize.EIGHT -> Difficulty(searchDepth = 5, candidateLimit = 12, neighbourhood = 2)
    }
}
