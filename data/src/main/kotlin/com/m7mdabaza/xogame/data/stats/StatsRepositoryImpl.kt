package com.m7mdabaza.xogame.data.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.m7mdabaza.xogame.data.di.StatsStore
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stats in their own Preferences DataStore: five int keys per record, e.g.
 * `VS_COMPUTER_EIGHT_HARD_wins` against the phone, or `TWO_PLAYERS_THREE_wins`
 * (no level) in two-player mode. Absent keys read as 0.
 *
 * Unreleased builds before milestone 10 kept one record per level (e.g.
 * `VS_COMPUTER_EASY_wins`); those keys are never read and go with a reset.
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
                    for (key in StatsKey.all) {
                        val record = prefs.read(key)
                        if (record != LevelStats()) put(key, record)
                    }
                },
            )
        }
        .distinctUntilChanged()

    override suspend fun update(key: StatsKey, transform: (LevelStats) -> LevelStats) {
        store.edit { prefs -> prefs.write(key, transform(prefs.read(key))) }
    }

    override suspend fun reset() {
        store.edit { it.clear() }
    }

    private fun Preferences.read(key: StatsKey): LevelStats {
        val keys = Keys(key)
        return LevelStats(
            wins = this[keys.wins] ?: 0,
            losses = this[keys.losses] ?: 0,
            draws = this[keys.draws] ?: 0,
            currentStreak = this[keys.currentStreak] ?: 0,
            bestStreak = this[keys.bestStreak] ?: 0,
        )
    }

    private fun MutablePreferences.write(key: StatsKey, stats: LevelStats) {
        val keys = Keys(key)
        this[keys.wins] = stats.wins
        this[keys.losses] = stats.losses
        this[keys.draws] = stats.draws
        this[keys.currentStreak] = stats.currentStreak
        this[keys.bestStreak] = stats.bestStreak
    }

    private class Keys(key: StatsKey) {
        private val prefix = listOfNotNull(key.mode.name, key.boardSize.name, key.level?.name).joinToString("_", postfix = "_")
        val wins = intPreferencesKey(prefix + "wins")
        val losses = intPreferencesKey(prefix + "losses")
        val draws = intPreferencesKey(prefix + "draws")
        val currentStreak = intPreferencesKey(prefix + "current_streak")
        val bestStreak = intPreferencesKey(prefix + "best_streak")
    }
}
