package com.m7mdabaza.xogame.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.m7mdabaza.xogame.data.di.SettingsStore
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings in a Preferences DataStore. Enums are stored by name; an unknown
 * name (e.g. written by a future version) falls back to the default.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:SettingsStore private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            soundEnabled = this[SOUND] ?: defaults.soundEnabled,
            vibrationEnabled = this[VIBRATION] ?: defaults.vibrationEnabled,
            theme = enumOrDefault(this[THEME], defaults.theme),
            matchLength = enumOrDefault(this[MATCH_LENGTH], defaults.matchLength),
            humanSymbol = enumOrDefault(this[HUMAN_SYMBOL], defaults.humanSymbol),
            starter = enumOrDefault(this[STARTER], defaults.starter),
            xName = this[X_NAME] ?: defaults.xName,
            oName = this[O_NAME] ?: defaults.oName,
            boardSize = enumOrDefault(this[BOARD_SIZE], defaults.boardSize),
            level = enumOrDefault(this[LEVEL], defaults.level),
            challengeReminder = this[CHALLENGE_REMINDER] ?: defaults.challengeReminder,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[SOUND] = settings.soundEnabled
        this[VIBRATION] = settings.vibrationEnabled
        this[THEME] = settings.theme.name
        this[MATCH_LENGTH] = settings.matchLength.name
        this[HUMAN_SYMBOL] = settings.humanSymbol.name
        this[STARTER] = settings.starter.name
        this[X_NAME] = settings.xName
        this[O_NAME] = settings.oName
        this[BOARD_SIZE] = settings.boardSize.name
        this[LEVEL] = settings.level.name
        this[CHALLENGE_REMINDER] = settings.challengeReminder
    }

    private companion object {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val THEME = stringPreferencesKey("theme")
        val MATCH_LENGTH = stringPreferencesKey("match_length")
        val HUMAN_SYMBOL = stringPreferencesKey("human_symbol")
        val STARTER = stringPreferencesKey("starter")
        val X_NAME = stringPreferencesKey("x_name")
        val O_NAME = stringPreferencesKey("o_name")
        val BOARD_SIZE = stringPreferencesKey("board_size")
        val LEVEL = stringPreferencesKey("level")
        val CHALLENGE_REMINDER = booleanPreferencesKey("challenge_reminder")

        inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
