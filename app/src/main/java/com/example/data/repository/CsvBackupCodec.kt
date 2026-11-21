package com.example.data.repository

import org.json.JSONArray
import org.json.JSONObject

/**
 * Codec CSV (RFC-4180) untuk ekspor dan pemulihan penuh seluruh pengaturan toko,
 * konfigurasi sistem, produk, transaksi beserta itemnya, pengeluaran, tutup shift,
 * akun staf, dan laporan harian secara offline.
 */
object CsvBackupCodec {

    private const val UTF8_BOM = "\uFEFF"

    private const val SECTION_META_HEADER = "META_HEADER"
    private const val SECTION_META = "META"
    private const val SECTION_SETTING_HEADER = "SETTING_HEADER"
    private const val SECTION_SETTING = "SETTING"
    private const val SECTION_PRODUCT_HEADER = "PRODUCT_HEADER"
    private const val SECTION_PRODUCT = "PRODUCT"
    private const val SECTION_TRANSACTION_HEADER = "TRANSACTION_HEADER"
    private const val SECTION_TRANSACTION = "TRANSACTION"
    private const val SECTION_TRANSACTION_ITEM_HEADER = "TRANSACTION_ITEM_HEADER"
    private const val SECTION_TRANSACTION_ITEM = "TRANSACTION_ITEM"
    private const val SECTION_EXPENSE_HEADER = "EXPENSE_HEADER"
    private const val SECTION_EXPENSE = "EXPENSE"
    private const val SECTION_SHIFT_CLOSING_HEADER = "SHIFT_CLOSING_HEADER"
    private const val SECTION_SHIFT_CLOSING = "SHIFT_CLOSING"
    private const val SECTION_USER_HEADER = "USER_HEADER"
    private const val SECTION_USER = "USER"
    private const val SECTION_DAILY_REPORT_HEADER = "DAILY_REPORT_HEADER"
    private const val SECTION_DAILY_REPORT = "DAILY_REPORT"

    private val SETTING_COLUMNS = listOf("group", "key", "value")

    private val PRODUCT_COLUMNS = listOf(
        "id", "name", "category", "barcode", "buyPrice", "sellPrice",
        "stock", "minStockAlert", "unit", "description", "isAvailable", "updatedAt"
    )

    private val TRANSACTION_COLUMNS = listOf(
        "id", "invoiceNumber", "cashierId", "cashierName", "timestamp",
        "subtotal", "discount", "tax", "grandTotal", "paymentMethod",
        "amountPaid", "changeAmount", "customerName", "orderType", "tableNumber",
        "notes", "isSynced", "syncedAt", "profitAmount", "totalItemsCount"
    )

    private val TRANSACTION_ITEM_COLUMNS = listOf(
        "id", "transactionId", "productId", "productName",
        "unitPrice", "costPrice", "quantity", "subtotal", "itemNotes"
    )

    private val EXPENSE_COLUMNS = listOf(
        "id", "title", "amount", "category", "timestamp", "notes", "createdBy"
    )

    private val SHIFT_CLOSING_COLUMNS = listOf(
        "id", "closingTime", "cashierId", "cashierName", "totalTransactions",
        "totalCash", "totalNonCash", "totalRevenue", "expectedCash", "actualCash",
        "difference", "notes"
    )

    private val USER_COLUMNS = listOf(
        "id", "username", "pin", "fullName", "role", "phone", "isActive", "createdAt"
    )

    private val DAILY_REPORT_COLUMNS = listOf(
        "id", "date", "dateTimestamp", "totalRevenue", "totalProfit",
        "totalTransactions", "totalItemsSold", "totalCash", "totalNonCash",
        "totalQris", "totalTransfer", "totalDiscount", "totalTax",
        "expectedCash", "actualCash", "cashDifference", "topProductSummary",
        "cashierNames", "shiftCount", "notes", "isSynced", "syncedAt"
    )

