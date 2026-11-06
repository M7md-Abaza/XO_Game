package com.m7mdabaza.xogame.presentation

import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.AchievementStatus
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.ChallengeProgress
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.domain.usecase.GetDailyChallengeUseCase
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.StartChallengeUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.presentation.achievements.AchievementsScreen
import com.m7mdabaza.xogame.presentation.challenge.ChallengePhase
import com.m7mdabaza.xogame.presentation.challenge.ChallengeUiState
import com.m7mdabaza.xogame.presentation.challenge.DailyChallengeScreen
import com.m7mdabaza.xogame.presentation.challenge.FailReason
import com.m7mdabaza.xogame.presentation.game.GameActions
import com.m7mdabaza.xogame.presentation.game.GameScreen
import com.m7mdabaza.xogame.presentation.game.GameUiState
import com.m7mdabaza.xogame.presentation.game.MatchSettings
import com.m7mdabaza.xogame.presentation.home.GamePickerContent
import com.m7mdabaza.xogame.presentation.home.HomeScreen
import com.m7mdabaza.xogame.presentation.home.HomeUiState
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsDialog
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsUiState
import com.m7mdabaza.xogame.presentation.settings.SettingsActions
import com.m7mdabaza.xogame.presentation.settings.SettingsScreen
import com.m7mdabaza.xogame.presentation.settings.SettingsUiState
import com.m7mdabaza.xogame.presentation.splash.SplashContent
import com.m7mdabaza.xogame.presentation.stats.StatsScreen
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs Google's Accessibility Test Framework (labels, touch targets,
 * contrast, ...) over each screen, and checks the labels and touch areas
 * added for screen-reader and motor accessibility.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val play = PlayMoveUseCase(WinChecker())

    private fun game(mode: GameMode, boardSize: BoardSize, vararg moves: Cell): GameState =
        moves.fold(StartGameUseCase()(GameConfig(mode, boardSize, if (mode == GameMode.VS_COMPUTER) Level.MEDIUM else null))) { state, cell ->
            (play(state, cell) as MoveResult.Played).state
        }

    private fun gameState(game: GameState, showSetup: Boolean = false, match: MatchState = MatchState(MatchLength.BEST_OF_3)) =
        GameUiState(
            mode = game.config.mode,
            boardSize = game.config.boardSize,
            level = game.config.level,
            settings = MatchSettings(length = match.length),
            game = game,
            match = match,
            showSetup = showSetup,
            hasStarted = !showSetup,
            bannerAdUnitId = "",
        )

    /** Shows [content] and runs every accessibility check on it. */
    private fun check(dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent { XoTheme(darkTheme = dark) { Surface { content() } } }
        rule.enableAccessibilityChecks()
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }

    @Test
    fun home() = check {
        HomeScreen(HomeUiState(bannerAdUnitId = "", canRequestAds = false), { _, _, _ -> }, {}, {}, showRemoveAdsBadge = true)
    }

    @Test
    fun gamePickerVsComputer() = check {
        GamePickerContent(GameMode.VS_COMPUTER, BoardSize.THREE, Level.HARD, {}, {}, { _, _ -> })
    }

    @Test
    fun gamePickerTwoPlayers() = check {
        GamePickerContent(GameMode.TWO_PLAYERS, BoardSize.THREE, Level.EASY, {}, {}, { _, _ -> })
    }

    private fun checkBoard(boardSize: BoardSize) = check {
        GameScreen(gameState(game(GameMode.VS_COMPUTER, boardSize, Cell(0, 0))), showBanner = false, actions = GameActions(), showRemoveAdsBadge = true)
    }

    @Test
    fun game3x3() = checkBoard(BoardSize.THREE)

    @Test
    fun game4x4() = checkBoard(BoardSize.FOUR)

    @Test
    fun game8x8() = checkBoard(BoardSize.EIGHT)

    @Test
    fun roundOver() = check {
        val won = game(GameMode.TWO_PLAYERS, BoardSize.THREE, Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(0, 2))
        GameScreen(gameState(won, match = MatchState(MatchLength.BEST_OF_3, xWins = 1)), showBanner = false, actions = GameActions())
    }

    @Test
    fun setupDialog() = check {
        GameScreen(gameState(game(GameMode.VS_COMPUTER, BoardSize.THREE), showSetup = true), showBanner = false, actions = GameActions())
    }

    @Test
    fun settings() = check {
        SettingsScreen(
            SettingsUiState(),
            showPrivacySettings = true,
            actions = SettingsActions(),
            removeAds = RemoveAdsUiState(price = "$1.99", canSimulatePurchase = true),
        )
    }

    @Test
    fun stats() = check {
        StatsScreen(
            Stats(mapOf(StatsKey(GameMode.VS_COMPUTER, BoardSize.THREE, Level.EASY) to LevelStats(wins = 3, losses = 1, draws = 2, bestStreak = 2))),
            onBack = {},
            onReset = {},
        )
    }

    /** The three-way mode switch and the two-phone cards (own results, with streaks). */
    @Test
    fun statsTwoPhones() {
        rule.setContent {
            XoTheme {
                Surface {
                    StatsScreen(
                        Stats(mapOf(StatsKey(GameMode.TWO_PHONES, BoardSize.FOUR) to LevelStats(wins = 2, losses = 1, currentStreak = 1, bestStreak = 2))),
                        onBack = {},
                        onReset = {},
                    )
                }
            }
        }
        rule.onNodeWithText(string(R.string.stats_tab_two_phones)).performClick()
        // Each stat cell reads as one item, "Best streak: 2".
        rule.onNodeWithContentDescription("${string(R.string.stats_best_streak)}: 2").assertExists()
        rule.enableAccessibilityChecks()
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }

    @Test
    fun achievements() = check {
        AchievementsScreen(
            Achievement.entries.mapIndexed { i, a -> AchievementStatus(a, progress = if (i == 4) 37 else 0, unlockedAtMillis = if (i < 2) 1_700_000_000_000 else null) },
            onBack = {},
        )
    }

    private val today = 20_396L

    private fun challengeState(phase: ChallengePhase, failReason: FailReason? = null): ChallengeUiState {
        val challenge = GetDailyChallengeUseCase { today * TimeProvider.MILLIS_PER_DAY }()
        return ChallengeUiState(
            phase = phase,
            challenge = challenge,
            game = StartChallengeUseCase(StartGameUseCase(), play)(challenge),
            failReason = failReason,
            progress = ChallengeProgress(setOf(today - 1, today - 2), today),
            bannerAdUnitId = "",
        )
    }

    @Test
    fun dailyChallenge() = check {
        DailyChallengeScreen(challengeState(ChallengePhase.PLAYING), showBanner = false, onBack = {}, onCellClick = {}, onRetry = {}, showRemoveAdsBadge = true)
    }

    @Test
    fun dailyChallengeFailed() = check {
        DailyChallengeScreen(challengeState(ChallengePhase.FAILED, FailReason.NO_FORK), showBanner = false, onBack = {}, onCellClick = {}, onRetry = {})
    }

    @Test
    fun achievementCardsAreReadAsOneItem() {
        rule.setContent {
            XoTheme { AchievementsScreen(listOf(AchievementStatus(Achievement.GAMES_100, progress = 37, unlockedAtMillis = null)), onBack = {}) }
        }
        val title = string(R.string.achievement_games_100)
        val description = string(R.string.achievement_games_100_desc)
        rule.onNodeWithContentDescription("$title. $description. ${string(R.string.achievements_progress, 37, 100)}").assertExists()
    }

    @Test
    fun splash() = check { SplashContent() }

    @Test
    fun splashDark() = check(dark = true) { SplashContent() }

    @Test
    fun removeAdsDialog() = check { RemoveAdsDialog(RemoveAdsUiState(price = "$1.99"), {}, {}, {}) }

    @Test
    fun removeAdsDialogDark() = check(dark = true) { RemoveAdsDialog(RemoveAdsUiState(price = "$1.99"), {}, {}, {}) }

    @Test
    fun removeAdsDialogUnavailable() = check { RemoveAdsDialog(RemoveAdsUiState(price = null), {}, {}, {}) }

    // --- Specific fixes ------------------------------------------------------------------

    @Test
    fun markOnlyChoicesHaveLabels() {
        rule.setContent {
            XoTheme { GameScreen(gameState(game(GameMode.VS_COMPUTER, BoardSize.THREE), showSetup = true), showBanner = false, actions = GameActions()) }
        }
        rule.onNodeWithContentDescription("X").assertExists()
        rule.onNodeWithContentDescription("O").assertExists()
    }

    @Test
    fun scoreboardChipsAreReadAsOneItem() {
        rule.setContent {
            XoTheme {
                GameScreen(
                    gameState(game(GameMode.VS_COMPUTER, BoardSize.THREE), match = MatchState(MatchLength.BEST_OF_3, oWins = 2)),
                    showBanner = false,
                    actions = GameActions(),
                )
            }
        }
        rule.onNodeWithContentDescription(string(R.string.game_score_description, string(R.string.game_you), "X", 0)).assertExists()
        rule.onNodeWithContentDescription(string(R.string.game_score_description, string(R.string.game_phone), "O", 2)).assertExists()
    }

    @Test
    fun theWholeSquareIsTheTouchTarget() {
        val clicked = mutableListOf<Cell>()
        rule.setContent {
            XoTheme {
                GameScreen(
                    gameState(game(GameMode.TWO_PLAYERS, BoardSize.EIGHT)),
                    showBanner = false,
                    actions = GameActions(onCellClick = { clicked += it }),
                )
            }
        }
        val description = string(R.string.game_cell_description, 4, 5, string(R.string.game_cell_empty))
        // A tap in the very corner of the square, outside the rounded highlight, still counts.
        rule.onNodeWithContentDescription(description).performTouchInput { click(Offset(1f, 1f)) }
        assertEquals(listOf(Cell(3, 4)), clicked)
    }
}
