package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.local.PosDatabase
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.PosRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service latar belakang untuk menjaga aplikasi Kasir POS tetap berjalan:
 * 1. Memantau stok makanan, minuman, dan menu secara real-time lalu mengirimkan
 *    push notifikasi otomatis saat stok menipis atau habis.
 * 2. Menjaga sinkronisasi transaksi tertunda ke Cloud Firebase Realtime Database
 *    tetap aktif meski aplikasi diminimalkan.
 */
class PosBackgroundService : Service() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Background service coroutine error: ${throwable.message}", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    private var stockMonitorJob: Job? = null
    private var periodicSyncJob: Job? = null

    private lateinit var database: PosDatabase
    private lateinit var repository: PosRepository
    private lateinit var firebaseSyncRepository: FirebaseSyncRepository

    @Volatile
    private var cachedStoreName: String = "Kasir POS"

    override fun onCreate() {
        super.onCreate()
        database = PosDatabase.getDatabase(applicationContext)
        repository = PosRepository(database)
        firebaseSyncRepository = FirebaseSyncRepository(applicationContext)
        PosNotificationHelper.ensureChannels(this)
        serviceScope.launch {
            LocalWebReportServer.start(applicationContext, database)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_BG_SERVICE_ENABLED, false).apply()
                    _userStoppedFromNotif.value = System.currentTimeMillis()
                }
                stopMonitoringAndSelf()
                return START_NOT_STICKY
            }
            ACTION_CHECK_STOCK -> {
                if (!promoteToForeground()) {
                    stopMonitoringAndSelf()
                    return START_NOT_STICKY
                }
                startLoopsIfNeeded()
                serviceScope.launch {
                    checkLowStockOnce(forceAlert = true)
                }
                return START_STICKY
            }
            else -> {
                if (!promoteToForeground()) {
                    stopMonitoringAndSelf()
                    return START_NOT_STICKY
                }
                startLoopsIfNeeded()
                return START_STICKY
            }
        }
    }

    private fun promoteToForeground(): Boolean {
        return try {
            val notification = PosNotificationHelper.buildForegroundServiceNotification(
                context = this,
                storeName = cachedStoreName,
                lowStockCount = 0,
                outOfStockCount = 0
            )
            val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                PosNotificationHelper.NOTIFICATION_ID_FOREGROUND,
                notification,
                fgType
            )
            _isRunning.value = true
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to promote service to foreground: ${t.message}", t)
            _isRunning.value = false
            false
        }
    }

    private fun startLoopsIfNeeded() {
        if (stockMonitorJob?.isActive != true) {
            stockMonitorJob = serviceScope.launch {
                // Observe store name continuously
                launch {
                    try {
                        repository.getStoreProfile().collect { profile ->
                            if (profile != null && profile.storeName.isNotBlank()) {
                                cachedStoreName = profile.storeName
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "Store profile collect warning: ${t.message}")
                    }
                }

                // Observe products continuously for instant low-stock push notifications
                try {
                    database.productDao().getAllProducts().collect { products ->
                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val notifEnabled = prefs.getBoolean(KEY_LOW_STOCK_NOTIF_ENABLED, true)

                        val lowCount = products.count { it.isAvailable && it.isLowStock }
                        val outCount = products.count { it.isAvailable && it.isOutOfStock }

                        updateOngoingNotification(lowCount, outCount)

                        if (notifEnabled) {
                            PosNotificationHelper.notifyLowStockIfNeeded(
                                context = applicationContext,
                                storeName = cachedStoreName,
                                allProducts = products,
                                forceAlert = false
                            )
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Product stock monitor error: ${t.message}", t)
                }
            }
        }

        if (periodicSyncJob?.isActive != true) {
            periodicSyncJob = serviceScope.launch {
                while (true) {
                    try {
                        delay(15_000L)
                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val dbId = prefs.getString("KEY_FIREBASE_DATABASE_ID", "foodrock-report-default-rtdb")
                            ?: "foodrock-report-default-rtdb"
                        val projId = prefs.getString("KEY_FIREBASE_PROJECT_ID", "foodrock-report")
                            ?: "foodrock-report"
                        val rawHostUrl = prefs.getString("KEY_WEB_HOSTING_URL", "")
                        val hostUrl = if (rawHostUrl.isNullOrBlank() || FirebaseSyncRepository.isAiStudioProxyUrl(rawHostUrl)) {
                            LocalWebReportServer.getLocalBaseUrl()
                        } else {
                            rawHostUrl.trim()
                        }

                        // 1. Sync pending transactions in background
                        val pending = repository.getUnsyncedTransactionsList()
                        for (trx in pending) {
                            val items = repository.getItemsForTransaction(trx.id)
                            val ok = firebaseSyncRepository.syncTransaction(
                                trx,
                                items,
                                databaseId = dbId,
                                projectId = projId,
                                hostingUrl = hostUrl
                            )
                            if (ok) {
                                repository.updateSyncStatus(trx.id, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                            }
                        }

                        // 1B. Retry pending OR not-yet-remote-synced expenses in background
                        val pendingExpRaw = prefs.getString("KEY_PENDING_EXPENSE_IDS", "") ?: ""
                        val pendingExpIds = pendingExpRaw.split(",").mapNotNull { it.trim().toLongOrNull() }.toMutableSet()
                        val syncedExpRaw = prefs.getString("KEY_SYNCED_EXPENSE_IDS", "") ?: ""
                        val syncedExpIds = syncedExpRaw.split(",").mapNotNull { it.trim().toLongOrNull() }.toMutableSet()
                        val remoteExpRaw = prefs.getString("KEY_SYNCED_REMOTE_EXPENSE_IDS", "") ?: ""
                        val remoteExpIds = remoteExpRaw.split(",").mapNotNull { it.trim().toLongOrNull() }.toMutableSet()

                        val allExps = repository.getExpensesBetweenList(0L, System.currentTimeMillis() + 86_400_000L)
                        val expTargets = allExps.filter { it.id in pendingExpIds || it.id !in syncedExpIds || it.id !in remoteExpIds }
                        if (expTargets.isNotEmpty()) {
                            var prefsChanged = false
                            for (exp in expTargets) {
                                val res = firebaseSyncRepository.syncExpenseDetailed(
                                    exp,
                                    databaseId = dbId,
                                    projectId = projId,
                                    hostingUrl = hostUrl
                                )
                                if (res.anyOk) {
                                    if (pendingExpIds.remove(exp.id)) prefsChanged = true
                                    if (syncedExpIds.add(exp.id)) prefsChanged = true
                                    if (res.remoteOk && remoteExpIds.add(exp.id)) prefsChanged = true
                                }
                            }
                            if (prefsChanged) {
                                prefs.edit()
                                    .putString("KEY_PENDING_EXPENSE_IDS", pendingExpIds.joinToString(","))
                                    .putString("KEY_SYNCED_EXPENSE_IDS", syncedExpIds.joinToString(","))
                                    .putString("KEY_SYNCED_REMOTE_EXPENSE_IDS", remoteExpIds.joinToString(","))
                                    .apply()
                            }
                        }

                        // 2. Jalankan Tutup Toko Otomatis Terjadwal jika jam tutup telah tiba
                        AutoStoreCloseManager.executeScheduledStoreClosing(
                            context = applicationContext,
                            triggeredByAlarm = false
                        )

                        // 3. Check date rollover in background so midnight reset happens automatically
                        checkBackgroundDateRollover(prefs, dbId)

                        // 4. Jalankan Auto Backup Google Drive terjadwal jika waktunya tiba
                        checkScheduledDriveBackup(prefs)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Periodic background sync error: ${t.message}")
                    }
                }
            }
        }
    }

    private suspend fun checkScheduledDriveBackup(prefs: android.content.SharedPreferences) {
        try {
            val enabled = prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false)
            val connected = prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_CONNECTED, false)
            if (!enabled || !connected) return

            val freq = DriveBackupFrequency.fromId(
                prefs.getString(
                    DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY,
                    DriveBackupFrequency.DAILY_SCHEDULED.id
                )
            )
            val hour = prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, DriveAutoBackupManager.DEFAULT_HOUR)
            val minute = prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, DriveAutoBackupManager.DEFAULT_MINUTE)
            val lastDate = prefs.getString(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_DATE, "") ?: ""
            val lastTs = prefs.getLong(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_TIMESTAMP, 0L)

            if (DriveAutoBackupManager.shouldRunScheduledBackupFromService(
                    enabled = enabled,
                    connected = connected,
                    frequency = freq,
                    hour = hour,
                    minute = minute,
                    lastBackupDate = lastDate,
                    lastBackupTimestamp = lastTs
                )
            ) {
                DriveAutoBackupManager.performBackupToDrive(
                    context = applicationContext,
                    reason = "Auto Backup (${freq.label})",
                    isManual = false
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Scheduled Drive backup check warning: ${t.message}")
        }
    }

    private suspend fun checkBackgroundDateRollover(prefs: android.content.SharedPreferences, dbId: String) {
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val currentDateStr = dateFormat.format(java.util.Date())
            val lastDateStr = prefs.getString("KEY_LAST_ACTIVE_DATE", "") ?: ""

            if (lastDateStr.isNotBlank() && lastDateStr != currentDateStr) {
                val isoStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())
                val resetStats = org.json.JSONObject(mapOf(
                    "todayRevenue" to 0,
                    "todayTransactionsCount" to 0,
                    "lastTransactionAmount" to 0,
                    "lastInvoiceNumber" to "-",
                    "lastItemsSummary" to "Belum ada transaksi hari ini",
                    "lastUpdated" to isoStr,
                    "date" to currentDateStr
                )).toString()
                firebaseSyncRepository.syncToRealtimeDatabase(dbId, "realtime_stats/today", resetStats)
            }
            if (lastDateStr != currentDateStr) {
                prefs.edit().putString("KEY_LAST_ACTIVE_DATE", currentDateStr).apply()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Background date rollover check warning: ${t.message}")
        }
    }

    private suspend fun checkLowStockOnce(forceAlert: Boolean) {
        try {
            val products = database.productDao().getAllProductsList()
            val lowCount = products.count { it.isAvailable && it.isLowStock }
            val outCount = products.count { it.isAvailable && it.isOutOfStock }
            updateOngoingNotification(lowCount, outCount)

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val notifEnabled = prefs.getBoolean(KEY_LOW_STOCK_NOTIF_ENABLED, true)
            if (notifEnabled || forceAlert) {
                PosNotificationHelper.notifyLowStockIfNeeded(
                    context = applicationContext,
                    storeName = cachedStoreName,
                    allProducts = products,
                    forceAlert = forceAlert
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Manual low stock check failed: ${t.message}")
        }
    }

    private fun updateOngoingNotification(lowCount: Int, outCount: Int) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoCloseEnabled = prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)
            val syncSummary = if (autoCloseEnabled) {
                val h = prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, AutoStoreCloseManager.DEFAULT_HOUR)
                val m = prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, AutoStoreCloseManager.DEFAULT_MINUTE)
                "Auto tutup ${AutoStoreCloseManager.formatTime(h, m)} • Sinkron aktif"
            } else {
                "Pemantauan stok & sinkron aktif"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val updated = PosNotificationHelper.buildForegroundServiceNotification(
                context = this,
                storeName = cachedStoreName,
                lowStockCount = lowCount,
                outOfStockCount = outCount,
                syncSummary = syncSummary
            )
            nm.notify(PosNotificationHelper.NOTIFICATION_ID_FOREGROUND, updated)
        } catch (_: Throwable) {
        }
    }

    private fun stopMonitoringAndSelf() {
        _isRunning.value = false
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PosBackgroundService"
        const val PREFS_NAME = "pos_app_prefs"
        const val KEY_BG_SERVICE_ENABLED = "KEY_BG_SERVICE_ENABLED"
        const val KEY_LOW_STOCK_NOTIF_ENABLED = "KEY_LOW_STOCK_NOTIF_ENABLED"

        const val ACTION_START = "com.example.action.START_POS_BACKGROUND"
        const val ACTION_STOP = "com.example.action.STOP_POS_BACKGROUND"
        const val ACTION_CHECK_STOCK = "com.example.action.CHECK_STOCK_NOW"
        const val EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _userStoppedFromNotif = MutableStateFlow(0L)
        val userStoppedFromNotif: StateFlow<Long> = _userStoppedFromNotif.asStateFlow()

        fun start(context: Context) {
            try {
                PosNotificationHelper.ensureChannels(context)
                val intent = Intent(context, PosBackgroundService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to start PosBackgroundService: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PosBackgroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop PosBackgroundService: ${t.message}")
            }
        }

        fun checkStockNow(context: Context) {
            try {
                val intent = Intent(context, PosBackgroundService::class.java).apply {
                    action = ACTION_CHECK_STOCK
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to trigger stock check: ${t.message}")
            }
        }
    }
}
