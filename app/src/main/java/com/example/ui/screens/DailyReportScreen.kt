package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import com.google.firebase.auth.FirebaseUser
import com.example.data.model.UserRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.local.TopProductSummary
import com.example.data.model.DailyReport
import com.example.data.model.ShiftClosing
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.ui.DateFilter
import com.example.ui.PosViewModel
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyReportScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val selectedFilter by viewModel.selectedDateFilter.collectAsState()
    val transactionsFlow by viewModel.reportTransactions.collectAsState()
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    val topProductsFlow by viewModel.topSellingProducts.collectAsState()
    val topProducts by topProductsFlow.collectAsState(initial = emptyList())

    val paymentStatsFlow by viewModel.paymentMethodStats.collectAsState()
    val paymentStats by paymentStatsFlow.collectAsState(initial = emptyList())

    val profitFlow by viewModel.totalProfitFlow.collectAsState()
    val totalProfit by profitFlow.collectAsState(initial = 0.0)

    val shiftClosings by viewModel.allShiftClosings.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()

    val cloudUser by viewModel.cloudUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val lastSyncStatus by viewModel.lastSyncStatus.collectAsState()
    val webDashboardUrl by viewModel.webDashboardUrl.collectAsState()
    val firebaseProjectId by viewModel.firebaseProjectId.collectAsState()
    val firebaseDatabaseId by viewModel.firebaseDatabaseId.collectAsState()

    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val isAutoSyncActive by viewModel.isAutoSyncActive.collectAsState()
    var syncStatusFilter by remember { mutableStateOf("ALL") }

    val allDailyReports by viewModel.allDailyReports.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    var selectedDailyReportForDetail by remember { mutableStateOf<DailyReport?>(null) }
    var searchReportDate by remember { mutableStateOf("") }

    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showWebHostingDialog by remember { mutableStateOf(false) }

    // Filter transactions based on sync status filter
    val filteredTransactions = remember(transactions, syncStatusFilter) {
        when (syncStatusFilter) {
            "SYNCED" -> transactions.filter { it.isSynced }
            "UNSYNCED" -> transactions.filter { !it.isSynced }
            else -> transactions
        }
    }

    // Compute live metrics
    val totalRevenue = transactions.sumOf { it.grandTotal }
    val totalTrxCount = transactions.size
    val averageTicket = if (totalTrxCount > 0) totalRevenue / totalTrxCount else 0.0
    val netProfit = totalProfit ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Web Laporan Owner (Firebase Cloud)
        item {
            val isCashier = currentUser?.role != UserRole.ADMIN
            val separator = if (webDashboardUrl.contains("?")) "&" else "?"
            val reportWebUrl = "$webDashboardUrl${separator}projectId=$firebaseProjectId&databaseId=$firebaseDatabaseId" +
                    (if (isCashier) "&role=cashier" else "")

            OwnerWebReportCard(
                webUrl = reportWebUrl,
                projectId = firebaseProjectId,
                databaseId = firebaseDatabaseId,
                cloudUser = cloudUser,
                isSyncing = isCloudSyncing,
                syncStatus = lastSyncStatus,
                canSignOut = !isCashier,
                onLoginGoogle = {
                    viewModel.signInGoogleCloud(
                        context = context,
                        onSuccess = { Toast.makeText(context, "Berhasil masuk ke Firebase!", Toast.LENGTH_SHORT).show() },
                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                    )
                },
                onSignOut = {
                    viewModel.signOutGoogleCloud()
                    Toast.makeText(context, "Keluar dari Firebase", Toast.LENGTH_SHORT).show()
                },
                onSyncNow = {
                    viewModel.syncAllToCloud { success, msg ->
                        Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                    }
                },
                onOpenWeb = {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reportWebUrl))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Tidak dapat membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onShareWeb = {
                    val shareText = "🌐 *Web Dashboard Laporan Owner - ${storeProfile.storeName}*\n\n" +
                            "Owner dapat memantau omset, pesanan, dan laporan tutup kasir secara langsung melalui link berikut:\n" +
                            "$reportWebUrl\n\n" +
                            "• Project: $firebaseProjectId\n" +
                            "• Database: $firebaseDatabaseId\n\n" +
                            "_Buka link di browser HP, Tablet, atau Laptop kapan saja._"
                    try {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Link Web Laporan Owner"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal membagikan link", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenHostingSettings = {
                    viewModel.selectTab(com.example.ui.MainTab.CLOUD_HOSTING)
                }
            )
        }

        // Date Range Filter Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateFilter.values().forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // WhatsApp Share Button to Owner (Prominent Callout)
        item {
            Button(
                onClick = { showWhatsAppDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("send_whatsapp_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kirim Laporan ke Owner via WhatsApp",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Real-Time Indicator Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Laporan Real-Time sinkron langsung dengan transaksi kasir",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF166534)
                )
            }
        }

        // Card: Pelacakan Database & Sinkronisasi Otomatis
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_database_auto_sync_tracker"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDBEAFE),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Penyimpanan Database & Sinkron Otomatis",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Room DB (Lokal) & Firebase Realtime Database (Cloud Live)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAutoSyncActive) "Auto-Sync ON" else "Auto-Sync OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoSyncActive) Color(0xFF059669) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isAutoSyncActive,
                                onCheckedChange = { viewModel.toggleAutoSync(it) },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("switch_auto_sync")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Counter stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "💾 Room DB Lokal", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(text = "${transactions.size} Transaksi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "🟢 Tersinkron Cloud", fontSize = 10.sp, color = Color(0xFF047857))
                                Text(text = "$syncedCount Transaksi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (unsyncedCount > 0) Color(0xFFFFFBEB) else Color.White,
                            border = BorderStroke(1.dp, if (unsyncedCount > 0) Color(0xFFFDE68A) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "🟠 Antrean Tertunda", fontSize = 10.sp, color = if (unsyncedCount > 0) Color(0xFFB45309) else Color(0xFF64748B))
                                Text(text = "$unsyncedCount Transaksi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (unsyncedCount > 0) Color(0xFFB45309) else Color(0xFF0F172A))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.testCloudConnection { ok, msg ->
                                    try {
                                        Toast.makeText(context, msg, if (ok) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Log.e("DailyReportScreen", "Toast error", e)
                                    }
                                }
                            },
                            enabled = !isCloudSyncing,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_test_cloud_connection"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            if (isCloudSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Memeriksa...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cek Koneksi Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.syncPendingTransactions { success, msg ->
                                    try {
                                        Toast.makeText(context, msg ?: "Sinkron selesai", if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Log.e("DailyReportScreen", "Toast error", e)
                                    }
                                }
                            },
                            enabled = !isCloudSyncing,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(38.dp)
                                .testTag("btn_sync_pending_now"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (unsyncedCount > 0) Color(0xFFD97706) else Color(0xFF0F172A))
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCloudSyncing) "Menyinkronkan..." else if (unsyncedCount > 0) "Sinkronkan ($unsyncedCount)" else "Sinkronkan Semua",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 💾 DATABASE ROOM LAPORAN (ARSIP LOKAL SQLITE - TABEL daily_reports)
        item {
            RoomDailyReportArchiveCard(
                reports = allDailyReports,
                isGenerating = isGeneratingReport,
                searchQuery = searchReportDate,
                onSearchChange = { searchReportDate = it },
                onGenerateToday = {
                    viewModel.generateOrRefreshTodayReport { success, msg ->
                        Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                    }
                },
                onViewDetail = { report ->
                    selectedDailyReportForDetail = report
                },
                onShareWhatsApp = { report ->
                    val text = BluetoothPrinterManager.generateDailyReportPlainText(storeProfile, report)
                    try {
                        val uri = Uri.parse("https://api.whatsapp.com/send?text=" + URLEncoder.encode(text, "UTF-8"))
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = { report ->
                    viewModel.deleteDailyReport(report) { success ->
                        if (success) Toast.makeText(context, "Laporan ${report.date} dihapus dari Room", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 4 KPI Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        title = "Total Omset (Penjualan)",
                        value = BluetoothPrinterManager.formatRupiah(totalRevenue),
                        icon = Icons.Default.Payments,
                        iconColor = Color(0xFF1E40AF),
                        containerColor = Color(0xFFDBEAFE),
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        title = "Keuntungan Bersih",
                        value = BluetoothPrinterManager.formatRupiah(netProfit),
                        icon = Icons.Default.TrendingUp,
                        iconColor = Color(0xFF15803D),
                        containerColor = Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        title = "Total Transaksi",
                        value = "$totalTrxCount struk",
                        icon = Icons.Default.Receipt,
                        iconColor = Color(0xFFB45309),
                        containerColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        title = "Rata-rata / Struk",
                        value = BluetoothPrinterManager.formatRupiah(averageTicket),
                        icon = Icons.Default.ShoppingBag,
                        iconColor = Color(0xFF0F766E),
                        containerColor = Color(0xFFCCFBF1),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Payment Method Breakdown
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (paymentStats.isEmpty()) {
                        Text(
                            text = "Belum ada transaksi di periode ini",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        paymentStats.forEach { stat ->
                            val percent = if (totalRevenue > 0) (stat.totalAmount / totalRevenue).toFloat() else 0f
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${stat.paymentMethod} (${stat.count}x)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = BluetoothPrinterManager.formatRupiah(stat.totalAmount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Selling Products
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Produk Terlaris",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (topProducts.isEmpty()) {
                        Text(
                            text = "Belum ada produk terjual",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        val maxSold = topProducts.firstOrNull()?.totalSold ?: 1
                        topProducts.forEachIndexed { index, prod ->
                            TopProductItemRow(rank = index + 1, item = prod, maxSold = maxSold)
                        }
                    }
                }
            }
        }

        // Shift Closings History (Riwayat Tutup Kasir)
        if (shiftClosings.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.LockClock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Riwayat Tutup Kasir / Shift",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Online Real-Time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Banner Akses Web Realtime DB Online
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Portal Realtime DB Online",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                    Text(
                                        text = "foodrock-report-default-rtdb",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Laporan tutup kasir & selisih uang kas ter-update instan di web tanpa perlu refresh browser.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Primary Action: Buka Web Tutup Toko (RTDB Only)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val baseUrl = webDashboardUrl.removeSuffix("/").removeSuffix("/index.html").removeSuffix("/owner.html").removeSuffix("/rtdb-laporan.html").removeSuffix("/tutup-toko.html")
                                            val tutupTokoUrl = "$baseUrl/tutup-toko.html?projectId=$firebaseProjectId&databaseId=$firebaseDatabaseId"
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tutupTokoUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                clipboardManager.setText(AnnotatedString(tutupTokoUrl))
                                                Toast.makeText(context, "URL disalin: $tutupTokoUrl", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_open_tutup_toko_web"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 7.dp, horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Web Tutup Toko (RTDB)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val baseUrl = webDashboardUrl.removeSuffix("/").removeSuffix("/index.html").removeSuffix("/owner.html").removeSuffix("/rtdb-laporan.html").removeSuffix("/tutup-toko.html")
                                            val tutupTokoUrl = "$baseUrl/tutup-toko.html?projectId=$firebaseProjectId&databaseId=$firebaseDatabaseId"
                                            clipboardManager.setText(AnnotatedString(tutupTokoUrl))
                                            Toast.makeText(context, "Link Web Tutup Toko Disalin!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("btn_copy_tutup_toko_link"),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        contentPadding = PaddingValues(vertical = 7.dp, horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Salin Link", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Secondary Action: Web RTDB Lengkap
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val baseUrl = webDashboardUrl.removeSuffix("/").removeSuffix("/index.html").removeSuffix("/owner.html").removeSuffix("/rtdb-laporan.html").removeSuffix("/tutup-toko.html")
                                            val rtdbWebUrl = "$baseUrl/rtdb-laporan.html?projectId=$firebaseProjectId&databaseId=$firebaseDatabaseId"
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rtdbWebUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                clipboardManager.setText(AnnotatedString(rtdbWebUrl))
                                                Toast.makeText(context, "URL disalin: $rtdbWebUrl", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_open_rtdb_web"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Portal RTDB Lengkap", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        shiftClosings.take(5).forEach { closing ->
                            ShiftClosingRowItem(closing = closing)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // Transaction History Header with Sync Filters
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pelacakan Transaksi (${filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Klik baris untuk lihat struk",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sync status filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = syncStatusFilter == "ALL",
                        onClick = { syncStatusFilter = "ALL" },
                        label = { Text("Semua (${transactions.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = syncStatusFilter == "SYNCED",
                        onClick = { syncStatusFilter = "SYNCED" },
                        label = { Text("🟢 Tersinkron (${transactions.count { it.isSynced }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = syncStatusFilter == "UNSYNCED",
                        onClick = { syncStatusFilter = "UNSYNCED" },
                        label = { Text("🟠 Tertunda (${transactions.count { !it.isSynced }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Transaction History List
        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (syncStatusFilter == "UNSYNCED") {
                            "Semua transaksi telah tersinkronkan ke database!"
                        } else {
                            "Tidak ada transaksi pada filter yang dipilih"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { trx ->
                TransactionRowCard(
                    transaction = trx,
                    onClick = { viewModel.showReceiptFor(trx) },
                    onSyncClick = {
                        viewModel.syncSingleTransaction(trx) { success, msg ->
                            try {
                                Toast.makeText(context, msg ?: "Status tersimpan", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("DailyReportScreen", "Toast error", e)
                            }
                        }
                    }
                )
            }
        }
    }

    // Modal Kirim Laporan ke WhatsApp Owner
    if (showWhatsAppDialog) {
        val reportMessage = viewModel.generateWhatsAppReportText(
            transactions = transactions,
            topProducts = topProducts,
            paymentSummaries = paymentStats,
            totalProfit = netProfit
        )

        WhatsAppSendDialog(
            initialPhone = storeProfile.ownerPhone,
            reportText = reportMessage,
            onDismiss = { showWhatsAppDialog = false }
        )
    }

    // Modal Pengaturan Hosting Web Owner
    if (showWebHostingDialog) {
        WebHostingConfigDialog(
            viewModel = viewModel,
            onDismiss = { showWebHostingDialog = false }
        )
    }

    // Modal Detail Database Room Laporan (Arsip SQLite)
    if (selectedDailyReportForDetail != null) {
        DailyReportDetailDialog(
            report = selectedDailyReportForDetail!!,
            storeProfile = storeProfile,
            onDismiss = { selectedDailyReportForDetail = null },
            onPrint = {
                viewModel.printDailyReport(selectedDailyReportForDetail!!)
                Toast.makeText(context, "Mengirim ke printer...", Toast.LENGTH_SHORT).show()
            },
            onShareWhatsApp = {
                val text = BluetoothPrinterManager.generateDailyReportPlainText(storeProfile, selectedDailyReportForDetail!!)
                try {
                    val uri = Uri.parse("https://api.whatsapp.com/send?text=" + URLEncoder.encode(text, "UTF-8"))
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    Toast.makeText(context, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
}

@Composable
fun WebHostingConfigDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentFirebaseProjectId by viewModel.firebaseProjectId.collectAsState()
    val currentFirebaseDatabaseId by viewModel.firebaseDatabaseId.collectAsState()
    val currentWebDashboardUrl by viewModel.webDashboardUrl.collectAsState()

    var inputProjectId by remember { mutableStateOf(currentFirebaseProjectId) }
    var inputDatabaseId by remember { mutableStateOf(currentFirebaseDatabaseId) }
    var inputWebUrl by remember { mutableStateOf(currentWebDashboardUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pengaturan Hosting Web Owner", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Konfigurasikan Firebase Project ID, Database ID, dan URL hosting agar dashboard laporan owner terhubung secara real-time:",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                // 1. Firebase Project ID
                OutlinedTextField(
                    value = inputProjectId,
                    onValueChange = {
                        inputProjectId = it
                        if (inputWebUrl.startsWith("https://") && inputWebUrl.endsWith(".web.app")) {
                            inputWebUrl = "https://${it.trim()}.web.app"
                        }
                    },
                    label = { Text("Firebase Project ID Laporan Owner", fontSize = 11.sp) },
                    placeholder = { Text("Contoh: gen-lang-client-0076807780", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // 2. Real-time Database ID
                OutlinedTextField(
                    value = inputDatabaseId,
                    onValueChange = { inputDatabaseId = it },
                    label = { Text("Real-time Database Instance ID", fontSize = 11.sp) },
                    placeholder = { Text("Contoh: foodrock-report-default-rtdb", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Storage, null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // 3. URL Hosting Web Laporan
                OutlinedTextField(
                    value = inputWebUrl,
                    onValueChange = { inputWebUrl = it },
                    label = { Text("URL Hosting Web Laporan", fontSize = 11.sp) },
                    placeholder = { Text("https://<project-id>.web.app", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Language, null, modifier = Modifier.size(16.dp), tint = Color(0xFF7C3AED)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick buttons
                Text(text = "Template URL Cepat:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.clickable {
                            val pid = inputProjectId.trim().ifBlank { viewModel.defaultFirebaseProjectId }
                            inputWebUrl = "https://$pid.web.app"
                        }
                    ) {
                        Text(".web.app", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.clickable {
                            val pid = inputProjectId.trim().ifBlank { viewModel.defaultFirebaseProjectId }
                            inputWebUrl = "https://$pid.firebaseapp.com"
                        }
                    ) {
                        Text(".firebaseapp.com", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            inputWebUrl = viewModel.defaultHostingUrl
                        }
                    ) {
                        Text("Server AI Studio", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateWebHostingConfig(inputProjectId, inputDatabaseId, inputWebUrl)
                    Toast.makeText(context, "Pengaturan hosting berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun WhatsAppSendDialog(
    initialPhone: String,
    reportText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var ownerPhone by remember { mutableStateOf(initialPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kirim ke WhatsApp Owner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input WhatsApp Number
                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = { ownerPhone = it },
                    label = { Text("Nomor WhatsApp Owner") },
                    placeholder = { Text("081234567890") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_owner_phone_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message Preview Box
                Text(
                    text = "Pratinjau Pesan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reportText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Copy & Send to WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(reportText))
                            Toast.makeText(context, "Teks laporan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin")
                    }

                    Button(
                        onClick = {
                            val cleanNumber = ownerPhone.trim()
                                .replace("-", "")
                                .replace(" ", "")
                            val formattedNumber = if (cleanNumber.startsWith("0")) {
                                "62" + cleanNumber.substring(1)
                            } else if (cleanNumber.startsWith("+")) {
                                cleanNumber.substring(1)
                            } else {
                                cleanNumber
                            }

                            val encodedText = try {
                                URLEncoder.encode(reportText, "UTF-8")
                            } catch (e: Exception) {
                                reportText
                            }

                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedText")
                            val intent = Intent(Intent.ACTION_VIEW, uri)

                            try {
                                context.startActivity(intent)
                                onDismiss()
                            } catch (e: Exception) {
                                // Fallback to generic share chooser
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan Kasir"))
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
                        ),
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("send_whatsapp_action_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim WhatsApp", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftClosingRowItem(closing: ShiftClosing) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(closing.closingTime))
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${closing.cashierName} • $dateStr",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = BluetoothPrinterManager.formatRupiah(closing.totalRevenue),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${closing.totalTransactions} Transaksi • Kas Laci: ${BluetoothPrinterManager.formatRupiah(closing.actualCash)}",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            val isBalanced = closing.difference == 0.0
            Text(
                text = if (isBalanced) "Kas Pas (Rp 0)" else "Selisih: ${BluetoothPrinterManager.formatRupiah(closing.difference)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isBalanced) Color(0xFF166534) else Color(0xFFDC2626)
            )
        }
        if (closing.notes.isNotBlank()) {
            Text(
                text = "Catatan: ${closing.notes}",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable {
                    val summary = "Shift #${closing.id} • Kasir: ${closing.cashierName} • Omset: ${BluetoothPrinterManager.formatRupiah(closing.totalRevenue)} • Kas Laci: ${BluetoothPrinterManager.formatRupiah(closing.actualCash)} • Selisih: ${BluetoothPrinterManager.formatRupiah(closing.difference)}"
                    clipboardManager.setText(AnnotatedString(summary))
                    Toast.makeText(context, "Ringkasan shift disalin!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Salin Info", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFDCFCE7),
                modifier = Modifier.clickable {
                    val diffText = if (closing.difference == 0.0) "Pas (Rp 0)" else "Selisih ${BluetoothPrinterManager.formatRupiah(closing.difference)}"
                    val msg = "*🔒 LAPORAN TUTUP KASIR (SHIFT)*\n" +
                            "📋 Shift #${closing.id} (${dateStr})\n" +
                            "👤 Kasir: ${closing.cashierName}\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "💰 Total Omset: ${BluetoothPrinterManager.formatRupiah(closing.totalRevenue)}\n" +
                            "💵 Kas Tunai: ${BluetoothPrinterManager.formatRupiah(closing.totalCash)}\n" +
                            "💳 Non-Tunai: ${BluetoothPrinterManager.formatRupiah(closing.totalNonCash)}\n" +
                            "🧾 Total Struk: ${closing.totalTransactions}\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "💼 Kas Sistem: ${BluetoothPrinterManager.formatRupiah(closing.expectedCash)}\n" +
                            "💵 Kas Fisik: ${BluetoothPrinterManager.formatRupiah(closing.actualCash)}\n" +
                            "⚖️ Status: $diffText\n" +
                            "📝 Catatan: ${closing.notes.ifBlank { "-" }}\n\n" +
                            "_Laporan otomatis terunggah ke Cloud Database._"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Kirim WA", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TopProductItemRow(
    rank: Int,
    item: TopProductSummary,
    maxSold: Int
) {
    val progress = (item.totalSold.toFloat() / maxSold).coerceIn(0f, 1f)

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = if (rank <= 3) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F5F9),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$rank",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) MaterialTheme.colorScheme.primary else Color(0xFF475569)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.productName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.totalSold} terjual",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = BluetoothPrinterManager.formatRupiah(item.totalRevenue),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}

@Composable
private fun TransactionRowCard(
    transaction: Transaction,
    onClick: () -> Unit,
    onSyncClick: (() -> Unit)? = null
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        .format(Date(transaction.timestamp))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (transaction.isSynced) Color(0xFFE2E8F0) else Color(0xFFFDE68A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_row_${transaction.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${transaction.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.paymentMethod.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dateStr • Kasir: ${transaction.cashierName}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    if (transaction.customerName.isNotBlank() && transaction.customerName != "Pelanggan Umum") {
                        Text(
                            text = "Pelanggan: ${transaction.customerName}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = BluetoothPrinterManager.formatRupiah(transaction.grandTotal),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (transaction.profitAmount > 0) {
                        Text(
                            text = "Laba: +${BluetoothPrinterManager.formatRupiah(transaction.profitAmount)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sync Status & Quick Sync Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (transaction.isSynced) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tersinkron Cloud",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Menunggu Sinkron",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                if (!transaction.isSynced && onSyncClick != null) {
                    OutlinedButton(
                        onClick = onSyncClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Sinkronkan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerWebReportCard(
    webUrl: String,
    projectId: String,
    databaseId: String,
    cloudUser: FirebaseUser?,
    isSyncing: Boolean,
    syncStatus: String?,
    canSignOut: Boolean = true,
    onLoginGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenWeb: () -> Unit,
    onShareWeb: () -> Unit,
    onOpenHostingSettings: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("owner_web_report_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Cloud Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Web Laporan Owner (Real-Time)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pantau penjualan via browser HP / Laptop",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cloud Connection Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (cloudUser != null) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (cloudUser != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (cloudUser != null) Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (cloudUser != null) {
                                "Terhubung: ${cloudUser.email ?: cloudUser.displayName ?: "Akun Google"}"
                            } else {
                                "⚡ Sinkron Langsung Real-Time Aktif"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (cloudUser != null) Color(0xFF065F46) else Color(0xFF047857)
                        )
                    }

                    if (cloudUser == null) {
                        Button(
                            onClick = onLoginGoogle,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("google_login_button")
                        ) {
                            Text("Masuk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (canSignOut) {
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("google_signout_button")
                        ) {
                            Text("Keluar", fontSize = 10.sp, color = Color(0xFFDC2626))
                        }
                    } else {
                        // Logout disabled when logged in as cashier
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.testTag("google_signout_disabled_badge")
                        ) {
                            Text(
                                text = "Logout Dinonaktifkan (Kasir)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Project ID & Database Info Badges + Atur Hosting Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "☁️ Project:", fontSize = 10.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = projectId, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), maxLines = 1)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🗄️ Database:", fontSize = 10.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = databaseId, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenHostingSettings,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_configure_hosting_shortcut")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Atur", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // URL Container with Copy button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LINK WEB DASHBOARD:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = webUrl,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2563EB),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(webUrl))
                            Toast.makeText(context, "Link berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("copy_web_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Link",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Buka Web & Bagikan Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenWeb,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("open_web_dashboard_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Web", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onShareWeb,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("share_web_dashboard_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan Link", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sync Now Button & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = syncStatus ?: "Siap Sinkron",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("sync_now_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sinkron...", fontSize = 11.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sinkronkan Semua", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomDailyReportArchiveCard(
    reports: List<DailyReport>,
    isGenerating: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onGenerateToday: () -> Unit,
    onViewDetail: (DailyReport) -> Unit,
    onShareWhatsApp: (DailyReport) -> Unit,
    onDelete: (DailyReport) -> Unit
) {
    var reportToDelete by remember { mutableStateOf<DailyReport?>(null) }

    val filteredReports = remember(reports, searchQuery) {
        if (searchQuery.isBlank()) reports
        else reports.filter {
            it.date.contains(searchQuery.trim(), ignoreCase = true) ||
            it.cashierNames.contains(searchQuery.trim(), ignoreCase = true) ||
            it.notes.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_room_daily_reports"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF047857).copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Database Room: Arsip Laporan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFECFDF5)
                            ) {
                                Text(
                                    text = "SQLite",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tabel daily_reports • Tersimpan offline di HP & siap cetak/ekspor",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${reports.size} Arsip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Action & Search Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cari tanggal atau kasir...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                Button(
                    onClick = onGenerateToday,
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_generate_today_report")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Merekap...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Hari Ini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Reports Content
            if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ditemukan laporan untuk '$searchQuery'" else "Belum ada laporan harian di Database Room",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Laporan harian otomatis dibuat saat kasir tutup shift, atau klik tombol 'Simpan Hari Ini' di atas.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredReports.forEach { report ->
                        DailyReportRowItem(
                            report = report,
                            onViewDetail = { onViewDetail(report) },
                            onShareWhatsApp = { onShareWhatsApp(report) },
                            onDelete = { reportToDelete = report }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Alert for Delete
    if (reportToDelete != null) {
        val rep = reportToDelete!!
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("Hapus Laporan Room?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Laporan harian tanggal ${rep.date} (Omset ${BluetoothPrinterManager.formatRupiah(rep.totalRevenue)}) akan dihapus dari database lokal SQLite.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(rep)
                        reportToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun DailyReportRowItem(
    report: DailyReport,
    onViewDetail: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetail),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Date & Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (report.shiftCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE0E7FF)
                        ) {
                            Text(
                                text = "${report.shiftCount} Shift",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3730A3),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (report.isSynced) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Cloud", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "ID: #${report.id}",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Row 2: Metrics Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Omset", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = BluetoothPrinterManager.formatRupiah(report.totalRevenue),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Column {
                    Text("Laba Bersih", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = BluetoothPrinterManager.formatRupiah(report.totalProfit),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
                Column {
                    Text("Transaksi / Item", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = "${report.totalTransactions} Trx • ${report.totalItemsSold} Item",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }
            }

            // Row 3: Cash audit and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kas Fisik: ${BluetoothPrinterManager.formatRupiah(report.actualCash)}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    if (report.cashDifference != 0.0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${if (report.cashDifference > 0) "+" else ""}${BluetoothPrinterManager.formatRupiah(report.cashDifference)})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.cashDifference >= 0) Color(0xFF15803D) else Color(0xFFDC2626)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onViewDetail,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Detail Laporan",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onShareWhatsApp,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share WhatsApp",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Laporan",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyReportDetailDialog(
    report: DailyReport,
    storeProfile: StoreProfile,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laporan Harian (Room DB)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${storeProfile.storeName} • Tanggal: ${report.date}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider()

                // Omset & Keuntungan
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Penjualan (Omset):", fontSize = 12.sp, color = Color(0xFF1E3A8A))
                            Text(
                                BluetoothPrinterManager.formatRupiah(report.totalRevenue),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Keuntungan Bersih (Profit):", fontSize = 12.sp, color = Color(0xFF15803D))
                            Text(
                                BluetoothPrinterManager.formatRupiah(report.totalProfit),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Transaksi / Item:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(
                                "${report.totalTransactions} Transaksi (${report.totalItemsSold} Item)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                // Metode Pembayaran
                Text("Rincian Metode Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Tunai (Cash):", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(BluetoothPrinterManager.formatRupiah(report.totalCash), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• QRIS:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(BluetoothPrinterManager.formatRupiah(report.totalQris), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Transfer Bank:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(BluetoothPrinterManager.formatRupiah(report.totalTransfer), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Diskon Diberikan:", fontSize = 12.sp, color = Color(0xFFDC2626))
                            Text("-${BluetoothPrinterManager.formatRupiah(report.totalDiscount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                        }
                    }
                }

                // Kas Laci & Rekonsiliasi
                Text("Rekonsiliasi Kas Laci (Drawer)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Uang Diharapkan Sistem:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(BluetoothPrinterManager.formatRupiah(report.expectedCash), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Uang Dihitung Fisik Kasir:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(BluetoothPrinterManager.formatRupiah(report.actualCash), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selisih Kas:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val diff = report.cashDifference
                            val diffColor = if (diff >= 0) Color(0xFF15803D) else Color(0xFFDC2626)
                            Text(
                                text = "${if (diff > 0) "+" else ""}${BluetoothPrinterManager.formatRupiah(diff)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        }
                        if (report.cashierNames.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Petugas Kasir:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(report.cashierNames, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Produk Terlaris
                if (report.topProductSummary.isNotBlank()) {
                    Text("Produk Terlaris Hari Ini", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.topProductSummary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShareWhatsApp,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim WA", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPrint,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Thermal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

