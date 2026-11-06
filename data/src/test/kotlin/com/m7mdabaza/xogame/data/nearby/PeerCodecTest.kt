package com.m7mdabaza.xogame.data.nearby

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.multiplayer.PROTOCOL_VERSION
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import com.m7mdabaza.xogame.domain.multiplayer.RequestKind
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerCodecTest {

    private val setup = TwoPhoneSetup(BoardSize.EIGHT, MatchLength.BEST_OF_5, Player.O)

    @Test
    fun `every message survives the trip`() {
        val messages = listOf(
            PeerMessage.Hello(PROTOCOL_VERSION, "سارة"),
            PeerMessage.Setup(setup),
            PeerMessage.Ready,
            PeerMessage.Move(stage = 3, index = 7, cell = Cell(6, 2)),
            PeerMessage.Continue(4),
            PeerMessage.Request(5, RequestKind.RestartRound),
            PeerMessage.Request(5, RequestKind.NewMatch(setup)),
            PeerMessage.Answer(5, accepted = true),
            PeerMessage.Leave,
        )
        for (message in messages) assertEquals(message, PeerCodec.decode(PeerCodec.encode(message)))
    }

    @Test
    fun `the hello keeps its shape, and fields a later version adds are ignored`() {
        assertEquals("""{"type":"hello","protocolVersion":1,"name":"Ahmed"}""", PeerCodec.encode(PeerMessage.Hello(1, "Ahmed")).decodeToString())
        val fromLater = """{"type":"hello","protocolVersion":2,"name":"Ahmed","avatar":"cat"}"""
        assertEquals(PeerMessage.Hello(2, "Ahmed"), PeerCodec.decode(fromLater.encodeToByteArray()))
    }

    @Test
    fun `bytes this version doesn't understand decode to nothing`() {
        for (bytes in listOf(
            """{"type":"emoji","stage":1}""",
            """{"type":"setup","setup":{"board":"TEN","length":"SINGLE","first":"X"}}""",
            """{"type":"request","stage":1,"kind":"swap_sides"}""",
            "not json",
            "",
        )) {
            assertNull(bytes, PeerCodec.decode(bytes.encodeToByteArray()))
        }
    }

    @Test
    fun `an announcement carries the version, board, length and name`() {
        val announcement = Announcement.of("Ahmed | the best", setup)
        val read = Announcement.decode(announcement.encode(), fallbackName = "?")
        assertEquals(announcement, read)
        assertTrue(read.compatible)
        assertTrue(announcement.encode().size < 100)
    }

    @Test
    fun `another version's announcement still shows the name, not as playable`() {
        val newer = Announcement.decode("${PROTOCOL_VERSION + 1}|THREE|SINGLE|Sara".encodeToByteArray(), "?")
        assertEquals("Sara", newer.name)
        assertFalse(newer.compatible)
        val unknownBoard = Announcement.decode("$PROTOCOL_VERSION|TEN|SINGLE|Sara".encodeToByteArray(), "?")
        assertFalse(unknownBoard.compatible)
        val garbage = Announcement.decode(byteArrayOf(1, 2, 3), "Nearby name")
        assertEquals("Nearby name", garbage.name)
        assertFalse(garbage.compatible)
        assertFalse(Announcement.decode(null, "x").compatible)
    }
}
