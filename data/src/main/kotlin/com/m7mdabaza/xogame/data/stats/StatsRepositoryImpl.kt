package com.m7mdabaza.xogame.data.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.m7mdabaza.xogame.data.di.StatsStore
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stats in their own Preferences DataStore: five int keys per mode and
 * level, e.g. `VS_COMPUTER_EASY_wins`. Absent keys read as 0.
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    @param:StatsStore private val store: DataStore<Preferences>,
) : StatsRepository {

    override val stats: Flow<Stats> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Stats(
                buildMap {
                    for (mode in GameMode.entries) for (level in Level.entries) {
                        val record = prefs.read(mode, level)
                        if (record != LevelStats()) put(mode to level, record)
                    }
                },
            )
        }
        .distinctUntilChanged()

    override suspend fun update(mode: GameMode, level: Level, transform: (LevelStats) -> LevelStats) {
        store.edit { prefs -> prefs.write(mode, level, transform(prefs.read(mode, level))) }
    }

    override suspend fun reset() {
        store.edit { it.clear() }
    }

    private fun Preferences.read(mode: GameMode, level: Level): LevelStats {
        val keys = Keys(mode, level)
        return LevelStats(
            wins = this[keys.wins] ?: 0,
            losses = this[keys.losses] ?: 0,
            draws = this[keys.draws] ?: 0,
            currentStreak = this[keys.currentStreak] ?: 0,
            bestStreak = this[keys.bestStreak] ?: 0,
        )
    }

    private fun MutablePreferences.write(mode: GameMode, level: Level, stats: LevelStats) {
        val keys = Keys(mode, level)
        this[keys.wins] = stats.wins
        this[keys.losses] = stats.losses
        this[keys.draws] = stats.draws
        this[keys.currentStreak] = stats.currentStreak
        this[keys.bestStreak] = stats.bestStreak
    }

    private class Keys(mode: GameMode, level: Level) {
        private val prefix = "${mode.name}_${level.name}_"
        val wins = intPreferencesKey(prefix + "wins")
        val losses = intPreferencesKey(prefix + "losses")
        val draws = intPreferencesKey(prefix + "draws")
        val currentStreak = intPreferencesKey(prefix + "current_streak")
        val bestStreak = intPreferencesKey(prefix + "best_streak")
    }
}
