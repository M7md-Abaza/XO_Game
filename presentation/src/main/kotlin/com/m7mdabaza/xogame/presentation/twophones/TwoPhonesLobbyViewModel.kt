package com.m7mdabaza.xogame.presentation.twophones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.LobbyError
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.MAX_PEER_NAME_LENGTH
import com.m7mdabaza.xogame.domain.multiplayer.PeerLobby
import com.m7mdabaza.xogame.domain.multiplayer.PeerRole
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** The lobby's own choices: the player's name and, when hosting, the match to offer. */
data class LobbyForm(
    val name: String = "",
    val boardSize: BoardSize = BoardSize.THREE,
    val matchLength: MatchLength = MatchLength.SINGLE,
    /** [Starter.PLAYER_ONE] is this player, [Starter.PLAYER_TWO] the friend. */
    val starter: Starter = Starter.PLAYER_ONE,
    /** The host form is open (before hosting starts). */
    val choosingHostSetup: Boolean = false,
)

/**
 * The two-phone lobby: the name, hosting or joining, and the host's answer to
 * a join request. The connection itself lives in [PeerLobby] and outlives
 * this screen once connected; leaving the screen otherwise stops the lobby.
 */
@HiltViewModel
class TwoPhonesLobbyViewModel @Inject constructor(
    private val lobby: PeerLobby,
    private val observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val random: Random,
) : ViewModel() {

    val lobbyState: StateFlow<LobbyState> = lobby.state

    val errors: Flow<LobbyError> = lobby.errors

    private val _form = MutableStateFlow(LobbyForm())
    val form: StateFlow<LobbyForm> = _form.asStateFlow()

    init {
        // Start from the saved name and the last local choices.
        viewModelScope.launch {
            val settings = observeSettings().first()
            _form.update {
                it.copy(name = settings.myName, boardSize = settings.boardSize, matchLength = settings.matchLength, starter = settings.starter)
            }
        }
    }

    fun onNameChange(name: String) = _form.update { it.copy(name = name.take(MAX_PEER_NAME_LENGTH)) }

    fun onBoardSizeChange(boardSize: BoardSize) = _form.update { it.copy(boardSize = boardSize) }

    fun onMatchLengthChange(matchLength: MatchLength) = _form.update { it.copy(matchLength = matchLength) }

    fun onStarterChange(starter: Starter) = _form.update { it.copy(starter = starter) }

    /** Opens the host form. */
    fun onHostChosen() = _form.update { it.copy(choosingHostSetup = true) }

    fun onHostFormClosed() = _form.update { it.copy(choosingHostSetup = false) }

    fun onStartHosting() {
        val form = _form.value
        val host = PeerRole.HOST.symbol
        val firstPlayer = when (form.starter) {
            Starter.PLAYER_ONE -> host
            Starter.PLAYER_TWO -> host.opponent
            Starter.RANDOM -> if (random.nextBoolean()) host else host.opponent
        }
        _form.update { it.copy(choosingHostSetup = false) }
        lobby.host(saveName(), TwoPhoneSetup(form.boardSize, form.matchLength, firstPlayer))
    }

    fun onSearch() = lobby.search(saveName())

    fun onJoin(hostId: String) = lobby.join(hostId)

    fun onAnswer(accept: Boolean) = lobby.answer(accept)

    /** Stops hosting or searching (back to the two choices). */
    fun onStop() = lobby.stop()

    override fun onCleared() {
        // A connected game keeps its connection (PeerLobby.stop leaves it alone).
        lobby.stop()
    }

    /** Saves the name for next time and returns it as sent to the friend. */
    private fun saveName(): String {
        val name = _form.value.name.trim().take(MAX_PEER_NAME_LENGTH)
        viewModelScope.launch { updateSettings { it.copy(myName = name) } }
        return name
    }
}
