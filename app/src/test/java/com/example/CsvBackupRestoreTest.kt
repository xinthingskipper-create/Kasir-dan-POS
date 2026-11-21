package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PosDatabase
import com.example.data.model.CloudHostingConfig
import com.example.data.model.DailyReport
import com.example.data.model.Expense
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ShiftClosing
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BackupRestoreManager
import com.example.data.repository.CsvBackupCodec
import com.example.service.AutoStoreCloseManager
import com.example.service.DriveAutoBackupManager
import com.example.service.DriveBackupFrequency
import com.example.service.PosBackgroundService
import com.example.ui.PosViewModel
import com.example.ui.screens.StoreCsvBackupSection
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CsvBackupRestoreTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Application
    private lateinit var db: PosDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `offline csv backup exports and restores all settings transactions and quoted fields losslessly`() = runTest {
        val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(BackupRestoreManager.KEY_CUSTOM_CATEGORIES, JSONArray(listOf("Makanan", "Minuman", "Snack")).toString())
            .putString(BackupRestoreManager.KEY_CUSTOM_UNITS, JSONArray(listOf("porsi", "gelas", "pack")).toString())
            .putString(BackupRestoreManager.KEY_FOOD_DEFAULT_UNIT, "porsi")
            .putString(BackupRestoreManager.KEY_DRINK_DEFAULT_UNIT, "gelas")
            .putInt(BackupRestoreManager.KEY_TABLE_COUNT, 12)
            .putString("KEY_FIREBASE_PROJECT_ID", "csv-pos-proj")
            .putString("KEY_FIREBASE_DATABASE_ID", "csv-pos-rtdb")
            .putString("KEY_WEB_HOSTING_URL", "https://csv-pos-proj.web.app")
            .putBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, false)
            .putBoolean(PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, true)
            .putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, true)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 22)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, 45)
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, true)
            .putString(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY, DriveBackupFrequency.EVERY_12_HOURS.id)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, 20)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, 15)
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, true)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, 10)
            .commit()

        db.storeProfileDao().insertStoreProfile(
            StoreProfile(
                id = 1,
                storeName = "Warung \"Maju, Jaya\"",
                address = "Jl. Merdeka No. 10, Blok A",
                phone = "081200001111",
                ownerPhone = "081299998888",
                receiptHeader = "HEADER, CSV",
                receiptFooter = "Terima kasih!\nDatang kembali, ya."
            )
        )
        db.cloudHostingConfigDao().insertOrUpdateConfig(
            CloudHostingConfig(
                id = 1,
                ownerProjectId = "csv-pos-proj",
                ownerDatabaseId = "csv-pos-rtdb",
                backupEmail = "owner@csvtest.id",
                enableLanServer = true,
                lanPort = 8181
            )
        )
        val prodId = db.productDao().insertProduct(
            Product(
                id = 21L,
                name = "Ayam Bakar, Madu",
                category = "Makanan",
                buyPrice = 12000.0,
                sellPrice = 24000.0,
                stock = 18,
                unit = "porsi"
            )
        )
        val trxId = db.transactionDao().insertTransaction(
            Transaction(
                id = 301L,
                invoiceNumber = "TRX-CSV-301",
                cashierId = 2L,
                cashierName = "Budi, Kasir",
                timestamp = 1_760_100_000_000L,
                subtotal = 48000.0,
                grandTotal = 48000.0,
                paymentMethod = PaymentMethod.TRANSFER,
                amountPaid = 50000.0,
                changeAmount = 2000.0,
                orderType = OrderType.DINE_IN,
                tableNumber = "Meja 4",
                notes = "Pedas \"sedang\"\nTanpa kecap",
                isSynced = true,
                syncedAt = 1_760_100_005_000L,
                profitAmount = 24000.0,
                totalItemsCount = 2
            )
        )
        db.transactionDao().insertTransactionItems(
            listOf(
                TransactionItem(
                    id = 401L,
                    transactionId = trxId,
                    productId = prodId,
                    productName = "Ayam Bakar, Madu",
                    unitPrice = 24000.0,
                    costPrice = 12000.0,
                    quantity = 2,
                    subtotal = 48000.0,
                    itemNotes = "Paha"
                )
            )
        )
        db.expenseDao().insertExpense(
            Expense(
                id = 501L,
                title = "Gas LPG 12kg",
                amount = 210000.0,
                category = "Operasional",
                timestamp = 1_760_100_200_000L,
                notes = "Nota #12",
                createdBy = "Admin"
            )
        )
        db.shiftClosingDao().insertShiftClosing(
            ShiftClosing(
                id = 601L,
                closingTime = 1_760_100_500_000L,
                cashierId = 2L,
                cashierName = "Budi, Kasir",
                totalTransactions = 1,
                totalCash = 0.0,
                totalNonCash = 48000.0,
                totalRevenue = 48000.0,
                expectedCash = 0.0,
                actualCash = 0.0,
                difference = 0.0,
                notes = "Aman"
            )
        )
        db.userDao().insertUser(
            User(
                id = 2L,
                username = "budi",
                pin = "5678",
                fullName = "Budi, Kasir",
                role = UserRole.KASIR,
                phone = "0855667788"
            )
        )
        db.dailyReportDao().insertOrUpdateDailyReport(
            DailyReport(
                id = 701L,
                date = "2026-11-21",
                dateTimestamp = 1_760_100_600_000L,
                totalRevenue = 48000.0,
                totalProfit = 24000.0,
                totalTransactions = 1,
                totalItemsSold = 2,
                totalNonCash = 48000.0,
                totalTransfer = 48000.0,
                topProductSummary = "Ayam Bakar, Madu (2)",
                cashierNames = "Budi, Kasir",
                shiftCount = 1,
                isSynced = false
            )
        )

        val backupMgr = BackupRestoreManager(context, db)
        val exported = backupMgr.exportCsvBackup()
        assertEquals("CSV", exported.format)
        assertTrue(exported.fileName.endsWith(".csv"))
        assertTrue(File(exported.filePath).exists())
        assertEquals(1, exported.productCount)
        assertEquals(1, exported.transactionCount)
        assertEquals(1, exported.expenseCount)

        // Verify FileProvider can create a shareable content URI for the CSV file
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(exported.filePath)
        )
        assertNotNull(shareUri)
        assertTrue(shareUri.toString().contains("backup_kasirpos_"))

        // Clear prefs and restore into fresh DB
        prefs.edit().clear().commit()
        val restoreDb = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val restoreMgr = BackupRestoreManager(context, restoreDb)
            val restoreResult = restoreMgr.restoreFromCsvString(exported.csvString)
            assertTrue(restoreResult.isSuccess)
            val stats = restoreResult.getOrThrow()
            assertEquals("CSV", stats.format)
            assertTrue(stats.settingsRestored)
            assertEquals(1, stats.productCount)
            assertEquals(1, stats.transactionCount)
            assertEquals(1, stats.expenseCount)
            assertEquals(1, stats.shiftCount)
            assertEquals(1, stats.userCount)
            assertEquals(1, stats.dailyReportCount)

            val profile = restoreDb.storeProfileDao().getStoreProfileSync()
            assertEquals("Warung \"Maju, Jaya\"", profile?.storeName)
            assertEquals("Terima kasih!\nDatang kembali, ya.", profile?.receiptFooter)

            val cloud = restoreDb.cloudHostingConfigDao().getConfigSync()
            assertEquals("owner@csvtest.id", cloud?.backupEmail)
            assertEquals(8181, cloud?.lanPort)

            val trxs = restoreDb.transactionDao().getAllTransactionsList()
            assertEquals(1, trxs.size)
            assertEquals("TRX-CSV-301", trxs.first().invoiceNumber)
            assertEquals("Pedas \"sedang\"\nTanpa kecap", trxs.first().notes)
            assertEquals(24000.0, trxs.first().profitAmount, 0.01)
            assertEquals(1_760_100_005_000L, trxs.first().syncedAt)

            val items = restoreDb.transactionDao().getAllTransactionItemsList()
            assertEquals(1, items.size)
            assertEquals("Ayam Bakar, Madu", items.first().productName)

            assertEquals(12, prefs.getInt(BackupRestoreManager.KEY_TABLE_COUNT, 0))
            assertEquals("gelas", prefs.getString(BackupRestoreManager.KEY_DRINK_DEFAULT_UNIT, ""))
            assertEquals(true, prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false))
            assertEquals(22, prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 0))
            assertEquals(DriveBackupFrequency.EVERY_12_HOURS.id, prefs.getString(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY, ""))
        } finally {
            restoreDb.close()
        }
    }

    @Test
    fun `csv codec supports semicolon excel delimiter and rejects invalid csv`() {
        val semicolonCsv = """
            META_HEADER;app;version;exportTimestamp;exportDate
            META;KasirPOS;2;1760000000000;2026-11-21
            SETTING_HEADER;group;key;value
            SETTING;storeProfile;storeName;Toko Semicolon
            SETTING;appSettings;tableCount;9
        """.trimIndent()

        val decoded = CsvBackupCodec.decodeCsvToJsonSnapshot(semicolonCsv)
        assertTrue(decoded.isSuccess)
        val root = decoded.getOrThrow()
        assertEquals("Toko Semicolon", root.getJSONObject("storeProfile").getString("storeName"))
        assertEquals(9, root.getJSONObject("appSettings").getInt("tableCount"))

        val invalid = CsvBackupCodec.decodeCsvToJsonSnapshot("col1,col2\nfoo,bar")
        assertTrue(invalid.isFailure)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `StoreCsvBackupSection renders offline csv actions and opens share dialog`() {
        val viewModel = PosViewModel(context)

        composeTestRule.setContent {
            StoreCsvBackupSection(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("csv_backup_restore_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_export_backup_csv").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_restore_backup_csv").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_manual_csv_restore").assertIsDisplayed()

        // Click Backup & Bagikan CSV and verify share dialog opens
        composeTestRule.onNodeWithTag("btn_export_backup_csv").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithTag("btn_share_csv_file").assertIsDisplayed()
                true
            } catch (_: Throwable) {
                false
            }
        }
        composeTestRule.onNodeWithTag("btn_copy_csv_text").assertIsDisplayed()
    }
}
