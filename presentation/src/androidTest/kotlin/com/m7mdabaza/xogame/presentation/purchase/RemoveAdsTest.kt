package com.m7mdabaza.xogame.presentation.purchase

import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.game.GameActions
import com.m7mdabaza.xogame.presentation.game.GameScreen
import com.m7mdabaza.xogame.presentation.game.GameUiState
import com.m7mdabaza.xogame.presentation.game.MatchSettings
import com.m7mdabaza.xogame.presentation.home.HomeScreen
import com.m7mdabaza.xogame.presentation.home.HomeUiState
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The "No ads" badge in the screens and the Remove-ads dialog (both stateless). */
@RunWith(AndroidJUnit4::class)
class RemoveAdsTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val gameState: GameUiState
        get() {
            val config = GameConfig(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY)
            return GameUiState(
                mode = config.mode,
                boardSize = config.boardSize,
                level = config.level,
                settings = MatchSettings(length = MatchLength.SINGLE),
                game = StartGameUseCase()(config),
                match = MatchState(MatchLength.SINGLE),
                showSetup = false,
                hasStarted = true,
                bannerAdUnitId = "",
            )
        }

    private fun show(content: @Composable () -> Unit) = rule.setContent { XoTheme { Surface { content() } } }

    @Test
    fun theGameBadgeAsksForTheDialog() {
        var opened = 0
        show { GameScreen(gameState, showBanner = false, actions = GameActions(onRemoveAds = { opened++ }), showRemoveAdsBadge = true) }
        rule.onNodeWithText(string(R.string.remove_ads_badge)).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun noBadgeWhenItIsNotOffered() {
        show { GameScreen(gameState, showBanner = false, actions = GameActions()) }
        rule.onNodeWithText(string(R.string.remove_ads_badge)).assertDoesNotExist()
    }

    @Test
    fun theHomeBadgeAsksForTheDialog() {
        var opened = 0
        show {
            HomeScreen(
                HomeUiState(bannerAdUnitId = "", canRequestAds = false),
                onStartGame = { _, _, _ -> },
                onOpenStats = {},
                onOpenSettings = {},
                showRemoveAdsBadge = true,
                onRemoveAds = { opened++ },
            )
        }
        rule.onNodeWithText(string(R.string.remove_ads_badge)).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun atLargeFontSizesTheGameBadgeIsJustItsIconButKeepsItsName() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                XoTheme { Surface { GameScreen(gameState, showBanner = false, actions = GameActions(), showRemoveAdsBadge = true) } }
            }
        }
        rule.onNodeWithText(string(R.string.remove_ads_badge)).assertDoesNotExist()
        rule.onNodeWithContentDescription(string(R.string.remove_ads_title)).assertExists()
    }

    @Test
    fun theDialogShowsThePriceAndSendsEachAction() {
        var bought = 0
        var restored = 0
        var dismissed = 0
        show { RemoveAdsDialog(RemoveAdsUiState(price = "$1.99"), { bought++ }, { restored++ }, { dismissed++ }) }
        rule.onNodeWithText(string(R.string.remove_ads_benefit_assists)).assertExists()
        rule.onNodeWithText(string(R.string.remove_ads_dialog_buy, "$1.99")).assertIsEnabled().performClick()
        rule.onNodeWithText(string(R.string.restore_purchases)).performClick()
        rule.onNodeWithText(string(R.string.remove_ads_not_now)).performClick()
        assertEquals(listOf(1, 1, 1), listOf(bought, restored, dismissed))
    }

    @Test
    fun withoutAPriceTheDialogExplainsAndCantBuy() {
        show { RemoveAdsDialog(RemoveAdsUiState(price = null), {}, {}, {}) }
        rule.onNodeWithText(string(R.string.remove_ads_not_available)).assertExists()
        rule.onNode(hasText(string(R.string.remove_ads_title)) and hasClickAction()).assertIsNotEnabled()
        rule.onNodeWithText(string(R.string.restore_purchases)).assertIsEnabled()
    }

    @Test
    fun whileBusyNothingCanBeStartedTwice() {
        show { RemoveAdsDialog(RemoveAdsUiState(price = "$1.99", busy = true), {}, {}, {}) }
        rule.onNodeWithText(string(R.string.restore_purchases)).assertIsNotEnabled()
        rule.onNode(hasText(string(R.string.remove_ads_dialog_buy, "$1.99")) and hasClickAction()).assertIsNotEnabled()
    }
}
