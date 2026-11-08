package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.PosDatabase
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ShiftClosing
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.User
import com.example.data.model.UserRole
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
    val filePath: String = "",
    val fileName: String = "",
    val fileUri: Uri? = null,
    val jsonString: String = ""
)

class BackupRestoreManager(
    private val context: Context,
    private val database: PosDatabase
) {
    suspend fun exportBackup(): BackupStats = withContext(Dispatchers.IO) {
        val storeProfile = database.storeProfileDao().getStoreProfileSync() ?: StoreProfile()
        val products = database.productDao().getAllProductsList()
        val transactions = database.transactionDao().getAllTransactionsList()
        val transactionItems = database.transactionDao().getAllTransactionItemsList()
        val shiftClosings = database.shiftClosingDao().getAllShiftClosingsList()
        val users = database.userDao().getAllUsersList()
        val dailyReports = database.dailyReportDao().getAllDailyReportsList()

        val rootJson = JSONObject().apply {
            put("app", "KasirPOS")
            put("version", 1)
            put("exportTimestamp", System.currentTimeMillis())
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // Store Profile
            put("storeProfile", JSONObject().apply {
                put("storeName", storeProfile.storeName)
                put("address", storeProfile.address)
                put("phone", storeProfile.phone)
                put("ownerPhone", storeProfile.ownerPhone)
                put("receiptHeader", storeProfile.receiptHeader)
                put("receiptFooter", storeProfile.receiptFooter)
            })

            // Products
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

            // Transactions
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
                })
            }
            put("transactions", transactionsArray)

            // Transaction Items
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

            // Shift Closings
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

            // Users
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

            // Daily Reports
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
                })
            }
            put("dailyReports", dailyReportsArray)
        }

        val jsonString = rootJson.toString(2)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "backup_kasirpos_$timeStamp.json"

        // Save to cache/files dir
        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val backupFile = File(backupDir, fileName)
        FileOutputStream(backupFile).use { fos ->
            fos.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        BackupStats(
            productCount = products.size,
            transactionCount = transactions.size,
            shiftCount = shiftClosings.size,
            userCount = users.size,
            filePath = backupFile.absolutePath,
            fileName = fileName,
            fileUri = null,
            jsonString = jsonString
        )
    }

    suspend fun restoreFromJsonString(jsonString: String): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Restore Store Profile
            if (root.has("storeProfile")) {
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
            }

            // Restore Products
            val restoredProducts = mutableListOf<Product>()
            if (root.has("products")) {
                val array = root.getJSONArray("products")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    restoredProducts.add(
                        Product(
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
                    )
                }
                if (restoredProducts.isNotEmpty()) {
                    database.productDao().insertProducts(restoredProducts)
                }
            }

            // Restore Transactions
            val restoredTrxs = mutableListOf<Transaction>()
            if (root.has("transactions")) {
                val array = root.getJSONArray("transactions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pMethod = try {
                        PaymentMethod.valueOf(obj.optString("paymentMethod", "TUNAI"))
                    } catch (e: Exception) {
                        PaymentMethod.TUNAI
                    }
                    val oType = try {
                        OrderType.valueOf(obj.optString("orderType", "DINE_IN"))
                    } catch (e: Exception) {
                        OrderType.DINE_IN
                    }
                    restoredTrxs.add(
                        Transaction(
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
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (restoredTrxs.isNotEmpty()) {
                    database.transactionDao().insertTransactions(restoredTrxs)
                }
            }

            // Restore Transaction Items
            if (root.has("transactionItems")) {
                val array = root.getJSONArray("transactionItems")
                val restoredItems = mutableListOf<TransactionItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    restoredItems.add(
                        TransactionItem(
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
                    )
                }
                if (restoredItems.isNotEmpty()) {
                    database.transactionDao().insertTransactionItems(restoredItems)
                }
            }

            // Restore Shift Closings
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

            // Restore Users
            val restoredUsers = mutableListOf<User>()
            if (root.has("users")) {
                val array = root.getJSONArray("users")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val uRole = try {
                        UserRole.valueOf(obj.optString("role", "KASIR"))
                    } catch (e: Exception) {
                        UserRole.KASIR
                    }
                    restoredUsers.add(
                        User(
                            id = obj.optLong("id", 0),
                            username = obj.optString("username", "user"),
                            pin = obj.optString("pin", "1234"),
                            fullName = obj.optString("fullName", "User"),
                            role = uRole,
                            phone = obj.optString("phone", ""),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (restoredUsers.isNotEmpty()) {
                    database.userDao().insertUsers(restoredUsers)
                }
            }

            // Restore Daily Reports
            if (root.has("dailyReports")) {
                val array = root.getJSONArray("dailyReports")
                val restoredReports = mutableListOf<com.example.data.model.DailyReport>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    restoredReports.add(
                        com.example.data.model.DailyReport(
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
                            isSynced = obj.optBoolean("isSynced", false)
                        )
                    )
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
                    userCount = restoredUsers.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readJsonFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Tidak dapat membuka file URI")
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
