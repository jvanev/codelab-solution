package com.sap.codelab.model

sealed interface GeofenceRegisterResult {
    val resultCode: Int

    /**
     * The geofence was added successfully.
     */
    data object Success : GeofenceRegisterResult {
        override val resultCode: Int = 1
    }

    /**
     * Missing fine or background location permission.
     */
    data object MissingPermission : GeofenceRegisterResult {
        override val resultCode: Int = 2
    }

    /**
     * The location service is off.
     */
    data object LocationUnavailable : GeofenceRegisterResult {
        override val resultCode: Int = 3
    }

    /**
     * The limit of 100 registered geofences has been reached.
     */
    data object LimitReached : GeofenceRegisterResult {
        override val resultCode: Int = 4
    }

    /**
     * The Google Play Services are missing, disabled, or outdated.
     */
    data object PlayServicesUnavailable : GeofenceRegisterResult {
        override val resultCode: Int = 5
    }

    /**
     * General failure.
     */
    data class Failure(val cause: Throwable) : GeofenceRegisterResult {
        override val resultCode: Int = 6
    }
}