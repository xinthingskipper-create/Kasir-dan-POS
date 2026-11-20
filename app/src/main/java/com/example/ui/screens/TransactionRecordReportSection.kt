package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.Expense
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

enum class TransactionRecordPeriod(val title: String, val chipLabel: String) {
    DAILY("Laporan Transaksi per Hari", "Harian"),
    MONTHLY("Laporan Transaksi per Bulan", "Bulanan"),
    YEARLY("Laporan Transaksi per Tahun", "Tahunan")
}

enum class TransactionReportMenuOption(
    val title: String,
    val defaultPeriod: TransactionRecordPeriod
) {
    TODAY("Laporan transaksi hari ini", TransactionRecordPeriod.DAILY),
    THIS_MONTH("Laporan transaksi bulan ini", TransactionRecordPeriod.DAILY),
    THIS_YEAR("Laporan transaksi tahun ini", TransactionRecordPeriod.MONTHLY),
    ALL("Laporan semua transaksi", TransactionRecordPeriod.DAILY)
}

enum class TransactionChartMetric(val label: String) {
    COUNT("Jml Transaksi"),
    REVENUE("Pendapatan"),
    PROFIT("Keuntungan"),
    EXPENSE("Pengeluaran")
}

data class TransactionPeriodRecord(
    val periodKey: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val fullPeriodTitle: String,
    val startTime: Long,
    val endTime: Long,
    val transactionCount: Int,
    val totalRevenue: Double,
    val totalProfit: Double,
    val year: Int,
    val monthZeroBased: Int,
    val dayOfMonth: Int,
    val transactions: List<Transaction>,
    val totalExpense: Double = 0.0,
    val expenseCount: Int = 0,
    val expenses: List<Expense> = emptyList()
) {
    val netProfit: Double get() = totalProfit - totalExpense
}

data class ChartPoint(
    val x: Float,
    val value: Double,
    val label: String
)

data class ChartTick(
    val x: Float,
    val label: String
)

data class TransactionChartSeries(
    val points: List<ChartPoint>,
    val xTicks: List<ChartTick>,
    val maxX: Float,
    val maxY: Double
)

private val INDONESIAN_SHORT_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
)

private val EMERALD_PRIMARY = Color(0xFF00796B)
private val EMERALD_DARK = Color(0xFF00695C)
private val SEGMENT_INACTIVE_BG = Color(0xFFCFD4DC)
private val CHART_GOLD = Color(0xFFEAB308)
private val CHART_AMBER = Color(0xFFF59E0B)

fun filterTransactionsByMenuOption(
    transactions: List<Transaction>,
    option: TransactionReportMenuOption,
    nowMillis: Long = System.currentTimeMillis()
): List<Transaction> {
    if (option == TransactionReportMenuOption.ALL || transactions.isEmpty()) return transactions

    val refCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val targetYear = refCal.get(Calendar.YEAR)
    val targetMonth = refCal.get(Calendar.MONTH)
    val targetDay = refCal.get(Calendar.DAY_OF_MONTH)

    return transactions.filter { trx ->
        val trxCal = Calendar.getInstance().apply { timeInMillis = trx.timestamp }
        val yearMatch = trxCal.get(Calendar.YEAR) == targetYear
        val monthMatch = yearMatch && trxCal.get(Calendar.MONTH) == targetMonth
        val dayMatch = monthMatch && trxCal.get(Calendar.DAY_OF_MONTH) == targetDay
        when (option) {
            TransactionReportMenuOption.TODAY -> dayMatch
            TransactionReportMenuOption.THIS_MONTH -> monthMatch
            TransactionReportMenuOption.THIS_YEAR -> yearMatch
            TransactionReportMenuOption.ALL -> true
        }
    }
}

fun filterExpensesByMenuOption(
    expenses: List<Expense>,
    option: TransactionReportMenuOption,
    nowMillis: Long = System.currentTimeMillis()
): List<Expense> {
    if (option == TransactionReportMenuOption.ALL || expenses.isEmpty()) return expenses

    val refCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val targetYear = refCal.get(Calendar.YEAR)
    val targetMonth = refCal.get(Calendar.MONTH)
    val targetDay = refCal.get(Calendar.DAY_OF_MONTH)

    return expenses.filter { exp ->
        val expCal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
        val yearMatch = expCal.get(Calendar.YEAR) == targetYear
        val monthMatch = yearMatch && expCal.get(Calendar.MONTH) == targetMonth
        val dayMatch = monthMatch && expCal.get(Calendar.DAY_OF_MONTH) == targetDay
        when (option) {
            TransactionReportMenuOption.TODAY -> dayMatch
            TransactionReportMenuOption.THIS_MONTH -> monthMatch
            TransactionReportMenuOption.THIS_YEAR -> yearMatch
            TransactionReportMenuOption.ALL -> true
        }
    }
}

