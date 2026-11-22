package com.example.data.repository

import com.example.data.local.PaymentSummary
import com.example.data.local.PosDatabase
import com.example.data.local.TopProductSummary
import com.example.data.model.DailyReport
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val database: PosDatabase) {

    private val userDao = database.userDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val storeProfileDao = database.storeProfileDao()
    private val shiftClosingDao = database.shiftClosingDao()
    private val cloudHostingConfigDao = database.cloudHostingConfigDao()
    private val dailyReportDao = database.dailyReportDao()
    private val expenseDao = database.expenseDao()

    // --- Expenses (Pengeluaran Toko) ---
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<com.example.data.model.Expense>> =
        expenseDao.getExpensesBetween(startTime, endTime)

    suspend fun getExpensesBetweenList(startTime: Long, endTime: Long): List<com.example.data.model.Expense> = withContext(Dispatchers.IO) {
        expenseDao.getExpensesBetweenList(startTime, endTime)
    }

    fun getTotalExpensesBetween(startTime: Long, endTime: Long): Flow<Double?> =
        expenseDao.getTotalExpensesBetween(startTime, endTime)

    fun getAllExpenses(): Flow<List<com.example.data.model.Expense>> =
        expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: com.example.data.model.Expense): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: com.example.data.model.Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    // --- Users & Auth ---
    fun getAllActiveUsers(): Flow<List<User>> = userDao.getAllActiveUsers()

    suspend fun authenticate(username: String, pin: String): User? = withContext(Dispatchers.IO) {
        userDao.authenticate(username.trim(), pin.trim())
    }

    suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) {
        val normalized = user.username.trim().lowercase()
        val cleanUser = user.copy(username = normalized)
        val existing = userDao.getUserByUsername(normalized)
        if (cleanUser.id == 0L && existing != null) {
            val updated = cleanUser.copy(id = existing.id)
            userDao.updateUser(updated)
            existing.id
        } else {
            userDao.insertUser(cleanUser)
        }
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user.copy(username = user.username.trim().lowercase()))
    }

    suspend fun deleteUser(user: User) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    suspend fun getUserCount(): Int = withContext(Dispatchers.IO) {
        userDao.getUserCount()
    }

    suspend fun deduplicateUsersIfNeeded() = withContext(Dispatchers.IO) {
        userSeedMutex.withLock {
            userDao.deduplicateUsersByUsername()
        }
    }

    suspend fun seedDefaultUsersIfEmpty() = withContext(Dispatchers.IO) {
        userSeedMutex.withLock {
            userDao.deduplicateUsersByUsername()
            if (userDao.getUserCount() == 0) {
                defaultUsersList().forEach { defaultUser ->
                    if (userDao.getUserByUsername(defaultUser.username) == null) {
                        userDao.insertUser(defaultUser)
                    }
                }
                userDao.deduplicateUsersByUsername()
            }
        }
    }

    suspend fun resetDefaultUsers() = withContext(Dispatchers.IO) {
        userSeedMutex.withLock {
            defaultUsersList().forEach { defaultUser ->
                val existing = userDao.getUserByUsername(defaultUser.username)
                if (existing != null) {
                    userDao.updateUser(defaultUser.copy(id = existing.id, isActive = true))
                } else {
                    userDao.insertUser(defaultUser)
                }
            }
            userDao.deduplicateUsersByUsername()
        }
    }

    private fun defaultUsersList(): List<User> = listOf(
        User(
            username = "admin",
            pin = "1234",
            fullName = "Budi Santoso (Admin)",
            role = UserRole.ADMIN,
            phone = "081211112222"
        ),
        User(
            username = "kasir1",
            pin = "1111",
            fullName = "Siti Aminah (Kasir 1)",
            role = UserRole.KASIR,
            phone = "081233334444"
        ),
        User(
            username = "kasir2",
            pin = "2222",
            fullName = "Rian Pratama (Kasir 2)",
            role = UserRole.KASIR,
            phone = "081255556666"
        )
    )

    // --- Products & Inventory ---
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getAllProductsList(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAllProductsList()
    }

    fun getAllCategories(): Flow<List<String>> = productDao.getAllCategories()

    fun getLowStockProducts(): Flow<List<Product>> = productDao.getLowStockProducts()

    fun getOutOfStockProducts(): Flow<List<Product>> = productDao.getOutOfStockProducts()

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductsByCategory(category: String) = withContext(Dispatchers.IO) {
        productDao.deleteProductsByCategory(category)
    }

    suspend fun adjustStock(productId: Long, newStock: Int) = withContext(Dispatchers.IO) {
        productDao.updateStock(productId, newStock)
    }

    suspend fun renameProductCategory(oldCategory: String, newCategory: String) = withContext(Dispatchers.IO) {
        productDao.renameCategory(oldCategory, newCategory)
    }

    suspend fun updateUnitByCategory(category: String, newUnit: String) = withContext(Dispatchers.IO) {
        productDao.updateUnitByCategory(category, newUnit)
    }

    // --- Transactions & POS Checkout ---
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(startTime, endTime)

    suspend fun getTransactionsBetweenList(startTime: Long, endTime: Long): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getTransactionsBetweenList(startTime, endTime)
    }

    suspend fun getTransactionById(transactionId: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(transactionId)
    }

    suspend fun deleteTransactionsBetween(startTime: Long, endTime: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionItemsBetween(startTime, endTime)
        transactionDao.deleteTransactionsBetween(startTime, endTime)
    }

    suspend fun deleteTransactionById(transactionId: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteItemsByTransactionId(transactionId)
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem> = withContext(Dispatchers.IO) {
        transactionDao.getItemsForTransaction(transactionId)
    }

    fun getTopSellingProducts(startTime: Long, endTime: Long): Flow<List<TopProductSummary>> =
        transactionDao.getTopSellingProducts(startTime, endTime)

    fun getPaymentMethodBreakdown(startTime: Long, endTime: Long): Flow<List<PaymentSummary>> =
        transactionDao.getPaymentMethodBreakdown(startTime, endTime)

    fun getTotalProfitBetween(startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalProfitBetween(startTime, endTime)

    suspend fun processCheckout(
        transaction: Transaction,
        items: List<TransactionItem>
    ): Long = withContext(Dispatchers.IO) {
        val totalCost = items.sumOf { it.costPrice * it.quantity }
        val profit = transaction.grandTotal - totalCost
        val totalCount = items.sumOf { it.quantity }

        val enrichedTransaction = transaction.copy(
            profitAmount = profit,
            totalItemsCount = totalCount
        )

        // Insert transaction header
        val trxId = transactionDao.insertTransaction(enrichedTransaction)

        // Assign transactionId to all items and insert
        val itemsWithId = items.map { it.copy(transactionId = trxId) }
        transactionDao.insertTransactionItems(itemsWithId)

        // Atomically decrease product stock
        for (item in items) {
            productDao.decreaseStock(item.productId, item.quantity)
        }

        trxId
    }

    // --- Pelacakan Sinkronisasi Database Otomatis ---
    fun getUnsyncedTransactions(): Flow<List<Transaction>> = transactionDao.getUnsyncedTransactions()

    fun getUnsyncedCountFlow(): Flow<Int> = transactionDao.getUnsyncedCountFlow()

    fun getSyncedCountFlow(): Flow<Int> = transactionDao.getSyncedCountFlow()

    suspend fun getUnsyncedTransactionsList(): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getUnsyncedTransactionsList()
    }

    suspend fun updateSyncStatus(id: Long, isSynced: Boolean, syncedAt: Long?, error: String?) = withContext(Dispatchers.IO) {
        transactionDao.updateSyncStatus(id, isSynced, syncedAt, error)
    }

    suspend fun markAsSynced(invoiceNumber: String) = withContext(Dispatchers.IO) {
        transactionDao.markAsSynced(invoiceNumber)
    }

    // --- Store Profile ---
    fun getStoreProfile(): Flow<StoreProfile?> = storeProfileDao.getStoreProfile()

    suspend fun getStoreProfileSync(): StoreProfile = withContext(Dispatchers.IO) {
        storeProfileDao.getStoreProfileSync() ?: StoreProfile(id = 1)
    }

    suspend fun updateStoreProfile(storeProfile: StoreProfile) = withContext(Dispatchers.IO) {
        storeProfileDao.updateStoreProfile(storeProfile.copy(id = 1))
    }

    // --- Shift Closings (Tutup Kasir) ---
    fun getAllShiftClosings(): Flow<List<com.example.data.model.ShiftClosing>> = shiftClosingDao.getAllShiftClosings()

    suspend fun insertShiftClosing(closing: com.example.data.model.ShiftClosing): Long = withContext(Dispatchers.IO) {
        shiftClosingDao.insertShiftClosing(closing)
    }

    // --- Cloud Hosting & Owner Report Config (Room Persistence) ---
    fun getCloudHostingConfig(): Flow<com.example.data.model.CloudHostingConfig?> = cloudHostingConfigDao.getConfigFlow()

    suspend fun getCloudHostingConfigSync(): com.example.data.model.CloudHostingConfig = withContext(Dispatchers.IO) {
        cloudHostingConfigDao.getConfigSync() ?: com.example.data.model.CloudHostingConfig()
    }

    suspend fun saveCloudHostingConfig(config: com.example.data.model.CloudHostingConfig) = withContext(Dispatchers.IO) {
        cloudHostingConfigDao.insertOrUpdateConfig(config)
    }

    // --- Database Room Laporan Harian (Daily Reports SQLite) ---
    fun getAllDailyReports(): Flow<List<DailyReport>> = dailyReportDao.getAllDailyReports()

    fun getDailyReportByDate(date: String): Flow<DailyReport?> = dailyReportDao.getDailyReportByDate(date)

    suspend fun getDailyReportByDateDirect(date: String): DailyReport? = withContext(Dispatchers.IO) {
        dailyReportDao.getDailyReportByDateDirect(date)
    }

    suspend fun saveDailyReport(report: DailyReport): Long = withContext(Dispatchers.IO) {
        dailyReportDao.insertOrUpdateDailyReport(report)
    }

    suspend fun deleteDailyReportById(id: Long) = withContext(Dispatchers.IO) {
        dailyReportDao.deleteDailyReportById(id)
    }

    suspend fun markDailyReportAsSynced(date: String) = withContext(Dispatchers.IO) {
        dailyReportDao.markDailyReportAsSynced(date)
    }

    /**
     * Menghitung dan mengarsipkan rekapitulasi laporan harian langsung ke Room Database (tabel daily_reports).
     * Dapat dipanggil kapan saja untuk meng-generate rekap penjualan hari ini atau tanggal tertentu.
     */
    suspend fun generateOrUpdateDailyReportForDate(
        dateStr: String,
        startTime: Long,
        endTime: Long
    ): DailyReport = withContext(Dispatchers.IO) {
        val transactions = transactionDao.getAllTransactionsList()
            .filter { it.timestamp in startTime..endTime }

        val totalRevenue = transactions.sumOf { it.grandTotal }
        val totalProfit = transactions.sumOf { it.profitAmount }
        val totalTransactions = transactions.size
        val totalItemsSold = transactions.sumOf { it.totalItemsCount }
        val totalCash = transactions.filter { it.paymentMethod == com.example.data.model.PaymentMethod.TUNAI }.sumOf { it.grandTotal }
        val totalNonCash = transactions.filter { it.paymentMethod != com.example.data.model.PaymentMethod.TUNAI }.sumOf { it.grandTotal }
        val totalQris = transactions.filter { it.paymentMethod == com.example.data.model.PaymentMethod.QRIS }.sumOf { it.grandTotal }
        val totalTransfer = transactions.filter { it.paymentMethod == com.example.data.model.PaymentMethod.TRANSFER }.sumOf { it.grandTotal }
        val totalDiscount = transactions.sumOf { it.discount }
        val totalTax = transactions.sumOf { it.tax }

        val closings = shiftClosingDao.getAllShiftClosingsList()
            .filter { it.closingTime in startTime..endTime }
        val shiftCount = closings.size
        val expectedCash = if (closings.isNotEmpty()) closings.sumOf { it.expectedCash } else totalCash
        val actualCash = if (closings.isNotEmpty()) closings.sumOf { it.actualCash } else totalCash
        val cashDifference = actualCash - expectedCash

        val cashiers = transactions.map { it.cashierName }.distinct().joinToString(", ").ifBlank { "Kasir" }

        // Cari 5 produk terlaris hari tersebut
        val allTrxIds = transactions.map { it.id }.toSet()
        val allItems = transactionDao.getAllTransactionItemsList().filter { it.transactionId in allTrxIds }
        val topSummary = allItems.groupBy { it.productName }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .joinToString(", ") { "${it.first} (${it.second}x)" }

        val existing = dailyReportDao.getDailyReportByDateDirect(dateStr)
        val report = DailyReport(
            id = existing?.id ?: 0,
            date = dateStr,
            dateTimestamp = startTime,
            totalRevenue = totalRevenue,
            totalProfit = totalProfit,
            totalTransactions = totalTransactions,
            totalItemsSold = totalItemsSold,
            totalCash = totalCash,
            totalNonCash = totalNonCash,
            totalQris = totalQris,
            totalTransfer = totalTransfer,
            totalDiscount = totalDiscount,
            totalTax = totalTax,
            expectedCash = expectedCash,
            actualCash = actualCash,
            cashDifference = cashDifference,
            topProductSummary = topSummary.ifBlank { "Belum ada produk terjual" },
            cashierNames = cashiers,
            shiftCount = shiftCount,
            notes = existing?.notes ?: "Rekap transaksi tersimpan permanen di Room Database.",
            isSynced = existing?.isSynced ?: false,
            syncedAt = existing?.syncedAt,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        dailyReportDao.insertOrUpdateDailyReport(report)
        report
    }

    companion object {
        private val userSeedMutex = kotlinx.coroutines.sync.Mutex()

        fun generateInvoiceNumber(): String {
            val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            return "TRX-${dateFormat.format(Date())}"
        }
    }
}
