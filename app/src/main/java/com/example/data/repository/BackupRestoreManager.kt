package com.example.data.repository

import android.content.Context
import android.net.Uri
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
import com.example.service.AutoStoreCloseManager
import com.example.service.DriveAutoBackupManager
import com.example.service.DriveBackupFrequency
import com.example.service.PosBackgroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupStats(
    val productCount: Int,
    val transactionCount: Int,
    val shiftCount: Int,
    val userCount: Int,
    val expenseCount: Int = 0,
    val dailyReportCount: Int = 0,
    val settingsRestored: Boolean = false,
    val filePath: String = "",
    val fileName: String = "",
    val fileUri: Uri? = null,
    val jsonString: String = "",
    val csvString: String = "",
    val format: String = "JSON",
    val skippedEditedOrDeletedCount: Int = 0,
    val skippedSettingsCount: Int = 0
)

class BackupRestoreManager(
    private val context: Context,
    private val database: PosDatabase
) {
    private data class SnapshotData(
        val rootJson: JSONObject,
        val productCount: Int,
        val transactionCount: Int,
        val shiftCount: Int,
        val userCount: Int,
        val expenseCount: Int,
        val dailyReportCount: Int
    )

    suspend fun exportBackup(): BackupStats = withContext(Dispatchers.IO) {
        val snapshot = buildSnapshotData()
        val jsonString = snapshot.rootJson.toString(2)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "backup_kasirpos_$timeStamp.json"

        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val backupFile = File(backupDir, fileName)
        FileOutputStream(backupFile).use { fos ->
            fos.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        BackupStats(
            productCount = snapshot.productCount,
            transactionCount = snapshot.transactionCount,
            shiftCount = snapshot.shiftCount,
            userCount = snapshot.userCount,
            expenseCount = snapshot.expenseCount,
            dailyReportCount = snapshot.dailyReportCount,
            settingsRestored = true,
            filePath = backupFile.absolutePath,
            fileName = fileName,
            fileUri = null,
            jsonString = jsonString,
            format = "JSON"
        )
    }

    suspend fun exportCsvBackup(): BackupStats = withContext(Dispatchers.IO) {
        val snapshot = buildSnapshotData()
        val csvString = CsvBackupCodec.encodeFromJsonSnapshot(snapshot.rootJson)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "backup_kasirpos_$timeStamp.csv"

        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val backupFile = File(backupDir, fileName)
        FileOutputStream(backupFile).use { fos ->
            fos.write(csvString.toByteArray(Charsets.UTF_8))
        }

        BackupStats(
            productCount = snapshot.productCount,
            transactionCount = snapshot.transactionCount,
            shiftCount = snapshot.shiftCount,
            userCount = snapshot.userCount,
            expenseCount = snapshot.expenseCount,
            dailyReportCount = snapshot.dailyReportCount,
            settingsRestored = true,
            filePath = backupFile.absolutePath,
            fileName = fileName,
            fileUri = null,
            csvString = csvString,
            format = "CSV"
        )
    }

    private suspend fun buildSnapshotData(): SnapshotData {
        val storeProfile = database.storeProfileDao().getStoreProfileSync() ?: StoreProfile()
        val cloudConfig = database.cloudHostingConfigDao().getConfigSync() ?: CloudHostingConfig()
        val products = database.productDao().getAllProductsList()
        val transactions = database.transactionDao().getAllTransactionsList()
        val transactionItems = database.transactionDao().getAllTransactionItemsList()
        val expenses = database.expenseDao().getAllExpensesList()
        val shiftClosings = database.shiftClosingDao().getAllShiftClosingsList()
        val users = database.userDao().getAllUsersList()
        val dailyReports = database.dailyReportDao().getAllDailyReportsList()
        val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)

        val rootJson = JSONObject().apply {
            put("app", "KasirPOS")
            put("version", 2)
            put("exportTimestamp", System.currentTimeMillis())
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // 1. Store Profile
            put("storeProfile", JSONObject().apply {
                put("storeName", storeProfile.storeName)
                put("address", storeProfile.address)
                put("phone", storeProfile.phone)
                put("ownerPhone", storeProfile.ownerPhone)
                put("receiptHeader", storeProfile.receiptHeader)
                put("receiptFooter", storeProfile.receiptFooter)
            })

            // 2. Cloud Hosting Config (Room)
            put("cloudHostingConfig", JSONObject().apply {
                put("useSeparateOwnerProject", cloudConfig.useSeparateOwnerProject)
                put("ownerProjectId", cloudConfig.ownerProjectId)
                put("ownerDatabaseId", cloudConfig.ownerDatabaseId)
                put("ownerHostingUrl", cloudConfig.ownerHostingUrl)
                put("enableConsumerWeb", cloudConfig.enableConsumerWeb)
                put("consumerProjectId", cloudConfig.consumerProjectId)
                put("consumerDatabaseId", cloudConfig.consumerDatabaseId)
                put("consumerWebUrl", cloudConfig.consumerWebUrl)
                put("enableEmailBackup", cloudConfig.enableEmailBackup)
                put("backupEmail", cloudConfig.backupEmail)
                put("autoBackupOnShiftClose", cloudConfig.autoBackupOnShiftClose)
                put("lastBackupTimestamp", cloudConfig.lastBackupTimestamp)
                put("enableLanServer", cloudConfig.enableLanServer)
                put("lanPort", cloudConfig.lanPort)
                put("lanIpAddress", cloudConfig.lanIpAddress)
                put("updatedAt", cloudConfig.updatedAt)
            })

            // 3. All App & Operational Settings (SharedPreferences)
            put("appSettings", JSONObject().apply {
                val customCategoriesRaw = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
                if (!customCategoriesRaw.isNullOrBlank()) {
                    put("customCategories", try { JSONArray(customCategoriesRaw) } catch (_: Throwable) { JSONArray() })
                }
                val customUnitsRaw = prefs.getString(KEY_CUSTOM_UNITS, null)
                if (!customUnitsRaw.isNullOrBlank()) {
                    put("customUnits", try { JSONArray(customUnitsRaw) } catch (_: Throwable) { JSONArray() })
                }
                put("foodDefaultUnit", prefs.getString(KEY_FOOD_DEFAULT_UNIT, "porsi") ?: "porsi")
                put("drinkDefaultUnit", prefs.getString(KEY_DRINK_DEFAULT_UNIT, "pcs") ?: "pcs")
                put("tableCount", prefs.getInt(KEY_TABLE_COUNT, 5))

                put("firebaseProjectId", prefs.getString("KEY_FIREBASE_PROJECT_ID", "foodrock-report") ?: "foodrock-report")
                put("firebaseDatabaseId", prefs.getString("KEY_FIREBASE_DATABASE_ID", "foodrock-report-default-rtdb") ?: "foodrock-report-default-rtdb")
                put("webHostingUrl", prefs.getString("KEY_WEB_HOSTING_URL", "http://127.0.0.1:8080/owner.html") ?: "http://127.0.0.1:8080/owner.html")

                put("bgServiceEnabled", prefs.getBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true))
                put("lowStockNotifEnabled", prefs.getBoolean(PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, true))

                put("autoCloseEnabled", prefs.getBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false))
                put("autoCloseHour", prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, AutoStoreCloseManager.DEFAULT_HOUR))
                put("autoCloseMinute", prefs.getInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, AutoStoreCloseManager.DEFAULT_MINUTE))

                put("driveAutoBackupEnabled", prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, false))
                put(
                    "driveAutoBackupFrequency",
                    prefs.getString(
                        DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY,
                        DriveBackupFrequency.DAILY_SCHEDULED.id
                    ) ?: DriveBackupFrequency.DAILY_SCHEDULED.id
                )
                put("driveAutoBackupHour", prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, DriveAutoBackupManager.DEFAULT_HOUR))
                put("driveAutoBackupMinute", prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, DriveAutoBackupManager.DEFAULT_MINUTE))
                put("driveAutoBackupWifiOnly", prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, false))
                put("driveAutoBackupOnShiftClose", prefs.getBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, true))
                put("driveAutoBackupKeepCount", prefs.getInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, DriveAutoBackupManager.DEFAULT_KEEP_COUNT))
                put("driveAccountEmail", prefs.getString(DriveAutoBackupManager.KEY_DRIVE_ACCOUNT_EMAIL, "") ?: "")
            })

            // 4. Products
            val productsArray = JSONArray()
            for (p in products) {
                productsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("barcode", p.barcode)
                    put("buyPrice", p.buyPrice)
                    put("sellPrice", p.sellPrice)
                    put("stock", p.stock)
                    put("minStockAlert", p.minStockAlert)
                    put("unit", p.unit)
                    put("description", p.description)
                    put("isAvailable", p.isAvailable)
                    put("updatedAt", p.updatedAt)
                })
            }
            put("products", productsArray)

            // 5. Transactions
            val transactionsArray = JSONArray()
            for (t in transactions) {
                transactionsArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("invoiceNumber", t.invoiceNumber)
                    put("cashierId", t.cashierId)
                    put("cashierName", t.cashierName)
                    put("timestamp", t.timestamp)
                    put("subtotal", t.subtotal)
                    put("discount", t.discount)
                    put("tax", t.tax)
                    put("grandTotal", t.grandTotal)
                    put("paymentMethod", t.paymentMethod.name)
                    put("amountPaid", t.amountPaid)
                    put("changeAmount", t.changeAmount)
                    put("customerName", t.customerName)
                    put("orderType", t.orderType.name)
                    put("tableNumber", t.tableNumber)
                    put("notes", t.notes)
                    put("isSynced", t.isSynced)
                    put("syncedAt", t.syncedAt ?: JSONObject.NULL)
                    put("profitAmount", t.profitAmount)
                    put("totalItemsCount", t.totalItemsCount)
                })
            }
            put("transactions", transactionsArray)

            // 6. Transaction Items
            val itemsArray = JSONArray()
            for (ti in transactionItems) {
                itemsArray.put(JSONObject().apply {
                    put("id", ti.id)
                    put("transactionId", ti.transactionId)
                    put("productId", ti.productId)
                    put("productName", ti.productName)
                    put("unitPrice", ti.unitPrice)
                    put("costPrice", ti.costPrice)
                    put("quantity", ti.quantity)
                    put("subtotal", ti.subtotal)
                    put("itemNotes", ti.itemNotes)
                })
            }
            put("transactionItems", itemsArray)

            // 7. Expenses
            val expensesArray = JSONArray()
            for (e in expenses) {
                expensesArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("amount", e.amount)
                    put("category", e.category)
                    put("timestamp", e.timestamp)
                    put("notes", e.notes)
                    put("createdBy", e.createdBy)
                })
            }
            put("expenses", expensesArray)

            // 8. Shift Closings
            val shiftsArray = JSONArray()
            for (s in shiftClosings) {
                shiftsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("closingTime", s.closingTime)
                    put("cashierId", s.cashierId)
                    put("cashierName", s.cashierName)
                    put("totalTransactions", s.totalTransactions)
                    put("totalCash", s.totalCash)
                    put("totalNonCash", s.totalNonCash)
                    put("totalRevenue", s.totalRevenue)
                    put("expectedCash", s.expectedCash)
                    put("actualCash", s.actualCash)
                    put("difference", s.difference)
                    put("notes", s.notes)
                })
            }
            put("shiftClosings", shiftsArray)

            // 9. Users
            val usersArray = JSONArray()
            for (u in users) {
                usersArray.put(JSONObject().apply {
                    put("id", u.id)
                    put("username", u.username)
                    put("pin", u.pin)
                    put("fullName", u.fullName)
                    put("role", u.role.name)
                    put("phone", u.phone)
                    put("isActive", u.isActive)
                    put("createdAt", u.createdAt)
                })
            }
            put("users", usersArray)

            // 10. Daily Reports
            val dailyReportsArray = JSONArray()
            for (dr in dailyReports) {
                dailyReportsArray.put(JSONObject().apply {
                    put("id", dr.id)
                    put("date", dr.date)
                    put("dateTimestamp", dr.dateTimestamp)
                    put("totalRevenue", dr.totalRevenue)
                    put("totalProfit", dr.totalProfit)
                    put("totalTransactions", dr.totalTransactions)
                    put("totalItemsSold", dr.totalItemsSold)
                    put("totalCash", dr.totalCash)
                    put("totalNonCash", dr.totalNonCash)
                    put("totalQris", dr.totalQris)
                    put("totalTransfer", dr.totalTransfer)
                    put("totalDiscount", dr.totalDiscount)
                    put("totalTax", dr.totalTax)
                    put("expectedCash", dr.expectedCash)
                    put("actualCash", dr.actualCash)
                    put("cashDifference", dr.cashDifference)
                    put("topProductSummary", dr.topProductSummary)
                    put("cashierNames", dr.cashierNames)
                    put("shiftCount", dr.shiftCount)
                    put("notes", dr.notes)
                    put("isSynced", dr.isSynced)
                    put("syncedAt", dr.syncedAt ?: JSONObject.NULL)
                })
            }
            put("dailyReports", dailyReportsArray)
        }

        return SnapshotData(
            rootJson = rootJson,
            productCount = products.size,
            transactionCount = transactions.size,
            shiftCount = shiftClosings.size,
            userCount = users.size,
            expenseCount = expenses.size,
            dailyReportCount = dailyReports.size
        )
    }

    suspend fun restoreFromCsvString(
        csvString: String,
        preserveEditedAndDeleted: Boolean = true
    ): Result<BackupStats> = withContext(Dispatchers.IO) {
        val decoded = CsvBackupCodec.decodeCsvToJsonSnapshot(csvString)
        if (decoded.isFailure) {
            return@withContext Result.failure(
                decoded.exceptionOrNull() ?: IllegalArgumentException("Format CSV tidak valid")
            )
        }
        val jsonObj = decoded.getOrThrow()
        val restoreResult = restoreFromJsonString(
            jsonString = jsonObj.toString(),
            preserveEditedAndDeleted = preserveEditedAndDeleted
        )
        restoreResult.map { stats -> stats.copy(format = "CSV") }
    }

    suspend fun restoreFromJsonString(
        jsonString: String,
        preserveEditedAndDeleted: Boolean = true
    ): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var settingsRestored = false
            var skippedItemsCount = 0
            var skippedSettingsCount = 0

            // 1. Restore Store Profile (skip if locally edited)
            if (root.has("storeProfile")) {
                val skipStoreProfile = preserveEditedAndDeleted &&
                    RestoreGuardTracker.isSettingEdited(context, RestoreGuardTracker.SETTING_STORE_PROFILE)
                if (skipStoreProfile) {
                    skippedSettingsCount++
                } else {
                    val spObj = root.getJSONObject("storeProfile")
                    val storeProfile = StoreProfile(
                        id = 1,
                        storeName = spObj.optString("storeName", "KASIR POS"),
                        address = spObj.optString("address", ""),
                        phone = spObj.optString("phone", ""),
                        ownerPhone = spObj.optString("ownerPhone", ""),
                        receiptHeader = spObj.optString("receiptHeader", "STRUK PEMBELIAN"),
                        receiptFooter = spObj.optString("receiptFooter", "Terima kasih!")
                    )
                    database.storeProfileDao().insertStoreProfile(storeProfile)
                    settingsRestored = true
                }
            }

            // 2. Restore Cloud Hosting Config (skip if locally edited)
            if (root.has("cloudHostingConfig")) {
                val skipCloudHosting = preserveEditedAndDeleted &&
                    RestoreGuardTracker.isSettingEdited(context, RestoreGuardTracker.SETTING_CLOUD_HOSTING)
                if (skipCloudHosting) {
                    skippedSettingsCount++
                } else {
                    val cc = root.getJSONObject("cloudHostingConfig")
                    val current = database.cloudHostingConfigDao().getConfigSync() ?: CloudHostingConfig()
                    val restoredConfig = CloudHostingConfig(
                        id = 1,
                        useSeparateOwnerProject = cc.optBoolean("useSeparateOwnerProject", current.useSeparateOwnerProject),
                        ownerProjectId = cc.optString("ownerProjectId", current.ownerProjectId),
                        ownerDatabaseId = cc.optString("ownerDatabaseId", current.ownerDatabaseId),
                        ownerHostingUrl = cc.optString("ownerHostingUrl", current.ownerHostingUrl),
                        enableConsumerWeb = cc.optBoolean("enableConsumerWeb", current.enableConsumerWeb),
                        consumerProjectId = cc.optString("consumerProjectId", current.consumerProjectId),
                        consumerDatabaseId = cc.optString("consumerDatabaseId", current.consumerDatabaseId),
                        consumerWebUrl = cc.optString("consumerWebUrl", current.consumerWebUrl),
                        enableEmailBackup = cc.optBoolean("enableEmailBackup", current.enableEmailBackup),
                        backupEmail = cc.optString("backupEmail", current.backupEmail),
                        autoBackupOnShiftClose = cc.optBoolean("autoBackupOnShiftClose", current.autoBackupOnShiftClose),
                        lastBackupTimestamp = cc.optLong("lastBackupTimestamp", current.lastBackupTimestamp),
                        enableLanServer = cc.optBoolean("enableLanServer", current.enableLanServer),
                        lanPort = cc.optInt("lanPort", current.lanPort),
                        lanIpAddress = cc.optString("lanIpAddress", current.lanIpAddress),
                        updatedAt = cc.optLong("updatedAt", System.currentTimeMillis())
                    )
                    database.cloudHostingConfigDao().insertOrUpdateConfig(restoredConfig)
                    settingsRestored = true
                }
            }

            // 3. Restore SharedPreferences App Settings (granular per-setting guard)
            if (root.has("appSettings")) {
                val s = root.getJSONObject("appSettings")
                val prefs = context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                var anyAppSettingApplied = false

                fun canRestoreSetting(settingKey: String): Boolean {
                    if (!preserveEditedAndDeleted) return true
                    val blocked = RestoreGuardTracker.isSettingEdited(context, settingKey)
                    if (blocked) skippedSettingsCount++
                    return !blocked
                }

                s.optJSONArray("customCategories")?.let { arr ->
                    val backupCats = parseJsonStringList(arr)
                    val deletedCats = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedCategories(context) else emptySet()
                    val renamedCats = if (preserveEditedAndDeleted) RestoreGuardTracker.getRenamedCategories(context) else emptyMap()
                    val isCatsEdited = preserveEditedAndDeleted &&
                        RestoreGuardTracker.isSettingEdited(context, RestoreGuardTracker.SETTING_CUSTOM_CATEGORIES)

                    if (!isCatsEdited && deletedCats.isEmpty() && renamedCats.isEmpty()) {
                        editor.putString(KEY_CUSTOM_CATEGORIES, toJsonStringArray(backupCats).toString())
                        anyAppSettingApplied = true
                    } else {
                        val currentLocalRaw = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
                        val merged = parseJsonStringListFromString(currentLocalRaw).toMutableList()
                        var blockedAnyCat = false
                        var addedUntouchedCat = false
                        for (cat in backupCats) {
                            val norm = cat.trim().lowercase(Locale.ROOT)
                            if (norm in deletedCats || norm in renamedCats.keys) {
                                blockedAnyCat = true
                                continue
                            }
                            if (merged.none { it.equals(cat, ignoreCase = true) }) {
                                merged.add(cat)
                                addedUntouchedCat = true
                            }
                        }
                        if (blockedAnyCat || isCatsEdited) {
                            skippedSettingsCount++
                        }
                        if (addedUntouchedCat) {
                            editor.putString(KEY_CUSTOM_CATEGORIES, toJsonStringArray(merged).toString())
                            anyAppSettingApplied = true
                        }
                    }
                }

                s.optJSONArray("customUnits")?.let { arr ->
                    val backupUnits = parseJsonStringList(arr)
                    val deletedUnits = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedUnits(context) else emptySet()
                    val isUnitsEdited = preserveEditedAndDeleted &&
                        RestoreGuardTracker.isSettingEdited(context, RestoreGuardTracker.SETTING_CUSTOM_UNITS)

                    if (!isUnitsEdited && deletedUnits.isEmpty()) {
                        editor.putString(KEY_CUSTOM_UNITS, toJsonStringArray(backupUnits).toString())
                        anyAppSettingApplied = true
                    } else {
                        val currentLocalRaw = prefs.getString(KEY_CUSTOM_UNITS, null)
                        val merged = parseJsonStringListFromString(currentLocalRaw).toMutableList()
                        var blockedAnyUnit = false
                        var addedUntouchedUnit = false
                        for (unit in backupUnits) {
                            val norm = unit.trim().lowercase(Locale.ROOT)
                            if (norm in deletedUnits) {
                                blockedAnyUnit = true
                                continue
                            }
                            if (merged.none { it.equals(unit, ignoreCase = true) }) {
                                merged.add(unit)
                                addedUntouchedUnit = true
                            }
                        }
                        if (blockedAnyUnit || isUnitsEdited) {
                            skippedSettingsCount++
                        }
                        if (addedUntouchedUnit) {
                            editor.putString(KEY_CUSTOM_UNITS, toJsonStringArray(merged).toString())
                            anyAppSettingApplied = true
                        }
                    }
                }

                if (s.has("foodDefaultUnit") && canRestoreSetting(RestoreGuardTracker.SETTING_FOOD_DEFAULT_UNIT)) {
                    editor.putString(KEY_FOOD_DEFAULT_UNIT, s.optString("foodDefaultUnit", "porsi"))
                    anyAppSettingApplied = true
                }
                if (s.has("drinkDefaultUnit") && canRestoreSetting(RestoreGuardTracker.SETTING_DRINK_DEFAULT_UNIT)) {
                    editor.putString(KEY_DRINK_DEFAULT_UNIT, s.optString("drinkDefaultUnit", "pcs"))
                    anyAppSettingApplied = true
                }
                if (s.has("tableCount") && canRestoreSetting(RestoreGuardTracker.SETTING_TABLE_COUNT)) {
                    editor.putInt(KEY_TABLE_COUNT, s.optInt("tableCount", 5).coerceIn(0, 100))
                    anyAppSettingApplied = true
                }
                if (s.has("firebaseProjectId") && canRestoreSetting(RestoreGuardTracker.SETTING_FIREBASE_PROJECT_ID)) {
                    editor.putString("KEY_FIREBASE_PROJECT_ID", s.optString("firebaseProjectId", "foodrock-report"))
                    anyAppSettingApplied = true
                }
                if (s.has("firebaseDatabaseId") && canRestoreSetting(RestoreGuardTracker.SETTING_FIREBASE_DATABASE_ID)) {
                    editor.putString("KEY_FIREBASE_DATABASE_ID", s.optString("firebaseDatabaseId", "foodrock-report-default-rtdb"))
                    anyAppSettingApplied = true
                }
                if (s.has("webHostingUrl") && canRestoreSetting(RestoreGuardTracker.SETTING_WEB_HOSTING_URL)) {
                    editor.putString("KEY_WEB_HOSTING_URL", s.optString("webHostingUrl", "http://127.0.0.1:8080/owner.html"))
                    anyAppSettingApplied = true
                }
                if (s.has("bgServiceEnabled") && canRestoreSetting(RestoreGuardTracker.SETTING_BG_SERVICE_ENABLED)) {
                    editor.putBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, s.optBoolean("bgServiceEnabled", true))
                    anyAppSettingApplied = true
                }
                if (s.has("lowStockNotifEnabled") && canRestoreSetting(RestoreGuardTracker.SETTING_LOW_STOCK_NOTIF_ENABLED)) {
                    editor.putBoolean(PosBackgroundService.KEY_LOW_STOCK_NOTIF_ENABLED, s.optBoolean("lowStockNotifEnabled", true))
                    anyAppSettingApplied = true
                }
                if ((s.has("autoCloseEnabled") || s.has("autoCloseHour") || s.has("autoCloseMinute")) &&
                    canRestoreSetting(RestoreGuardTracker.SETTING_AUTO_CLOSE)
                ) {
                    if (s.has("autoCloseEnabled")) {
                        editor.putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, s.optBoolean("autoCloseEnabled", false))
                    }
                    if (s.has("autoCloseHour")) {
                        editor.putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_HOUR, s.optInt("autoCloseHour", AutoStoreCloseManager.DEFAULT_HOUR).coerceIn(0, 23))
                    }
                    if (s.has("autoCloseMinute")) {
                        editor.putInt(AutoStoreCloseManager.KEY_AUTO_CLOSE_MINUTE, s.optInt("autoCloseMinute", AutoStoreCloseManager.DEFAULT_MINUTE).coerceIn(0, 59))
                    }
                    anyAppSettingApplied = true
                }
                if (s.has("driveAutoBackupEnabled") && canRestoreSetting(RestoreGuardTracker.SETTING_DRIVE_AUTO_BACKUP_ENABLED)) {
                    editor.putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ENABLED, s.optBoolean("driveAutoBackupEnabled", false))
                    anyAppSettingApplied = true
                }
                if (s.has("driveAutoBackupFrequency") && canRestoreSetting(RestoreGuardTracker.SETTING_DRIVE_AUTO_BACKUP_FREQUENCY)) {
                    editor.putString(
                        DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_FREQUENCY,
                        DriveBackupFrequency.fromId(s.optString("driveAutoBackupFrequency")).id
                    )
                    anyAppSettingApplied = true
                }
                if ((s.has("driveAutoBackupHour") || s.has("driveAutoBackupMinute")) &&
                    canRestoreSetting(RestoreGuardTracker.SETTING_DRIVE_AUTO_BACKUP_TIME)
                ) {
                    if (s.has("driveAutoBackupHour")) {
                        editor.putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_HOUR, s.optInt("driveAutoBackupHour", DriveAutoBackupManager.DEFAULT_HOUR).coerceIn(0, 23))
                    }
                    if (s.has("driveAutoBackupMinute")) {
                        editor.putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_MINUTE, s.optInt("driveAutoBackupMinute", DriveAutoBackupManager.DEFAULT_MINUTE).coerceIn(0, 59))
                    }
                    anyAppSettingApplied = true
                }
                if (s.has("driveAutoBackupWifiOnly") && canRestoreSetting(RestoreGuardTracker.SETTING_DRIVE_AUTO_BACKUP_WIFI_ONLY)) {
                    editor.putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_WIFI_ONLY, s.optBoolean("driveAutoBackupWifiOnly", false))
                    anyAppSettingApplied = true
                }
                if (s.has("driveAutoBackupOnShiftClose") && canRestoreSetting(RestoreGuardTracker.SETTING_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE)) {
                    editor.putBoolean(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE, s.optBoolean("driveAutoBackupOnShiftClose", true))
                    anyAppSettingApplied = true
                }
                if (s.has("driveAutoBackupKeepCount") && canRestoreSetting(RestoreGuardTracker.SETTING_DRIVE_AUTO_BACKUP_KEEP_COUNT)) {
                    editor.putInt(DriveAutoBackupManager.KEY_DRIVE_AUTO_BACKUP_KEEP_COUNT, s.optInt("driveAutoBackupKeepCount", DriveAutoBackupManager.DEFAULT_KEEP_COUNT).coerceIn(0, 100))
                    anyAppSettingApplied = true
                }
                if (anyAppSettingApplied) {
                    editor.apply()
                    settingsRestored = true
                }
            }

            // 4. Restore Products (skip locally deleted or edited products)
            val restoredProducts = mutableListOf<Product>()
            if (root.has("products")) {
                val array = root.getJSONArray("products")
                val existingProducts = database.productDao().getAllProductsList()
                val existingById = existingProducts.associateBy { it.id }
                val existingByName = existingProducts.associateBy { it.name.trim().lowercase(Locale.ROOT) }
                val deletedIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedProductIds(context) else emptySet()
                val deletedNames = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedProductNames(context) else emptySet()
                val editedIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getEditedProductIds(context) else emptySet()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val product = Product(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", "Produk"),
                        category = obj.optString("category", "Umum"),
                        barcode = obj.optString("barcode", ""),
                        buyPrice = obj.optDouble("buyPrice", 0.0),
                        sellPrice = obj.optDouble("sellPrice", 0.0),
                        stock = obj.optInt("stock", 0),
                        minStockAlert = obj.optInt("minStockAlert", 5),
                        unit = obj.optString("unit", "pcs"),
                        description = obj.optString("description", ""),
                        isAvailable = obj.optBoolean("isAvailable", true),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    if (preserveEditedAndDeleted) {
                        val normName = product.name.trim().lowercase(Locale.ROOT)
                        val existingByIdMatch = existingById[product.id]
                        val existingByNameMatch = existingByName[normName]
                        val isDeleted = (product.id > 0L && product.id in deletedIds) ||
                            (normName.isNotBlank() && normName in deletedNames)
                        val isEdited = (product.id > 0L && product.id in editedIds) ||
                            (existingByNameMatch != null && existingByNameMatch.id in editedIds) ||
                            (existingByIdMatch != null && existingByIdMatch.updatedAt > product.updatedAt) ||
                            (existingByNameMatch != null && existingByNameMatch.id != product.id)
                        if (isDeleted || isEdited) {
                            skippedItemsCount++
                            continue
                        }
                    }
                    restoredProducts.add(product)
                }
                if (restoredProducts.isNotEmpty()) {
                    database.productDao().insertProducts(restoredProducts)
                }
            }

            // 5. Restore Transactions (skip locally deleted transactions)
            val restoredTrxs = mutableListOf<Transaction>()
            val existingTransactions = database.transactionDao().getAllTransactionsList()
            val deletedTrxIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedTransactionIds(context) else emptySet()
            val deletedTrxInvoices = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedTransactionInvoices(context) else emptySet()
            val restoredTrxIds = mutableSetOf<Long>()
            val skippedTrxIds = mutableSetOf<Long>()

            if (root.has("transactions")) {
                val array = root.getJSONArray("transactions")
                val existingById = existingTransactions.associateBy { it.id }
                val existingByInvoice = existingTransactions.associateBy { it.invoiceNumber.trim().lowercase(Locale.ROOT) }
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pMethod = try {
                        PaymentMethod.valueOf(obj.optString("paymentMethod", "TUNAI"))
                    } catch (_: Exception) {
                        PaymentMethod.TUNAI
                    }
                    val oType = try {
                        OrderType.valueOf(obj.optString("orderType", "DINE_IN"))
                    } catch (_: Exception) {
                        OrderType.DINE_IN
                    }
                    val syncedAtVal = if (obj.has("syncedAt") && !obj.isNull("syncedAt")) {
                        obj.optLong("syncedAt")
                    } else null
                    val trx = Transaction(
                        id = obj.optLong("id", 0),
                        invoiceNumber = obj.optString("invoiceNumber", "INV"),
                        cashierId = obj.optLong("cashierId", 1),
                        cashierName = obj.optString("cashierName", "Kasir"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        discount = obj.optDouble("discount", 0.0),
                        tax = obj.optDouble("tax", 0.0),
                        grandTotal = obj.optDouble("grandTotal", 0.0),
                        paymentMethod = pMethod,
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        changeAmount = obj.optDouble("changeAmount", 0.0),
                        customerName = obj.optString("customerName", "Pelanggan"),
                        orderType = oType,
                        tableNumber = obj.optString("tableNumber", ""),
                        notes = obj.optString("notes", ""),
                        isSynced = obj.optBoolean("isSynced", false),
                        syncedAt = syncedAtVal,
                        profitAmount = obj.optDouble("profitAmount", 0.0),
                        totalItemsCount = obj.optInt("totalItemsCount", 0)
                    )
                    if (preserveEditedAndDeleted) {
                        val normInv = trx.invoiceNumber.trim().lowercase(Locale.ROOT)
                        val existingMatch = existingById[trx.id] ?: existingByInvoice[normInv]
                        val isDeleted = (trx.id > 0L && trx.id in deletedTrxIds) ||
                            (normInv.isNotBlank() && normInv in deletedTrxInvoices)
                        val keepNewerLocal = existingMatch != null && existingMatch.isSynced && !trx.isSynced
                        if (isDeleted || keepNewerLocal) {
                            if (trx.id > 0L) skippedTrxIds.add(trx.id)
                            skippedItemsCount++
                            continue
                        }
                    }
                    restoredTrxs.add(trx)
                    if (trx.id > 0L) restoredTrxIds.add(trx.id)
                }
                if (restoredTrxs.isNotEmpty()) {
                    database.transactionDao().insertTransactions(restoredTrxs)
                }
            }

            // 6. Restore Transaction Items (only for non-deleted/non-skipped transactions)
            if (root.has("transactionItems")) {
                val array = root.getJSONArray("transactionItems")
                val restoredItems = mutableListOf<TransactionItem>()
                val hasTrxSection = root.has("transactions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val item = TransactionItem(
                        id = obj.optLong("id", 0),
                        transactionId = obj.optLong("transactionId", 0),
                        productId = obj.optLong("productId", 0),
                        productName = obj.optString("productName", ""),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        costPrice = obj.optDouble("costPrice", 0.0),
                        quantity = obj.optInt("quantity", 1),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        itemNotes = obj.optString("itemNotes", "")
                    )
                    if (preserveEditedAndDeleted) {
                        val parentDeleted = item.transactionId in deletedTrxIds || item.transactionId in skippedTrxIds
                        val parentMissingInSection = hasTrxSection && item.transactionId !in restoredTrxIds
                        if (parentDeleted || parentMissingInSection) {
                            continue
                        }
                    }
                    restoredItems.add(item)
                }
                if (restoredItems.isNotEmpty()) {
                    database.transactionDao().insertTransactionItems(restoredItems)
                }
            }

            // 7. Restore Expenses (skip locally deleted or edited expenses)
            val restoredExpenses = mutableListOf<Expense>()
            if (root.has("expenses")) {
                val array = root.getJSONArray("expenses")
                val existingExpenses = database.expenseDao().getAllExpensesList()
                val deletedExpenseIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedExpenseIds(context) else emptySet()
                val deletedExpenseFps = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedExpenseFingerprints(context) else emptySet()
                val editedExpenseIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getEditedExpenseIds(context) else emptySet()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val expense = Expense(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title", "Pengeluaran"),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "Operasional"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        notes = obj.optString("notes", ""),
                        createdBy = obj.optString("createdBy", "Admin")
                    )
                    if (preserveEditedAndDeleted) {
                        val fp = RestoreGuardTracker.expenseFingerprint(expense.title, expense.timestamp)
                        val isDeletedOrEdited = (expense.id > 0L && expense.id in deletedExpenseIds) ||
                            fp in deletedExpenseFps ||
                            (expense.id > 0L && expense.id in editedExpenseIds) ||
                            existingExpenses.any {
                                it.id != expense.id &&
                                    RestoreGuardTracker.expenseFingerprint(it.title, it.timestamp) == fp
                            }
                        if (isDeletedOrEdited) {
                            skippedItemsCount++
                            continue
                        }
                    }
                    restoredExpenses.add(expense)
                }
                if (restoredExpenses.isNotEmpty()) {
                    database.expenseDao().insertExpenses(restoredExpenses)
                }
            }

            // 8. Restore Shift Closings
            val restoredShifts = mutableListOf<ShiftClosing>()
            if (root.has("shiftClosings")) {
                val array = root.getJSONArray("shiftClosings")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    restoredShifts.add(
                        ShiftClosing(
                            id = obj.optLong("id", 0),
                            closingTime = obj.optLong("closingTime", System.currentTimeMillis()),
                            cashierId = obj.optLong("cashierId", 1),
                            cashierName = obj.optString("cashierName", "Kasir"),
                            totalTransactions = obj.optInt("totalTransactions", 0),
                            totalCash = obj.optDouble("totalCash", 0.0),
                            totalNonCash = obj.optDouble("totalNonCash", 0.0),
                            totalRevenue = obj.optDouble("totalRevenue", 0.0),
                            expectedCash = obj.optDouble("expectedCash", 0.0),
                            actualCash = obj.optDouble("actualCash", 0.0),
                            difference = obj.optDouble("difference", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (restoredShifts.isNotEmpty()) {
                    database.shiftClosingDao().insertShiftClosings(restoredShifts)
                }
            }

            // 9. Restore Users (skip locally deleted or edited users)
            val restoredUsers = mutableListOf<User>()
            if (root.has("users")) {
                val array = root.getJSONArray("users")
                val existingUsers = database.userDao().getAllUsersList()
                val existingByUsername = existingUsers.associateBy { it.username.trim().lowercase(Locale.ROOT) }
                val deletedUserIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedUserIds(context) else emptySet()
                val deletedUsernames = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedUsernames(context) else emptySet()
                val editedUserIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getEditedUserIds(context) else emptySet()
                val editedUsernames = if (preserveEditedAndDeleted) RestoreGuardTracker.getEditedUsernames(context) else emptySet()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val uRole = try {
                        UserRole.valueOf(obj.optString("role", "KASIR"))
                    } catch (_: Exception) {
                        UserRole.KASIR
                    }
                    val user = User(
                        id = obj.optLong("id", 0),
                        username = obj.optString("username", "user"),
                        pin = obj.optString("pin", "1234"),
                        fullName = obj.optString("fullName", "User"),
                        role = uRole,
                        phone = obj.optString("phone", ""),
                        isActive = obj.optBoolean("isActive", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    val normUsername = user.username.trim().lowercase(Locale.ROOT)
                    val existingMatch = existingByUsername[normUsername]
                    if (preserveEditedAndDeleted) {
                        val isDeletedOrEdited = (user.id > 0L && user.id in deletedUserIds) ||
                            (normUsername.isNotBlank() && normUsername in deletedUsernames) ||
                            (user.id > 0L && user.id in editedUserIds) ||
                            (normUsername.isNotBlank() && normUsername in editedUsernames) ||
                            (existingMatch != null && (existingMatch.id in editedUserIds || existingMatch.id != user.id))
                        if (isDeletedOrEdited) {
                            skippedItemsCount++
                            continue
                        }
                    }
                    val targetUser = if (existingMatch != null && existingMatch.id > 0L) {
                        user.copy(id = existingMatch.id, username = normUsername.ifBlank { user.username })
                    } else {
                        user.copy(username = normUsername.ifBlank { user.username })
                    }
                    restoredUsers.add(targetUser)
                }
                if (restoredUsers.isNotEmpty()) {
                    database.userDao().insertUsers(restoredUsers.distinctBy { it.username.trim().lowercase(Locale.ROOT) })
                    database.userDao().deduplicateUsersByUsername()
                }
            }

            // 10. Restore Daily Reports (skip locally deleted reports)
            val restoredReports = mutableListOf<DailyReport>()
            if (root.has("dailyReports")) {
                val array = root.getJSONArray("dailyReports")
                val existingReports = database.dailyReportDao().getAllDailyReportsList()
                val existingById = existingReports.associateBy { it.id }
                val existingByDate = existingReports.associateBy { it.date.trim() }
                val deletedReportIds = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedDailyReportIds(context) else emptySet()
                val deletedReportDates = if (preserveEditedAndDeleted) RestoreGuardTracker.getDeletedDailyReportDates(context) else emptySet()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val syncedAtVal = if (obj.has("syncedAt") && !obj.isNull("syncedAt")) {
                        obj.optLong("syncedAt")
                    } else null
                    val report = DailyReport(
                        id = obj.optLong("id", 0),
                        date = obj.optString("date", ""),
                        dateTimestamp = obj.optLong("dateTimestamp", System.currentTimeMillis()),
                        totalRevenue = obj.optDouble("totalRevenue", 0.0),
                        totalProfit = obj.optDouble("totalProfit", 0.0),
                        totalTransactions = obj.optInt("totalTransactions", 0),
                        totalItemsSold = obj.optInt("totalItemsSold", 0),
                        totalCash = obj.optDouble("totalCash", 0.0),
                        totalNonCash = obj.optDouble("totalNonCash", 0.0),
                        totalQris = obj.optDouble("totalQris", 0.0),
                        totalTransfer = obj.optDouble("totalTransfer", 0.0),
                        totalDiscount = obj.optDouble("totalDiscount", 0.0),
                        totalTax = obj.optDouble("totalTax", 0.0),
                        expectedCash = obj.optDouble("expectedCash", 0.0),
                        actualCash = obj.optDouble("actualCash", 0.0),
                        cashDifference = obj.optDouble("cashDifference", 0.0),
                        topProductSummary = obj.optString("topProductSummary", ""),
                        cashierNames = obj.optString("cashierNames", ""),
                        shiftCount = obj.optInt("shiftCount", 0),
                        notes = obj.optString("notes", ""),
                        isSynced = obj.optBoolean("isSynced", false),
                        syncedAt = syncedAtVal
                    )
                    if (preserveEditedAndDeleted) {
                        val cleanDate = report.date.trim()
                        val existingMatch = existingById[report.id] ?: existingByDate[cleanDate]
                        val isDeleted = (report.id > 0L && report.id in deletedReportIds) ||
                            (cleanDate.isNotBlank() && cleanDate in deletedReportDates)
                        val keepNewerLocal = existingMatch != null && existingMatch.isSynced && !report.isSynced
                        if (isDeleted || keepNewerLocal) {
                            skippedItemsCount++
                            continue
                        }
                    }
                    restoredReports.add(report)
                }
                if (restoredReports.isNotEmpty()) {
                    database.dailyReportDao().insertDailyReports(restoredReports)
                }
            }

            Result.success(
                BackupStats(
                    productCount = restoredProducts.size,
                    transactionCount = restoredTrxs.size,
                    shiftCount = restoredShifts.size,
                    userCount = restoredUsers.size,
                    expenseCount = restoredExpenses.size,
                    dailyReportCount = restoredReports.size,
                    settingsRestored = settingsRestored,
                    skippedEditedOrDeletedCount = skippedItemsCount,
                    skippedSettingsCount = skippedSettingsCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseJsonStringList(arr: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val v = arr.optString(i).trim()
            if (v.isNotBlank() && list.none { it.equals(v, ignoreCase = true) }) {
                list.add(v)
            }
        }
        return list
    }

    private fun parseJsonStringListFromString(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            parseJsonStringList(JSONArray(raw))
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun toJsonStringArray(values: List<String>): JSONArray {
        val arr = JSONArray()
        values.forEach { arr.put(it) }
        return arr
    }

    suspend fun readJsonFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Tidak dapat membuka file URI")
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    companion object {
        const val KEY_CUSTOM_CATEGORIES = "KEY_CUSTOM_CATEGORIES"
        const val KEY_CUSTOM_UNITS = "KEY_CUSTOM_UNITS"
        const val KEY_FOOD_DEFAULT_UNIT = "KEY_FOOD_DEFAULT_UNIT"
        const val KEY_DRINK_DEFAULT_UNIT = "KEY_DRINK_DEFAULT_UNIT"
        const val KEY_TABLE_COUNT = "KEY_TABLE_COUNT"
    }
}
