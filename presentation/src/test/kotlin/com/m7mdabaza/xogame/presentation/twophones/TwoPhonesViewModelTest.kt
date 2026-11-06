package com.m7mdabaza.xogame.presentation.twophones

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import com.m7mdabaza.xogame.presentation.testing.FakePeerLobby
import com.m7mdabaza.xogame.presentation.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class TwoPhonesViewModelTest {

    private val lobby = FakePeerLobby()
    private val settings = FakeSettingsRepository(
        Settings(myName = "Ahmed", boardSize = BoardSize.EIGHT, matchLength = MatchLength.BEST_OF_3, starter = Starter.PLAYER_TWO),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun lobbyViewModel(random: Random = Random(0)) =
        TwoPhonesLobbyViewModel(lobby, ObserveSettingsUseCase(settings), UpdateSettingsUseCase(settings), random)

    /** Creates a ViewModel in a store, so the test can clear it like the screen going away. */
    private inline fun <reified T : ViewModel> inStore(store: ViewModelStore, noinline create: () -> T): T =
        ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[T::class]

    // --- Lobby -----------------------------------------------------------------------------

    @Test
    fun `the lobby starts from the saved name and the last choices`() {
        assertEquals(
            LobbyForm(name = "Ahmed", boardSize = BoardSize.EIGHT, matchLength = MatchLength.BEST_OF_3, starter = Starter.PLAYER_TWO),
            lobbyViewModel().form.value,
        )
    }

    @Test
    fun `hosting offers the chosen match as X and saves the name, trimmed`() {
        val vm = lobbyViewModel()
        vm.onNameChange("  Omar  ")
        vm.onHostChosen()
        assertTrue(vm.form.value.choosingHostSetup)
        vm.onBoardSizeChange(BoardSize.FOUR)
        vm.onStarterChange(Starter.PLAYER_ONE)
        vm.onStartHosting()
        assertEquals("Omar" to TwoPhoneSetup(BoardSize.FOUR, MatchLength.BEST_OF_3, Player.X), lobby.hosted)
        assertEquals("Omar", settings.settings.value.myName)
        assertFalse(vm.form.value.choosingHostSetup)
    }

    @Test
    fun `"my friend starts" gives O the first move, and random picks either`() {
        val vm = lobbyViewModel()
        vm.onStartHosting() // PLAYER_TWO from the settings
        assertEquals(Player.O, lobby.hosted?.second?.firstPlayer)

        val firsts = (0 until 20).map { seed ->
            val random = lobbyViewModel(Random(seed))
            random.onStarterChange(Starter.RANDOM)
            random.onStartHosting()
            lobby.hosted?.second?.firstPlayer
        }
        assertEquals(setOf(Player.X, Player.O), firsts.toSet())
    }

    @Test
    fun `names are limited to what the friend is sent`() {
        val vm = lobbyViewModel()
        vm.onNameChange("A".repeat(40))
        assertEquals(20, vm.form.value.name.length)
    }

    @Test
    fun `searching, joining, answering and stopping go to the lobby`() {
        val vm = lobbyViewModel()
        vm.onSearch()
        vm.onJoin("host-1")
        vm.onAnswer(true)
        vm.onStop()
        assertEquals(listOf("search", "join host-1", "answer true", "stop"), lobby.calls)
        assertEquals("Ahmed", lobby.searchedAs)
    }

    @Test
    fun `leaving the lobby screen stops the lobby`() {
        val store = ViewModelStore()
        inStore(store) { lobbyViewModel() }
        store.clear()
        assertEquals(listOf("stop"), lobby.calls)
    }

    // --- Permissions -----------------------------------------------------------------------

    @Test
    fun `each Android version asks for what Nearby needs there`() {
        assertEquals(listOf(Manifest.permission.ACCESS_COARSE_LOCATION), nearbyPermissions(sdk = 28))
        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), nearbyPermissions(sdk = 30))
        assertTrue(Manifest.permission.ACCESS_FINE_LOCATION in nearbyPermissions(sdk = 32))
        assertTrue(Manifest.permission.BLUETOOTH_SCAN in nearbyPermissions(sdk = 31))
        val tiramisu = nearbyPermissions(sdk = 33)
        assertTrue(Manifest.permission.NEARBY_WIFI_DEVICES in tiramisu)
        assertFalse(tiramisu.any { "LOCATION" in it })
        assertTrue(nearbyPermissionIsLocation(sdk = 32))
        assertFalse(nearbyPermissionIsLocation(sdk = 33))
    }
}
