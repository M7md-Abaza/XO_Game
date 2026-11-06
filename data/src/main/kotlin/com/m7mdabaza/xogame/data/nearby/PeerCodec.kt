package com.m7mdabaza.xogame.data.nearby

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.multiplayer.PROTOCOL_VERSION
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.RequestKind
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Two-phone messages as bytes: small JSON objects with a "type" field.
 * Unknown fields are ignored, so a later version may add some; the "hello"
 * object must keep its shape forever (see [PeerMessage.Hello]).
 */
internal object PeerCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun encode(message: PeerMessage): ByteArray = json.encodeToString(Wire.serializer(), message.toWire()).encodeToByteArray()

    /** The message, or null if the bytes aren't one this version understands. */
    fun decode(bytes: ByteArray): PeerMessage? =
        runCatching { json.decodeFromString(Wire.serializer(), bytes.decodeToString()).toMessage() }.getOrNull()

    @Serializable
    private sealed interface Wire {
        @Serializable
        @SerialName("hello")
        data class Hello(val protocolVersion: Int, val name: String) : Wire

        @Serializable
        @SerialName("setup")
        data class Setup(val setup: SetupWire) : Wire

        @Serializable
        @SerialName("ready")
        data object Ready : Wire

        @Serializable
        @SerialName("move")
        data class Move(val stage: Int, val index: Int, val row: Int, val col: Int) : Wire

        @Serializable
        @SerialName("continue")
        data class Continue(val stage: Int) : Wire

        @Serializable
        @SerialName("request")
        data class Request(val stage: Int, val kind: String, val setup: SetupWire? = null) : Wire

        @Serializable
        @SerialName("answer")
        data class Answer(val stage: Int, val accepted: Boolean) : Wire

        @Serializable
        @SerialName("leave")
        data object Leave : Wire
    }

    @Serializable
    private data class SetupWire(val board: String, val length: String, val first: String)

    private const val RESTART_ROUND = "restart_round"
    private const val NEW_MATCH = "new_match"

    private fun PeerMessage.toWire(): Wire = when (this) {
        is PeerMessage.Hello -> Wire.Hello(protocolVersion, name)
        is PeerMessage.Setup -> Wire.Setup(setup.toWire())
        PeerMessage.Ready -> Wire.Ready
        is PeerMessage.Move -> Wire.Move(stage, index, cell.row, cell.col)
        is PeerMessage.Continue -> Wire.Continue(stage)
        is PeerMessage.Request -> when (val kind = kind) {
            RequestKind.RestartRound -> Wire.Request(stage, RESTART_ROUND)
            is RequestKind.NewMatch -> Wire.Request(stage, NEW_MATCH, kind.setup.toWire())
        }
        is PeerMessage.Answer -> Wire.Answer(stage, accepted)
        PeerMessage.Leave -> Wire.Leave
    }

    /** Throws on values this version doesn't know (caught in [decode]). */
    private fun Wire.toMessage(): PeerMessage = when (this) {
        is Wire.Hello -> PeerMessage.Hello(protocolVersion, name)
        is Wire.Setup -> PeerMessage.Setup(setup.toSetup())
        Wire.Ready -> PeerMessage.Ready
        is Wire.Move -> PeerMessage.Move(stage, index, Cell(row, col))
        is Wire.Continue -> PeerMessage.Continue(stage)
        is Wire.Request -> PeerMessage.Request(
            stage,
            when (kind) {
                RESTART_ROUND -> RequestKind.RestartRound
                NEW_MATCH -> RequestKind.NewMatch(checkNotNull(setup).toSetup())
                else -> error("Unknown request $kind")
            },
        )
        is Wire.Answer -> PeerMessage.Answer(stage, accepted)
        Wire.Leave -> PeerMessage.Leave
    }

    private fun TwoPhoneSetup.toWire() = SetupWire(boardSize.name, matchLength.name, firstPlayer.name)

    private fun SetupWire.toSetup() = TwoPhoneSetup(BoardSize.valueOf(board), MatchLength.valueOf(length), Player.valueOf(first))
}

/**
 * What a host advertises to phones looking for a game: its protocol version,
 * board, match length and name, as `1|THREE|BEST_OF_3|Ahmed` in UTF-8. Kept
 * short: Bluetooth advertisements are small.
 */
internal data class Announcement(val protocolVersion: Int, val name: String, val boardSize: BoardSize?, val matchLength: MatchLength?) {

    val compatible: Boolean get() = protocolVersion == PROTOCOL_VERSION && boardSize != null && matchLength != null

    fun encode(): ByteArray = listOf(protocolVersion, boardSize?.name.orEmpty(), matchLength?.name.orEmpty(), name).joinToString(SEPARATOR).encodeToByteArray()

    companion object {
        private const val SEPARATOR = "|"

        fun of(name: String, setup: TwoPhoneSetup) = Announcement(PROTOCOL_VERSION, name, setup.boardSize, setup.matchLength)

        /** Reads an announcement; [fallbackName] (Nearby's endpoint name) if the bytes are unreadable. */
        fun decode(bytes: ByteArray?, fallbackName: String): Announcement {
            val parts = bytes?.decodeToString()?.split(SEPARATOR, limit = 4)
            val version = parts?.getOrNull(0)?.toIntOrNull()
            if (parts == null || parts.size < 4 || version == null) return Announcement(-1, fallbackName, null, null)
            // Another version may use other boards or lengths; show the name, but not as playable.
            val board = BoardSize.entries.firstOrNull { it.name == parts[1] }
            val length = MatchLength.entries.firstOrNull { it.name == parts[2] }
            return Announcement(version, parts[3], board, length)
        }
    }
}
