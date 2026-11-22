package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.PosDatabase
import com.example.data.model.StoreProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Embedded HTTP/1.1 Server di dalam aplikasi Android Kasir POS.
 *
 * Menyajikan portal Web Laporan Chrome (`owner.html`, `tutup-toko.html`, `laporan-online-rtdb.html`)
 * sekaligus endpoint REST `/api/health` & `/api/sync/...` langsung dari Room Database (SQLite)
 * sehingga laporan Transaksi Per Nota, Tutup Shift, Arsip Harian, Profil Toko, dan Catatan Pengeluaran
 * selalu tampil instan & akurat di Google Chrome (perangkat maupun LAN) tanpa bergantung pada server eksternal.
 */
object LocalWebReportServer {

    private const val TAG = "LocalWebReportServer"
    private val PORT_CANDIDATES = listOf(8080, 8765, 8766, 8767, 8768, 8769, 8770, 0)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var acceptThread: Thread? = null

    @Volatile
    private var executor: ExecutorService? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var posDatabase: PosDatabase? = null

    @Volatile
    private var testAssetsDir: File? = null

    private val relayOverlay = ConcurrentHashMap<String, JSONObject>()
    private val deletedKeys = ConcurrentHashMap.newKeySet<String>()

    private val _activePort = MutableStateFlow<Int?>(null)
    val activePort: StateFlow<Int?> = _activePort.asStateFlow()

    private val _serverUrlFlow = MutableStateFlow<String?>(null)
    val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()

