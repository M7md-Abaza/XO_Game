package com.m7mdabaza.xogame.presentation.twophones

import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.JoinRequest
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.NearbyHost
import com.m7mdabaza.xogame.domain.multiplayer.PROTOCOL_VERSION
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.PeerRole
import com.m7mdabaza.xogame.domain.multiplayer.RequestKind
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneRules
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneState
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The two-phone lobby and game screens (stateless): what they show, what they send, and accessibility. */
@RunWith(AndroidJUnit4::class)
class TwoPhonesScreensTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val setup = TwoPhoneSetup(BoardSize.FOUR, MatchLength.BEST_OF_3, Player.X)
    private val hosts = listOf(
        NearbyHost("h1", "Ahmed", BoardSize.THREE, MatchLength.SINGLE, compatible = true),
        NearbyHost("h2", "Omar", null, null, compatible = false),
    )

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) =
        rule.setContent { XoTheme(darkTheme = dark) { Surface { content() } } }

    private fun lobby(state: LobbyState, form: LobbyForm = LobbyForm(name = "Sara"), gate: LobbyGate = LobbyGate.READY, actions: LobbyActions = LobbyActions()) =
        show { TwoPhonesLobbyScreen(state, form, gate, actions) }

    /** Runs Google's accessibility checks on what is shown. */
    private fun checkAccessibility() {
        rule.enableAccessibilityChecks()
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }

    private val rules = TwoPhoneRules(StartGameUseCase(), PlayMoveUseCase(WinChecker()))

    /** The host's view once the match started: Ahmed (X) against Sara (O), Ahmed to move. */
    private fun playing(): TwoPhoneState {
        var state = rules.start(PeerRole.HOST, "Ahmed", setup).state
        state = rules.receive(state, PeerMessage.Hello(PROTOCOL_VERSION, "Sara")).state
        return rules.receive(state, PeerMessage.Ready).state
    }

    private fun TwoPhoneState.iPlay(row: Int, col: Int) = rules.play(this, Cell(row, col)).state

    private fun TwoPhoneState.friendPlays(row: Int, col: Int) =
        rules.receive(this, PeerMessage.Move(stage, checkNotNull(game).history.size, Cell(row, col))).state

    private fun TwoPhoneState.friendSends(message: PeerMessage) = rules.receive(this, message).state

    /** Ahmed (X) took the top row of the 4×4 board. */
    private fun roundWon() =
        playing().iPlay(0, 0).friendPlays(1, 0).iPlay(0, 1).friendPlays(1, 1).iPlay(0, 2).friendPlays(1, 2).iPlay(0, 3)

    private fun game(state: TwoPhoneState, actions: TwoPhonesGameActions = TwoPhonesGameActions(), dark: Boolean = false) =
        show(dark) { TwoPhonesGameScreen(state.toUiState(), actions) }

    private fun cell(row: Int, col: Int, content: String) =
        rule.onNodeWithContentDescription(string(R.string.game_cell_description, row + 1, col + 1, content))

    private fun inDialog(text: String) = rule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    // --- What the lobby sends -------------------------------------------------------------

    @Test
    fun theChoicesHostOrSearchAndTakeTheName() {
        val calls = mutableListOf<String>()
        // The name is kept like the ViewModel keeps it; a fixed value would make the field send it back.
        var form by mutableStateOf(LobbyForm(name = "Sara"))
        show {
            TwoPhonesLobbyScreen(
                LobbyState.Idle,
                form,
                LobbyGate.READY,
                LobbyActions(
                    onNameChange = { calls += "name $it"; form = form.copy(name = it) },
                    onHostChosen = { calls += "host" },
                    onSearch = { calls += "search" },
                ),
            )
        }
        rule.onNode(hasText(string(R.string.twophones_your_name)) and hasClickAction()).performTextReplacement("Omar")
        rule.onNodeWithText(string(R.string.twophones_host)).performClick()
        rule.onNodeWithText(string(R.string.twophones_join)).performClick()
        assertEquals(listOf("name Omar", "host", "search"), calls)
    }

    @Test
    fun hostsOfAnotherVersionCantBeJoined() {
        val joined = mutableListOf<String>()
        lobby(LobbyState.Searching(hosts), actions = LobbyActions(onJoin = { joined += it }))
        rule.onNode(hasText("Ahmed") and hasClickAction()).assertIsEnabled().performClick()
        rule.onNode(hasText("Omar") and hasClickAction()).assertIsNotEnabled()
        rule.onNodeWithText(string(R.string.twophones_host_other_version)).assertExists()
        assertEquals(listOf("h1"), joined)
    }

    @Test
    fun theHostSeesTheCodeAndAnswers() {
        val answers = mutableListOf<Boolean>()
        lobby(LobbyState.Hosting(setup, JoinRequest("Sara", "4821")), actions = LobbyActions(onAnswer = { answers += it }))
        rule.onNodeWithText(string(R.string.twophones_request_title, "Sara")).assertExists()
        rule.onNodeWithText("4821").assertExists()
        rule.onNodeWithText(string(R.string.twophones_accept)).performClick()
        rule.onNodeWithText(string(R.string.twophones_decline)).performClick()
        assertEquals(listOf(true, false), answers)
    }

    @Test
    fun aMissingPermissionAsksFirst() {
        var asked = 0
        lobby(LobbyState.Idle, gate = LobbyGate.NEEDS_PERMISSION, actions = LobbyActions(onGateAction = { asked++ }))
        rule.onNodeWithText(string(R.string.twophones_host)).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.twophones_permission_allow)).performClick()
        assertEquals(1, asked)
    }


    // --- Accessibility --------------------------------------------------------------------

    @Test
    fun accessibilityChoices() {
        lobby(LobbyState.Idle)
        checkAccessibility()
    }

    @Test
    fun accessibilityHostForm() {
        lobby(LobbyState.Idle, form = LobbyForm(name = "Sara", choosingHostSetup = true))
        checkAccessibility()
    }

    @Test
    fun accessibilityWaiting() {
        lobby(LobbyState.Hosting(setup))
        checkAccessibility()
    }

    @Test
    fun accessibilityJoinRequest() {
        lobby(LobbyState.Hosting(setup, JoinRequest("Sara", "4821")))
        checkAccessibility()
    }

    @Test
    fun accessibilitySearching() {
        lobby(LobbyState.Searching(hosts))
        checkAccessibility()
    }

    @Test
    fun accessibilityJoining() {
        lobby(LobbyState.Joining(hosts.first(), code = "4821"))
        checkAccessibility()
    }

    @Test
    fun accessibilityPermission() {
        lobby(LobbyState.Idle, gate = LobbyGate.NEEDS_PERMISSION)
        checkAccessibility()
    }

    // --- The game -------------------------------------------------------------------------

    @Test
    fun onYourTurnTheBoardIsOpenWithoutUndoOrHint() {
        val tapped = mutableListOf<Cell>()
        game(playing(), TwoPhonesGameActions(onCellClick = { tapped += it }))
        rule.onNodeWithText(string(R.string.game_turn_you)).assertExists()
        rule.onNodeWithText(string(R.string.twophones_title)).assertExists()
        rule.onNodeWithText(string(R.string.game_undo)).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.game_hint)).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.twophones_leave)).assertIsEnabled()
        cell(1, 2, string(R.string.game_cell_empty)).performClick()
        assertEquals(listOf(Cell(1, 2)), tapped)
    }

    @Test
    fun onTheFriendsTurnTheBoardWaits() {
        game(playing().iPlay(0, 0))
        rule.onNodeWithText(string(R.string.game_turn_player, "Sara")).assertExists()
        cell(1, 1, string(R.string.game_cell_empty)).assertIsNotEnabled()
    }

    @Test
    fun aRestartWaitsForTheFriendsAnswer() {
        val state = rules.request(playing().iPlay(0, 0).friendPlays(1, 1), RequestKind.RestartRound).state
        game(state)
        rule.onNodeWithText(string(R.string.twophones_waiting_answer, "Sara")).assertExists()
        rule.onNodeWithText(string(R.string.game_restart)).assertIsNotEnabled()
        cell(2, 2, string(R.string.game_cell_empty)).assertIsNotEnabled()
    }

    @Test
    fun theFriendsRequestIsAnswered() {
        val answers = mutableListOf<Boolean>()
        val newMatch = TwoPhoneSetup(BoardSize.EIGHT, MatchLength.SINGLE, Player.O)
        game(playing().friendSends(PeerMessage.Request(0, RequestKind.NewMatch(newMatch))), TwoPhonesGameActions(onAnswer = { answers += it }))
        rule.onNodeWithText(string(R.string.twophones_request_new_match_title, "Sara")).assertExists()
        rule.onNodeWithText(string(R.string.twophones_new_match_friend_starts, "Sara")).assertExists()
        inDialog(string(R.string.twophones_accept)).performClick()
        inDialog(string(R.string.twophones_decline)).performClick()
        assertEquals(listOf(true, false), answers)
    }

    @Test
    fun leavingAsksFirst() {
        var left = 0
        game(playing(), TwoPhonesGameActions(onLeave = { left++ }))
        rule.onNodeWithText(string(R.string.twophones_leave)).performClick()
        rule.onNodeWithText(string(R.string.twophones_leave_text, "Sara")).assertExists()
        inDialog(string(R.string.twophones_stay)).performClick()
        assertEquals(0, left)
        rule.onNodeWithText(string(R.string.twophones_leave)).performClick()
        inDialog(string(R.string.twophones_leave)).performClick()
        assertEquals(1, left)
    }

    @Test
    fun aNewMatchIsAskedForWithTheChosenSettings() {
        val asked = mutableListOf<Triple<BoardSize, MatchLength, Starter>>()
        game(playing(), TwoPhonesGameActions(onRequestNewMatch = { board, length, starter -> asked += Triple(board, length, starter) }))
        rule.onNodeWithContentDescription(string(R.string.game_new_match)).performClick()
        inDialog(string(R.string.board_size, 8)).performClick()
        inDialog(string(R.string.twophones_starter_friend)).performClick()
        inDialog(string(R.string.twophones_ask)).performClick()
        assertEquals(listOf(Triple(BoardSize.EIGHT, MatchLength.BEST_OF_3, Starter.PLAYER_TWO)), asked)
        rule.onNodeWithText(string(R.string.twophones_ask)).assertDoesNotExist()
    }

    @Test
    fun afterARoundTheNextRoundWaitsForBothPlayers() {
        var continued = 0
        game(roundWon().friendSends(PeerMessage.Continue(0)), TwoPhonesGameActions(onContinue = { continued++ }))
        rule.onNodeWithText(string(R.string.game_result_you_win)).assertExists()
        rule.onNodeWithText(string(R.string.twophones_friend_ready, "Sara")).assertExists()
        rule.onNodeWithText(string(R.string.game_next_round)).performClick()
        assertEquals(1, continued)
    }

    @Test
    fun afterContinuingThePlayerWaitsForTheFriend() {
        game(rules.continueAfterRound(roundWon()).state)
        rule.onNodeWithText(string(R.string.twophones_waiting_friend, "Sara")).assertIsNotEnabled()
    }

    @Test
    fun whenTheFriendLeavesTheGameEndsWithTheWayHome() {
        var exited = 0
        game(playing().iPlay(0, 0).friendSends(PeerMessage.Leave), TwoPhonesGameActions(onExit = { exited++ }))
        rule.onNodeWithText(string(R.string.twophones_end_friend_left, "Sara")).assertExists()
        // Nobody's turn any more behind the dialog.
        rule.onNodeWithText(string(R.string.game_turn_player, "Sara")).assertDoesNotExist()
        inDialog(string(R.string.twophones_back_home)).performClick()
        assertEquals(1, exited)
    }

    /** Large text in a right-to-left layout: the board and both buttons still fit on a phone screen. */
    @Test
    fun atFontScale2RightToLeftTheGameStillFits() {
        rule.setContent {
            XoTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    Surface { TwoPhonesGameScreen(playing().iPlay(0, 0).toUiState(), TwoPhonesGameActions()) }
                }
            }
        }
        // In a right-to-left layout the Latin name is wrapped in bidi marks (see `isolated`).
        val sara = androidx.core.text.BidiFormatter.getInstance(true).unicodeWrap("Sara")
        rule.onNodeWithText(string(R.string.game_turn_player, sara)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.game_restart)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.twophones_leave)).assertIsDisplayed()
        cell(3, 3, string(R.string.game_cell_empty)).assertIsDisplayed()
        checkAccessibility()
    }

    @Test
    fun atFontScale2TheFriendsRequestKeepsItsButtons() {
        rule.setContent {
            XoTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                    Surface {
                        TwoPhonesGameScreen(
                            playing().friendSends(PeerMessage.Request(0, RequestKind.NewMatch(setup))).toUiState(),
                            TwoPhonesGameActions(),
                        )
                    }
                }
            }
        }
        inDialog(string(R.string.twophones_accept)).assertIsDisplayed()
        inDialog(string(R.string.twophones_decline)).assertIsDisplayed()
    }

    @Test
    fun accessibilityLeaveDialog() {
        game(playing())
        rule.onNodeWithText(string(R.string.twophones_leave)).performClick()
        checkAccessibility()
    }

    @Test
    fun withoutAConnectionTheGameOffersTheWayHome() {
        var exited = 0
        show { TwoPhonesGameScreen(TwoPhonesUiState(), TwoPhonesGameActions(onExit = { exited++ })) }
        rule.onNodeWithText(string(R.string.twophones_end_connection_lost)).assertExists()
        rule.onNodeWithText(string(R.string.twophones_back_home)).performClick()
        assertEquals(1, exited)
    }

    @Test
    fun accessibilityGameDark() {
        game(playing().iPlay(1, 1), dark = true)
        checkAccessibility()
    }

    @Test
    fun accessibilityWaitingForTheFriend() {
        game(rules.continueAfterRound(roundWon()).state)
        checkAccessibility()
    }

    @Test
    fun accessibilityFriendsRequest() {
        game(playing().friendSends(PeerMessage.Request(0, RequestKind.NewMatch(setup))))
        checkAccessibility()
    }

    @Test
    fun accessibilityNewMatchDialog() {
        game(playing())
        rule.onNodeWithContentDescription(string(R.string.game_new_match)).performClick()
        checkAccessibility()
    }

    @Test
    fun accessibilityGameEnded() {
        game(playing().friendSends(PeerMessage.Leave))
        checkAccessibility()
    }
}
