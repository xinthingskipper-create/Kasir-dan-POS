package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothPrinterManager
import com.example.bluetooth.PrinterDevice
import com.example.data.local.PaymentSummary
import com.example.data.local.PosDatabase
import com.example.data.local.TopProductSummary
import com.example.data.model.DailyReport
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.User
import com.example.data.model.UserRole
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.repository.BackupRestoreManager
import com.example.data.repository.BackupStats
import com.example.data.repository.DriveAuthResult
import com.example.data.repository.DriveBackupFile
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.GoogleDriveBackupManager
import com.example.data.repository.PosRepository
import com.example.service.DriveAutoBackupManager
import com.example.service.DriveBackupFrequency
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String = ""
) {
    val subtotal: Double
        get() = product.sellPrice * quantity
}

enum class Screen {
    SPLASH,
    LOGIN,
    MAIN
}

enum class MainTab(val title: String) {
    POS("Kasir"),
    INVENTORY("Inventaris"),
    EXPENSES("Pengeluaran"),
    REPORT("Laporan"),
    USERS("Karyawan"),
    PRINTER("Printer"),
    SETTINGS("Toko"),
    CLOUD_HOSTING("Hosting & Cloud")
}

enum class StoreSettingsCategory(val label: String, val subtitle: String) {
    PROFILE("Profil & Struk", "Identitas toko, WhatsApp owner, dan catatan struk"),
    OPERATIONAL("Operasional", "Kategori menu, satuan produk, dan jumlah meja"),
    AUTOMATION("Layanan & Jadwal", "Layanan latar belakang, notif stok, dan tutup toko otomatis"),
    HOSTING("Hosting & Web", "Firebase Project ID, Realtime Database, dan URL web"),
    BACKUP("Backup & Restore", "Auto backup Google Drive serta ekspor & restore JSON lokal")
}

enum class DateFilter(val label: String) {
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    CUSTOM_DATE("Pilih Tanggal"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    ALL("Semua")
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PosViewModel", "Intercepted coroutine error safely: ${throwable.message}", throwable)
    }

    private val database = PosDatabase.getDatabase(application)
    private val repository = PosRepository(database)
    val printerManager = BluetoothPrinterManager(application)
    val firebaseSyncRepository = FirebaseSyncRepository(application)
    val backupRestoreManager = BackupRestoreManager(application, database)
    val googleDriveBackupManager = GoogleDriveBackupManager(application)

