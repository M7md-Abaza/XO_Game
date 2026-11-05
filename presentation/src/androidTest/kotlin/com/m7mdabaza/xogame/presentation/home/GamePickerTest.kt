package com.m7mdabaza.xogame.presentation.home

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for the new-game picker: the board (and level) it starts. */
@RunWith(AndroidJUnit4::class)
class GamePickerTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private var started: Pair<BoardSize, Level?>? = null

    private fun show(mode: GameMode, boardSize: BoardSize = BoardSize.THREE, level: Level = Level.EASY) {
        rule.setContent {
            XoTheme {
                var board by remember { mutableStateOf(boardSize) }
                var chosenLevel by remember { mutableStateOf(level) }
                GamePickerContent(
                    mode = mode,
                    boardSize = board,
                    level = chosenLevel,
                    onBoardSizeSelected = { board = it },
                    onLevelSelected = { chosenLevel = it },
                    onStart = { b, l -> started = b to l },
                )
            }
        }
    }

    private fun boardCard(label: String) = rule.onNode(hasText(label) and isSelectable())

    @Test
    fun againstThePhoneTheLastChoiceIsPreselectedAndPlayStartsTheChosenGame() {
        show(GameMode.VS_COMPUTER, BoardSize.EIGHT, Level.MEDIUM)
        boardCard("8×8").assertIsSelected()
        boardCard("3×3").assertIsNotSelected()

        boardCard("4×4").performClick()
        boardCard("4×4").assertIsSelected()
        boardCard("8×8").assertIsNotSelected()
        rule.onNodeWithText(string(R.string.level_hard)).performClick()
        rule.onNodeWithText(string(R.string.level_hard_desc)).assertIsDisplayed()

        rule.onNodeWithText(string(R.string.picker_play)).performScrollTo().performClick()
        assertEquals(BoardSize.FOUR to Level.HARD, started)
    }

    @Test
    fun hardOn3x3SaysThePhoneNeverLoses() {
        show(GameMode.VS_COMPUTER, BoardSize.THREE, Level.HARD)
        rule.onNodeWithText(string(R.string.level_hard_unbeatable_desc)).assertIsDisplayed()
    }

    @Test
    fun forTwoPlayersTappingABoardStartsWithoutALevel() {
        show(GameMode.TWO_PLAYERS)
        rule.onNodeWithText(string(R.string.picker_play)).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.level_easy)).assertDoesNotExist()
        rule.onNodeWithText("8×8").performClick()
        assertEquals(BoardSize.EIGHT to null, started)
    }
}
