package com.m7mdabaza.xogame.data.progress

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.m7mdabaza.xogame.data.di.AchievementsStore
import com.m7mdabaza.xogame.data.di.ChallengeStore
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.repository.AchievementsRepository
import com.m7mdabaza.xogame.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Solved daily challenges as a set of day numbers (stored as strings). */
@Singleton
class ChallengeRepositoryImpl @Inject constructor(
    @param:ChallengeStore private val store: DataStore<Preferences>,
) : ChallengeRepository {

    override val solvedDays: Flow<Set<Long>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[SOLVED].orEmpty().mapNotNull { it.toLongOrNull() }.toSet() }
        .distinctUntilChanged()

    override suspend fun markSolved(epochDay: Long) {
        store.edit { it[SOLVED] = it[SOLVED].orEmpty() + epochDay.toString() }
    }

    private companion object {
        val SOLVED = stringSetPreferencesKey("solved_days")
    }
}

/** One unlock-time key per achievement, e.g. `FIRST_WIN`. Unknown keys (removed achievements) are ignored. */
@Singleton
class AchievementsRepositoryImpl @Inject constructor(
    @param:AchievementsStore private val store: DataStore<Preferences>,
) : AchievementsRepository {

    override val unlocked: Flow<Map<Achievement, Long>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Achievement.entries.mapNotNull { achievement -> prefs[key(achievement)]?.let { achievement to it } }.toMap()
        }
        .distinctUntilChanged()

    override suspend fun unlock(achievements: Collection<Achievement>, atMillis: Long) {
        store.edit { prefs ->
            achievements.forEach { achievement ->
                if (prefs[key(achievement)] == null) prefs[key(achievement)] = atMillis
            }
        }
    }

    private fun key(achievement: Achievement) = longPreferencesKey(achievement.name)
}
