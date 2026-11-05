package com.m7mdabaza.xogame.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The Remove-ads section has its own state (`RemoveAdsUiState`, shared with the "No ads" badge). */
data class SettingsUiState(
    val settings: Settings = Settings(),
    val language: AppLanguage = AppLanguage.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val languageController: AppLanguageController,
) : ViewModel() {

    private val language = MutableStateFlow(languageController.current)

    val uiState: StateFlow<SettingsUiState> = combine(observeSettings(), language, ::SettingsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(language = language.value))

    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

    fun setVibration(enabled: Boolean) = update { it.copy(vibrationEnabled = enabled) }

    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }

    fun setMatchLength(length: MatchLength) = update { it.copy(matchLength = length) }

    fun setChallengeReminder(enabled: Boolean) = update { it.copy(challengeReminder = enabled) }

    fun setLanguage(language: AppLanguage) {
        if (language == this.language.value) return
        this.language.value = language
        languageController.set(language)
    }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { updateSettings(transform) }
    }
}