    fun encodeFromJsonSnapshot(root: JSONObject, includeBom: Boolean = true): String {
        val sb = StringBuilder()
        if (includeBom) {
            sb.append(UTF8_BOM)
        }

        // 1. Metadata
        appendRow(sb, listOf(SECTION_META_HEADER, "app", "version", "exportTimestamp", "exportDate"))
        appendRow(
            sb,
            listOf(
                SECTION_META,
                root.optString("app", "KasirPOS"),
                root.optInt("version", 2).toString(),
                root.optLong("exportTimestamp", System.currentTimeMillis()).toString(),
                root.optString("exportDate", "")
            )
        )

        // 2. Settings (storeProfile, cloudHostingConfig, appSettings)
        appendRow(sb, listOf(SECTION_SETTING_HEADER) + SETTING_COLUMNS)
        val settingGroups = listOf("storeProfile", "cloudHostingConfig", "appSettings")
        for (groupName in settingGroups) {
            val groupObj = root.optJSONObject(groupName) ?: continue
            val keys = groupObj.keys()
            val sortedKeys = mutableListOf<String>()
            while (keys.hasNext()) {
                sortedKeys.add(keys.next())
            }
            sortedKeys.sort()
            for (key in sortedKeys) {
                val rawValue = groupObj.opt(key)
                val valueStr = when (rawValue) {
                    null, JSONObject.NULL -> ""
                    else -> rawValue.toString()
                }
                appendRow(sb, listOf(SECTION_SETTING, groupName, key, valueStr))
            }
        }

        // 3. Products
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_PRODUCT_HEADER,
            rowTag = SECTION_PRODUCT,
            columns = PRODUCT_COLUMNS,
            array = root.optJSONArray("products")
        )