fun aggregateTransactionRecords(
    transactions: List<Transaction>,
    period: TransactionRecordPeriod,
    expenses: List<Expense> = emptyList()
): List<TransactionPeriodRecord> {
    if (transactions.isEmpty() && expenses.isEmpty()) return emptyList()

    fun keyForTimestamp(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return when (period) {
            TransactionRecordPeriod.DAILY -> String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            TransactionRecordPeriod.MONTHLY -> String.format(Locale.US, "%04d-%02d", year, month + 1)
            TransactionRecordPeriod.YEARLY -> String.format(Locale.US, "%04d", year)
        }
    }

    val groupedTrx = transactions.groupBy { keyForTimestamp(it.timestamp) }
    val groupedExp = expenses.groupBy { keyForTimestamp(it.timestamp) }
    val allKeys = (groupedTrx.keys + groupedExp.keys).distinct()

    return allKeys.map { key ->
        val trxList = groupedTrx[key].orEmpty()
        val expList = groupedExp[key].orEmpty()
        val anchorTimestamp = trxList.firstOrNull()?.timestamp ?: expList.first().timestamp
        val firstCal = Calendar.getInstance().apply { timeInMillis = anchorTimestamp }
        val year = firstCal.get(Calendar.YEAR)
        val month = firstCal.get(Calendar.MONTH)
        val day = firstCal.get(Calendar.DAY_OF_MONTH)
        val monthShort = INDONESIAN_SHORT_MONTHS.getOrElse(month) { "Jan" }

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (period) {
                TransactionRecordPeriod.DAILY -> {
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TransactionRecordPeriod.MONTHLY -> {
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                TransactionRecordPeriod.YEARLY -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            when (period) {
                TransactionRecordPeriod.DAILY -> {
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TransactionRecordPeriod.MONTHLY -> {
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                TransactionRecordPeriod.YEARLY -> {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                }
            }
        }

        val (primary, secondary, fullTitle) = when (period) {
            TransactionRecordPeriod.DAILY -> Triple(
                day.toString(),
                "$monthShort\n$year",
                "$day $monthShort $year"
            )
            TransactionRecordPeriod.MONTHLY -> Triple(
                monthShort,
                year.toString(),
                "$monthShort $year"
            )
            TransactionRecordPeriod.YEARLY -> Triple(
                year.toString(),
                "Tahun",
                "Tahun $year"
            )
        }

        TransactionPeriodRecord(
            periodKey = key,
            primaryLabel = primary,
            secondaryLabel = secondary,
            fullPeriodTitle = fullTitle,
            startTime = startCal.timeInMillis,
            endTime = endCal.timeInMillis,
            transactionCount = trxList.size,
            totalRevenue = trxList.sumOf { it.grandTotal },
            totalProfit = trxList.sumOf { it.profitAmount },
            year = year,
            monthZeroBased = month,
            dayOfMonth = day,
            transactions = trxList.sortedByDescending { it.timestamp },
            totalExpense = expList.sumOf { it.amount },
            expenseCount = expList.size,
            expenses = expList.sortedByDescending { it.timestamp }
        )
    }.sortedByDescending { it.startTime }
}

fun formatMetricNodeLabel(value: Double, metric: TransactionChartMetric): String {
    return when (metric) {
        TransactionChartMetric.COUNT -> String.format(Locale("id", "ID"), "%.2f", value).replace('.', ',')
        TransactionChartMetric.REVENUE, TransactionChartMetric.PROFIT, TransactionChartMetric.EXPENSE -> {
            val rounded = value.roundToLong()
            when {
                rounded == 0L -> "0"
                kotlin.math.abs(rounded) >= 1_000_000L -> {
                    val jt = rounded / 1_000_000.0
                    String.format(Locale("id", "ID"), "%.1fjt", jt)
                }
                kotlin.math.abs(rounded) >= 1_000L -> {
                    val rb = rounded / 1_000.0
                    if (rounded % 1_000L == 0L) "${rounded / 1_000L}rb" else String.format(Locale("id", "ID"), "%.1frb", rb)
                }
                else -> rounded.toString()
            }
        }
    }
}

fun buildTransactionChartSeries(
    records: List<TransactionPeriodRecord>,
    period: TransactionRecordPeriod,
    metric: TransactionChartMetric
): TransactionChartSeries {
    val chronological = records.sortedBy { it.startTime }
    if (chronological.isEmpty()) {
        return TransactionChartSeries(
            points = emptyList(),
            xTicks = listOf(0, 3, 6, 9, 12, 15).map { ChartTick(it.toFloat(), it.toString()) },
            maxX = 15f,
            maxY = 1.0
        )
    }

    fun metricValue(rec: TransactionPeriodRecord): Double = when (metric) {
        TransactionChartMetric.COUNT -> rec.transactionCount.toDouble()
        TransactionChartMetric.REVENUE -> rec.totalRevenue
        TransactionChartMetric.PROFIT -> rec.totalProfit
        TransactionChartMetric.EXPENSE -> rec.totalExpense
    }

    val points = mutableListOf<ChartPoint>()
    val ticks = mutableListOf<ChartTick>()
    val maxX: Float

    when (period) {
        TransactionRecordPeriod.DAILY -> {
            val sameMonthAndYear = chronological.map { it.year to it.monthZeroBased }.distinct().size == 1
            if (sameMonthAndYear) {
                val maxDayInRecords = chronological.maxOf { it.dayOfMonth }
                val axisMax = max(15, ((maxDayInRecords + 2) / 3) * 3)
                maxX = axisMax.toFloat()

                if (chronological.size == 1) {
                    points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, metric)))
                }
                chronological.forEach { rec ->
                    val v = metricValue(rec)
                    points.add(ChartPoint(rec.dayOfMonth.toFloat(), v, formatMetricNodeLabel(v, metric)))
                }
                val step = if (axisMax <= 18) 3 else 5
                var t = 0
                while (t <= axisMax) {
                    ticks.add(ChartTick(t.toFloat(), t.toString()))
                    t += step
                }
            } else {
                if (chronological.size == 1) {
                    points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, metric)))
                }
                val startOffset = if (chronological.size == 1) 1 else 0
                chronological.forEachIndexed { idx, rec ->
                    val xVal = (idx + startOffset).toFloat()
                    val v = metricValue(rec)
                    points.add(ChartPoint(xVal, v, formatMetricNodeLabel(v, metric)))
                }
                maxX = max(1f, points.last().x)
                val step = max(1, chronological.size / 5)
                chronological.forEachIndexed { idx, rec ->
                    if (idx % step == 0 || idx == chronological.lastIndex) {
                        ticks.add(
                            ChartTick(
                                (idx + startOffset).toFloat(),
                                "${rec.dayOfMonth} ${INDONESIAN_SHORT_MONTHS[rec.monthZeroBased]}"
                            )
                        )
                    }
                }
            }
        }

        TransactionRecordPeriod.MONTHLY -> {
            val sameYear = chronological.map { it.year }.distinct().size == 1
            if (sameYear) {
                maxX = 12f
                if (chronological.size == 1) {
                    points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, metric)))
                }
                chronological.forEach { rec ->
                    val v = metricValue(rec)
                    val monthNum = (rec.monthZeroBased + 1).toFloat()
                    points.add(ChartPoint(monthNum, v, formatMetricNodeLabel(v, metric)))
                }
                listOf(0, 2, 4, 6, 8, 10, 12).forEach { m ->
                    val lbl = if (m == 0) "0" else INDONESIAN_SHORT_MONTHS[m - 1]
                    ticks.add(ChartTick(m.toFloat(), lbl))
                }
            } else {
                if (chronological.size == 1) {
                    points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, metric)))
                }
                val startOffset = if (chronological.size == 1) 1 else 0
                chronological.forEachIndexed { idx, rec ->
                    val xVal = (idx + startOffset).toFloat()
                    val v = metricValue(rec)
                    points.add(ChartPoint(xVal, v, formatMetricNodeLabel(v, metric)))
                }
                maxX = max(1f, points.last().x)
                val step = max(1, chronological.size / 4)
                chronological.forEachIndexed { idx, rec ->
                    if (idx % step == 0 || idx == chronological.lastIndex) {
                        ticks.add(
                            ChartTick(
                                (idx + startOffset).toFloat(),
                                "${INDONESIAN_SHORT_MONTHS[rec.monthZeroBased]} '${rec.year % 100}"
                            )
                        )
                    }
                }
            }
        }

        TransactionRecordPeriod.YEARLY -> {
            if (chronological.size == 1) {
                val rec = chronological.first()
                val v = metricValue(rec)
                points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, metric)))
                points.add(ChartPoint(1f, v, formatMetricNodeLabel(v, metric)))
                maxX = 1.15f
                ticks.add(ChartTick(0f, "0"))
                ticks.add(ChartTick(1f, rec.year.toString()))
            } else {
                chronological.forEachIndexed { idx, rec ->
                    val v = metricValue(rec)
                    points.add(ChartPoint(idx.toFloat(), v, formatMetricNodeLabel(v, metric)))
                    ticks.add(ChartTick(idx.toFloat(), rec.year.toString()))
                }
                maxX = (chronological.size - 1).toFloat().coerceAtLeast(1f)
            }
        }
    }

    val rawMaxY = points.maxOfOrNull { it.value } ?: 1.0
    val safeMaxY = if (rawMaxY <= 0.0) 1.0 else rawMaxY
    return TransactionChartSeries(
        points = points,
        xTicks = ticks,
        maxX = maxX,
        maxY = safeMaxY
    )
}

fun buildPerNotaChartSeries(
    transactions: List<Transaction>,
    metric: TransactionChartMetric
): TransactionChartSeries {
    val chronological = transactions.sortedBy { it.timestamp }
    if (chronological.isEmpty()) {
        return TransactionChartSeries(
            points = emptyList(),
            xTicks = listOf(0, 1, 2, 3, 4, 5).map { ChartTick(it.toFloat(), "#$it") },
            maxX = 5f,
            maxY = 1.0
        )
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    val points = mutableListOf<ChartPoint>()
    val ticks = mutableListOf<ChartTick>()

    val singleNota = chronological.size == 1
    if (singleNota) {
        points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, metric)))
        ticks.add(ChartTick(0f, "0"))
    }
    val offset = if (singleNota) 1 else 0
    chronological.forEachIndexed { idx, trx ->
        val xVal = (idx + offset).toFloat()
        val v = when (metric) {
            TransactionChartMetric.COUNT -> (idx + 1).toDouble()
            TransactionChartMetric.REVENUE -> trx.grandTotal
            TransactionChartMetric.PROFIT -> trx.profitAmount
            TransactionChartMetric.EXPENSE -> 0.0
        }
        points.add(ChartPoint(xVal, v, formatMetricNodeLabel(v, metric)))
    }

    val maxX = max(1f, points.last().x)
    val step = max(1, chronological.size / 4)
    chronological.forEachIndexed { idx, trx ->
        if (idx % step == 0 || idx == chronological.lastIndex) {
            ticks.add(
                ChartTick(
                    (idx + offset).toFloat(),
                    timeFmt.format(Date(trx.timestamp))
                )
            )
        }
    }

    val rawMaxY = points.maxOfOrNull { it.value } ?: 1.0
    val safeMaxY = if (rawMaxY <= 0.0) 1.0 else rawMaxY
    return TransactionChartSeries(
        points = points,
        xTicks = ticks,
        maxX = maxX,
        maxY = safeMaxY
    )
}

