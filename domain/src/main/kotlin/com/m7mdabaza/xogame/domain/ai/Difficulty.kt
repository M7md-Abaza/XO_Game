package com.m7mdabaza.xogame.domain.ai

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
 */
data class Difficulty(
    val searchDepth: Int,
    val candidateLimit: Int = Int.MAX_VALUE,
    val neighbourhood: Int? = null,
    val mistakeChance: Double = 0.0,
) {
    init {
        require(searchDepth >= 1) { "searchDepth must be at least 1" }
        require(candidateLimit >= 1) { "candidateLimit must be at least 1" }
        require(mistakeChance in 0.0..1.0) { "mistakeChance must be a probability" }
    }
}

/** The AI strength for each [Level]: the computer opponent and the hint helper. */
object DifficultyPolicy {

    /**
     * The computer opponent. Beatable on Easy, harder on the bigger boards.
     * Hard looks 5 moves ahead (odd depths play better on 8×8, see [hint]),
     * over slightly fewer candidates than hints so hints stay a little stronger.
     */
    fun computer(level: Level): Difficulty = when (level) {
        Level.EASY -> Difficulty(searchDepth = 9, mistakeChance = 0.35)
        Level.MEDIUM -> Difficulty(searchDepth = 4, mistakeChance = 0.15)
        Level.HARD -> Difficulty(searchDepth = 5, candidateLimit = 10, neighbourhood = 2, mistakeChance = 0.05)
    }

    /**
     * Hints: full strength, no mistakes. Perfect on 3×3.
     *
     * 8×8 was calibrated head to head (randomised openings): odd depths beat
     * the even depth above them, and depth 6 gains nothing over 5 at 2–5× the
     * time. Depth 5 beats the Hard computer clearly and takes a few ms per
     * move on a desktop JVM.
     */
    fun hint(level: Level): Difficulty = when (level) {
        Level.EASY -> Difficulty(searchDepth = 9)
        Level.MEDIUM -> Difficulty(searchDepth = 6)
        Level.HARD -> Difficulty(searchDepth = 5, candidateLimit = 12, neighbourhood = 2)
    }
}