    @Synchronized
    fun start(
        context: Context,
        database: PosDatabase,
        preferredPort: Int = 8080,
        assetsFallbackDir: File? = null
    ): Int? {
        appContext = context.applicationContext
        posDatabase = database
        if (assetsFallbackDir != null) {
            testAssetsDir = assetsFallbackDir
        }

        val existing = serverSocket
        if (existing != null && !existing.isClosed && _activePort.value != null) {
            return _activePort.value
        }

        val candidates = (listOf(preferredPort) + PORT_CANDIDATES).distinct()
        var boundSocket: ServerSocket? = null
        for (port in candidates) {
            try {
                val sock = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(InetAddress.getByName("0.0.0.0"), port), 64)
                }
                boundSocket = sock
                break
            } catch (t: Throwable) {
                Log.d(TAG, "Port $port busy/unavailable: ${t.message}")
            }
        }

        if (boundSocket == null) {
            Log.e(TAG, "Unable to bind embedded Web Report Server to any port")
            return null
        }

        serverSocket = boundSocket
        val boundPort = boundSocket.localPort
        _activePort.value = boundPort
        _serverUrlFlow.value = "http://127.0.0.1:$boundPort/owner.html"

        val pool = Executors.newCachedThreadPool()
        executor = pool

        val thread = Thread({
            Log.i(TAG, "Embedded LocalWebReportServer listening on http://127.0.0.1:$boundPort")
            while (!boundSocket.isClosed) {
                try {
                    val client = boundSocket.accept()
                    pool.execute { handleClient(client) }
                } catch (t: Throwable) {
                    if (!boundSocket.isClosed) {
                        Log.w(TAG, "Accept error: ${t.message}")
                    }
                }
            }
        }, "LocalWebReportServer-Accept").apply {
            isDaemon = true
            start()
        }
        acceptThread = thread
        return boundPort
    }

    @Synchronized
    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        serverSocket = null
        try {
            executor?.shutdownNow()
        } catch (_: Throwable) {
        }
        executor = null
        acceptThread = null
        _activePort.value = null
        _serverUrlFlow.value = null
        relayOverlay.clear()
        deletedKeys.clear()
    }

    fun isRunning(): Boolean = serverSocket?.isClosed == false && _activePort.value != null

    fun getLocalBaseUrl(): String {
        val port = _activePort.value ?: 8080
        return "http://127.0.0.1:$port"
    }

    fun getLanIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            var fallbackIp: String? = null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || iface.name.startsWith("wlan", ignoreCase = true)) {
                            return host
                        }
                        if (fallbackIp == null) {
                            fallbackIp = host
                        }
                    }
                }
            }
            fallbackIp
        } catch (_: Throwable) {
            null
        }
    }

    fun getLanBaseUrl(): String? {
        val ip = getLanIpAddress() ?: return null
        val port = _activePort.value ?: 8080
        return "http://$ip:$port"
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10_000
            val input = BufferedInputStream(socket.getInputStream())
            val output = socket.getOutputStream()

            val requestLine = readAsciiLine(input) ?: return
            val parts = requestLine.trim().split(" ")
            if (parts.size < 2) {
                writeResponse(output, 400, "application/json; charset=utf-8", """{"error":"Bad Request"}""".toByteArray())
                return
            }

            val method = parts[0].uppercase(Locale.ROOT)
            val rawTarget = parts[1]
            val rawPath = rawTarget.substringBefore("?")

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readAsciiLine(input) ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase(Locale.ROOT)
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 4 * 1024 * 1024) ?: 0
            val bodyBytes = if (contentLength > 0) readExactBytes(input, contentLength) else ByteArray(0)
            val bodyText = bodyBytes.toString(Charsets.UTF_8)

            if (method == "OPTIONS") {
                writeResponse(output, 204, "text/plain; charset=utf-8", ByteArray(0))
                return
            }

            when {
                rawPath == "/api/health" -> {
                    val healthJson = JSONObject(
                        mapOf(
                            "ok" to true,
                            "source" to "android-room-live",
                            "port" to (_activePort.value ?: 8080)
                        )
                    ).toString()
                    writeResponse(output, 200, "application/json; charset=utf-8", healthJson.toByteArray(Charsets.UTF_8))
                }

                rawPath == "/api/sync" || rawPath.startsWith("/api/sync/") -> {
                    handleApiSync(method, rawPath, bodyText, output)
                }

                rawPath == "/" || rawPath == "/owner.html" || rawPath == "/index.html" ||
                    rawPath == "/rtdb-laporan.html" || rawPath == "/laporan-online-rtdb.html" ||
                    rawPath == "/tutup-toko.html" -> {
                    val fileName = when (rawPath) {
                        "/" -> "owner.html"
                        else -> rawPath.removePrefix("/")
                    }
                    val htmlBytes = loadWebAssetBytes(fileName)
                    if (htmlBytes != null) {
                        writeResponse(output, 200, "text/html; charset=utf-8", htmlBytes)
                    } else {
                        val fallbackHtml = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Laporan POS</title></head><body><h2>Asset $fileName belum tersedia</h2></body></html>"
                        writeResponse(output, 404, "text/html; charset=utf-8", fallbackHtml.toByteArray(Charsets.UTF_8))
                    }
                }

                else -> {
                    val notFound = JSONObject(mapOf("error" to "Not Found", "path" to rawPath)).toString()
                    writeResponse(output, 404, "application/json; charset=utf-8", notFound.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (t: Throwable) {
            Log.d(TAG, "Client request warning: ${t.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun handleApiSync(
        method: String,
        rawPath: String,
        bodyText: String,
        output: OutputStream
    ) {
        val nodePath = rawPath.removePrefix("/api/sync").trim('/')
            .removeSuffix(".json")
            .trim('/')
        val decodedSegments = if (nodePath.isEmpty()) emptyList() else nodePath.split("/").map {
            try {
                URLDecoder.decode(it, "UTF-8")
            } catch (_: Throwable) {
                it
            }
        }

        if (method == "GET") {
            val payloadStr = buildJsonForSyncPath(decodedSegments)
            writeResponse(output, 200, "application/json; charset=utf-8", payloadStr.toByteArray(Charsets.UTF_8))
            return
        }

        if (method == "DELETE") {
            handleSyncDelete(decodedSegments)
            writeResponse(output, 200, "application/json; charset=utf-8", """{"ok":true}""".toByteArray(Charsets.UTF_8))
            return
        }

        if (method == "PUT" || method == "PATCH" || method == "POST") {
            handleSyncWrite(decodedSegments, bodyText, method)
            writeResponse(output, 200, "application/json; charset=utf-8", """{"ok":true}""".toByteArray(Charsets.UTF_8))
            return
        }

        writeResponse(output, 405, "application/json; charset=utf-8", """{"error":"Method not allowed"}""".toByteArray(Charsets.UTF_8))
    }

    private fun handleSyncWrite(segments: List<String>, bodyText: String, method: String = "PUT") {
        if (segments.isEmpty()) return
        val fullKey = segments.joinToString("/")
        deletedKeys.remove(fullKey)
        try {
            val obj = if (bodyText.isBlank()) JSONObject() else JSONObject(bodyText)
            if (method.equals("PATCH", ignoreCase = true)) {
                val existing = relayOverlay[fullKey]
                if (existing != null) {
                    val merged = JSONObject(existing.toString())
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        merged.put(key, obj.get(key))
                    }
                    relayOverlay[fullKey] = merged
                    return
                }
            }
            relayOverlay[fullKey] = obj
        } catch (_: Throwable) {
        }
    }

    private fun purgeExpenseOverlayKeys(expKey: String) {
        deletedKeys.add("expenses/$expKey")
        deletedKeys.add("cash_flows/CF-$expKey")
        deletedKeys.add("cash_flows/$expKey")
        deletedKeys.add("realtime_stats/expenses/$expKey")
        relayOverlay.keys.filter {
            it == "expenses/$expKey" ||
                it == "cash_flows/CF-$expKey" ||
                it == "cash_flows/$expKey" ||
                it == "realtime_stats/expenses/$expKey" ||
                (it.startsWith("reports/daily_expenses/") && it.endsWith("/$expKey")) ||
                (it.startsWith("realtime_stats/expenses_by_date/") && it.endsWith("/$expKey"))
        }.forEach {
            deletedKeys.add(it)
            relayOverlay.remove(it)
        }
        val lastExp = relayOverlay["realtime_stats/last_expense"]
        if (lastExp != null && (lastExp.optString("lastExpenseId") == expKey || lastExp.optString("id") == expKey)) {
            relayOverlay.remove("realtime_stats/last_expense")
        }
    }

    private fun handleSyncDelete(segments: List<String>) {
        if (segments.isEmpty()) return
        val fullKey = segments.joinToString("/")
        deletedKeys.add(fullKey)
        relayOverlay.remove(fullKey)

        val db = posDatabase ?: return
        runBlocking(Dispatchers.IO) {
            try {
                when {
                    segments.size >= 2 && segments[0] == "expenses" -> {
                        val key = segments[1]
                        val idNum = key.removePrefix("EXP-").toLongOrNull()
                        if (idNum != null) {
                            val match = db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
                                .firstOrNull { it.id == idNum }
                            if (match != null) {
                                appContext?.let {
                                    com.example.data.repository.RestoreGuardTracker.recordExpenseDeleted(
                                        it, match.id, match.title, match.timestamp
                                    )
                                }
                                db.expenseDao().deleteExpense(match)
                            }
                        }
                        purgeExpenseOverlayKeys(key)
                    }

                    segments.size >= 2 && segments[0] == "transactions" -> {
                        val inv = segments[1]
                        deleteRoomTransactionByInvoice(db, inv)
                    }

                    segments.size >= 4 && segments[0] == "reports" && segments[1] == "daily_transactions" -> {
                        val inv = segments[3]
                        deleteRoomTransactionByInvoice(db, inv)
                    }

                    segments.size >= 4 && segments[0] == "reports" && segments[1] == "daily_expenses" -> {
                        val key = segments[3]
                        val idNum = key.removePrefix("EXP-").toLongOrNull()
                        if (idNum != null) {
                            val match = db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
                                .firstOrNull { it.id == idNum }
                            if (match != null) {
                                appContext?.let {
                                    com.example.data.repository.RestoreGuardTracker.recordExpenseDeleted(
                                        it, match.id, match.title, match.timestamp
                                    )
                                }
                                db.expenseDao().deleteExpense(match)
                            }
                        }
                        purgeExpenseOverlayKeys(key)
                    }

                    segments.size >= 3 && segments[0] == "realtime_stats" && segments[1] == "expenses" -> {
                        val key = segments[2]
                        val idNum = key.removePrefix("EXP-").toLongOrNull()
                        if (idNum != null) {
                            val match = db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
                                .firstOrNull { it.id == idNum }
                            if (match != null) {
                                appContext?.let {
                                    com.example.data.repository.RestoreGuardTracker.recordExpenseDeleted(
                                        it, match.id, match.title, match.timestamp
                                    )
                                }
                                db.expenseDao().deleteExpense(match)
                            }
                        }
                        purgeExpenseOverlayKeys(key)
                    }

                    segments.size >= 4 && segments[0] == "realtime_stats" && segments[1] == "expenses_by_date" -> {
                        val key = segments[3]
                        val idNum = key.removePrefix("EXP-").toLongOrNull()
                        if (idNum != null) {
                            val match = db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
                                .firstOrNull { it.id == idNum }
                            if (match != null) {
                                appContext?.let {
                                    com.example.data.repository.RestoreGuardTracker.recordExpenseDeleted(
                                        it, match.id, match.title, match.timestamp
                                    )
                                }
                                db.expenseDao().deleteExpense(match)
                            }
                        }
                        purgeExpenseOverlayKeys(key)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Error deleting from Room DB on $fullKey: ${t.message}")
            }
        }
    }

    private suspend fun deleteRoomTransactionByInvoice(db: PosDatabase, invoiceOrId: String) {
        val all = db.transactionDao().getAllTransactionsList()
        val target = all.firstOrNull {
            it.invoiceNumber.equals(invoiceOrId, ignoreCase = true) ||
                it.id.toString() == invoiceOrId
        } ?: return
        appContext?.let {
            com.example.data.repository.RestoreGuardTracker.recordTransactionDeleted(
                it, target.id, target.invoiceNumber
            )
        }
        db.transactionDao().deleteItemsByTransactionId(target.id)
        db.transactionDao().deleteTransactionById(target.id)
        deletedKeys.add("transactions/${target.invoiceNumber}")
    }

    private fun buildJsonForSyncPath(segments: List<String>): String {
        val db = posDatabase
        if (db == null) {
            return "{}"
        }

        return runBlocking(Dispatchers.IO) {
            val root = segments.firstOrNull().orEmpty()
            when (root) {
                "" -> {
                    val result = JSONObject()
                    result.put("expenses", buildExpensesJson(db))
                    result.put("cash_flows", buildCashFlowsJson(db))
                    result.put("transactions", buildTransactionsJson(db))
                    val reportsObj = JSONObject()
                    reportsObj.put("daily_transactions", buildDailyTransactionsNestedJson(db))
                    reportsObj.put("daily_expenses", buildDailyExpensesNestedJson(db))
                    reportsObj.put("daily", buildDailyReportsJson(db))
                    result.put("reports", reportsObj)
                    result.put("shift_reports", buildShiftReportsJson(db))
                    result.put("daily_reports", buildDailyReportsJson(db))
                    val profileObj = JSONObject()
                    profileObj.put("store_main", buildStoreProfileJson(db))
                    result.put("store_profile", profileObj)
                    result.put("realtime_stats", buildRealtimeStatsJson(db))
                    result.toString()
                }

                "expenses" -> {
                    val all = buildExpensesJson(db)
                    if (segments.size >= 2) {
                        (all.optJSONObject(segments[1]) ?: JSONObject()).toString()
                    } else {
                        all.toString()
                    }
                }

                "cash_flows" -> {
                    val all = buildCashFlowsJson(db)
                    if (segments.size >= 2) {
                        (all.optJSONObject(segments[1]) ?: JSONObject()).toString()
                    } else {
                        all.toString()
                    }
                }

                "transactions" -> {
                    val all = buildTransactionsJson(db)
                    if (segments.size >= 2) {
                        (all.optJSONObject(segments[1]) ?: JSONObject()).toString()
                    } else {
                        all.toString()
                    }
                }

                "reports" -> {
                    val sub = segments.getOrNull(1).orEmpty()
                    when (sub) {
                        "daily_transactions" -> {
                            val nested = buildDailyTransactionsNestedJson(db)
                            when {
                                segments.size >= 4 -> (nested.optJSONObject(segments[2])?.optJSONObject(segments[3]) ?: JSONObject()).toString()
                                segments.size == 3 -> (nested.optJSONObject(segments[2]) ?: JSONObject()).toString()
                                else -> nested.toString()
                            }
                        }

                        "daily_expenses" -> {
                            val nested = buildDailyExpensesNestedJson(db)
                            when {
                                segments.size >= 4 -> (nested.optJSONObject(segments[2])?.optJSONObject(segments[3]) ?: JSONObject()).toString()
                                segments.size == 3 -> (nested.optJSONObject(segments[2]) ?: JSONObject()).toString()
                                else -> nested.toString()
                            }
                        }

                        "daily" -> {
                            val all = buildDailyReportsJson(db)
                            if (segments.size >= 3) (all.optJSONObject(segments[2]) ?: JSONObject()).toString() else all.toString()
                        }

                        else -> {
                            val wrapper = JSONObject()
                            wrapper.put("daily_transactions", buildDailyTransactionsNestedJson(db))
                            wrapper.put("daily_expenses", buildDailyExpensesNestedJson(db))
                            wrapper.put("daily", buildDailyReportsJson(db))
                            wrapper.toString()
                        }
                    }
                }

                "shift_reports" -> {
                    val all = buildShiftReportsJson(db)
                    if (segments.size >= 2) {
                        (all.optJSONObject(segments[1]) ?: JSONObject()).toString()
                    } else {
                        all.toString()
                    }
                }

                "daily_reports" -> {
                    val all = buildDailyReportsJson(db)
                    if (segments.size >= 2) {
                        (all.optJSONObject(segments[1]) ?: JSONObject()).toString()
                    } else {
                        all.toString()
                    }
                }

                "store_profile" -> {
                    val profile = buildStoreProfileJson(db)
                    if (segments.size >= 2) {
                        profile.toString()
                    } else {
                        JSONObject().apply { put("store_main", profile) }.toString()
                    }
                }

                "realtime_stats" -> {
                    val stats = buildRealtimeStatsJson(db)
                    when {
                        segments.size >= 4 -> (stats.optJSONObject(segments[1])?.optJSONObject(segments[2])?.optJSONObject(segments[3]) ?: JSONObject()).toString()
                        segments.size == 3 -> (stats.optJSONObject(segments[1])?.optJSONObject(segments[2]) ?: JSONObject()).toString()
                        segments.size == 2 -> (stats.optJSONObject(segments[1]) ?: JSONObject()).toString()
                        else -> stats.toString()
                    }
                }

                else -> {
                    (relayOverlay[segments.joinToString("/")] ?: JSONObject()).toString()
                }
            }
        }
    }

    private suspend fun buildExpensesJson(db: PosDatabase): JSONObject {
        val result = JSONObject()
        val expenses = try {
            db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
        } catch (_: Throwable) {
            emptyList()
        }
        val dateFmt = localDateFormat()
        val isoFmt = utcIsoFormat()

        for (exp in expenses) {
            val expKey = "EXP-${exp.id}"
            if ("expenses/$expKey" in deletedKeys) continue
            val author = exp.createdBy.ifBlank { "Kasir" }
            val d = Date(exp.timestamp)
            val iso = isoFmt.format(d)
            val obj = JSONObject().apply {
                put("id", expKey)
                put("localId", exp.id)
                put("title", exp.title)
                put("amount", exp.amount)
                put("category", exp.category.ifBlank { "Operasional" })
                put("notes", exp.notes)
                put("createdBy", author)
                put("cashierName", author)
                put("date", dateFmt.format(d))
                put("timestamp", iso)
                put("timestampMillis", exp.timestamp)
                put("syncedAt", iso)
                put("storeId", "store_main")
            }
            result.put(expKey, obj)
        }

        // Include extra relay writes if any (flat /expenses/{id} or nested /reports/daily_expenses/{date}/{id})
        for ((k, v) in relayOverlay) {
            if (k.startsWith("expenses/") && k !in deletedKeys) {
                val id = k.removePrefix("expenses/")
                if (!result.has(id)) {
                    result.put(id, v)
                }
            } else if (k.startsWith("reports/daily_expenses/") && k !in deletedKeys) {
                val subParts = k.removePrefix("reports/daily_expenses/").split("/")
                if (subParts.size >= 2) {
                    val datePart = subParts[0]
                    val id = subParts[1]
                    if (!result.has(id) && "expenses/$id" !in deletedKeys) {
                        val enriched = JSONObject(v.toString())
                        if (!enriched.has("id")) enriched.put("id", id)
                        if (enriched.optString("date", "").isBlank()) enriched.put("date", datePart)
                        result.put(id, enriched)
                    }
                }
            } else if (k.startsWith("realtime_stats/expenses/") && k !in deletedKeys) {
                val id = k.removePrefix("realtime_stats/expenses/")
                if (!result.has(id) && "expenses/$id" !in deletedKeys) {
                    result.put(id, v)
                }
            } else if (k.startsWith("realtime_stats/expenses_by_date/") && k !in deletedKeys) {
                val subParts = k.removePrefix("realtime_stats/expenses_by_date/").split("/")
                if (subParts.size >= 2) {
                    val datePart = subParts[0]
                    val id = subParts[1]
                    if (!result.has(id) && "expenses/$id" !in deletedKeys) {
                        val enriched = JSONObject(v.toString())
                        if (!enriched.has("id")) enriched.put("id", id)
                        if (enriched.optString("date", "").isBlank()) enriched.put("date", datePart)
                        result.put(id, enriched)
                    }
                }
            }
        }
        return result
    }

    private suspend fun buildCashFlowsJson(db: PosDatabase): JSONObject {
        val result = JSONObject()
        val expenses = try {
            db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
        } catch (_: Throwable) {
            emptyList()
        }
        val dateFmt = localDateFormat()
        val isoFmt = utcIsoFormat()

        for (exp in expenses) {
            val expKey = "EXP-${exp.id}"
            if ("expenses/$expKey" in deletedKeys) continue
            val cfKey = "CF-$expKey"
            val d = Date(exp.timestamp)
            val obj = JSONObject().apply {
                put("id", cfKey)
                put("type", "OUT")
                put("category", exp.category.ifBlank { "Operasional" })
                put("description", exp.title)
                put("notes", exp.notes)
                put("amount", exp.amount)
                put("cashierName", exp.createdBy.ifBlank { "Admin" })
                put("date", dateFmt.format(d))
                put("timestamp", isoFmt.format(d))
                put("timestampMillis", exp.timestamp)
            }
            result.put(cfKey, obj)
        }
        for ((k, v) in relayOverlay) {
            if (k.startsWith("cash_flows/") && k !in deletedKeys) {
                val id = k.removePrefix("cash_flows/")
                if (!result.has(id)) {
                    result.put(id, v)
                }
            }
        }
        return result
    }

    private suspend fun buildTransactionsJson(db: PosDatabase): JSONObject {
        val result = JSONObject()
        val trxs = try {
            db.transactionDao().getAllTransactionsList()
        } catch (_: Throwable) {
            emptyList()
        }
        val allItems = try {
            db.transactionDao().getAllTransactionItemsList()
        } catch (_: Throwable) {
            emptyList()
        }
        val itemsByTrx = allItems.groupBy { it.transactionId }
        val dateFmt = localDateFormat()
        val isoFmt = utcIsoFormat()

        for (trx in trxs) {
            val key = trx.invoiceNumber.ifBlank { "TRX-${trx.id}" }
            if ("transactions/$key" in deletedKeys) continue
            val items = itemsByTrx[trx.id].orEmpty()
            val itemsSummary = if (items.isNotEmpty()) {
                items.joinToString(", ") { "${it.productName} (${it.quantity}x)" }
            } else {
                "${trx.totalItemsCount.coerceAtLeast(1)} item pesanan"
            }
            val itemsArray = JSONArray()
            for (it in items) {
                itemsArray.put(
                    JSONObject().apply {
                        put("id", it.id)
                        put("productId", it.productId)
                        put("productName", it.productName)
                        put("unitPrice", it.unitPrice)
                        put("quantity", it.quantity)
                        put("subtotal", it.subtotal)
                        put("notes", it.itemNotes ?: "")
                    }
                )
            }

            val d = Date(trx.timestamp)
            val dateStr = dateFmt.format(d)
            val isoStr = isoFmt.format(d)
            val obj = JSONObject().apply {
                put("id", key)
                put("invoiceNumber", key)
                put("orderNumber", key)
                put("customerName", trx.customerName)
                put("orderType", trx.orderType.name)
                put("tableNumber", trx.tableNumber)
                put("notes", trx.notes)
                put("itemsSummary", itemsSummary)
                put("items", itemsArray)
                put("subtotalAmount", trx.subtotal)
                put("discountAmount", trx.discount)
                put("taxAmount", trx.tax)
                put("totalAmount", trx.grandTotal)
                put("grandTotal", trx.grandTotal)
                put("profitAmount", trx.profitAmount)
                put("totalItemsCount", trx.totalItemsCount)
                put("paymentMethod", trx.paymentMethod.name)
                put("cashGiven", trx.amountPaid)
                put("changeAmount", trx.changeAmount)
                put("cashierName", trx.cashierName)
                put("cashierRole", "Kasir")
                put("timestamp", isoStr)
                put("timestampMillis", trx.timestamp)
                put("date", dateStr)
                put("storeId", "store_main")
                put("syncedAt", isoStr)
            }
            result.put(key, obj)
        }

        for ((k, v) in relayOverlay) {
            if (k.startsWith("transactions/") && k !in deletedKeys) {
                val id = k.removePrefix("transactions/")
                if (!result.has(id)) {
                    result.put(id, v)
                }
            } else if (k.startsWith("reports/daily_transactions/") && k !in deletedKeys) {
                val subParts = k.removePrefix("reports/daily_transactions/").split("/")
                if (subParts.size >= 2) {
                    val datePart = subParts[0]
                    val id = subParts[1]
                    if (!result.has(id) && "transactions/$id" !in deletedKeys) {
                        val enriched = JSONObject(v.toString())
                        if (!enriched.has("id")) enriched.put("id", id)
                        if (enriched.optString("date", "").isBlank()) enriched.put("date", datePart)
                        result.put(id, enriched)
                    }
                }
            }
        }
        return result
    }

    private suspend fun buildDailyTransactionsNestedJson(db: PosDatabase): JSONObject {
        val nested = JSONObject()
        val flat = buildTransactionsJson(db)
        val keys = flat.keys()
        while (keys.hasNext()) {
            val inv = keys.next()
            val obj = flat.optJSONObject(inv) ?: continue
            val dateStr = obj.optString("date", "").ifBlank { localDateFormat().format(Date()) }
            val dayBucket = nested.optJSONObject(dateStr) ?: JSONObject().also { nested.put(dateStr, it) }
            dayBucket.put(inv, obj)
        }
        for ((k, v) in relayOverlay) {
            if (k.startsWith("reports/daily_transactions/") && k !in deletedKeys) {
                val subParts = k.removePrefix("reports/daily_transactions/").split("/")
                if (subParts.size >= 2) {
                    val datePart = subParts[0]
                    val invKey = subParts[1]
                    if ("transactions/$invKey" !in deletedKeys) {
                        val dayBucket = nested.optJSONObject(datePart) ?: JSONObject().also { nested.put(datePart, it) }
                        if (!dayBucket.has(invKey)) {
                            dayBucket.put(invKey, v)
                        }
                    }
                }
            }
        }
        return nested
    }

    private suspend fun buildDailyExpensesNestedJson(db: PosDatabase): JSONObject {
        val flat = buildExpensesJson(db)
        val nested = JSONObject()
        val keys = flat.keys()
        while (keys.hasNext()) {
            val expKey = keys.next()
            val obj = flat.optJSONObject(expKey) ?: continue
            val dateStr = obj.optString("date", "").ifBlank { localDateFormat().format(Date()) }
            val dayBucket = nested.optJSONObject(dateStr) ?: JSONObject().also { nested.put(dateStr, it) }
            dayBucket.put(expKey, obj)
        }
        for ((k, v) in relayOverlay) {
            if (k.startsWith("reports/daily_expenses/") && k !in deletedKeys) {
                val subParts = k.removePrefix("reports/daily_expenses/").split("/")
                if (subParts.size >= 2) {
                    val datePart = subParts[0]
                    val expKey = subParts[1]
                    if ("expenses/$expKey" !in deletedKeys) {
                        val dayBucket = nested.optJSONObject(datePart) ?: JSONObject().also { nested.put(datePart, it) }
                        if (!dayBucket.has(expKey)) {
                            dayBucket.put(expKey, v)
                        }
                    }
                }
            }
        }
        return nested
    }

    private fun expenseToJsonObject(
        exp: com.example.data.model.Expense,
        dateFmt: SimpleDateFormat,
        isoFmt: SimpleDateFormat
    ): JSONObject {
        val expKey = "EXP-${exp.id}"
        val author = exp.createdBy.ifBlank { "Kasir" }
        val d = Date(exp.timestamp)
        val iso = isoFmt.format(d)
        return JSONObject().apply {
            put("id", expKey)
            put("localId", exp.id)
            put("title", exp.title)
            put("amount", exp.amount)
            put("category", exp.category.ifBlank { "Operasional" })
            put("notes", exp.notes)
            put("createdBy", author)
            put("cashierName", author)
            put("date", dateFmt.format(d))
            put("timestamp", iso)
            put("timestampMillis", exp.timestamp)
            put("syncedAt", iso)
            put("storeId", "store_main")
        }
    }

    private suspend fun buildShiftReportsJson(db: PosDatabase): JSONObject {
        val result = JSONObject()
        val closings = try {
            db.shiftClosingDao().getAllShiftClosingsList()
        } catch (_: Throwable) {
            emptyList()
        }
        val expenses = try {
            db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
        } catch (_: Throwable) {
            emptyList()
        }
        val dateFmt = localDateFormat()
        val isoFmt = utcIsoFormat()
        val expensesByDate = expenses.groupBy { dateFmt.format(Date(it.timestamp)) }

        for (c in closings) {
            val docId = "SHIFT-${c.id}"
            if ("shift_reports/$docId" in deletedKeys) continue
            val d = Date(c.closingTime)
            val dateStr = dateFmt.format(d)
            val isoStr = isoFmt.format(d)
            val dayExpenses = expensesByDate[dateStr].orEmpty()
            val totalExp = dayExpenses.sumOf { it.amount }
            val expNotes = dayExpenses.joinToString("; ") { exp ->
                val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                "${exp.title} [${exp.category}]: Rp ${String.format(Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
            }
            val expArray = JSONArray().apply {
                dayExpenses.forEach { put(expenseToJsonObject(it, dateFmt, isoFmt)) }
            }

            val obj = JSONObject().apply {
                put("id", docId)
                put("shiftNumber", "Shift-${c.id}")
                put("cashierName", c.cashierName)
                put("cashierRole", "Kasir")
                put("startTime", isoStr)
                put("endTime", isoStr)
                put("totalTransactions", c.totalTransactions)
                put("totalRevenue", c.totalRevenue)
                put("grossRevenue", c.totalRevenue)
                put("totalCash", c.totalCash)
                put("totalNonCash", c.totalNonCash)
                put("totalExpenses", totalExp)
                put("expenseNotes", expNotes)
                put("expensesList", expArray)
                put("expectedCash", c.expectedCash)
                put("actualCash", c.actualCash)
                put("difference", c.difference)
                put("notes", c.notes)
                put("timestamp", isoStr)
                put("timestampMillis", c.closingTime)
                put("closingTime", c.closingTime)
                put("date", dateStr)
                put("storeId", "store_main")
                put("status", "CLOSED")
            }
            result.put(docId, obj)
        }

        for ((k, v) in relayOverlay) {
            if (k.startsWith("shift_reports/") && k !in deletedKeys) {
                val id = k.removePrefix("shift_reports/")
                if (!result.has(id)) {
                    result.put(id, v)
                }
            }
        }
        return result
    }

    private suspend fun buildDailyReportsJson(db: PosDatabase): JSONObject {
        val result = JSONObject()
        val reports = try {
            db.dailyReportDao().getAllDailyReportsList()
        } catch (_: Throwable) {
            emptyList()
        }
        val expenses = try {
            db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
        } catch (_: Throwable) {
            emptyList()
        }
        val dateFmt = localDateFormat()
        val isoFmt = utcIsoFormat()
        val expensesByDate = expenses.groupBy { dateFmt.format(Date(it.timestamp)) }

        for (dr in reports) {
            val dateStr = dr.date
            if ("daily_reports/$dateStr" in deletedKeys) continue
            val dayExpenses = expensesByDate[dateStr].orEmpty()
            val totalExp = dayExpenses.sumOf { it.amount }
            val expNotes = dayExpenses.joinToString("; ") { exp ->
                val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                "${exp.title} [${exp.category}]: Rp ${String.format(Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
            }
            val expArray = JSONArray().apply {
                dayExpenses.forEach { put(expenseToJsonObject(it, dateFmt, isoFmt)) }
            }

            val obj = JSONObject().apply {
                put("date", dr.date)
                put("dateTimestamp", dr.dateTimestamp)
                put("totalRevenue", dr.totalRevenue)
                put("grossRevenue", dr.totalRevenue)
                put("totalProfit", dr.totalProfit)
                put("totalExpenses", totalExp)
                put("expenseNotes", expNotes)
                put("expensesList", expArray)
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
                put("difference", dr.cashDifference)
                put("topProductSummary", dr.topProductSummary)
                put("cashierNames", dr.cashierNames)
                put("shiftCount", dr.shiftCount)
                put("notes", dr.notes)
                put("archivedByTutupToko", true)
                put("lastUpdated", isoFmt.format(Date(dr.updatedAt)))
                put("updatedAt", dr.updatedAt)
            }
            result.put(dateStr, obj)
        }

        for ((k, v) in relayOverlay) {
            if (k.startsWith("daily_reports/") && k !in deletedKeys) {
                val id = k.removePrefix("daily_reports/")
                if (!result.has(id)) {
                    result.put(id, v)
                }
            }
        }
        return result
    }

    private suspend fun buildStoreProfileJson(db: PosDatabase): JSONObject {
        val profile = try {
            db.storeProfileDao().getStoreProfileSync() ?: StoreProfile(id = 1)
        } catch (_: Throwable) {
            StoreProfile(id = 1)
        }
        return JSONObject().apply {
            put("storeName", profile.storeName)
            put("address", profile.address)
            put("phone", profile.phone)
            put("ownerPhone", profile.ownerPhone)
            put("footerMessage", profile.receiptFooter)
            put("updatedAt", utcIsoFormat().format(Date()))
        }
    }

    private suspend fun buildRealtimeStatsJson(db: PosDatabase): JSONObject {
        val dateFmt = localDateFormat()
        val isoFmt = utcIsoFormat()
        val todayStr = dateFmt.format(Date())

        val trxs = try {
            db.transactionDao().getAllTransactionsList()
                .filter { dateFmt.format(Date(it.timestamp)) == todayStr }
        } catch (_: Throwable) {
            emptyList()
        }
        val latestTrx = trxs.maxByOrNull { it.timestamp }

        val expenses = try {
            db.expenseDao().getExpensesBetweenList(0L, Long.MAX_VALUE)
                .filter { dateFmt.format(Date(it.timestamp)) == todayStr }
        } catch (_: Throwable) {
            emptyList()
        }
        val latestExp = expenses.maxByOrNull { it.timestamp }
        val allFlatExps = buildExpensesJson(db)
        val allNestedExps = buildDailyExpensesNestedJson(db)

        val todayObj = JSONObject().apply {
            put("todayRevenue", trxs.sumOf { it.grandTotal })
            put("todayTransactionsCount", trxs.size)
            put("lastTransactionAmount", latestTrx?.grandTotal ?: 0.0)
            put("lastInvoiceNumber", latestTrx?.invoiceNumber ?: "-")
            put("lastCustomerName", latestTrx?.customerName ?: "-")
            put("date", todayStr)
            put("lastUpdated", isoFmt.format(Date()))
        }
        val expObj = JSONObject().apply {
            put("lastExpenseId", latestExp?.let { "EXP-${it.id}" } ?: "")
            put("lastExpenseTitle", latestExp?.title ?: "")
            put("lastExpenseAmount", latestExp?.amount ?: 0.0)
            put("lastExpenseCategory", latestExp?.category ?: "Operasional")
            put("lastExpenseNotes", latestExp?.notes ?: "")
            put("lastExpenseBy", latestExp?.createdBy ?: "Admin")
            put("todayExpensesTotal", expenses.sumOf { it.amount })
            put("date", todayStr)
            put("timestampMillis", latestExp?.timestamp ?: System.currentTimeMillis())
        }

        return JSONObject().apply {
            put("today", todayObj)
            put("last_expense", expObj)
            put("expenses", allFlatExps)
            put("expenses_by_date", allNestedExps)
        }
    }

    private fun loadWebAssetBytes(fileName: String): ByteArray? {
        val fallbackDir = testAssetsDir
        if (fallbackDir != null) {
            val file = File(fallbackDir, fileName)
            if (file.isFile) {
                return file.readBytes()
            }
        }
        val ctx = appContext ?: return null
        return try {
            ctx.assets.open("web/$fileName").use { it.readBytes() }
        } catch (t: Throwable) {
            Log.w(TAG, "Asset web/$fileName not found: ${t.message}")
            null
        }
    }

    private fun readAsciiLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) {
                return if (baos.size() == 0) null else baos.toString(Charsets.US_ASCII.name())
            }
            if (b == '\n'.code) {
                val bytes = baos.toByteArray()
                val len = if (prev == '\r'.code && bytes.isNotEmpty()) bytes.size - 1 else bytes.size
                return String(bytes, 0, len, Charsets.US_ASCII)
            }
            baos.write(b)
            prev = b
        }
    }

    private fun readExactBytes(input: InputStream, count: Int): ByteArray {
        val buf = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buf, offset, count - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == count) buf else buf.copyOf(offset)
    }

    private fun writeResponse(output: OutputStream, statusCode: Int, contentType: String, body: ByteArray) {
        val statusText = when (statusCode) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "Error"
        }
        val headers = buildString {
            append("HTTP/1.1 $statusCode $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Private-Network: true\r\n")
            append("Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type, Authorization, Accept, Access-Control-Request-Private-Network\r\n")
            append("Cache-Control: no-store, no-cache, must-revalidate\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(headers.toByteArray(Charsets.US_ASCII))
        if (body.isNotEmpty()) {
            output.write(body)
        }
        output.flush()
    }

    private fun localDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun utcIsoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
