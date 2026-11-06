package com.m7mdabaza.xogame.data.nearby

import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.multiplayer.JoinRequest
import com.m7mdabaza.xogame.domain.multiplayer.LobbyError
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.NearbyHost
import com.m7mdabaza.xogame.domain.multiplayer.PROTOCOL_VERSION
import com.m7mdabaza.xogame.domain.multiplayer.PeerEvent
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.PeerRole
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records what the lobby asks of Nearby and lets the test play Nearby's part. */
internal class FakeNearbyGateway : NearbyGateway {
    override var isAvailable = true

    var advertiseStatus: Int? = null
    var discoverStatus: Int? = null
    var requestStatus: Int? = null
    var sendWorks = true

    var advertisedInfo: ByteArray? = null
    var hostListener: ConnectionListener? = null
    var discovery: DiscoveryListener? = null
    var guestListener: ConnectionListener? = null
    val requests = mutableListOf<Pair<String, String>>()
    val payloadListeners = mutableMapOf<String, PayloadListener>()
    val rejected = mutableListOf<String>()
    val disconnected = mutableListOf<String>()
    val sent = mutableListOf<Pair<Long, PeerMessage?>>()
    var advertising = false
    var discovering = false
    private var nextPayloadId = 100L

    override suspend fun startAdvertising(endpointInfo: ByteArray, listener: ConnectionListener): Int? {
        advertisedInfo = endpointInfo
        hostListener = listener
        advertising = advertiseStatus == null
        return advertiseStatus
    }

    override fun stopAdvertising() {
        advertising = false
    }

    override suspend fun startDiscovery(listener: DiscoveryListener): Int? {
        discovery = listener
        discovering = discoverStatus == null
        return discoverStatus
    }

    override fun stopDiscovery() {
        discovering = false
    }

    override suspend fun requestConnection(name: String, endpointId: String, listener: ConnectionListener): Int? {
        requests += name to endpointId
        guestListener = listener
        return requestStatus
    }

    override suspend fun acceptConnection(endpointId: String, listener: PayloadListener): Int? {
        payloadListeners[endpointId] = listener
        return null
    }

    override suspend fun rejectConnection(endpointId: String): Int? {
        rejected += endpointId
        return null
    }

    override suspend fun send(endpointId: String, bytes: ByteArray): Long? {
        if (!sendWorks) return null
        val id = nextPayloadId++
        sent += id to PeerCodec.decode(bytes)
        return id
    }

