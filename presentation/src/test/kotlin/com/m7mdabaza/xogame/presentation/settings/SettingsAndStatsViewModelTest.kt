package com.m7mdabaza.xogame.presentation.settings

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveRemoveAdsOfferUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveStatsUseCase
import com.m7mdabaza.xogame.domain.usecase.PurchaseRemoveAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.ResetStatsUseCase
import com.m7mdabaza.xogame.domain.usecase.RestorePurchasesUseCase
import com.m7mdabaza.xogame.domain.usecase.SimulateRemoveAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import com.m7mdabaza.xogame.presentation.XoAppViewModel
import com.m7mdabaza.xogame.presentation.stats.StatsViewModel
import com.m7mdabaza.xogame.presentation.testing.FakeLanguageController
import com.m7mdabaza.xogame.presentation.testing.FakePurchaseRepository
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
    private val purchases = FakePurchaseRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun settingsViewModel() = SettingsViewModel(
        ObserveSettingsUseCase(settingsRepository),
        UpdateSettingsUseCase(settingsRepository),
        language,
        ObserveAdsRemovedUseCase(purchases),
        ObserveRemoveAdsOfferUseCase(purchases),
        PurchaseRemoveAdsUseCase(purchases),
        RestorePurchasesUseCase(purchases),
        SimulateRemoveAdsUseCase(purchases),
    )

    @Test
    fun `settings screen shows the stored settings and the current language`() = runTest(dispatcher) {
        settingsRepository.settings.value = Settings(theme = ThemeMode.DARK)
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope) // stateIn(WhileSubscribed) needs a collector
        assertEquals(
            SettingsUiState(Settings(theme = ThemeMode.DARK), AppLanguage.ENGLISH, removeAdsPrice = "$1.99", canSimulatePurchase = true),
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

    // --- Remove ads ----------------------------------------------------------------------

    @Test
    fun `buying Remove ads shows a thank-you and the screen switches to ads removed`() = runTest(dispatcher) {
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope)
        vm.onRemoveAds()
        assertEquals(SettingsMessage.PURCHASED, vm.uiState.value.message)
        assertEquals(true, vm.uiState.value.adsRemoved)
        vm.onMessageShown()
        assertEquals(null, vm.uiState.value.message)
    }

    @Test
    fun `purchase outcomes map to messages, a cancel says nothing`() = runTest(dispatcher) {
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope)
        val expected = mapOf(
            PurchaseResult.CANCELLED to null,
            PurchaseResult.PENDING to SettingsMessage.PENDING,
            PurchaseResult.ALREADY_OWNED to SettingsMessage.ALREADY_OWNED,
            PurchaseResult.UNAVAILABLE to SettingsMessage.UNAVAILABLE,
            PurchaseResult.ERROR to SettingsMessage.PURCHASE_ERROR,
        )
        for ((result, message) in expected) {
            purchases.purchaseResult = result
            vm.onRemoveAds()
            assertEquals("for $result", message, vm.uiState.value.message)
            assertEquals(false, vm.uiState.value.busy)
            vm.onMessageShown()
        }
    }

    @Test
    fun `restore reports what it found`() = runTest(dispatcher) {
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope)
        vm.onRestorePurchases()
        assertEquals(SettingsMessage.NOTHING_TO_RESTORE, vm.uiState.value.message)

        purchases.restoreResult = RestoreResult.ERROR
        vm.onRestorePurchases()
        assertEquals(SettingsMessage.RESTORE_ERROR, vm.uiState.value.message)

        purchases.restoreResult = RestoreResult.RESTORED
        vm.onRestorePurchases()
        assertEquals(SettingsMessage.RESTORED, vm.uiState.value.message)
        assertEquals(true, vm.uiState.value.adsRemoved)
    }

    @Test
    fun `the debug switch simulates the purchase`() = runTest(dispatcher) {
        val vm = settingsViewModel()
        vm.uiState.launchIn(backgroundScope)
        assertEquals(true, vm.uiState.value.canSimulatePurchase)
        vm.onSimulateRemoveAds(true)
        assertEquals(true, vm.uiState.value.adsRemoved)
        vm.onSimulateRemoveAds(false)
        assertEquals(false, vm.uiState.value.adsRemoved)
    }
}