fun buildPerNotaExpenseChartSeries(
    expenses: List<Expense>
): TransactionChartSeries {
    val chronological = expenses.sortedBy { it.timestamp }
    if (chronological.isEmpty()) {
        return TransactionChartSeries(
            points = emptyList(),
            xTicks = listOf(0, 1, 2, 3, 4, 5).map { ChartTick(it.toFloat(), "#$it") },
            maxX = 5f,
            maxY = 1.0
        )
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    val points = mutableListOf<ChartPoint>()
    val ticks = mutableListOf<ChartTick>()
    val single = chronological.size == 1
    if (single) {
        points.add(ChartPoint(0f, 0.0, formatMetricNodeLabel(0.0, TransactionChartMetric.EXPENSE)))
        ticks.add(ChartTick(0f, "0"))
    }
    val offset = if (single) 1 else 0
    chronological.forEachIndexed { idx, exp ->
        val xVal = (idx + offset).toFloat()
        points.add(ChartPoint(xVal, exp.amount, formatMetricNodeLabel(exp.amount, TransactionChartMetric.EXPENSE)))
    }
    val maxX = max(1f, points.last().x)
    val step = max(1, chronological.size / 4)
    chronological.forEachIndexed { idx, exp ->
        if (idx % step == 0 || idx == chronological.lastIndex) {
            ticks.add(ChartTick((idx + offset).toFloat(), timeFmt.format(Date(exp.timestamp))))
        }
    }
    val rawMaxY = points.maxOfOrNull { it.value } ?: 1.0
    val safeMaxY = if (rawMaxY <= 0.0) 1.0 else rawMaxY
    return TransactionChartSeries(
        points = points,
        xTicks = ticks,
        maxX = maxX,
        maxY = safeMaxY
    )
}

private fun isSameLocalDay(millisA: Long, millisB: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = millisA }
    val calB = Calendar.getInstance().apply { timeInMillis = millisB }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

internal fun formatKpDateShort(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = INDONESIAN_SHORT_MONTHS.getOrElse(cal.get(Calendar.MONTH)) { "" }
    val year = cal.get(Calendar.YEAR)
    return "$day $month $year"
}

private fun shiftLocalDay(millis: Long, deltaDays: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = PosViewModel.getStartOfDayForMillis(millis)
        add(Calendar.DAY_OF_YEAR, deltaDays)
    }
    return cal.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReportDateSelectorBar(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val todayStart = PosViewModel.getStartOfDay(0)
    val isToday = isSameLocalDay(selectedDateMillis, todayStart)
    val formattedDate = remember(selectedDateMillis) { formatKpDateShort(selectedDateMillis) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0FDFA),
        border = BorderStroke(1.dp, Color(0xFF99F6E4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onDateSelected(shiftLocalDay(selectedDateMillis, -1)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_kp_prev_date")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Tanggal Sebelumnya",
                    tint = EMERALD_PRIMARY
                )
            }

            Surface(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCCFBF1)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_kp_pick_date")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pilih Tanggal Laporan",
                        tint = EMERALD_PRIMARY,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isToday) "Hari Ini • $formattedDate" else formattedDate,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isToday) {
                            Text(
                                text = "Tanggal Sebelumnya",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EMERALD_PRIMARY
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    val nextDay = shiftLocalDay(selectedDateMillis, 1)
                    if (nextDay <= todayStart) {
                        onDateSelected(nextDay)
                    }
                },
                enabled = !isToday,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_kp_next_date")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tanggal Berikutnya",
                    tint = if (isToday) Color(0xFF94A3B8) else EMERALD_PRIMARY
                )
            }

            if (!isToday) {
                TextButton(
                    onClick = { onDateSelected(todayStart) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_kp_today_date")
                ) {
                    Text(
                        text = "Hari Ini",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EMERALD_PRIMARY
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = PosViewModel.localStartOfDayToUtcPickerMillis(selectedDateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            modifier = Modifier.testTag("kp_date_picker_dialog"),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val localMillis = PosViewModel.normalizeUtcDatePickerMillisToLocalStartOfDay(utcMillis)
                            onDateSelected(localMillis)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("btn_confirm_kp_date")
                ) {
                    Text("Pilih", fontWeight = FontWeight.Bold, color = EMERALD_PRIMARY)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TransactionReportMenuCard(
    onSelectOption: (TransactionReportMenuOption) -> Unit,
    onBackClick: (() -> Unit)? = null,
    selectedDateMillis: Long? = null,
    onSelectedDateChange: ((Long) -> Unit)? = null,
    expenses: List<Expense> = emptyList(),
    onSelectExpenseReport: (() -> Unit)? = null,
    onAddExpenseClick: (() -> Unit)? = null,
    isExpenseSyncing: Boolean = false,
    expenseSyncStatus: String = "Auto-Sync Web Chrome aktif",
    onSyncExpensesNow: (() -> Unit)? = null,
    onOpenWebChromeExpenses: (() -> Unit)? = null
) {
    val effectiveDateMillis = selectedDateMillis ?: PosViewModel.getStartOfDay(0)
    val isToday = isSameLocalDay(effectiveDateMillis, PosViewModel.getStartOfDay(0))
    val formattedDate = remember(effectiveDateMillis) { formatKpDateShort(effectiveDateMillis) }
    val selectedDateExpenses = remember(expenses, effectiveDateMillis) {
        expenses.filter { isSameLocalDay(it.timestamp, effectiveDateMillis) }
    }
    val selectedDateExpenseTotal = remember(selectedDateExpenses) {
        selectedDateExpenses.sumOf { it.amount }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_transaction_report_menu"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header matching screenshot: Back arrow + "Laporan Transaksi"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_report_menu_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Kembali",
                            tint = EMERALD_PRIMARY,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = "Laporan Transaksi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EMERALD_PRIMARY,
                    modifier = Modifier.testTag("text_report_menu_header")
                )
            }

            if (onSelectedDateChange != null) {
                TransactionReportDateSelectorBar(
                    selectedDateMillis = effectiveDateMillis,
                    onDateSelected = onSelectedDateChange,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            TransactionReportMenuOption.values().forEachIndexed { index, option ->
                val optionSubLabel = when {
                    onSelectedDateChange == null -> null
                    option == TransactionReportMenuOption.TODAY ->
                        if (isToday) "Hari Ini ($formattedDate)" else "Tanggal dipilih: $formattedDate"
                    option == TransactionReportMenuOption.THIS_MONTH && !isToday ->
                        "Bulan dari tanggal $formattedDate"
                    option == TransactionReportMenuOption.THIS_YEAR && !isToday ->
                        "Tahun dari tanggal $formattedDate"
                    else -> null
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable { onSelectOption(option) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag("menu_report_${option.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalyticsMagnifierIcon(
                        color = EMERALD_PRIMARY,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        if (!optionSubLabel.isNullOrBlank()) {
                            Text(
                                text = optionSubLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isToday) Color(0xFF64748B) else EMERALD_PRIMARY
                            )
                        }
                    }
                }

                if (index < TransactionReportMenuOption.values().lastIndex) {
                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (onSelectExpenseReport != null) {
                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .background(Color(0xFFFFF1F2).copy(alpha = 0.45f))
                        .clickable { onSelectExpenseReport() }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag("menu_report_expenses"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFE4E6),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Laporan Pengeluaran",
                                tint = Color(0xFFBE123C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Laporan pengeluaran",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF881337)
                        )
                        Text(
                            text = if (selectedDateExpenses.isEmpty()) {
                                "Belum ada pengeluaran ($formattedDate)"
                            } else {
                                "${selectedDateExpenses.size} catatan • -${BluetoothPrinterManager.formatRupiah(selectedDateExpenseTotal)} ($formattedDate)"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBE123C)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .testTag("badge_menu_expense_autosync")
                        ) {
                            Icon(
                                imageVector = if (isExpenseSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                contentDescription = "Status Auto-Sync Pengeluaran Web Chrome",
                                tint = if (isExpenseSyncing) Color(0xFFD97706) else EMERALD_PRIMARY,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isExpenseSyncing) "Menyinkronkan ke Web Chrome..." else "Auto-Sync Web Chrome",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpenseSyncing) Color(0xFFB45309) else EMERALD_PRIMARY,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onOpenWebChromeExpenses != null) {
                            IconButton(
                                onClick = onOpenWebChromeExpenses,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_open_web_expense_from_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Buka Laporan Pengeluaran di Web Chrome",
                                    tint = EMERALD_PRIMARY,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (onAddExpenseClick != null) {
                            OutlinedButton(
                                onClick = onAddExpenseClick,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_quick_add_expense_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Catat Pengeluaran",
                                    tint = Color(0xFFBE123C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Catat",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBE123C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionExpenseReportPanel(
    allTransactions: List<Transaction>,
    allExpenses: List<Expense>,
    storeProfile: StoreProfile,
    selectedDateMillis: Long,
    onSelectedDateChange: (Long) -> Unit,
    onBackToMenu: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    isExpenseSyncing: Boolean = false,
    expenseSyncStatus: String = "Otomatis tersinkron ke Laporan Web Chrome",
    onSyncExpensesNow: (() -> Unit)? = null,
    onOpenWebChromeExpenses: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val timeFmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")) }

    var scopeOption by remember { mutableStateOf(TransactionReportMenuOption.TODAY) }
    var searchQuery by remember { mutableStateOf("") }
    var expensePendingDelete by remember { mutableStateOf<Expense?>(null) }

    val scopedExpenses = remember(allExpenses, scopeOption, selectedDateMillis) {
        filterExpensesByMenuOption(allExpenses, scopeOption, selectedDateMillis)
            .sortedByDescending { it.timestamp }
    }
    val scopedTransactions = remember(allTransactions, scopeOption, selectedDateMillis) {
        filterTransactionsByMenuOption(allTransactions, scopeOption, selectedDateMillis)
    }

    val filteredExpenses = remember(scopedExpenses, searchQuery) {
        if (searchQuery.isBlank()) scopedExpenses
        else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            scopedExpenses.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.category.lowercase(Locale.ROOT).contains(q) ||
                    it.notes.lowercase(Locale.ROOT).contains(q) ||
                    it.createdBy.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    val totalRevenue = remember(scopedTransactions) { scopedTransactions.sumOf { it.grandTotal } }
    val grossProfit = remember(scopedTransactions) { scopedTransactions.sumOf { it.profitAmount } }
    val totalExpense = remember(scopedExpenses) { scopedExpenses.sumOf { it.amount } }
    val netProfit = grossProfit - totalExpense
    val formattedDate = remember(selectedDateMillis) { formatKpDateShort(selectedDateMillis) }

    val scopeLabel = when (scopeOption) {
        TransactionReportMenuOption.TODAY -> formattedDate
        TransactionReportMenuOption.THIS_MONTH -> "Bulan (${formattedDate.substringAfter(" ")})"
        TransactionReportMenuOption.THIS_YEAR -> {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            "Tahun ${cal.get(Calendar.YEAR)}"
        }
        TransactionReportMenuOption.ALL -> "Semua Waktu"
    }

    fun shareExpenseSummary() {
        val summary = buildString {
            appendLine("💸 *LAPORAN PENGELUARAN TRANSAKSI*")
            appendLine("🏪 ${storeProfile.storeName}")
            appendLine("📅 Cakupan: $scopeLabel")
            appendLine("══════════════════════")
            appendLine("💰 Pendapatan: ${BluetoothPrinterManager.formatRupiah(totalRevenue)}")
            appendLine("📈 Laba Kotor: ${BluetoothPrinterManager.formatRupiah(grossProfit)}")
            appendLine("💸 Total Pengeluaran (${scopedExpenses.size}): -${BluetoothPrinterManager.formatRupiah(totalExpense)}")
            appendLine("📊 Laba Bersih: ${BluetoothPrinterManager.formatRupiah(netProfit)}")
            appendLine("──────────────────────")
            if (scopedExpenses.isEmpty()) {
                appendLine("Belum ada catatan pengeluaran pada periode ini.")
            } else {
                scopedExpenses.take(25).forEach { exp ->
                    appendLine("• *${exp.title}* [${exp.category}] (${timeFmt.format(Date(exp.timestamp))})")
                    appendLine("  Nominal: -${BluetoothPrinterManager.formatRupiah(exp.amount)}${if (exp.notes.isNotBlank()) " | ${exp.notes}" else ""}")
                }
            }
        }
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Pengeluaran - ${storeProfile.storeName}")
                putExtra(Intent.EXTRA_TEXT, summary)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan Pengeluaran"))
        } catch (_: Exception) {
            clipboardManager.setText(AnnotatedString(summary))
            Toast.makeText(context, "Laporan pengeluaran disalin ke clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_transaction_expense_report"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToMenu,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_back_expense_to_menu")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Kembali ke Menu Laporan",
                        tint = EMERALD_PRIMARY,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Laporan Pengeluaran",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBE123C),
                        modifier = Modifier.testTag("text_expense_report_title")
                    )
                    Text(
                        text = "Terhubung ke Laporan Transaksi • $scopeLabel",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = { shareExpenseSummary() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_share_expense_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan Laporan Pengeluaran",
                        tint = EMERALD_PRIMARY
                    )
                }

                Button(
                    onClick = onAddExpenseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_add_expense_from_report")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Date Selector Bar
            TransactionReportDateSelectorBar(
                selectedDateMillis = selectedDateMillis,
                onDateSelected = onSelectedDateChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Scope Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val scopeLabels = listOf(
                    TransactionReportMenuOption.TODAY to "Per Tgl",
                    TransactionReportMenuOption.THIS_MONTH to "Bulan",
                    TransactionReportMenuOption.THIS_YEAR to "Tahun",
                    TransactionReportMenuOption.ALL to "Semua"
                )
                scopeLabels.forEach { (opt, lbl) ->
                    val isSelected = scopeOption == opt
                    Surface(
                        onClick = { scopeOption = opt },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFBE123C) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF9F1239) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tab_expense_scope_${opt.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = lbl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Auto-Sync Web Chrome Banner inside Laporan Pengeluaran
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("badge_trx_expense_autosync"),
                shape = RoundedCornerShape(10.dp),
                color = if (isExpenseSyncing) Color(0xFFFFFBEB) else Color(0xFFF0FDFA),
                border = BorderStroke(1.dp, if (isExpenseSyncing) Color(0xFFFDE68A) else Color(0xFF99F6E4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isExpenseSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFD97706)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Auto-Sync Web Chrome",
                            tint = EMERALD_PRIMARY,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isExpenseSyncing) "Menyinkronkan ke Web Chrome..." else "Auto-Sync Web Chrome Aktif",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpenseSyncing) Color(0xFFB45309) else EMERALD_PRIMARY
                        )
                        Text(
                            text = expenseSyncStatus,
                            fontSize = 10.5.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (onSyncExpensesNow != null) {
                        IconButton(
                            onClick = onSyncExpensesNow,
                            enabled = !isExpenseSyncing,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_sync_trx_expense_now")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronkan Pengeluaran Sekarang",
                                tint = EMERALD_PRIMARY,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    if (onOpenWebChromeExpenses != null) {
                        OutlinedButton(
                            onClick = onOpenWebChromeExpenses,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF5EEAD4)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_open_web_expense_from_trx_report")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = EMERALD_PRIMARY,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Web",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EMERALD_PRIMARY
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary KPI Strip: Total Pengeluaran & Dampak ke Laba Bersih
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF1F2),
                border = BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Pengeluaran (${scopedExpenses.size} catatan)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9F1239)
                            )
                            Text(
                                text = "- ${BluetoothPrinterManager.formatRupiah(totalExpense)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFBE123C),
                                modifier = Modifier.testTag("text_total_expense_panel")
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Laba Bersih",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = BluetoothPrinterManager.formatRupiah(netProfit),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netProfit >= 0) EMERALD_PRIMARY else Color(0xFFDC2626)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFFECDD3))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Omset Penjualan: ${BluetoothPrinterManager.formatRupiah(totalRevenue)}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Laba Kotor: ${BluetoothPrinterManager.formatRupiah(grossProfit)}",
                            fontSize = 11.sp,
                            color = EMERALD_PRIMARY,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari pengeluaran, kategori, catatan...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFFBE123C),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("input_search_trx_expenses")
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada catatan pengeluaran pada $scopeLabel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    filteredExpenses.forEach { exp ->
                        TransactionExpenseItemRow(
                            expense = exp,
                            timeLabel = timeFmt.format(Date(exp.timestamp)),
                            onDeleteClick = { expensePendingDelete = exp }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }

    if (expensePendingDelete != null) {
        val target = expensePendingDelete!!
        AlertDialog(
            onDismissRequest = { expensePendingDelete = null },
            title = { Text("Hapus Catatan Pengeluaran?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Pengeluaran '${target.title}' sebesar ${BluetoothPrinterManager.formatRupiah(target.amount)} akan dihapus dan disinkronkan ke Web Chrome.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(target)
                        expensePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_trx_expense")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { expensePendingDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun TransactionExpenseItemRow(
    expense: Expense,
    timeLabel: String,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("row_record_expense_${expense.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = expense.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFE4E6)
                ) {
                    Text(
                        text = expense.category.ifBlank { "Operasional" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBE123C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "$timeLabel • Oleh: ${expense.createdBy.ifBlank { "Admin" }}",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            if (expense.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = expense.notes,
                    fontSize = 11.5.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "- ${BluetoothPrinterManager.formatRupiah(expense.amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFBE123C)
        )

        if (onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_delete_record_expense_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Hapus ${expense.title}",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMagnifierIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.068f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Baseline chart axis
        drawLine(
            color = color,
            start = Offset(w * 0.10f, h * 0.88f),
            end = Offset(w * 0.78f, h * 0.88f),
            strokeWidth = w * 0.068f,
            cap = StrokeCap.Round
        )

        // Bar columns
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.88f),
            end = Offset(w * 0.22f, h * 0.62f),
            strokeWidth = w * 0.068f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.40f, h * 0.88f),
            end = Offset(w * 0.40f, h * 0.44f),
            strokeWidth = w * 0.068f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.88f),
            end = Offset(w * 0.58f, h * 0.72f),
            strokeWidth = w * 0.068f,
            cap = StrokeCap.Round
        )

        // Line trend nodes
        val n1 = Offset(w * 0.22f, h * 0.46f)
        val n2 = Offset(w * 0.40f, h * 0.28f)
        val n3 = Offset(w * 0.60f, h * 0.22f)
        drawLine(color = color, start = n1, end = n2, strokeWidth = w * 0.065f, cap = StrokeCap.Round)
        drawLine(color = color, start = n2, end = n3, strokeWidth = w * 0.065f, cap = StrokeCap.Round)

        listOf(n1, n2).forEach { c ->
            drawCircle(color = Color.White, radius = w * 0.07f, center = c)
            drawCircle(color = color, radius = w * 0.07f, center = c, style = stroke)
        }

        // Magnifying glass ring + handle
        val magCenter = Offset(w * 0.62f, h * 0.48f)
        val magRadius = w * 0.22f
        drawCircle(color = Color.White, radius = magRadius, center = magCenter)
        drawCircle(color = color, radius = magRadius, center = magCenter, style = stroke)
        drawLine(
            color = color,
            start = Offset(magCenter.x + magRadius * 0.70f, magCenter.y + magRadius * 0.70f),
            end = Offset(w * 0.90f, h * 0.84f),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TransactionRecordReportSection(
    allTransactions: List<Transaction>,
    storeProfile: StoreProfile,
    onDeletePeriod: (TransactionPeriodRecord) -> Unit,
    onOpenReceipt: (Transaction) -> Unit,
    onDeleteSingleTransaction: (Transaction) -> Unit,
    initialPeriod: TransactionRecordPeriod = TransactionRecordPeriod.DAILY,
    menuOptionTitle: String? = null,
    onBackToMenu: (() -> Unit)? = null,
    selectedDateMillis: Long? = null,
    onSelectedDateChange: ((Long) -> Unit)? = null,
    allExpenses: List<Expense> = emptyList(),
    onDeleteExpense: ((Expense) -> Unit)? = null,
    onAddExpenseClick: (() -> Unit)? = null,
    isExpenseSyncing: Boolean = false,
    expenseSyncStatus: String = "Otomatis tersinkron ke Laporan Web Chrome",
    onSyncExpensesNow: (() -> Unit)? = null,
    onOpenWebChromeExpenses: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedPeriod by remember(initialPeriod) { mutableStateOf(initialPeriod) }
    var selectedMetric by remember { mutableStateOf(TransactionChartMetric.COUNT) }
    var menuExpanded by remember { mutableStateOf(false) }
    var periodToDelete by remember { mutableStateOf<TransactionPeriodRecord?>(null) }
    var periodForDetail by remember { mutableStateOf<TransactionPeriodRecord?>(null) }
    var singleNotaToDelete by remember { mutableStateOf<Transaction?>(null) }
    var singleExpenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var localDateMillis by remember { mutableStateOf<Long?>(null) }

    val activeDateMillis = selectedDateMillis ?: localDateMillis ?: PosViewModel.getStartOfDay(0)
    val todayStartMillis = PosViewModel.getStartOfDay(0)
    val isActiveDateToday = isSameLocalDay(activeDateMillis, todayStartMillis)
    val activeDateFormatted = remember(activeDateMillis) { formatKpDateShort(activeDateMillis) }

    val handleDateChange: (Long) -> Unit = { newMillis ->
        val normalized = PosViewModel.getStartOfDayForMillis(newMillis)
        if (onSelectedDateChange != null) {
            onSelectedDateChange(normalized)
        } else {
            localDateMillis = normalized
        }
    }

    val isTodayPerNotaMode = menuOptionTitle == TransactionReportMenuOption.TODAY.title &&
        selectedPeriod == TransactionRecordPeriod.DAILY

    val effectiveTransactions = remember(allTransactions, onSelectedDateChange, localDateMillis, isTodayPerNotaMode) {
        if (onSelectedDateChange == null && localDateMillis != null && isTodayPerNotaMode) {
            allTransactions.filter { isSameLocalDay(it.timestamp, localDateMillis!!) }
        } else {
            allTransactions
        }
    }
    val effectiveExpenses = remember(allExpenses, onSelectedDateChange, localDateMillis, isTodayPerNotaMode) {
        if (onSelectedDateChange == null && localDateMillis != null && isTodayPerNotaMode) {
            allExpenses.filter { isSameLocalDay(it.timestamp, localDateMillis!!) }
        } else {
            allExpenses
        }
    }

    val sortedPerNotaTransactions = remember(effectiveTransactions) {
        effectiveTransactions.sortedByDescending { it.timestamp }
    }
    val sortedPerNotaExpenses = remember(effectiveExpenses) {
        effectiveExpenses.sortedByDescending { it.timestamp }
    }
    val periodRecords = remember(effectiveTransactions, effectiveExpenses, selectedPeriod) {
        aggregateTransactionRecords(effectiveTransactions, selectedPeriod, effectiveExpenses)
    }
    val chartSeries = remember(
        isTodayPerNotaMode,
        sortedPerNotaTransactions,
        sortedPerNotaExpenses,
        periodRecords,
        selectedPeriod,
        selectedMetric
    ) {
        if (isTodayPerNotaMode) {
            if (selectedMetric == TransactionChartMetric.EXPENSE) {
                buildPerNotaExpenseChartSeries(sortedPerNotaExpenses)
            } else {
                buildPerNotaChartSeries(sortedPerNotaTransactions, selectedMetric)
            }
        } else {
            buildTransactionChartSeries(periodRecords, selectedPeriod, selectedMetric)
        }
    }

    fun buildShareSummaryText(): String {
        val timeFmt = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        if (isTodayPerNotaMode) {
            val totalTrx = sortedPerNotaTransactions.size
            val totalRev = sortedPerNotaTransactions.sumOf { it.grandTotal }
            val totalProf = sortedPerNotaTransactions.sumOf { it.profitAmount }
            val totalExp = sortedPerNotaExpenses.sumOf { it.amount }
            val netProf = totalProf - totalExp
            val headerTitle = if (isActiveDateToday) {
                (menuOptionTitle ?: selectedPeriod.title).uppercase(Locale.getDefault())
            } else {
                "LAPORAN TRANSAKSI ($activeDateFormatted)"
            }
            return buildString {
                appendLine("📊 *$headerTitle (PER NOTA)*")
                appendLine("📅 Tanggal: $activeDateFormatted")
                appendLine("🏪 ${storeProfile.storeName}")
                appendLine("══════════════════════")
                appendLine("🧾 Total Nota: $totalTrx")
                appendLine("💰 Total Pendapatan: ${BluetoothPrinterManager.formatRupiah(totalRev)}")
                appendLine("📈 Keuntungan Kotor: ${BluetoothPrinterManager.formatRupiah(totalProf)}")
                appendLine("💸 Total Pengeluaran (${sortedPerNotaExpenses.size}): -${BluetoothPrinterManager.formatRupiah(totalExp)}")
                appendLine("✅ Laba Bersih: ${BluetoothPrinterManager.formatRupiah(netProf)}")
                appendLine("──────────────────────")
                sortedPerNotaTransactions.take(25).forEach { trx ->
                    appendLine("• *#${trx.invoiceNumber}* (${timeFmt.format(Date(trx.timestamp))} • ${trx.paymentMethod.label})")
                    appendLine("  Pendapatan: ${BluetoothPrinterManager.formatRupiah(trx.grandTotal)} | Keuntungan: ${BluetoothPrinterManager.formatRupiah(trx.profitAmount)}")
                }
                if (sortedPerNotaExpenses.isNotEmpty()) {
                    appendLine("──────────────────────")
                    appendLine("💸 *LAPORAN PENGELUARAN:*")
                    sortedPerNotaExpenses.take(20).forEach { exp ->
                        appendLine("• *${exp.title}* [${exp.category}] (${timeFmt.format(Date(exp.timestamp))}): -${BluetoothPrinterManager.formatRupiah(exp.amount)}")
                    }
                }
            }
        }
        val totalTrx = periodRecords.sumOf { it.transactionCount }
        val totalRev = periodRecords.sumOf { it.totalRevenue }
        val totalProf = periodRecords.sumOf { it.totalProfit }
        val totalExp = periodRecords.sumOf { it.totalExpense }
        val netProf = totalProf - totalExp
        return buildString {
            appendLine("📊 *${(menuOptionTitle ?: selectedPeriod.title).uppercase(Locale.getDefault())}*")
            appendLine("🏪 ${storeProfile.storeName}")
            appendLine("══════════════════════")
            appendLine("🧾 Total Transaksi: $totalTrx")
            appendLine("💰 Total Pendapatan: ${BluetoothPrinterManager.formatRupiah(totalRev)}")
            appendLine("📈 Keuntungan Kotor: ${BluetoothPrinterManager.formatRupiah(totalProf)}")
            appendLine("💸 Total Pengeluaran: -${BluetoothPrinterManager.formatRupiah(totalExp)}")
            appendLine("✅ Laba Bersih: ${BluetoothPrinterManager.formatRupiah(netProf)}")
            appendLine("──────────────────────")
            periodRecords.take(15).forEach { rec ->
                appendLine("• *${rec.fullPeriodTitle}* (${rec.transactionCount} transaksi • ${rec.expenseCount} pengeluaran)")
                appendLine("  Pendapatan: ${BluetoothPrinterManager.formatRupiah(rec.totalRevenue)} | Laba Kotor: ${BluetoothPrinterManager.formatRupiah(rec.totalProfit)} | Pengeluaran: -${BluetoothPrinterManager.formatRupiah(rec.totalExpense)}")
            }
        }
    }

    fun shareRecordSummary() {
        val summary = buildShareSummaryText()
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${menuOptionTitle ?: selectedPeriod.title} - ${storeProfile.storeName}")
                putExtra(Intent.EXTRA_TEXT, summary)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Bagikan ${selectedPeriod.title}"))
        } catch (_: Exception) {
            clipboardManager.setText(AnnotatedString(summary))
            Toast.makeText(context, "Ringkasan laporan disalin ke clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_transaction_record_report"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Bar (Emerald Header + Back Button + Period Selector Pills + Overflow Menu)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackToMenu != null) {
                        IconButton(
                            onClick = onBackToMenu,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_back_to_report_menu")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Kembali ke Menu Laporan",
                                tint = EMERALD_PRIMARY,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (onBackToMenu != null) 2.dp else 4.dp)
                    ) {
                        Text(
                            text = selectedPeriod.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EMERALD_PRIMARY,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("text_record_report_title")
                        )
                        if (!menuOptionTitle.isNullOrBlank()) {
                            val subtitleText = if (isTodayPerNotaMode) {
                                if (isActiveDateToday) {
                                    "$menuOptionTitle • $activeDateFormatted"
                                } else {
                                    "Catatan transaksi tanggal $activeDateFormatted"
                                }
                            } else {
                                menuOptionTitle
                            }
                            Text(
                                text = subtitleText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (!isActiveDateToday && isTodayPerNotaMode) EMERALD_PRIMARY else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_record_report_more")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opsi Periode Laporan",
                                tint = EMERALD_PRIMARY
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            TransactionRecordPeriod.values().forEach { period ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = period.title,
                                            fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedPeriod == period) EMERALD_PRIMARY else Color(0xFF0F172A)
                                        )
                                    },
                                    onClick = {
                                        selectedPeriod = period
                                        menuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Bagikan Laporan") },
                                onClick = {
                                    menuExpanded = false
                                    shareRecordSummary()
                                }
                            )
                        }
                    }
                }

                // Harian / Bulanan / Tahunan Period Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionRecordPeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            onClick = { selectedPeriod = period },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) EMERALD_PRIMARY else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) EMERALD_DARK else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("tab_record_period_${period.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = period.chipLabel,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                if (isTodayPerNotaMode || onSelectedDateChange != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TransactionReportDateSelectorBar(
                        selectedDateMillis = activeDateMillis,
                        onDateSelected = handleDateChange
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // Responsive Split Pane (Landscape/Tablet >= 600dp side-by-side matching screenshot; Portrait stacked)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp
                val perNotaDateDisplay = if (selectedDateMillis != null || localDateMillis != null) activeDateMillis else null

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 340.dp)
                    ) {
                        LeftChartPane(
                            selectedMetric = selectedMetric,
                            onSelectMetric = { selectedMetric = it },
                            chartSeries = chartSeries,
                            storeName = storeProfile.storeName,
                            onShare = { shareRecordSummary() },
                            modifier = Modifier
                                .weight(0.46f)
                                .padding(16.dp)
                        )

                        VerticalDivider(
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp,
                            modifier = Modifier.heightIn(min = 340.dp)
                        )

                        if (isTodayPerNotaMode) {
                            RightPerNotaListPane(
                                transactions = sortedPerNotaTransactions,
                                onDeleteTransaction = { singleNotaToDelete = it },
                                onOpenReceipt = onOpenReceipt,
                                selectedDateMillis = perNotaDateDisplay,
                                expenses = sortedPerNotaExpenses,
                                onDeleteExpense = if (onDeleteExpense != null) ({ singleExpenseToDelete = it }) else null,
                                onAddExpenseClick = onAddExpenseClick,
                                isExpenseSyncing = isExpenseSyncing,
                                expenseSyncStatus = expenseSyncStatus,
                                onSyncExpensesNow = onSyncExpensesNow,
                                onOpenWebChromeExpenses = onOpenWebChromeExpenses,
                                modifier = Modifier.weight(0.54f)
                            )
                        } else {
                            RightRecordListPane(
                                records = periodRecords,
                                period = selectedPeriod,
                                onDeleteRecord = { periodToDelete = it },
                                onOpenDetail = { periodForDetail = it },
                                modifier = Modifier.weight(0.54f)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LeftChartPane(
                            selectedMetric = selectedMetric,
                            onSelectMetric = { selectedMetric = it },
                            chartSeries = chartSeries,
                            storeName = storeProfile.storeName,
                            onShare = { shareRecordSummary() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        if (isTodayPerNotaMode) {
                            RightPerNotaListPane(
                                transactions = sortedPerNotaTransactions,
                                onDeleteTransaction = { singleNotaToDelete = it },
                                onOpenReceipt = onOpenReceipt,
                                selectedDateMillis = perNotaDateDisplay,
                                expenses = sortedPerNotaExpenses,
                                onDeleteExpense = if (onDeleteExpense != null) ({ singleExpenseToDelete = it }) else null,
                                onAddExpenseClick = onAddExpenseClick,
                                isExpenseSyncing = isExpenseSyncing,
                                expenseSyncStatus = expenseSyncStatus,
                                onSyncExpensesNow = onSyncExpensesNow,
                                onOpenWebChromeExpenses = onOpenWebChromeExpenses,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            RightRecordListPane(
                                records = periodRecords,
                                period = selectedPeriod,
                                onDeleteRecord = { periodToDelete = it },
                                onOpenDetail = { periodForDetail = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm Period Delete Dialog
    if (periodToDelete != null) {
        val target = periodToDelete!!
        AlertDialog(
            onDismissRequest = { periodToDelete = null },
            title = { Text("Hapus Record Transaksi?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Semua ${target.transactionCount} transaksi pada periode ${target.fullPeriodTitle} " +
                        "(total ${BluetoothPrinterManager.formatRupiah(target.totalRevenue)}) akan dihapus dari riwayat lokal."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePeriod(target)
                        periodToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_period")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { periodToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirm Single Nota Delete Dialog (from Today Per-Nota list)
    if (singleNotaToDelete != null) {
        val trx = singleNotaToDelete!!
        AlertDialog(
            onDismissRequest = { singleNotaToDelete = null },
            title = { Text("Hapus Nota ${trx.invoiceNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Transaksi nota ${trx.invoiceNumber} senilai " +
                        "${BluetoothPrinterManager.formatRupiah(trx.grandTotal)} akan dihapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSingleTransaction(trx)
                        singleNotaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_nota")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleNotaToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirm Single Expense Delete Dialog (from Laporan Pengeluaran)
    if (singleExpenseToDelete != null && onDeleteExpense != null) {
        val exp = singleExpenseToDelete!!
        AlertDialog(
            onDismissRequest = { singleExpenseToDelete = null },
            title = { Text("Hapus Pengeluaran ${exp.title}?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Catatan pengeluaran senilai ${BluetoothPrinterManager.formatRupiah(exp.amount)} akan dihapus dan disinkronkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(exp)
                        singleExpenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_record_expense")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleExpenseToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Period Transaction Breakdown Detail Modal
    if (periodForDetail != null) {
        val activePeriod = periodRecords.find { it.periodKey == periodForDetail!!.periodKey }
            ?: periodForDetail!!
        TransactionPeriodDetailDialog(
            record = activePeriod,
            onDismiss = { periodForDetail = null },
            onSelectTransaction = { trx ->
                periodForDetail = null
                onOpenReceipt(trx)
            },
            onDeleteTransaction = { trx ->
                onDeleteSingleTransaction(trx)
            },
            onDeleteExpense = onDeleteExpense
        )
    }
}

@Composable
private fun LeftChartPane(
    selectedMetric: TransactionChartMetric,
    onSelectMetric: (TransactionChartMetric) -> Unit,
    chartSeries: TransactionChartSeries,
    storeName: String,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 2x2 Metric Control: Jml Transaksi | Pendapatan / Keuntungan | Pengeluaran
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SEGMENT_INACTIVE_BG),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            listOf(
                listOf(TransactionChartMetric.COUNT, TransactionChartMetric.REVENUE),
                listOf(TransactionChartMetric.PROFIT, TransactionChartMetric.EXPENSE)
            ).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        val isSelected = selectedMetric == metric
                        val activeBg = if (metric == TransactionChartMetric.EXPENSE) Color(0xFFBE123C) else EMERALD_DARK
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .background(if (isSelected) activeBg else SEGMENT_INACTIVE_BG)
                                .clickable { onSelectMetric(metric) }
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                                .testTag("segment_metric_${metric.name.lowercase()}")
                        ) {
                            Text(
                                text = metric.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Golden-Yellow Area Line Chart
        TransactionAreaLineChart(
            series = chartSeries,
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .testTag("canvas_transaction_area_chart")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Footer: Store Badge (left) + Share / Bagikan (right)
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
                    shape = RoundedCornerShape(6.dp),
                    color = EMERALD_PRIMARY,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = storeName.ifBlank { "Kasir POS" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EMERALD_PRIMARY,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onShare)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .heightIn(min = 36.dp)
                    .testTag("btn_share_transaction_record")
            ) {
                Surface(
                    shape = CircleShape,
                    color = EMERALD_PRIMARY,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan Laporan",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bagikan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EMERALD_PRIMARY
                )
            }
        }
    }
}

@Composable
private fun TransactionAreaLineChart(
    series: TransactionChartSeries,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val leftPad = 18.dp.toPx()
        val rightPad = 18.dp.toPx()
        val topPad = 24.dp.toPx()
        val bottomPad = 24.dp.toPx()

        val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
        val baseY = topPad + chartH
        val axisColor = Color(0xFF94A3B8)

        // Left & Right framing vertical axis lines + Bottom horizontal axis line (matching screenshot)
        drawLine(
            color = axisColor,
            start = Offset(leftPad, topPad),
            end = Offset(leftPad, baseY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = axisColor,
            start = Offset(leftPad, baseY),
            end = Offset(leftPad + chartW, baseY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = axisColor,
            start = Offset(leftPad + chartW, topPad),
            end = Offset(leftPad + chartW, baseY),
            strokeWidth = 1.dp.toPx()
        )

        // X-Axis ticks & labels
        val safeMaxX = series.maxX.coerceAtLeast(1f)
        series.xTicks.forEach { tick ->
            val xPx = leftPad + (tick.x / safeMaxX).coerceIn(0f, 1f) * chartW
            val measured = textMeasurer.measure(
                text = AnnotatedString(tick.label),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (xPx - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    y = baseY + 5.dp.toPx()
                )
            )
        }

        if (series.points.isEmpty()) return@Canvas

        val safeMaxY = series.maxY.coerceAtLeast(1.0)
        val coords = series.points.map { pt ->
            val xPx = leftPad + (pt.x / safeMaxX).coerceIn(0f, 1f) * chartW
            val ratioY = (pt.value / safeMaxY).toFloat().coerceIn(0f, 1f)
            val yPx = baseY - (ratioY * (chartH - 6.dp.toPx()))
            Triple(Offset(xPx, yPx), pt.label, pt.value)
        }

        // Gradient Area Fill beneath the slope
        if (coords.size >= 2) {
            val fillPath = Path().apply {
                moveTo(coords.first().first.x, baseY)
                coords.forEach { (offset, _, _) ->
                    lineTo(offset.x, offset.y)
                }
                lineTo(coords.last().first.x, baseY)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CHART_AMBER.copy(alpha = 0.65f),
                        CHART_GOLD.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = topPad,
                    endY = baseY
                )
            )

            val linePath = Path().apply {
                moveTo(coords.first().first.x, coords.first().first.y)
                coords.drop(1).forEach { (offset, _, _) ->
                    lineTo(offset.x, offset.y)
                }
            }
            drawPath(
                path = linePath,
                color = CHART_GOLD,
                style = Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw hollow-center yellow-gold node circles + value label above each node
        val outerRadius = 5.5.dp.toPx()
        val innerRadius = 2.6.dp.toPx()

        coords.forEach { (center, labelText, _) ->
            drawCircle(color = CHART_GOLD, radius = outerRadius, center = center)
            drawCircle(color = Color.White, radius = innerRadius, center = center)

            val measuredValue = textMeasurer.measure(
                text = AnnotatedString(labelText),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Medium
                )
            )
            val labelX = (center.x - measuredValue.size.width / 2f)
                .coerceIn(2f, (size.width - measuredValue.size.width - 2f).coerceAtLeast(2f))
            val labelY = (center.y - outerRadius - measuredValue.size.height - 3.dp.toPx())
                .coerceAtLeast(0f)

            drawText(
                textLayoutResult = measuredValue,
                topLeft = Offset(labelX, labelY)
            )
        }
    }
}

@Composable
private fun RightRecordListPane(
    records: List<TransactionPeriodRecord>,
    period: TransactionRecordPeriod,
    onDeleteRecord: (TransactionPeriodRecord) -> Unit,
    onOpenDetail: (TransactionPeriodRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Belum ada record transaksi ${period.chipLabel.lowercase(Locale.getDefault())}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var showAllRecords by remember(period) { mutableStateOf(false) }
    val visibleRecords = if (showAllRecords || records.size <= 15) records else records.take(15)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        visibleRecords.forEach { record ->
            TransactionPeriodRecordRow(
                record = record,
                onDeleteClick = { onDeleteRecord(record) },
                onDetailClick = { onOpenDetail(record) }
            )
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        }
        if (records.size > 15) {
            TextButton(
                onClick = { showAllRecords = !showAllRecords },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = if (showAllRecords) "Tampilkan Lebih Sedikit" else "Tampilkan Semua (${records.size} Record)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EMERALD_PRIMARY
                )
            }
        }
    }
}

@Composable
private fun RightPerNotaListPane(
    transactions: List<Transaction>,
    onDeleteTransaction: (Transaction) -> Unit,
    onOpenReceipt: (Transaction) -> Unit,
    selectedDateMillis: Long? = null,
    expenses: List<Expense> = emptyList(),
    onDeleteExpense: ((Expense) -> Unit)? = null,
    onAddExpenseClick: (() -> Unit)? = null,
    isExpenseSyncing: Boolean = false,
    expenseSyncStatus: String = "Otomatis tersinkron ke Laporan Web Chrome",
    onSyncExpensesNow: (() -> Unit)? = null,
    onOpenWebChromeExpenses: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }
    val todayStart = PosViewModel.getStartOfDay(0)
    val isToday = selectedDateMillis == null || isSameLocalDay(selectedDateMillis, todayStart)
    val dateLabel = remember(selectedDateMillis) {
        selectedDateMillis?.let { formatKpDateShort(it) } ?: "Hari Ini"
    }

    val totalRevenue = remember(transactions) { transactions.sumOf { it.grandTotal } }
    val totalProfit = remember(transactions) { transactions.sumOf { it.profitAmount } }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }
    val netProfit = totalProfit - totalExpense

    var showAllNotas by remember(transactions.size) { mutableStateOf(false) }
    val visibleNotas = if (showAllNotas || transactions.size <= 25) transactions else transactions.take(25)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (transactions.isNotEmpty() || expenses.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("summary_per_nota_finance"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pendapatan", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(
                            text = BluetoothPrinterManager.formatRupiah(totalRevenue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Laba Kotor", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(
                            text = BluetoothPrinterManager.formatRupiah(totalProfit),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EMERALD_PRIMARY,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pengeluaran", fontSize = 10.sp, color = Color(0xFF9F1239))
                        Text(
                            text = "-${BluetoothPrinterManager.formatRupiah(totalExpense)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBE123C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Laba Bersih", fontSize = 10.sp, color = Color(0xFF475569))
                        Text(
                            text = BluetoothPrinterManager.formatRupiah(netProfit),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netProfit >= 0) EMERALD_PRIMARY else Color(0xFFDC2626),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isToday) "Belum ada transaksi nota hari ini" else "Belum ada transaksi nota pada $dateLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0FDFA))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isToday) "Catatan Transaksi per Nota Hari Ini" else "Catatan Transaksi per Nota ($dateLabel)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EMERALD_PRIMARY
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = EMERALD_PRIMARY.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${transactions.size} Nota",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EMERALD_PRIMARY,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            visibleNotas.forEach { trx ->
                TransactionNotaRecordRow(
                    transaction = trx,
                    timeText = timeFmt.format(Date(trx.timestamp)),
                    onDeleteClick = { onDeleteTransaction(trx) },
                    onDetailClick = { onOpenReceipt(trx) }
                )
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }

            if (transactions.size > 25) {
                TextButton(
                    onClick = { showAllNotas = !showAllNotas },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (showAllNotas) "Tampilkan Lebih Sedikit" else "Tampilkan Semua (${transactions.size} Nota)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EMERALD_PRIMARY
                    )
                }
            }
        }

        // Laporan Pengeluaran section inside Per-Nota Laporan Transaksi
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("section_per_nota_expenses")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF1F2))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Laporan Pengeluaran ($dateLabel)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBE123C)
                    )
                    Text(
                        text = "${expenses.size} catatan • -${BluetoothPrinterManager.formatRupiah(totalExpense)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9F1239)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onOpenWebChromeExpenses != null) {
                        IconButton(
                            onClick = onOpenWebChromeExpenses,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_open_web_expense_per_nota")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Buka Pengeluaran di Web Chrome",
                                tint = EMERALD_PRIMARY,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    if (onAddExpenseClick != null) {
                        OutlinedButton(
                            onClick = onAddExpenseClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_add_expense_per_nota")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Catat Pengeluaran",
                                tint = Color(0xFFBE123C),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catat",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBE123C)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFFFE4E6)
                        ) {
                            Text(
                                text = "-${BluetoothPrinterManager.formatRupiah(totalExpense)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBE123C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isExpenseSyncing) Color(0xFFFFFBEB) else Color(0xFFF0FDFA))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("badge_per_nota_expense_autosync"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpenseSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                        contentDescription = "Auto-Sync Pengeluaran",
                        tint = if (isExpenseSyncing) Color(0xFFD97706) else EMERALD_PRIMARY,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isExpenseSyncing) "Menyinkronkan pengeluaran ke Web Chrome..." else expenseSyncStatus,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isExpenseSyncing) Color(0xFFB45309) else EMERALD_PRIMARY,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onSyncExpensesNow != null) {
                    TextButton(
                        onClick = onSyncExpensesNow,
                        enabled = !isExpenseSyncing,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_sync_per_nota_expense_now")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sinkronkan Sekarang",
                            tint = EMERALD_PRIMARY,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Sinkron",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EMERALD_PRIMARY
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFFECDD3), thickness = 1.dp)

            if (expenses.isEmpty()) {
                Text(
                    text = "Belum ada pengeluaran tercatat pada $dateLabel",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            } else {
                expenses.forEach { exp ->
                    TransactionExpenseItemRow(
                        expense = exp,
                        timeLabel = timeFmt.format(Date(exp.timestamp)),
                        onDeleteClick = if (onDeleteExpense != null) ({ onDeleteExpense(exp) }) else null
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun TransactionNotaRecordRow(
    transaction: Transaction,
    timeText: String,
    onDeleteClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("row_record_nota_${transaction.invoiceNumber}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Nota number, timestamp, payment method & item count
        Column(modifier = Modifier.width(122.dp)) {
            Text(
                text = "#${transaction.invoiceNumber}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$timeText • ${transaction.paymentMethod.label}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = EMERALD_PRIMARY,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${transaction.totalItemsCount} item • ${transaction.customerName}",
                fontSize = 11.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Middle Column: Pendapatan & Keuntungan per nota
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pendapatan",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF475569)
            )
            Text(
                text = BluetoothPrinterManager.formatRupiah(transaction.grandTotal),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keuntungan",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF475569)
            )
            Text(
                text = BluetoothPrinterManager.formatRupiah(transaction.profitAmount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        // Right Column: Delete Trash (Red) + View Struk Arrow (Teal)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_delete_nota_${transaction.invoiceNumber}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Hapus Nota ${transaction.invoiceNumber}",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = onDetailClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_detail_nota_${transaction.invoiceNumber}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Lihat Nota ${transaction.invoiceNumber}",
                    tint = EMERALD_PRIMARY,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionPeriodRecordRow(
    record: TransactionPeriodRecord,
    onDeleteClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("row_record_period_${record.periodKey}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Date/Month/Year + transaction & expense count
        Column(modifier = Modifier.width(114.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.primaryLabel,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = record.secondaryLabel,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${record.transactionCount} transaksi",
                fontSize = 12.5.sp,
                color = Color(0xFF64748B)
            )
            if (record.expenseCount > 0) {
                Text(
                    text = "${record.expenseCount} pengeluaran",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBE123C)
                )
            }
        }

        // Middle Column: Pendapatan, Keuntungan, Pengeluaran & Laba Bersih
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pendapatan",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF475569)
            )
            Text(
                text = BluetoothPrinterManager.formatRupiah(record.totalRevenue),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keuntungan",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF475569)
            )
            Text(
                text = BluetoothPrinterManager.formatRupiah(record.totalProfit),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = EMERALD_PRIMARY
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pengeluaran",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF9F1239)
            )
            Text(
                text = "-${BluetoothPrinterManager.formatRupiah(record.totalExpense)}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBE123C)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Laba Bersih",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF475569)
            )
            Text(
                text = BluetoothPrinterManager.formatRupiah(record.netProfit),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (record.netProfit >= 0) EMERALD_PRIMARY else Color(0xFFDC2626)
            )
        }

        // Right Column: Delete Trash (Red) + Detail Arrow (Teal)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_delete_period_${record.periodKey}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Hapus Record ${record.fullPeriodTitle}",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = onDetailClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_detail_period_${record.periodKey}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Lihat Detail ${record.fullPeriodTitle}",
                    tint = EMERALD_PRIMARY,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionPeriodDetailDialog(
    record: TransactionPeriodRecord,
    onDismiss: () -> Unit,
    onSelectTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onDeleteExpense: ((Expense) -> Unit)? = null
) {
    val timeFmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")) }
    var pendingDeleteTrx by remember { mutableStateOf<Transaction?>(null) }
    var pendingDeleteExpense by remember { mutableStateOf<Expense?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detail Laporan • ${record.fullPeriodTitle}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EMERALD_PRIMARY
                        )
                        Text(
                            text = "${record.transactionCount} transaksi • Omset ${BluetoothPrinterManager.formatRupiah(record.totalRevenue)}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Pengeluaran (${record.expenseCount}): -${BluetoothPrinterManager.formatRupiah(record.totalExpense)} • Bersih: ${BluetoothPrinterManager.formatRupiah(record.netProfit)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBE123C)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (record.transactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Daftar Transaksi (${record.transactions.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EMERALD_PRIMARY
                            )
                        }
                        items(record.transactions, key = { "trx_${it.id}" }) { trx ->
                            Surface(
                                onClick = { onSelectTransaction(trx) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${trx.invoiceNumber} • ${trx.customerName}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${timeFmt.format(Date(trx.timestamp))} • ${trx.paymentMethod.label} • ${trx.cashierName}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "Omset: ${BluetoothPrinterManager.formatRupiah(trx.grandTotal)} | Laba: ${BluetoothPrinterManager.formatRupiah(trx.profitAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EMERALD_PRIMARY
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingDeleteTrx = trx },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Hapus Transaksi ${trx.invoiceNumber}",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (record.expenses.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Laporan Pengeluaran (${record.expenses.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBE123C)
                            )
                        }
                        items(record.expenses, key = { "exp_${it.id}" }) { exp ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF1F2),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${exp.title} • ${exp.category.ifBlank { "Operasional" }}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF881337)
                                        )
                                        Text(
                                            text = "${timeFmt.format(Date(exp.timestamp))} • Oleh: ${exp.createdBy.ifBlank { "Admin" }}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        if (exp.notes.isNotBlank()) {
                                            Text(
                                                text = exp.notes,
                                                fontSize = 10.5.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        Text(
                                            text = "- ${BluetoothPrinterManager.formatRupiah(exp.amount)}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFBE123C)
                                        )
                                    }
                                    if (onDeleteExpense != null) {
                                        IconButton(
                                            onClick = { pendingDeleteExpense = exp },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Hapus Pengeluaran ${exp.title}",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteTrx != null) {
        val trx = pendingDeleteTrx!!
        AlertDialog(
            onDismissRequest = { pendingDeleteTrx = null },
            title = { Text("Hapus Transaksi ${trx.invoiceNumber}?") },
            text = { Text("Transaksi senilai ${BluetoothPrinterManager.formatRupiah(trx.grandTotal)} akan dihapus dari record.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(trx)
                        pendingDeleteTrx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTrx = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (pendingDeleteExpense != null && onDeleteExpense != null) {
        val exp = pendingDeleteExpense!!
        AlertDialog(
            onDismissRequest = { pendingDeleteExpense = null },
            title = { Text("Hapus Pengeluaran ${exp.title}?") },
            text = { Text("Pengeluaran senilai ${BluetoothPrinterManager.formatRupiah(exp.amount)} akan dihapus dan disinkronkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(exp)
                        pendingDeleteExpense = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteExpense = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
