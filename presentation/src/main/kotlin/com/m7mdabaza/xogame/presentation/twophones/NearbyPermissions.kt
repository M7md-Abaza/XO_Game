package com.m7mdabaza.xogame.presentation.twophones

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * The runtime permissions Nearby Connections needs on this Android version
 * (the manifest entries are in :data). Until Android 12L that includes
 * location, because Android counts Bluetooth scanning as location.
 */
// The newer permission names are plain strings, used only on the versions that have them.
@SuppressLint("InlinedApi")
fun nearbyPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> = when {
    sdk >= Build.VERSION_CODES.TIRAMISU -> listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
    )
    sdk >= Build.VERSION_CODES.S -> listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    sdk >= Build.VERSION_CODES.Q -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
}

/** The permission Android shows is called "location" before Android 13. */
fun nearbyPermissionIsLocation(sdk: Int = Build.VERSION.SDK_INT): Boolean = sdk < Build.VERSION_CODES.TIRAMISU

fun hasNearbyPermissions(context: Context): Boolean =
    nearbyPermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

fun isBluetoothOn(context: Context): Boolean =
    context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true

/** Android 11 and older only report nearby Bluetooth devices while Location is on. */
fun isLocationNeededButOff(context: Context): Boolean {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) return false
    val manager = context.getSystemService(LocationManager::class.java) ?: return false
    return !LocationManagerCompat.isLocationEnabled(manager)
}

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun openLocationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
