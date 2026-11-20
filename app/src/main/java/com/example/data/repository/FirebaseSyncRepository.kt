package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.DailyReport
import com.example.data.model.Expense
import com.example.data.model.ShiftClosing
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Crash-proof Firebase Cloud Sync Repository.
 * Safely and directly communicates with Firebase Realtime Database
 * for live cloud synchronization and web hosting reports (pure Realtime Database).
 */
class FirebaseSyncRepository(private val context: Context) {

    private fun ensureFirebaseInitialized(): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            true
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "FirebaseApp initialization check failed", t)
            false
        }
    }

    private val auth: FirebaseAuth?
        get() {
            return try {
                if (ensureFirebaseInitialized()) FirebaseAuth.getInstance() else null
            } catch (t: Throwable) {
                Log.e("FirebaseSync", "FirebaseAuth unavailable", t)
                null
            }
        }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (t: Throwable) {
            null
        }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val currentAuth = auth
        if (currentAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        try {
            currentAuth.addAuthStateListener(listener)
        } catch (t: Throwable) {
            trySend(null)
        }
        awaitClose {
            try {
                currentAuth.removeAuthStateListener(listener)
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    suspend fun signInWithGoogle(
        context: Context,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentAuth = auth
        if (currentAuth == null) {
            onError("Firebase Auth belum terpasang atau tidak aktif")
            return
        }

        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            "574700312081-27c027gvct93gl0ghs7tnlatjgi0p7mo.apps.googleusercontent.com"
        }

        try {
            val credentialManager = CredentialManager.create(context)
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = currentAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                onSuccess(user)
            } else {
                onError("Gagal login Firebase: User null")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("FirebaseSync", "Google Sign-in was cancelled by user")
            onError("Login Google dibatalkan")
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Error Google Sign In", t)
            onError(t.localizedMessage ?: "Terjadi kesalahan autentikasi")
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Sign out error", t)
        }
    }

    companion object {
        private val PLACEHOLDER_PREFIXES = listOf("ai-studio", "gen-lang-client", "laporan-service", "(default)")

        fun isPlaceholderCloudId(value: String?): Boolean {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isEmpty()) return true
            val lower = trimmed.lowercase(Locale.ROOT)
            return PLACEHOLDER_PREFIXES.any { prefix ->
                if (prefix.startsWith("(")) lower == prefix else lower == prefix || lower.startsWith(prefix)
            }
        }

        private fun normalizeRtdbBaseUrls(rawTarget: String): List<String> {
            val cleaned = rawTarget.trim().trimEnd('/')
            if (cleaned.isEmpty()) return emptyList()

            if (cleaned.startsWith("http://", ignoreCase = true) || cleaned.startsWith("https://", ignoreCase = true)) {
                val noQuery = cleaned.substringBefore("?").substringBefore("#").trimEnd('/')
                val withoutJson = noQuery.removeSuffix("/.json").removeSuffix(".json").trimEnd('/')
                return listOf(withoutJson)
            }

            if (cleaned.contains(".firebasedatabase.app", ignoreCase = true) ||
                cleaned.contains(".firebaseio.com", ignoreCase = true)
            ) {
                val hostOnly = cleaned.substringBefore("/").trimEnd('/')
                return listOf("https://$hostOnly")
            }

            val candidates = mutableListOf(
                "https://$cleaned.asia-southeast1.firebasedatabase.app",
                "https://$cleaned.firebaseio.com"
            )
            if (!cleaned.endsWith("-default-rtdb", ignoreCase = true)) {
                candidates.add("https://$cleaned-default-rtdb.asia-southeast1.firebasedatabase.app")
                candidates.add("https://$cleaned-default-rtdb.firebaseio.com")
            }
            return candidates
        }

        fun buildRtdbUrlCandidates(
            databaseId: String? = null,
            projectId: String? = null,
            path: String,
            explicitDatabaseUrl: String? = null
        ): List<String> {
            val cleanPath = path.trim().trimStart('/').removeSuffix(".json")
            val bases = mutableListOf<String>()

            if (!isPlaceholderCloudId(explicitDatabaseUrl)) {
                bases.addAll(normalizeRtdbBaseUrls(explicitDatabaseUrl!!))
            }
            if (!isPlaceholderCloudId(databaseId)) {
                bases.addAll(normalizeRtdbBaseUrls(databaseId!!))
            }
            if (!isPlaceholderCloudId(projectId)) {
                val cleanProj = projectId!!.trim().trimEnd('/')
                if (!cleanProj.endsWith("-default-rtdb", ignoreCase = true)) {
                    bases.add("https://$cleanProj-default-rtdb.asia-southeast1.firebasedatabase.app")
                    bases.add("https://$cleanProj-default-rtdb.firebaseio.com")
                }
                bases.addAll(normalizeRtdbBaseUrls(cleanProj))
            }

            // Fallback default RTDB
            bases.add("https://foodrock-report-default-rtdb.asia-southeast1.firebasedatabase.app")
            bases.add("https://foodrock-report-default-rtdb.firebaseio.com")

            return bases.distinct().map { base ->
                if (cleanPath.isEmpty()) "${base.trimEnd('/')}/.json" else "${base.trimEnd('/')}/$cleanPath.json"
            }
        }

        fun appendAuthTokenToRtdbUrl(url: String, authToken: String?): String {
            val token = authToken?.trim().orEmpty()
            if (token.isEmpty()) return url
            val sep = if (url.contains("?")) "&" else "?"
            val encoded = try {
                java.net.URLEncoder.encode(token, "UTF-8")
            } catch (_: Throwable) {
                token
            }
            return "$url${sep}auth=$encoded"
        }

        fun isAiStudioProxyUrl(url: String?): Boolean {
            val cleaned = url?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (cleaned.isEmpty()) return false
            return (cleaned.contains("ais-dev-") || cleaned.contains("ais-pre-")) && cleaned.contains(".run.app")
        }

        fun buildHostingRelayUrl(hostingUrl: String?, path: String): String? {
            val raw = hostingUrl?.trim().orEmpty()
            if (raw.isEmpty()) return null
            if (!raw.startsWith("http://", ignoreCase = true) && !raw.startsWith("https://", ignoreCase = true)) {
                return null
            }
            return try {
                val noQuery = raw.substringBefore("?").substringBefore("#").trimEnd('/')
                val schemeIdx = noQuery.indexOf("://")
                if (schemeIdx < 0) return null
                val scheme = noQuery.substring(0, schemeIdx)
                val afterScheme = noQuery.substring(schemeIdx + 3)
                val hostAndPort = afterScheme.substringBefore("/")
                if (hostAndPort.isBlank()) return null
                val origin = "$scheme://$hostAndPort"
                val cleanPath = path.trim().trimStart('/').removeSuffix(".json")
                if (cleanPath.isEmpty()) "$origin/api/sync" else "$origin/api/sync/$cleanPath"
            } catch (_: Throwable) {
                null
            }
        }

        fun isLoopbackUrl(url: String?): Boolean {
            val raw = url?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (raw.isEmpty()) return false
            val withoutScheme = raw.substringAfter("://", raw).substringBefore("/").substringBefore("?").substringBefore(":")
            return withoutScheme == "127.0.0.1" || withoutScheme == "localhost" || withoutScheme == "10.0.2.2"
        }
    }

    private fun resolveConfiguredRtdbUrl(): String? {
        return try {
            val fromApp = if (ensureFirebaseInitialized()) {
                FirebaseApp.getApps(context).firstOrNull()?.options?.databaseUrl?.trim()
            } else null
            if (!isPlaceholderCloudId(fromApp)) return fromApp

            val resId = context.resources.getIdentifier("firebase_database_url", "string", context.packageName)
            if (resId != 0) {
                val fromRes = context.getString(resId).trim()
                if (!isPlaceholderCloudId(fromRes)) return fromRes
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun fetchAuthTokenOrNull(): String? {
        return try {
            val user = auth?.currentUser ?: return null
            withTimeoutOrNull(1500L) {
                user.getIdToken(false).await()?.token
            }
        } catch (_: Throwable) {
            null
        }
    }

    private data class RtdbWriteAttempt(
        val ok: Boolean,
        val endpoint: String? = null,
        val httpCode: Int? = null,
        val error: String? = null
    )

    data class ExpenseSyncResult(
        val relayOk: Boolean,
        val rtdbOk: Boolean,
        val isLoopbackRelay: Boolean,
        val dailyRtdbOk: Boolean = false,
        val rtdbHttpCode: Int? = null,
        val rtdbError: String? = null,
        val rtdbEndpoint: String? = null
    ) {
        val anyOk: Boolean get() = relayOk || rtdbOk
        val remoteOk: Boolean get() = rtdbOk || (!isLoopbackRelay && relayOk)
    }

    /**
     * Checks if Firebase Realtime Database Cloud, Hosting Relay, and Internet are reachable.
     */
    suspend fun checkCloudConnection(
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val isConnectedToNetwork = try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                @Suppress("DEPRECATION")
                val activeNetwork = cm?.activeNetworkInfo
                activeNetwork?.isConnected == true
            } catch (t: Throwable) {
                true
            }

            if (!isConnectedToNetwork) {
                return@withContext Pair(false, "Perangkat sedang offline (tidak ada koneksi internet). Data kasir tersimpan aman di Room DB.")
            }

            // 1. Check Hosting Relay health first if available (ignoring AI Studio auth proxy URLs)
            val candidateRelayBase = if (!isAiStudioProxyUrl(hostingUrl) && !hostingUrl.isNullOrBlank()) {
                hostingUrl
            } else if (com.example.service.LocalWebReportServer.isRunning()) {
                com.example.service.LocalWebReportServer.getLocalBaseUrl()
            } else {
                null
            }
            val relayHealthUrl = buildHostingRelayUrl(candidateRelayBase, "")?.let {
                it.removeSuffix("/api/sync") + "/api/health"
            }
            if (relayHealthUrl != null) {
                try {
                    val req = Request.Builder().url(relayHealthUrl).get().build()
                    rtdbClient.newCall(req).execute().use { resp ->
                        val cType = resp.header("Content-Type").orEmpty()
                        val bodyStr = resp.body?.string().orEmpty()
                        if (resp.isSuccessful && !resp.isRedirect && !isHtmlResponse(cType, bodyStr)) {
                            return@withContext Pair(true, "🟢 Terhubung ke Web Chrome Server Aktif ($relayHealthUrl)")
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            // 2. Ping Firebase Realtime Database (via lightweight HTTP REST)
            val rtdbUrls = buildRtdbUrlCandidates(databaseId = databaseId, projectId = projectId, path = "")
                .map { "$it?shallow=true" }
            for (url in rtdbUrls) {
                try {
                    val req = Request.Builder().url(url).head().build()
                    rtdbClient.newCall(req).execute().use { resp ->
                        if (resp.code == 200 || resp.code == 401 || resp.code == 403) {
                            return@withContext Pair(true, "🟢 Terhubung ke Firebase Realtime Database")
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            // 3. Fallback Internet Check (generate_204)
            val internetReachable = try {
                val req = Request.Builder().url("https://www.google.com/generate_204").head().build()
                rtdbClient.newCall(req).execute().use { resp -> resp.isSuccessful || resp.code == 204 }
            } catch (_: Throwable) {
                false
            }

            if (internetReachable) {
                Pair(true, "🟢 Koneksi Internet Aktif. Data siap disinkronkan.")
            } else {
                Pair(false, "Server Cloud belum merespons. Data tersimpan aman offline di Room DB.")
            }
        } catch (t: Throwable) {
            Log.w("FirebaseSync", "Cloud connection test note: ${t.message}")
            Pair(false, "Koneksi offline: ${t.localizedMessage ?: "Tersimpan aman di Room DB"}")
        }
    }

    /**
     * Menyinkronkan transaksi penjualan langsung ke Firebase Realtime Database & Hosting Web Chrome Relay.
     */
    suspend fun syncTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
        storeId: String = "store_main",
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveUserId = try {
                auth?.currentUser?.uid ?: "pos_terminal_${storeId}"
            } catch (t: Throwable) {
                "pos_terminal_${storeId}"
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val dateStr = dateFormat.format(Date(transaction.timestamp))
            val isoStr = isoFormat.format(Date(transaction.timestamp))

            val itemsSummary = items.joinToString(", ") { "${it.productName} (${it.quantity}x)" }
            val itemsList = items.map { item ->
                mapOf(
                    "id" to item.id,
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "unitPrice" to item.unitPrice,
                    "quantity" to item.quantity,
                    "subtotal" to item.subtotal,
                    "notes" to (item.itemNotes ?: "")
                )
            }

            val payload = hashMapOf<String, Any>(
                "id" to transaction.invoiceNumber,
                "invoiceNumber" to transaction.invoiceNumber,
                "orderNumber" to transaction.invoiceNumber,
                "customerName" to transaction.customerName,
                "orderType" to transaction.orderType.name,
                "tableNumber" to (transaction.tableNumber ?: ""),
                "notes" to (transaction.notes ?: ""),
                "itemsSummary" to itemsSummary,
                "items" to itemsList,
                "subtotalAmount" to transaction.subtotal,
                "discountAmount" to transaction.discount,
                "taxAmount" to transaction.tax,
                "totalAmount" to transaction.grandTotal,
                "grandTotal" to transaction.grandTotal,
                "profitAmount" to transaction.profitAmount,
                "totalItemsCount" to transaction.totalItemsCount,
                "paymentMethod" to transaction.paymentMethod.name,
                "cashGiven" to transaction.amountPaid,
                "changeAmount" to transaction.changeAmount,
                "cashierName" to transaction.cashierName,
                "cashierRole" to "Kasir",
                "timestamp" to isoStr,
                "timestampMillis" to transaction.timestamp,
                "date" to dateStr,
                "storeId" to storeId,
                "userId" to effectiveUserId,
                "syncedAt" to isoStr
            )

            val rtdbPayload = JSONObject(payload as Map<*, *>).toString()
            val statsJson = JSONObject(mapOf(
                "lastTransactionAmount" to transaction.grandTotal,
                "lastInvoiceNumber" to transaction.invoiceNumber,
                "lastCustomerName" to transaction.customerName,
                "lastPaymentMethod" to transaction.paymentMethod.name,
                "lastCashierName" to transaction.cashierName,
                "lastItemsSummary" to itemsSummary,
                "lastTotalItemsCount" to transaction.totalItemsCount,
                "lastUpdated" to isoStr,
                "date" to dateStr,
                "storeId" to storeId
            )).toString()

            val trackingMap = mutableMapOf<String, Any>(
                "orderNumber" to transaction.invoiceNumber,
                "invoiceNumber" to transaction.invoiceNumber,
                "customerName" to transaction.customerName,
                "orderType" to transaction.orderType.name,
                "tableNumber" to (transaction.tableNumber ?: ""),
                "itemsSummary" to itemsSummary,
                "items" to itemsList,
                "totalAmount" to transaction.grandTotal,
                "grandTotal" to transaction.grandTotal,
                "paymentMethod" to transaction.paymentMethod.name,
                "status" to "SELESAI",
                "orderStatus" to "SELESAI",
                "paymentStatus" to "LUNAS",
                "cashierName" to transaction.cashierName,
                "date" to dateStr,
                "timestamp" to isoStr,
                "syncedAt" to isoStr,
                "notes" to (transaction.notes ?: "")
            )
            val trackingJson = JSONObject(trackingMap as Map<*, *>).toString()

            val relayTrxOk = sendToHostingRelay(hostingUrl, "transactions/${transaction.invoiceNumber}", rtdbPayload, "PUT")
            sendToHostingRelay(hostingUrl, "reports/daily_transactions/$dateStr/${transaction.invoiceNumber}", rtdbPayload, "PUT")
            sendToHostingRelay(hostingUrl, "realtime_stats/today", statsJson, "PUT")
            sendToHostingRelay(hostingUrl, "customer_tracking/${transaction.invoiceNumber}", trackingJson, "PUT")
            sendToHostingRelay(hostingUrl, "purchase_tracking/${transaction.invoiceNumber}", trackingJson, "PUT")

            var rtdbSuccess = false
            try {
                rtdbSuccess = syncToRealtimeDatabase(databaseId, "transactions/${transaction.invoiceNumber}", rtdbPayload, projectId)
                syncToRealtimeDatabase(databaseId, "reports/daily_transactions/$dateStr/${transaction.invoiceNumber}", rtdbPayload, projectId)
                syncToRealtimeDatabase(databaseId, "realtime_stats/today", statsJson, projectId)
                syncToRealtimeDatabase(databaseId, "customer_tracking/${transaction.invoiceNumber}", trackingJson, projectId)
                syncToRealtimeDatabase(databaseId, "purchase_tracking/${transaction.invoiceNumber}", trackingJson, projectId)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w("FirebaseSync", "RTDB sync note: ${t.message}")
            }

            Log.d("FirebaseSync", "Transaction synced (RTDB=$rtdbSuccess, Relay=$relayTrxOk): ${transaction.invoiceNumber}")
            rtdbSuccess || relayTrxOk
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Safe catch: Failed to sync transaction ${transaction.invoiceNumber}", t)
            false
        }
    }

    /**
     * Menyinkronkan laporan tutup kasir (shift closing) ke Realtime Database & Hosting Web Chrome Relay.
     */
    suspend fun syncShiftClosing(
        closing: ShiftClosing,
        storeId: String = "store_main",
        databaseId: String? = null,
        totalExpenses: Double = 0.0,
        expenseNotes: String = "",
        projectId: String? = null,
        hostingUrl: String? = null,
        expensesList: List<Expense> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveUserId = try {
                auth?.currentUser?.uid ?: "pos_terminal_${storeId}"
            } catch (t: Throwable) {
                "pos_terminal_${storeId}"
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val dateStr = dateFormat.format(Date(closing.closingTime))
            val isoStr = isoFormat.format(Date(closing.closingTime))

            val docId = "SHIFT-${closing.id.takeIf { it > 0 } ?: closing.closingTime}"
            val expensePayloadList = expensesList.map { buildExpensePayloadMap(it, storeId) }
            val resolvedTotalExp = if (totalExpenses > 0.0) totalExpenses else expensesList.sumOf { it.amount }
            val resolvedExpNotes = expenseNotes.ifBlank {
                expensesList.joinToString("; ") { exp ->
                    val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                    "${exp.title} [${exp.category}]: Rp ${String.format(Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
                }
            }

            val payload = hashMapOf<String, Any>(
                "id" to docId,
                "shiftNumber" to "Shift-${closing.id}",
                "cashierName" to closing.cashierName,
                "cashierRole" to "Kasir",
                "startTime" to isoStr,
                "endTime" to isoStr,
                "totalTransactions" to closing.totalTransactions.toDouble(),
                "totalRevenue" to closing.totalRevenue,
                "grossRevenue" to closing.totalRevenue,
                "totalCash" to closing.totalCash,
                "totalNonCash" to closing.totalNonCash,
                "totalExpenses" to resolvedTotalExp,
                "expenseNotes" to resolvedExpNotes,
                "expensesList" to expensePayloadList,
                "expectedCash" to closing.expectedCash,
                "actualCash" to closing.actualCash,
                "difference" to closing.difference,
                "itemsSold" to 0.0,
                "notes" to (closing.notes ?: ""),
                "timestamp" to isoStr,
                "timestampMillis" to closing.closingTime,
                "date" to dateStr,
                "storeId" to storeId,
                "userId" to effectiveUserId,
                "status" to "CLOSED"
            )

            val rtdbPayload = JSONObject(payload as Map<*, *>).toString()
            val dailyPayload = JSONObject(mapOf(
                "date" to dateStr,
                "totalRevenue" to closing.totalRevenue,
                "grossRevenue" to closing.totalRevenue,
                "totalCash" to closing.totalCash,
                "totalNonCash" to closing.totalNonCash,
                "totalExpenses" to resolvedTotalExp,
                "expenseNotes" to resolvedExpNotes,
                "expensesList" to expensePayloadList,
                "totalTransactions" to closing.totalTransactions,
                "expectedCash" to closing.expectedCash,
                "actualCash" to closing.actualCash,
                "difference" to closing.difference,
                "lastShiftId" to docId,
                "lastShiftCashier" to closing.cashierName,
                "archivedByTutupToko" to true,
                "lastUpdated" to isoStr,
                "storeId" to storeId
            )).toString()
            val statsShiftPayload = JSONObject(mapOf(
                "lastShiftClosingId" to docId,
                "lastShiftCashier" to closing.cashierName,
                "lastShiftRevenue" to closing.totalRevenue,
                "lastShiftActualCash" to closing.actualCash,
                "lastShiftExpenses" to resolvedTotalExp,
                "lastShiftDifference" to closing.difference,
                "lastShiftTimestamp" to isoStr,
                "date" to dateStr
            )).toString()

            val relayShiftOk = sendToHostingRelay(hostingUrl, "shift_reports/$docId", rtdbPayload, "PUT")
            sendToHostingRelay(hostingUrl, "daily_reports/$dateStr", dailyPayload, "PATCH")
            sendToHostingRelay(hostingUrl, "realtime_stats/last_shift", statsShiftPayload, "PUT")

            var rtdbShiftSuccess = false
            try {
                rtdbShiftSuccess = syncToRealtimeDatabase(databaseId, "shift_reports/$docId", rtdbPayload, projectId)
                patchToRealtimeDatabase(databaseId, "daily_reports/$dateStr", dailyPayload, projectId)
                syncToRealtimeDatabase(databaseId, "realtime_stats/last_shift", statsShiftPayload, projectId)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w("FirebaseSync", "RTDB shift sync note: ${t.message}")
            }

            Log.d("FirebaseSync", "Shift closing sync result ($docId): RTDB=$rtdbShiftSuccess Relay=$relayShiftOk")
            rtdbShiftSuccess || relayShiftOk
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Safe catch: Failed to sync shift closing", t)
            false
        }
    }

    /**
     * Membangun payload JSON Map untuk catatan pengeluaran agar kompatibel di semua portal Web Chrome (/expenses).
     */
    fun buildExpensePayloadMap(
        expense: Expense,
        storeId: String = "store_main"
    ): Map<String, Any> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val dateStr = dateFormat.format(Date(expense.timestamp))
        val isoStr = isoFormat.format(Date(expense.timestamp))
        val expKey = "EXP-${expense.id.takeIf { it > 0 } ?: expense.timestamp}"
        val author = expense.createdBy.ifBlank { "Kasir" }
        return mapOf(
            "id" to expKey,
            "localId" to expense.id,
            "title" to expense.title,
            "amount" to expense.amount,
            "category" to expense.category.ifBlank { "Operasional" },
            "notes" to expense.notes,
            "createdBy" to author,
            "cashierName" to author,
            "date" to dateStr,
            "timestamp" to isoStr,
            "timestampMillis" to expense.timestamp,
            "syncedAt" to isoStr,
            "storeId" to storeId
        )
    }

    /**
     * Menyinkronkan catatan pengeluaran operasional toko langsung ke /expenses/{id}
     * (Firebase Realtime Database + Hosting Web Chrome Relay) dan memperbarui statistik live.
     */
    suspend fun syncExpense(
        expense: Expense,
        storeId: String = "store_main",
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ): Boolean = syncExpenseDetailed(
        expense = expense,
        storeId = storeId,
        databaseId = databaseId,
        projectId = projectId,
        hostingUrl = hostingUrl
    ).anyOk

    suspend fun syncExpenseDetailed(
        expense: Expense,
        storeId: String = "store_main",
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ): ExpenseSyncResult = withContext(Dispatchers.IO) {
        val loopbackRelay = isLoopbackUrl(hostingUrl)
        try {
            val payloadMap = buildExpensePayloadMap(expense, storeId)
            val expKey = payloadMap["id"] as String
            val dateStr = (payloadMap["date"] as? String).orEmpty()
            val isoStr = (payloadMap["syncedAt"] as? String).orEmpty()
            val payload = JSONObject(payloadMap).toString()
            val cashFlowKey = "CF-$expKey"
            val cashFlowPayload = JSONObject(
                mapOf(
                    "id" to cashFlowKey,
                    "type" to "OUT",
                    "category" to expense.category.ifBlank { "Operasional" },
                    "description" to expense.title,
                    "notes" to expense.notes,
                    "amount" to expense.amount,
                    "cashierName" to expense.createdBy.ifBlank { "Kasir" },
                    "date" to dateStr,
                    "timestamp" to isoStr,
                    "timestampMillis" to expense.timestamp,
                    "storeId" to storeId
                )
            ).toString()
            val statsExpensePayload = JSONObject(
                mapOf(
                    "lastExpenseId" to expKey,
                    "lastExpenseTitle" to expense.title,
                    "lastExpenseAmount" to expense.amount,
                    "lastExpenseCategory" to expense.category,
                    "lastExpenseNotes" to expense.notes,
                    "lastExpenseBy" to expense.createdBy,
                    "date" to dateStr,
                    "timestampMillis" to expense.timestamp,
                    "lastUpdated" to isoStr
                )
            ).toString()

            val relayPrimaryOk = sendToHostingRelay(hostingUrl, "expenses/$expKey", payload, "PUT")
            val relayDailyOk = if (dateStr.isNotBlank()) {
                sendToHostingRelay(hostingUrl, "reports/daily_expenses/$dateStr/$expKey", payload, "PUT")
            } else false
            val relayStatsExpOk = sendToHostingRelay(hostingUrl, "realtime_stats/expenses/$expKey", payload, "PUT")
            val relayStatsDailyOk = if (dateStr.isNotBlank()) {
                sendToHostingRelay(hostingUrl, "realtime_stats/expenses_by_date/$dateStr/$expKey", payload, "PUT")
            } else false
            if (relayPrimaryOk || relayDailyOk || relayStatsExpOk || relayStatsDailyOk) {
                sendToHostingRelay(hostingUrl, "cash_flows/$cashFlowKey", cashFlowPayload, "PUT")
                sendToHostingRelay(hostingUrl, "realtime_stats/last_expense", statsExpensePayload, "PUT")
            }
            val relayOk = relayPrimaryOk || relayDailyOk || relayStatsExpOk || relayStatsDailyOk

            // Mirror also to embedded LocalWebReportServer when hostingUrl is remote so Chrome localRelay stays fresh
            if (!loopbackRelay && com.example.service.LocalWebReportServer.isRunning()) {
                val localBase = com.example.service.LocalWebReportServer.getLocalBaseUrl()
                sendToHostingRelay(localBase, "expenses/$expKey", payload, "PUT")
                sendToHostingRelay(localBase, "realtime_stats/expenses/$expKey", payload, "PUT")
                if (dateStr.isNotBlank()) {
                    sendToHostingRelay(localBase, "reports/daily_expenses/$dateStr/$expKey", payload, "PUT")
                    sendToHostingRelay(localBase, "realtime_stats/expenses_by_date/$dateStr/$expKey", payload, "PUT")
                }
                sendToHostingRelay(localBase, "cash_flows/$cashFlowKey", cashFlowPayload, "PUT")
                sendToHostingRelay(localBase, "realtime_stats/last_expense", statsExpensePayload, "PUT")
            }

            // Write sub-paths independently + mirror into /realtime_stats so older rules allowing /realtime_stats still persist expenses & notes
            val primaryAttempt = sendToRealtimeDatabaseDetailed(databaseId, "expenses/$expKey", payload, usePatch = false, projectId = projectId)
            val dailyAttempt = if (dateStr.isNotBlank()) {
                sendToRealtimeDatabaseDetailed(databaseId, "reports/daily_expenses/$dateStr/$expKey", payload, usePatch = false, projectId = projectId)
            } else {
                RtdbWriteAttempt(ok = false)
            }
            val statsExpAttempt = sendToRealtimeDatabaseDetailed(databaseId, "realtime_stats/expenses/$expKey", payload, usePatch = false, projectId = projectId)
            val statsDailyExpAttempt = if (dateStr.isNotBlank()) {
                sendToRealtimeDatabaseDetailed(databaseId, "realtime_stats/expenses_by_date/$dateStr/$expKey", payload, usePatch = false, projectId = projectId)
            } else {
                RtdbWriteAttempt(ok = false)
            }
            sendToRealtimeDatabaseDetailed(databaseId, "cash_flows/$cashFlowKey", cashFlowPayload, usePatch = false, projectId = projectId)
            sendToRealtimeDatabaseDetailed(databaseId, "realtime_stats/last_expense", statsExpensePayload, usePatch = false, projectId = projectId)

            val rtdbOk = primaryAttempt.ok || dailyAttempt.ok || statsExpAttempt.ok || statsDailyExpAttempt.ok
            val anyEndpoint = primaryAttempt.endpoint
                ?: dailyAttempt.endpoint
                ?: statsExpAttempt.endpoint
                ?: statsDailyExpAttempt.endpoint
            val diagCode = primaryAttempt.httpCode
                ?: dailyAttempt.httpCode
                ?: statsExpAttempt.httpCode
                ?: statsDailyExpAttempt.httpCode
            ExpenseSyncResult(
                relayOk = relayOk,
                rtdbOk = rtdbOk,
                isLoopbackRelay = loopbackRelay,
                dailyRtdbOk = dailyAttempt.ok || statsDailyExpAttempt.ok,
                rtdbHttpCode = diagCode,
                rtdbError = if (!rtdbOk) (primaryAttempt.error ?: dailyAttempt.error ?: statsExpAttempt.error) else null,
                rtdbEndpoint = anyEndpoint
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w("FirebaseSync", "Failed to sync expense ${expense.id}: ${t.message}")
            ExpenseSyncResult(
                relayOk = false,
                rtdbOk = false,
                isLoopbackRelay = loopbackRelay,
                rtdbError = t.message
            )
        }
    }

    /**
     * Memperbarui / mengosongkan node /realtime_stats/last_expense agar pengeluaran yang sudah dihapus
     * di aplikasi tidak muncul kembali sebagai fallback di laporan Web Chrome.
     */
    suspend fun syncLastExpenseStat(
        latestExpense: Expense?,
        todayTotal: Double = 0.0,
        todayDateStr: String? = null,
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val now = Date()
            val resolvedDate = latestExpense?.let { dateFmt.format(Date(it.timestamp)) }
                ?: todayDateStr?.takeIf { it.isNotBlank() }
                ?: dateFmt.format(now)
            val payload = JSONObject(
                mapOf(
                    "lastExpenseId" to (latestExpense?.let { "EXP-${it.id.takeIf { id -> id > 0 } ?: it.timestamp}" } ?: ""),
                    "lastExpenseTitle" to (latestExpense?.title ?: ""),
                    "lastExpenseAmount" to (latestExpense?.amount ?: 0.0),
                    "lastExpenseCategory" to (latestExpense?.category?.ifBlank { "Operasional" } ?: "Operasional"),
                    "lastExpenseNotes" to (latestExpense?.notes ?: ""),
                    "lastExpenseBy" to (latestExpense?.createdBy?.ifBlank { "Kasir" } ?: ""),
                    "todayExpensesTotal" to todayTotal,
                    "date" to resolvedDate,
                    "timestampMillis" to (latestExpense?.timestamp ?: 0L),
                    "lastUpdated" to isoFmt.format(now)
                )
            ).toString()

            sendToHostingRelay(hostingUrl, "realtime_stats/last_expense", payload, "PUT")
            if (!isLoopbackUrl(hostingUrl) && com.example.service.LocalWebReportServer.isRunning()) {
                sendToHostingRelay(com.example.service.LocalWebReportServer.getLocalBaseUrl(), "realtime_stats/last_expense", payload, "PUT")
            }
            sendToRealtimeDatabaseDetailed(databaseId, "realtime_stats/last_expense", payload, usePatch = false, projectId = projectId)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.d("FirebaseSync", "syncLastExpenseStat note: ${t.message}")
        }
    }

    /**
     * Menyinkronkan ulang seluruh daftar catatan pengeluaran ke Firebase Realtime Database & Hosting Web Chrome.
     */
    suspend fun syncAllExpenses(
        expenses: List<Expense>,
        storeId: String = "store_main",
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        for (expense in expenses) {
            if (syncExpense(expense, storeId = storeId, databaseId = databaseId, projectId = projectId, hostingUrl = hostingUrl)) {
                successCount++
            }
        }
        successCount to expenses.size
    }

    /**
     * Menghapus catatan pengeluaran dari node /expenses, /cash_flows, dan /reports/daily_expenses
     * di Realtime Database & Hosting Web Chrome Relay, sekaligus memperbarui /realtime_stats/last_expense.
     */
    suspend fun deleteExpenseFromRtdb(
        expenseId: Long,
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null,
        timestampMillis: Long? = null,
        latestRemainingExpense: Expense? = null,
        remainingTodayTotal: Double = 0.0,
        todayDateStr: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (expenseId <= 0L) return@withContext false
        val expKey = "EXP-$expenseId"
        val path = "expenses/$expKey"
        val cashFlowPath = "cash_flows/CF-$expKey"
        val legacyCashFlowPath = "cash_flows/$expKey"
        val statsPath = "realtime_stats/expenses/$expKey"
        val dateStr = timestampMillis?.let { ts ->
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))
        }
        val dailyPath = dateStr?.let { "reports/daily_expenses/$it/$expKey" }
        val statsDailyPath = dateStr?.let { "realtime_stats/expenses_by_date/$it/$expKey" }

        val relayDeleted = sendToHostingRelay(hostingUrl, path, null, "DELETE")
        sendToHostingRelay(hostingUrl, cashFlowPath, null, "DELETE")
        sendToHostingRelay(hostingUrl, legacyCashFlowPath, null, "DELETE")
        sendToHostingRelay(hostingUrl, statsPath, null, "DELETE")
        if (dailyPath != null) {
            sendToHostingRelay(hostingUrl, dailyPath, null, "DELETE")
        }
        if (statsDailyPath != null) {
            sendToHostingRelay(hostingUrl, statsDailyPath, null, "DELETE")
        }
        if (!isLoopbackUrl(hostingUrl) && com.example.service.LocalWebReportServer.isRunning()) {
            val localBase = com.example.service.LocalWebReportServer.getLocalBaseUrl()
            sendToHostingRelay(localBase, path, null, "DELETE")
            sendToHostingRelay(localBase, cashFlowPath, null, "DELETE")
            sendToHostingRelay(localBase, legacyCashFlowPath, null, "DELETE")
            sendToHostingRelay(localBase, statsPath, null, "DELETE")
            if (dailyPath != null) sendToHostingRelay(localBase, dailyPath, null, "DELETE")
            if (statsDailyPath != null) sendToHostingRelay(localBase, statsDailyPath, null, "DELETE")
        }

        val rtdbDeleted = deleteRtdbNode(databaseId, projectId, path)
        deleteRtdbNode(databaseId, projectId, cashFlowPath)
        deleteRtdbNode(databaseId, projectId, legacyCashFlowPath)
        val rtdbStatsDeleted = deleteRtdbNode(databaseId, projectId, statsPath)
        if (dailyPath != null) {
            deleteRtdbNode(databaseId, projectId, dailyPath)
        }
        if (statsDailyPath != null) {
            deleteRtdbNode(databaseId, projectId, statsDailyPath)
        }
        syncLastExpenseStat(
            latestExpense = latestRemainingExpense,
            todayTotal = remainingTodayTotal,
            todayDateStr = todayDateStr,
            databaseId = databaseId,
            projectId = projectId,
            hostingUrl = hostingUrl
        )
        relayDeleted || rtdbDeleted || rtdbStatsDeleted
    }

    private suspend fun deleteRtdbNode(
        databaseId: String?,
        projectId: String?,
        nodePath: String
    ): Boolean {
        val authToken = fetchAuthTokenOrNull()
        val configuredDbUrl = resolveConfiguredRtdbUrl()
        val candidates = buildRtdbUrlCandidates(
            databaseId = databaseId,
            projectId = projectId,
            path = nodePath,
            explicitDatabaseUrl = configuredDbUrl
        )
        var anyDeleted = false
        for (baseUrl in candidates) {
            val targetUrls = if (!authToken.isNullOrBlank()) {
                listOf(appendAuthTokenToRtdbUrl(baseUrl, authToken), baseUrl)
            } else {
                listOf(baseUrl)
            }
            for (url in targetUrls) {
                try {
                    val req = Request.Builder().url(url).delete().build()
                    rtdbClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            anyDeleted = true
                            break
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            if (anyDeleted) break
        }
        return anyDeleted
    }

    private suspend fun fetchRtdbNodeJson(
        databaseId: String?,
        projectId: String?,
        nodePath: String,
        hostingUrl: String? = null
    ): JSONObject? = withContext(Dispatchers.IO) {
        val authToken = fetchAuthTokenOrNull()
        val configuredDbUrl = resolveConfiguredRtdbUrl()
        val candidates = buildRtdbUrlCandidates(
            databaseId = databaseId,
            projectId = projectId,
            path = nodePath,
            explicitDatabaseUrl = configuredDbUrl
        )
        for (baseUrl in candidates) {
            val targetUrls = if (!authToken.isNullOrBlank()) {
                listOf(appendAuthTokenToRtdbUrl(baseUrl, authToken), baseUrl)
            } else {
                listOf(baseUrl)
            }
            for (url in targetUrls) {
                try {
                    val req = Request.Builder().url(url).get().build()
                    rtdbClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty().trim()
                        if (resp.isSuccessful && body.isNotEmpty() && body != "null" && body.startsWith("{")) {
                            return@withContext JSONObject(body)
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
        val relayUrl = buildHostingRelayUrl(hostingUrl, nodePath)
        if (!relayUrl.isNullOrBlank()) {
            try {
                val req = Request.Builder().url(relayUrl).get().build()
                rtdbClient.newCall(req).execute().use { resp ->
                    val contentType = resp.header("Content-Type").orEmpty()
                    val body = resp.body?.string().orEmpty().trim()
                    if (resp.isSuccessful && !isHtmlResponse(contentType, body) && body.startsWith("{")) {
                        return@withContext JSONObject(body)
                    }
                }
            } catch (_: Throwable) {
            }
        }
        null
    }

    /**
     * Rekonsiliasi dua arah catatan pengeluaran antara Room DB dan Firebase RTDB/Relay:
     * 1) Menghapus sisa pengeluaran lama (EXP-<id Room>) di RTDB/Relay yang sudah dihapus di aplikasi.
     * 2) Mengimpor pengeluaran yang dicatat langsung dari Web Chrome (EXP-<timestamp 13 digit> / CASH-<timestamp>) ke Room lalu kanonisasi ke EXP-<roomId>.
     * 3) Memastikan /realtime_stats/last_expense selalu mencerminkan pengeluaran terbaru yang benar-benar ada.
     */
    suspend fun reconcileExpensesWithRtdb(
        localExpenses: List<Expense>,
        alreadyHandledWebKeys: Set<String> = emptySet(),
        storeId: String = "store_main",
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null,
        onImportWebExpense: (suspend (remoteKey: String, parsed: Expense) -> Expense?)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val expNode = fetchRtdbNodeJson(databaseId, projectId, "expenses", hostingUrl)
            val dailyExpNode = fetchRtdbNodeJson(databaseId, projectId, "reports/daily_expenses", hostingUrl)
            val statsNode = fetchRtdbNodeJson(databaseId, projectId, "realtime_stats", hostingUrl)

            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val currentExpenses = localExpenses.toMutableList()
            val localIds = currentExpenses.map { it.id }.toMutableSet()

            fun cleanRemoteNote(raw: String?): String {
                val t = raw?.trim().orEmpty()
                return if (t.isEmpty() || t == "-" || t.equals("null", ignoreCase = true)) "" else t
            }

            fun parseTimestamp(obj: JSONObject, fallbackDate: String?): Long {
                val tsMillis = obj.optLong("timestampMillis", 0L)
                if (tsMillis > 1_000_000_000_000L) return tsMillis
                val iso = obj.optString("timestamp", "").ifBlank { obj.optString("syncedAt", "") }
                if (iso.isNotBlank()) {
                    try {
                        val clean = iso.substringBefore(".").removeSuffix("Z")
                        isoFmt.parse(clean)?.time?.let { return it }
                    } catch (_: Throwable) {
                    }
                }
                val d = obj.optString("date", "").ifBlank { fallbackDate.orEmpty() }
                if (d.length >= 10) {
                    try {
                        dateFmt.parse(d.take(10))?.time?.let { return it + 12L * 3600_000L }
                    } catch (_: Throwable) {
                    }
                }
                return System.currentTimeMillis()
            }

            data class RemoteOcc(
                val rawKey: String,
                val path: String,
                val dateHint: String?,
                val payload: JSONObject
            )

            val occurrencesByKey = linkedMapOf<String, MutableList<RemoteOcc>>()
            fun addOcc(rawKey: String, path: String, dateHint: String?, obj: JSONObject?) {
                if (obj == null) return
                val normKey = rawKey.trim().removePrefix("CF-")
                if (normKey.isBlank()) return
                occurrencesByKey.getOrPut(normKey) { mutableListOf() }.add(RemoteOcc(normKey, path, dateHint, obj))
            }

            if (expNode != null) {
                val keys = expNode.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    addOcc(k, "expenses/$k", null, expNode.optJSONObject(k))
                }
            }
            if (dailyExpNode != null) {
                val dKeys = dailyExpNode.keys()
                while (dKeys.hasNext()) {
                    val dKey = dKeys.next()
                    val dayObj = dailyExpNode.optJSONObject(dKey) ?: continue
                    val eKeys = dayObj.keys()
                    while (eKeys.hasNext()) {
                        val k = eKeys.next()
                        addOcc(k, "reports/daily_expenses/$dKey/$k", dKey, dayObj.optJSONObject(k))
                    }
                }
            }
            val statsExps = statsNode?.optJSONObject("expenses")
            if (statsExps != null) {
                val keys = statsExps.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    addOcc(k, "realtime_stats/expenses/$k", null, statsExps.optJSONObject(k))
                }
            }
            val statsDailyExps = statsNode?.optJSONObject("expenses_by_date")
            if (statsDailyExps != null) {
                val dKeys = statsDailyExps.keys()
                while (dKeys.hasNext()) {
                    val dKey = dKeys.next()
                    val dayObj = statsDailyExps.optJSONObject(dKey) ?: continue
                    val eKeys = dayObj.keys()
                    while (eKeys.hasNext()) {
                        val k = eKeys.next()
                        addOcc(k, "realtime_stats/expenses_by_date/$dKey/$k", dKey, dayObj.optJSONObject(k))
                    }
                }
            }

            suspend fun purgeRemoteKey(normKey: String, occs: List<RemoteOcc>) {
                val extraDates = occs.mapNotNull { occ ->
                    occ.dateHint?.takeIf { it.isNotBlank() }
                        ?: occ.payload.optString("date", "").takeIf { it.length >= 10 }?.take(10)
                }.toMutableSet()
                val allPaths = linkedSetOf(
                    "expenses/$normKey",
                    "cash_flows/CF-$normKey",
                    "cash_flows/$normKey",
                    "realtime_stats/expenses/$normKey"
                )
                extraDates.forEach { d ->
                    allPaths.add("reports/daily_expenses/$d/$normKey")
                    allPaths.add("realtime_stats/expenses_by_date/$d/$normKey")
                }
                occs.forEach { allPaths.add(it.path) }

                for (p in allPaths) {
                    sendToHostingRelay(hostingUrl, p, null, "DELETE")
                    if (!isLoopbackUrl(hostingUrl) && com.example.service.LocalWebReportServer.isRunning()) {
                        sendToHostingRelay(com.example.service.LocalWebReportServer.getLocalBaseUrl(), p, null, "DELETE")
                    }
                    deleteRtdbNode(databaseId, projectId, p)
                }
            }

            for ((normKey, occs) in occurrencesByKey) {
                val primaryObj = occs.firstOrNull { it.path.startsWith("expenses/") }?.payload
                    ?: occs.firstOrNull { it.path.startsWith("reports/daily_expenses/") }?.payload
                    ?: occs.first().payload

                val expNum = when {
                    normKey.startsWith("EXP-", ignoreCase = true) -> normKey.drop(4).toLongOrNull()
                    normKey.startsWith("CASH-", ignoreCase = true) -> normKey.drop(5).toLongOrNull()
                    else -> primaryObj.optLong("localId", 0L).takeIf { it > 0L }
                }

                if (normKey.startsWith("EXP-", ignoreCase = true) && expNum != null && expNum > 0L && expNum < 1_000_000_000_000L) {
                    // Record originating from Android Room ID -> prune if deleted locally
                    if (expNum !in localIds) {
                        purgeRemoteKey(normKey, occs)
                    }
                } else if ((normKey.startsWith("EXP-", ignoreCase = true) || normKey.startsWith("CASH-", ignoreCase = true)) &&
                    expNum != null && expNum >= 1_000_000_000_000L
                ) {
                    // Created on Chrome Web with 13-digit epoch ms
                    if (normKey in alreadyHandledWebKeys) {
                        purgeRemoteKey(normKey, occs)
                        continue
                    }
                    val title = primaryObj.optString("title", "").ifBlank {
                        primaryObj.optString("description", "").ifBlank { "Pengeluaran" }
                    }.trim()
                    val amount = primaryObj.optDouble("amount", 0.0)
                    val category = primaryObj.optString("category", "").ifBlank { "Operasional" }.trim()
                    val notes = cleanRemoteNote(primaryObj.optString("notes", ""))
                    val createdBy = primaryObj.optString("createdBy", "").ifBlank {
                        primaryObj.optString("cashierName", "").ifBlank { "Admin" }
                    }.trim()
                    val ts = parseTimestamp(primaryObj, occs.firstOrNull()?.dateHint)

                    val alreadyImported = currentExpenses.any {
                        kotlin.math.abs(it.timestamp - ts) <= 2000L &&
                            kotlin.math.abs(it.amount - amount) < 0.01 &&
                            it.title.equals(title, ignoreCase = true)
                    }
                    if (!alreadyImported && onImportWebExpense != null && amount > 0.0) {
                        val imported = onImportWebExpense(
                            normKey,
                            Expense(
                                title = title,
                                category = category,
                                amount = amount,
                                timestamp = ts,
                                notes = notes,
                                createdBy = createdBy
                            )
                        )
                        if (imported != null) {
                            currentExpenses.add(imported)
                            localIds.add(imported.id)
                            syncExpenseDetailed(
                                imported,
                                storeId = storeId,
                                databaseId = databaseId,
                                projectId = projectId,
                                hostingUrl = hostingUrl
                            )
                            purgeRemoteKey(normKey, occs)
                        }
                    } else if (alreadyImported) {
                        purgeRemoteKey(normKey, occs)
                    }
                }
            }

            val todayStr = dateFmt.format(Date())
            val todayTotal = currentExpenses.filter { dateFmt.format(Date(it.timestamp)) == todayStr }.sumOf { it.amount }
            val latest = currentExpenses.maxByOrNull { it.timestamp }
            syncLastExpenseStat(
                latestExpense = latest,
                todayTotal = todayTotal,
                todayDateStr = todayStr,
                databaseId = databaseId,
                projectId = projectId,
                hostingUrl = hostingUrl
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.d("FirebaseSync", "reconcileExpensesWithRtdb note: ${t.message}")
        }
    }

    /**
     * Menyinkronkan identitas profil toko ke Firebase Realtime Database & Hosting Web Chrome Relay.
     */
    suspend fun syncStoreProfile(
        profile: StoreProfile,
        storeId: String = "store_main",
        databaseId: String? = null,
        projectId: String? = null,
        hostingUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveUserId = try {
                auth?.currentUser?.uid ?: "pos_terminal_${storeId}"
            } catch (t: Throwable) {
                "pos_terminal_${storeId}"
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val payload = hashMapOf<String, Any>(
                "storeName" to (profile.storeName ?: "Kasir POS"),
                "address" to (profile.address ?: ""),
                "phone" to (profile.phone ?: ""),
                "footerMessage" to (profile.receiptFooter ?: ""),
                "updatedAt" to isoFormat.format(Date()),
                "userId" to effectiveUserId
            )

            val rtdbPayload = JSONObject(payload as Map<*, *>).toString()
            val relayOk = sendToHostingRelay(hostingUrl, "store_profile/$storeId", rtdbPayload, "PUT")
            var rtdbProfileSuccess = false
            try {
                rtdbProfileSuccess = syncToRealtimeDatabase(databaseId, "store_profile/$storeId", rtdbPayload, projectId)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w("FirebaseSync", "RTDB store profile note: ${t.message}")
            }

            Log.d("FirebaseSync", "Store profile synced ($storeId): RTDB=$rtdbProfileSuccess Relay=$relayOk")
            rtdbProfileSuccess || relayOk
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Safe catch: Failed to sync store profile", t)
            false
        }
    }

    private val rtdbClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3500L, TimeUnit.MILLISECONDS)
            .readTimeout(3500L, TimeUnit.MILLISECONDS)
            .writeTimeout(3500L, TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private fun isHtmlResponse(contentType: String, body: String): Boolean {
        if (contentType.contains("text/html", ignoreCase = true)) return true
        val trimmed = body.trimStart()
        return trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)
    }

    private fun sendToHostingRelay(
        hostingUrl: String?,
        path: String,
        jsonBody: String?,
        method: String
    ): Boolean {
        val relayUrl = buildHostingRelayUrl(hostingUrl, path) ?: return false
        return try {
            val builder = Request.Builder().url(relayUrl)
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val req = when (method.uppercase(Locale.ROOT)) {
                "DELETE" -> builder.delete().build()
                "PATCH" -> builder.patch((jsonBody ?: "{}").toRequestBody(mediaType)).build()
                "POST" -> builder.post((jsonBody ?: "{}").toRequestBody(mediaType)).build()
                else -> builder.put((jsonBody ?: "{}").toRequestBody(mediaType)).build()
            }
            rtdbClient.newCall(req).execute().use { resp ->
                val contentType = resp.header("Content-Type").orEmpty()
                val respBody = resp.body?.string().orEmpty()
                if (resp.isSuccessful && !resp.isRedirect && !isHtmlResponse(contentType, respBody)) {
                    Log.i("FirebaseSync", "Synced via Web Chrome Hosting Relay: $relayUrl")
                    true
                } else {
                    Log.d("FirebaseSync", "Hosting Relay rejected (${resp.code}, type=$contentType) for $relayUrl")
                    false
                }
            }
        } catch (t: Throwable) {
            Log.d("FirebaseSync", "Hosting Relay attempt failed for $relayUrl: ${t.message}")
            false
        }
    }

    /**
     * Menyinkronkan data langsung ke Firebase Realtime Database via REST API.
     */
    suspend fun syncToRealtimeDatabase(
        targetHostOrProject: String?,
        path: String,
        jsonBody: String,
        projectId: String? = null
    ): Boolean = sendToRealtimeDatabase(targetHostOrProject, path, jsonBody, usePatch = false, projectId = projectId)

    suspend fun patchToRealtimeDatabase(
        targetHostOrProject: String?,
        path: String,
        jsonBody: String,
        projectId: String? = null
    ): Boolean = sendToRealtimeDatabase(targetHostOrProject, path, jsonBody, usePatch = true, projectId = projectId)

    private suspend fun sendToRealtimeDatabase(
        targetHostOrProject: String?,
        path: String,
        jsonBody: String,
        usePatch: Boolean,
        projectId: String? = null
    ): Boolean = sendToRealtimeDatabaseDetailed(
        targetHostOrProject = targetHostOrProject,
        path = path,
        jsonBody = jsonBody,
        usePatch = usePatch,
        projectId = projectId
    ).ok

    private suspend fun sendToRealtimeDatabaseDetailed(
        targetHostOrProject: String?,
        path: String,
        jsonBody: String,
        usePatch: Boolean,
        projectId: String? = null
    ): RtdbWriteAttempt = withContext(Dispatchers.IO) {
        val configuredDbUrl = resolveConfiguredRtdbUrl()
        val authToken = fetchAuthTokenOrNull()
        val urls = buildRtdbUrlCandidates(
            databaseId = targetHostOrProject,
            projectId = projectId,
            path = path,
            explicitDatabaseUrl = configuredDbUrl
        )

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        var lastCode: Int? = null
        var lastError: String? = null

        for (baseUrl in urls) {
            val targetUrls = if (!authToken.isNullOrBlank()) {
                listOf(appendAuthTokenToRtdbUrl(baseUrl, authToken), baseUrl)
            } else {
                listOf(baseUrl)
            }
            for (url in targetUrls) {
                try {
                    val body = jsonBody.toRequestBody(mediaType)
                    val requestBuilder = Request.Builder().url(url)
                    val request = if (usePatch) requestBuilder.patch(body).build() else requestBuilder.put(body).build()
                    rtdbClient.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Log.i("FirebaseSync", "Synced to Realtime Database successfully: $baseUrl")
                            return@withContext RtdbWriteAttempt(
                                ok = true,
                                endpoint = baseUrl,
                                httpCode = response.code
                            )
                        }
                        lastCode = response.code
                        lastError = respBody.trim().take(120).ifEmpty { "HTTP ${response.code}" }
                        if (response.code == 401 || response.code == 403) {
                            Log.w("FirebaseSync", "⚠️ RTDB Permission Denied (${response.code}) on $baseUrl: $respBody. Pastikan Rules di Firebase Console sudah di-PUBLISH.")
                        } else {
                            Log.d("FirebaseSync", "RTDB sync failed (${response.code}) for $baseUrl: $respBody")
                        }
                    }
                } catch (t: Throwable) {
                    lastError = t.message?.take(120) ?: t.javaClass.simpleName
                    Log.d("FirebaseSync", "RTDB connection attempt failed for $baseUrl: ${t.message}")
                }
            }
        }
        RtdbWriteAttempt(ok = false, httpCode = lastCode, error = lastError)
    }

    /**
     * Menyinkronkan rekap laporan harian (DailyReport dari Room DB) ke Realtime Database & Hosting Relay.
     */
    suspend fun syncDailyReport(
        dailyReport: DailyReport,
        databaseId: String? = null,
        totalExpenses: Double = 0.0,
        expenseNotes: String = "",
        projectId: String? = null,
        hostingUrl: String? = null,
        expensesList: List<Expense> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val expensePayloadList = expensesList.map { buildExpensePayloadMap(it) }
            val resolvedTotalExp = if (totalExpenses > 0.0) totalExpenses else expensesList.sumOf { it.amount }
            val resolvedExpNotes = expenseNotes.ifBlank {
                expensesList.joinToString("; ") { exp ->
                    val notePart = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
                    "${exp.title} [${exp.category}]: Rp ${String.format(Locale("id", "ID"), "%,.0f", exp.amount)}$notePart"
                }
            }
            val payload = mapOf(
                "date" to dailyReport.date,
                "dateTimestamp" to dailyReport.dateTimestamp,
                "totalRevenue" to dailyReport.totalRevenue,
                "grossRevenue" to dailyReport.totalRevenue,
                "totalProfit" to dailyReport.totalProfit,
                "totalExpenses" to resolvedTotalExp,
                "expenseNotes" to resolvedExpNotes,
                "expensesList" to expensePayloadList,
                "totalTransactions" to dailyReport.totalTransactions,
                "totalItemsSold" to dailyReport.totalItemsSold,
                "totalCash" to dailyReport.totalCash,
                "totalNonCash" to dailyReport.totalNonCash,
                "totalQris" to dailyReport.totalQris,
                "totalTransfer" to dailyReport.totalTransfer,
                "totalDiscount" to dailyReport.totalDiscount,
                "totalTax" to dailyReport.totalTax,
                "expectedCash" to dailyReport.expectedCash,
                "actualCash" to dailyReport.actualCash,
                "cashDifference" to dailyReport.cashDifference,
                "topProductSummary" to dailyReport.topProductSummary,
                "cashierNames" to dailyReport.cashierNames,
                "shiftCount" to dailyReport.shiftCount,
                "notes" to dailyReport.notes,
                "archivedByTutupToko" to true,
                "lastUpdated" to isoFormat.format(Date()),
                "updatedAt" to System.currentTimeMillis()
            )
            val jsonStr = JSONObject(payload).toString()
            val relay1 = sendToHostingRelay(hostingUrl, "daily_reports/${dailyReport.date}", jsonStr, "PATCH")
            val relay2 = sendToHostingRelay(hostingUrl, "reports/daily/${dailyReport.date}", jsonStr, "PATCH")
            val rtdb1 = patchToRealtimeDatabase(databaseId, "daily_reports/${dailyReport.date}", jsonStr, projectId)
            val rtdb2 = patchToRealtimeDatabase(databaseId, "reports/daily/${dailyReport.date}", jsonStr, projectId)
            relay1 || relay2 || rtdb1 || rtdb2
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Failed to sync daily report to RTDB: ${t.message}", t)
            false
        }
    }
}


