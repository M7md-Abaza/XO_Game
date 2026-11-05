package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import kotlinx.coroutines.flow.Flow

/**
 * The version of the two-phone messages below. Two phones play together only
 * when their versions are equal; raise it whenever a message changes meaning.
 */
const val PROTOCOL_VERSION = 1

/** Longest player name sent to the friend. */
const val MAX_PEER_NAME_LENGTH = 20

/** Which side of the connection this phone is. The host plays X, the guest O. */
enum class PeerRole {
    /** Chose the match and waited for a friend. */
    HOST,

    /** Found the host and joined. */
    GUEST;

    val symbol: Player get() = if (this == HOST) Player.X else Player.O
}

/**
 * What a two-phone match is played with. [firstPlayer] is concrete: a "random"
 * choice is resolved by the phone that proposes the setup, so both phones agree.
 */
data class TwoPhoneSetup(val boardSize: BoardSize, val matchLength: MatchLength, val firstPlayer: Player)

/**
 * Everything the two phones say to each other. Messages arrive in the order
 * they were sent (see [PeerChannel]).
 *
 * Messages that change the board carry the [stage] they were made at: a
 * counter both phones raise together each time the board is reset (next
 * round, restart, new match, rematch). It lets each phone check that the
 * friend is on the same board.
 */
sealed interface PeerMessage {

    /**
     * The first message from both phones. Its shape must never change: phones
     * with other versions read it to tell the player which one to update.
     */
    data class Hello(val protocolVersion: Int, val name: String) : PeerMessage

    /** Host → guest, after the hellos: the match to play. */
    data class Setup(val setup: TwoPhoneSetup) : PeerMessage

    /** Guest → host: the setup arrived, the match starts. */
    data object Ready : PeerMessage

    /** A mark placed by the sender; [index] is its number in the round (0 for the first move). */
    data class Move(val stage: Int, val index: Int, val cell: Cell) : PeerMessage

    /**
     * After a finished round the sender wants to go on: the next round, or a
     * rematch when the match is over. The board resets once both phones sent it.
     */
    data class Continue(val stage: Int) : PeerMessage

    /** The sender asks to restart the round or start a new match; the friend answers. */
    data class Request(val stage: Int, val kind: RequestKind) : PeerMessage

    /** The answer to the friend's [Request]. */
    data class Answer(val stage: Int, val accepted: Boolean) : PeerMessage

    /** The sender leaves the game; the connection closes. */
    data object Leave : PeerMessage
}

/** What a [PeerMessage.Request] asks for. */
sealed interface RequestKind {
    /** Start the round being played again from an empty board; the score stays. */
    data object RestartRound : RequestKind

    /** Start a new match with [setup]; the score resets. */
    data class NewMatch(val setup: TwoPhoneSetup) : RequestKind
}

/** What happens on an open connection. */
sealed interface PeerEvent {
    data class Received(val message: PeerMessage) : PeerEvent

    /** The connection ended (the friend closed it, went out of range, ...). Comes once, last. */
    data object Disconnected : PeerEvent
}

/**
 * An open connection to the friend's phone (Nearby Connections in the app, an
 * in-memory pair in tests). Messages arrive complete and in the order they
 * were sent.
 */
interface PeerChannel {
    /** The friend's messages, then [PeerEvent.Disconnected]. Collected once. */
    val events: Flow<PeerEvent>

    suspend fun send(message: PeerMessage)

    /** Closes the connection after the messages already sent; the friend gets [PeerEvent.Disconnected]. */
    fun close()
}
