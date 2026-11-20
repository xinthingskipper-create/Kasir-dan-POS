package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.PosDatabase
import com.example.data.model.PaymentMethod
import com.example.data.model.ShiftClosing
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AutoCloseResult(
    val executed: Boolean,
    val totalTransactions: Int = 0,
    val totalRevenue: Double = 0.0,
    val cloudSynced: Boolean = false,
    val message: String = ""
)

/**
 * Mengelola jadwal Tutup Toko Otomatis berbasis jam & menit yang dikonfigurasi di Pengaturan Toko.
 * Ketika waktu terjadwal tiba (baik melalui AlarmManager maupun pemantauan PosBackgroundService),
 * sistem otomatis:
 * 1. Menghitung total omset, tunai, non-tunai, & pengeluaran hari ini.
 * 2. Menyimpan ShiftClosing & arsip DailyReport ke Room Database.
 * 3. Mengirim seluruh transaksi, pengeluaran, & arsip laporan harian ke Database Laporan Online.
 * 4. Menampilkan push notifikasi hasil Tutup Toko Otomatis.
 */
object AutoStoreCloseManager {

    private const val TAG = "AutoStoreCloseManager"

    const val KEY_AUTO_CLOSE_ENABLED = "KEY_AUTO_CLOSE_ENABLED"
    const val KEY_AUTO_CLOSE_HOUR = "KEY_AUTO_CLOSE_HOUR"
    const val KEY_AUTO_CLOSE_MINUTE = "KEY_AUTO_CLOSE_MINUTE"
    const val KEY_AUTO_CLOSE_CONFIGURED_AT = "KEY_AUTO_CLOSE_CONFIGURED_AT"
    const val KEY_LAST_AUTO_CLOSE_DATE = "KEY_LAST_AUTO_CLOSE_DATE"
    const val KEY_LAST_AUTO_CLOSE_STATUS = "KEY_LAST_AUTO_CLOSE_STATUS"
    const val KEY_LAST_AUTO_CLOSE_TIMESTAMP = "KEY_LAST_AUTO_CLOSE_TIMESTAMP"

    const val DEFAULT_HOUR = 22
    const val DEFAULT_MINUTE = 0
    private const val ALARM_REQUEST_CODE = 4401

    private val executeMutex = Mutex()

    private val _lastAutoCloseEventMillis = MutableStateFlow(0L)
    val lastAutoCloseEventMillis: StateFlow<Long> = _lastAutoCloseEventMillis.asStateFlow()

    fun formatTime(hour: Int, minute: Int): String {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun computeScheduledMillisForDay(referenceMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun computeNextAlarmTriggerMillis(nowMillis: Long, hour: Int, minute: Int): Long {
        val todayScheduled = computeScheduledMillisForDay(nowMillis, hour, minute)
        return if (todayScheduled > nowMillis) {
            todayScheduled
        } else {
            val tomorrowCal = Calendar.getInstance().apply {
                timeInMillis = todayScheduled
                add(Calendar.DAY_OF_YEAR, 1)
            }
            tomorrowCal.timeInMillis
        }
    }

    fun formatNextScheduleDescription(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        lastAutoCloseDate: String,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (!enabled) return "Tutup toko otomatis nonaktif"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(nowMillis))
        val todayScheduled = computeScheduledMillisForDay(nowMillis, hour, minute)
        val timeText = formatTime(hour, minute)
        return if (lastAutoCloseDate != todayStr && todayScheduled > nowMillis) {
            "Hari Ini pukul $timeText"
        } else {
            "Besok pukul $timeText"
        }
    }

    /**
     * Memeriksa apakah tutup toko otomatis harus dijalankan pada waktu [nowMillis].
     * Mencegah eksekusi ulang di hari yang sama ([lastAutoCloseDate] == hari ini),
     * serta mencegah langsung menutup toko jika jadwal diaktifkan setelah jam tersebut lewat hari ini.
     */
    fun shouldTriggerAutoCloseNow(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        configuredAtMillis: Long,
        lastAutoCloseDate: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!enabled) return false
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(nowMillis))
        if (lastAutoCloseDate == todayStr) return false

        val todayScheduled = computeScheduledMillisForDay(nowMillis, hour, minute)
        if (nowMillis < todayScheduled) return false

        // Jika jadwal baru diubah/diaktifkan hari ini setelah jam tutup toko terlewat, tunggu besok.
        if (configuredAtMillis >= todayScheduled) return false

