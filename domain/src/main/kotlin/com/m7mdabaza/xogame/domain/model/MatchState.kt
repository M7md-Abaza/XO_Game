package com.m7mdabaza.xogame.domain.model

import kotlin.math.abs

/** How many rounds make up a match. */
enum class MatchLength(val rounds: Int) {
    /** Independent rounds with a running score that never ends on its own. */
    SINGLE(1),
    BEST_OF_3(3),
    BEST_OF_5(5),
}

/**
 * The score of a match. Symbols stay with the same participant for the whole
 * match, so the score is kept per [Player].
 *
 * A best-of-N match lasts at most N rounds, draws included, and ends early
 * once the leader can no longer be caught. If the wins are level after N
 * rounds the match is drawn. [MatchLength.SINGLE] never ends: it is a
 * running tally of independent rounds.
 */
data class MatchState(
    val length: MatchLength,
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0,
) {
    val roundsPlayed: Int get() = xWins + oWins + draws

    fun wins(player: Player): Int = if (player == Player.X) xWins else oWins

    /** The 1-based number of the round being played (or about to be). */
    val currentRound: Int get() = roundsPlayed + 1

    val isOver: Boolean
        get() = when (length) {
            MatchLength.SINGLE -> false
            else -> {
                val remaining = length.rounds - roundsPlayed
                remaining <= 0 || abs(xWins - oWins) > remaining
            }
        }

    /** The match winner once [isOver], or null while it runs or when it is drawn. */
    val winner: Player?
        get() = when {
            !isOver -> null
            xWins > oWins -> Player.X
            oWins > xWins -> Player.O
            else -> null
        }

    /** Adds the finished round [result]. */
    fun record(result: GameResult): MatchState {
        check(!isOver) { "The match is already over" }
        return when (result) {
            is GameResult.Win -> if (result.player == Player.X) copy(xWins = xWins + 1) else copy(oWins = oWins + 1)
            GameResult.Draw -> copy(draws = draws + 1)
            GameResult.InProgress -> throw IllegalArgumentException("The round has not finished")
        }
    }
}
