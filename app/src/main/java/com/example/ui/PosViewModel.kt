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
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.PosRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    REPORT("Laporan"),
    USERS("Karyawan"),
    PRINTER("Printer"),
    SETTINGS("Toko"),
    CLOUD_HOSTING("Hosting & Cloud")
}

enum class DateFilter(val label: String) {
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini")
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
    val defaultHostingUrl: String = "https://ais-dev-wgdowcmyzzxhdk2ko2iu46-852851605479.asia-southeast1.run.app"

    private val savedProjectId = prefs.getString("KEY_FIREBASE_PROJECT_ID", null)?.let {
        if (it.contains("gen-lang-client")) defaultFirebaseProjectId else it
    } ?: defaultFirebaseProjectId

    private val savedDatabaseId = prefs.getString("KEY_FIREBASE_DATABASE_ID", null)?.let {
        if (it.contains("ai-studio-android-kasirpos") || it.isBlank()) defaultDatabaseId else it
    } ?: defaultDatabaseId

    private val _firebaseProjectId = MutableStateFlow(savedProjectId)
    val firebaseProjectId: StateFlow<String> = _firebaseProjectId.asStateFlow()

    private val _firebaseDatabaseId = MutableStateFlow(savedDatabaseId)
    val firebaseDatabaseId: StateFlow<String> = _firebaseDatabaseId.asStateFlow()

    private val _webDashboardUrl = MutableStateFlow(
        prefs.getString("KEY_WEB_HOSTING_URL", defaultHostingUrl)?.ifBlank { defaultHostingUrl }
            ?: defaultHostingUrl
    )
    val webDashboardUrl: StateFlow<String> = _webDashboardUrl.asStateFlow()

    val currentWebDashboardUrl: String
        get() = _webDashboardUrl.value

