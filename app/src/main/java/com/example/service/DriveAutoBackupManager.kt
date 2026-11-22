package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.PosDatabase
import com.example.data.repository.BackupRestoreManager
import com.example.data.repository.BackupStats
import com.example.data.repository.DriveBackupFile
import com.example.data.repository.GoogleDriveBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DriveBackupFrequency(
    val id: String,
    val label: String,
    val description: String,
    val intervalHours: Long?
) {
    DAILY_SCHEDULED(
        id = "DAILY_SCHEDULED",
        label = "Harian Terjadwal",
        description = "Backup otomatis setiap hari pada jam yang ditentukan",
        intervalHours = 24L
    ),
    EVERY_6_HOURS(
        id = "EVERY_6_HOURS",
        label = "Setiap 6 Jam",
        description = "Backup otomatis berkala tiap 6 jam",
        intervalHours = 6L
    ),
    EVERY_12_HOURS(
        id = "EVERY_12_HOURS",
        label = "Setiap 12 Jam",
        description = "Backup otomatis berkala tiap 12 jam",
        intervalHours = 12L
    ),
    ON_SHIFT_CLOSE(
        id = "ON_SHIFT_CLOSE",
        label = "Saat Tutup Kasir",
        description = "Backup otomatis setiap selesai Tutup Shift / Tutup Toko",
        intervalHours = null
    ),
    REALTIME_AND_DAILY(
        id = "REALTIME_AND_DAILY",
        label = "Tiap Transaksi & Harian",
        description = "Backup otomatis setelah transaksi/ubah pengaturan + jadwal harian",
        intervalHours = 24L
    );

    companion object {
        fun fromId(id: String?): DriveBackupFrequency {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DAILY_SCHEDULED
        }
    }
}

data class DriveBackupExecutionResult(
    val success: Boolean,
    val file: DriveBackupFile? = null,
    val stats: BackupStats? = null,
    val message: String = ""
)

object DriveAutoBackupManager {

    private const val TAG = "DriveAutoBackupManager"
    private const val PERIODIC_WORK_NAME = "kasir_pos_drive_periodic_backup"
    private const val DEBOUNCED_WORK_NAME = "kasir_pos_drive_debounced_backup"

    const val KEY_DRIVE_CONNECTED = "KEY_DRIVE_CONNECTED"
    const val KEY_DRIVE_ACCOUNT_EMAIL = "KEY_DRIVE_ACCOUNT_EMAIL"
    const val KEY_DRIVE_AUTO_BACKUP_ENABLED = "KEY_DRIVE_AUTO_BACKUP_ENABLED"
    const val KEY_DRIVE_AUTO_BACKUP_FREQUENCY = "KEY_DRIVE_AUTO_BACKUP_FREQUENCY"
    const val KEY_DRIVE_AUTO_BACKUP_HOUR = "KEY_DRIVE_AUTO_BACKUP_HOUR"
    const val KEY_DRIVE_AUTO_BACKUP_MINUTE = "KEY_DRIVE_AUTO_BACKUP_MINUTE"
    const val KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY = "KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY"
    const val KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE = "KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE"
    const val KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT = "KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT"
    const val KEY_LAST_DRIVE_BACKUP_TIMESTAMP = "KEY_LAST_DRIVE_BACKUP_TIMESTAMP"
    const val KEY_LAST_DRIVE_BACKUP_STATUS = "KEY_LAST_DRIVE_BACKUP_STATUS"
    const val KEY_LAST_DRIVE_BACKUP_FILE = "KEY_LAST_DRIVE_BACKUP_FILE"
    const val KEY_LAST_DRIVE_BACKUP_DATE = "KEY_LAST_DRIVE_BACKUP_DATE"

    const val DEFAULT_HOUR = 22
    const val DEFAULT_MINUTE = 30
    const val DEFAULT_KEEP_COUNT = 10

    private val backupMutex = Mutex()

