package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Receiver untuk menyalakan kembali layanan latar belakang POS secara otomatis
 * setelah perangkat dinyalakan ulang (BOOT_COMPLETED) jika fitur diaktifkan.
 */
class PosBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = try {
                context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
            } catch (_: Throwable) {
                return
            }

            // Android 15+ (API 35+) prohibits launching dataSync foreground services directly from BOOT_COMPLETED
            if (Build.VERSION.SDK_INT < 35) {
                try {
                    val enabled = prefs.getBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true)
                    if (enabled) {
                        PosBackgroundService.start(context)
                    }
                } catch (t: Throwable) {
                    Log.w("PosBootReceiver", "Unable to start foreground service on boot: ${t.message}")
                }
            }

            try {
                if (prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)) {
                    AutoStoreCloseManager.scheduleNextAlarm(context)
                }
            } catch (t: Throwable) {
                Log.w("PosBootReceiver", "Unable to reschedule auto close alarm: ${t.message}")
            }

            try {
                if (DriveAutoBackupManager.isAutoBackupEnabled(context)) {
                    DriveAutoBackupManager.schedulePeriodicBackup(context)
                }
            } catch (t: Throwable) {
                Log.w("PosBootReceiver", "Unable to reschedule Drive backup: ${t.message}")
            }
        }
    }
}
