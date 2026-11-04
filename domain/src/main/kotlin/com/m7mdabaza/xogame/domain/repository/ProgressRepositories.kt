package com.m7mdabaza.xogame.domain.repository

import com.m7mdabaza.xogame.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

/** Which daily challenges were solved. */
interface ChallengeRepository {
    /** Solved days, as days since 1970-01-01. */
    val solvedDays: Flow<Set<Long>>

    suspend fun markSolved(epochDay: Long)
}

/** Unlocked achievements. Kept separately from the stats, so they survive a stats reset. */
interface AchievementsRepository {
    /** Unlock time (epoch millis) of every unlocked achievement. */
    val unlocked: Flow<Map<Achievement, Long>>

    /** Unlocks [achievements] at [atMillis]; already unlocked ones keep their first time. */
    suspend fun unlock(achievements: Collection<Achievement>, atMillis: Long)
}
