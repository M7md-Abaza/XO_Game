package com.m7mdabaza.xogame.data.nearby

/** What a connection reports (Nearby's `ConnectionLifecycleCallback`, simplified). */
internal interface ConnectionListener {
    /** A connection is being set up; both phones show [code]. [incoming]: the other phone asked. */
    fun onConnectionInitiated(endpointId: String, name: String, code: String, incoming: Boolean)

    /** Both sides accepted ([connected]), or one rejected it or it failed ([statusCode]). */
    fun onConnectionResult(endpointId: String, connected: Boolean, statusCode: Int)

    fun onDisconnected(endpointId: String)
}

/** What searching reports (Nearby's `EndpointDiscoveryCallback`). */
internal interface DiscoveryListener {
    fun onEndpointFound(endpointId: String, endpointInfo: ByteArray?, endpointName: String)

    fun onEndpointLost(endpointId: String)
}

/** What an accepted connection delivers. */
internal interface PayloadListener {
    fun onBytes(bytes: ByteArray)

    /** An outgoing payload arrived or failed. */
    fun onSent(payloadId: Long)
}

/**
 * The part of Google's Nearby Connections the lobby uses, so the lobby can be
 * tested with a fake. The calls return null on success, or a Nearby status
 * code (`ConnectionsStatusCodes` / `CommonStatusCodes`).
 */
internal interface NearbyGateway {
    val isAvailable: Boolean

    suspend fun startAdvertising(endpointInfo: ByteArray, listener: ConnectionListener): Int?

    fun stopAdvertising()

    suspend fun startDiscovery(listener: DiscoveryListener): Int?

    fun stopDiscovery()

    suspend fun requestConnection(name: String, endpointId: String, listener: ConnectionListener): Int?

    suspend fun acceptConnection(endpointId: String, listener: PayloadListener): Int?

    suspend fun rejectConnection(endpointId: String): Int?

    /** Sends [bytes]; the payload id on success (see [PayloadListener.onSent]), or null when it failed. */
    suspend fun send(endpointId: String, bytes: ByteArray): Long?

    /** Ends a connection or cancels a pending one. */
    fun disconnect(endpointId: String)
}
