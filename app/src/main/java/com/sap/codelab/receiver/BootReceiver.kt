package com.sap.codelab.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sap.codelab.work.GeofenceRegisterWorker

/**
 * A broadcast receiver for re-registering currently stored geofences after reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            GeofenceRegisterWorker.schedule(context, triggerImmediately = true)
        }
    }
}