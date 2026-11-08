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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    // --- Users & Auth ---
    fun getAllActiveUsers(): Flow<List<User>> = userDao.getAllActiveUsers()

    suspend fun authenticate(username: String, pin: String): User? = withContext(Dispatchers.IO) {
        userDao.authenticate(username.trim(), pin.trim())
    }

    suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    suspend fun getUserCount(): Int = withContext(Dispatchers.IO) {
        userDao.getUserCount()
    }

    suspend fun seedDefaultUsersIfEmpty() = withContext(Dispatchers.IO) {
        if (userDao.getUserCount() == 0) {
            val admin = com.example.data.model.User(
                username = "admin",
                pin = "1234",
                fullName = "Budi Santoso (Admin)",
                role = com.example.data.model.UserRole.ADMIN,
                phone = "081211112222"
            )
            val kasir1 = com.example.data.model.User(
                username = "kasir1",
                pin = "1111",
                fullName = "Siti Aminah (Kasir 1)",
                role = com.example.data.model.UserRole.KASIR,
                phone = "081233334444"
            )
            val kasir2 = com.example.data.model.User(
                username = "kasir2",
                pin = "2222",
                fullName = "Rian Pratama (Kasir 2)",
                role = com.example.data.model.UserRole.KASIR,
                phone = "081255556666"
            )
            userDao.insertUsers(listOf(admin, kasir1, kasir2))
        }
    }

    suspend fun resetDefaultUsers() = withContext(Dispatchers.IO) {
        val admin = com.example.data.model.User(
            username = "admin",
            pin = "1234",
            fullName = "Budi Santoso (Admin)",
            role = com.example.data.model.UserRole.ADMIN,
            phone = "081211112222"
        )
        val kasir1 = com.example.data.model.User(
            username = "kasir1",
            pin = "1111",
            fullName = "Siti Aminah (Kasir 1)",
            role = com.example.data.model.UserRole.KASIR,
            phone = "081233334444"
        )
        val kasir2 = com.example.data.model.User(
            username = "kasir2",
            pin = "2222",
            fullName = "Rian Pratama (Kasir 2)",
            role = com.example.data.model.UserRole.KASIR,
            phone = "081255556666"
        )
        userDao.insertUsers(listOf(admin, kasir1, kasir2))
    }

    // --- Products & Inventory ---
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

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

    // --- Transactions & POS Checkout ---
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(startTime, endTime)

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
        storeProfileDao.getStoreProfileSync() ?: StoreProfile()
    }

    suspend fun updateStoreProfile(storeProfile: StoreProfile) = withContext(Dispatchers.IO) {
        storeProfileDao.updateStoreProfile(storeProfile)
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
        fun generateInvoiceNumber(): String {
            val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            return "TRX-${dateFormat.format(Date())}"
        }
    }
}
