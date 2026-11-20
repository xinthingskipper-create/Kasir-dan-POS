package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Product::class,
        Transaction::class,
        TransactionItem::class,
        StoreProfile::class,
        com.example.data.model.ShiftClosing::class,
        com.example.data.model.CloudHostingConfig::class,
        com.example.data.model.DailyReport::class,
        com.example.data.model.Expense::class
    ],
    version = 7,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun storeProfileDao(): StoreProfileDao
    abstract fun shiftClosingDao(): ShiftClosingDao
    abstract fun cloudHostingConfigDao(): CloudHostingConfigDao
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos_database"
                )
                    .addMigrations(MIGRATION_6_7)
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            try {
                // Ensure default store profile exists if table was ever empty
                db.execSQL("""
                    INSERT OR IGNORE INTO store_profile (id, storeName, address, phone, ownerPhone, receiptHeader, receiptFooter)
                    VALUES (1, 'KASIR POS NUSANTARA', 'Jl. Merdeka No. 45, Jakarta Pusat', '0812-9876-5432', '081298765432', 'STRUK PEMBELIAN RESMI', 'Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan.')
                """.trimIndent())
                // Ensure sembako products are removed if any remained from previous version
                db.execSQL("DELETE FROM products WHERE LOWER(category) = 'sembako'")
                // Ensure expenses table exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL
                    )
                """.trimIndent())
            } catch (t: Throwable) {
                // Safe ignore
            }
        }

        private suspend fun populateInitialData(db: PosDatabase) {
            // 1. Store Profile
            db.storeProfileDao().insertStoreProfile(
                StoreProfile(
                    id = 1,
                    storeName = "KASIR POS NUSANTARA",
                    address = "Jl. Merdeka No. 45, Jakarta Pusat",
                    phone = "0812-9876-5432",
                    receiptHeader = "STRUK PEMBELIAN RESMI",
                    receiptFooter = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan."
                )
            )

            // 2. Default Users
            val admin = User(
                username = "admin",
                pin = "1234",
                fullName = "Budi Santoso (Admin)",
                role = UserRole.ADMIN,
                phone = "081211112222"
            )
            val kasir1 = User(
                username = "kasir1",
                pin = "1111",
                fullName = "Siti Aminah (Kasir 1)",
                role = UserRole.KASIR,
                phone = "081233334444"
            )
            val kasir2 = User(
                username = "kasir2",
                pin = "2222",
                fullName = "Rian Pratama (Kasir 2)",
                role = UserRole.KASIR,
                phone = "081255556666"
            )
            db.userDao().insertUsers(listOf(admin, kasir1, kasir2))

            // 3. Initial Products (Makanan, Minuman, Snack - Sembako removed)
            val products = listOf(
                // Minuman
                Product(name = "Kopi Susu Gula Aren", category = "Minuman", barcode = "899001", buyPrice = 8000.0, sellPrice = 18000.0, stock = 50, minStockAlert = 10, unit = "cup"),
                Product(name = "Teh Tarik Dingin", category = "Minuman", barcode = "899002", buyPrice = 5000.0, sellPrice = 12000.0, stock = 40, minStockAlert = 8, unit = "cup"),
                Product(name = "Es Jeruk Peras Segar", category = "Minuman", barcode = "899003", buyPrice = 4000.0, sellPrice = 10000.0, stock = 30, minStockAlert = 5, unit = "cup"),
                Product(name = "Air Mineral 600ml", category = "Minuman", barcode = "899004", buyPrice = 2500.0, sellPrice = 5000.0, stock = 65, minStockAlert = 15, unit = "botol"),
                Product(name = "Matcha Latte Ice", category = "Minuman", barcode = "899005", buyPrice = 11000.0, sellPrice = 22000.0, stock = 4, minStockAlert = 8, unit = "cup"),

                // Makanan
                Product(name = "Mie Goreng Spesial", category = "Makanan", barcode = "899010", buyPrice = 12000.0, sellPrice = 18000.0, stock = 35, minStockAlert = 5, unit = "porsi"),
                Product(name = "Nasi Goreng Ayam", category = "Makanan", barcode = "899011", buyPrice = 14000.0, sellPrice = 22000.0, stock = 25, minStockAlert = 5, unit = "porsi"),
                Product(name = "Ayam Geprek Sambal Bawang", category = "Makanan", barcode = "899012", buyPrice = 15000.0, sellPrice = 23000.0, stock = 20, minStockAlert = 5, unit = "porsi"),
                Product(name = "Roti Bakar Coklat Keju", category = "Makanan", barcode = "899013", buyPrice = 10000.0, sellPrice = 16000.0, stock = 18, minStockAlert = 4, unit = "porsi"),

                // Snack
                Product(name = "Keripik Singkong Balado", category = "Snack", barcode = "899030", buyPrice = 6000.0, sellPrice = 10000.0, stock = 25, minStockAlert = 5, unit = "bks"),
                Product(name = "Wafer Coklat Renyah", category = "Snack", barcode = "899031", buyPrice = 5500.0, sellPrice = 9000.0, stock = 30, minStockAlert = 5, unit = "bks")
            )
            db.productDao().insertProducts(products)

            // 4. Initial Sample Transactions for today
            val now = System.currentTimeMillis()
            val trx1 = Transaction(
                invoiceNumber = "TRX-20261025-001",
                cashierId = 2,
                cashierName = "Siti Aminah (Kasir 1)",
                timestamp = now - (3 * 3600 * 1000),
                subtotal = 54000.0,
                discount = 0.0,
                tax = 0.0,
                grandTotal = 54000.0,
                paymentMethod = PaymentMethod.TUNAI,
                amountPaid = 60000.0,
                changeAmount = 6000.0,
                customerName = "Bpk. Hendra",
                isSynced = true,
                syncedAt = now - (3 * 3600 * 1000),
                profitAmount = 26000.0,
                totalItemsCount = 3
            )
            val trx1Id = db.transactionDao().insertTransaction(trx1)
            db.transactionDao().insertTransactionItems(
                listOf(
                    TransactionItem(transactionId = trx1Id, productId = 1, productName = "Kopi Susu Gula Aren", unitPrice = 18000.0, costPrice = 8000.0, quantity = 2, subtotal = 36000.0),
                    TransactionItem(transactionId = trx1Id, productId = 6, productName = "Mie Goreng Spesial", unitPrice = 18000.0, costPrice = 12000.0, quantity = 1, subtotal = 18000.0)
                )
            )

            val trx2 = Transaction(
                invoiceNumber = "TRX-20261025-002",
                cashierId = 1,
                cashierName = "Budi Santoso (Admin)",
                timestamp = now - (1 * 3600 * 1000),
                subtotal = 92000.0,
                discount = 5000.0,
                tax = 0.0,
                grandTotal = 87000.0,
                paymentMethod = PaymentMethod.QRIS,
                amountPaid = 87000.0,
                changeAmount = 0.0,
                customerName = "Ibu Maya",
                isSynced = true,
                syncedAt = now - (1 * 3600 * 1000),
                profitAmount = 37000.0,
                totalItemsCount = 5
            )
            val trx2Id = db.transactionDao().insertTransaction(trx2)
            db.transactionDao().insertTransactionItems(
                listOf(
                    TransactionItem(transactionId = trx2Id, productId = 7, productName = "Nasi Goreng Ayam", unitPrice = 22000.0, costPrice = 14000.0, quantity = 2, subtotal = 44000.0),
                    TransactionItem(transactionId = trx2Id, productId = 8, productName = "Ayam Geprek Sambal Bawang", unitPrice = 23000.0, costPrice = 15000.0, quantity = 1, subtotal = 23000.0),
                    TransactionItem(transactionId = trx2Id, productId = 2, productName = "Teh Tarik Dingin", unitPrice = 12000.0, costPrice = 5000.0, quantity = 2, subtotal = 24000.0)
                )
            )

            // 5. Initial Cloud Hosting & Owner Report Config
            db.cloudHostingConfigDao().insertOrUpdateConfig(
                com.example.data.model.CloudHostingConfig(
                    id = 1,
                    useSeparateOwnerProject = true,
                    ownerProjectId = "foodrock-report",
                    ownerDatabaseId = "foodrock-report-default-rtdb",
                    ownerHostingUrl = "http://127.0.0.1:8080/owner.html",
                    enableConsumerWeb = false,
                    consumerProjectId = "gen-lang-client-0076807780",
                    consumerDatabaseId = "(default)",
                    consumerWebUrl = "https://menu-konsumen.web.app",
                    enableEmailBackup = true,
                    backupEmail = "owner@gmail.com",
                    autoBackupOnShiftClose = true,
                    enableLanServer = true,
                    lanPort = 8080,
                    lanIpAddress = "192.168.1.100"
                )
            )

            // 6. Initial Daily Report Archive in Room
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = dateFormat.format(java.util.Date(now))
            val yesterdayStr = dateFormat.format(java.util.Date(now - 86400000L))

            db.dailyReportDao().insertDailyReports(
                listOf(
                    com.example.data.model.DailyReport(
                        date = todayStr,
                        dateTimestamp = now,
                        totalRevenue = 141000.0,
                        totalProfit = 63000.0,
                        totalTransactions = 2,
                        totalItemsSold = 8,
                        totalCash = 54000.0,
                        totalNonCash = 87000.0,
                        totalQris = 87000.0,
                        totalTransfer = 0.0,
                        totalDiscount = 5000.0,
                        totalTax = 0.0,
                        expectedCash = 54000.0,
                        actualCash = 54000.0,
                        cashDifference = 0.0,
                        topProductSummary = "Kopi Susu Gula Aren (2x), Teh Tarik Dingin (2x), Nasi Goreng Ayam (2x)",
                        cashierNames = "Siti Aminah, Budi Santoso",
                        shiftCount = 1,
                        notes = "Laporan harian otomatis tersimpan di Room Database.",
                        isSynced = true,
                        syncedAt = now
                    ),
                    com.example.data.model.DailyReport(
                        date = yesterdayStr,
                        dateTimestamp = now - 86400000L,
                        totalRevenue = 325000.0,
                        totalProfit = 142000.0,
                        totalTransactions = 5,
                        totalItemsSold = 18,
                        totalCash = 180000.0,
                        totalNonCash = 145000.0,
                        totalQris = 145000.0,
                        totalTransfer = 0.0,
                        totalDiscount = 10000.0,
                        totalTax = 0.0,
                        expectedCash = 180000.0,
                        actualCash = 180000.0,
                        cashDifference = 0.0,
                        topProductSummary = "Ayam Geprek Sambal Bawang (4x), Mie Goreng Spesial (3x)",
                        cashierNames = "Siti Aminah",
                        shiftCount = 2,
                        notes = "Rekap penjualan kemarin diarsipkan di SQLite.",
                        isSynced = true,
                        syncedAt = now - 86400000L
                    )
                )
            )

            // 6. Sample Expenses (Pengeluaran Operasional Toko)
            try {
                db.expenseDao().insertExpense(
                    com.example.data.model.Expense(
                        title = "Listrik & Internet Toko",
                        amount = 150000.0,
                        category = "Utilitas",
                        timestamp = now - 7200000L,
                        notes = "Tagihan bulanan operasional toko",
                        createdBy = "Admin"
                    )
                )
                db.expenseDao().insertExpense(
                    com.example.data.model.Expense(
                        title = "Plastik & Perlengkapan Kemasan",
                        amount = 45000.0,
                        category = "Bahan/Kemasan",
                        timestamp = now - 3600000L,
                        notes = "Kemasan take away & kantong belanja",
                        createdBy = "Admin"
                    )
                )
            } catch (t: Throwable) {
                // Safe ignore
            }
        }
    }
}
