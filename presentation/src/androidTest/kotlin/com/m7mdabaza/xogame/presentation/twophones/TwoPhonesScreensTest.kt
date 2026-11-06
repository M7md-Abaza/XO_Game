package com.m7mdabaza.xogame.presentation.twophones

import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.multiplayer.JoinRequest
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.NearbyHost
import com.m7mdabaza.xogame.domain.multiplayer.PROTOCOL_VERSION
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.PeerRole
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

    private fun playing(): TwoPhoneState {
        val rules = TwoPhoneRules(StartGameUseCase(), PlayMoveUseCase(WinChecker()))
        var state = rules.start(PeerRole.HOST, "Ahmed", setup).state
        state = rules.receive(state, PeerMessage.Hello(PROTOCOL_VERSION, "Sara")).state
        return rules.receive(state, PeerMessage.Ready).state
    }

    // --- What the lobby sends -------------------------------------------------------------

    @Test
    fun theChoicesHostOrSearchAndTakeTheName() {
        val calls = mutableListOf<String>()
        lobby(
            LobbyState.Idle,
            actions = LobbyActions(
                onNameChange = { calls += "name $it" },
                onHostChosen = { calls += "host" },
                onSearch = { calls += "search" },
            ),
        )
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

    @Test
    fun theGameShowsWhoYouPlayWithAndWhyItEnded() {
        show { TwoPhonesGameScreen(playing(), connectionMissing = false, onLeave = {}, onExit = {}) }
        rule.onNodeWithText(string(R.string.twophones_connected, "Sara")).assertExists()
        rule.onNodeWithText(string(R.string.twophones_you_play, "X")).assertExists()
    }

    @Test
    fun withoutAConnectionTheGameOffersTheWayHome() {
        var exited = 0
        show { TwoPhonesGameScreen(null, connectionMissing = true, onLeave = {}, onExit = { exited++ }) }
        rule.onNodeWithText(string(R.string.twophones_end_connection_lost)).assertExists()
        rule.onNodeWithText(string(R.string.twophones_back_home)).performClick()
        assertEquals(1, exited)
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

    @Test
    fun accessibilityGameDark() {
        show(dark = true) { TwoPhonesGameScreen(playing(), connectionMissing = false, onLeave = {}, onExit = {}) }
        checkAccessibility()
    }
}