        // 4. Transactions
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_TRANSACTION_HEADER,
            rowTag = SECTION_TRANSACTION,
            columns = TRANSACTION_COLUMNS,
            array = root.optJSONArray("transactions")
        )

        // 5. Transaction Items
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_TRANSACTION_ITEM_HEADER,
            rowTag = SECTION_TRANSACTION_ITEM,
            columns = TRANSACTION_ITEM_COLUMNS,
            array = root.optJSONArray("transactionItems")
        )

        // 6. Expenses
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_EXPENSE_HEADER,
            rowTag = SECTION_EXPENSE,
            columns = EXPENSE_COLUMNS,
            array = root.optJSONArray("expenses")
        )

        // 7. Shift Closings
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_SHIFT_CLOSING_HEADER,
            rowTag = SECTION_SHIFT_CLOSING,
            columns = SHIFT_CLOSING_COLUMNS,
            array = root.optJSONArray("shiftClosings")
        )

        // 8. Users
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_USER_HEADER,
            rowTag = SECTION_USER,
            columns = USER_COLUMNS,
            array = root.optJSONArray("users")
        )

        // 9. Daily Reports
        appendObjectSection(
            sb = sb,
            headerTag = SECTION_DAILY_REPORT_HEADER,
            rowTag = SECTION_DAILY_REPORT,
            columns = DAILY_REPORT_COLUMNS,
            array = root.optJSONArray("dailyReports")
        )

        return sb.toString()
    }

    fun decodeCsvToJsonSnapshot(rawCsv: String): Result<JSONObject> {
        return try {
            val cleaned = rawCsv.removePrefix(UTF8_BOM)
            if (cleaned.isBlank()) {
                return Result.failure(IllegalArgumentException("File CSV kosong"))
            }

            val delimiter = detectDelimiter(cleaned)
            val rows = parseCsvRows(cleaned, delimiter)
            if (rows.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tidak ada baris CSV yang valid"))
            }

            val root = JSONObject()
            val storeProfileObj = JSONObject()
            val cloudHostingObj = JSONObject()
            val appSettingsObj = JSONObject()

            val productsArr = JSONArray()
            val transactionsArr = JSONArray()
            val transactionItemsArr = JSONArray()
            val expensesArr = JSONArray()
            val shiftClosingsArr = JSONArray()
            val usersArr = JSONArray()
            val dailyReportsArr = JSONArray()

            var settingHeaders = SETTING_COLUMNS
            var productHeaders = PRODUCT_COLUMNS
            var transactionHeaders = TRANSACTION_COLUMNS
            var transactionItemHeaders = TRANSACTION_ITEM_COLUMNS
            var expenseHeaders = EXPENSE_COLUMNS
            var shiftClosingHeaders = SHIFT_CLOSING_COLUMNS
            var userHeaders = USER_COLUMNS
            var dailyReportHeaders = DAILY_REPORT_COLUMNS

            var recognizedSections = 0

            for (row in rows) {
                if (row.all { it.isBlank() }) continue
                val section = row[0].trim()
                val values = if (row.size > 1) row.subList(1, row.size) else emptyList()

                when (section) {
                    SECTION_META_HEADER -> recognizedSections++
                    SECTION_META -> {
                        recognizedSections++
                        root.put("app", values.getOrNull(0).orEmpty().ifBlank { "KasirPOS" })
                        root.put("version", values.getOrNull(1)?.toIntOrNull() ?: 2)
                        root.put("exportTimestamp", values.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis())
                        root.put("exportDate", values.getOrNull(3).orEmpty())
                    }

                    SECTION_SETTING_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) settingHeaders = values.map { it.trim() }
                    }
                    SECTION_SETTING -> {
                        recognizedSections++
                        val map = mapRowValues(settingHeaders, values)
                        val group = map["group"]?.trim().orEmpty()
                        val key = map["key"]?.trim().orEmpty()
                        val rawVal = map["value"] ?: ""
                        if (key.isNotEmpty()) {
                            when (group) {
                                "storeProfile" -> storeProfileObj.put(key, rawVal)
                                "cloudHostingConfig" -> putTypedCloudSetting(cloudHostingObj, key, rawVal)
                                "appSettings" -> putTypedAppSetting(appSettingsObj, key, rawVal)
                            }
                        }
                    }

                    SECTION_PRODUCT_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) productHeaders = values.map { it.trim() }
                    }
                    SECTION_PRODUCT -> {
                        recognizedSections++
                        val map = mapRowValues(productHeaders, values)
                        productsArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("name", map["name"] ?: "Produk")
                            put("category", map["category"] ?: "Umum")
                            put("barcode", map["barcode"] ?: "")
                            put("buyPrice", map["buyPrice"]?.toDoubleOrNull() ?: 0.0)
                            put("sellPrice", map["sellPrice"]?.toDoubleOrNull() ?: 0.0)
                            put("stock", map["stock"]?.toIntOrNull() ?: 0)
                            put("minStockAlert", map["minStockAlert"]?.toIntOrNull() ?: 5)
                            put("unit", map["unit"] ?: "pcs")
                            put("description", map["description"] ?: "")
                            put("isAvailable", parseBoolean(map["isAvailable"], true))
                            put("updatedAt", map["updatedAt"]?.toLongOrNull() ?: System.currentTimeMillis())
                        })
                    }

                    SECTION_TRANSACTION_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) transactionHeaders = values.map { it.trim() }
                    }
                    SECTION_TRANSACTION -> {
                        recognizedSections++
                        val map = mapRowValues(transactionHeaders, values)
                        val syncedAtLong = map["syncedAt"]?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull()
                        transactionsArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("invoiceNumber", map["invoiceNumber"] ?: "INV")
                            put("cashierId", map["cashierId"]?.toLongOrNull() ?: 1L)
                            put("cashierName", map["cashierName"] ?: "Kasir")
                            put("timestamp", map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis())
                            put("subtotal", map["subtotal"]?.toDoubleOrNull() ?: 0.0)
                            put("discount", map["discount"]?.toDoubleOrNull() ?: 0.0)
                            put("tax", map["tax"]?.toDoubleOrNull() ?: 0.0)
                            put("grandTotal", map["grandTotal"]?.toDoubleOrNull() ?: 0.0)
                            put("paymentMethod", map["paymentMethod"] ?: "TUNAI")
                            put("amountPaid", map["amountPaid"]?.toDoubleOrNull() ?: 0.0)
                            put("changeAmount", map["changeAmount"]?.toDoubleOrNull() ?: 0.0)
                            put("customerName", map["customerName"] ?: "Pelanggan")
                            put("orderType", map["orderType"] ?: "DINE_IN")
                            put("tableNumber", map["tableNumber"] ?: "")
                            put("notes", map["notes"] ?: "")
                            put("isSynced", parseBoolean(map["isSynced"], false))
                            put("syncedAt", syncedAtLong ?: JSONObject.NULL)
                            put("profitAmount", map["profitAmount"]?.toDoubleOrNull() ?: 0.0)
                            put("totalItemsCount", map["totalItemsCount"]?.toIntOrNull() ?: 0)
                        })
                    }

                    SECTION_TRANSACTION_ITEM_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) transactionItemHeaders = values.map { it.trim() }
                    }
                    SECTION_TRANSACTION_ITEM -> {
                        recognizedSections++
                        val map = mapRowValues(transactionItemHeaders, values)
                        transactionItemsArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("transactionId", map["transactionId"]?.toLongOrNull() ?: 0L)
                            put("productId", map["productId"]?.toLongOrNull() ?: 0L)
                            put("productName", map["productName"] ?: "")
                            put("unitPrice", map["unitPrice"]?.toDoubleOrNull() ?: 0.0)
                            put("costPrice", map["costPrice"]?.toDoubleOrNull() ?: 0.0)
                            put("quantity", map["quantity"]?.toIntOrNull() ?: 1)
                            put("subtotal", map["subtotal"]?.toDoubleOrNull() ?: 0.0)
                            put("itemNotes", map["itemNotes"] ?: "")
                        })
                    }

                    SECTION_EXPENSE_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) expenseHeaders = values.map { it.trim() }
                    }
                    SECTION_EXPENSE -> {
                        recognizedSections++
                        val map = mapRowValues(expenseHeaders, values)
                        expensesArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("title", map["title"] ?: "Pengeluaran")
                            put("amount", map["amount"]?.toDoubleOrNull() ?: 0.0)
                            put("category", map["category"] ?: "Operasional")
                            put("timestamp", map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis())
                            put("notes", map["notes"] ?: "")
                            put("createdBy", map["createdBy"] ?: "Admin")
                        })
                    }

                    SECTION_SHIFT_CLOSING_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) shiftClosingHeaders = values.map { it.trim() }
                    }
                    SECTION_SHIFT_CLOSING -> {
                        recognizedSections++
                        val map = mapRowValues(shiftClosingHeaders, values)
                        shiftClosingsArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("closingTime", map["closingTime"]?.toLongOrNull() ?: System.currentTimeMillis())
                            put("cashierId", map["cashierId"]?.toLongOrNull() ?: 1L)
                            put("cashierName", map["cashierName"] ?: "Kasir")
                            put("totalTransactions", map["totalTransactions"]?.toIntOrNull() ?: 0)
                            put("totalCash", map["totalCash"]?.toDoubleOrNull() ?: 0.0)
                            put("totalNonCash", map["totalNonCash"]?.toDoubleOrNull() ?: 0.0)
                            put("totalRevenue", map["totalRevenue"]?.toDoubleOrNull() ?: 0.0)
                            put("expectedCash", map["expectedCash"]?.toDoubleOrNull() ?: 0.0)
                            put("actualCash", map["actualCash"]?.toDoubleOrNull() ?: 0.0)
                            put("difference", map["difference"]?.toDoubleOrNull() ?: 0.0)
                            put("notes", map["notes"] ?: "")
                        })
                    }

                    SECTION_USER_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) userHeaders = values.map { it.trim() }
                    }
                    SECTION_USER -> {
                        recognizedSections++
                        val map = mapRowValues(userHeaders, values)
                        usersArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("username", map["username"] ?: "user")
                            put("pin", map["pin"] ?: "1234")
                            put("fullName", map["fullName"] ?: "User")
                            put("role", map["role"] ?: "KASIR")
                            put("phone", map["phone"] ?: "")
                            put("isActive", parseBoolean(map["isActive"], true))
                            put("createdAt", map["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis())
                        })
                    }

                    SECTION_DAILY_REPORT_HEADER -> {
                        recognizedSections++
                        if (values.isNotEmpty()) dailyReportHeaders = values.map { it.trim() }
                    }
                    SECTION_DAILY_REPORT -> {
                        recognizedSections++
                        val map = mapRowValues(dailyReportHeaders, values)
                        val syncedAtLong = map["syncedAt"]?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull()
                        dailyReportsArr.put(JSONObject().apply {
                            put("id", map["id"]?.toLongOrNull() ?: 0L)
                            put("date", map["date"] ?: "")
                            put("dateTimestamp", map["dateTimestamp"]?.toLongOrNull() ?: System.currentTimeMillis())
                            put("totalRevenue", map["totalRevenue"]?.toDoubleOrNull() ?: 0.0)
                            put("totalProfit", map["totalProfit"]?.toDoubleOrNull() ?: 0.0)
                            put("totalTransactions", map["totalTransactions"]?.toIntOrNull() ?: 0)
                            put("totalItemsSold", map["totalItemsSold"]?.toIntOrNull() ?: 0)
                            put("totalCash", map["totalCash"]?.toDoubleOrNull() ?: 0.0)
                            put("totalNonCash", map["totalNonCash"]?.toDoubleOrNull() ?: 0.0)
                            put("totalQris", map["totalQris"]?.toDoubleOrNull() ?: 0.0)
                            put("totalTransfer", map["totalTransfer"]?.toDoubleOrNull() ?: 0.0)
                            put("totalDiscount", map["totalDiscount"]?.toDoubleOrNull() ?: 0.0)
                            put("totalTax", map["totalTax"]?.toDoubleOrNull() ?: 0.0)
                            put("expectedCash", map["expectedCash"]?.toDoubleOrNull() ?: 0.0)
                            put("actualCash", map["actualCash"]?.toDoubleOrNull() ?: 0.0)
                            put("cashDifference", map["cashDifference"]?.toDoubleOrNull() ?: 0.0)
                            put("topProductSummary", map["topProductSummary"] ?: "")
                            put("cashierNames", map["cashierNames"] ?: "")
                            put("shiftCount", map["shiftCount"]?.toIntOrNull() ?: 0)
                            put("notes", map["notes"] ?: "")
                            put("isSynced", parseBoolean(map["isSynced"], false))
                            put("syncedAt", syncedAtLong ?: JSONObject.NULL)
                        })
                    }
                }
            }

            if (recognizedSections == 0) {
                return Result.failure(
                    IllegalArgumentException("Format CSV tidak dikenali sebagai cadangan KasirPOS")
                )
            }

            if (storeProfileObj.length() > 0) root.put("storeProfile", storeProfileObj)
            if (cloudHostingObj.length() > 0) root.put("cloudHostingConfig", cloudHostingObj)
            if (appSettingsObj.length() > 0) root.put("appSettings", appSettingsObj)
            root.put("products", productsArr)
            root.put("transactions", transactionsArr)
            root.put("transactionItems", transactionItemsArr)
            root.put("expenses", expensesArr)
            root.put("shiftClosings", shiftClosingsArr)
            root.put("users", usersArr)
            root.put("dailyReports", dailyReportsArr)

            Result.success(root)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun appendObjectSection(
        sb: StringBuilder,
        headerTag: String,
        rowTag: String,
        columns: List<String>,
        array: JSONArray?
    ) {
        appendRow(sb, listOf(headerTag) + columns)
        if (array == null) return
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val row = mutableListOf(rowTag)
            for (col in columns) {
                val raw = obj.opt(col)
                row.add(
                    when (raw) {
                        null, JSONObject.NULL -> ""
                        else -> raw.toString()
                    }
                )
            }
            appendRow(sb, row)
        }
    }

    private fun appendRow(sb: StringBuilder, cells: List<String>) {
        for (i in cells.indices) {
            if (i > 0) sb.append(',')
            sb.append(escapeCsvCell(cells[i]))
        }
        sb.append('\n')
    }

    fun escapeCsvCell(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r') ||
            value.contains(';')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        if (firstLine.contains("${SECTION_META_HEADER};") || firstLine.contains("${SECTION_SETTING_HEADER};")) {
            return ';'
        }
        if (firstLine.contains("${SECTION_META_HEADER},") || firstLine.contains("${SECTION_SETTING_HEADER},")) {
            return ','
        }
        val commas = firstLine.count { it == ',' }
        val semicolons = firstLine.count { it == ';' }
        return if (semicolons > commas) ';' else ','
    }

    fun parseCsvRows(text: String, delimiter: Char = ','): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length

        while (i < len) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < len && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> {
                        if (currentField.isEmpty()) {
                            inQuotes = true
                        } else {
                            currentField.append(ch)
                        }
                    }
                    delimiter -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < len && text[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }
        return rows
    }

    private fun mapRowValues(headers: List<String>, values: List<String>): Map<String, String> {
        val map = LinkedHashMap<String, String>(headers.size)
        for (i in headers.indices) {
            if (i < values.size) {
                map[headers[i]] = values[i]
            }
        }
        return map
    }

    private fun putTypedCloudSetting(target: JSONObject, key: String, rawVal: String) {
        when (key) {
            "useSeparateOwnerProject",
            "enableConsumerWeb",
            "enableEmailBackup",
            "autoBackupOnShiftClose",
            "enableLanServer" -> target.put(key, parseBoolean(rawVal, false))
            "lanPort" -> target.put(key, rawVal.toIntOrNull() ?: 8080)
            "lastBackupTimestamp",
            "updatedAt" -> target.put(key, rawVal.toLongOrNull() ?: 0L)
            else -> target.put(key, rawVal)
        }
    }

    private fun putTypedAppSetting(target: JSONObject, key: String, rawVal: String) {
        when (key) {
            "customCategories",
            "customUnits" -> {
                if (rawVal.isNotBlank()) {
                    try {
                        target.put(key, JSONArray(rawVal))
                    } catch (_: Throwable) {
                        val fallbackArr = JSONArray()
                        rawVal.split('|').map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                            fallbackArr.put(it)
                        }
                        target.put(key, fallbackArr)
                    }
                }
            }
            "tableCount",
            "autoCloseHour",
            "autoCloseMinute",
            "driveAutoBackupHour",
            "driveAutoBackupMinute",
            "driveAutoBackupKeepCount" -> {
                rawVal.toIntOrNull()?.let { target.put(key, it) }
            }
            "bgServiceEnabled",
            "lowStockNotifEnabled",
            "autoCloseEnabled",
            "driveAutoBackupEnabled",
            "driveAutoBackupWifiOnly",
            "driveAutoBackupOnShiftClose" -> {
                target.put(key, parseBoolean(rawVal, false))
            }
            else -> target.put(key, rawVal)
        }
    }

    private fun parseBoolean(raw: String?, default: Boolean): Boolean {
        val normalized = raw?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "true", "1", "yes", "ya" -> true
            "false", "0", "no", "tidak" -> false
            else -> default
        }
    }
}
