package com.m7mdabaza.xogame.domain.model

/**
 * The puzzle of one day, the same on every phone: an 8×8 position where X
 * moves next and can force a win in 2 moves, by making two winning threats
 * at once (the defender can block only one).
 */
data class DailyChallenge(
    /** The day it belongs to, as days since 1970-01-01 (local calendar). */
    val epochDay: Long,
    /** The moves that build the position, alternating X and O, X first. X moves next. */
    val setupMoves: List<Cell>,
    /** Every first move that forces the win. */
    val solutions: Set<Cell>,
) {
    val boardSize: BoardSize get() = BoardSize.EIGHT

    /** The phone defends at full Hard strength. */
    val level: Level get() = Level.HARD
    val player: Player get() = Player.X

    /** The player's moves allowed to win, the first one included. */
    val movesToWin: Int get() = 2
}

/** Which days' challenges were solved, seen from [today]. */
data class ChallengeProgress(val solvedDays: Set<Long>, val today: Long) {

    val solvedToday: Boolean get() = today in solvedDays

    val totalSolved: Int get() = solvedDays.size

    /** Consecutive solved days up to today, or up to yesterday while today is still open. */
    val currentStreak: Int
        get() {
            var day = if (solvedToday) today else today - 1
            var streak = 0
            while (day in solvedDays) {
                streak++
                day--
            }
            return streak
        }

    /** The longest run of consecutive solved days ever. */
    val bestStreak: Int
        get() {
            var best = 0
            var run = 0
            var previous: Long? = null
            for (day in solvedDays.sorted()) {
                run = if (previous != null && day == previous + 1) run + 1 else 1
                best = maxOf(best, run)
                previous = day
            }
            return best
        }
}

/** Local achievements. [target] is the number to reach (1 for one-off ones). */
enum class Achievement(val target: Int) {
    /** Win a game against the phone. */
    FIRST_WIN(1),

    /** Beat the phone on Medium, on any board. */
    BEAT_MEDIUM(1),

    /** Beat the phone on Hard, on any board (3×3 Hard never loses, so 4×4 or 8×8). */
    BEAT_HARD(1),

    /** Win 5 games in a row against the phone on one board at one level. */
    WIN_STREAK_5(5),

    /** Finish 100 games in any mode. */
    GAMES_100(100),

    /** Finish a two-player game (on one phone). */
    TWO_PLAYER_GAME(1),

    /** Win a two-phone game against a friend. */
    BEAT_A_FRIEND(1),

    /** Solve a daily challenge. */
    FIRST_CHALLENGE(1),

    /** Solve the daily challenge 7 days in a row. */
    CHALLENGE_STREAK_7(7),
    ;

    /** How far along the player is, 0..[target], from their stats and challenge history. */
    fun progress(stats: Stats, challenge: ChallengeProgress): Int {
        val vs = GameMode.VS_COMPUTER
        val value = when (this) {
            FIRST_WIN -> stats.all(vs).sumOf { it.wins }
            BEAT_MEDIUM -> BoardSize.entries.sumOf { stats[vs, it, Level.MEDIUM].wins }
            BEAT_HARD -> BoardSize.entries.sumOf { stats[vs, it, Level.HARD].wins }
            WIN_STREAK_5 -> stats.all(vs).maxOf { it.bestStreak }
            GAMES_100 -> stats.playedTotal
            TWO_PLAYER_GAME -> stats.played(GameMode.TWO_PLAYERS)
            BEAT_A_FRIEND -> stats.all(GameMode.TWO_PHONES).sumOf { it.wins }
            FIRST_CHALLENGE -> challenge.totalSolved
            CHALLENGE_STREAK_7 -> challenge.bestStreak
        }
        return value.coerceAtMost(target)
    }
}

/** An achievement as shown in the list. */
data class AchievementStatus(
    val achievement: Achievement,
    val progress: Int,
    /** When it was unlocked (epoch millis), or null while locked. Unlocks are kept even if stats are reset. */
    val unlockedAtMillis: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAtMillis != null
}
