package com.sap.codelab.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * @return `true` if the specified [permission] is granted, `false` otherwise.
 */
internal fun Context.isPermissionGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/**
 * @return `true` if `android.permission.ACCESS_BACKGROUND_LOCATION` is granted
 *         or not required (below API Level 29, Android Q), `false` otherwise.
 */
internal fun Context.hasBackgroundLocationPermission() =
    !isSdkAtLeast(Build.VERSION_CODES.Q) ||
            isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

/**
 * @return `true` if the required Geofencing permissions are granted, `false` otherwise.
 */
internal fun Context.hasGeofencingPermissions() =
    hasBackgroundLocationPermission() &&
            isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)

/**
 * @return `true` if `Manifest.permission.POST_NOTIFICATIONS` is granted
 *         or not required (below API Level 33, Android Tiramisu), `false` otherwise.
 */
internal fun Context.hasNotificationPermission() =
    !isSdkAtLeast(Build.VERSION_CODES.TIRAMISU) ||
            isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)

/**
 * @return `true` if the Google Play Services are available on the device;
 *         `false` if outdated, disabled, or not installed at all.
 */
internal fun Context.isPlayServicesAvailable() = GoogleApiAvailability.getInstance()
    .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS