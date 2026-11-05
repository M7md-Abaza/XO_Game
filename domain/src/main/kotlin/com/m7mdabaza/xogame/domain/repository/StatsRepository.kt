package com.m7mdabaza.xogame.domain.repository

import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    /** All records, then every change. */
    val stats: Flow<Stats>

    /** Atomically replaces one record with [transform] applied to it. */
    suspend fun update(key: StatsKey, transform: (LevelStats) -> LevelStats)

    /** Deletes every record. */
    suspend fun reset()
}
