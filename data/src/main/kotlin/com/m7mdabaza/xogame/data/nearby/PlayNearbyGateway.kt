package com.m7mdabaza.xogame.data.nearby

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionOptions
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * [NearbyGateway] on Google Play services' Nearby Connections: one friend at a
 * time (point to point), and a non-disruptive connection type, so Nearby
 * never takes the phone off its Wi-Fi (both phones keep their internet, for
 * ads). Callbacks arrive on the main thread.
 */
@Singleton
internal class PlayNearbyGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : NearbyGateway {

    private val client by lazy { Nearby.getConnectionsClient(context) }

    override val isAvailable: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    override suspend fun startAdvertising(endpointInfo: ByteArray, listener: ConnectionListener): Int? {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .setConnectionType(ConnectionType.NON_DISRUPTIVE)
            .build()
        return client.startAdvertising(endpointInfo, SERVICE_ID, listener.toCallback(), options).status()
    }

    override fun stopAdvertising() = client.stopAdvertising()

    override suspend fun startDiscovery(listener: DiscoveryListener): Int? {
        val callback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                listener.onEndpointFound(endpointId, info.endpointInfo, info.endpointName)
            }

            override fun onEndpointLost(endpointId: String) = listener.onEndpointLost(endpointId)
        }
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        return client.startDiscovery(SERVICE_ID, callback, options).status()
    }

    override fun stopDiscovery() = client.stopDiscovery()

    override suspend fun requestConnection(name: String, endpointId: String, listener: ConnectionListener): Int? {
        val options = ConnectionOptions.Builder().setConnectionType(ConnectionType.NON_DISRUPTIVE).build()
        return client.requestConnection(name, endpointId, listener.toCallback(), options).status()
    }

    override suspend fun acceptConnection(endpointId: String, listener: PayloadListener): Int? {
        val callback = object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                if (payload.type == Payload.Type.BYTES) payload.asBytes()?.let(listener::onBytes)
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) listener.onSent(update.payloadId)
            }
        }
        return client.acceptConnection(endpointId, callback).status()
    }

    override suspend fun rejectConnection(endpointId: String): Int? = client.rejectConnection(endpointId).status()

    override suspend fun send(endpointId: String, bytes: ByteArray): Long? {
        val payload = Payload.fromBytes(bytes)
        return if (client.sendPayload(endpointId, payload).status() == null) payload.id else null
    }

    override fun disconnect(endpointId: String) = client.disconnectFromEndpoint(endpointId)

    private fun ConnectionListener.toCallback() = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) =
            this@toCallback.onConnectionInitiated(endpointId, info.endpointName, info.authenticationDigits, info.isIncomingConnection)

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) =
            this@toCallback.onConnectionResult(endpointId, resolution.status.isSuccess, resolution.status.statusCode)

        override fun onDisconnected(endpointId: String) = this@toCallback.onDisconnected(endpointId)
    }

    /** Null when the task succeeded, else its status code. */
    private suspend fun Task<*>.status(): Int? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(null) }
        addOnFailureListener { e ->
            if (continuation.isActive) continuation.resume((e as? ApiException)?.statusCode ?: CommonStatusCodes.ERROR)
        }
        addOnCanceledListener { if (continuation.isActive) continuation.resume(CommonStatusCodes.CANCELED) }
    }

    companion object {
        /** Only phones running this app see each other. Never change it, or versions stop finding each other. */
        const val SERVICE_ID = "com.m7mdabaza.x_ogame.nearby"
    }
}
