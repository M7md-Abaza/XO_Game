package com.m7mdabaza.xogame.domain.repository

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Stats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    /** All records, then every change. */
    val stats: Flow<Stats>

    /** Atomically replaces one record with [transform] applied to it. */
    suspend fun update(mode: GameMode, level: Level, transform: (LevelStats) -> LevelStats)

    /** Deletes every record. */
    suspend fun reset()
}
