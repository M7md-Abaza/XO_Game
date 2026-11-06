package com.m7mdabaza.xogame.presentation.testing

import com.m7mdabaza.xogame.domain.multiplayer.LobbyError
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.PeerChannel
import com.m7mdabaza.xogame.domain.multiplayer.PeerEvent
import com.m7mdabaza.xogame.domain.multiplayer.PeerLobby
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Records what the screens ask of the lobby; tests set its [state]. */
class FakePeerLobby(override var isAvailable: Boolean = true) : PeerLobby {
    override val state = MutableStateFlow<LobbyState>(LobbyState.Idle)
    val errorChannel = Channel<LobbyError>(Channel.UNLIMITED)
    override val errors: Flow<LobbyError> = errorChannel.receiveAsFlow()
    val calls = mutableListOf<String>()
    var hosted: Pair<String, TwoPhoneSetup>? = null
    var searchedAs: String? = null

    override fun host(name: String, setup: TwoPhoneSetup) {
        calls += "host"
        hosted = name to setup
    }

    override fun search(name: String) {
        calls += "search"
        searchedAs = name
    }

    override fun join(hostId: String) {
        calls += "join $hostId"
    }

    override fun answer(accept: Boolean) {
        calls += "answer $accept"
    }

    override fun stop() {
        calls += "stop"
    }
}

/** A connection whose friend is the test: [receive] plays the friend's messages; [sent] holds this phone's. */
class FakePeerChannel : PeerChannel {
    private val inbox = Channel<PeerEvent>(Channel.UNLIMITED)
    override val events: Flow<PeerEvent> = inbox.receiveAsFlow()
    val sent = mutableListOf<PeerMessage>()
    var closed = false
        private set

    fun receive(message: PeerMessage) {
        inbox.trySend(PeerEvent.Received(message))
    }

    fun disconnect() {
        inbox.trySend(PeerEvent.Disconnected)
    }

    override suspend fun send(message: PeerMessage) {
        if (!closed) sent += message
    }

    override fun close() {
        closed = true
    }
}
