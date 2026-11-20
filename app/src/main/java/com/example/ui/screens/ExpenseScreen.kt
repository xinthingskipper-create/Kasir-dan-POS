package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.Expense
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpensePeriodFilter(val label: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    THIS_MONTH("Bulan Ini"),
    CUSTOM_DATE("Pilih Tanggal")
}

val EXPENSE_CATEGORIES = listOf(
    "Operasional",
    "Bahan/Kemasan",
    "Utilitas",
    "Gaji",
    "Sewa",
    "Lainnya"
)

fun filterExpensesForScreen(
    expenses: List<Expense>,
    periodFilter: ExpensePeriodFilter,
    selectedDateMillis: Long,
    selectedCategory: String,
    searchQuery: String,
    referenceNowMillis: Long = System.currentTimeMillis()
): List<Expense> {
    val startEnd: Pair<Long, Long>? = when (periodFilter) {
        ExpensePeriodFilter.ALL -> null
        ExpensePeriodFilter.TODAY -> {
            PosViewModel.getStartOfDayForMillis(referenceNowMillis) to
                PosViewModel.getEndOfDayForMillis(referenceNowMillis)
        }
        ExpensePeriodFilter.THIS_MONTH -> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = referenceNowMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            start to cal.timeInMillis
        }
        ExpensePeriodFilter.CUSTOM_DATE -> {
            PosViewModel.getStartOfDayForMillis(selectedDateMillis) to
                PosViewModel.getEndOfDayForMillis(selectedDateMillis)
        }
    }

    val query = searchQuery.trim().lowercase(Locale.ROOT)
    return expenses.filter { exp ->
        val matchesDate = if (startEnd == null) {
            true
        } else {
            exp.timestamp in startEnd.first..startEnd.second
        }
        val matchesCat = selectedCategory == "Semua" ||
            exp.category.equals(selectedCategory, ignoreCase = true)
        val matchesQuery = query.isBlank() ||
            exp.title.lowercase(Locale.ROOT).contains(query) ||
            exp.category.lowercase(Locale.ROOT).contains(query) ||
            exp.notes.lowercase(Locale.ROOT).contains(query) ||
            exp.createdBy.lowercase(Locale.ROOT).contains(query)
        matchesDate && matchesCat && matchesQuery
    }.sortedByDescending { it.timestamp }
}