    private val _lastBackupEventMillis = MutableStateFlow(0L)
    val lastBackupEventMillis: StateFlow<Long> = _lastBackupEventMillis.asStateFlow()

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutoBackupEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DRIVE_AUTO_BACKUP_ENABLED, false)

    fun isDriveConnected(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DRIVE_CONNECTED, false)

    fun getFrequency(context: Context): DriveBackupFrequency =
        DriveBackupFrequency.fromId(
            getPrefs(context).getString(KEY_DRIVE_AUTO_BACKUP_FREQUENCY, DriveBackupFrequency.DAILY_SCHEDULED.id)
        )

    fun isWifiOnly(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false)

    fun isBackupOnShiftCloseEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, true)

    fun getKeepCount(context: Context): Int =
        getPrefs(context).getInt(KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, DEFAULT_KEEP_COUNT).coerceIn(0, 100)

    fun schedulePeriodicBackup(context: Context) {
        val prefs = getPrefs(context)
        val enabled = prefs.getBoolean(KEY_DRIVE_AUTO_BACKUP_ENABLED, false)
        val frequency = DriveBackupFrequency.fromId(
            prefs.getString(KEY_DRIVE_AUTO_BACKUP_FREQUENCY, DriveBackupFrequency.DAILY_SCHEDULED.id)
        )
        val wifiOnly = prefs.getBoolean(KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false)

        val workManager = try {
            WorkManager.getInstance(context.applicationContext)
        } catch (t: Throwable) {
            Log.w(TAG, "WorkManager unavailable: ${t.message}")
            return
        }

        try {
            if (!enabled || frequency.intervalHours == null) {
                workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
                return
            }

            val intervalHours = frequency.intervalHours
            val initialDelayMinutes = if (
                frequency == DriveBackupFrequency.DAILY_SCHEDULED ||
                frequency == DriveBackupFrequency.REALTIME_AND_DAILY
            ) {
                val hour = prefs.getInt(KEY_DRIVE_AUTO_BACKUP_HOUR, DEFAULT_HOUR).coerceIn(0, 23)
                val minute = prefs.getInt(KEY_DRIVE_AUTO_BACKUP_MINUTE, DEFAULT_MINUTE).coerceIn(0, 59)
                computeInitialDelayMinutesForTime(hour, minute)
            } else {
                15L
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DriveAutoBackupWorker>(intervalHours, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .setInputData(workDataOf(DriveAutoBackupWorker.KEY_REASON to "Jadwal Otomatis (${frequency.label})"))
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "Scheduled Google Drive periodic backup: ${frequency.label}, initialDelay=${initialDelayMinutes}m, wifiOnly=$wifiOnly")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed scheduling periodic backup safely: ${t.message}")
        }
    }

    fun triggerDebouncedAutoBackup(
        context: Context,
        reason: String,
        delaySeconds: Long = 25L
    ) {
        val prefs = getPrefs(context)
        val enabled = prefs.getBoolean(KEY_DRIVE_AUTO_BACKUP_ENABLED, false)
        val connected = prefs.getBoolean(KEY_DRIVE_CONNECTED, false)
        if (!enabled || !connected) return

        val frequency = getFrequency(context)
        if (frequency != DriveBackupFrequency.REALTIME_AND_DAILY) return

        val wifiOnly = prefs.getBoolean(KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false)
        val workManager = try {
            WorkManager.getInstance(context.applicationContext)
        } catch (_: Throwable) {
            return
        }

        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val req = OneTimeWorkRequestBuilder<DriveAutoBackupWorker>()
                .setInitialDelay(delaySeconds.coerceAtLeast(5L), TimeUnit.SECONDS)
                .setConstraints(constraints)
                .setInputData(workDataOf(DriveAutoBackupWorker.KEY_REASON to reason))
                .build()

            workManager.enqueueUniqueWork(
                DEBOUNCED_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Failed scheduling debounced backup safely: ${t.message}")
        }
    }

    fun computeInitialDelayMinutesForTime(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return ((target.timeInMillis - nowMillis) / 60_000L).coerceAtLeast(1L)
    }

    fun shouldRunScheduledBackupFromService(
        enabled: Boolean,
        connected: Boolean,
        frequency: DriveBackupFrequency,
        hour: Int,
        minute: Int,
        lastBackupDate: String,
        lastBackupTimestamp: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!enabled || !connected) return false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        return when (frequency) {
            DriveBackupFrequency.DAILY_SCHEDULED, DriveBackupFrequency.REALTIME_AND_DAILY -> {
                if (lastBackupDate == todayStr) return false
                val scheduledToday = AutoStoreCloseManager.computeScheduledMillisForDay(nowMillis, hour, minute)
                nowMillis >= scheduledToday
            }
            DriveBackupFrequency.EVERY_6_HOURS -> {
                nowMillis - lastBackupTimestamp >= TimeUnit.HOURS.toMillis(6)
            }
            DriveBackupFrequency.EVERY_12_HOURS -> {
                nowMillis - lastBackupTimestamp >= TimeUnit.HOURS.toMillis(12)
            }
            DriveBackupFrequency.ON_SHIFT_CLOSE -> false
        }
    }

    suspend fun performBackupToDrive(
        context: Context,
        reason: String = "Manual",
        isManual: Boolean = false
    ): DriveBackupExecutionResult = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            val appContext = context.applicationContext
            val prefs = getPrefs(appContext)
            val connected = prefs.getBoolean(KEY_DRIVE_CONNECTED, false)
            if (!connected) {
                return@withLock DriveBackupExecutionResult(
                    success = false,
                    message = "Akun Google Drive belum dihubungkan"
                )
            }

            if (!GoogleDriveBackupManager.isNetworkAvailable(appContext)) {
                val msg = "Menunggu koneksi internet untuk backup Google Drive"
                if (isManual) recordStatus(prefs, msg)
                return@withLock DriveBackupExecutionResult(success = false, message = msg)
            }

            val wifiOnly = prefs.getBoolean(KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false)
            if (!isManual && wifiOnly && !GoogleDriveBackupManager.isWifiConnected(appContext)) {
                return@withLock DriveBackupExecutionResult(
                    success = false,
                    message = "Menunggu jaringan Wi-Fi sesuai pengaturan auto backup"
                )
            }

            val driveManager = GoogleDriveBackupManager(appContext)
            val tokenResult = driveManager.getSilentAccessToken()
            val token = tokenResult.getOrElse { err ->
                val msg = err.message ?: "Sesi Google Drive perlu dihubungkan ulang"
                recordStatus(prefs, "⚠️ $msg")
                return@withLock DriveBackupExecutionResult(success = false, message = msg)
            }

            return@withLock performUploadWithToken(
                context = appContext,
                accessToken = token,
                reason = reason
            )
        }
    }

    suspend fun performUploadWithToken(
        context: Context,
        accessToken: String,
        reason: String = "Manual"
    ): DriveBackupExecutionResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = getPrefs(appContext)
        val database = PosDatabase.getDatabase(appContext)
        val backupManager = BackupRestoreManager(appContext, database)
        val driveManager = GoogleDriveBackupManager(appContext)

        try {
            val stats = backupManager.exportBackup()
            val keepCount = prefs.getInt(KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, DEFAULT_KEEP_COUNT).coerceIn(0, 100)
            val uploadResult = driveManager.uploadBackupJson(
                accessToken = accessToken,
                fileName = stats.fileName,
                jsonContent = stats.jsonString,
                keepCount = keepCount
            )
            val uploaded = uploadResult.getOrElse { err ->
                val msg = "Gagal upload Drive: ${err.message}"
                recordStatus(prefs, "❌ $msg")
                return@withContext DriveBackupExecutionResult(success = false, stats = stats, message = msg)
            }

            val now = System.currentTimeMillis()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val timeFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(now))
            val statusMsg = "✅ Tersimpan ke Drive ($timeFormatted • $reason): ${stats.transactionCount} transaksi, ${stats.productCount} produk, & seluruh pengaturan"

            prefs.edit()
                .putLong(KEY_LAST_DRIVE_BACKUP_TIMESTAMP, now)
                .putString(KEY_LAST_DRIVE_BACKUP_STATUS, statusMsg)
                .putString(KEY_LAST_DRIVE_BACKUP_FILE, uploaded.name)
                .putString(KEY_LAST_DRIVE_BACKUP_DATE, todayStr)
                .apply()

            try {
                database.cloudHostingConfigDao().updateLastBackupTimestamp(now)
            } catch (_: Throwable) {
            }

            _lastBackupEventMillis.value = now
            DriveBackupExecutionResult(
                success = true,
                file = uploaded,
                stats = stats,
                message = statusMsg
            )
        } catch (t: Throwable) {
            val msg = "Gagal backup ke Drive: ${t.message}"
            recordStatus(prefs, "❌ $msg")
            DriveBackupExecutionResult(success = false, message = msg)
        }
    }

    private fun recordStatus(prefs: SharedPreferences, status: String) {
        prefs.edit().putString(KEY_LAST_DRIVE_BACKUP_STATUS, status).apply()
        _lastBackupEventMillis.value = System.currentTimeMillis()
    }
}

class DriveAutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reason = inputData.getString(KEY_REASON) ?: "Auto Backup Terjadwal"
        val res = DriveAutoBackupManager.performBackupToDrive(
            context = applicationContext,
            reason = reason,
            isManual = false
        )
        return if (res.success) {
            Result.success()
        } else {
            val prefs = DriveAutoBackupManager.getPrefs(applicationContext)
            val enabled = prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false)
            val connected = prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_CONNECTED, false)
            if (enabled && connected) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_REASON = "KEY_BACKUP_REASON"
    }
}
