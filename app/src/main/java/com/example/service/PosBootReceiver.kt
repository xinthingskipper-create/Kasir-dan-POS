package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.os.UserManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receiver untuk menjadwalkan ulang pemantauan latar belakang (WorkManager), alarm tutup toko,
 * dan backup Google Drive setelah perangkat menyala ulang atau aplikasi diperbarui.
 * Seluruh inisialisasi dijalankan di Dispatchers.IO via goAsync() agar tidak pernah
 * memblokir Main Thread dan menyebabkan ANR ("Aplikasi tidak menanggapi") saat boot.
 */
class PosBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        if (!UserManagerCompat.isUserUnlocked(context)) {
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(4_000L) {
                    val prefs = try {
                        appContext.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
                    } catch (_: Throwable) {
                        return@withTimeoutOrNull
                    }

                    try {
                        PosNotificationHelper.ensureChannels(appContext)
                    } catch (_: Throwable) {
                    }

                    try {
                        PosBackgroundService.schedulePeriodicWorker(appContext)
                    } catch (t: Throwable) {
                        Log.w("PosBootReceiver", "Unable to schedule periodic worker: ${t.message}")
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        try {
                            val enabled = prefs.getBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true)
                            if (enabled) {
                                PosBackgroundService.start(appContext)
                            }
                        } catch (t: Throwable) {
                            Log.w("PosBootReceiver", "Unable to start foreground service on boot: ${t.message}")
                        }
                    }

                    try {
                        if (prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)) {
                            AutoStoreCloseManager.scheduleNextAlarm(appContext)
                        }
                    } catch (t: Throwable) {
                        Log.w("PosBootReceiver", "Unable to reschedule auto close alarm: ${t.message}")
                    }

                    try {
                        if (DriveAutoBackupManager.isAutoBackupEnabled(appContext)) {
                            DriveAutoBackupManager.schedulePeriodicBackup(appContext)
                        }
                    } catch (t: Throwable) {
                        Log.w("PosBootReceiver", "Unable to reschedule Drive backup: ${t.message}")
                    }
                }
            } catch (t: Throwable) {
                Log.w("PosBootReceiver", "Boot receiver handled error: ${t.message}", t)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