    val cloudUser: StateFlow<FirebaseUser?> = firebaseSyncRepository.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseSyncRepository.currentUser)

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow<String?>("Siap Sinkron ke Firebase")
    val lastSyncStatus: StateFlow<String?> = _lastSyncStatus.asStateFlow()

    // --- Preferences & Web Hosting Configuration ---
    private val prefs = application.getSharedPreferences("pos_app_prefs", android.content.Context.MODE_PRIVATE)

    val defaultFirebaseProjectId: String = "foodrock-report"
    val defaultDatabaseId: String = "foodrock-report-default-rtdb"

    val localWebServerUrl: StateFlow<String?> = com.example.service.LocalWebReportServer.serverUrlFlow

    val defaultHostingUrl: String
        get() = com.example.service.LocalWebReportServer.serverUrlFlow.value ?: "http://127.0.0.1:8080/owner.html"

    fun getLanWebUrl(): String? = com.example.service.LocalWebReportServer.getLanBaseUrl()?.let { "$it/owner.html" }

    fun resolveEffectiveHostingUrl(rawUrl: String? = null): String {
        val candidate = (rawUrl ?: _webDashboardUrl.value).trim()
        if (candidate.isEmpty() ||
            FirebaseSyncRepository.isAiStudioProxyUrl(candidate) ||
            candidate.contains("localhost:3000", ignoreCase = true) ||
            candidate.contains("127.0.0.1:3000", ignoreCase = true)
        ) {
            return defaultHostingUrl
        }
        // Rewrite stale loopback host/port (e.g. http://127.0.0.1:8080/owner.html when bound on :8765)
        if (FirebaseSyncRepository.isLoopbackUrl(candidate) && com.example.service.LocalWebReportServer.isRunning()) {
            val activeBase = com.example.service.LocalWebReportServer.getLocalBaseUrl().trimEnd('/')
            val withoutScheme = candidate.substringAfter("://", candidate)
            val slashIdx = withoutScheme.indexOf('/')
            val suffix = if (slashIdx >= 0) withoutScheme.substring(slashIdx) else "/owner.html"
            return "$activeBase$suffix"
        }
        return candidate
    }

    private val savedProjectId = prefs.getString("KEY_FIREBASE_PROJECT_ID", null)?.let {
        if (FirebaseSyncRepository.isPlaceholderCloudId(it)) defaultFirebaseProjectId else it.trim()
    } ?: defaultFirebaseProjectId

    private val savedDatabaseId = prefs.getString("KEY_FIREBASE_DATABASE_ID", null)?.let {
        if (FirebaseSyncRepository.isPlaceholderCloudId(it)) defaultDatabaseId else it.trim()
    } ?: defaultDatabaseId

    private val initialRawHostUrl = prefs.getString("KEY_WEB_HOSTING_URL", null)
    private val _webDashboardUrl = MutableStateFlow(
        if (initialRawHostUrl.isNullOrBlank() || FirebaseSyncRepository.isAiStudioProxyUrl(initialRawHostUrl)) {
            "http://127.0.0.1:8080/owner.html"
        } else {
            initialRawHostUrl.trim()
        }
    )

    private val _firebaseProjectId = MutableStateFlow(savedProjectId)
    val firebaseProjectId: StateFlow<String> = _firebaseProjectId.asStateFlow()

    private val _firebaseDatabaseId = MutableStateFlow(savedDatabaseId)
    val firebaseDatabaseId: StateFlow<String> = _firebaseDatabaseId.asStateFlow()

    val webDashboardUrl: StateFlow<String> = _webDashboardUrl.asStateFlow()

    val currentWebDashboardUrl: String
        get() = resolveEffectiveHostingUrl(_webDashboardUrl.value)

    fun buildReportUrl(
        rawBaseUrl: String? = null,
        projectId: String? = null,
        databaseId: String? = null,
        view: String? = null,
        dateMillis: Long? = null
    ): String {
        com.example.service.LocalWebReportServer.start(getApplication(), database)
        val rawResolved = resolveEffectiveHostingUrl(rawBaseUrl ?: webDashboardUrl.value).trimEnd('/')
        val baseUrl = if (!rawResolved.endsWith(".html", ignoreCase = true) && !rawResolved.contains("?")) {
            "$rawResolved/owner.html"
        } else {
            rawResolved
        }
        val projId = (projectId ?: firebaseProjectId.value).trim().let {
            if (FirebaseSyncRepository.isPlaceholderCloudId(it)) defaultFirebaseProjectId else it
        }
        val dbId = (databaseId ?: firebaseDatabaseId.value).trim().let {
            if (FirebaseSyncRepository.isPlaceholderCloudId(it)) defaultDatabaseId else it
        }
        val localRelay = com.example.service.LocalWebReportServer.getLocalBaseUrl()
        val sep = if (baseUrl.contains("?")) "&" else "?"
        val viewParam = if (!view.isNullOrBlank()) "&view=${Uri.encode(view)}" else ""
        val dateParam = dateMillis?.let { millis ->
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(millis))
            "&date=${Uri.encode(dateStr)}"
        }.orEmpty()
        return "$baseUrl${sep}projectId=${Uri.encode(projId)}&databaseId=${Uri.encode(dbId)}&localRelay=${Uri.encode(localRelay)}$viewParam$dateParam"
    }

    fun buildChromeWebReportUrl(view: String? = null, dateMillis: Long? = null): String =
        buildReportUrl(
            rawBaseUrl = webDashboardUrl.value,
            projectId = null,
            databaseId = null,
            view = view,
            dateMillis = dateMillis
        )

    fun updateWebHostingConfig(projectId: String, databaseId: String, hostingUrl: String) {
        val cleanPid = projectId.trim().let {
            if (FirebaseSyncRepository.isPlaceholderCloudId(it)) defaultFirebaseProjectId else it
        }
        val cleanDbId = databaseId.trim().let {
            if (FirebaseSyncRepository.isPlaceholderCloudId(it)) defaultDatabaseId else it
        }
        val cleanUrl = resolveEffectiveHostingUrl(hostingUrl)

        _firebaseProjectId.value = cleanPid
        _firebaseDatabaseId.value = cleanDbId
        _webDashboardUrl.value = cleanUrl

        prefs.edit()
            .putString("KEY_FIREBASE_PROJECT_ID", cleanPid)
            .putString("KEY_FIREBASE_DATABASE_ID", cleanDbId)
            .putString("KEY_WEB_HOSTING_URL", cleanUrl)
            .apply()
        scheduleDebouncedDriveBackupIfActive("Perubahan Pengaturan Hosting")
    }

    fun resetWebHostingConfig() {
        updateWebHostingConfig(defaultFirebaseProjectId, defaultDatabaseId, defaultHostingUrl)
    }

    // --- Room Database Cloud Hosting Persistence ---
    val cloudHostingConfig: StateFlow<com.example.data.model.CloudHostingConfig> = repository.getCloudHostingConfig()
        .map { it ?: com.example.data.model.CloudHostingConfig() }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            com.example.data.model.CloudHostingConfig()
        )

    // --- Pelacakan Laporan & Sinkronisasi Database Otomatis ---
    private val _isAutoSyncActive = MutableStateFlow(true)
    val isAutoSyncActive: StateFlow<Boolean> = _isAutoSyncActive.asStateFlow()

    val unsyncedCount: StateFlow<Int> = repository.getUnsyncedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedCount: StateFlow<Int> = repository.getSyncedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unsyncedTransactions: StateFlow<List<Transaction>> = repository.getUnsyncedTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAutoSync(enabled: Boolean) {
        _isAutoSyncActive.value = enabled
    }

    // --- Layanan Latar Belakang & Push Notifikasi Stok Menipis/Habis ---
    private val _isBackgroundServiceEnabled = MutableStateFlow(
        prefs.getBoolean(com.example.service.PosBackgroundService.KEY_BG_SERVICE_ENABLED, true)
    )
    val isBackgroundServiceEnabled: StateFlow<Boolean> = _isBackgroundServiceEnabled.asStateFlow()

    private val _isLowStockNotifEnabled = MutableStateFlow(
        prefs.getBoolean(com.example.service.PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, true)
    )
    val isLowStockNotifEnabled: StateFlow<Boolean> = _isLowStockNotifEnabled.asStateFlow()

    val isBackgroundServiceRunning: StateFlow<Boolean> = com.example.service.PosBackgroundService.isRunning

    // --- Pengaturan Tutup Toko Otomatis Terjadwal ---
    private val _isAutoCloseEnabled = MutableStateFlow(
        prefs.getBoolean(com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)
    )
    val isAutoCloseEnabled: StateFlow<Boolean> = _isAutoCloseEnabled.asStateFlow()

    private val _autoCloseHour = MutableStateFlow(
        prefs.getInt(
            com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR,
            com.example.service.AutoStoreCloseManager.DEFAULT_HOUR
        ).coerceIn(0, 23)
    )
    val autoCloseHour: StateFlow<Int> = _autoCloseHour.asStateFlow()

    private val _autoCloseMinute = MutableStateFlow(
        prefs.getInt(
            com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE,
            com.example.service.AutoStoreCloseManager.DEFAULT_MINUTE
        ).coerceIn(0, 59)
    )
    val autoCloseMinute: StateFlow<Int> = _autoCloseMinute.asStateFlow()

    private val _lastAutoCloseStatus = MutableStateFlow(
        prefs.getString(com.example.service.AutoStoreCloseManager.KEY_LAST_AUTO_CLOSE_STATUS, "") ?: ""
    )
    val lastAutoCloseStatus: StateFlow<String> = _lastAutoCloseStatus.asStateFlow()

    fun setAutoCloseSchedule(
        enabled: Boolean,
        hour: Int = _autoCloseHour.value,
        minute: Int = _autoCloseMinute.value
    ) {
        val cleanHour = hour.coerceIn(0, 23)
        val cleanMinute = minute.coerceIn(0, 59)
        _isAutoCloseEnabled.value = enabled
        _autoCloseHour.value = cleanHour
        _autoCloseMinute.value = cleanMinute

        prefs.edit()
            .putBoolean(com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, enabled)
            .putInt(com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, cleanHour)
            .putInt(com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, cleanMinute)
            .putLong(com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_CONFIGURED_AT, System.currentTimeMillis())
            .apply()

        val appContext = getApplication<Application>()
        if (enabled) {
            com.example.service.PosNotificationHelper.ensureChannels(appContext)
            com.example.service.AutoStoreCloseManager.scheduleNextAlarm(appContext)
            if (!_isBackgroundServiceEnabled.value) {
                setBackgroundServiceEnabled(true)
            } else {
                com.example.service.PosBackgroundService.start(appContext)
            }
        } else {
            com.example.service.AutoStoreCloseManager.cancelAlarm(appContext)
            if (_isBackgroundServiceEnabled.value) {
                com.example.service.PosBackgroundService.start(appContext)
            }
        }
        scheduleDebouncedDriveBackupIfActive("Perubahan Jadwal Tutup Toko")
    }

    fun getNextAutoCloseDescription(): String {
        val lastDate = prefs.getString(com.example.service.AutoStoreCloseManager.KEY_LAST_AUTO_CLOSE_DATE, "") ?: ""
        return com.example.service.AutoStoreCloseManager.formatNextScheduleDescription(
            enabled = _isAutoCloseEnabled.value,
            hour = _autoCloseHour.value,
            minute = _autoCloseMinute.value,
            lastAutoCloseDate = lastDate
        )
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        _isBackgroundServiceEnabled.value = enabled
        prefs.edit().putBoolean(com.example.service.PosBackgroundService.KEY_BG_SERVICE_ENABLED, enabled).apply()
        val appContext = getApplication<Application>()
        if (enabled) {
            com.example.service.PosBackgroundService.start(appContext)
        } else {
            com.example.service.PosBackgroundService.stop(appContext)
        }
        scheduleDebouncedDriveBackupIfActive("Perubahan Layanan Latar Belakang")
    }

    fun setLowStockNotifEnabled(enabled: Boolean) {
        _isLowStockNotifEnabled.value = enabled
        prefs.edit().putBoolean(com.example.service.PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, enabled).apply()
        if (enabled) {
            checkLowStockAndNotify(forceAlert = true)
        }
        scheduleDebouncedDriveBackupIfActive("Perubahan Notifikasi Stok")
    }

    fun ensureBackgroundServiceStarted() {
        val appContext = getApplication<Application>()
        com.example.service.PosNotificationHelper.ensureChannels(appContext)
        if (_isBackgroundServiceEnabled.value && !com.example.service.PosBackgroundService.isRunning.value) {
            com.example.service.PosBackgroundService.start(appContext)
        }
        if (_isAutoCloseEnabled.value) {
            com.example.service.AutoStoreCloseManager.scheduleNextAlarm(appContext)
        }
        if (_isLowStockNotifEnabled.value) {
            checkLowStockAndNotify(forceAlert = false)
        }
    }

    fun checkLowStockAndNotify(
        forceAlert: Boolean = false,
        onResult: ((criticalCount: Int, sentCount: Int) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val allProds = repository.getAllProductsList()
            val criticalCount = allProds.count { it.isAvailable && (it.isLowStock || it.isOutOfStock) }
            val shouldSend = _isLowStockNotifEnabled.value || forceAlert
            val sentCount = if (shouldSend) {
                com.example.service.PosNotificationHelper.notifyLowStockIfNeeded(
                    context = getApplication(),
                    storeName = storeProfile.value.storeName,
                    allProducts = allProds,
                    forceAlert = forceAlert
                )
            } else {
                0
            }
            if (onResult != null) {
                withContext(Dispatchers.Main) {
                    onResult(criticalCount, sentCount)
                }
            }
        }
    }

    fun saveCloudHostingConfig(config: com.example.data.model.CloudHostingConfig) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.saveCloudHostingConfig(config)
                updateWebHostingConfig(
                    projectId = config.ownerProjectId,
                    databaseId = config.ownerDatabaseId,
                    hostingUrl = config.ownerHostingUrl
                )
            } catch (t: Throwable) {
                Log.w("PosViewModel", "Failed to save cloud config to Room", t)
            }
        }
    }

    init {
        com.example.service.LocalWebReportServer.start(application, database)
        _webDashboardUrl.value = resolveEffectiveHostingUrl(_webDashboardUrl.value)

        viewModelScope.launch(coroutineExceptionHandler) {
            com.example.service.LocalWebReportServer.serverUrlFlow.collect { url ->
                if (!url.isNullOrBlank()) {
                    _webDashboardUrl.value = resolveEffectiveHostingUrl(_webDashboardUrl.value)
                }
            }
        }

        if (_isAutoCloseEnabled.value) {
            com.example.service.AutoStoreCloseManager.scheduleNextAlarm(application)
        }
        if (DriveAutoBackupManager.isAutoBackupEnabled(application)) {
            DriveAutoBackupManager.schedulePeriodicBackup(application)
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            DriveAutoBackupManager.lastBackupEventMillis.collect { ts ->
                if (ts > 0L) {
                    reloadDriveStatusFromPrefs()
                }
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            com.example.service.AutoStoreCloseManager.lastAutoCloseEventMillis.collect { ts ->
                if (ts > 0L) {
                    _lastAutoCloseStatus.value = prefs.getString(
                        com.example.service.AutoStoreCloseManager.KEY_LAST_AUTO_CLOSE_STATUS,
                        ""
                    ) ?: ""
                    refreshReportData()
                }
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            com.example.service.PosBackgroundService.userStoppedFromNotif.collect { ts ->
                if (ts > 0L) {
                    _isBackgroundServiceEnabled.value = false
                }
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.seedDefaultUsersIfEmpty()
            } catch (e: Throwable) {
                // Ignore transient seeding errors
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.deleteProductsByCategory("Sembako")
            } catch (e: Throwable) {
                // Ignore
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.getCloudHostingConfig().collect { config ->
                    if (config != null) {
                        if (!FirebaseSyncRepository.isPlaceholderCloudId(config.ownerProjectId)) {
                            _firebaseProjectId.value = config.ownerProjectId.trim()
                        }
                        if (!FirebaseSyncRepository.isPlaceholderCloudId(config.ownerDatabaseId)) {
                            _firebaseDatabaseId.value = config.ownerDatabaseId.trim()
                        }
                        _webDashboardUrl.value = resolveEffectiveHostingUrl(config.ownerHostingUrl)
                    }
                }
            } catch (e: Throwable) {
                Log.w("PosViewModel", "Error collecting cloud hosting config", e)
            }
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                kotlinx.coroutines.delay(2500)
                syncPendingTransactionsInternal()
                syncPendingExpensesInternal()
            } catch (e: Throwable) {
                // Ignore transient initial sync errors
            }
        }
        // Recurring Background Synchronization Engine (checks pending transactions & expenses every 25s)
        viewModelScope.launch(coroutineExceptionHandler) {
            while (true) {
                try {
                    kotlinx.coroutines.delay(25000)
                    if (_isAutoSyncActive.value && !_isCloudSyncing.value) {
                        syncPendingTransactionsInternal()
                        syncPendingExpensesInternal()
                    }
                } catch (e: Throwable) {
                    // Ignore periodic background sync error
                }
            }
        }

        // Date Rollover & Auto-Archive Engine (initial check & periodic check every 30s)
        checkDateRolloverAndAutoArchive()
        viewModelScope.launch(coroutineExceptionHandler) {
            while (true) {
                try {
                    kotlinx.coroutines.delay(30000)
                    checkDateRolloverAndAutoArchive()
                } catch (e: Throwable) {
                    // Ignore periodic check error
                }
            }
        }
    }

    // --- Navigation & App State ---
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.POS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _selectedStoreSettingsCategory = MutableStateFlow(StoreSettingsCategory.PROFILE)
    val selectedStoreSettingsCategory: StateFlow<StoreSettingsCategory> = _selectedStoreSettingsCategory.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectStoreSettingsCategory(category: StoreSettingsCategory) {
        _selectedStoreSettingsCategory.value = category
    }

    fun openStoreSettings(category: StoreSettingsCategory) {
        _selectedStoreSettingsCategory.value = category
        selectTab(MainTab.SETTINGS)
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        if (tab == MainTab.REPORT) {
            checkDateRolloverAndAutoArchive()
            refreshReportData()
            syncExpensesForTransactionReportAuto(forceAll = false)
        } else if (tab == MainTab.EXPENSES) {
            syncExpensesForTransactionReportAuto(forceAll = false)
        }
    }

    // --- Auth & Users with Remember Me ---
    private val _rememberMe = MutableStateFlow(prefs.getBoolean("KEY_REMEMBER_ME", false))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    val savedUsername: String
        get() = prefs.getString("KEY_SAVED_USERNAME", "") ?: ""

    val savedPin: String
        get() = prefs.getString("KEY_SAVED_PIN", "") ?: ""

    fun setRememberMe(enabled: Boolean) {
        _rememberMe.value = enabled
        prefs.edit().putBoolean("KEY_REMEMBER_ME", enabled).apply()
        if (!enabled) {
            prefs.edit().remove("KEY_SAVED_USERNAME").remove("KEY_SAVED_PIN").apply()
        }
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    val allUsers: StateFlow<List<User>> = repository.getAllActiveUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun login(username: String, pin: String) {
        if (username.isBlank() || pin.isBlank()) {
            _loginError.value = "Username dan PIN tidak boleh kosong"
            return
        }

        viewModelScope.launch {
            val user = repository.authenticate(username, pin)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null

                if (_rememberMe.value) {
                    prefs.edit()
                        .putBoolean("KEY_REMEMBER_ME", true)
                        .putString("KEY_SAVED_USERNAME", username.trim())
                        .putString("KEY_SAVED_PIN", pin.trim())
                        .apply()
                } else {
                    prefs.edit()
                        .putBoolean("KEY_REMEMBER_ME", false)
                        .remove("KEY_SAVED_USERNAME")
                        .remove("KEY_SAVED_PIN")
                        .apply()
                }

                _currentTab.value = MainTab.POS
                _currentScreen.value = Screen.MAIN
            } else {
                _loginError.value = "Username atau PIN salah. Coba lagi."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        clearCart()
        _currentScreen.value = Screen.LOGIN
    }

    fun resetDefaultUsers() {
        viewModelScope.launch {
            repository.resetDefaultUsers()
            _loginError.value = "Akun default berhasil dipulihkan! Silakan pilih akun pada Akses Cepat Staff."
        }
    }

    fun addUser(username: String, pin: String, fullName: String, role: UserRole, phone: String) {
        viewModelScope.launch {
            val user = User(
                username = username.trim().lowercase(),
                pin = pin.trim(),
                fullName = fullName.trim(),
                role = role,
                phone = phone.trim()
            )
            repository.insertUser(user)
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // --- Store Profile ---
    val storeProfile: StateFlow<StoreProfile> = repository.getStoreProfile()
        .map { it ?: StoreProfile(id = 1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoreProfile(id = 1))

    fun updateStoreProfile(name: String, address: String, phone: String, ownerPhone: String, footer: String) {
        viewModelScope.launch {
            val updated = StoreProfile(
                id = 1,
                storeName = name.trim().ifEmpty { "KASIR POS" },
                address = address.trim(),
                phone = phone.trim(),
                ownerPhone = ownerPhone.trim(),
                receiptFooter = footer.trim()
            )
            repository.updateStoreProfile(updated)
            scheduleDebouncedDriveBackupIfActive("Perubahan Profil Toko")

            // Also sync store profile to Firebase Realtime Database & Hosting Relay
            try {
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val hostUrl = webDashboardUrl.value
                firebaseSyncRepository.syncStoreProfile(updated, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
            } catch (t: Throwable) {
                Log.w("PosViewModel", "Failed to sync store profile to RTDB: ${t.message}")
            }
        }
    }

    // --- Pengaturan Kategori, Satuan (Porsi/Pcs) & Jumlah Meja ---
    private fun loadStringListPref(key: String, defaults: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return defaults
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotBlank() && list.none { it.equals(item, ignoreCase = true) }) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) defaults else list
        } catch (_: Throwable) {
            defaults
        }
    }

    private fun saveStringListPref(key: String, values: List<String>) {
        val arr = JSONArray()
        values.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private val _configuredCategories = MutableStateFlow(
        loadStringListPref(KEY_CUSTOM_CATEGORIES, DEFAULT_CATEGORIES)
    )
    val configuredCategories: StateFlow<List<String>> = _configuredCategories.asStateFlow()

    private val _configuredUnits = MutableStateFlow(
        loadStringListPref(KEY_CUSTOM_UNITS, DEFAULT_UNITS)
    )
    val configuredUnits: StateFlow<List<String>> = _configuredUnits.asStateFlow()

    private val _foodDefaultUnit = MutableStateFlow(
        prefs.getString(KEY_FOOD_DEFAULT_UNIT, DEFAULT_FOOD_UNIT)?.trim()?.ifBlank { DEFAULT_FOOD_UNIT }
            ?: DEFAULT_FOOD_UNIT
    )
    val foodDefaultUnit: StateFlow<String> = _foodDefaultUnit.asStateFlow()

    private val _drinkDefaultUnit = MutableStateFlow(
        prefs.getString(KEY_DRINK_DEFAULT_UNIT, DEFAULT_DRINK_UNIT)?.trim()?.ifBlank { DEFAULT_DRINK_UNIT }
            ?: DEFAULT_DRINK_UNIT
    )
    val drinkDefaultUnit: StateFlow<String> = _drinkDefaultUnit.asStateFlow()

    private val _tableCount = MutableStateFlow(
        prefs.getInt(KEY_TABLE_COUNT, DEFAULT_TABLE_COUNT).coerceIn(0, 100)
    )
    val tableCount: StateFlow<Int> = _tableCount.asStateFlow()

    fun defaultUnitForCategory(category: String): String {
        val normalized = category.trim().lowercase(java.util.Locale.ROOT)
        return when {
            normalized.contains("makan") || normalized.contains("food") -> _foodDefaultUnit.value
            normalized.contains("minum") || normalized.contains("drink") ||
                normalized.contains("kopi") || normalized.contains("teh") ||
                normalized.contains("jus") || normalized.contains("juice") -> _drinkDefaultUnit.value
            else -> _configuredUnits.value.firstOrNull { it.equals("pcs", ignoreCase = true) }
                ?: _configuredUnits.value.firstOrNull()
                ?: "pcs"
        }
    }

    fun addCategory(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        val current = _configuredCategories.value
        if (current.any { it.equals(clean, ignoreCase = true) }) return false
        val updated = current + clean
        _configuredCategories.value = updated
        saveStringListPref(KEY_CUSTOM_CATEGORIES, updated)
        scheduleDebouncedDriveBackupIfActive("Perubahan Kategori Menu")
        return true
    }

    fun renameCategory(oldName: String, newName: String): Boolean {
        val cleanOld = oldName.trim()
        val cleanNew = newName.trim()
        if (cleanOld.isEmpty() || cleanNew.isEmpty()) return false
        val current = _configuredCategories.value
        val duplicateWithOther = current.any {
            !it.equals(cleanOld, ignoreCase = true) && it.equals(cleanNew, ignoreCase = true)
        }
        if (duplicateWithOther) return false
        val updated = current.map { if (it.equals(cleanOld, ignoreCase = true)) cleanNew else it }
            .let { if (it.any { c -> c.equals(cleanNew, ignoreCase = true) }) it else it + cleanNew }
        _configuredCategories.value = updated
        saveStringListPref(KEY_CUSTOM_CATEGORIES, updated)
        if (_selectedCategory.value?.equals(cleanOld, ignoreCase = true) == true) {
            _selectedCategory.value = cleanNew
        }
        viewModelScope.launch {
            repository.renameProductCategory(cleanOld, cleanNew)
        }
        scheduleDebouncedDriveBackupIfActive("Perubahan Nama Kategori")
        return true
    }

    fun removeCategory(name: String): Boolean {
        val clean = name.trim()
        val current = _configuredCategories.value
        if (current.size <= 1) return false
        val updated = current.filterNot { it.equals(clean, ignoreCase = true) }
        if (updated.size == current.size) return false
        _configuredCategories.value = updated
        saveStringListPref(KEY_CUSTOM_CATEGORIES, updated)
        if (_selectedCategory.value?.equals(clean, ignoreCase = true) == true) {
            _selectedCategory.value = null
        }
        scheduleDebouncedDriveBackupIfActive("Hapus Kategori Menu")
        return true
    }

    fun addUnit(unitName: String): Boolean {
        val clean = unitName.trim()
        if (clean.isEmpty()) return false
        val current = _configuredUnits.value
        if (current.any { it.equals(clean, ignoreCase = true) }) return false
        val updated = current + clean
        _configuredUnits.value = updated
        saveStringListPref(KEY_CUSTOM_UNITS, updated)
        scheduleDebouncedDriveBackupIfActive("Tambah Satuan Produk")
        return true
    }

    fun removeUnit(unitName: String): Boolean {
        val clean = unitName.trim()
        val current = _configuredUnits.value
        if (current.size <= 1) return false
        if (clean.equals(_foodDefaultUnit.value, ignoreCase = true) ||
            clean.equals(_drinkDefaultUnit.value, ignoreCase = true)
        ) {
            return false
        }
        val updated = current.filterNot { it.equals(clean, ignoreCase = true) }
        if (updated.size == current.size) return false
        _configuredUnits.value = updated
        saveStringListPref(KEY_CUSTOM_UNITS, updated)
        scheduleDebouncedDriveBackupIfActive("Hapus Satuan Produk")
        return true
    }

    fun setFoodDefaultUnit(unit: String, applyToExistingProducts: Boolean = false) {
        val clean = unit.trim().ifBlank { DEFAULT_FOOD_UNIT }
        if (_configuredUnits.value.none { it.equals(clean, ignoreCase = true) }) {
            addUnit(clean)
        }
        _foodDefaultUnit.value = clean
        prefs.edit().putString(KEY_FOOD_DEFAULT_UNIT, clean).apply()
        if (applyToExistingProducts) {
            applyUnitToCategoryProducts("Makanan", clean)
        }
        scheduleDebouncedDriveBackupIfActive("Ubah Satuan Default Makanan")
    }

    fun setDrinkDefaultUnit(unit: String, applyToExistingProducts: Boolean = false) {
        val clean = unit.trim().ifBlank { DEFAULT_DRINK_UNIT }
        if (_configuredUnits.value.none { it.equals(clean, ignoreCase = true) }) {
            addUnit(clean)
        }
        _drinkDefaultUnit.value = clean
        prefs.edit().putString(KEY_DRINK_DEFAULT_UNIT, clean).apply()
        if (applyToExistingProducts) {
            applyUnitToCategoryProducts("Minuman", clean)
        }
        scheduleDebouncedDriveBackupIfActive("Ubah Satuan Default Minuman")
    }

    fun applyUnitToCategoryProducts(category: String, unit: String) {
        val cleanCat = category.trim()
        val cleanUnit = unit.trim()
        if (cleanCat.isEmpty() || cleanUnit.isEmpty()) return
        viewModelScope.launch {
            repository.updateUnitByCategory(cleanCat, cleanUnit)
        }
    }

    fun setTableCount(count: Int) {
        val clamped = count.coerceIn(0, 100)
        _tableCount.value = clamped
        prefs.edit().putInt(KEY_TABLE_COUNT, clamped).apply()
        scheduleDebouncedDriveBackupIfActive("Ubah Jumlah Meja")
    }

    fun resetOperationalSettingsToDefaults() {
        _configuredCategories.value = DEFAULT_CATEGORIES
        _configuredUnits.value = DEFAULT_UNITS
        _foodDefaultUnit.value = DEFAULT_FOOD_UNIT
        _drinkDefaultUnit.value = DEFAULT_DRINK_UNIT
        _tableCount.value = DEFAULT_TABLE_COUNT
        saveStringListPref(KEY_CUSTOM_CATEGORIES, DEFAULT_CATEGORIES)
        saveStringListPref(KEY_CUSTOM_UNITS, DEFAULT_UNITS)
        prefs.edit()
            .putString(KEY_FOOD_DEFAULT_UNIT, DEFAULT_FOOD_UNIT)
            .putString(KEY_DRINK_DEFAULT_UNIT, DEFAULT_DRINK_UNIT)
            .putInt(KEY_TABLE_COUNT, DEFAULT_TABLE_COUNT)
            .apply()
        scheduleDebouncedDriveBackupIfActive("Reset Pengaturan Operasional")
    }

    // --- Shift Closings (Tutup Kasir) ---
    val allShiftClosings: StateFlow<List<com.example.data.model.ShiftClosing>> = repository.getAllShiftClosings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun closeShift(actualCash: Double, notes: String, printReceipt: Boolean): Long {
        val user = _currentUser.value ?: return -1
        val startTime = getStartOfDay(0)
        val endTime = getEndOfDay(0)

        // Calculate today's metrics
        val todayList = repository.getTransactionsBetween(startTime, endTime).first()
        val targetList = todayList.filter { it.cashierId == user.id || user.role == UserRole.ADMIN }
        val totalCount = targetList.size
        val totalCash = targetList.filter { it.paymentMethod == PaymentMethod.TUNAI }.sumOf { it.grandTotal }
        val totalNonCash = targetList.filter { it.paymentMethod != PaymentMethod.TUNAI }.sumOf { it.grandTotal }
        val totalRevenue = targetList.sumOf { it.grandTotal }

        val closing = com.example.data.model.ShiftClosing(
            cashierId = user.id,
            cashierName = user.fullName,
            totalTransactions = totalCount,
            totalCash = totalCash,
            totalNonCash = totalNonCash,
            totalRevenue = totalRevenue,
            expectedCash = totalCash,
            actualCash = actualCash,
            difference = actualCash - totalCash,
            notes = notes
        )

        val id = repository.insertShiftClosing(closing)
        val closingWithId = closing.copy(id = id)

        // Update laporan harian di Room Database
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = dateFormat.format(java.util.Date())
            repository.generateOrUpdateDailyReportForDate(todayStr, startTime, endTime)
        } catch (t: Throwable) {
            Log.w("PosViewModel", "Room daily report note: ${t.message}")
        }

        if (printReceipt) {
            printerManager.printShiftClosing(storeProfile.value, closingWithId)
        }

        // Kirim notifikasi langsung saat Tutup Toko ditekan
        com.example.service.PosNotificationHelper.notifyStoreClosed(
            context = getApplication(),
            storeName = storeProfile.value.storeName,
            totalRevenue = totalRevenue,
            totalTransactions = totalCount,
            totalCash = totalCash,
            totalNonCash = totalNonCash,
            cashierName = user.fullName,
            isAutomatic = false,
            cloudSynced = null,
            onlyAlertOnce = false
        )

        // Sinkronisasi otomatis ke Database Cloud untuk SEMUA TRANSAKSI & LAPORAN TUTUP KASIR
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            _isCloudSyncing.value = true
            _lastSyncStatus.value = "☁️ Tutup Toko: Menyinkronkan seluruh transaksi & laporan ke Database..."
            var syncCompletedOk = false
            try {
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)

                // 1. Ambil semua transaksi shift hari ini dan transaksi pending yang belum tersinkron
                val unsyncedList = try {
                    repository.getUnsyncedTransactionsList()
                } catch (t: Throwable) {
                    emptyList()
                }

                val allTrxToSync = (targetList + unsyncedList).distinctBy { it.id }
                var syncedTrxCount = 0

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
                            repository.updateSyncStatus(
                                trx.id,
                                isSynced = true,
                                syncedAt = System.currentTimeMillis(),
                                error = null
                            )
                        } else {
                            repository.updateSyncStatus(
                                trx.id,
                                isSynced = false,
                                syncedAt = null,
                                error = "Tersimpan aman di Room DB"
                            )
                        }
                    } catch (tTrx: Throwable) {
                        Log.w("PosViewModel", "Failed to sync trx ${trx.invoiceNumber}", tTrx)
                    }
                }

                val todayStart = getStartOfDay(0)
                val todayEnd = getEndOfDay(0)
                val todayExpenses = try {
                    repository.getExpensesBetweenList(todayStart, todayEnd)
                } catch (tExp: Throwable) {
                    emptyList()
                }
                val totalExpenses = todayExpenses.sumOf { it.amount }
                val expenseNotesSummary = todayExpenses.joinToString("; ") { exp ->
                    val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                    "${exp.title} [${exp.category}]: Rp ${String.format(java.util.Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
                }

                // Sinkronkan pengeluaran hari ini agar lengkap di Realtime Database & Web Laporan Chrome
                for (exp in todayExpenses) {
                    try {
                        val expRes = firebaseSyncRepository.syncExpenseDetailed(
                            exp,
                            databaseId = dbId,
                            projectId = projId,
                            hostingUrl = hostUrl
                        )
                        if (expRes.anyOk) {
                            clearExpensePending(exp.id)
                            markExpenseSynced(exp.id)
                            if (expRes.remoteOk) {
                                markExpenseRemoteSynced(exp.id)
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }

                // 2. Sinkronkan Laporan Tutup Kasir (Firebase Realtime Database /shift_reports & /daily_reports)
                val shiftSyncSuccess = firebaseSyncRepository.syncShiftClosing(
                    closing = closingWithId,
                    databaseId = dbId,
                    totalExpenses = totalExpenses,
                    expenseNotes = expenseNotesSummary,
                    projectId = projId,
                    hostingUrl = hostUrl,
                    expensesList = todayExpenses
                )

                // 3. Sinkronkan Profil Toko
                try {
                    val profile = repository.getStoreProfileSync()
                    firebaseSyncRepository.syncStoreProfile(profile, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                } catch (tProf: Throwable) {
                    // safe ignore
                }

                // 4. Kirim Arsip Laporan Harian ke Database Online (HANYA SAAT TUTUP TOKO DITEKAN)
                var dailyReportSynced = false
                try {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val todayStr = dateFormat.format(java.util.Date())
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
                        if (dailyReportSynced) repository.markDailyReportAsSynced(todayStr)
                    }
                } catch (tDaily: Throwable) {
                    Log.w("PosViewModel", "Daily report sync note: ${tDaily.message}")
                }

                syncCompletedOk = dailyReportSynced || shiftSyncSuccess

                val statusMessage = if (syncedTrxCount > 0 || shiftSyncSuccess || dailyReportSynced) {
                    "☁️ Tutup Toko Berhasil! $syncedTrxCount transaksi & laporan shift #${closingWithId.id} otomatis tersinkron ke Cloud Database!"
                } else {
                    "Laporan tutup toko & transaksi tersimpan aman offline di Room DB."
                }
                _lastSyncStatus.value = statusMessage
                Log.i("PosViewModel", statusMessage)

                // Auto backup seluruh pengaturan & transaksi ke Google Drive saat Tutup Kasir
                if (_driveAutoBackupEnabled.value && _driveBackupOnShiftClose.value && _driveConnected.value) {
                    DriveAutoBackupManager.performBackupToDrive(
                        context = getApplication(),
                        reason = "Tutup Kasir (${user.fullName})",
                        isManual = false
                    )
                }
            } catch (t: Throwable) {
                Log.e("PosViewModel", "Error in automatic shift transactions sync", t)
                _lastSyncStatus.value = "Tutup toko tersimpan lokal di Room DB (offline)"
            } finally {
                com.example.service.PosNotificationHelper.notifyStoreClosed(
                    context = getApplication(),
                    storeName = storeProfile.value.storeName,
                    totalRevenue = totalRevenue,
                    totalTransactions = totalCount,
                    totalCash = totalCash,
                    totalNonCash = totalNonCash,
                    cashierName = user.fullName,
                    isAutomatic = false,
                    cloudSynced = syncCompletedOk,
                    onlyAlertOnce = true
                )
                _isCloudSyncing.value = false
            }
        }
        return id
    }

    fun generateWhatsAppReportText(
        transactions: List<Transaction>,
        topProducts: List<TopProductSummary>,
        paymentSummaries: List<PaymentSummary>,
        totalProfit: Double
    ): String {
        val profile = storeProfile.value
        val dateLabel = getActiveReportPeriodLabel()
        val totalRevenue = transactions.sumOf { it.grandTotal }
        val totalCount = transactions.size
        val avgTicket = if (totalCount > 0) totalRevenue / totalCount else 0.0

        val sb = StringBuilder()
        sb.appendLine("📊 *LAPORAN PENJUALAN KASIR POS*")
        sb.appendLine("🏪 Toko: *${profile.storeName}*")
        sb.appendLine("📅 Periode: *${dateLabel}*")
        sb.appendLine("👤 Kasir/Admin: *${currentUser.value?.fullName ?: "Kasir"}*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("💰 *Total Omset:* ${BluetoothPrinterManager.formatRupiah(totalRevenue)}")
        sb.appendLine("📈 *Laba Bersih:* ${BluetoothPrinterManager.formatRupiah(totalProfit)}")
        sb.appendLine("🧾 *Total Transaksi:* $totalCount struk")
        sb.appendLine("🏷️ *Rata-rata/Struk:* ${BluetoothPrinterManager.formatRupiah(avgTicket)}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("💳 *Rincian Pembayaran:*")
        if (paymentSummaries.isEmpty()) {
            sb.appendLine("  (Belum ada data pembayaran)")
        } else {
            paymentSummaries.forEach { pay ->
                sb.appendLine("• *${pay.paymentMethod}:* ${BluetoothPrinterManager.formatRupiah(pay.totalAmount)} (${pay.count}x)")
            }
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🏆 *Top 5 Produk Terlaris:*")
        if (topProducts.isEmpty()) {
            sb.appendLine("  (Belum ada produk terjual)")
        } else {
            topProducts.take(5).forEachIndexed { i, p ->
                sb.appendLine("${i + 1}. *${p.productName}* (${p.totalSold} terjual - ${BluetoothPrinterManager.formatRupiah(p.totalRevenue)})")
            }
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("_Laporan otomatis dikirim dari Aplikasi Kasir POS._")
        return sb.toString()
    }

    // --- Inventory & Products ---
    val allProducts: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = combine(
        _configuredCategories,
        repository.getAllCategories()
    ) { configured, dbCategories ->
        val merged = mutableListOf<String>()
        configured.forEach { cat ->
            val trimmed = cat.trim()
            if (trimmed.isNotBlank() && merged.none { it.equals(trimmed, ignoreCase = true) }) {
                merged.add(trimmed)
            }
        }
        dbCategories.forEach { cat ->
            val trimmed = cat.trim()
            if (trimmed.isNotBlank() && merged.none { it.equals(trimmed, ignoreCase = true) }) {
                merged.add(trimmed)
            }
        }
        merged
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _configuredCategories.value)

    val lowStockProducts: StateFlow<List<Product>> = repository.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outOfStockProducts: StateFlow<List<Product>> = repository.getOutOfStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addProduct(
        name: String,
        category: String,
        barcode: String,
        buyPrice: Double,
        sellPrice: Double,
        stock: Int,
        minStock: Int,
        unit: String
    ) {
        viewModelScope.launch {
            val product = Product(
                name = name.trim(),
                category = category.trim().ifBlank { "Umum" },
                barcode = barcode.trim(),
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                minStockAlert = minStock,
                unit = unit.trim().ifBlank { "pcs" }
            )
            repository.insertProduct(product)
            checkLowStockAndNotify(forceAlert = false)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            checkLowStockAndNotify(forceAlert = false)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun adjustStock(productId: Long, newStock: Int) {
        viewModelScope.launch {
            repository.adjustStock(productId, newStock)
            checkLowStockAndNotify(forceAlert = false)
        }
    }

    // --- POS Cart & Checkout ---
    private val _cart = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    val cart: StateFlow<Map<Long, CartItem>> = _cart.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _isTaxEnabled = MutableStateFlow(false)
    val isTaxEnabled: StateFlow<Boolean> = _isTaxEnabled.asStateFlow()

    val cartItemCount: StateFlow<Int> = _cart.combine(MutableStateFlow(0)) { cartMap, _ ->
        cartMap.values.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartSubtotal: StateFlow<Double> = _cart.combine(MutableStateFlow(0.0)) { cartMap, _ ->
        cartMap.values.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addToCart(product: Product) {
        val current = _cart.value.toMutableMap()
        val existing = current[product.id]
        val currentQty = existing?.quantity ?: 0

        if (currentQty >= product.stock) {
            // Cannot exceed current stock
            return
        }

        if (existing != null) {
            current[product.id] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current[product.id] = CartItem(product = product, quantity = 1)
        }
        _cart.value = current
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        val current = _cart.value.toMutableMap()
        val item = current[productId] ?: return

        if (quantity <= 0) {
            current.remove(productId)
        } else {
            val validQty = quantity.coerceAtMost(item.product.stock)
            current[productId] = item.copy(quantity = validQty)
        }
        _cart.value = current
    }

    fun removeFromCart(productId: Long) {
        val current = _cart.value.toMutableMap()
        current.remove(productId)
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _discountAmount.value = 0.0
        _isTaxEnabled.value = false
    }

    fun setDiscount(amount: Double) {
        _discountAmount.value = amount.coerceAtLeast(0.0)
    }

    fun toggleTax(enabled: Boolean) {
        _isTaxEnabled.value = enabled
    }

    // --- Receipt Dialog State ---
    private val _receiptDialogVisible = MutableStateFlow(false)
    val receiptDialogVisible: StateFlow<Boolean> = _receiptDialogVisible.asStateFlow()

    private val _activeReceiptTransaction = MutableStateFlow<Transaction?>(null)
    val activeReceiptTransaction: StateFlow<Transaction?> = _activeReceiptTransaction.asStateFlow()

    private val _activeReceiptItems = MutableStateFlow<List<TransactionItem>>(emptyList())
    val activeReceiptItems: StateFlow<List<TransactionItem>> = _activeReceiptItems.asStateFlow()

    fun dismissReceiptDialog() {
        _receiptDialogVisible.value = false
    }

    fun showReceiptFor(transaction: Transaction) {
        viewModelScope.launch {
            val items = repository.getItemsForTransaction(transaction.id)
            _activeReceiptTransaction.value = transaction
            _activeReceiptItems.value = items
            _receiptDialogVisible.value = true
        }
    }

    fun processPayment(
        paymentMethod: PaymentMethod,
        amountPaid: Double,
        customerName: String,
        orderType: com.example.data.model.OrderType = com.example.data.model.OrderType.DINE_IN,
        tableNumber: String = "",
        notes: String = ""
    ) {
        val cartMap = _cart.value
        if (cartMap.isEmpty()) return

        val user = _currentUser.value ?: return
        val subtotal = cartMap.values.sumOf { it.subtotal }
        val discount = _discountAmount.value.coerceAtMost(subtotal)
        val afterDiscount = subtotal - discount
        val tax = if (_isTaxEnabled.value) afterDiscount * 0.11 else 0.0
        val grandTotal = afterDiscount + tax

        val change = (amountPaid - grandTotal).coerceAtLeast(0.0)

        val transaction = Transaction(
            invoiceNumber = PosRepository.generateInvoiceNumber(),
            cashierId = user.id,
            cashierName = user.fullName,
            timestamp = System.currentTimeMillis(),
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            amountPaid = amountPaid,
            changeAmount = change,
            customerName = customerName.ifBlank { "Pelanggan Umum" },
            orderType = orderType,
            tableNumber = tableNumber.trim(),
            notes = notes
        )

        val transactionItems = cartMap.values.map { cartItem ->
            TransactionItem(
                transactionId = 0,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                unitPrice = cartItem.product.sellPrice,
                costPrice = cartItem.product.buyPrice,
                quantity = cartItem.quantity,
                subtotal = cartItem.subtotal,
                itemNotes = cartItem.notes
            )
        }

        viewModelScope.launch {
            val newTrxId = repository.processCheckout(transaction, transactionItems)
            val completedTrx = transaction.copy(id = newTrxId)

            _activeReceiptTransaction.value = completedTrx
            _activeReceiptItems.value = transactionItems.map { it.copy(transactionId = newTrxId) }
            _receiptDialogVisible.value = true

            // Reset cart
            clearCart()

            // Cek dan kirim push notifikasi jika stok makanan/minuman menipis atau habis
            checkLowStockAndNotify(forceAlert = false)

            // Automatic Sync to Cloud Database & Web Chrome Relay (asynchronous in background on IO)
            val itemsForSync = transactionItems.map { it.copy(transactionId = newTrxId) }
            val dbId = firebaseDatabaseId.value
            val projId = firebaseProjectId.value
            val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val syncSuccess = firebaseSyncRepository.syncTransaction(
                        completedTrx,
                        itemsForSync,
                        databaseId = dbId,
                        projectId = projId,
                        hostingUrl = hostUrl
                    )
                    if (syncSuccess) {
                        repository.updateSyncStatus(newTrxId, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                        _lastSyncStatus.value = "⚡ Transaksi ${completedTrx.invoiceNumber} otomatis tersinkron ke Cloud!"
                    } else {
                        repository.updateSyncStatus(newTrxId, isSynced = false, syncedAt = null, error = "Tersimpan offline di Room DB")
                        _lastSyncStatus.value = "Tersimpan offline di Room DB (menunggu sinkron)"
                    }
                    syncPendingExpensesInternal()
                } catch (e: Exception) {
                    repository.updateSyncStatus(newTrxId, isSynced = false, syncedAt = null, error = e.localizedMessage)
                    _lastSyncStatus.value = "Tersimpan offline di Room DB (menunggu sinkron)"
                }
                scheduleDebouncedDriveBackupIfActive("Transaksi ${completedTrx.invoiceNumber}")
            }
        }
    }

    // --- Real-time Daily Reports ---
    private val _selectedDateFilter = MutableStateFlow(DateFilter.TODAY)
    val selectedDateFilter: StateFlow<DateFilter> = _selectedDateFilter.asStateFlow()

    private val _selectedCustomDateMillis = MutableStateFlow(getStartOfDay(0))
    val selectedCustomDateMillis: StateFlow<Long> = _selectedCustomDateMillis.asStateFlow()

    val activeReportPeriodLabel: StateFlow<String> = combine(_selectedDateFilter, _selectedCustomDateMillis) { filter, customMillis ->
        formatPeriodLabel(filter, customMillis)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        formatPeriodLabel(DateFilter.TODAY, getStartOfDay(0))
    )

    fun setDateFilter(filter: DateFilter) {
        _selectedDateFilter.value = filter
        when (filter) {
            DateFilter.TODAY -> _selectedCustomDateMillis.value = getStartOfDay(0)
            DateFilter.YESTERDAY -> _selectedCustomDateMillis.value = getStartOfDay(-1)
            else -> Unit
        }
        refreshReportData()
    }

    fun setCustomDate(dateMillis: Long, isUtcPickerMillis: Boolean = true) {
        val rawLocalStart = if (isUtcPickerMillis) {
            normalizeUtcDatePickerMillisToLocalStartOfDay(dateMillis)
        } else {
            getStartOfDayForMillis(dateMillis)
        }
        val todayStart = getStartOfDay(0)
        val yesterdayStart = getStartOfDay(-1)
        val clampedStart = if (rawLocalStart > todayStart) todayStart else rawLocalStart

        _selectedCustomDateMillis.value = clampedStart
        _selectedDateFilter.value = when (clampedStart) {
            todayStart -> DateFilter.TODAY
            yesterdayStart -> DateFilter.YESTERDAY
            else -> DateFilter.CUSTOM_DATE
        }
        refreshReportData()
    }

    fun stepSelectedDate(dayDelta: Int) {
        val baseMillis = when (_selectedDateFilter.value) {
            DateFilter.TODAY -> getStartOfDay(0)
            DateFilter.YESTERDAY -> getStartOfDay(-1)
            DateFilter.CUSTOM_DATE -> _selectedCustomDateMillis.value
            else -> _selectedCustomDateMillis.value
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_YEAR, dayDelta)
        }
        setCustomDate(cal.timeInMillis, isUtcPickerMillis = false)
    }

    fun getActiveReportPeriodLabel(): String {
        return formatPeriodLabel(_selectedDateFilter.value, _selectedCustomDateMillis.value)
    }

    private val _reportStartTime = MutableStateFlow(getStartOfDay(0))
    private val _reportEndTime = MutableStateFlow(getEndOfDay(0))

    val allTransactionsHistory: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTransactionsForPeriod(
        startTime: Long,
        endTime: Long,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                repository.deleteTransactionsBetween(startTime, endTime)
                withContext(Dispatchers.Main) { onResult?.invoke(true) }
            } catch (t: Throwable) {
                Log.w("PosViewModel", "Failed deleting transactions for period: ${t.message}")
                withContext(Dispatchers.Main) { onResult?.invoke(false) }
            }
        }
    }

    fun deleteSingleTransaction(
        transactionId: Long,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                repository.deleteTransactionById(transactionId)
                withContext(Dispatchers.Main) { onResult?.invoke(true) }
            } catch (t: Throwable) {
                Log.w("PosViewModel", "Failed deleting single transaction: ${t.message}")
                withContext(Dispatchers.Main) { onResult?.invoke(false) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportTransactions: StateFlow<List<Transaction>> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getTransactionsBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val topSellingProducts: StateFlow<List<TopProductSummary>> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getTopSellingProducts(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val paymentMethodStats: StateFlow<List<PaymentSummary>> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getPaymentMethodBreakdown(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalProfitFlow: StateFlow<Double> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getTotalProfitBetween(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val expensesFlow: StateFlow<List<com.example.data.model.Expense>> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getExpensesBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalExpensesFlow: StateFlow<Double> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getTotalExpensesBetween(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val grossProfitFlow: StateFlow<Double> = combine(_reportStartTime, _reportEndTime) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getTotalProfitBetween(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netProfitFlow: StateFlow<Double> = combine(grossProfitFlow, totalExpensesFlow) { gross, expenses ->
        gross - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allExpensesFlow: StateFlow<List<com.example.data.model.Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExpenseSyncing = MutableStateFlow(false)
    val isExpenseSyncing: StateFlow<Boolean> = _isExpenseSyncing.asStateFlow()

    private val _expenseSyncStatus = MutableStateFlow("Otomatis tersinkron ke Laporan Web Chrome")
    val expenseSyncStatus: StateFlow<String> = _expenseSyncStatus.asStateFlow()

    private val isTrxExpenseAutoSyncRunning = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile
    private var lastTrxExpenseAutoSyncAtMs: Long = 0L

    @Synchronized
    private fun getPendingExpenseIds(): MutableSet<Long> {
        val raw = prefs.getString("KEY_PENDING_EXPENSE_IDS", "") ?: ""
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
            .toMutableSet()
    }

    @Synchronized
    private fun markExpensePending(expenseId: Long) {
        if (expenseId <= 0L) return
        val ids = getPendingExpenseIds()
        ids.add(expenseId)
        prefs.edit().putString("KEY_PENDING_EXPENSE_IDS", ids.joinToString(",")).apply()
    }

    @Synchronized
    private fun clearExpensePending(expenseId: Long) {
        if (expenseId <= 0L) return
        val ids = getPendingExpenseIds()
        if (ids.remove(expenseId)) {
            prefs.edit().putString("KEY_PENDING_EXPENSE_IDS", ids.joinToString(",")).apply()
        }
    }

    @Synchronized
    private fun getSyncedExpenseIds(): MutableSet<Long> {
        val raw = prefs.getString("KEY_SYNCED_EXPENSE_IDS", "") ?: ""
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
            .toMutableSet()
    }

    @Synchronized
    private fun markExpenseSynced(expenseId: Long) {
        if (expenseId <= 0L) return
        val ids = getSyncedExpenseIds()
        if (ids.add(expenseId)) {
            prefs.edit().putString("KEY_SYNCED_EXPENSE_IDS", ids.joinToString(",")).apply()
        }
    }

    @Synchronized
    private fun unmarkExpenseSynced(expenseId: Long) {
        if (expenseId <= 0L) return
        val ids = getSyncedExpenseIds()
        if (ids.remove(expenseId)) {
            prefs.edit().putString("KEY_SYNCED_EXPENSE_IDS", ids.joinToString(",")).apply()
        }
    }

    @Synchronized
    private fun getSyncedRemoteExpenseIds(): MutableSet<Long> {
        val raw = prefs.getString("KEY_SYNCED_REMOTE_EXPENSE_IDS", "") ?: ""
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
            .toMutableSet()
    }

    @Synchronized
    private fun markExpenseRemoteSynced(expenseId: Long) {
        if (expenseId <= 0L) return
        val ids = getSyncedRemoteExpenseIds()
        if (ids.add(expenseId)) {
            prefs.edit().putString("KEY_SYNCED_REMOTE_EXPENSE_IDS", ids.joinToString(",")).apply()
        }
    }

    @Synchronized
    private fun unmarkExpenseRemoteSynced(expenseId: Long) {
        if (expenseId <= 0L) return
        val ids = getSyncedRemoteExpenseIds()
        if (ids.remove(expenseId)) {
            prefs.edit().putString("KEY_SYNCED_REMOTE_EXPENSE_IDS", ids.joinToString(",")).apply()
        }
    }

    suspend fun syncPendingExpensesInternal(): Pair<Int, Int> {
        val allList = try {
            repository.getAllExpenses().first()
        } catch (_: Throwable) {
            emptyList()
        }
        val pendingIds = getPendingExpenseIds()
        val syncedIds = getSyncedExpenseIds()
        val remoteSyncedIds = getSyncedRemoteExpenseIds()
        val existingIds = allList.map { it.id }.toSet()
        for (orphanId in pendingIds.filter { it !in existingIds }) {
            clearExpensePending(orphanId)
        }
        val toSync = allList.filter { it.id in pendingIds || it.id !in syncedIds || it.id !in remoteSyncedIds }
        if (toSync.isEmpty()) return 0 to 0

        com.example.service.LocalWebReportServer.start(getApplication(), database)
        val dbId = firebaseDatabaseId.value
        val projId = firebaseProjectId.value
        val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
        var okCount = 0
        var remoteOkCount = 0
        var lastRtdbHint: String? = null
        for (exp in toSync) {
            try {
                val res = firebaseSyncRepository.syncExpenseDetailed(
                    exp,
                    databaseId = dbId,
                    projectId = projId,
                    hostingUrl = hostUrl
                )
                if (res.anyOk) {
                    clearExpensePending(exp.id)
                    markExpenseSynced(exp.id)
                    if (res.remoteOk) {
                        markExpenseRemoteSynced(exp.id)
                        remoteOkCount++
                    } else if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                        lastRtdbHint = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                    }
                    okCount++
                } else if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                    lastRtdbHint = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                }
            } catch (_: Throwable) {
            }
        }
        if (remoteOkCount > 0) {
            _expenseSyncStatus.value = "✓ $remoteOkCount pengeluaran tersinkron ke Realtime Database & Web Chrome"
        } else if (okCount > 0) {
            val hint = lastRtdbHint?.let { " ($it)" } ?: ""
            _expenseSyncStatus.value = "Relay lokal aktif ($okCount) • RTDB belum terjangkau$hint"
        }
        return okCount to (toSync.size - okCount)
    }

    /**
     * Dipanggil otomatis ketika pengguna membuka bagian Laporan Transaksi / Laporan Pengeluaran
     * agar semua catatan pengeluaran di Laporan Transaksi selalu tersinkron langsung ke Realtime Database & Web Online Chrome.
     */
    fun syncExpensesForTransactionReportAuto(
        forceAll: Boolean = false,
        onComplete: ((Int, Int) -> Unit)? = null
    ) {
        if (!isTrxExpenseAutoSyncRunning.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                com.example.service.LocalWebReportServer.start(getApplication(), database)
                val allList = try {
                    repository.getAllExpenses().first()
                } catch (_: Throwable) {
                    allExpensesFlow.value
                }
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
                val nowMs = System.currentTimeMillis()
                val recentlySyncedAll = (nowMs - lastTrxExpenseAutoSyncAtMs) < 3000L

                if (forceAll && !recentlySyncedAll) {
                    try {
                        firebaseSyncRepository.reconcileExpensesWithRtdb(
                            localExpenses = allList,
                            databaseId = dbId,
                            projectId = projId,
                            hostingUrl = hostUrl
                        ) { _, parsed ->
                            val newId = repository.insertExpense(parsed)
                            markExpenseSynced(newId)
                            clearExpensePending(newId)
                            parsed.copy(id = newId)
                        }
                    } catch (_: Throwable) {
                    }
                }

                val refreshedList = try {
                    repository.getAllExpenses().first()
                } catch (_: Throwable) {
                    allList
                }
                val pendingIds = getPendingExpenseIds()
                val syncedIds = getSyncedExpenseIds()
                val remoteSyncedIds = getSyncedRemoteExpenseIds()
                val unsyncedList = refreshedList.filter { it.id in pendingIds || it.id !in syncedIds || it.id !in remoteSyncedIds }

                val targets = when {
                    refreshedList.isEmpty() -> emptyList()
                    unsyncedList.isNotEmpty() -> (unsyncedList + refreshedList.take(25)).distinctBy { it.id }
                    forceAll && !recentlySyncedAll -> refreshedList.take(100)
                    else -> emptyList()
                }

                if (targets.isEmpty()) {
                    if (refreshedList.isEmpty()) {
                        _expenseSyncStatus.value = "Auto-Sync RTDB & Web Chrome aktif • Belum ada pengeluaran"
                    } else if (pendingIds.isEmpty() && refreshedList.all { it.id in remoteSyncedIds }) {
                        _expenseSyncStatus.value = "✓ ${refreshedList.size} pengeluaran tersinkron ke Realtime Database & Web Chrome"
                    } else if (pendingIds.isEmpty()) {
                        _expenseSyncStatus.value = "Tersimpan di Relay lokal (${refreshedList.size}) • menunggu koneksi RTDB"
                    }
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(refreshedList.size, refreshedList.size)
                    }
                    return@launch
                }

                _isExpenseSyncing.value = true
                _expenseSyncStatus.value = "Menyinkronkan otomatis ${targets.size} pengeluaran ke Realtime Database & Web Chrome..."
                var ok = 0
                var rtdbOkCount = 0
                var lastRtdbHint: String? = null
                for (exp in targets) {
                    try {
                        val res = firebaseSyncRepository.syncExpenseDetailed(
                            exp,
                            databaseId = dbId,
                            projectId = projId,
                            hostingUrl = hostUrl
                        )
                        if (res.anyOk) {
                            clearExpensePending(exp.id)
                            markExpenseSynced(exp.id)
                            if (res.remoteOk) {
                                markExpenseRemoteSynced(exp.id)
                                rtdbOkCount++
                            } else if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                                lastRtdbHint = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                            }
                            ok++
                        } else {
                            markExpensePending(exp.id)
                            if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                                lastRtdbHint = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                            }
                        }
                    } catch (_: Throwable) {
                        markExpensePending(exp.id)
                    }
                }
                // Sekaligus sinkronkan transaksi pending agar laporan Laba Kotor & Bersih di Chrome konsisten
                try {
                    syncPendingTransactionsInternal()
                } catch (_: Throwable) {
                }

                lastTrxExpenseAutoSyncAtMs = System.currentTimeMillis()
                _expenseSyncStatus.value = when {
                    rtdbOkCount == targets.size && allList.isNotEmpty() ->
                        "✓ ${allList.size} pengeluaran tersinkron ke Realtime Database & Web Chrome"
                    rtdbOkCount > 0 ->
                        "✓ $rtdbOkCount/${targets.size} tersinkron ke Realtime Database"
                    ok > 0 -> {
                        val hint = lastRtdbHint?.let { " ($it)" } ?: ""
                        "Relay lokal aktif ($ok/${targets.size}) • RTDB gagal/offline$hint"
                    }
                    else -> {
                        val hint = lastRtdbHint?.let { " ($it)" } ?: ""
                        "Belum terkirim ke RTDB$hint — otomatis coba ulang"
                    }
                }
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(if (rtdbOkCount > 0) rtdbOkCount else ok, targets.size)
                }
            } finally {
                _isExpenseSyncing.value = false
                isTrxExpenseAutoSyncRunning.set(false)
            }
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String = "Operasional",
        notes: String = "",
        timestampMillis: Long = System.currentTimeMillis(),
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            com.example.service.LocalWebReportServer.start(getApplication(), database)
            val user = _currentUser.value?.fullName ?: "Admin"
            val newExpense = com.example.data.model.Expense(
                title = title.trim(),
                amount = amount,
                category = category.trim().ifBlank { "Operasional" },
                timestamp = timestampMillis,
                notes = notes.trim(),
                createdBy = user
            )
            val insertedId = repository.insertExpense(newExpense)
            markExpensePending(insertedId)
            val savedExpense = newExpense.copy(id = insertedId)
            _isExpenseSyncing.value = true
            _expenseSyncStatus.value = "Menyinkronkan '${savedExpense.title}' ke Realtime Database & Web Chrome..."
            val res = try {
                firebaseSyncRepository.syncExpenseDetailed(
                    savedExpense,
                    databaseId = firebaseDatabaseId.value,
                    projectId = firebaseProjectId.value,
                    hostingUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
                )
            } catch (t: Throwable) {
                Log.w("PosViewModel", "Expense sync note: ${t.message}")
                FirebaseSyncRepository.ExpenseSyncResult(
                    relayOk = false,
                    rtdbOk = false,
                    isLoopbackRelay = true,
                    rtdbError = t.message
                )
            }
            val synced = res.anyOk
            if (synced) {
                clearExpensePending(insertedId)
                markExpenseSynced(insertedId)
                if (res.remoteOk) {
                    markExpenseRemoteSynced(insertedId)
                }
            }
            _isExpenseSyncing.value = false
            val rtdbHint = res.rtdbHttpCode?.let { " (HTTP $it)" }
                ?: res.rtdbError?.take(48)?.let { " ($it)" }
                ?: ""
            val msg = when {
                res.rtdbOk && res.relayOk ->
                    "✓ Tersinkron ke Realtime Database & Web Chrome (${savedExpense.title})"
                res.rtdbOk ->
                    "✓ Tersinkron ke Realtime Database (${savedExpense.title})"
                res.relayOk ->
                    "Relay lokal tersimpan • RTDB gagal/offline$rtdbHint (${savedExpense.title})"
                else ->
                    "Tersimpan lokal — otomatis kirim ulang ke RTDB$rtdbHint"
            }
            _expenseSyncStatus.value = msg
            if (res.rtdbOk) {
                _lastSyncStatus.value = "Pengeluaran '${savedExpense.title}' tersinkron ke Realtime Database & Web Chrome"
            } else if (synced) {
                _lastSyncStatus.value = "Pengeluaran '${savedExpense.title}' tersimpan di Relay lokal (menunggu RTDB)"
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(synced, msg)
            }
        }
    }

    fun deleteExpense(
        expense: com.example.data.model.Expense,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            com.example.service.LocalWebReportServer.start(getApplication(), database)
            repository.deleteExpense(expense)
            clearExpensePending(expense.id)
            unmarkExpenseSynced(expense.id)
            unmarkExpenseRemoteSynced(expense.id)
            val remaining = try {
                repository.getAllExpenses().first()
            } catch (_: Throwable) {
                emptyList()
            }
            val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val todayStr = dateFmt.format(java.util.Date())
            val remainingTodayTotal = remaining
                .filter { dateFmt.format(java.util.Date(it.timestamp)) == todayStr }
                .sumOf { it.amount }
            val latestRemaining = remaining.maxByOrNull { it.timestamp }
            _isExpenseSyncing.value = true
            val deleted = try {
                firebaseSyncRepository.deleteExpenseFromRtdb(
                    expense.id,
                    databaseId = firebaseDatabaseId.value,
                    projectId = firebaseProjectId.value,
                    hostingUrl = resolveEffectiveHostingUrl(webDashboardUrl.value),
                    timestampMillis = expense.timestamp,
                    latestRemainingExpense = latestRemaining,
                    remainingTodayTotal = remainingTodayTotal,
                    todayDateStr = todayStr
                )
            } catch (t: Throwable) {
                Log.w("PosViewModel", "Expense delete sync note: ${t.message}")
                false
            }
            _isExpenseSyncing.value = false
            _expenseSyncStatus.value = if (deleted) {
                "Catatan '${expense.title}' dihapus dari Realtime Database & Web Chrome"
            } else {
                "Catatan '${expense.title}' dihapus lokal"
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(deleted)
            }
        }
    }

    fun syncAllExpensesToCloud(onResult: ((Int, Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            com.example.service.LocalWebReportServer.start(getApplication(), database)
            val initialList = try {
                repository.getAllExpenses().first()
            } catch (_: Throwable) {
                allExpensesFlow.value
            }
            _isExpenseSyncing.value = true
            _expenseSyncStatus.value = "Menyinkronkan & mencocokkan catatan pengeluaran ke Realtime Database & Web Chrome..."
            val dbId = firebaseDatabaseId.value
            val projId = firebaseProjectId.value
            val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)

            try {
                firebaseSyncRepository.reconcileExpensesWithRtdb(
                    localExpenses = initialList,
                    databaseId = dbId,
                    projectId = projId,
                    hostingUrl = hostUrl
                ) { _, parsed ->
                    val newId = repository.insertExpense(parsed)
                    markExpenseSynced(newId)
                    clearExpensePending(newId)
                    parsed.copy(id = newId)
                }
            } catch (_: Throwable) {
            }

            val allList = try {
                repository.getAllExpenses().first()
            } catch (_: Throwable) {
                initialList
            }
            if (allList.isEmpty()) {
                _isExpenseSyncing.value = false
                _expenseSyncStatus.value = "✓ Pengeluaran disinkronkan (0 catatan aktif di aplikasi & RTDB)"
                withContext(Dispatchers.Main) {
                    onResult?.invoke(0, 0)
                }
                return@launch
            }
            var ok = 0
            var rtdbOkCount = 0
            var lastRtdbHint: String? = null
            for (exp in allList) {
                try {
                    val res = firebaseSyncRepository.syncExpenseDetailed(
                        exp,
                        databaseId = dbId,
                        projectId = projId,
                        hostingUrl = hostUrl
                    )
                    if (res.anyOk) {
                        clearExpensePending(exp.id)
                        markExpenseSynced(exp.id)
                        if (res.remoteOk) {
                            markExpenseRemoteSynced(exp.id)
                            rtdbOkCount++
                        } else if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                            lastRtdbHint = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                        }
                        ok++
                    } else {
                        markExpensePending(exp.id)
                        if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                            lastRtdbHint = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                        }
                    }
                } catch (_: Throwable) {
                    markExpensePending(exp.id)
                }
            }
            val total = allList.size
            lastTrxExpenseAutoSyncAtMs = System.currentTimeMillis()
            _isExpenseSyncing.value = false
            _expenseSyncStatus.value = when {
                rtdbOkCount == total ->
                    "✓ $rtdbOkCount/$total catatan tersinkron ke Realtime Database & Web Chrome"
                rtdbOkCount > 0 ->
                    "✓ $rtdbOkCount/$total tersinkron ke Realtime Database"
                ok > 0 -> {
                    val hint = lastRtdbHint?.let { " ($it)" } ?: ""
                    "Relay lokal aktif ($ok/$total) • RTDB gagal/offline$hint"
                }
                else -> {
                    val hint = lastRtdbHint?.let { " ($it)" } ?: ""
                    "0/$total gagal kirim ke RTDB$hint (otomatis coba ulang)"
                }
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(if (rtdbOkCount > 0) rtdbOkCount else ok, total)
            }
        }
    }

    fun printProfitExpenseReceipt(
        grossProfit: Double,
        expenses: Double,
        netProfit: Double,
        grossSales: Double,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val profile = storeProfile.value
            val label = getActiveReportPeriodLabel()
            val cashier = currentUser.value?.fullName ?: "Admin"
            val success = printerManager.printProfitExpenseReceipt(
                profile = profile,
                grossProfit = grossProfit,
                expenses = expenses,
                netProfit = netProfit,
                grossSales = grossSales,
                periodLabel = label,
                cashierName = cashier
            )
            withContext(Dispatchers.Main) {
                onResult?.invoke(success, printerManager.printStatus.value ?: "Selesai")
            }
        }
    }

    fun generateWhatsAppProfitExpenseText(
        grossProfit: Double,
        expenses: Double,
        netProfit: Double,
        grossSales: Double
    ): String {
        val profile = storeProfile.value
        val dateLabel = getActiveReportPeriodLabel()
        val cashier = currentUser.value?.fullName ?: "Admin"
        val sb = StringBuilder()
        sb.appendLine("📊 *LAPORAN KEUANGAN TOKO*")
        sb.appendLine("🏪 *${profile.storeName}*")
        sb.appendLine("📅 *Periode:* $dateLabel")
        sb.appendLine("👤 *Petugas:* $cashier")
        sb.appendLine("=".repeat(28))
        sb.appendLine("💰 *Penjualan Kotor (Omset):* ${BluetoothPrinterManager.formatRupiah(grossSales)}")
        sb.appendLine("📦 *LABA KOTOR:* ${BluetoothPrinterManager.formatRupiah(grossProfit)}")
        sb.appendLine("💸 *PENGELUARAN:* ${BluetoothPrinterManager.formatRupiah(expenses)}")
        sb.appendLine("-".repeat(28))
        sb.appendLine("📈 *LABA BERSIH:* ${BluetoothPrinterManager.formatRupiah(netProfit)}")
        sb.appendLine("=".repeat(28))
        sb.appendLine("🗄️ *Penyimpanan Database Arsip Laporan:* Tersimpan di Cloud Hosting")
        sb.appendLine("🌐 _Laporan otomatis terverifikasi sistem POS_")
        return sb.toString()
    }

    fun refreshReportData() {
        when (_selectedDateFilter.value) {
            DateFilter.TODAY -> {
                _reportStartTime.value = getStartOfDay(0)
                _reportEndTime.value = getEndOfDay(0)
            }
            DateFilter.YESTERDAY -> {
                _reportStartTime.value = getStartOfDay(-1)
                _reportEndTime.value = getEndOfDay(-1)
            }
            DateFilter.CUSTOM_DATE -> {
                val selectedMillis = _selectedCustomDateMillis.value
                _reportStartTime.value = getStartOfDayForMillis(selectedMillis)
                _reportEndTime.value = getEndOfDayForMillis(selectedMillis)
            }
            DateFilter.LAST_7_DAYS -> {
                _reportStartTime.value = getStartOfDay(-6)
                _reportEndTime.value = getEndOfDay(0)
            }
            DateFilter.THIS_MONTH -> {
                _reportStartTime.value = getStartOfMonth()
                _reportEndTime.value = getEndOfDay(0)
            }
            DateFilter.ALL -> {
                _reportStartTime.value = 0L
                _reportEndTime.value = Long.MAX_VALUE
            }
        }
    }

    /**
     * Memeriksa pergantian tanggal otomatis.
     * Jika terjadi perubahan hari/tanggal:
     * 1. Mengarsipkan laporan hari sebelumnya ke Room DB & Realtime Database (/daily_reports/$date).
     * 2. Mereset statistik hari ini ke 0 (Rp 0, 0 struk) di aplikasi dan RTDB (realtime_stats/today).
     * 3. Menyimpan arsip permanen setiap harinya.
     */
    fun checkDateRolloverAndAutoArchive() {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val currentDateStr = dateFormat.format(java.util.Date())
            val lastDateStr = prefs.getString("KEY_LAST_ACTIVE_DATE", "") ?: ""

            if (lastDateStr.isNotBlank() && lastDateStr != currentDateStr) {
                Log.d("PosViewModel", "Pergantian tanggal terdeteksi: $lastDateStr -> $currentDateStr. Mereset laporan hari ini ke Rp 0 (arsip database dikirim saat tekan Tutup Toko).")

                // 1. Reset statistik live RTDB hari ini ke 0 (tanpa mengirim arsip /daily_reports otomatis)
                try {
                    val targetRtdb = if (!firebaseDatabaseId.value.isNullOrBlank() && firebaseDatabaseId.value != "(default)" && !firebaseDatabaseId.value.contains("ai-studio")) {
                        firebaseDatabaseId.value
                    } else {
                        "foodrock-report-default-rtdb"
                    }
                    val isoStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date())
                    val resetStats = org.json.JSONObject(mapOf(
                        "todayRevenue" to 0,
                        "todayTransactionsCount" to 0,
                        "lastTransactionAmount" to 0,
                        "lastInvoiceNumber" to "-",
                        "lastCustomerName" to "-",
                        "lastPaymentMethod" to "-",
                        "lastCashierName" to "-",
                        "lastItemsSummary" to "Belum ada transaksi hari ini",
                        "lastTotalItemsCount" to 0,
                        "lastUpdated" to isoStr,
                        "date" to currentDateStr,
                        "storeId" to storeProfile.value.storeName
                    )).toString()
                    firebaseSyncRepository.syncToRealtimeDatabase(targetRtdb, "realtime_stats/today", resetStats)
                } catch (e: Throwable) {
                    Log.w("PosViewModel", "Catatan reset live stats RTDB: ${e.message}")
                }

                // 2. Reset rentang waktu laporan hari ini di aplikasi agar kembali ke 0
                withContext(Dispatchers.Main) {
                    _selectedDateFilter.value = DateFilter.TODAY
                    _selectedCustomDateMillis.value = getStartOfDay(0)
                    refreshReportData()
                }
            }

            prefs.edit().putString("KEY_LAST_ACTIVE_DATE", currentDateStr).apply()
        }
    }

    // --- Room Database Laporan Harian (Daily Reports Archive) ---
    val allDailyReports: StateFlow<List<DailyReport>> = repository.getAllDailyReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    fun generateOrRefreshTodayReport(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            _isGeneratingReport.value = true
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val todayStr = dateFormat.format(java.util.Date())
                repository.generateOrUpdateDailyReportForDate(
                    dateStr = todayStr,
                    startTime = getStartOfDay(0),
                    endTime = getEndOfDay(0)
                )
                withContext(Dispatchers.Main) {
                    onResult?.invoke(true, "Laporan hari ini ($todayStr) tersimpan lokal di Room DB. Tekan Tutup Toko untuk mengirim arsip ke Database Online.")
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Gagal mengarsipkan laporan: ${t.localizedMessage}")
                }
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun deleteDailyReport(report: DailyReport, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                repository.deleteDailyReportById(report.id)
                withContext(Dispatchers.Main) { onResult?.invoke(true) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) { onResult?.invoke(false) }
            }
        }
    }

    // --- Bluetooth Thermal Printer Actions ---
    private val _pairedDevicesList = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val pairedDevicesList: StateFlow<List<PrinterDevice>> = _pairedDevicesList.asStateFlow()

    fun loadPairedDevices() {
        _pairedDevicesList.value = printerManager.getPairedDevices()
    }

    fun connectPrinter(address: String) {
        viewModelScope.launch {
            val success = printerManager.connectToDevice(address)
            loadPairedDevices()
        }
    }

    fun disconnectPrinter() {
        printerManager.disconnect()
        loadPairedDevices()
    }

    fun printCurrentActiveReceipt() {
        val trx = _activeReceiptTransaction.value ?: return
        val items = _activeReceiptItems.value
        val profile = storeProfile.value

        viewModelScope.launch {
            printerManager.printReceipt(profile, trx, items)
        }
    }

    fun printTestReceipt() {
        val profile = storeProfile.value
        viewModelScope.launch {
            printerManager.printTestPage(profile)
        }
    }

    fun printDailyReport(report: DailyReport) {
        val profile = storeProfile.value
        viewModelScope.launch {
            printerManager.printDailyReport(profile, report)
        }
    }

    fun printOmsetReceipt(totalRevenue: Double, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val profile = storeProfile.value
            val label = getActiveReportPeriodLabel()
            val cashier = currentUser.value?.fullName ?: "Admin"
            val success = printerManager.printOmsetReceipt(profile, totalRevenue, label, cashier)
            withContext(Dispatchers.Main) {
                onResult?.invoke(success, printerManager.printStatus.value ?: "Selesai")
            }
        }
    }

    fun testCloudConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
            _isCloudSyncing.value = true
            _lastSyncStatus.value = "Sedang memeriksa koneksi server Cloud..."
            try {
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
                val (connected, message) = withContext(Dispatchers.IO) {
                    firebaseSyncRepository.checkCloudConnection(databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                }
                _lastSyncStatus.value = message
                try {
                    onResult(connected, message)
                } catch (t: Throwable) {
                    Log.e("PosViewModel", "onResult error", t)
                }
            } catch (t: Throwable) {
                val err = "Koneksi offline: ${t.localizedMessage ?: "Jaringan terputus"}"
                _lastSyncStatus.value = err
                try {
                    onResult(false, err)
                } catch (t2: Throwable) {
                    Log.e("PosViewModel", "onResult error", t2)
                }
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun syncAllToCloud(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _isCloudSyncing.value = true
            _lastSyncStatus.value = "Sedang menyinkronkan data langsung ke Web Chrome..."
            try {
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
                val profile = repository.getStoreProfileSync()
                try {
                    firebaseSyncRepository.syncStoreProfile(profile, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                } catch (t: Throwable) {
                    Log.w("PosViewModel", "syncStoreProfile note", t)
                }

                val transactions = try {
                    repository.getAllTransactions().first().take(25)
                } catch (t: Throwable) {
                    emptyList()
                }
                var trxCount = 0
                for (t in transactions) {
                    try {
                        val items = repository.getItemsForTransaction(t.id)
                        val success = firebaseSyncRepository.syncTransaction(t, items, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                        if (success) {
                            trxCount++
                            repository.updateSyncStatus(t.id, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                        } else {
                            repository.updateSyncStatus(t.id, isSynced = false, syncedAt = null, error = "Tersimpan offline di Room DB")
                        }
                    } catch (tItem: Throwable) {
                        repository.updateSyncStatus(t.id, isSynced = false, syncedAt = null, error = tItem.localizedMessage)
                    }
                }

                val recentExpenses = try {
                    repository.getAllExpenses().first().take(100)
                } catch (tExp: Throwable) {
                    emptyList()
                }
                val dateFmtUs = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val expensesByDate = recentExpenses.groupBy { dateFmtUs.format(java.util.Date(it.timestamp)) }

                val closings = try {
                    repository.getAllShiftClosings().first().take(10)
                } catch (t: Throwable) {
                    emptyList()
                }
                var shiftCount = 0
                for (c in closings) {
                    try {
                        val cDateStr = dateFmtUs.format(java.util.Date(c.closingTime))
                        val dayExps = expensesByDate[cDateStr].orEmpty()
                        val totalExp = dayExps.sumOf { it.amount }
                        val expNotes = dayExps.joinToString("; ") { exp ->
                            val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                            "${exp.title} [${exp.category}]: Rp ${String.format(java.util.Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
                        }
                        val success = firebaseSyncRepository.syncShiftClosing(
                            closing = c,
                            databaseId = dbId,
                            totalExpenses = totalExp,
                            expenseNotes = expNotes,
                            projectId = projId,
                            hostingUrl = hostUrl,
                            expensesList = dayExps
                        )
                        if (success) shiftCount++
                    } catch (tShift: Throwable) {
                        Log.w("PosViewModel", "syncShiftClosing note", tShift)
                    }
                }

                try {
                    firebaseSyncRepository.reconcileExpensesWithRtdb(
                        localExpenses = recentExpenses,
                        databaseId = dbId,
                        projectId = projId,
                        hostingUrl = hostUrl
                    ) { _, parsed ->
                        val newId = repository.insertExpense(parsed)
                        markExpenseSynced(newId)
                        clearExpensePending(newId)
                        parsed.copy(id = newId)
                    }
                } catch (_: Throwable) {
                }

                val syncedExpensesList = try {
                    database.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
                } catch (_: Throwable) {
                    recentExpenses
                }

                var expRelayCount = 0
                var expRemoteCount = 0
                var lastExpErr: String? = null
                for (exp in syncedExpensesList) {
                    try {
                        val res = firebaseSyncRepository.syncExpenseDetailed(exp, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                        if (res.anyOk) {
                            clearExpensePending(exp.id)
                            markExpenseSynced(exp.id)
                            expRelayCount++
                            if (res.remoteOk) {
                                markExpenseRemoteSynced(exp.id)
                                expRemoteCount++
                            } else if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                                lastExpErr = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                            }
                        } else if (!res.rtdbError.isNullOrBlank() || res.rtdbHttpCode != null) {
                            lastExpErr = res.rtdbHttpCode?.let { "HTTP $it" } ?: res.rtdbError
                        }
                    } catch (_: Throwable) {
                    }
                }
                lastTrxExpenseAutoSyncAtMs = System.currentTimeMillis()
                _expenseSyncStatus.value = when {
                    syncedExpensesList.isEmpty() -> "Belum ada catatan pengeluaran"
                    expRemoteCount == syncedExpensesList.size -> "✓ $expRemoteCount/${syncedExpensesList.size} pengeluaran tersinkron ke Realtime Database & Web Chrome"
                    expRemoteCount > 0 -> "✓ $expRemoteCount/${syncedExpensesList.size} pengeluaran tersinkron ke Realtime Database"
                    expRelayCount > 0 -> "Relay lokal aktif ($expRelayCount) • RTDB gagal${lastExpErr?.let { " ($it)" } ?: ""}"
                    else -> "Gagal kirim ke RTDB${lastExpErr?.let { " ($it)" } ?: ""}"
                }

                val msg = when {
                    trxCount > 0 || shiftCount > 0 || expRemoteCount > 0 ->
                        "Berhasil sinkron $trxCount transaksi, $expRemoteCount pengeluaran ke RTDB & $shiftCount shift ke Web Chrome!"
                    expRelayCount > 0 ->
                        "Tersimpan di Relay Lokal ($expRelayCount pengeluaran) • RTDB gagal${lastExpErr?.let { " ($it)" } ?: ""}"
                    else ->
                        "Semua data sudah tersinkronkan atau jaringan sedang offline."
                }
                _lastSyncStatus.value = msg
                onComplete(true, msg)
            } catch (t: Throwable) {
                val errMsg = "Gagal sinkron: ${t.localizedMessage ?: "Jaringan terputus"}"
                _lastSyncStatus.value = errMsg
                onComplete(false, errMsg)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    suspend fun syncPendingTransactionsInternal(): Pair<Int, Int> {
        val pending = try {
            repository.getUnsyncedTransactionsList().take(15)
        } catch (t: Throwable) {
            emptyList()
        }
        if (pending.isEmpty()) return Pair(0, 0)

        val dbId = firebaseDatabaseId.value
        val projId = firebaseProjectId.value
        val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
        var successCount = 0
        var failCount = 0

        for (trx in pending) {
            try {
                val items = repository.getItemsForTransaction(trx.id)
                val ok = firebaseSyncRepository.syncTransaction(trx, items, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                if (ok) {
                    repository.updateSyncStatus(trx.id, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                    successCount++
                } else {
                    repository.updateSyncStatus(trx.id, isSynced = false, syncedAt = null, error = "Tersimpan offline di Room DB")
                    failCount++
                }
            } catch (t: Throwable) {
                repository.updateSyncStatus(trx.id, isSynced = false, syncedAt = null, error = t.localizedMessage)
                failCount++
            }
        }

        if (successCount > 0) {
            _lastSyncStatus.value = "⚡ $successCount transaksi berhasil disinkronkan otomatis"
        }
        return Pair(successCount, failCount)
    }

    fun syncPendingTransactions(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _isCloudSyncing.value = true
            try {
                val (success, fail) = syncPendingTransactionsInternal()
                val (expSuccess, expFail) = syncPendingExpensesInternal()
                val totalOk = success + expSuccess
                val totalFail = fail + expFail
                val msg = if (totalFail == 0 && totalOk > 0) {
                    "Semua $success transaksi & $expSuccess pengeluaran berhasil disinkronkan!"
                } else if (totalOk == 0 && totalFail == 0) {
                    "Semua data sudah tersinkronkan ke Web Chrome."
                } else {
                    "$totalOk berhasil disinkronkan, $totalFail menunggu jaringan"
                }
                _lastSyncStatus.value = msg
                onComplete(true, msg)
            } catch (t: Throwable) {
                val errMsg = "Gagal sinkron: ${t.localizedMessage ?: "Periksa internet"}"
                _lastSyncStatus.value = errMsg
                onComplete(false, errMsg)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun syncSingleTransaction(transaction: Transaction, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                val items = repository.getItemsForTransaction(transaction.id)
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val hostUrl = resolveEffectiveHostingUrl(webDashboardUrl.value)
                val ok = firebaseSyncRepository.syncTransaction(transaction, items, databaseId = dbId, projectId = projId, hostingUrl = hostUrl)
                if (ok) {
                    repository.updateSyncStatus(transaction.id, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                    onResult(true, "Transaksi #${transaction.invoiceNumber} berhasil disinkronkan!")
                } else {
                    repository.updateSyncStatus(transaction.id, isSynced = false, syncedAt = null, error = "Tersimpan offline di Room DB")
                    onResult(false, "Tersimpan lokal di Room DB (Server belum merespons)")
                }
            } catch (t: Throwable) {
                val errMsg = "Error: ${t.localizedMessage ?: "Gagal koneksi"}"
                onResult(false, errMsg)
            }
        }
    }

    fun signInGoogleCloud(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            firebaseSyncRepository.signInWithGoogle(
                context = context,
                onSuccess = { user ->
                    _lastSyncStatus.value = "Terhubung: ${user.email ?: user.displayName}"
                    syncAllToCloud { _, _ -> }
                    onSuccess()
                },
                onError = { err ->
                    _lastSyncStatus.value = "Gagal login: $err"
                    onError(err)
                }
            )
        }
    }

    fun signOutGoogleCloud() {
        firebaseSyncRepository.signOut()
        _lastSyncStatus.value = "Keluar dari Firebase"
    }

    // --- Google Drive Auto Backup & Restore ---
    private val _driveConnected = MutableStateFlow(
        prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_CONNECTED, false)
    )
    val driveConnected: StateFlow<Boolean> = _driveConnected.asStateFlow()

    private val _driveAccountEmail = MutableStateFlow(
        prefs.getString(DriveAutoBackupManager.KEY_DRIVE_ACCOUNT_EMAIL, "") ?: ""
    )
    val driveAccountEmail: StateFlow<String> = _driveAccountEmail.asStateFlow()

    private val _driveAutoBackupEnabled = MutableStateFlow(
        prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false)
    )
    val driveAutoBackupEnabled: StateFlow<Boolean> = _driveAutoBackupEnabled.asStateFlow()

    private val _driveBackupFrequency = MutableStateFlow(
        DriveBackupFrequency.fromId(
            prefs.getString(
                DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY,
                DriveBackupFrequency.DAILY_SCHEDULED.id
            )
        )
    )
    val driveBackupFrequency: StateFlow<DriveBackupFrequency> = _driveBackupFrequency.asStateFlow()

    private val _driveBackupHour = MutableStateFlow(
        prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, DriveAutoBackupManager.DEFAULT_HOUR).coerceIn(0, 23)
    )
    val driveBackupHour: StateFlow<Int> = _driveBackupHour.asStateFlow()

    private val _driveBackupMinute = MutableStateFlow(
        prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, DriveAutoBackupManager.DEFAULT_MINUTE).coerceIn(0, 59)
    )
    val driveBackupMinute: StateFlow<Int> = _driveBackupMinute.asStateFlow()

    private val _driveBackupWifiOnly = MutableStateFlow(
        prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false)
    )
    val driveBackupWifiOnly: StateFlow<Boolean> = _driveBackupWifiOnly.asStateFlow()

    private val _driveBackupOnShiftClose = MutableStateFlow(
        prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, true)
    )
    val driveBackupOnShiftClose: StateFlow<Boolean> = _driveBackupOnShiftClose.asStateFlow()

    private val _driveBackupKeepCount = MutableStateFlow(
        prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, DriveAutoBackupManager.DEFAULT_KEEP_COUNT).coerceIn(0, 100)
    )
    val driveBackupKeepCount: StateFlow<Int> = _driveBackupKeepCount.asStateFlow()

    private val _lastDriveBackupStatus = MutableStateFlow(
        prefs.getString(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_STATUS, "") ?: ""
    )
    val lastDriveBackupStatus: StateFlow<String> = _lastDriveBackupStatus.asStateFlow()

    private val _lastDriveBackupFile = MutableStateFlow(
        prefs.getString(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_FILE, "") ?: ""
    )
    val lastDriveBackupFile: StateFlow<String> = _lastDriveBackupFile.asStateFlow()

    private val _lastDriveBackupTimestamp = MutableStateFlow(
        prefs.getLong(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_TIMESTAMP, 0L)
    )
    val lastDriveBackupTimestamp: StateFlow<Long> = _lastDriveBackupTimestamp.asStateFlow()

    private val _isDriveBackingUp = MutableStateFlow(false)
    val isDriveBackingUp: StateFlow<Boolean> = _isDriveBackingUp.asStateFlow()

    private val _isDriveRestoring = MutableStateFlow(false)
    val isDriveRestoring: StateFlow<Boolean> = _isDriveRestoring.asStateFlow()

    private val _isLoadingDriveFiles = MutableStateFlow(false)
    val isLoadingDriveFiles: StateFlow<Boolean> = _isLoadingDriveFiles.asStateFlow()

    private val _driveBackupFiles = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val driveBackupFiles: StateFlow<List<DriveBackupFile>> = _driveBackupFiles.asStateFlow()

    private fun reloadDriveStatusFromPrefs() {
        _lastDriveBackupStatus.value = prefs.getString(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_STATUS, "") ?: ""
        _lastDriveBackupFile.value = prefs.getString(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_FILE, "") ?: ""
        _lastDriveBackupTimestamp.value = prefs.getLong(DriveAutoBackupManager.KEY_LAST_DRIVE_BACKUP_TIMESTAMP, 0L)
    }

    fun refreshAllSettingsFromPrefs() {
        _configuredCategories.value = loadStringListPref(KEY_CUSTOM_CATEGORIES, DEFAULT_CATEGORIES)
        _configuredUnits.value = loadStringListPref(KEY_CUSTOM_UNITS, DEFAULT_UNITS)
        _foodDefaultUnit.value = prefs.getString(KEY_FOOD_DEFAULT_UNIT, DEFAULT_FOOD_UNIT)
            ?.trim()?.ifBlank { DEFAULT_FOOD_UNIT } ?: DEFAULT_FOOD_UNIT
        _drinkDefaultUnit.value = prefs.getString(KEY_DRINK_DEFAULT_UNIT, DEFAULT_DRINK_UNIT)
            ?.trim()?.ifBlank { DEFAULT_DRINK_UNIT } ?: DEFAULT_DRINK_UNIT
        _tableCount.value = prefs.getInt(KEY_TABLE_COUNT, DEFAULT_TABLE_COUNT).coerceIn(0, 100)

        prefs.getString("KEY_FIREBASE_PROJECT_ID", null)?.let { _firebaseProjectId.value = it }
        prefs.getString("KEY_FIREBASE_DATABASE_ID", null)?.let { _firebaseDatabaseId.value = it }
        prefs.getString("KEY_WEB_HOSTING_URL", null)?.let { _webDashboardUrl.value = resolveEffectiveHostingUrl(it) }

        _isBackgroundServiceEnabled.value = prefs.getBoolean(
            com.example.service.PosBackgroundService.KEY_BG_SERVICE_ENABLED, true
        )
        _isLowStockNotifEnabled.value = prefs.getBoolean(
            com.example.service.PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, true
        )
        _isAutoCloseEnabled.value = prefs.getBoolean(
            com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false
        )
        _autoCloseHour.value = prefs.getInt(
            com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR,
            com.example.service.AutoStoreCloseManager.DEFAULT_HOUR
        ).coerceIn(0, 23)
        _autoCloseMinute.value = prefs.getInt(
            com.example.service.AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE,
            com.example.service.AutoStoreCloseManager.DEFAULT_MINUTE
        ).coerceIn(0, 59)

        _driveAutoBackupEnabled.value = prefs.getBoolean(
            DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false
        )
        _driveBackupFrequency.value = DriveBackupFrequency.fromId(
            prefs.getString(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY, DriveBackupFrequency.DAILY_SCHEDULED.id)
        )
        _driveBackupHour.value = prefs.getInt(
            DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, DriveAutoBackupManager.DEFAULT_HOUR
        ).coerceIn(0, 23)
        _driveBackupMinute.value = prefs.getInt(
            DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, DriveAutoBackupManager.DEFAULT_MINUTE
        ).coerceIn(0, 59)
        _driveBackupWifiOnly.value = prefs.getBoolean(
            DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false
        )
        _driveBackupOnShiftClose.value = prefs.getBoolean(
            DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, true
        )
        _driveBackupKeepCount.value = prefs.getInt(
            DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, DriveAutoBackupManager.DEFAULT_KEEP_COUNT
        ).coerceIn(0, 100)
        reloadDriveStatusFromPrefs()

        if (_isAutoCloseEnabled.value) {
            com.example.service.AutoStoreCloseManager.scheduleNextAlarm(getApplication())
        }
        DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
    }

    private fun scheduleDebouncedDriveBackupIfActive(reason: String) {
        if (_driveAutoBackupEnabled.value && _driveConnected.value) {
            DriveAutoBackupManager.triggerDebouncedAutoBackup(getApplication(), reason = reason)
        }
    }

    fun startGoogleDriveAuthorization(
        onNeedsResolution: (android.app.PendingIntent) -> Unit,
        onAuthorized: (DriveAuthResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(coroutineExceptionHandler) {
            val res = googleDriveBackupManager.requestAuthorization()
            res.onSuccess { authResult ->
                if (authResult.needsResolution && authResult.pendingIntent != null) {
                    onNeedsResolution(authResult.pendingIntent)
                } else {
                    saveDriveConnection(authResult.email)
                    onAuthorized(authResult)
                }
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Gagal meminta izin Google Drive")
            }
        }
    }

    fun handleGoogleDriveAuthorizationIntent(
        data: android.content.Intent?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(coroutineExceptionHandler) {
            val res = googleDriveBackupManager.handleAuthorizationIntent(data)
            res.onSuccess { authResult ->
                saveDriveConnection(authResult.email)
                onSuccess(authResult.email)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Gagal menghubungkan Google Drive")
            }
        }
    }

    private fun saveDriveConnection(email: String) {
        val cleanEmail = email.trim()
        _driveConnected.value = true
        _driveAccountEmail.value = cleanEmail
        prefs.edit()
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_CONNECTED, true)
            .putString(DriveAutoBackupManager.KEY_DRIVE_ACCOUNT_EMAIL, cleanEmail)
            .apply()
        if (_driveAutoBackupEnabled.value) {
            DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch(coroutineExceptionHandler) {
            googleDriveBackupManager.disconnectAccount()
            _driveConnected.value = false
            _driveAccountEmail.value = ""
            _driveAutoBackupEnabled.value = false
            _driveBackupFiles.value = emptyList()
            prefs.edit()
                .putBoolean(DriveAutoBackupManager.KEY_DRIVE_CONNECTED, false)
                .putString(DriveAutoBackupManager.KEY_DRIVE_ACCOUNT_EMAIL, "")
                .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false)
                .apply()
            DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
        }
    }

    fun setDriveAutoBackupEnabled(enabled: Boolean) {
        _driveAutoBackupEnabled.value = enabled
        prefs.edit()
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, enabled)
            .apply()
        DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
    }

    fun setDriveBackupFrequency(frequency: DriveBackupFrequency) {
        _driveBackupFrequency.value = frequency
        prefs.edit()
            .putString(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY, frequency.id)
            .apply()
        DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
    }

    fun setDriveBackupScheduleTime(hour: Int, minute: Int) {
        val cleanH = hour.coerceIn(0, 23)
        val cleanM = minute.coerceIn(0, 59)
        _driveBackupHour.value = cleanH
        _driveBackupMinute.value = cleanM
        prefs.edit()
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, cleanH)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, cleanM)
            .apply()
        DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
    }

    fun setDriveBackupWifiOnly(wifiOnly: Boolean) {
        _driveBackupWifiOnly.value = wifiOnly
        prefs.edit()
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, wifiOnly)
            .apply()
        DriveAutoBackupManager.schedulePeriodicBackup(getApplication())
    }

    fun setDriveBackupOnShiftClose(enabled: Boolean) {
        _driveBackupOnShiftClose.value = enabled
        prefs.edit()
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, enabled)
            .apply()
    }

    fun setDriveBackupKeepCount(count: Int) {
        val clamped = count.coerceIn(0, 100)
        _driveBackupKeepCount.value = clamped
        prefs.edit()
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, clamped)
            .apply()
    }

    fun backupToGoogleDriveNow(
        onSuccess: (String) -> Unit,
        onNeedsReAuth: (android.app.PendingIntent) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _isDriveBackingUp.value = true
            try {
                val authRes = googleDriveBackupManager.requestAuthorization()
                val auth = authRes.getOrElse { err ->
                    onError(err.localizedMessage ?: "Gagal autentikasi Google Drive")
                    return@launch
                }
                if (auth.needsResolution && auth.pendingIntent != null) {
                    onNeedsReAuth(auth.pendingIntent)
                    return@launch
                }
                val token = auth.accessToken
                if (token.isNullOrBlank()) {
                    onError("Token Google Drive tidak tersedia")
                    return@launch
                }
                if (auth.email.isNotBlank()) {
                    saveDriveConnection(auth.email)
                }
                val execResult = DriveAutoBackupManager.performUploadWithToken(
                    context = getApplication(),
                    accessToken = token,
                    reason = "Backup Manual"
                )
                reloadDriveStatusFromPrefs()
                if (execResult.success) {
                    onSuccess(execResult.message)
                } else {
                    onError(execResult.message)
                }
            } catch (t: Throwable) {
                onError(t.localizedMessage ?: "Gagal backup ke Google Drive")
            } finally {
                _isDriveBackingUp.value = false
            }
        }
    }

    fun fetchGoogleDriveBackups(
        onNeedsReAuth: (android.app.PendingIntent) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _isLoadingDriveFiles.value = true
            try {
                val authRes = googleDriveBackupManager.requestAuthorization()
                val auth = authRes.getOrElse { err ->
                    onError(err.localizedMessage ?: "Gagal autentikasi Google Drive")
                    return@launch
                }
                if (auth.needsResolution && auth.pendingIntent != null) {
                    onNeedsReAuth(auth.pendingIntent)
                    return@launch
                }
                val token = auth.accessToken ?: run {
                    onError("Token Google Drive kosong")
                    return@launch
                }
                if (auth.email.isNotBlank()) {
                    saveDriveConnection(auth.email)
                }
                val listRes = googleDriveBackupManager.listBackupFiles(token)
                listRes.onSuccess { files ->
                    _driveBackupFiles.value = files
                }.onFailure { err ->
                    onError(err.localizedMessage ?: "Gagal mengambil daftar backup Google Drive")
                }
            } catch (t: Throwable) {
                onError(t.localizedMessage ?: "Gagal memuat file Google Drive")
            } finally {
                _isLoadingDriveFiles.value = false
            }
        }
    }

    fun restoreFromGoogleDrive(
        file: DriveBackupFile,
        onSuccess: (BackupStats) -> Unit,
        onNeedsReAuth: (android.app.PendingIntent) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _isDriveRestoring.value = true
            try {
                val authRes = googleDriveBackupManager.requestAuthorization()
                val auth = authRes.getOrElse { err ->
                    onError(err.localizedMessage ?: "Gagal autentikasi Google Drive")
                    return@launch
                }
                if (auth.needsResolution && auth.pendingIntent != null) {
                    onNeedsReAuth(auth.pendingIntent)
                    return@launch
                }
                val token = auth.accessToken ?: run {
                    onError("Token Google Drive kosong")
                    return@launch
                }
                val downloadRes = googleDriveBackupManager.downloadBackupJson(token, file.id)
                val jsonContent = downloadRes.getOrElse { err ->
                    onError(err.localizedMessage ?: "Gagal mengunduh file dari Google Drive")
                    return@launch
                }
                val restoreRes = backupRestoreManager.restoreFromJsonString(jsonContent)
                restoreRes.onSuccess { stats ->
                    refreshAllSettingsFromPrefs()
                    onSuccess(stats)
                }.onFailure { err ->
                    onError("File backup rusak atau tidak valid: ${err.localizedMessage}")
                }
            } catch (t: Throwable) {
                onError(t.localizedMessage ?: "Gagal memulihkan dari Google Drive")
            } finally {
                _isDriveRestoring.value = false
            }
        }
    }

    // --- Local JSON Backup & Restore ---
    fun exportLocalBackup(onSuccess: (BackupStats) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val stats = backupRestoreManager.exportBackup()
                onSuccess(stats)
            } catch (e: Exception) {
                onError("Gagal melakukan backup: ${e.message}")
            }
        }
    }

    fun restoreLocalBackup(jsonString: String, onSuccess: (BackupStats) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = backupRestoreManager.restoreFromJsonString(jsonString)
                if (result.isSuccess) {
                    refreshAllSettingsFromPrefs()
                    onSuccess(result.getOrThrow())
                } else {
                    onError("Format JSON tidak valid atau data rusak: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onError("Gagal memulihkan backup: ${e.message}")
            }
        }
    }

    suspend fun readBackupJsonFromUri(uri: Uri): String {
        return backupRestoreManager.readJsonFromUri(uri)
    }

    companion object {
        fun getStartOfDay(daysOffset: Int): Long {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, daysOffset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun getEndOfDay(daysOffset: Int): Long {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, daysOffset)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }

        fun getStartOfMonth(): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun getStartOfDayForMillis(millis: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun getEndOfDayForMillis(millis: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }

        fun normalizeUtcDatePickerMillisToLocalStartOfDay(utcMillis: Long): Long {
            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
            }
            val localCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return localCal.timeInMillis
        }

        fun localStartOfDayToUtcPickerMillis(localMillis: Long): Long {
            val localCal = Calendar.getInstance().apply {
                timeInMillis = localMillis
            }
            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, localCal.get(Calendar.YEAR))
                set(Calendar.MONTH, localCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return utcCal.timeInMillis
        }

        fun formatPeriodLabel(filter: DateFilter, customDateMillis: Long): String {
            val idLocale = java.util.Locale("id", "ID")
            val fmt = java.text.SimpleDateFormat("EEE, dd MMM yyyy", idLocale)
            return when (filter) {
                DateFilter.TODAY -> "Hari Ini (${fmt.format(java.util.Date(getStartOfDay(0)))})"
                DateFilter.YESTERDAY -> "Kemarin (${fmt.format(java.util.Date(getStartOfDay(-1)))})"
                DateFilter.CUSTOM_DATE -> fmt.format(java.util.Date(customDateMillis))
                DateFilter.LAST_7_DAYS -> "7 Hari Terakhir"
                DateFilter.THIS_MONTH -> "Bulan Ini"
                DateFilter.ALL -> "Semua Waktu"
            }
        }

        const val KEY_CUSTOM_CATEGORIES = "KEY_CUSTOM_CATEGORIES"
        const val KEY_CUSTOM_UNITS = "KEY_CUSTOM_UNITS"
        const val KEY_FOOD_DEFAULT_UNIT = "KEY_FOOD_DEFAULT_UNIT"
        const val KEY_DRINK_DEFAULT_UNIT = "KEY_DRINK_DEFAULT_UNIT"
        const val KEY_TABLE_COUNT = "KEY_TABLE_COUNT"

        val DEFAULT_CATEGORIES = listOf("Makanan", "Minuman", "Snack")
        val DEFAULT_UNITS = listOf("porsi", "pcs", "cup", "botol", "bks", "paket")
        const val DEFAULT_FOOD_UNIT = "porsi"
        const val DEFAULT_DRINK_UNIT = "pcs"
        const val DEFAULT_TABLE_COUNT = 5
    }
}
