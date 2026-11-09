package com.sap.codelab

import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IGeofenceRepository

internal class FakeGeofenceRepository : IGeofenceRepository {

    var result: GeofenceRegisterResult = GeofenceRegisterResult.Success

    var registeredMemos = mutableListOf<Memo>()
    var removedIds = mutableListOf<Long>()

    override suspend fun addGeofence(memo: Memo): GeofenceRegisterResult =
        addGeofences(listOf(memo))

    override suspend fun addGeofences(
        memos: List<Memo>,
        triggerImmediately: Boolean,
    ): GeofenceRegisterResult {
        registeredMemos += memos

        return result
    }

    override fun removeGeofence(memoId: Long) {
        removedIds += memoId
    }
}