package com.m7mdabaza.xogame.data.nearby

import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.m7mdabaza.xogame.domain.multiplayer.ConnectedPeer
import com.m7mdabaza.xogame.domain.multiplayer.JoinRequest
import com.m7mdabaza.xogame.domain.multiplayer.LobbyError
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.NearbyHost
import com.m7mdabaza.xogame.domain.multiplayer.PeerLobby
import com.m7mdabaza.xogame.domain.multiplayer.PeerRole
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PeerLobby] on Nearby Connections.
 * - The host advertises an [Announcement] and shows each join request with its
 *   code; it answers one at a time and turns others away.
 * - The guest searches, stops searching to ask a host (connections are more
 *   reliable that way) and accepts its own request at once; a refusal or a
 *   failure goes back to searching.
 *
 * Everything runs on the main thread ([scope] and Nearby's callbacks). Each
 * start bumps [attempt], so callbacks from an earlier attempt are ignored.
 */
@Singleton
internal class NearbyPeerLobby internal constructor(
    private val gateway: NearbyGateway,
    private val scope: CoroutineScope,
) : PeerLobby {

    @Inject
    constructor(gateway: NearbyGateway) : this(gateway, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

    private val _state = MutableStateFlow<LobbyState>(LobbyState.Idle)
    override val state: StateFlow<LobbyState> = _state.asStateFlow()

    private val _errors = Channel<LobbyError>(Channel.BUFFERED)
    override val errors: Flow<LobbyError> = _errors.receiveAsFlow()

    override val isAvailable: Boolean get() = gateway.isAvailable

    private var attempt = 0
    private var myName = ""

    /** The connection being set up or open, and its channel once accepted. */
    private var endpointId: String? = null
    private var channel: NearbyPeerChannel? = null

    /** Hosts found while searching, in the order found. */
    private val hosts = linkedMapOf<String, NearbyHost>()

    // --- Host --------------------------------------------------------------------------

    override fun host(name: String, setup: TwoPhoneSetup) {
        reset()
        myName = name
        val current = attempt
        _state.value = LobbyState.Hosting(setup)
        scope.launch {
            val status = gateway.startAdvertising(Announcement.of(name, setup).encode(), HostListener(current))
            if (status != null && current == attempt) fail(status)
        }
    }

    override fun answer(accept: Boolean) {
        val hosting = _state.value as? LobbyState.Hosting ?: return
        val request = hosting.request?.takeUnless { it.accepted } ?: return
        val id = endpointId ?: return
        if (!accept) {
            endpointId = null
            _state.value = LobbyState.Hosting(hosting.setup)
            scope.launch { gateway.rejectConnection(id) }
            return
        }
        _state.value = hosting.copy(request = request.copy(accepted = true))
        val current = attempt
        val accepted = newChannel(id)
        scope.launch {
            val status = gateway.acceptConnection(id, accepted)
            if (status != null && current == attempt && channel === accepted) {
                dropPending()
                _state.value = LobbyState.Hosting(hosting.setup)
                _errors.trySend(LobbyError.FAILED)
            }
        }
    }

    private inner class HostListener(private val owner: Int) : ConnectionListener {
        override fun onConnectionInitiated(endpointId: String, name: String, code: String, incoming: Boolean) {
            val hosting = _state.value as? LobbyState.Hosting
            if (owner != attempt || hosting == null || hosting.request != null || this@NearbyPeerLobby.endpointId != null) {
                // Busy (or from an earlier attempt): turn it away.
                scope.launch { gateway.rejectConnection(endpointId) }
                return
            }
            this@NearbyPeerLobby.endpointId = endpointId
            _state.value = hosting.copy(request = JoinRequest(name, code))
        }

        override fun onConnectionResult(endpointId: String, connected: Boolean, statusCode: Int) {
            if (owner != attempt || endpointId != this@NearbyPeerLobby.endpointId) return
            val hosting = _state.value as? LobbyState.Hosting ?: return
            val accepted = channel
            val request = hosting.request
            if (connected && accepted != null && request != null) {
                gateway.stopAdvertising()
                _state.value = LobbyState.Connected(ConnectedPeer(accepted, PeerRole.HOST, myName, request.name, hosting.setup))
            } else {
                // The guest gave up, or this host declined: keep waiting for someone else.
                if (request?.accepted == true) _errors.trySend(LobbyError.FAILED)
                dropPending()
                _state.value = LobbyState.Hosting(hosting.setup)
            }
        }

        override fun onDisconnected(endpointId: String) = onEndpointGone(owner, endpointId)
    }

    // --- Guest -------------------------------------------------------------------------

    override fun search(name: String) {
        reset()
        myName = name
        startSearching()
    }

    private fun startSearching() {
        val current = attempt
        _state.value = LobbyState.Searching(hosts.values.toList())
        scope.launch {
            val status = gateway.startDiscovery(GuestDiscovery(current))
            if (status != null && current == attempt) fail(status)
        }
    }

    override fun join(hostId: String) {
        if (_state.value !is LobbyState.Searching) return
        val host = hosts[hostId]?.takeIf { it.compatible } ?: return
        gateway.stopDiscovery()
        attempt++
        val current = attempt
        endpointId = hostId
        _state.value = LobbyState.Joining(host)
        scope.launch {
            val status = gateway.requestConnection(myName, hostId, GuestListener(current))
            if (status != null && current == attempt) {
                failJoin(if (status == ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN) LobbyError.HOST_GONE else status.toLobbyError())
            }
        }
    }

    private inner class GuestDiscovery(private val owner: Int) : DiscoveryListener {
        override fun onEndpointFound(endpointId: String, endpointInfo: ByteArray?, endpointName: String) {
            if (owner != attempt) return
            val announcement = Announcement.decode(endpointInfo, endpointName)
            hosts[endpointId] = NearbyHost(
                id = endpointId,
                name = announcement.name,
                boardSize = announcement.boardSize.takeIf { announcement.compatible },
                matchLength = announcement.matchLength.takeIf { announcement.compatible },
                compatible = announcement.compatible,
            )
            publishHosts()
        }

        override fun onEndpointLost(endpointId: String) {
            if (owner != attempt) return
            hosts.remove(endpointId)
            publishHosts()
        }
    }

    private fun publishHosts() {
        if (_state.value is LobbyState.Searching) _state.value = LobbyState.Searching(hosts.values.toList())
    }

    private inner class GuestListener(private val owner: Int) : ConnectionListener {
        override fun onConnectionInitiated(endpointId: String, name: String, code: String, incoming: Boolean) {
            val joining = _state.value as? LobbyState.Joining
            if (owner != attempt || joining == null || endpointId != this@NearbyPeerLobby.endpointId) {
                scope.launch { gateway.rejectConnection(endpointId) }
                return
            }
            _state.value = joining.copy(code = code)
            // The guest chose this host by tapping it, so its side accepts at once; the host decides.
            val accepted = newChannel(endpointId)
            scope.launch {
                val status = gateway.acceptConnection(endpointId, accepted)
                if (status != null && owner == attempt && channel === accepted) failJoin(status.toLobbyError())
            }
        }

        override fun onConnectionResult(endpointId: String, connected: Boolean, statusCode: Int) {
            if (owner != attempt || endpointId != this@NearbyPeerLobby.endpointId) return
            val joining = _state.value as? LobbyState.Joining ?: return
            val accepted = channel
            if (connected && accepted != null) {
                _state.value = LobbyState.Connected(ConnectedPeer(accepted, PeerRole.GUEST, myName, joining.host.name, setup = null))
            } else {
                failJoin(if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) LobbyError.DECLINED else LobbyError.FAILED)
            }
        }

        override fun onDisconnected(endpointId: String) = onEndpointGone(owner, endpointId)
    }

    /** Joining didn't work: say why and look for hosts again. */
    private fun failJoin(error: LobbyError) {
        _errors.trySend(error)
        reset()
        startSearching()
    }

    // --- Both --------------------------------------------------------------------------

    override fun stop() {
        if (_state.value is LobbyState.Connected) return
        reset()
        _state.value = LobbyState.Idle
    }

    private fun onEndpointGone(owner: Int, endpointId: String) {
        if (owner != attempt || endpointId != this.endpointId) return
        when (val state = _state.value) {
            // The game hears it from its channel, which then ends (see newChannel).
            is LobbyState.Connected -> channel?.onDisconnected()
            is LobbyState.Joining -> failJoin(LobbyError.HOST_GONE)
            is LobbyState.Hosting -> {
                dropPending()
                _state.value = LobbyState.Hosting(state.setup)
            }
            else -> Unit
        }
    }

    private fun newChannel(endpointId: String): NearbyPeerChannel {
        val created = NearbyPeerChannel(endpointId, gateway, scope) { ended ->
            // The game is over (either side): the lobby is free again.
            if (channel === ended) {
                channel = null
                this.endpointId = null
                if (_state.value is LobbyState.Connected) _state.value = LobbyState.Idle
            }
        }
        channel = created
        return created
    }

    /** Forgets a connection that didn't open, cancelling it at Nearby. */
    private fun dropPending() {
        endpointId?.let(gateway::disconnect)
        endpointId = null
        channel = null
    }

    /** Stops whatever runs (callbacks from it are ignored from now on). */
    private fun reset() {
        attempt++
        gateway.stopAdvertising()
        gateway.stopDiscovery()
        dropPending()
        hosts.clear()
    }

    private fun fail(status: Int) {
        reset()
        _state.value = LobbyState.Idle
        _errors.trySend(status.toLobbyError())
    }

    private fun Int.toLobbyError(): LobbyError = when (this) {
        ConnectionsStatusCodes.MISSING_SETTING_LOCATION_MUST_BE_ON -> LobbyError.LOCATION_OFF
        ConnectionsStatusCodes.STATUS_BLUETOOTH_ERROR,
        ConnectionsStatusCodes.MISSING_PERMISSION_NEARBY_WIFI_DEVICES,
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH,
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADMIN,
        ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_WIFI_STATE,
        ConnectionsStatusCodes.MISSING_PERMISSION_CHANGE_WIFI_STATE,
        ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION,
        ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_FINE_LOCATION,
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_SCAN,
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADVERTISE,
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_CONNECT,
        -> LobbyError.RADIO_OFF
        CommonStatusCodes.API_NOT_CONNECTED,
        CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED,
        CommonStatusCodes.SERVICE_DISABLED,
        -> LobbyError.NOT_AVAILABLE
        else -> LobbyError.FAILED
    }
}
