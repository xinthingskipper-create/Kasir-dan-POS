package com.example.service

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
 * Receiver AlarmManager untuk menjadwalkan eksekusi Tutup Toko Otomatis melalui WorkManager
 * secara cepat di background thread (Dispatchers.IO) sehingga Main Thread tidak pernah
 * terblokir atau terkena ANR ("Aplikasi tidak menanggapi").
 */
class AutoCloseAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_AUTO_CLOSE_TRIGGERED) return
        if (!UserManagerCompat.isUserUnlocked(context)) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val enqueued = withTimeoutOrNull(3_000L) {
                    AutoStoreCloseManager.enqueueImmediateWorker(appContext)
                } == true

                if (!enqueued) {
                    withTimeoutOrNull(5_000L) {
                        AutoStoreCloseManager.executeScheduledStoreClosing(
                            context = appContext,
                            triggeredByAlarm = true
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.e("AutoCloseAlarmReceiver", "Failed scheduling auto store close: ${t.message}", t)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Throwable) {
                }
            }
        }
    }

    companion object {
        const val ACTION_AUTO_CLOSE_TRIGGERED = "com.example.action.AUTO_STORE_CLOSE_TRIGGERED"
    }
}
