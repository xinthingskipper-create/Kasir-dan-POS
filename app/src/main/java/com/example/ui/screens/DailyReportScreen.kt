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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkDateRolloverAndAutoArchive()
        viewModel.refreshReportData()
    }
    val clipboardManager = LocalClipboardManager.current
    val selectedFilter by viewModel.selectedDateFilter.collectAsState()
    val transactions by viewModel.reportTransactions.collectAsState()
    val topProducts by viewModel.topSellingProducts.collectAsState()
    val paymentStats by viewModel.paymentMethodStats.collectAsState()
    val netProfit by viewModel.netProfitFlow.collectAsState()
    val grossProfit by viewModel.grossProfitFlow.collectAsState()
    val totalExpenses by viewModel.totalExpensesFlow.collectAsState()
    val expensesList by viewModel.expensesFlow.collectAsState()

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
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showExpensesListDialog by remember { mutableStateOf(false) }
    var orderSearchQuery by remember { mutableStateOf("") }

    val isAdmin = currentUser?.role == UserRole.ADMIN

    // Filter transactions based on sync status filter and search query
    val filteredTransactions = remember(transactions, syncStatusFilter, orderSearchQuery, isAdmin) {
        val listBySync = if (isAdmin) {
            when (syncStatusFilter) {
                "SYNCED" -> transactions.filter { it.isSynced }
                "UNSYNCED" -> transactions.filter { !it.isSynced }
                else -> transactions
            }
        } else {
            transactions
        }

        if (orderSearchQuery.isBlank()) {
            listBySync
        } else {
            val query = orderSearchQuery.trim().lowercase(Locale.ROOT)
            listBySync.filter { trx ->
                trx.invoiceNumber.lowercase(Locale.ROOT).contains(query) ||
                trx.customerName.lowercase(Locale.ROOT).contains(query) ||
                trx.tableNumber.lowercase(Locale.ROOT).contains(query) ||
                trx.paymentMethod.name.lowercase(Locale.ROOT).contains(query) ||
                trx.paymentMethod.label.lowercase(Locale.ROOT).contains(query)
            }
        }
    }

    // Compute live metrics
    val totalRevenue = transactions.sumOf { it.grandTotal }
    val totalTrxCount = transactions.size
    val averageTicket = if (totalTrxCount > 0) totalRevenue / totalTrxCount else 0.0

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
        // Web Laporan & Omset Display
        if (isAdmin) {
            // Header: Laporan Keuangan Toko
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laporan Keuangan Toko",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Laba Kotor, Pengeluaran, & Laba Bersih",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live RTDB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
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
                            label = { Text(filter.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 1. KARTU LABA KOTOR
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_laba_kotor"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "LABA KOTOR",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "Penjualan kotor dikurangi modal HPP",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = selectedFilter.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = BluetoothPrinterManager.formatRupiah(grossProfit),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.testTag("text_laba_kotor_amount")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Penjualan Kotor (Omset):", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text(BluetoothPrinterManager.formatRupiah(totalRevenue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                                }
                            }
                            val cogs = if (totalRevenue >= grossProfit) totalRevenue - grossProfit else 0.0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Modal Produk (HPP):", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text(BluetoothPrinterManager.formatRupiah(cogs), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }
                }
            }

            // 2. KARTU PENGELUARAN (BEBAN OPERASIONAL)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_pengeluaran"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF3F1D28)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4C0519),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = Color(0xFFFB7185),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "PENGELUARAN",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDA4AF),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "Biaya operasional & beban toko",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF4C0519).copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${expensesList.size} Catatan",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFECDD3),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = BluetoothPrinterManager.formatRupiah(totalExpenses),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFB7185),
                            modifier = Modifier.testTag("text_pengeluaran_amount")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddExpenseDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_add_expense"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Catat Pengeluaran", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            if (expensesList.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { showExpensesListDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_view_expenses"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFB7185)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFB7185))
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rincian Beban", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. KARTU LABA BERSIH (HERO NET PROFIT)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_laba_bersih_hero"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF042F2E)),
                    border = BorderStroke(1.5.dp, Color(0xFF0D9488)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF134E4A),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF5EEAD4),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "LABA BERSIH",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF99F6E4),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = BluetoothPrinterManager.formatRupiah(netProfit),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2DD4BF),
                            modifier = Modifier.testTag("text_laba_bersih_amount")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF115E59)
                        ) {
                            Text(
                                text = "Laba Kotor (${BluetoothPrinterManager.formatRupiah(grossProfit)}) − Pengeluaran (${BluetoothPrinterManager.formatRupiah(totalExpenses)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCCFBF1),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons: WhatsApp & Print
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showWhatsAppDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_share_laba_wa"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp Owner", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.printProfitExpenseReceipt(
                                        grossProfit = grossProfit,
                                        expenses = totalExpenses,
                                        netProfit = netProfit,
                                        grossSales = totalRevenue
                                    ) { ok, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_print_laba"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cetak Struk", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Callout Banner: Penyimpanan Database Arsip Laporan Dipindahkan ke Hosting
            item {
                val separator = if (webDashboardUrl.contains("?")) "&" else "?"
                val reportWebUrl = "$webDashboardUrl${separator}projectId=$firebaseProjectId&databaseId=$firebaseDatabaseId"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Penyimpanan Database & Arsip di Hosting",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Arsip laporan harian & database tersimpan di Cloud Hosting",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.selectTab(com.example.ui.MainTab.CLOUD_HOSTING) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_go_to_hosting_tab"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Tab Hosting", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(reportWebUrl)))
                                    } catch (e: Exception) {
                                        clipboardManager.setText(AnnotatedString(reportWebUrl))
                                        Toast.makeText(context, "URL disalin: $reportWebUrl", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(40.dp)
                                    .testTag("btn_open_web_hosting_chrome"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Web Online Chrome", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Tampilan Riwayat Pesanan Khusus Kasir
        if (!isAdmin) {
            // Transaction History Header with Consumer Order Search
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Riwayat Pesanan Konsumen (${filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ketuk pesanan untuk melihat detail & cetak ulang struk",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Bar for Consumer Orders
                    OutlinedTextField(
                        value = orderSearchQuery,
                        onValueChange = { orderSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_orders"),
                        placeholder = {
                            Text(
                                text = "Cari pesanan (no. struk, nama pelanggan, menu)...",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (orderSearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { orderSearchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // Transaction History List
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (orderSearchQuery.isNotBlank()) "Tidak ditemukan pesanan dengan kata kunci \"$orderSearchQuery\""
                            else "Belum ada riwayat pesanan konsumen pada periode ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { trx ->
                    TransactionRowCard(
                        transaction = trx,
                        isAdmin = false,
                        onClick = { viewModel.showReceiptFor(trx) },
                        onSyncClick = null
                    )
                }
            }
        }
    }

    // Modal Kirim Laporan ke WhatsApp Owner
    if (showWhatsAppDialog) {
        val reportMessage = viewModel.generateWhatsAppProfitExpenseText(
            grossProfit = grossProfit,
            expenses = totalExpenses,
            netProfit = netProfit,
            grossSales = totalRevenue
        )

        WhatsAppSendDialog(
            initialPhone = storeProfile.ownerPhone,
            reportText = reportMessage,
            onDismiss = { showWhatsAppDialog = false }
        )
    }

    // Modal Tambah Pengeluaran Baru
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { title, amount, category, notes ->
                viewModel.addExpense(title, amount, category, notes)
                showAddExpenseDialog = false
                Toast.makeText(context, "Pengeluaran ${BluetoothPrinterManager.formatRupiah(amount)} berhasil dicatat", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Rincian Pengeluaran Toko
    if (showExpensesListDialog) {
        ExpensesListDialog(
            expenses = expensesList,
            totalExpenses = totalExpenses,
            onDelete = { exp ->
                viewModel.deleteExpense(exp)
                Toast.makeText(context, "Catatan pengeluaran dihapus", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showExpensesListDialog = false }
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
    isAdmin: Boolean = true,
    onClick: () -> Unit,
    onSyncClick: (() -> Unit)? = null
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        .format(Date(transaction.timestamp))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (!isAdmin || transaction.isSynced) Color(0xFFE2E8F0) else Color(0xFFFDE68A)),
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
                    if (isAdmin && transaction.profitAmount > 0) {
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

            if (isAdmin) {
                // Sync Status & Quick Sync Pill for Admin
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
            } else {
                // Cashier consumer order footer
                val totalQty = if (transaction.totalItemsCount > 0) transaction.totalItemsCount else 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "$totalQty item • ${transaction.orderType.label}" + (if (transaction.tableNumber.isNotBlank()) " • Meja ${transaction.tableNumber}" else ""),
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lihat Struk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                .padding(12.dp)
        ) {
            // Header: Title & Cloud Status Badge (Compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Column {
                        Text(
                            text = "Web Laporan Owner (Real-Time)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pantau penjualan via browser HP / Laptop",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Database Room: Arsip Laporan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFECFDF5)
                            ) {
                                Text(
                                    text = "SQLite",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tabel daily_reports • Tersimpan offline di HP",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${reports.size} Arsip",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
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

/**
 * Dialog Tambah Catatan Pengeluaran / Beban Toko
 */
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Operasional") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val categories = listOf("Operasional", "Utilitas", "Bahan/Kemasan", "Gaji", "Sewa", "Lainnya")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4C0519),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Catat Pengeluaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(errorMessage, color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Pengeluaran") },
                    placeholder = { Text("Contoh: Listrik, Plastik Kemasan, Es Batu") },
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) amountText = input
                    },
                    label = { Text("Nominal Biaya (Rp)") },
                    placeholder = { Text("Contoh: 50000") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Column {
                    Text("Kategori Pengeluaran", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE11D48),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Keterangan (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (title.isBlank()) {
                                errorMessage = "Nama pengeluaran tidak boleh kosong"
                            } else if (parsedAmount == null || parsedAmount <= 0.0) {
                                errorMessage = "Masukkan nominal pengeluaran yang valid"
                            } else {
                                onSave(title, parsedAmount, selectedCategory, notes)
                            }
                        },
                        modifier = Modifier.weight(1.2f).height(44.dp).testTag("btn_save_expense"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog Menampilkan Daftar Rincian Pengeluaran
 */
@Composable
fun ExpensesListDialog(
    expenses: List<com.example.data.model.Expense>,
    totalExpenses: Double,
    onDelete: (com.example.data.model.Expense) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rincian Pengeluaran Toko", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Total Beban: ${BluetoothPrinterManager.formatRupiah(totalExpenses)} (${expenses.size} item)",
                            fontSize = 12.sp,
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                if (expenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada catatan pengeluaran pada periode ini", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expenses, key = { it.id }) { exp ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF1F2),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF9F1239))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFECDD3)
                                            ) {
                                                Text(exp.category, fontSize = 10.sp, color = Color(0xFF881337), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.SemiBold)
                                            }
                                            Text(dateFormat.format(java.util.Date(exp.timestamp)), fontSize = 10.5.sp, color = Color(0xFF64748B))
                                        }
                                        if (exp.notes.isNotBlank()) {
                                            Text(exp.notes, fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = BluetoothPrinterManager.formatRupiah(exp.amount),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFFBE123C)
                                        )
                                        IconButton(onClick = { onDelete(exp) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

