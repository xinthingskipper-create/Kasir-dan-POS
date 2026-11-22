package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.UserManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * BroadcastReceiver aman untuk menangani tombol aksi pada notifikasi ("Cek Stok" dan "Hentikan")
 * di Dispatchers.IO via goAsync(), tanpa memblokir Main Thread dan tanpa PendingIntent.getService().
 */
class PosNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!UserManagerCompat.isUserUnlocked(context)) return
        val action = intent?.action ?: return
        val appContext = context.applicationContext

        if (action == PosBackgroundService.ACTION_STOP) {
            try {
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(PosNotificationHelper.NOTIFICATION_ID_FOREGROUND)
            } catch (_: Throwable) {
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(4_000L) {
                    when (action) {
                        PosBackgroundService.ACTION_STOP -> {
                            PosBackgroundService.markStoppedFromNotification(appContext)
                            PosBackgroundService.stop(appContext)
                        }
                        PosBackgroundService.ACTION_CHECK_STOCK -> {
                            PosBackgroundService.checkStockNow(appContext)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w("PosNotifActionReceiver", "Handled notification action error: ${t.message}", t)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
