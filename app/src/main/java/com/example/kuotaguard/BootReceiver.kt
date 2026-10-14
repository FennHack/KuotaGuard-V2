package com.example.kuotaguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            Prefs.enabled(context) && Prefs.boot(context)
        ) {
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
        }
    }
}
