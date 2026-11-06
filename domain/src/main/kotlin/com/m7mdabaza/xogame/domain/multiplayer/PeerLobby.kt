package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.MatchLength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A nearby phone hosting a game, as a phone looking for one sees it.
 *
 * @property boardSize the host's board and [matchLength] its match length;
 *   null when the host's app is too old or too new to play with this one.
 * @property compatible both phones run the same [PROTOCOL_VERSION].
 */
data class NearbyHost(
    val id: String,
    val name: String,
    val boardSize: BoardSize?,
    val matchLength: MatchLength?,
    val compatible: Boolean,
)

/** Someone asks to join this phone's game. Both phones show the same [code]. */
data class JoinRequest(val name: String, val code: String, val accepted: Boolean = false)

/**
 * An open connection, ready for the game (see [TwoPhoneSession]). [myName] is
 * this player's name as sent; [setup] is the host's, null on the guest.
 */
data class ConnectedPeer(
    val channel: PeerChannel,
    val role: PeerRole,
    val myName: String,
    val friendName: String,
    val setup: TwoPhoneSetup?,
)

/** Where finding a friend stands. */
sealed interface LobbyState {
    data object Idle : LobbyState

    /** Visible to nearby phones and waiting; [request] once someone asks to join. */
    data class Hosting(val setup: TwoPhoneSetup, val request: JoinRequest? = null) : LobbyState

    /** Looking for nearby hosts. */
    data class Searching(val hosts: List<NearbyHost> = emptyList()) : LobbyState

    /** Asked [host] to play and waits for them to accept; [code] once both phones show it. */
    data class Joining(val host: NearbyHost, val code: String? = null) : LobbyState

    /** Both accepted: the game takes over [peer]. */
    data class Connected(val peer: ConnectedPeer) : LobbyState
}

enum class LobbyError {
    /** This phone can't connect to nearby phones (no Google Play services, or too old). */
    NOT_AVAILABLE,

    /** A permission is missing or Bluetooth is off. */
    RADIO_OFF,

    /** Location is off; Android 11 and older need it to find nearby phones. */
    LOCATION_OFF,

    /** The host said no. */
    DECLINED,

    /** The host stopped hosting or went out of range. */
    HOST_GONE,

    /** Anything else; trying again usually works. */
    FAILED,
}

/**
 * Finding a friend's phone nearby and connecting to it (Nearby Connections in
 * the app). The host advertises its game; the guest searches, picks a host and
 * asks to join; both phones show the same code and the host accepts.
 *
 * One lobby for the app: the connection it makes outlives the lobby screen and
 * is ended by the game (`PeerChannel.close`).
 */
interface PeerLobby {
    val state: StateFlow<LobbyState>

    /** Things that went wrong, each shown once. */
    val errors: Flow<LobbyError>

    /** Whether this phone can play over two phones at all (e.g. it has Google Play services). */
    val isAvailable: Boolean

    /** Starts hosting [setup] as [name]. */
    fun host(name: String, setup: TwoPhoneSetup)

    /** Starts looking for hosts; [name] is what the host will see. */
    fun search(name: String)

    /** Asks the host with [hostId] (from [LobbyState.Searching]) to play. */
    fun join(hostId: String)

    /** The host answers the [JoinRequest]. */
    fun answer(accept: Boolean)

    /** Stops hosting or searching and drops a pending request. A [LobbyState.Connected] game is left alone. */
    fun stop()
}
