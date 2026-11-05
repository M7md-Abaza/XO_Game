package com.m7mdabaza.xogame.presentation.settings

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveStatsUseCase
import com.m7mdabaza.xogame.domain.usecase.ResetStatsUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import com.m7mdabaza.xogame.presentation.XoAppViewModel
import com.m7mdabaza.xogame.presentation.stats.StatsViewModel
import com.m7mdabaza.xogame.presentation.testing.FakeLanguageController
import com.m7mdabaza.xogame.presentation.testing.FakeSettingsRepository
import com.m7mdabaza.xogame.presentation.testing.FakeStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsAndStatsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val settingsRepository = FakeSettingsRepository()
    private val statsRepository = FakeStatsRepository()
    private val language = FakeLanguageController(AppLanguage.ENGLISH)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun settingsViewModel() = SettingsViewModel(
        ObserveSettingsUseCase(settingsRepository),
        UpdateSettingsUseCase(settingsRepository),
        language,
    )

    @Test
    fun `settings screen shows the stored settings and the current language`() = runTest(dispatcher) {
        settingsRepository.settings.value = Settings(theme = ThemeMode.DARK)
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope) // stateIn(WhileSubscribed) needs a collector
        assertEquals(
            SettingsUiState(Settings(theme = ThemeMode.DARK), AppLanguage.ENGLISH),
            vm.uiState.value,
        )
    }

    @Test
    fun `each control updates its own setting`() = runTest(dispatcher) {
        val vm = settingsViewModel()
        vm.setSound(false)
        vm.setVibration(false)
        vm.setTheme(ThemeMode.LIGHT)
        vm.setMatchLength(MatchLength.BEST_OF_5)
        vm.setChallengeReminder(false)
        advanceUntilIdle()
        assertEquals(
            Settings(
                soundEnabled = false,
                vibrationEnabled = false,
                theme = ThemeMode.LIGHT,
                matchLength = MatchLength.BEST_OF_5,
                challengeReminder = false,
            ),
            settingsRepository.settings.value,
        )
    }

    @Test
    fun `changing the language applies it once`() = runTest(dispatcher) {
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope)
        vm.setLanguage(AppLanguage.ARABIC)
        vm.setLanguage(AppLanguage.ARABIC)
        assertEquals(listOf(AppLanguage.ARABIC), language.applied)
        assertEquals(AppLanguage.ARABIC, vm.uiState.value.language)
    }

    @Test
    fun `the app theme follows the setting`() = runTest(dispatcher) {
        val vm = XoAppViewModel(ObserveSettingsUseCase(settingsRepository))
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
        settingsRepository.update { it.copy(theme = ThemeMode.DARK) }
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun `stats screen shows the records and can reset them`() = runTest(dispatcher) {
        val record = LevelStats(wins = 3, losses = 1)
        statsRepository.stats.value = Stats(mapOf(StatsKey(GameMode.VS_COMPUTER, BoardSize.FOUR, Level.MEDIUM) to record))
        val vm = StatsViewModel(ObserveStatsUseCase(statsRepository), ResetStatsUseCase(statsRepository))
        vm.stats.launchIn(backgroundScope)
        assertEquals(record, vm.stats.value[GameMode.VS_COMPUTER, BoardSize.FOUR, Level.MEDIUM])

        vm.reset()
        assertEquals(Stats(), vm.stats.value)
    }
}