    fun updateWebHostingConfig(projectId: String, databaseId: String, hostingUrl: String) {
        val cleanPid = projectId.trim().ifBlank { defaultFirebaseProjectId }
        val cleanDbId = databaseId.trim().ifBlank { defaultDatabaseId }
        val cleanUrl = hostingUrl.trim().ifBlank { "https://$cleanPid.web.app" }

        _firebaseProjectId.value = cleanPid
        _firebaseDatabaseId.value = cleanDbId
        _webDashboardUrl.value = cleanUrl

        prefs.edit()
            .putString("KEY_FIREBASE_PROJECT_ID", cleanPid)
            .putString("KEY_FIREBASE_DATABASE_ID", cleanDbId)
            .putString("KEY_WEB_HOSTING_URL", cleanUrl)
            .apply()
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
                        _firebaseProjectId.value = config.ownerProjectId
                        _firebaseDatabaseId.value = config.ownerDatabaseId
                        _webDashboardUrl.value = config.ownerHostingUrl
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
            } catch (e: Throwable) {
                // Ignore transient initial sync errors
            }
        }
        // Recurring Background Synchronization Engine (checks pending transactions safely every 25s)
        viewModelScope.launch(coroutineExceptionHandler) {
            while (true) {
                try {
                    kotlinx.coroutines.delay(25000)
                    if (_isAutoSyncActive.value && !_isCloudSyncing.value) {
                        syncPendingTransactionsInternal()
                    }
                } catch (e: Throwable) {
                    // Ignore periodic background sync error
                }
            }
        }
    }

    // --- Navigation & App State ---
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.POS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
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
            _loginError.value = "Akun default telah dipulihkan! Admin: admin (PIN: 1234), Kasir: kasir1 (PIN: 1111)"
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
        .combine(MutableStateFlow(StoreProfile())) { profile, default ->
            profile ?: default
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreProfile())

    fun updateStoreProfile(name: String, address: String, phone: String, ownerPhone: String, footer: String) {
        viewModelScope.launch {
            val updated = storeProfile.value.copy(
                storeName = name.trim(),
                address = address.trim(),
                phone = phone.trim(),
                ownerPhone = ownerPhone.trim(),
                receiptFooter = footer.trim()
            )
            repository.updateStoreProfile(updated)
        }
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
        // Sinkronisasi otomatis ke Database Cloud untuk SEMUA TRANSAKSI & LAPORAN TUTUP KASIR
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            _isCloudSyncing.value = true
            _lastSyncStatus.value = "☁️ Tutup Kasir: Menyinkronkan seluruh transaksi shift ke Database..."
            try {
                val dbId = firebaseDatabaseId.value

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
                        val ok = firebaseSyncRepository.syncTransaction(trx, items, databaseId = dbId)
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

                // 2. Sinkronkan Laporan Tutup Kasir (Firebase Realtime Database /shift_reports)
                val shiftSyncSuccess = firebaseSyncRepository.syncShiftClosing(closingWithId, databaseId = dbId)

                // 3. Sinkronkan Profil Toko
                try {
                    val profile = repository.getStoreProfileSync()
                    firebaseSyncRepository.syncStoreProfile(profile, databaseId = dbId)
                } catch (tProf: Throwable) {
                    // safe ignore
                }

                // 4. Sinkronkan Laporan Harian ke Realtime Database
                try {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val todayStr = dateFormat.format(java.util.Date())
                    val dailyRep = repository.getDailyReportByDateDirect(todayStr)
                    if (dailyRep != null) {
                        val dOk = firebaseSyncRepository.syncDailyReport(dailyRep, databaseId = dbId)
                        if (dOk) repository.markDailyReportAsSynced(todayStr)
                    }
                } catch (tDaily: Throwable) {
                    Log.w("PosViewModel", "Daily report sync note: ${tDaily.message}")
                }

                val statusMessage = if (syncedTrxCount > 0 || shiftSyncSuccess) {
                    "☁️ Tutup Kasir Berhasil! $syncedTrxCount transaksi & laporan shift #${closingWithId.id} otomatis tersinkron ke Cloud Database!"
                } else {
                    "Laporan tutup kasir & transaksi tersimpan aman offline di Room DB."
                }
                _lastSyncStatus.value = statusMessage
                Log.i("PosViewModel", statusMessage)
            } catch (t: Throwable) {
                Log.e("PosViewModel", "Error in automatic shift transactions sync", t)
                _lastSyncStatus.value = "Tutup kasir tersimpan lokal di Room DB (offline)"
            } finally {
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
        val dateLabel = selectedDateFilter.value.label
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

    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
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

            // Automatic Sync to Cloud Database (asynchronous in background on IO)
            val itemsForSync = transactionItems.map { it.copy(transactionId = newTrxId) }
            val dbId = firebaseDatabaseId.value
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val syncSuccess = firebaseSyncRepository.syncTransaction(completedTrx, itemsForSync, databaseId = dbId)
                    if (syncSuccess) {
                        repository.updateSyncStatus(newTrxId, isSynced = true, syncedAt = System.currentTimeMillis(), error = null)
                        _lastSyncStatus.value = "⚡ Transaksi ${completedTrx.invoiceNumber} otomatis tersinkron ke Cloud!"
                    } else {
                        repository.updateSyncStatus(newTrxId, isSynced = false, syncedAt = null, error = "Tersimpan offline di Room DB")
                        _lastSyncStatus.value = "Tersimpan offline di Room DB (menunggu sinkron)"
                    }
                } catch (e: Exception) {
                    repository.updateSyncStatus(newTrxId, isSynced = false, syncedAt = null, error = e.localizedMessage)
                    _lastSyncStatus.value = "Tersimpan offline di Room DB (menunggu sinkron)"
                }
            }

            // Update Room Database Laporan Harian (SQLite)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val todayStr = dateFormat.format(java.util.Date())
                    repository.generateOrUpdateDailyReportForDate(
                        dateStr = todayStr,
                        startTime = getStartOfDay(0),
                        endTime = getEndOfDay(0)
                    )
                } catch (t: Throwable) {
                    // Safe ignore
                }
            }
        }
    }

    // --- Real-time Daily Reports ---
    private val _selectedDateFilter = MutableStateFlow(DateFilter.TODAY)
    val selectedDateFilter: StateFlow<DateFilter> = _selectedDateFilter.asStateFlow()

    fun setDateFilter(filter: DateFilter) {
        _selectedDateFilter.value = filter
        refreshReportData()
    }

    private val _reportStartTime = MutableStateFlow(getStartOfDay(0))
    private val _reportEndTime = MutableStateFlow(getEndOfDay(0))

    val reportTransactions = combine(_reportStartTime, _reportEndTime) { start, end ->
        repository.getTransactionsBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getTransactionsBetween(getStartOfDay(0), getEndOfDay(0)))

    val topSellingProducts = combine(_reportStartTime, _reportEndTime) { start, end ->
        repository.getTopSellingProducts(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getTopSellingProducts(getStartOfDay(0), getEndOfDay(0)))

    val paymentMethodStats = combine(_reportStartTime, _reportEndTime) { start, end ->
        repository.getPaymentMethodBreakdown(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getPaymentMethodBreakdown(getStartOfDay(0), getEndOfDay(0)))

    val totalProfitFlow = combine(_reportStartTime, _reportEndTime) { start, end ->
        repository.getTotalProfitBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getTotalProfitBetween(getStartOfDay(0), getEndOfDay(0)))

    private fun refreshReportData() {
        when (_selectedDateFilter.value) {
            DateFilter.TODAY -> {
                _reportStartTime.value = getStartOfDay(0)
                _reportEndTime.value = getEndOfDay(0)
            }
            DateFilter.YESTERDAY -> {
                _reportStartTime.value = getStartOfDay(-1)
                _reportEndTime.value = getEndOfDay(-1)
            }
            DateFilter.LAST_7_DAYS -> {
                _reportStartTime.value = getStartOfDay(-6)
                _reportEndTime.value = getEndOfDay(0)
            }
            DateFilter.THIS_MONTH -> {
                _reportStartTime.value = getStartOfMonth()
                _reportEndTime.value = getEndOfDay(0)
            }
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
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val todayStr = dateFormat.format(java.util.Date())
                val report = repository.generateOrUpdateDailyReportForDate(
                    dateStr = todayStr,
                    startTime = getStartOfDay(0),
                    endTime = getEndOfDay(0)
                )
                val dbId = firebaseDatabaseId.value
                val rtdbOk = firebaseSyncRepository.syncDailyReport(report, databaseId = dbId)
                if (rtdbOk) {
                    repository.markDailyReportAsSynced(report.date)
                }
                withContext(Dispatchers.Main) {
                    val syncMsg = if (rtdbOk) " & tersinkron ke Realtime Database Cloud!" else "!"
                    onResult?.invoke(true, "Laporan hari ini ($todayStr) berhasil disimpan di Room DB$syncMsg")
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

    fun testCloudConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
            _isCloudSyncing.value = true
            _lastSyncStatus.value = "Sedang memeriksa koneksi server Cloud..."
            try {
                val dbId = firebaseDatabaseId.value
                val projId = firebaseProjectId.value
                val (connected, message) = withContext(Dispatchers.IO) {
                    firebaseSyncRepository.checkCloudConnection(databaseId = dbId, projectId = projId)
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
            _lastSyncStatus.value = "Sedang menyinkronkan data langsung ke Firebase..."
            try {
                val dbId = firebaseDatabaseId.value
                val profile = repository.getStoreProfileSync()
                try {
                    firebaseSyncRepository.syncStoreProfile(profile, databaseId = dbId)
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
                        val success = firebaseSyncRepository.syncTransaction(t, items, databaseId = dbId)
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

                val closings = try {
                    repository.getAllShiftClosings().first().take(10)
                } catch (t: Throwable) {
                    emptyList()
                }
                var shiftCount = 0
                for (c in closings) {
                    try {
                        val success = firebaseSyncRepository.syncShiftClosing(c, databaseId = dbId)
                        if (success) shiftCount++
                    } catch (tShift: Throwable) {
                        Log.w("PosViewModel", "syncShiftClosing note", tShift)
                    }
                }

                val msg = if (trxCount > 0 || shiftCount > 0) {
                    "Berhasil sinkron $trxCount transaksi & $shiftCount shift ke Cloud!"
                } else {
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
        var successCount = 0
        var failCount = 0

        for (trx in pending) {
            try {
                val items = repository.getItemsForTransaction(trx.id)
                val ok = firebaseSyncRepository.syncTransaction(trx, items, databaseId = dbId)
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
                val msg = if (fail == 0 && success > 0) {
                    "Semua $success transaksi berhasil disinkronkan ke Cloud!"
                } else if (success == 0 && fail == 0) {
                    "Semua transaksi sudah tersinkronkan ke Cloud."
                } else {
                    "$success berhasil disinkronkan, $fail transaksi menunggu jaringan"
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
                val ok = firebaseSyncRepository.syncTransaction(transaction, items, databaseId = dbId)
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
    }
}
