package com.m7mdabaza.xogame.data.progress

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.m7mdabaza.xogame.data.billing.FakeDataStore
import com.m7mdabaza.xogame.domain.model.Achievement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRepositoriesTest {

    @Test
    fun `solved days are added, not duplicated`() = runTest {
        val repo = ChallengeRepositoryImpl(FakeDataStore())
        assertEquals(emptySet<Long>(), repo.solvedDays.first())
        repo.markSolved(20_000)
        repo.markSolved(20_001)
        repo.markSolved(20_000)
        assertEquals(setOf(20_000L, 20_001L), repo.solvedDays.first())
    }

    @Test
    fun `achievements keep their first unlock time`() = runTest {
        val repo = AchievementsRepositoryImpl(FakeDataStore())
        repo.unlock(listOf(Achievement.FIRST_WIN), atMillis = 100)
        repo.unlock(listOf(Achievement.FIRST_WIN, Achievement.BEAT_HARD), atMillis = 200)
        assertEquals(mapOf(Achievement.FIRST_WIN to 100L, Achievement.BEAT_HARD to 200L), repo.unlocked.first())
    }

    @Test
    fun `keys of achievements that no longer exist are ignored`() = runTest {
        val store = FakeDataStore(mutablePreferencesOf(longPreferencesKey("REMOVED_ONE") to 5L, longPreferencesKey("GAMES_100") to 9L))
        assertEquals(mapOf(Achievement.GAMES_100 to 9L), AchievementsRepositoryImpl(store).unlocked.first())
    }
}
