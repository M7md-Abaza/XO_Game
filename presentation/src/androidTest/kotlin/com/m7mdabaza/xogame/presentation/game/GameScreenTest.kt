package com.m7mdabaza.xogame.presentation.game

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for the stateless [GameScreen]: what it shows for a state, and which actions it sends. */
@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val play = PlayMoveUseCase(WinChecker())

    private fun game(mode: GameMode, vararg moves: Cell): GameState =
        moves.fold(StartGameUseCase()(GameConfig(mode, Level.EASY))) { state, cell ->
            (play(state, cell) as MoveResult.Played).state
        }

    private fun state(
        game: GameState,
        match: MatchState = MatchState(MatchLength.SINGLE),
        showSetup: Boolean = false,
    ) = GameUiState(
        mode = game.config.mode,
        level = game.config.level,
        settings = MatchSettings(length = match.length),
        game = game,
        match = match,
        showSetup = showSetup,
        hasStarted = !showSetup,
        bannerAdUnitId = "",
    )

    private fun show(state: GameUiState, actions: GameActions = GameActions()) {
        rule.setContent { XoTheme { GameScreen(state = state, showBanner = false, actions = actions) } }
    }

    private fun cell(row: Int, col: Int, content: String) =
        rule.onNodeWithContentDescription(string(R.string.game_cell_description, row + 1, col + 1, content))

    /** X: top row; O: middle row. X wins on the fifth move. */
    private val xWinsTopRow = arrayOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(0, 2))

    @Test
    fun setupSendsTheChosenSettings() {
        var started: MatchSettings? = null
        show(
            state(game(GameMode.VS_COMPUTER), showSetup = true),
            GameActions(onStartMatch = { started = it }),
        )
        rule.onNodeWithText(string(R.string.setup_starter_random)).performClick()
        rule.onNodeWithText(string(R.string.setup_match_best_of, 3)).performClick()
        rule.onNodeWithText(string(R.string.setup_take_turns)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.setup_start)).performClick()
        assertEquals(MatchSettings(starter = Starter.RANDOM, length = MatchLength.BEST_OF_3), started)
    }

    @Test
    fun tappingAnEmptySquareReportsItsCell() {
        val clicked = mutableListOf<Cell>()
        show(state(game(GameMode.TWO_PLAYERS, Cell(0, 0))), GameActions(onCellClick = { clicked += it }))

        rule.onNodeWithText(string(R.string.game_turn_player, string(R.string.game_player_default, "O")))
            .assertIsDisplayed()
        cell(0, 0, "X").assertIsNotEnabled()
        cell(1, 2, string(R.string.game_cell_empty)).assertIsEnabled().performClick()
        assertEquals(listOf(Cell(1, 2)), clicked)
    }

    @Test
    fun aFinishedRoundShowsTheWinnerAndTheNextRoundButton() {
        var next = false
        show(
            state(game(GameMode.TWO_PLAYERS, *xWinsTopRow), MatchState(MatchLength.BEST_OF_3, xWins = 1)),
            GameActions(onNextRound = { next = true }),
        )
        rule.onNodeWithText(string(R.string.game_result_player_wins, string(R.string.game_player_default, "X")))
            .assertIsDisplayed()
        cell(2, 2, string(R.string.game_cell_empty)).assertIsNotEnabled()
        rule.onNodeWithText(string(R.string.game_next_round)).performClick()
        assertTrue(next)
    }

    @Test
    fun aFinishedMatchShowsTheMatchDialog() {
        var setupOpened = false
        show(
            state(game(GameMode.VS_COMPUTER, *xWinsTopRow), MatchState(MatchLength.BEST_OF_3, xWins = 2)),
            GameActions(onOpenSetup = { setupOpened = true }),
        )
        rule.onNodeWithText(string(R.string.game_match_you_win)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.game_match_score, 2, 0)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.game_settings)).performClick()
        assertTrue(setupOpened)
    }
}
