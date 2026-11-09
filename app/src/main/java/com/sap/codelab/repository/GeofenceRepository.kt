package com.sap.codelab.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.PendingIntentCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.model.Memo
import com.sap.codelab.receiver.GeofenceReceiver
import com.sap.codelab.utils.hasGeofencingPermissions
import com.sap.codelab.utils.isPlayServicesAvailable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal object GeofenceRepository : IGeofenceRepository {
    /**
     * Holds the maximum number of geofences that can be registered.
     * The Geofencing API will drop requests for registering geofences past this limit.
     */
    const val GEOFENCES_LIMIT = 100

    private const val TRIGGER_RADIUS = 200f
    private const val GEOFENCE_RESPONSIVENESS = 5000

    private lateinit var appContext: Context
    private lateinit var geofencingClient: GeofencingClient

    fun initialize(applicationContext: Context) {
        if (!::geofencingClient.isInitialized) {
            appContext = applicationContext.applicationContext
            geofencingClient = LocationServices.getGeofencingClient(applicationContext)
        }
    }

    override suspend fun addGeofence(memo: Memo): GeofenceRegisterResult {
        return addGeofences(listOf(memo))
    }

    override suspend fun addGeofences(
        memos: List<Memo>,
        triggerImmediately: Boolean,
    ): GeofenceRegisterResult = when {
        memos.isEmpty() -> GeofenceRegisterResult.Success

        !appContext.hasGeofencingPermissions() -> GeofenceRegisterResult.MissingPermission

        !appContext.isPlayServicesAvailable() -> GeofenceRegisterResult.PlayServicesUnavailable

        else -> suspendCancellableCoroutine { continuation ->
            val geofences = memos.map { memo ->
                Geofence.Builder()
                    .setRequestId(memo.id.toString())
                    .setCircularRegion(
                        memo.reminderLatitude,
                        memo.reminderLongitude,
                        TRIGGER_RADIUS,
                    )
                    .setNotificationResponsiveness(GEOFENCE_RESPONSIVENESS)
                    .setTransitionTypes(
                        // Observed behavior: Registering only for ENTER transitions causes
                        // the geofences to be triggered only once.
                        // Workaround: Register for both ENTER and EXIT transitions,
                        // and ignore EXIT in the receiver.
                        Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT,
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(
                    if (triggerImmediately) GeofencingRequest.INITIAL_TRIGGER_ENTER else 0,
                )
                .addGeofences(geofences)
                .build()

            try {
                geofencingClient.addGeofences(request, createPendingIntent())
                    .also { task ->
                        task.addOnSuccessListener {
                            continuation.resume(GeofenceRegisterResult.Success)
                        }
                        task.addOnFailureListener {
                            continuation.resume(it.toRegisterResult())
                        }
                    }
            } catch (_: SecurityException) {
                continuation.resume(GeofenceRegisterResult.MissingPermission)
            }
        }
    }

    private fun Exception.toRegisterResult(): GeofenceRegisterResult = when {
        this is SecurityException ->
            GeofenceRegisterResult.MissingPermission

        this !is ApiException ->
            GeofenceRegisterResult.Failure(this)

        statusCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
            GeofenceRegisterResult.LocationUnavailable

        statusCode == GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
            GeofenceRegisterResult.LimitReached

        statusCode == GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION ->
            GeofenceRegisterResult.MissingPermission

        else -> GeofenceRegisterResult.Failure(this)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(appContext, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntentCompat.getBroadcast(appContext, 0, intent, flags, true)

        // May return null only if PendingIntent.FLAG_NO_CREATE has been supplied
        return requireNotNull(pendingIntent)
    }

    override fun removeGeofence(memoId: Long) {
        geofencingClient.removeGeofences(listOf(memoId.toString())).addOnFailureListener {
            Log.w(
                GeofenceRepository::class.java.simpleName,
                "Failed to remove geofence with id $memoId",
                it,
            )
        }
    }
}