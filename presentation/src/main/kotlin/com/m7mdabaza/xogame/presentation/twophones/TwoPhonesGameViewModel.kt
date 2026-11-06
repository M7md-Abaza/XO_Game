package com.m7mdabaza.xogame.presentation.twophones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.PeerLobby
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneRules
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSession
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * A two-phone game over the connection the lobby made. Without one (the app
 * was restarted, so the connection is gone) [match] stays null and
 * [connectionMissing] is true.
 */
@HiltViewModel
class TwoPhonesGameViewModel @Inject constructor(
    lobby: PeerLobby,
    rules: TwoPhoneRules,
) : ViewModel() {

    private val session: TwoPhoneSession? = (lobby.state.value as? LobbyState.Connected)?.peer?.let { peer ->
        TwoPhoneSession(peer.channel, rules, viewModelScope, peer.role, peer.myName, peer.setup)
    }

    val connectionMissing: Boolean = session == null

    val match: StateFlow<TwoPhoneState?> = session?.state ?: MutableStateFlow<TwoPhoneState?>(null).asStateFlow()

    /** Says goodbye to the friend (Back or "Leave"). */
    fun onLeave() {
        session?.leave()
    }

    override fun onCleared() {
        // Gone without a goodbye (e.g. the task was removed): the friend sees the connection lost.
        session?.close()
    }
}
