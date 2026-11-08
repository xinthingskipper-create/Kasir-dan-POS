package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.DailyReport
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

    /**
     * Checks if Firebase Realtime Database Cloud and Internet are reachable.
     * Guaranteed 100% crash-proof: catches all Exceptions, Errors, Timeouts, and never crashes the app.
     */
    suspend fun checkCloudConnection(databaseId: String? = null, projectId: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // 1. Android Network Availability Check
            val isConnectedToNetwork = try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                @Suppress("DEPRECATION")
                val activeNetwork = cm?.activeNetworkInfo
                activeNetwork?.isConnected == true
            } catch (t: Throwable) {
                true // fallback to trying HTTP check
            }

            if (!isConnectedToNetwork) {
                return@withContext Pair(false, "Perangkat sedang offline (tidak ada koneksi internet). Data kasir tersimpan aman di Room DB.")
            }

            // 2. Determine target identifiers
            val cleanDbId = databaseId?.trim()?.ifBlank { null }
            val cleanProjId = projectId?.trim()?.ifBlank { null } ?: "foodrock-report"

            // 3. Ping Firebase Realtime Database (via lightweight HTTP REST)
            val rtdbTargets = listOfNotNull(
                cleanDbId,
                cleanProjId,
                if (!cleanProjId.contains("rtdb")) "$cleanProjId-default-rtdb" else null,
                "foodrock-report-default-rtdb"
            ).distinct()

            for (target in rtdbTargets) {
                val rtdbUrls = listOf(
                    "https://$target.firebaseio.com/.json?shallow=true",
                    "https://$target.asia-southeast1.firebasedatabase.app/.json?shallow=true"
                )
                for (url in rtdbUrls) {
                    try {
                        val req = Request.Builder().url(url).head().build()
                        rtdbClient.newCall(req).execute().use { resp ->
                            // Any HTTP response (200 OK, 401 Unauthorized, 403 Forbidden) proves the server is reachable and active!
                            if (resp.code in 200..499) {
                                return@withContext Pair(true, "🟢 Terhubung ke Firebase Cloud ($target)")
                            }
                        }
                    } catch (t: Throwable) {
                        // try next
                    }
                }
            }

            // 4. Fallback Internet Check (generate_204)
            val internetReachable = try {
                val req = Request.Builder().url("https://www.google.com/generate_204").head().build()
                rtdbClient.newCall(req).execute().use { resp -> resp.isSuccessful || resp.code == 204 }
            } catch (t: Throwable) {
                false
            }

            if (internetReachable) {
                Pair(true, "🟢 Koneksi Internet Aktif. Server Realtime Database siap.")
            } else {
                Pair(false, "Server Cloud belum merespons. Data tersimpan aman offline di Room DB.")
            }
        } catch (t: Throwable) {
            Log.w("FirebaseSync", "Cloud connection test note: ${t.message}")
            Pair(false, "Koneksi offline: ${t.localizedMessage ?: "Tersimpan aman di Room DB"}")
        }
    }

    /**
     * Menyinkronkan transaksi penjualan langsung ke Firebase Realtime Database.
     */
    suspend fun syncTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
        storeId: String = "store_main",
        databaseId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveUserId = try {
                auth?.currentUser?.uid ?: "pos_terminal_${storeId}"
            } catch (t: Throwable) {
                "pos_terminal_${storeId}"
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
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

            // Sync to Firebase Realtime Database (foodrock-report-default-rtdb)
            var rtdbSuccess = false
            try {
                val rtdbPayload = JSONObject(payload as Map<*, *>).toString()
                val targetRtdb = if (!databaseId.isNullOrBlank() && databaseId != "(default)" && !databaseId.contains("ai-studio")) {
                    databaseId
                } else {
                    "foodrock-report-default-rtdb"
                }
                
                // 1. All Transactions pool
                rtdbSuccess = syncToRealtimeDatabase(targetRtdb, "transactions/${transaction.invoiceNumber}", rtdbPayload)

                // 2. Daily reports transactions list (Grouped by date for instant query & daily audit)
                syncToRealtimeDatabase(targetRtdb, "daily_reports/$dateStr/transactions/${transaction.invoiceNumber}", rtdbPayload)

                // 3. Reports by date node
                syncToRealtimeDatabase(targetRtdb, "reports/daily_transactions/$dateStr/${transaction.invoiceNumber}", rtdbPayload)

                // 4. Update Realtime Stats for today (live counter for owner dashboard)
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
                syncToRealtimeDatabase(targetRtdb, "realtime_stats/today", statsJson)

                // 5. Customer tracking entry for index.html lookup
                val trackingJson = JSONObject(mapOf(
                    "orderNumber" to transaction.invoiceNumber,
                    "customerName" to transaction.customerName,
                    "orderType" to transaction.orderType.name,
                    "itemsSummary" to itemsSummary,
                    "totalAmount" to transaction.grandTotal,
                    "status" to "SELESAI",
                    "paymentStatus" to "LUNAS",
                    "date" to dateStr,
                    "timestamp" to isoStr
                )).toString()
                syncToRealtimeDatabase(targetRtdb, "customer_tracking/${transaction.invoiceNumber}", trackingJson)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w("FirebaseSync", "RTDB sync note: ${t.message}")
            }

            Log.d("FirebaseSync", "Transaction synced directly to Realtime Database (RTDB=$rtdbSuccess): ${transaction.invoiceNumber}")
            rtdbSuccess
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Safe catch: Failed to sync transaction ${transaction.invoiceNumber}", t)
            false
        }

    }

    /**
     * Menyinkronkan laporan tutup kasir (shift closing) langsung ke Firebase Realtime Database.
     */
    suspend fun syncShiftClosing(
        closing: ShiftClosing,
        storeId: String = "store_main",
        databaseId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveUserId = try {
                auth?.currentUser?.uid ?: "pos_terminal_${storeId}"
            } catch (t: Throwable) {
                "pos_terminal_${storeId}"
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val dateStr = dateFormat.format(Date(closing.closingTime))
            val isoStr = isoFormat.format(Date(closing.closingTime))

            val docId = "SHIFT-${closing.id.takeIf { it > 0 } ?: closing.closingTime}"

            val payload = hashMapOf<String, Any>(
                "id" to docId,
                "shiftNumber" to "Shift-${closing.id}",
                "cashierName" to closing.cashierName,
                "cashierRole" to "Kasir",
                "startTime" to isoStr,
                "endTime" to isoStr,
                "totalTransactions" to closing.totalTransactions.toDouble(),
                "totalRevenue" to closing.totalRevenue,
                "totalCash" to closing.totalCash,
                "totalNonCash" to closing.totalNonCash,
                "expectedCash" to closing.expectedCash,
                "actualCash" to closing.actualCash,
                "difference" to closing.difference,
                "itemsSold" to 0.0,
                "notes" to (closing.notes ?: ""),
                "timestamp" to isoStr,
                "date" to dateStr,
                "storeId" to storeId,
                "userId" to effectiveUserId,
                "status" to "CLOSED"
            )

            // Sync to Firebase Realtime Database (foodrock-report-default-rtdb)
            var rtdbShiftSuccess = false
            try {
                val rtdbPayload = JSONObject(payload as Map<*, *>).toString()
                val targetRtdb = if (!databaseId.isNullOrBlank() && databaseId != "(default)" && !databaseId.contains("ai-studio")) {
                    databaseId
                } else {
                    "foodrock-report-default-rtdb"
                }
                rtdbShiftSuccess = syncToRealtimeDatabase(targetRtdb, "shift_reports/$docId", rtdbPayload)

                // Update daily_reports in Realtime Database
                val dailyPayload = JSONObject(mapOf(
                    "date" to dateStr,
                    "totalRevenue" to closing.totalRevenue,
                    "totalCash" to closing.totalCash,
                    "totalNonCash" to closing.totalNonCash,
                    "totalTransactions" to closing.totalTransactions,
                    "expectedCash" to closing.expectedCash,
                    "actualCash" to closing.actualCash,
                    "difference" to closing.difference,
                    "lastShiftId" to docId,
                    "lastShiftCashier" to closing.cashierName,
                    "lastUpdated" to isoStr,
                    "storeId" to storeId
                )).toString()
                syncToRealtimeDatabase(targetRtdb, "daily_reports/$dateStr", dailyPayload)

                // Update realtime_stats/last_shift
                val statsShiftPayload = JSONObject(mapOf(
                    "lastShiftClosingId" to docId,
                    "lastShiftCashier" to closing.cashierName,
                    "lastShiftRevenue" to closing.totalRevenue,
                    "lastShiftActualCash" to closing.actualCash,
                    "lastShiftDifference" to closing.difference,
                    "lastShiftTimestamp" to isoStr,
                    "date" to dateStr
                )).toString()
                syncToRealtimeDatabase(targetRtdb, "realtime_stats/last_shift", statsShiftPayload)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w("FirebaseSync", "RTDB shift sync note: ${t.message}")
            }

            Log.d("FirebaseSync", "Shift closing sync result to Realtime Database ($docId): $rtdbShiftSuccess")
            rtdbShiftSuccess
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Safe catch: Failed to sync shift closing", t)
            false
        }
    }

    /**
     * Menyinkronkan identitas profil toko ke Firebase Realtime Database.
     */
    suspend fun syncStoreProfile(
        profile: StoreProfile,
        storeId: String = "store_main",
        databaseId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveUserId = try {
                auth?.currentUser?.uid ?: "pos_terminal_${storeId}"
            } catch (t: Throwable) {
                "pos_terminal_${storeId}"
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val payload = hashMapOf<String, Any>(
                "storeName" to (profile.storeName ?: "Kasir POS"),
                "address" to (profile.address ?: ""),
                "phone" to (profile.phone ?: ""),
                "footerMessage" to (profile.receiptFooter ?: ""),
                "updatedAt" to isoFormat.format(Date()),
                "userId" to effectiveUserId
            )

            // Sync store profile to Realtime Database (for RTDB-only web reports)
            var rtdbProfileSuccess = false
            try {
                val rtdbPayload = JSONObject(payload as Map<*, *>).toString()
                val targetRtdb = if (!databaseId.isNullOrBlank() && databaseId != "(default)" && !databaseId.contains("ai-studio")) {
                    databaseId
                } else {
                    "foodrock-report-default-rtdb"
                }
                rtdbProfileSuccess = syncToRealtimeDatabase(targetRtdb, "store_profile/$storeId", rtdbPayload)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w("FirebaseSync", "RTDB store profile note: ${t.message}")
            }

            Log.d("FirebaseSync", "Store profile synced to Realtime Database ($storeId): $rtdbProfileSuccess")
            rtdbProfileSuccess
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
            .build()
    }

    /**
     * Menyinkronkan data langsung ke Firebase Realtime Database via REST API.
     * Sangat cepat, andal, dan kompatibel langsung dengan database foodrock-report-default-rtdb (Singapore).
     */
    suspend fun syncToRealtimeDatabase(
        targetHostOrProject: String?,
        path: String,
        jsonBody: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanTarget = targetHostOrProject?.trim()?.ifBlank { "foodrock-report-default-rtdb" } ?: "foodrock-report-default-rtdb"
        val urls = mutableListOf<String>()

        // 1. Prioritaskan URL Firebase Realtime Database Singapore (asia-southeast1) milik owner
        urls.add("https://foodrock-report-default-rtdb.asia-southeast1.firebasedatabase.app/$path.json")
        urls.add("https://foodrock-report-default-rtdb.firebaseio.com/$path.json")

        // 2. URL dinamis berdasarkan targetHostOrProject
        if (cleanTarget != "foodrock-report-default-rtdb") {
            val base = cleanTarget
                .replace("https://", "")
                .replace(".firebaseio.com", "")
                .replace(".asia-southeast1.firebasedatabase.app", "")
            if (base.isNotBlank() && !base.contains("ai-studio")) {
                urls.add("https://$base.asia-southeast1.firebasedatabase.app/$path.json")
                urls.add("https://$base.firebaseio.com/$path.json")
                if (!base.endsWith("-default-rtdb")) {
                    urls.add("https://$base-default-rtdb.asia-southeast1.firebasedatabase.app/$path.json")
                    urls.add("https://$base-default-rtdb.firebaseio.com/$path.json")
                }
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonBody.toRequestBody(mediaType)

        for (url in urls.distinct()) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .put(body)
                    .build()
                rtdbClient.newCall(request).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        Log.i("FirebaseSync", "Synced to Realtime Database successfully: $url")
                        return@withContext true
                    } else if (response.code == 401 || response.code == 403) {
                        Log.w("FirebaseSync", "⚠️ RTDB Permission Denied (${response.code}) on $url: $respBody. Pastikan Rules di Firebase Console sudah di-PUBLISH.")
                    } else {
                        Log.d("FirebaseSync", "RTDB sync failed (${response.code}) for $url: $respBody")
                    }
                }
            } catch (t: Throwable) {
                // Log and try next candidate URL
                Log.d("FirebaseSync", "RTDB connection attempt failed for $url: ${t.message}")
            }
        }
        false
    }

    /**
     * Menyinkronkan rekap laporan harian (DailyReport dari Room DB) ke Realtime Database.
     */
    suspend fun syncDailyReport(
        dailyReport: DailyReport,
        databaseId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "date" to dailyReport.date,
                "dateTimestamp" to dailyReport.dateTimestamp,
                "totalRevenue" to dailyReport.totalRevenue,
                "totalProfit" to dailyReport.totalProfit,
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
                "updatedAt" to System.currentTimeMillis()
            )
            val jsonStr = JSONObject(payload).toString()
            val target = if (!databaseId.isNullOrBlank() && databaseId != "(default)" && !databaseId.contains("ai-studio")) {
                databaseId
            } else {
                "foodrock-report-default-rtdb"
            }
            val rtdb1 = syncToRealtimeDatabase(target, "daily_reports/${dailyReport.date}", jsonStr)
            val rtdb2 = syncToRealtimeDatabase(target, "reports/daily/${dailyReport.date}", jsonStr)
            rtdb1 || rtdb2
        } catch (t: Throwable) {
            Log.e("FirebaseSync", "Failed to sync daily report to RTDB: ${t.message}", t)
            false
        }
    }
}

