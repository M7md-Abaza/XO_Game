package com.m7mdabaza.xogame.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.m7mdabaza.xogame.data.di.AdPolicyStore
import com.m7mdabaza.xogame.domain.model.AdPolicyState
import com.m7mdabaza.xogame.domain.repository.AdPolicyRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The interstitial bookkeeping: the last-shown time and the rounds since
 * then are stored (they matter across app restarts); the rounds of this
 * session live only in memory, so they start at 0 with every app start.
 */
@Singleton
class AdPolicyRepositoryImpl @Inject constructor(
    @param:AdPolicyStore private val store: DataStore<Preferences>,
) : AdPolicyRepository {

    private val roundsThisSession = MutableStateFlow(0)

    override val state: Flow<AdPolicyState> = combine(
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it },
        roundsThisSession,
    ) { prefs, session ->
        AdPolicyState(
            lastInterstitialAtMillis = prefs[LAST_SHOWN],
            roundsSinceInterstitial = prefs[ROUNDS_SINCE] ?: 0,
            roundsThisSession = session,
        )
    }

    override suspend fun recordRoundFinished() {
        roundsThisSession.update { it + 1 }
        store.edit { it[ROUNDS_SINCE] = (it[ROUNDS_SINCE] ?: 0) + 1 }
    }

    override suspend fun recordInterstitialShown(atMillis: Long) {
        store.edit {
            it[LAST_SHOWN] = atMillis
            it[ROUNDS_SINCE] = 0
        }
    }

    private companion object {
        val LAST_SHOWN = longPreferencesKey("last_interstitial_at")
        val ROUNDS_SINCE = intPreferencesKey("rounds_since_interstitial")
    }
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
