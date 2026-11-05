package com.m7mdabaza.xogame.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.m7mdabaza.xogame.data.settings.SettingsRepositoryImpl
import com.m7mdabaza.xogame.data.stats.StatsRepositoryImpl
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.RoundOutcome
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The DataStore repositories against real files in a temporary folder (plain JVM, no device). */
class RepositoriesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    /** A DataStore on [name]; reopening the same name reads what was written before. */
    private fun store(name: String): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("$name.preferences_pb") }
    }

    /** Closes every open store so the same file can be opened again. */
    private fun closeStores() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @After
    fun tearDown() = closeStores()

    // --- Settings ----------------------------------------------------------------

    @Test
    fun `settings start with the defaults`() = runTest {
        assertEquals(Settings(), SettingsRepositoryImpl(store("settings")).settings.first())
    }

    @Test
    fun `settings updates are saved and survive reopening the file`() = runTest {
        val changed = Settings(
            soundEnabled = false,
            vibrationEnabled = false,
            theme = ThemeMode.DARK,
            matchLength = MatchLength.BEST_OF_5,
            humanSymbol = Player.O,
            starter = Starter.RANDOM,
            xName = "Sara",
            oName = "عمر",
            boardSize = BoardSize.EIGHT,
            level = Level.MEDIUM,
            challengeReminder = false,
        )
        SettingsRepositoryImpl(store("settings")).update { changed }
        closeStores()

        assertEquals(changed, SettingsRepositoryImpl(store("settings")).settings.first())
    }

    @Test
    fun `settings update starts from the current value`() = runTest {
        val repository = SettingsRepositoryImpl(store("settings"))
        repository.update { it.copy(theme = ThemeMode.LIGHT) }
        repository.update { it.copy(soundEnabled = false) }
        assertEquals(Settings(theme = ThemeMode.LIGHT, soundEnabled = false), repository.settings.first())
    }

    @Test
    fun `unknown enum names fall back to the defaults`() = runTest {
        val store = store("settings")
        store.edit {
            it[stringPreferencesKey("theme")] = "SEPIA"
            it[stringPreferencesKey("match_length")] = "BEST_OF_7"
        }
        val settings = SettingsRepositoryImpl(store).settings.first()
        assertEquals(ThemeMode.SYSTEM, settings.theme)
        assertEquals(MatchLength.SINGLE, settings.matchLength)
    }

    // --- Stats -------------------------------------------------------------------

    @Test
    fun `stats start empty`() = runTest {
        assertEquals(Stats(), StatsRepositoryImpl(store("stats")).stats.first())
    }

    @Test
    fun `stats are kept per mode, board and level and survive reopening the file`() = runTest {
        val repository = StatsRepositoryImpl(store("stats"))
        val eightHard = StatsKey(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.HARD)
        repository.update(eightHard) { it.record(RoundOutcome.WIN).record(RoundOutcome.WIN) }
        repository.update(eightHard) { it.record(RoundOutcome.DRAW) }
        repository.update(StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.HARD)) { it.record(RoundOutcome.DRAW) }
        repository.update(StatsKey(GameMode.TWO_PLAYERS, BoardSize.THREE)) { it.record(RoundOutcome.LOSS) }
        closeStores()

        val stats = StatsRepositoryImpl(store("stats")).stats.first()
        assertEquals(LevelStats(wins = 2, draws = 1, currentStreak = 0, bestStreak = 2), stats[eightHard])
        assertEquals(LevelStats(draws = 1), stats[GameMode.VS_COMPUTER, BoardSize.THREE, Level.HARD])
        assertEquals(LevelStats(losses = 1), stats[GameMode.TWO_PLAYERS, BoardSize.THREE])
        assertEquals(LevelStats(), stats[GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.EASY])
        assertEquals(LevelStats(), stats[GameMode.TWO_PLAYERS, BoardSize.EIGHT])
    }

    @Test
    fun `records from before boards and levels were separate are ignored`() = runTest {
        val store = store("stats")
        store.edit { it[intPreferencesKey("VS_COMPUTER_EASY_wins")] = 7 }
        assertEquals(Stats(), StatsRepositoryImpl(store).stats.first())
    }

    @Test
    fun `reset deletes every record`() = runTest {
        val repository = StatsRepositoryImpl(store("stats"))
        repository.update(StatsKey(GameMode.VS_COMPUTER, BoardSize.FOUR, Level.EASY)) { it.record(RoundOutcome.WIN) }
        repository.reset()
        assertEquals(Stats(), repository.stats.first())
    }
}
