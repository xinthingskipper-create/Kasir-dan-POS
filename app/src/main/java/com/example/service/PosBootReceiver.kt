package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver untuk menyalakan kembali layanan latar belakang POS secara otomatis
 * setelah perangkat dinyalakan ulang (BOOT_COMPLETED) jika fitur diaktifkan.
 */
class PosBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true)
                if (enabled) {
                    PosBackgroundService.start(context)
                }
                if (prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)) {
                    AutoStoreCloseManager.scheduleNextAlarm(context)
                }
                if (DriveAutoBackupManager.isAutoBackupEnabled(context)) {
                    DriveAutoBackupManager.schedulePeriodicBackup(context)
                }
            } catch (t: Throwable) {
                Log.w("PosBootReceiver", "Unable to start foreground service on boot: ${t.message}")
            }
        }
    }
}