        return true
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoCloseAlarmReceiver::class.java).apply {
            action = AutoCloseAlarmReceiver.ACTION_AUTO_CLOSE_TRIGGERED
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleNextAlarm(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_AUTO_CLOSE_ENABLED, false)
        if (!enabled) {
            cancelAlarm(context)
            return
        }

        val hour = prefs.getInt(KEY_AUTO_CLOSE_HOUR, DEFAULT_HOUR).coerceIn(0, 23)
        val minute = prefs.getInt(KEY_AUTO_CLOSE_MINUTE, DEFAULT_MINUTE).coerceIn(0, 59)
        val triggerAt = computeNextAlarmTriggerMillis(nowMillis, hour, minute)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context)

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.d(TAG, "Scheduled auto store close at ${formatTime(hour, minute)} (triggerAt=$triggerAt)")
        } catch (t: Throwable) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (t2: Throwable) {
                Log.w(TAG, "Unable to schedule auto store close alarm: ${t2.message}")
            }
        }
    }

    fun cancelAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            alarmManager.cancel(buildAlarmPendingIntent(context))
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to cancel auto store close alarm: ${t.message}")
        }
    }

    /**
     * Menjalankan Tutup Toko Otomatis jika waktunya sudah terpenuhi (atau dipicu langsung oleh alarm/manual).
     */
    suspend fun executeScheduledStoreClosing(
        context: Context,
        triggeredByAlarm: Boolean = false
    ): AutoCloseResult = withContext(Dispatchers.IO) {
        executeMutex.withLock {
            val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_AUTO_CLOSE_ENABLED, false)
            if (!enabled) {
                return@withLock AutoCloseResult(executed = false, message = "Tutup toko otomatis nonaktif")
            }

            val hour = prefs.getInt(KEY_AUTO_CLOSE_HOUR, DEFAULT_HOUR).coerceIn(0, 23)
            val minute = prefs.getInt(KEY_AUTO_CLOSE_MINUTE, DEFAULT_MINUTE).coerceIn(0, 59)
            val configuredAt = prefs.getLong(KEY_AUTO_CLOSE_CONFIGURED_AT, 0L)
            val lastClosedDate = prefs.getString(KEY_LAST_AUTO_CLOSE_DATE, "") ?: ""
            val nowMillis = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date(nowMillis))

            if (lastClosedDate == todayStr) {
                if (triggeredByAlarm) {
                    scheduleNextAlarm(context, nowMillis + 60_000L)
                }
                return@withLock AutoCloseResult(executed = false, message = "Sudah tutup otomatis hari ini")
            }

            if (!triggeredByAlarm) {
                val shouldRun = shouldTriggerAutoCloseNow(
                    enabled = enabled,
                    hour = hour,
                    minute = minute,
                    configuredAtMillis = configuredAt,
                    lastAutoCloseDate = lastClosedDate,
                    nowMillis = nowMillis
                )
                if (!shouldRun) {
                    return@withLock AutoCloseResult(executed = false, message = "Belum memasuki jam tutup toko")
                }
            }

            val database = PosDatabase.getDatabase(context.applicationContext)
            val repository = PosRepository(database)
            val firebaseSyncRepository = FirebaseSyncRepository(context.applicationContext)

            val startCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val startTime = startCal.timeInMillis
            val endTime = endCal.timeInMillis

            val todayTransactions = try {
                repository.getTransactionsBetween(startTime, endTime).first()
            } catch (t: Throwable) {
                emptyList()
            }

            val totalCount = todayTransactions.size
            val totalCash = todayTransactions.filter { it.paymentMethod == PaymentMethod.TUNAI }.sumOf { it.grandTotal }
            val totalNonCash = todayTransactions.filter { it.paymentMethod != PaymentMethod.TUNAI }.sumOf { it.grandTotal }
            val totalRevenue = todayTransactions.sumOf { it.grandTotal }

            val scheduleTimeText = formatTime(hour, minute)
            val autoCashierLabel = "Tutup Toko Otomatis ($scheduleTimeText)"
            val closing = ShiftClosing(
                cashierId = 0L,
                cashierName = autoCashierLabel,
                totalTransactions = totalCount,
                totalCash = totalCash,
                totalNonCash = totalNonCash,
                totalRevenue = totalRevenue,
                expectedCash = totalCash,
                actualCash = totalCash,
                difference = 0.0,
                notes = "Tutup Toko Otomatis Terjadwal pukul $scheduleTimeText"
            )

            val insertedId = repository.insertShiftClosing(closing)
            val closingWithId = closing.copy(id = insertedId)

            // Tandai selesai hari ini segera setelah tersimpan di Room agar tidak terulang
            prefs.edit()
                .putString(KEY_LAST_AUTO_CLOSE_DATE, todayStr)
                .putLong(KEY_LAST_AUTO_CLOSE_TIMESTAMP, nowMillis)
                .apply()

            try {
                repository.generateOrUpdateDailyReportForDate(todayStr, startTime, endTime)
            } catch (t: Throwable) {
                Log.w(TAG, "Auto close Room daily report note: ${t.message}")
            }

            val dbId = prefs.getString("KEY_FIREBASE_DATABASE_ID", "foodrock-report-default-rtdb")
                ?.ifBlank { "foodrock-report-default-rtdb" }
                ?: "foodrock-report-default-rtdb"
            val projId = prefs.getString("KEY_FIREBASE_PROJECT_ID", "foodrock-report")
                ?.ifBlank { "foodrock-report" }
                ?: "foodrock-report"
            LocalWebReportServer.start(context, database)
            val rawHostUrl = prefs.getString("KEY_WEB_HOSTING_URL", "")
            val hostUrl = if (rawHostUrl.isNullOrBlank() || FirebaseSyncRepository.isAiStudioProxyUrl(rawHostUrl)) {
                LocalWebReportServer.getLocalBaseUrl()
            } else {
                rawHostUrl.trim()
            }

            var syncedTrxCount = 0
            var shiftSynced = false
            var dailyReportSynced = false
            var storeName = "Kasir POS"

            try {
                val unsyncedList = try {
                    repository.getUnsyncedTransactionsList()
                } catch (_: Throwable) {
                    emptyList()
                }
                val allTrxToSync = (todayTransactions + unsyncedList).distinctBy { it.id }
                for (trx in allTrxToSync) {
                    try {
                        val items = repository.getItemsForTransaction(trx.id)
                        val ok = firebaseSyncRepository.syncTransaction(
                            trx,
                            items,
                            databaseId = dbId,
                            projectId = projId,
                            hostingUrl = hostUrl
                        )
                        if (ok) {
                            syncedTrxCount++
                            repository.updateSyncStatus(trx.id, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                        }
                    } catch (_: Throwable) {
                    }
                }

                val todayExpenses = try {
                    repository.getExpensesBetweenList(startTime, endTime)
                } catch (_: Throwable) {
                    emptyList()
                }
                val totalExpenses = todayExpenses.sumOf { it.amount }
                val expenseNotesSummary = todayExpenses.joinToString("; ") { exp ->
                    val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                    "${exp.title} [${exp.category}]: Rp ${String.format(Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
                }

                for (exp in todayExpenses) {
                    try {
                        firebaseSyncRepository.syncExpense(exp, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                    } catch (_: Throwable) {
                    }
                }

                shiftSynced = firebaseSyncRepository.syncShiftClosing(
                    closing = closingWithId,
                    databaseId = dbId,
                    totalExpenses = totalExpenses,
                    expenseNotes = expenseNotesSummary,
                    projectId = projId,
                    hostingUrl = hostUrl,
                    expensesList = todayExpenses
                )

                val profile = try {
                    repository.getStoreProfileSync()
                } catch (_: Throwable) {
                    null
                }
                if (profile != null) {
                    storeName = profile.storeName.ifBlank { "Kasir POS" }
                    try {
                        firebaseSyncRepository.syncStoreProfile(profile, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                    } catch (_: Throwable) {
                    }
                }

                val dailyRep = repository.getDailyReportByDateDirect(todayStr)
                if (dailyRep != null) {
                    dailyReportSynced = firebaseSyncRepository.syncDailyReport(
                        dailyReport = dailyRep,
                        databaseId = dbId,
                        totalExpenses = totalExpenses,
                        expenseNotes = expenseNotesSummary,
                        projectId = projId,
                        hostingUrl = hostUrl,
                        expensesList = todayExpenses
                    )
                    if (dailyReportSynced) {
                        repository.markDailyReportAsSynced(todayStr)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Auto close cloud sync warning: ${t.message}")
            }

            val overallSynced = dailyReportSynced || shiftSynced
            val statusMessage = if (overallSynced) {
                "⏰ Tutup toko otomatis ($todayStr $scheduleTimeText): $totalCount transaksi & laporan terkirim ke Database Online."
            } else {
                "⏰ Tutup toko otomatis ($todayStr $scheduleTimeText): $totalCount transaksi tersimpan lokal di Room DB."
            }

            prefs.edit()
                .putString(KEY_LAST_AUTO_CLOSE_STATUS, statusMessage)
                .apply()

            _lastAutoCloseEventMillis.value = nowMillis

            // Jadwalkan otomatis untuk hari berikutnya
            scheduleNextAlarm(context, nowMillis + 60_000L)

            // Kirim Push Notifikasi Tutup Toko Otomatis
            PosNotificationHelper.notifyStoreClosed(
                context = context.applicationContext,
                storeName = storeName,
                totalRevenue = totalRevenue,
                totalTransactions = totalCount,
                totalCash = totalCash,
                totalNonCash = totalNonCash,
                cashierName = autoCashierLabel,
                isAutomatic = true,
                cloudSynced = overallSynced,
                onlyAlertOnce = false
            )

            AutoCloseResult(
                executed = true,
                totalTransactions = totalCount,
                totalRevenue = totalRevenue,
                cloudSynced = overallSynced,
                message = statusMessage
            )
        }
    }
}
