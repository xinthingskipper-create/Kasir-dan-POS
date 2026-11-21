package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PosDatabase
import com.example.data.model.CloudHostingConfig
import com.example.data.model.Expense
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.repository.BackupRestoreManager
import com.example.service.AutoStoreCloseManager
import com.example.service.DriveAutoBackupManager
import com.example.service.DriveBackupFrequency
import com.example.service.PosBackgroundService
import com.example.ui.PosViewModel
import com.example.ui.screens.GoogleDriveBackupCard
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoogleDriveBackupTest {

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
    fun `backup v2 exports and restores all settings transactions and expenses`() = runTest {
        val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(BackupRestoreManager.KEY_CUSTOM_CATEGORIES, JSONArray(listOf("Makanan", "Minuman", "Dessert")).toString())
            .putString(BackupRestoreManager.KEY_CUSTOM_UNITS, JSONArray(listOf("porsi", "gelas", "box")).toString())
            .putString(BackupRestoreManager.KEY_FOOD_DEFAULT_UNIT, "porsi")
            .putString(BackupRestoreManager.KEY_DRINK_DEFAULT_UNIT, "gelas")
            .putInt(BackupRestoreManager.KEY_TABLE_COUNT, 14)
            .putString("KEY_FIREBASE_PROJECT_ID", "my-pos-project")
            .putString("KEY_FIREBASE_DATABASE_ID", "my-pos-db")
            .putString("KEY_WEB_HOSTING_URL", "https://my-pos-project.web.app")
            .putBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, false)
            .putBoolean(PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, false)
            .putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, true)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 23)
            .putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, 15)
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, true)
            .putString(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY, DriveBackupFrequency.EVERY_6_HOURS.id)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, 21)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, 45)
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, true)
            .putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, true)
            .putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, 20)
            .commit()

        db.storeProfileDao().insertStoreProfile(
            StoreProfile(
                id = 1,
                storeName = "Warung Drive Nusantara",
                address = "Jl. Sudirman No. 88",
                phone = "0811223344",
                ownerPhone = "0811998877",
                receiptHeader = "HEADER TEST",
                receiptFooter = "FOOTER TEST"
            )
        )
        db.cloudHostingConfigDao().insertOrUpdateConfig(
            CloudHostingConfig(
                id = 1,
                ownerProjectId = "my-pos-project",
                ownerDatabaseId = "my-pos-db",
                backupEmail = "backup@example.com",
                enableLanServer = false,
                lanPort = 9090
            )
        )
        val prodId = db.productDao().insertProduct(
            Product(
                id = 10L,
                name = "Es Kopi Susu",
                category = "Minuman",
                buyPrice = 7000.0,
                sellPrice = 15000.0,
                stock = 30,
                unit = "gelas"
            )
        )
        val trxId = db.transactionDao().insertTransaction(
            Transaction(
                id = 55L,
                invoiceNumber = "TRX-DRIVE-055",
                cashierId = 1L,
                cashierName = "Admin",
                timestamp = 1_760_000_000_000L,
                subtotal = 30000.0,
                grandTotal = 30000.0,
                paymentMethod = PaymentMethod.QRIS,
                amountPaid = 30000.0,
                orderType = OrderType.TAKE_AWAY,
                tableNumber = "T3",
                isSynced = true,
                profitAmount = 16000.0,
                totalItemsCount = 2
            )
        )
        db.transactionDao().insertTransactionItems(
            listOf(
                TransactionItem(
                    id = 77L,
                    transactionId = trxId,
                    productId = prodId,
                    productName = "Es Kopi Susu",
                    unitPrice = 15000.0,
                    costPrice = 7000.0,
                    quantity = 2,
                    subtotal = 30000.0
                )
            )
        )
        db.expenseDao().insertExpense(
            Expense(
                id = 88L,
                title = "Beli Es Batu",
                amount = 25000.0,
                category = "Bahan/Kemasan",
                timestamp = 1_760_000_100_000L,
                notes = "2 karung",
                createdBy = "Admin"
            )
        )

        val backupMgr = BackupRestoreManager(context, db)
        val exported = backupMgr.exportBackup()
        assertEquals(1, exported.productCount)
        assertEquals(1, exported.transactionCount)
        assertEquals(1, exported.expenseCount)

        val rootJson = JSONObject(exported.jsonString)
        assertEquals(2, rootJson.getInt("version"))
        assertTrue(rootJson.has("appSettings"))
        assertTrue(rootJson.has("cloudHostingConfig"))
        assertTrue(rootJson.has("expenses"))
        assertTrue(rootJson.has("transactions"))

        // Wipe prefs & restore into fresh DB
        prefs.edit().clear().commit()
        val restoreDb = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val restoreMgr = BackupRestoreManager(context, restoreDb)
            val restoreResult = restoreMgr.restoreFromJsonString(exported.jsonString)
            assertTrue(restoreResult.isSuccess)
            val stats = restoreResult.getOrThrow()
            assertTrue(stats.settingsRestored)
            assertEquals(1, stats.productCount)
            assertEquals(1, stats.transactionCount)
            assertEquals(1, stats.expenseCount)

            val restoredProfile = restoreDb.storeProfileDao().getStoreProfileSync()
            assertEquals("Warung Drive Nusantara", restoredProfile?.storeName)
            assertEquals("0811998877", restoredProfile?.ownerPhone)

            val restoredCloud = restoreDb.cloudHostingConfigDao().getConfigSync()
            assertEquals("backup@example.com", restoredCloud?.backupEmail)
            assertEquals(9090, restoredCloud?.lanPort)

            val restoredTrxs = restoreDb.transactionDao().getAllTransactionsList()
            assertEquals(1, restoredTrxs.size)
            assertEquals("TRX-DRIVE-055", restoredTrxs.first().invoiceNumber)
            assertEquals(16000.0, restoredTrxs.first().profitAmount, 0.01)

            val restoredExpenses = restoreDb.expenseDao().getAllExpensesList()
            assertEquals(1, restoredExpenses.size)
            assertEquals("Beli Es Batu", restoredExpenses.first().title)

            assertEquals(14, prefs.getInt(BackupRestoreManager.KEY_TABLE_COUNT, 0))
            assertEquals("gelas", prefs.getString(BackupRestoreManager.KEY_DRINK_DEFAULT_UNIT, ""))
            assertEquals(true, prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false))
            assertEquals(23, prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, 0))
            assertEquals(true, prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false))
            assertEquals(DriveBackupFrequency.EVERY_6_HOURS.id, prefs.getString(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY, ""))
            assertEquals(true, prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false))
            assertEquals(20, prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, 0))
        } finally {
            restoreDb.close()
        }
    }

    @Test
    fun `drive auto backup schedule check evaluates daily and interval triggers accurately`() {
        val t2100 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 21, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val t2235 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 22, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Not connected -> false
        assertFalse(
            DriveAutoBackupManager.shouldRunScheduledBackupFromService(
                enabled = true,
                connected = false,
                frequency = DriveBackupFrequency.DAILY_SCHEDULED,
                hour = 22,
                minute = 30,
                lastBackupDate = "",
                lastBackupTimestamp = 0L,
                nowMillis = t2235
            )
        )

        // Before scheduled time 22:30 -> false
        assertFalse(
            DriveAutoBackupManager.shouldRunScheduledBackupFromService(
                enabled = true,
                connected = true,
                frequency = DriveBackupFrequency.DAILY_SCHEDULED,
                hour = 22,
                minute = 30,
                lastBackupDate = "",
                lastBackupTimestamp = 0L,
                nowMillis = t2100
            )
        )

        // Past 22:30 and not yet backed up today -> true
        assertTrue(
            DriveAutoBackupManager.shouldRunScheduledBackupFromService(
                enabled = true,
                connected = true,
                frequency = DriveBackupFrequency.DAILY_SCHEDULED,
                hour = 22,
                minute = 30,
                lastBackupDate = "2026-11-19",
                lastBackupTimestamp = 0L,
                nowMillis = t2235
            )
        )

        // Already backed up today -> false
        assertFalse(
            DriveAutoBackupManager.shouldRunScheduledBackupFromService(
                enabled = true,
                connected = true,
                frequency = DriveBackupFrequency.DAILY_SCHEDULED,
                hour = 22,
                minute = 30,
                lastBackupDate = "2026-11-20",
                lastBackupTimestamp = t2235 - 60_000L,
                nowMillis = t2235
            )
        )

        // 6 hour interval elapsed -> true
        assertTrue(
            DriveAutoBackupManager.shouldRunScheduledBackupFromService(
                enabled = true,
                connected = true,
                frequency = DriveBackupFrequency.EVERY_6_HOURS,
                hour = 22,
                minute = 30,
                lastBackupDate = "2026-11-20",
                lastBackupTimestamp = t2235 - (7L * 3600L * 1000L),
                nowMillis = t2235
            )
        )
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `GoogleDriveBackupCard displays and updates auto backup preferences`() {
        val viewModel = PosViewModel(context)

        composeTestRule.setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                GoogleDriveBackupCard(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_drive_backup_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_connect_google_drive").assertIsDisplayed()
        composeTestRule.onNodeWithTag("switch_drive_auto_backup").assertIsDisplayed()

        // Selecting frequency chip updates ViewModel StateFlow
        composeTestRule.onNodeWithTag("chip_drive_freq_EVERY_12_HOURS").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(DriveBackupFrequency.EVERY_12_HOURS, viewModel.driveBackupFrequency.value)

        // Toggling Wi-Fi only & retention updates ViewModel StateFlow
        composeTestRule.onNodeWithTag("switch_drive_wifi_only").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("chip_drive_keep_5").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(viewModel.driveBackupWifiOnly.value)
        assertEquals(5, viewModel.driveBackupKeepCount.value)
    }
}