    override fun disconnect(endpointId: String) {
        disconnected += endpointId
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyPeerLobbyTest {

    private val gateway = FakeNearbyGateway()
    private val setup = TwoPhoneSetup(BoardSize.FOUR, MatchLength.BEST_OF_3, Player.X)

    private fun test(block: suspend TestScope.(NearbyPeerLobby) -> Unit) = runTest(UnconfinedTestDispatcher()) {
        block(NearbyPeerLobby(gateway, backgroundScope))
    }

    private fun TestScope.errorsOf(lobby: NearbyPeerLobby): List<LobbyError> {
        val errors = mutableListOf<LobbyError>()
        backgroundScope.launch { lobby.errors.collect { errors += it } }
        return errors
    }

    private fun announce(name: String, version: Int = PROTOCOL_VERSION) = "$version|THREE|SINGLE|$name".encodeToByteArray()

    // --- Host ------------------------------------------------------------------------------

    @Test
    fun `the host advertises its game and shows who wants to join, with the code`() = test { lobby ->
        lobby.host("Ahmed", setup)
        assertEquals(LobbyState.Hosting(setup), lobby.state.value)
        assertEquals(Announcement.of("Ahmed", setup), Announcement.decode(gateway.advertisedInfo, "?"))

        gateway.hostListener!!.onConnectionInitiated("guest-1", "Sara", "4821", incoming = true)
        assertEquals(LobbyState.Hosting(setup, JoinRequest("Sara", "4821")), lobby.state.value)
    }

    @Test
    fun `accepting connects, stops advertising and hands over the connection`() = test { lobby ->
        lobby.host("Ahmed", setup)
        gateway.hostListener!!.onConnectionInitiated("guest-1", "Sara", "4821", incoming = true)
        lobby.answer(accept = true)
        assertEquals(LobbyState.Hosting(setup, JoinRequest("Sara", "4821", accepted = true)), lobby.state.value)
        assertTrue("guest-1" in gateway.payloadListeners)

        gateway.hostListener!!.onConnectionResult("guest-1", connected = true, statusCode = 0)
        val peer = (lobby.state.value as LobbyState.Connected).peer
        assertEquals(PeerRole.HOST, peer.role)
        assertEquals("Sara", peer.friendName)
        assertEquals(setup, peer.setup)
        assertEquals(false, gateway.advertising)
    }

    @Test
    fun `declining turns the guest away and keeps waiting`() = test { lobby ->
        lobby.host("Ahmed", setup)
        gateway.hostListener!!.onConnectionInitiated("guest-1", "Sara", "4821", incoming = true)
        lobby.answer(accept = false)
        assertEquals(listOf("guest-1"), gateway.rejected)
        assertEquals(LobbyState.Hosting(setup), lobby.state.value)
        assertTrue(gateway.advertising)

        gateway.hostListener!!.onConnectionInitiated("guest-2", "Omar", "1111", incoming = true)
        assertEquals(JoinRequest("Omar", "1111"), (lobby.state.value as LobbyState.Hosting).request)
    }

    @Test
    fun `a second request while one is shown is turned away`() = test { lobby ->
        lobby.host("Ahmed", setup)
        gateway.hostListener!!.onConnectionInitiated("guest-1", "Sara", "4821", incoming = true)
        gateway.hostListener!!.onConnectionInitiated("guest-2", "Omar", "1111", incoming = true)
        assertEquals(listOf("guest-2"), gateway.rejected)
        assertEquals(JoinRequest("Sara", "4821"), (lobby.state.value as LobbyState.Hosting).request)
    }

    @Test
    fun `when the guest gives up first the request goes away`() = test { lobby ->
        lobby.host("Ahmed", setup)
        gateway.hostListener!!.onConnectionInitiated("guest-1", "Sara", "4821", incoming = true)
        gateway.hostListener!!.onConnectionResult("guest-1", connected = false, statusCode = ConnectionsStatusCodes.STATUS_ERROR)
        assertEquals(LobbyState.Hosting(setup), lobby.state.value)
    }

    @Test
    fun `failures to start say why`() = test { lobby ->
        val errors = errorsOf(lobby)
        val cases = mapOf(
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADVERTISE to LobbyError.RADIO_OFF,
            ConnectionsStatusCodes.STATUS_BLUETOOTH_ERROR to LobbyError.RADIO_OFF,
            ConnectionsStatusCodes.MISSING_SETTING_LOCATION_MUST_BE_ON to LobbyError.LOCATION_OFF,
            CommonStatusCodes.API_NOT_CONNECTED to LobbyError.NOT_AVAILABLE,
            ConnectionsStatusCodes.STATUS_ERROR to LobbyError.FAILED,
        )
        for ((status, _) in cases) {
            gateway.advertiseStatus = status
            lobby.host("Ahmed", setup)
            assertEquals(LobbyState.Idle, lobby.state.value)
        }
        assertEquals(cases.values.toList(), errors)
    }

    // --- Guest -----------------------------------------------------------------------------

    @Test
    fun `searching lists the hosts found, marks other versions, and forgets lost ones`() = test { lobby ->
        lobby.search("Sara")
        assertEquals(LobbyState.Searching(), lobby.state.value)
        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        gateway.discovery!!.onEndpointFound("host-2", announce("Omar", PROTOCOL_VERSION + 1), "")
        assertEquals(
            listOf(
                NearbyHost("host-1", "Ahmed", BoardSize.THREE, MatchLength.SINGLE, compatible = true),
                NearbyHost("host-2", "Omar", null, null, compatible = false),
            ),
            (lobby.state.value as LobbyState.Searching).hosts,
        )
        gateway.discovery!!.onEndpointLost("host-1")
        assertEquals(listOf("host-2"), (lobby.state.value as LobbyState.Searching).hosts.map { it.id })
    }

    @Test
    fun `joining stops searching, accepts its own side with the code, and connects`() = test { lobby ->
        lobby.search("Sara")
        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        lobby.join("host-1")
        assertEquals(false, gateway.discovering)
        assertEquals(listOf("Sara" to "host-1"), gateway.requests)
        assertEquals("host-1", (lobby.state.value as LobbyState.Joining).host.id)

        gateway.guestListener!!.onConnectionInitiated("host-1", "Ahmed", "4821", incoming = false)
        assertEquals("4821", (lobby.state.value as LobbyState.Joining).code)
        assertTrue("host-1" in gateway.payloadListeners)

        gateway.guestListener!!.onConnectionResult("host-1", connected = true, statusCode = 0)
        val peer = (lobby.state.value as LobbyState.Connected).peer
        assertEquals(PeerRole.GUEST, peer.role)
        assertEquals("Ahmed", peer.friendName)
        assertNull(peer.setup)
    }

    @Test
    fun `a host from another version can't be joined`() = test { lobby ->
        lobby.search("Sara")
        gateway.discovery!!.onEndpointFound("host-2", announce("Omar", PROTOCOL_VERSION + 1), "")
        lobby.join("host-2")
        assertTrue(lobby.state.value is LobbyState.Searching)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun `a refusal or a lost host goes back to searching and says so`() = test { lobby ->
        val errors = errorsOf(lobby)
        lobby.search("Sara")
        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        lobby.join("host-1")
        gateway.guestListener!!.onConnectionInitiated("host-1", "Ahmed", "4821", incoming = false)
        gateway.guestListener!!.onConnectionResult("host-1", connected = false, statusCode = ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED)
        assertEquals(LobbyState.Searching(), lobby.state.value)
        assertTrue(gateway.discovering)

        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        lobby.join("host-1")
        gateway.guestListener!!.onDisconnected("host-1")
        assertTrue(lobby.state.value is LobbyState.Searching)

        gateway.requestStatus = ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN
        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        lobby.join("host-1")
        assertEquals(listOf(LobbyError.DECLINED, LobbyError.HOST_GONE, LobbyError.HOST_GONE), errors)
    }

    // --- Both ------------------------------------------------------------------------------

    @Test
    fun `stop ends hosting or searching, and later callbacks are ignored`() = test { lobby ->
        lobby.host("Ahmed", setup)
        val oldListener = gateway.hostListener!!
        lobby.stop()
        assertEquals(LobbyState.Idle, lobby.state.value)
        assertEquals(false, gateway.advertising)
        oldListener.onConnectionInitiated("guest-1", "Sara", "4821", incoming = true)
        assertEquals(LobbyState.Idle, lobby.state.value)
        assertEquals(listOf("guest-1"), gateway.rejected)

        lobby.search("Sara")
        val oldDiscovery = gateway.discovery!!
        lobby.stop()
        oldDiscovery.onEndpointFound("host-1", announce("Ahmed"), "")
        assertEquals(LobbyState.Idle, lobby.state.value)
    }

    @Test
    fun `stop cancels a pending join at Nearby`() = test { lobby ->
        lobby.search("Sara")
        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        lobby.join("host-1")
        lobby.stop()
        assertEquals(listOf("host-1"), gateway.disconnected)
        assertEquals(LobbyState.Idle, lobby.state.value)
    }

    private suspend fun TestScope.connectedGuest(lobby: NearbyPeerLobby): NearbyPeerChannel {
        lobby.search("Sara")
        gateway.discovery!!.onEndpointFound("host-1", announce("Ahmed"), "")
        lobby.join("host-1")
        gateway.guestListener!!.onConnectionInitiated("host-1", "Ahmed", "4821", incoming = false)
        gateway.guestListener!!.onConnectionResult("host-1", connected = true, statusCode = 0)
        return (lobby.state.value as LobbyState.Connected).peer.channel as NearbyPeerChannel
    }

    @Test
    fun `the connection carries messages both ways`() = test { lobby ->
        val channel = connectedGuest(lobby)
        val events = mutableListOf<PeerEvent>()
        backgroundScope.launch { channel.events.collect { events += it } }

        gateway.payloadListeners["host-1"]!!.onBytes(PeerCodec.encode(PeerMessage.Move(0, 0, Cell(1, 1))))
        assertEquals(listOf(PeerEvent.Received(PeerMessage.Move(0, 0, Cell(1, 1)))), events)

        channel.send(PeerMessage.Ready)
        assertEquals(PeerMessage.Ready, gateway.sent.single().second)
    }

    @Test
    fun `stop leaves a connected game alone`() = test { lobby ->
        connectedGuest(lobby)
        lobby.stop()
        assertTrue(lobby.state.value is LobbyState.Connected)
        assertTrue(gateway.disconnected.isEmpty())
    }

    @Test
    fun `when the friend's phone goes away the game hears it and the lobby is free again`() = test { lobby ->
        val channel = connectedGuest(lobby)
        val events = mutableListOf<PeerEvent>()
        backgroundScope.launch { channel.events.collect { events += it } }
        gateway.guestListener!!.onDisconnected("host-1")
        assertEquals(listOf(PeerEvent.Disconnected), events)
        assertEquals(LobbyState.Idle, lobby.state.value)
    }

    @Test
    fun `unreadable bytes hang up`() = test { lobby ->
        val channel = connectedGuest(lobby)
        val events = mutableListOf<PeerEvent>()
        backgroundScope.launch { channel.events.collect { events += it } }
        gateway.payloadListeners["host-1"]!!.onBytes("nonsense".encodeToByteArray())
        assertEquals(listOf(PeerEvent.Disconnected), events)
        assertEquals(listOf("host-1"), gateway.disconnected)
    }

    @Test
    fun `closing waits for the goodbye to arrive before hanging up`() = test { lobby ->
        val channel = connectedGuest(lobby)
        channel.send(PeerMessage.Leave)
        channel.close()
        runCurrent()
        assertTrue(gateway.disconnected.isEmpty())
        gateway.payloadListeners["host-1"]!!.onSent(gateway.sent.single().first)
        runCurrent()
        assertEquals(listOf("host-1"), gateway.disconnected)
        assertEquals(LobbyState.Idle, lobby.state.value)
    }

    @Test
    fun `closing hangs up anyway if the goodbye isn't confirmed in time`() = test { lobby ->
        val channel = connectedGuest(lobby)
        channel.send(PeerMessage.Leave)
        channel.close()
        advanceTimeBy(NearbyPeerChannel.CLOSE_WAIT_MILLIS + 1)
        runCurrent()
        assertEquals(listOf("host-1"), gateway.disconnected)
    }

    @Test
    fun `a failed send ends the connection`() = test { lobby ->
        val channel = connectedGuest(lobby)
        gateway.sendWorks = false
        val events = mutableListOf<PeerEvent>()
        backgroundScope.launch { channel.events.collect { events += it } }
        channel.send(PeerMessage.Ready)
        assertEquals(listOf(PeerEvent.Disconnected), events)
        assertEquals(LobbyState.Idle, lobby.state.value)
    }
}
