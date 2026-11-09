package com.sap.codelab.repository

import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.model.Memo

/**
 * Provides geofence-related functionality.
 */
internal interface IGeofenceRepository {
    /**
     * Adds a new, or updates an existing geofence with the details of the specified [memo].
     *
     * @return The result of the operation.
     */
    suspend fun addGeofence(memo: Memo): GeofenceRegisterResult

    /**
     * Adds new, or updates existing geofences with the details of the specified [memos].
     *
     * When [triggerImmediately] is `true`, a geofence is triggered immediately
     * if the device is already inside that geofence.
     *
     * @return The result of the operation.
     */
    suspend fun addGeofences(
        memos: List<Memo>,
        triggerImmediately: Boolean = false,
    ): GeofenceRegisterResult

    /**
     * Removes the geofence with the specified [memoId].
     */
    fun removeGeofence(memoId: Long)
}