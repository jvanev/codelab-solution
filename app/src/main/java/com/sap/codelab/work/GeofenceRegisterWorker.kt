package com.sap.codelab.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.repository.GeofenceRepository
import com.sap.codelab.repository.MemoRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * A background worker for re-registering geofences when needed.
 */
class GeofenceRegisterWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val triggerImmediately = inputData.getBoolean(KEY_TRIGGER_IMMEDIATELY, false)

        when (val result = registerGeofences(triggerImmediately)) {
            GeofenceRegisterResult.Success -> {
                // The fences are registered, stop the worker
                cancelWork()
                Result.success()
            }

            GeofenceRegisterResult.MissingPermission,
            GeofenceRegisterResult.PlayServicesUnavailable -> {
                Log.e(TAG, "Failed to register geofences; Error: $result")
                cancelWork()
                Result.success()
            }

            GeofenceRegisterResult.LimitReached -> {
                Log.e(TAG, "The geofences limit has been reached, cannot proceed further")
                cancelWork()
                Result.success()
            }

            // The location service is currently off, keep trying
            GeofenceRegisterResult.LocationUnavailable -> Result.success()

            is GeofenceRegisterResult.Failure -> {
                Log.e(TAG, "Failed to register geofences", result.cause)
                Result.success()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "An error occurred while trying to re-register geofences", e)
        Result.success()
    }

    private suspend fun registerGeofences(triggerImmediately: Boolean): GeofenceRegisterResult {
        val memos = MemoRepository.getOpen().first()
        return GeofenceRepository
            .addGeofences(
                memos = memos.takeLast(GeofenceRepository.GEOFENCES_LIMIT),
                triggerImmediately = triggerImmediately,
            )
    }

    private fun cancelWork() {
        WorkManager.getInstance(applicationContext).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "geofence_recovery"
        private const val WORK_REPEAT_INTERVAL = 15L
        private const val KEY_TRIGGER_IMMEDIATELY = "geofence_trigger_immediately"
        private val TAG = GeofenceRegisterWorker::class.java.simpleName

        @JvmStatic
        fun schedule(context: Context, triggerImmediately: Boolean = false) {
            val request = PeriodicWorkRequestBuilder<GeofenceRegisterWorker>(
                WORK_REPEAT_INTERVAL,
                TimeUnit.MINUTES,
            )
                .setInputData(workDataOf(KEY_TRIGGER_IMMEDIATELY to triggerImmediately))
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}