package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PosDatabase
import com.example.data.model.DailyReport
import com.example.data.model.Expense
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BackupRestoreManager
import com.example.data.repository.RestoreGuardTracker
import com.example.service.AutoStoreCloseManager
import com.example.service.PosBackgroundService
import com.example.ui.PosViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineRestoreGuardTest {

    private lateinit var context: Application
    private lateinit var sourceDb: PosDatabase
    private lateinit var activeDb: PosDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        RestoreGuardTracker.clearAll(context)
        sourceDb = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        activeDb = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        sourceDb.close()
        activeDb.close()
        RestoreGuardTracker.clearAll(context)
    }

    @Test
    fun `offline csv and json restore do not bring back deleted items or overwrite edited items and settings`() = runTest {
        val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(BackupRestoreManager.KEY_CUSTOM_CATEGORIES, JSONArray(listOf("Makanan", "Minuman", "Snack")).toString())
            .putString(BackupRestoreManager.KEY_CUSTOM_UNITS, JSONArray(listOf("porsi", "gelas", "pcs")).toString())
            .putInt(BackupRestoreManager.KEY_TABLE_COUNT, 8)
            .putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, true)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 22)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, 30)
            .commit()

        sourceDb.storeProfileDao().insertStoreProfile(
            StoreProfile(id = 1, storeName = "Warung Lama", address = "Jl. Lama", receiptFooter = "Footer Lama")
        )
        sourceDb.productDao().insertProducts(
            listOf(
                Product(id = 101L, name = "Nasi Goreng", category = "Makanan", buyPrice = 10000.0, sellPrice = 20000.0, stock = 25, unit = "porsi", updatedAt = 1000L),
                Product(id = 102L, name = "Kerupuk Kaleng", category = "Snack", buyPrice = 1000.0, sellPrice = 3000.0, stock = 40, unit = "pcs", updatedAt = 1000L),
                Product(id = 103L, name = "Es Teh Manis", category = "Minuman", buyPrice = 2000.0, sellPrice = 5000.0, stock = 30, unit = "gelas", updatedAt = 1000L)
            )
        )
        sourceDb.userDao().insertUsers(
            listOf(
                User(id = 1L, username = "admin", pin = "1234", fullName = "Admin Lama", role = UserRole.ADMIN),
                User(id = 2L, username = "kasir_lama", pin = "1111", fullName = "Kasir Dihapus", role = UserRole.KASIR)
            )
        )
        sourceDb.expenseDao().insertExpenses(
            listOf(
                Expense(id = 201L, title = "Pengeluaran Dihapus", amount = 50000.0, category = "Operasional", timestamp = 1_760_000_100_000L),
                Expense(id = 202L, title = "Pengeluaran Hilang Utuh", amount = 75000.0, category = "Bahan", timestamp = 1_760_000_200_000L)
            )
        )
        sourceDb.transactionDao().insertTransactions(
            listOf(
                Transaction(
                    id = 301L,
                    invoiceNumber = "INV-DEL-301",
                    cashierId = 1L,
                    cashierName = "Admin Lama",
                    subtotal = 20000.0,
                    grandTotal = 20000.0,
                    paymentMethod = PaymentMethod.TUNAI,
                    amountPaid = 20000.0,
                    orderType = OrderType.DINE_IN
                ),
                Transaction(
                    id = 302L,
                    invoiceNumber = "INV-KEEP-302",
                    cashierId = 1L,
                    cashierName = "Admin Lama",
                    subtotal = 5000.0,
                    grandTotal = 5000.0,
                    paymentMethod = PaymentMethod.QRIS,
                    amountPaid = 5000.0,
                    orderType = OrderType.TAKE_AWAY
                )
            )
        )
        sourceDb.transactionDao().insertTransactionItems(
            listOf(
                TransactionItem(id = 401L, transactionId = 301L, productId = 101L, productName = "Nasi Goreng", unitPrice = 20000.0, costPrice = 10000.0, quantity = 1, subtotal = 20000.0),
                TransactionItem(id = 402L, transactionId = 302L, productId = 103L, productName = "Es Teh Manis", unitPrice = 5000.0, costPrice = 2000.0, quantity = 1, subtotal = 5000.0)
            )
        )
        sourceDb.dailyReportDao().insertDailyReports(
            listOf(
                DailyReport(id = 501L, date = "2026-11-20", dateTimestamp = 1_760_000_000_000L, totalRevenue = 100000.0),
                DailyReport(id = 502L, date = "2026-11-21", dateTimestamp = 1_760_086_400_000L, totalRevenue = 150000.0)
            )
        )

        val sourceMgr = BackupRestoreManager(context, sourceDb)
        val csvBackup = sourceMgr.exportCsvBackup().csvString
        val jsonBackup = sourceMgr.exportBackup().jsonString

        // Simulate current device state after edits & deletions (product 103 & trx 302 & expense 202 were not edited/deleted)
        activeDb.storeProfileDao().insertStoreProfile(
            StoreProfile(id = 1, storeName = "Warung Baru Diedit", address = "Jl. Baru", receiptFooter = "Footer Baru")
        )
        RestoreGuardTracker.markSettingEdited(context, RestoreGuardTracker.SETTING_STORE_PROFILE)

        prefs.edit()
            .putString(BackupRestoreManager.KEY_CUSTOM_CATEGORIES, JSONArray(listOf("Makanan", "Minuman Dingin")).toString())
            .putString(BackupRestoreManager.KEY_CUSTOM_UNITS, JSONArray(listOf("porsi", "pcs")).toString())
            .putInt(BackupRestoreManager.KEY_TABLE_COUNT, 20)
            .putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 21)
            .commit()

        RestoreGuardTracker.recordCategoryDeleted(context, "Snack")
        RestoreGuardTracker.recordCategoryRenamed(context, "Minuman", "Minuman Dingin")
        RestoreGuardTracker.recordUnitDeleted(context, "gelas")
        RestoreGuardTracker.markSettingEdited(context, RestoreGuardTracker.SETTING_TABLE_COUNT)

        // Product 101 edited locally; Product 102 deleted locally; Product 103 absent locally and untouched
        activeDb.productDao().insertProduct(
            Product(id = 101L, name = "Nasi Goreng Spesial", category = "Makanan", buyPrice = 12000.0, sellPrice = 28000.0, stock = 12, unit = "porsi", updatedAt = 2000L)
        )
        RestoreGuardTracker.recordProductEdited(context, 101L)
        RestoreGuardTracker.recordProductDeleted(context, 102L, "Kerupuk Kaleng")

        // User admin edited; kasir_lama deleted
        activeDb.userDao().insertUser(
            User(id = 1L, username = "admin", pin = "9999", fullName = "Owner Baru", role = UserRole.ADMIN)
        )
        RestoreGuardTracker.recordUserEdited(context, 1L, "admin")
        RestoreGuardTracker.recordUserDeleted(context, 2L, "kasir_lama")

        // Expense 201 deleted; Transaction 301 deleted; Report 501 deleted
        RestoreGuardTracker.recordExpenseDeleted(context, 201L, "Pengeluaran Dihapus", 1_760_000_100_000L)
        RestoreGuardTracker.recordTransactionDeleted(context, 301L, "INV-DEL-301")
        RestoreGuardTracker.recordDailyReportDeleted(context, 501L, "2026-11-20")

        suspend fun assertProtectedStateAfterRestore(statsFormat: String, restoreCall: suspend () -> com.example.data.repository.BackupStats) {
            val stats = restoreCall()
            assertEquals(statsFormat, stats.format)
            assertTrue(stats.skippedEditedOrDeletedCount > 0)
            assertTrue(stats.skippedSettingsCount > 0)

            // Edited StoreProfile & tableCount stay intact; unedited autoClose restores from backup
            val profile = activeDb.storeProfileDao().getStoreProfileSync()
            assertEquals("Warung Baru Diedit", profile?.storeName)
            assertEquals(20, prefs.getInt(BackupRestoreManager.KEY_TABLE_COUNT, 0))
            assertTrue(prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false))
            assertEquals(22, prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 0))

            // Deleted category "Snack" & old renamed "Minuman" do NOT return
            val catsRaw = prefs.getString(BackupRestoreManager.KEY_CUSTOM_CATEGORIES, "[]") ?: "[]"
            val cats = JSONArray(catsRaw).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
            assertFalse(cats.any { it.equals("Snack", ignoreCase = true) })
            assertFalse(cats.any { it.equals("Minuman", ignoreCase = true) })
            assertTrue(cats.contains("Minuman Dingin"))

            // Deleted unit "gelas" does NOT return
            val unitsRaw = prefs.getString(BackupRestoreManager.KEY_CUSTOM_UNITS, "[]") ?: "[]"
            val units = JSONArray(unitsRaw).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
            assertFalse(units.any { it.equals("gelas", ignoreCase = true) })

            // Products: 101 stays edited, 102 stays deleted, untouched 103 restores
            val products = activeDb.productDao().getAllProductsList()
            val prod101 = products.firstOrNull { it.id == 101L }
            assertNotNull(prod101)
            assertEquals("Nasi Goreng Spesial", prod101?.name)
            assertEquals(28000.0, prod101?.sellPrice ?: 0.0, 0.01)
            assertEquals(12, prod101?.stock)
            assertNull(products.firstOrNull { it.id == 102L || it.name == "Kerupuk Kaleng" })
            assertNotNull(products.firstOrNull { it.id == 103L })

            // Users: admin keeps edited PIN, deleted kasir_lama stays gone
            val users = activeDb.userDao().getAllUsersList()
            val admin = users.firstOrNull { it.username == "admin" }
            assertEquals("9999", admin?.pin)
            assertNull(users.firstOrNull { it.username == "kasir_lama" })

            // Expenses: deleted 201 stays gone, untouched 202 restores
            val expenses = activeDb.expenseDao().getAllExpensesList()
            assertNull(expenses.firstOrNull { it.id == 201L })
            assertNotNull(expenses.firstOrNull { it.id == 202L })

            // Transactions & items: deleted 301 & item 401 stay gone, untouched 302 & item 402 restore
            val trxs = activeDb.transactionDao().getAllTransactionsList()
            assertNull(trxs.firstOrNull { it.id == 301L || it.invoiceNumber == "INV-DEL-301" })
            assertNotNull(trxs.firstOrNull { it.id == 302L })
            val items = activeDb.transactionDao().getAllTransactionItemsList()
            assertNull(items.firstOrNull { it.transactionId == 301L })
            assertNotNull(items.firstOrNull { it.transactionId == 302L })

            // Daily Reports: deleted 501 stays gone, untouched 502 restores
            val reports = activeDb.dailyReportDao().getAllDailyReportsList()
            assertNull(reports.firstOrNull { it.id == 501L || it.date == "2026-11-20" })
            assertNotNull(reports.firstOrNull { it.id == 502L })
        }

        val activeMgr = BackupRestoreManager(context, activeDb)
        // Test 1: Restore offline CSV
        assertProtectedStateAfterRestore("CSV") {
            activeMgr.restoreFromCsvString(csvBackup).getOrThrow()
        }
        // Test 2: Restore offline JSON with same protection
        // Reset untouched autoClose locally to false so we verify JSON restore works identically too
        prefs.edit().putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false).commit()
        assertProtectedStateAfterRestore("JSON") {
            activeMgr.restoreFromJsonString(jsonBackup).getOrThrow()
        }
    }

    @Test
    fun `PosViewModel edits and deletions automatically populate RestoreGuardTracker across offline CSV restore`() = runBlocking {
        val viewModel = PosViewModel(context)
        // Give Room seeders a brief moment
        Thread.sleep(250)

        // Customize & export offline CSV backup cleanly
        viewModel.setTableCount(9)
        viewModel.addCategory("Kopi")
        viewModel.updateStoreProfile("Toko Sebelum Edit", "Jl. Merdeka", "0811", "0812", "Footer 1")
        Thread.sleep(150)

        val db = PosDatabase.getDatabase(context)
        val prodToDelete = Product(id = 901L, name = "Menu Hapus VM", category = "Kopi", buyPrice = 5000.0, sellPrice = 12000.0, stock = 10, unit = "cup")
        val prodToEdit = Product(id = 902L, name = "Menu Edit VM", category = "Kopi", buyPrice = 6000.0, sellPrice = 15000.0, stock = 20, unit = "cup")
        db.productDao().insertProducts(listOf(prodToDelete, prodToEdit))

        val csvBackup = BackupRestoreManager(context, db).exportCsvBackup().csvString

        // Now perform real user edits/deletions through PosViewModel
        viewModel.deleteProduct(prodToDelete)
        viewModel.updateProduct(prodToEdit.copy(sellPrice = 22000.0))
        viewModel.adjustStock(902L, 7)
        viewModel.removeCategory("Kopi")
        viewModel.setTableCount(25)
        viewModel.updateStoreProfile("Toko Sesudah Edit", "Jl. Merdeka 2", "0811", "0812", "Footer 2")
        Thread.sleep(250)

        // Restore the earlier offline CSV backup
        val result = BackupRestoreManager(context, db).restoreFromCsvString(csvBackup)
        assertTrue(result.isSuccess)

        val productsAfter = db.productDao().getAllProductsList()
        assertNull(productsAfter.firstOrNull { it.id == 901L || it.name == "Menu Hapus VM" })
        val editedAfter = productsAfter.firstOrNull { it.id == 902L }
        assertNotNull(editedAfter)
        assertEquals(22000.0, editedAfter?.sellPrice ?: 0.0, 0.01)
        assertEquals(7, editedAfter?.stock)

        val profileAfter = db.storeProfileDao().getStoreProfileSync()
        assertEquals("Toko Sesudah Edit", profileAfter?.storeName)

        viewModel.refreshAllSettingsFromPrefs()
        assertEquals(25, viewModel.tableCount.value)
        assertFalse(viewModel.configuredCategories.value.any { it.equals("Kopi", ignoreCase = true) })
    }
}
