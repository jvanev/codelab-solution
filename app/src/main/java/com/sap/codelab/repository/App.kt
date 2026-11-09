package com.sap.codelab.repository

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.sap.codelab.R

/**
 * Extension of the Android Application class.
 */
internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MemoRepository.initialize(this)
        GeofenceRepository.initialize(this)

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "geofence_channel"
    }
}