@Composable
fun ExpenseScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allExpenses by viewModel.allExpensesFlow.collectAsState()
    val isSyncing by viewModel.isExpenseSyncing.collectAsState()
    val syncStatus by viewModel.expenseSyncStatus.collectAsState()

    val webDashboardUrl by viewModel.webDashboardUrl.collectAsState()
    val firebaseProjectId by viewModel.firebaseProjectId.collectAsState()
    val firebaseDatabaseId by viewModel.firebaseDatabaseId.collectAsState()
    val localWebServerUrl by viewModel.localWebServerUrl.collectAsState()

    val reportWebUrl = remember(webDashboardUrl, firebaseProjectId, firebaseDatabaseId, localWebServerUrl) {
        viewModel.buildChromeWebReportUrl(view = "expenses")
    }

    ExpenseScreenContent(
        expenses = allExpenses,
        isSyncing = isSyncing,
        syncStatusText = syncStatus,
        onAddExpense = { title, amount, category, notes, timestampMillis ->
            viewModel.addExpense(
                title = title,
                amount = amount,
                category = category,
                notes = notes,
                timestampMillis = timestampMillis
            ) { synced, message ->
                Toast.makeText(
                    context,
                    if (synced) "Tersimpan & langsung sinkron ke Web Chrome!" else message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onDeleteExpense = { expense ->
            viewModel.deleteExpense(expense) { deleted ->
                Toast.makeText(
                    context,
                    if (deleted) "Pengeluaran '${expense.title}' dihapus & disinkronkan ke Web Chrome" else "Pengeluaran '${expense.title}' dihapus lokal",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onSyncNow = {
            viewModel.syncAllExpensesToCloud { ok, total ->
                val text = if (total == 0) {
                    "Belum ada catatan pengeluaran"
                } else {
                    "Sinkronisasi selesai: $ok/$total catatan masuk ke Web Chrome"
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        },
        onOpenWebChrome = {
            viewModel.syncExpensesForTransactionReportAuto(forceAll = true)
            val freshUrl = viewModel.buildChromeWebReportUrl(view = "expenses")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(freshUrl)))
            } catch (_: Exception) {
                clipboardManager.setText(AnnotatedString(freshUrl))
                Toast.makeText(context, "URL Web Chrome disalin: $freshUrl", Toast.LENGTH_LONG).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreenContent(
    expenses: List<Expense>,
    isSyncing: Boolean,
    syncStatusText: String,
    onAddExpense: (title: String, amount: Double, category: String, notes: String, timestampMillis: Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onSyncNow: () -> Unit,
    onOpenWebChrome: () -> Unit
) {
    var periodFilter by remember { mutableStateOf(ExpensePeriodFilter.ALL) }
    var selectedDateMillis by remember { mutableLongStateOf(PosViewModel.getStartOfDay(0)) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDatePicker by remember { mutableStateOf(false) }
    var expensePendingDelete by remember { mutableStateOf<Expense?>(null) }

    val dateTimeFormatter = remember {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
    }
    val dateShortFormatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    }

    val todayStart = PosViewModel.getStartOfDay(0)
    val todayEnd = PosViewModel.getEndOfDay(0)
    val todayTotal = remember(expenses, todayStart, todayEnd) {
        expenses.filter { it.timestamp in todayStart..todayEnd }.sumOf { it.amount }
    }

    val monthTotal = remember(expenses) {
        val monthExpenses = filterExpensesForScreen(
            expenses = expenses,
            periodFilter = ExpensePeriodFilter.THIS_MONTH,
            selectedDateMillis = todayStart,
            selectedCategory = "Semua",
            searchQuery = ""
        )
        monthExpenses.sumOf { it.amount }
    }

    val filteredExpenses = remember(expenses, periodFilter, selectedDateMillis, selectedCategory, searchQuery) {
        filterExpensesForScreen(
            expenses = expenses,
            periodFilter = periodFilter,
            selectedDateMillis = selectedDateMillis,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery
        )
    }
    val filteredTotal = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .testTag("expense_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Card + Auto-Sync Status + Primary Actions
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_expense_header"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFE4E6),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = "Ikon Catatan Pengeluaran",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Catatan Pengeluaran",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Langsung otomatis sinkron ke Laporan Web Chrome Online",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Auto-sync live status banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSyncing) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                            border = BorderStroke(
                                1.dp,
                                if (isSyncing) Color(0xFFFCD34D) else Color(0xFF86EFAC)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("badge_expense_sync_status")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFD97706)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Auto Sync Aktif",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = syncStatusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSyncing) Color(0xFF92400E) else Color(0xFF166534),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Action Buttons Row (min height 48.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("btn_add_expense"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catat Pengeluaran", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onSyncNow,
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("btn_sync_expenses_now"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sinkron", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                            }

                            OutlinedButton(
                                onClick = onOpenWebChrome,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("btn_open_expense_web_chrome"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C)),
                                border = BorderStroke(1.dp, Color(0xFFFDBA74))
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Buka Web Chrome", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Web", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }

            // 2. KPI Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExpenseSummaryCard(
                        title = "Hari Ini",
                        amountText = BluetoothPrinterManager.formatRupiah(todayTotal),
                        subtitle = "Beban hari ini",
                        accentColor = Color(0xFFE11D48),
                        containerColor = Color(0xFFFFF1F2),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_expense_today")
                    )
                    ExpenseSummaryCard(
                        title = "Bulan Ini",
                        amountText = BluetoothPrinterManager.formatRupiah(monthTotal),
                        subtitle = "Akumulasi bulan",
                        accentColor = Color(0xFFEA580C),
                        containerColor = Color(0xFFFFF7ED),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_expense_month")
                    )
                    ExpenseSummaryCard(
                        title = "Ditampilkan",
                        amountText = BluetoothPrinterManager.formatRupiah(filteredTotal),
                        subtitle = "${filteredExpenses.size} catatan",
                        accentColor = Color(0xFF0F766E),
                        containerColor = Color(0xFFF0FDFA),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_expense_filtered")
                    )
                }
            }

            // 3. Period, Date Picker, Search & Category Filters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpensePeriodFilter.entries.forEach { mode ->
                                val selected = periodFilter == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { periodFilter = mode },
                                    label = { Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE11D48),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("chip_expense_period_${mode.name}")
                                )
                            }
                        }

                        // Date Navigation Row when selecting Custom Date
                        if (periodFilter == ExpensePeriodFilter.CUSTOM_DATE) {
                            val isTodaySelected = selectedDateMillis >= todayStart
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedDateMillis = shiftExpenseDayMillis(selectedDateMillis, -1)
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("btn_expense_prev_date"),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Hari sebelumnya")
                                }

                                OutlinedButton(
                                    onClick = { showFilterDatePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("btn_expense_pick_date")
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dateShortFormatter.format(Date(selectedDateMillis)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!isTodaySelected) {
                                            selectedDateMillis = shiftExpenseDayMillis(selectedDateMillis, 1)
                                        }
                                    },
                                    enabled = !isTodaySelected,
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("btn_expense_next_date"),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Hari berikutnya")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari nama pengeluaran, keterangan, petugas...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Bersihkan pencarian")
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_expense")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (listOf("Semua") + EXPENSE_CATEGORIES).forEach { cat ->
                                val isCatSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isCatSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F766E),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("chip_expense_filter_$cat")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Empty State or Expense Item Cards
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_expense_empty"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Belum ada catatan pengeluaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ketuk 'Catat Pengeluaran' untuk menyimpan pengeluaran dan menyinkronkannya otomatis ke Laporan Web Chrome.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id.takeIf { id -> id > 0 } ?: it.timestamp }) { exp ->
                    ExpenseItemCard(
                        expense = exp,
                        formattedDate = dateTimeFormatter.format(Date(exp.timestamp)),
                        onDeleteClick = { expensePendingDelete = exp }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseWithDateDialog(
            initialTimestampMillis = if (periodFilter == ExpensePeriodFilter.CUSTOM_DATE) {
                combineDateWithCurrentTime(selectedDateMillis)
            } else {
                System.currentTimeMillis()
            },
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, notes, tsMillis ->
                showAddDialog = false
                if (periodFilter == ExpensePeriodFilter.CUSTOM_DATE) {
                    selectedDateMillis = PosViewModel.getStartOfDayForMillis(tsMillis)
                }
                onAddExpense(title, amount, category, notes, tsMillis)
            }
        )
    }

    if (showFilterDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcDayStartFromLocalMillis(selectedDateMillis),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= utcDayStartFromLocalMillis(todayStart) + 86_399_999L
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showFilterDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            selectedDateMillis = localDayStartFromUtcPicker(utcMillis)
                            periodFilter = ExpensePeriodFilter.CUSTOM_DATE
                        }
                        showFilterDatePicker = false
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFilterDatePicker = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    expensePendingDelete?.let { expToDelete ->
        AlertDialog(
            onDismissRequest = { expensePendingDelete = null },
            title = { Text("Hapus Catatan Pengeluaran?") },
            text = {
                Text(
                    "Catatan '${expToDelete.title}' sebesar ${BluetoothPrinterManager.formatRupiah(expToDelete.amount)} akan dihapus dari aplikasi dan Laporan Web Chrome."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        expensePendingDelete = null
                        onDeleteExpense(expToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("confirm_delete_expense_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expensePendingDelete = null },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ExpenseSummaryCard(
    title: String,
    amountText: String,
    subtitle: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = amountText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpenseItemCard(
    expense: Expense,
    formattedDate: String,
    onDeleteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_expense_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFE4E6)
                    ) {
                        Text(
                            text = expense.category.ifBlank { "Operasional" },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9F1239),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "$formattedDate • Oleh: ${expense.createdBy.ifBlank { "Kasir" }}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Auto Sync Web Chrome",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- ${BluetoothPrinterManager.formatRupiah(expense.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFBE123C)
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("btn_delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus ${expense.title}",
                        tint = Color(0xFFE11D48)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseWithDateDialog(
    initialTimestampMillis: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, notes: String, timestampMillis: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var notes by remember { mutableStateOf("") }
    var selectedTimestampMillis by remember { mutableLongStateOf(initialTimestampMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val quickAmounts = listOf(10_000, 25_000, 50_000, 100_000)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
            ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFE4E6),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Catat Pengeluaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = "" },
                    label = { Text("Nama / Keperluan Pengeluaran") },
                    placeholder = { Text("Contoh: Gas LPG, Es Batu, Listrik, Kemasan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_expense_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            amountText = input
                            errorMessage = ""
                        }
                    },
                    label = { Text("Nominal Biaya (Rp)") },
                    placeholder = { Text("Contoh: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_expense_amount"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { nominal ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    amountText = nominal.toString()
                                    errorMessage = ""
                                }
                                .defaultMinSize(minHeight = 40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Rp ${nominal / 1000}rb",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Kategori Pengeluaran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EXPENSE_CATEGORIES.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE11D48),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("chip_expense_cat_$cat")
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_expense_dialog_date"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tanggal: ${dateFormatter.format(Date(selectedTimestampMillis))}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Keterangan (Opsional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_expense_notes"),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Langsung otomatis sinkron ke Laporan Web Chrome Online",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
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
                                onSave(title, parsedAmount, selectedCategory, notes, selectedTimestampMillis)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_save_expense"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Simpan & Sinkron", fontWeight = FontWeight.Bold)
                    }
                }
            }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcDayStartFromLocalMillis(selectedTimestampMillis),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= utcDayStartFromLocalMillis(PosViewModel.getStartOfDay(0)) + 86_399_999L
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utcMillis ->
                            selectedTimestampMillis = combineDateWithCurrentTime(localDayStartFromUtcPicker(utcMillis))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun combineDateWithCurrentTime(targetDayStartMillis: Long): Long {
    val nowCal = Calendar.getInstance()
    return Calendar.getInstance().apply {
        timeInMillis = targetDayStartMillis
        set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
        set(Calendar.SECOND, nowCal.get(Calendar.SECOND))
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun utcDayStartFromLocalMillis(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }.timeInMillis
}

private fun shiftExpenseDayMillis(baseDayMillis: Long, deltaDays: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = baseDayMillis
        add(Calendar.DAY_OF_YEAR, deltaDays)
    }
    return PosViewModel.getStartOfDayForMillis(cal.timeInMillis)
}

private fun localDayStartFromUtcPicker(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    return Calendar.getInstance().apply {
        clear()
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }.timeInMillis
}
