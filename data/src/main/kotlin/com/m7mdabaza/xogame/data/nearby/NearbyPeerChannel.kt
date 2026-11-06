package com.m7mdabaza.xogame.data.nearby

import com.m7mdabaza.xogame.domain.multiplayer.PeerChannel
import com.m7mdabaza.xogame.domain.multiplayer.PeerEvent
import com.m7mdabaza.xogame.domain.multiplayer.PeerMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A [PeerChannel] over one Nearby connection. It is created when this phone
 * accepts the connection, so it gets the friend's messages from the first
 * one. Used on the main thread, where Nearby delivers its callbacks.
 *
 * @param onEnded called once when the connection ends, whichever side ended it.
 */
internal class NearbyPeerChannel(
    private val endpointId: String,
    private val gateway: NearbyGateway,
    private val scope: CoroutineScope,
    private val onEnded: (NearbyPeerChannel) -> Unit,
) : PeerChannel, PayloadListener {

    private val inbox = Channel<PeerEvent>(Channel.UNLIMITED)
    override val events: Flow<PeerEvent> = inbox.receiveAsFlow()

    private val sendLock = Mutex()

    /** Sent payloads Nearby hasn't confirmed yet (arrived or failed), and confirmations that came first. */
    private val unconfirmed = mutableMapOf<Long, CompletableDeferred<Unit>>()
    private val confirmedEarly = mutableSetOf<Long>()

    private var closing = false
    private var ended = false

    override fun onBytes(bytes: ByteArray) {
        if (ended) return
        val message = PeerCodec.decode(bytes)
        if (message == null) {
            // Not a message this version understands (the hello already matched versions): hang up.
            gateway.disconnect(endpointId)
            end()
        } else {
            inbox.trySend(PeerEvent.Received(message))
        }
    }

    override fun onSent(payloadId: Long) {
        unconfirmed.remove(payloadId)?.complete(Unit) ?: confirmedEarly.add(payloadId)
    }

    override suspend fun send(message: PeerMessage) {
        sendLock.withLock {
            if (closing || ended) return
            val id = gateway.send(endpointId, PeerCodec.encode(message))
            if (id == null) {
                end() // The connection is gone; Nearby will say so too.
            } else if (!confirmedEarly.remove(id)) {
                unconfirmed[id] = CompletableDeferred()
            }
        }
    }

    /** Hangs up once the messages already sent (the goodbye) have arrived, or after a short wait. */
    override fun close() {
        if (closing || ended) return
        closing = true
        scope.launch {
            sendLock.withLock { }
            withTimeoutOrNull(CLOSE_WAIT_MILLIS) { unconfirmed.values.toList().awaitAll() }
            gateway.disconnect(endpointId)
            end()
        }
    }

    /** Nearby reported the connection gone. */
    fun onDisconnected() = end()

    private fun end() {
        if (ended) return
        ended = true
        inbox.trySend(PeerEvent.Disconnected)
        inbox.close()
        unconfirmed.values.forEach { it.complete(Unit) }
        unconfirmed.clear()
        onEnded(this)
    }

    companion object {
        const val CLOSE_WAIT_MILLIS = 1_500L
    }
}
