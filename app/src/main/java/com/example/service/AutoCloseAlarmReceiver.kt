package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receiver AlarmManager untuk mengeksekusi Tutup Toko Otomatis dan pengiriman laporan
 * ke Database Laporan secara terjadwal meski aplikasi sedang ditutup/diminimalkan.
 */
class AutoCloseAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_AUTO_CLOSE_TRIGGERED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AutoStoreCloseManager.executeScheduledStoreClosing(
                    context = appContext,
                    triggeredByAlarm = true
                )
            } catch (t: Throwable) {
                Log.e("AutoCloseAlarmReceiver", "Failed executing scheduled store close: ${t.message}", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_AUTO_CLOSE_TRIGGERED = "com.example.action.AUTO_STORE_CLOSE_TRIGGERED"
    }
}
