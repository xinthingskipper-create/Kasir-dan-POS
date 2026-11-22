package com.example.service

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.os.UserManagerCompat
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
import com.example.PosApplication
import com.example.data.local.PosDatabase
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.PosRepository
import kotlinx.coroutines.CancellationException
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Foreground Service latar belakang untuk menjaga aplikasi Kasir POS tetap responsif saat aktif:
 * 1. Memantau stok makanan, minuman, dan menu secara real-time lalu mengirimkan
 *    push notifikasi otomatis saat stok menipis atau habis.
 * 2. Menjaga sinkronisasi transaksi tertunda ke Cloud Firebase Realtime Database.
 * 3. Menggunakan START_NOT_STICKY, menangani onTimeout() (Android 14/15/16 dataSync 6 jam),
 *    serta mendelegasikan pemantauan ke WorkManager saat aplikasi ditutup agar tidak pernah
 *    memicu ForegroundServiceStartNotAllowedException atau ForegroundServiceDidNotStopInTimeException.
 */
class PosBackgroundService : Service() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Background service coroutine error: ${throwable.message}", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    private var stockMonitorJob: Job? = null
    private var periodicSyncJob: Job? = null

    private var database: PosDatabase? = null
    private var repository: PosRepository? = null
    private var firebaseSyncRepository: FirebaseSyncRepository? = null

    @Volatile
    private var cachedStoreName: String = "Kasir POS"

    override fun onCreate() {
        super.onCreate()
        // Jangan lakukan inisialisasi Room DB atau ServerSocket di Main Thread agar tidak memicu ANR
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!UserManagerCompat.isUserUnlocked(this)) {
            stopMonitoringAndSelf(startId)
            return START_NOT_STICKY
        }

        val action = intent?.action
        if (action == ACTION_STOP) {
            if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
                serviceScope.launch {
                    markStoppedFromNotification(applicationContext)
                }
            }
            stopMonitoringAndSelf(startId)
            return START_NOT_STICKY
        }

        // Jika sistem merestart service di latar belakang dengan intent null pada Android 12+,
        // hentikan tanpa mencoba startForeground agar tidak terkena FGS restriction atau ANR.
        if (intent == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAppInForeground(applicationContext)) {
            serviceScope.launch {
                schedulePeriodicWorker(applicationContext)
            }
            stopMonitoringAndSelf(startId)
            return START_NOT_STICKY
        }

        if (!promoteToForeground()) {
            serviceScope.launch {
                schedulePeriodicWorker(applicationContext)
            }
            stopMonitoringAndSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            if (!ensureDepsInitialized()) {
                stopMonitoringAndSelf(startId)
                return@launch
            }
            if (action == ACTION_CHECK_STOCK) {
                startLoopsIfNeeded()
                checkLowStockOnce(forceAlert = true)
            } else {
                startLoopsIfNeeded()
            }
        }
        return START_NOT_STICKY
    }

    @Synchronized
    private fun ensureDepsInitialized(): Boolean {
        return try {
            if (database == null) {
                val db = PosDatabase.getDatabase(applicationContext)
                database = db
                repository = PosRepository(db)
                firebaseSyncRepository = FirebaseSyncRepository(applicationContext)
                PosNotificationHelper.ensureChannels(applicationContext)
                if (isAppInForeground(applicationContext)) {
                    try {
                        LocalWebReportServer.start(applicationContext, db)
                    } catch (t: Throwable) {
                        Log.w(TAG, "LocalWebReportServer start note: ${t.message}")
                    }
                }
            }
            database != null && repository != null && firebaseSyncRepository != null
        } catch (t: Throwable) {
            Log.e(TAG, "Non-fatal error initializing PosBackgroundService deps: ${t.message}", t)
            false
        }
    }

    override fun onTimeout(startId: Int) {
        Log.w(TAG, "Foreground service reached OS timeout (startId=$startId); handing off to WorkManager")
        Thread({
            try {
                schedulePeriodicWorker(applicationContext)
            } catch (_: Throwable) {
            }
        }, "PosFgsTimeout").start()
        stopMonitoringAndSelf(startId)
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Foreground service reached OS timeout (startId=$startId, type=$fgsType); handing off to WorkManager")
        Thread({
            try {
                schedulePeriodicWorker(applicationContext)
            } catch (_: Throwable) {
            }
        }, "PosFgsTimeout").start()
        stopMonitoringAndSelf(startId)
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
        val db = database ?: return
        val repo = repository ?: return
        val syncRepo = firebaseSyncRepository ?: return

        if (stockMonitorJob?.isActive != true) {
            stockMonitorJob = serviceScope.launch {
                // Observe store name continuously
                launch {
                    try {
                        repo.getStoreProfile().collect { profile ->
                            if (profile != null && profile.storeName.isNotBlank()) {
                                cachedStoreName = profile.storeName
                            }
                        }
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        Log.w(TAG, "Store profile collect warning: ${t.message}")
                    }
                }

                // Observe products continuously for instant low-stock push notifications
                try {
                    db.productDao().getAllProducts().collect { products ->
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
                    if (t is CancellationException) throw t
                    Log.e(TAG, "Product stock monitor error: ${t.message}", t)
                }
            }
        }

        if (periodicSyncJob?.isActive != true) {
            periodicSyncJob = serviceScope.launch {
                while (true) {
                    try {
                        delay(15_000L)
                        runBackgroundSyncCycle(
                            context = applicationContext,
                            database = db,
                            repository = repo,
                            firebaseSyncRepository = syncRepo,
                            cachedStoreName = cachedStoreName,
                            forceStockAlert = false
                        )
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        Log.w(TAG, "Periodic background sync error: ${t.message}")
                    }
                }
            }
        }
    }

    private suspend fun checkLowStockOnce(forceAlert: Boolean) {
        val db = database ?: return
        try {
            val products = db.productDao().getAllProductsList()
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

    private fun stopMonitoringAndSelf(startId: Int? = null) {
        _isRunning.value = false
        stockMonitorJob?.cancel()
        periodicSyncJob?.cancel()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(PosNotificationHelper.NOTIFICATION_ID_FOREGROUND)
        } catch (_: Throwable) {
        }
        if (startId != null) {
            stopSelf(startId)
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        _isRunning.value = false
        stockMonitorJob?.cancel()
        periodicSyncJob?.cancel()
        serviceScope.cancel()
        if (!isAppInForeground(applicationContext)) {
            Thread({
                try {
                    LocalWebReportServer.stop()
                } catch (_: Throwable) {
                }
            }, "LocalWebStop").apply {
                isDaemon = true
                start()
            }
        }
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

        private const val PERIODIC_SYNC_WORK_NAME = "kasir_pos_periodic_background_sync"
        private const val IMMEDIATE_STOCK_CHECK_WORK_NAME = "kasir_pos_immediate_stock_check"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _userStoppedFromNotif = MutableStateFlow(0L)
        val userStoppedFromNotif: StateFlow<Long> = _userStoppedFromNotif.asStateFlow()

        fun isAppInForeground(context: Context): Boolean {
            if (PosApplication.hasStartedActivity()) return true
            return try {
                val info = ActivityManager.RunningAppProcessInfo()
                ActivityManager.getMyMemoryState(info)
                info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
            } catch (_: Throwable) {
                false
            }
        }

        fun markStoppedFromNotification(context: Context) {
            try {
                if (UserManagerCompat.isUserUnlocked(context)) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_BG_SERVICE_ENABLED, false).apply()
                }
            } catch (_: Throwable) {
            }
            _userStoppedFromNotif.value = System.currentTimeMillis()
        }

        fun start(context: Context) {
            if (!UserManagerCompat.isUserUnlocked(context)) return
            try {
                PosNotificationHelper.ensureChannels(context)
                schedulePeriodicWorker(context)

                // Android 12+ (API 31+) melarang startForegroundService dari latar belakang.
                // Gunakan WorkManager bila aplikasi sedang tidak terbuka di layar.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAppInForeground(context)) {
                    Log.i(TAG, "App is in background on API ${Build.VERSION.SDK_INT}; relying on WorkManager instead of FGS")
                    return
                }

                val intent = Intent(context, PosBackgroundService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to start PosBackgroundService: ${t.message}")
            }
        }

        fun stop(context: Context) {
            _isRunning.value = false
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(PosNotificationHelper.NOTIFICATION_ID_FOREGROUND)
            } catch (_: Throwable) {
            }
            try {
                // Jangan pernah memanggil startService() saat aplikasi di background karena akan melempar
                // BackgroundServiceStartNotAllowedException pada Android 8+. Gunakan stopService().
                context.stopService(Intent(context, PosBackgroundService::class.java))
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop PosBackgroundService: ${t.message}")
            }
            cancelPeriodicWorker(context)
        }

        fun checkStockNow(context: Context) {
            if (!UserManagerCompat.isUserUnlocked(context)) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAppInForeground(context)) {
                enqueueImmediateStockCheck(context)
                return
            }
            try {
                val intent = Intent(context, PosBackgroundService::class.java).apply {
                    action = ACTION_CHECK_STOCK
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to trigger FGS stock check, falling back to WorkManager: ${t.message}")
                enqueueImmediateStockCheck(context)
            }
        }

        fun schedulePeriodicWorker(context: Context) {
            if (!UserManagerCompat.isUserUnlocked(context)) return
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val bgEnabled = prefs.getBoolean(KEY_BG_SERVICE_ENABLED, true)
                val autoCloseEnabled = prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)
                val wm = WorkManager.getInstance(context.applicationContext)
                if (!bgEnabled && !autoCloseEnabled) {
                    wm.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
                    return
                }
                val request = PeriodicWorkRequestBuilder<PosBackgroundSyncWorker>(15, TimeUnit.MINUTES)
                    .setInputData(workDataOf(PosBackgroundSyncWorker.KEY_FORCE_STOCK_ALERT to false))
                    .build()
                wm.enqueueUniquePeriodicWork(
                    PERIODIC_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to schedule PosBackgroundSyncWorker: ${t.message}")
            }
        }

        fun cancelPeriodicWorker(context: Context) {
            try {
                WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
            } catch (_: Throwable) {
            }
        }

        fun enqueueImmediateStockCheck(context: Context) {
            if (!UserManagerCompat.isUserUnlocked(context)) return
            try {
                val request = OneTimeWorkRequestBuilder<PosBackgroundSyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                    .setInputData(workDataOf(PosBackgroundSyncWorker.KEY_FORCE_STOCK_ALERT to true))
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    IMMEDIATE_STOCK_CHECK_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to enqueue immediate stock check worker: ${t.message}")
            }
        }

        suspend fun runBackgroundSyncCycle(
            context: Context,
            database: PosDatabase,
            repository: PosRepository,
            firebaseSyncRepository: FirebaseSyncRepository,
            cachedStoreName: String = "Kasir POS",
            forceStockAlert: Boolean = false
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val dbId = prefs.getString("KEY_FIREBASE_DATABASE_ID", "foodrock-report-default-rtdb")
                ?.ifBlank { "foodrock-report-default-rtdb" }
                ?: "foodrock-report-default-rtdb"
            val projId = prefs.getString("KEY_FIREBASE_PROJECT_ID", "foodrock-report")
                ?.ifBlank { "foodrock-report" }
                ?: "foodrock-report"
            val rawHostUrl = prefs.getString("KEY_WEB_HOSTING_URL", "")
            val hostUrl = if (rawHostUrl.isNullOrBlank() || FirebaseSyncRepository.isAiStudioProxyUrl(rawHostUrl)) {
                if (LocalWebReportServer.isRunning()) LocalWebReportServer.getLocalBaseUrl() else ""
            } else {
                rawHostUrl.trim()
            }

            // 1. Sync pending transactions in background
            try {
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
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "Pending transaction sync note: ${t.message}")
            }

            // 1B. Retry pending OR not-yet-remote-synced expenses in background
            try {
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
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "Pending expense sync note: ${t.message}")
            }

            // 2. Check low-stock alert if requested or enabled when running from WorkManager
            if (forceStockAlert || prefs.getBoolean(KEY_LOW_STOCK_NOTIF_ENABLED, true)) {
                try {
                    val products = database.productDao().getAllProductsList()
                    val storeName = try {
                        repository.getStoreProfileSync()?.storeName?.ifBlank { cachedStoreName } ?: cachedStoreName
                    } catch (_: Throwable) {
                        cachedStoreName
                    }
                    PosNotificationHelper.notifyLowStockIfNeeded(
                        context = context.applicationContext,
                        storeName = storeName,
                        allProducts = products,
                        forceAlert = forceStockAlert
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Log.w(TAG, "Low stock worker check note: ${t.message}")
                }
            }

            // 3. Jalankan Tutup Toko Otomatis Terjadwal jika jam tutup telah tiba
            try {
                AutoStoreCloseManager.executeScheduledStoreClosing(
                    context = context.applicationContext,
                    triggeredByAlarm = false
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "Auto close background check note: ${t.message}")
            }

            // 4. Check date rollover in background so midnight reset happens automatically
            checkBackgroundDateRollover(prefs, dbId, firebaseSyncRepository)

            // 5. Jalankan Auto Backup Google Drive terjadwal jika waktunya tiba
            checkScheduledDriveBackup(context.applicationContext, prefs)
        }

        private suspend fun checkScheduledDriveBackup(appContext: Context, prefs: SharedPreferences) {
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
                        context = appContext,
                        reason = "Auto Backup (${freq.label})",
                        isManual = false
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "Scheduled Drive backup check warning: ${t.message}")
            }
        }

        private suspend fun checkBackgroundDateRollover(
            prefs: SharedPreferences,
            dbId: String,
            firebaseSyncRepository: FirebaseSyncRepository
        ) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val currentDateStr = dateFormat.format(Date())
                val lastDateStr = prefs.getString("KEY_LAST_ACTIVE_DATE", "") ?: ""

                if (lastDateStr.isNotBlank() && lastDateStr != currentDateStr) {
                    val isoStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                    val resetStats = JSONObject(mapOf(
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
                if (t is CancellationException) throw t
                Log.w(TAG, "Background date rollover check warning: ${t.message}")
            }
        }
    }
}

/**
 * WorkManager Worker aman untuk Android 12–16 saat aplikasi sedang tertutup/di latar belakang
 * sehingga sinkronisasi transaksi, cek stok menipis, tutup toko otomatis, dan backup Drive tetap berjalan
 * tanpa bergantung pada Foreground Service yang dapat dihentikan paksa oleh OS.
 */
class PosBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!UserManagerCompat.isUserUnlocked(applicationContext)) {
                return Result.retry()
            }
            val forceStockAlert = inputData.getBoolean(KEY_FORCE_STOCK_ALERT, false)
            val database = PosDatabase.getDatabase(applicationContext)
            val repository = PosRepository(database)
            val syncRepo = FirebaseSyncRepository(applicationContext)
            PosBackgroundService.runBackgroundSyncCycle(
                context = applicationContext,
                database = database,
                repository = repository,
                firebaseSyncRepository = syncRepo,
                forceStockAlert = forceStockAlert
            )
            Result.success()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w("PosBackgroundSyncWorker", "Background sync worker handled error: ${t.message}", t)
            Result.success()
        }
    }

    companion object {
        const val KEY_FORCE_STOCK_ALERT = "KEY_FORCE_STOCK_ALERT"
    }
}
