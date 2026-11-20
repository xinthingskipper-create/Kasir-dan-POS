package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.service.PosNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PosNotificationHelper.resetTracking()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `verify OrderType labels`() {
        assertEquals("Makan di Tempat", com.example.data.model.OrderType.DINE_IN.label)
        assertEquals("Di Bungkus", com.example.data.model.OrderType.TAKE_AWAY.label)
    }

    @Test
    fun `verify CloudCategoryTab labels`() {
        assertEquals("Source Code & Panduan Web", com.example.ui.screens.CloudCategoryTab.SOURCE_CODE.label)
        assertEquals("Semua", com.example.ui.screens.CloudCategoryTab.SEMUA.label)
    }

    @Test
    fun `verify SourceCodeSubTab enums`() {
        assertEquals("database.rules.json", com.example.ui.screens.SourceCodeSubTab.RTDB_RULES.fileName)
        assertEquals("realtime_database_schema.json", com.example.ui.screens.SourceCodeSubTab.REPORT_TABLES.fileName)
    }

    @Test
    fun `verify notification channels and foreground service notification`() {
        PosNotificationHelper.ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertNotNull(nm.getNotificationChannel(PosNotificationHelper.CHANNEL_LOW_STOCK_ID))
            assertNotNull(nm.getNotificationChannel(PosNotificationHelper.CHANNEL_BACKGROUND_ID))
        }

        val fgNotification = PosNotificationHelper.buildForegroundServiceNotification(
            context = context,
            storeName = "Warung Maju",
            lowStockCount = 2,
            outOfStockCount = 1
        )
        assertNotNull(fgNotification)
        assertTrue((fgNotification.actions?.size ?: 0) >= 2)
    }

    @Test
    fun `verify low stock notification alerts deduplication and restock reset`() {
        val foodLow = Product(
            id = 101L,
            name = "Nasi Goreng Spesial",
            category = "Makanan",
            buyPrice = 12000.0,
            sellPrice = 22000.0,
            stock = 3,
            minStockAlert = 5
        )
        val drinkLow = Product(
            id = 102L,
            name = "Matcha Latte Ice",
            category = "Minuman",
            buyPrice = 10000.0,
            sellPrice = 18000.0,
            stock = 0,
            minStockAlert = 8
        )
        val safeItem = Product(
            id = 103L,
            name = "Es Teh Manis",
            category = "Minuman",
            buyPrice = 3000.0,
            sellPrice = 7000.0,
            stock = 20,
            minStockAlert = 5
        )

        val products = listOf(foodLow, drinkLow, safeItem)

        // First trigger notifies for both critical items
        val firstSent = PosNotificationHelper.notifyLowStockIfNeeded(context, "Warung Maju", products, forceAlert = false)
        assertEquals(2, firstSent)

        // Same stock levels without forceAlert should not spam
        val duplicateSent = PosNotificationHelper.notifyLowStockIfNeeded(context, "Warung Maju", products, forceAlert = false)
        assertEquals(0, duplicateSent)

        // Decreasing stock further triggers again
        val droppedProducts = listOf(foodLow.copy(stock = 2), drinkLow, safeItem)
        val droppedSent = PosNotificationHelper.notifyLowStockIfNeeded(context, "Warung Maju", droppedProducts, forceAlert = false)
        assertEquals(1, droppedSent)

        // Force alert always notifies critical products
        val forcedSent = PosNotificationHelper.notifyLowStockIfNeeded(context, "Warung Maju", droppedProducts, forceAlert = true)
        assertEquals(2, forcedSent)

        // Restocking clears tracking; dropping again re-alerts
        val restocked = listOf(foodLow.copy(stock = 15), drinkLow.copy(stock = 15), safeItem)
        assertEquals(0, PosNotificationHelper.notifyLowStockIfNeeded(context, "Warung Maju", restocked, forceAlert = false))

        val reAlert = PosNotificationHelper.notifyLowStockIfNeeded(context, "Warung Maju", listOf(foodLow.copy(stock = 4), safeItem), forceAlert = false)
        assertEquals(1, reAlert)
    }

    @Test
    fun `verify web report template includes daily auto-reset and daily recording`() {
        val html = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        assertTrue(html.contains("activeDateFilter"))
        assertTrue(html.contains("resetToTodayFilter"))
        assertTrue(html.contains("lastKnownDate"))
        assertTrue(html.contains("extractDateStr"))
        assertTrue(html.contains("Pendapatan Kotor"))
        assertTrue(html.contains("Pengeluaran"))
        assertTrue(html.contains("Catatan Pengeluaran"))
        assertTrue(html.contains("Kas Fisik di Laci"))
        assertTrue(html.contains("Non-Tunai"))
        assertTrue(html.contains("fetchNodeMerged(\"expenses\")"))
        assertTrue(html.contains("/api/sync/"))
        assertTrue(html.contains(".json"))
        assertTrue(html.contains("archivedByTutupToko"))
        assertTrue(!html.contains("dailyTrxAgg"))
    }

    @Test
    fun `verify report custom date selection and previous day stepping`() {
        val viewModel = com.example.ui.PosViewModel(context)

        // Default is TODAY
        assertEquals(com.example.ui.DateFilter.TODAY, viewModel.selectedDateFilter.value)
        val todayStart = com.example.ui.PosViewModel.getStartOfDay(0)
        assertEquals(todayStart, viewModel.selectedCustomDateMillis.value)

        // Step -1 day -> YESTERDAY
        viewModel.stepSelectedDate(-1)
        assertEquals(com.example.ui.DateFilter.YESTERDAY, viewModel.selectedDateFilter.value)
        assertEquals(com.example.ui.PosViewModel.getStartOfDay(-1), viewModel.selectedCustomDateMillis.value)

        // Step -1 day again (2 days ago) -> CUSTOM_DATE
        viewModel.stepSelectedDate(-1)
        assertEquals(com.example.ui.DateFilter.CUSTOM_DATE, viewModel.selectedDateFilter.value)
        assertEquals(com.example.ui.PosViewModel.getStartOfDay(-2), viewModel.selectedCustomDateMillis.value)

        // Step +1 day -> back to YESTERDAY, then +1 -> TODAY, and +1 again clamps at TODAY
        viewModel.stepSelectedDate(1)
        assertEquals(com.example.ui.DateFilter.YESTERDAY, viewModel.selectedDateFilter.value)
        viewModel.stepSelectedDate(1)
        assertEquals(com.example.ui.DateFilter.TODAY, viewModel.selectedDateFilter.value)
        viewModel.stepSelectedDate(1)
        assertEquals(com.example.ui.DateFilter.TODAY, viewModel.selectedDateFilter.value)
        assertEquals(todayStart, viewModel.selectedCustomDateMillis.value)

        // Verify Material DatePicker UTC millis converts accurately to local start of day
        val fiveDaysAgoLocal = com.example.ui.PosViewModel.getStartOfDay(-5)
        val utcPickerMillis = com.example.ui.PosViewModel.localStartOfDayToUtcPickerMillis(fiveDaysAgoLocal)
        viewModel.setCustomDate(utcPickerMillis, isUtcPickerMillis = true)
        assertEquals(com.example.ui.DateFilter.CUSTOM_DATE, viewModel.selectedDateFilter.value)
        assertEquals(fiveDaysAgoLocal, viewModel.selectedCustomDateMillis.value)
    }

    @Test
    fun `verify store closing notification channel and posted notification`() {
        val sentManual = PosNotificationHelper.notifyStoreClosed(
            context = context,
            storeName = "Warung Maju",
            totalRevenue = 450000.0,
            totalTransactions = 12,
            totalCash = 300000.0,
            totalNonCash = 150000.0,
            cashierName = "Budi",
            isAutomatic = false,
            cloudSynced = true
        )
        assertTrue(sentManual)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertNotNull(nm.getNotificationChannel(PosNotificationHelper.CHANNEL_STORE_CLOSING_ID))
        }

        val postedManual = Shadows.shadowOf(nm).getNotification(PosNotificationHelper.NOTIFICATION_ID_STORE_CLOSING)
        assertNotNull(postedManual)
        assertTrue(postedManual.extras.getString(android.app.Notification.EXTRA_TITLE)?.contains("Tutup Toko Berhasil") == true)

        val sentAuto = PosNotificationHelper.notifyStoreClosed(
            context = context,
            storeName = "Warung Maju",
            totalRevenue = 620000.0,
            totalTransactions = 18,
            totalCash = 400000.0,
            totalNonCash = 220000.0,
            cashierName = "Tutup Toko Otomatis (22:00)",
            isAutomatic = true,
            cloudSynced = true
        )
        assertTrue(sentAuto)
        val postedAuto = Shadows.shadowOf(nm).getNotification(PosNotificationHelper.NOTIFICATION_ID_STORE_CLOSING)
        assertTrue(postedAuto.extras.getString(android.app.Notification.EXTRA_TITLE)?.contains("Tutup Toko Otomatis") == true)
    }

    @Test
    fun `verify scheduled auto store close rules and next alarm calculation`() {
        val cal10Am = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 13, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cal2159 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 13, 21, 59, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cal2200 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 13, 22, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cal2230 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 13, 22, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Before 22:00 -> should not trigger yet
        assertTrue(
            !com.example.service.AutoStoreCloseManager.shouldTriggerAutoCloseNow(
                enabled = true,
                hour = 22,
                minute = 0,
                configuredAtMillis = cal10Am,
                lastAutoCloseDate = "",
                nowMillis = cal2159
            )
        )

        // Exactly at 22:00 (configured earlier at 10:00 AM) -> triggers
        assertTrue(
            com.example.service.AutoStoreCloseManager.shouldTriggerAutoCloseNow(
                enabled = true,
                hour = 22,
                minute = 0,
                configuredAtMillis = cal10Am,
                lastAutoCloseDate = "",
                nowMillis = cal2200
            )
        )

        // Already triggered on 2026-11-13 -> deduplicated
        assertTrue(
            !com.example.service.AutoStoreCloseManager.shouldTriggerAutoCloseNow(
                enabled = true,
                hour = 22,
                minute = 0,
                configuredAtMillis = cal10Am,
                lastAutoCloseDate = "2026-11-13",
                nowMillis = cal2230
            )
        )

        // Configured AFTER 22:00 already passed -> waits for tomorrow
        assertTrue(
            !com.example.service.AutoStoreCloseManager.shouldTriggerAutoCloseNow(
                enabled = true,
                hour = 22,
                minute = 0,
                configuredAtMillis = cal2230,
                lastAutoCloseDate = "",
                nowMillis = cal2230 + 60_000L
            )
        )

        // Next alarm at 21:59 is today 22:00; next alarm at 22:00 is tomorrow 22:00
        assertEquals(cal2200, com.example.service.AutoStoreCloseManager.computeNextAlarmTriggerMillis(cal2159, 22, 0))
        val tomorrow2200 = cal2200 + 24L * 60L * 60L * 1000L
        assertEquals(tomorrow2200, com.example.service.AutoStoreCloseManager.computeNextAlarmTriggerMillis(cal2200, 22, 0))
    }

    @Test
    fun `verify ViewModel auto store close schedule persistence`() {
        val viewModel = com.example.ui.PosViewModel(context)
        viewModel.setAutoCloseSchedule(enabled = true, hour = 21, minute = 45)
        assertTrue(viewModel.isAutoCloseEnabled.value)
        assertEquals(21, viewModel.autoCloseHour.value)
        assertEquals(45, viewModel.autoCloseMinute.value)

        viewModel.setAutoCloseSchedule(enabled = false, hour = 21, minute = 45)
        assertTrue(!viewModel.isAutoCloseEnabled.value)
    }

    @Test
    fun `verify daily monthly and yearly transaction record aggregation and chart series`() {
        fun makeTrx(id: Long, year: Int, monthZero: Int, day: Int, revenue: Double, profit: Double): com.example.data.model.Transaction {
            val ts = java.util.Calendar.getInstance().apply {
                set(year, monthZero, day, 14, 30, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            return com.example.data.model.Transaction(
                id = id,
                invoiceNumber = "INV-$id",
                cashierId = 1L,
                cashierName = "Admin",
                timestamp = ts,
                subtotal = revenue,
                grandTotal = revenue,
                amountPaid = revenue,
                profitAmount = profit,
                totalItemsCount = 1
            )
        }

        val trxNov14 = makeTrx(1L, 2026, java.util.Calendar.NOVEMBER, 14, 15000.0, 2000.0)
        val trxNov10 = makeTrx(2L, 2026, java.util.Calendar.NOVEMBER, 10, 35000.0, 10000.0)
        val trxOct20 = makeTrx(3L, 2026, java.util.Calendar.OCTOBER, 20, 50000.0, 15000.0)
        val trx2025 = makeTrx(4L, 2025, java.util.Calendar.DECEMBER, 5, 100000.0, 30000.0)

        // Single-day record chart matches photo baseline (0,00 -> 1,00 on day 14)
        val singleDayRecords = com.example.ui.screens.aggregateTransactionRecords(
            listOf(trxNov14),
            com.example.ui.screens.TransactionRecordPeriod.DAILY
        )
        assertEquals(1, singleDayRecords.size)
        assertEquals("14", singleDayRecords.first().primaryLabel)
        assertEquals("Nov\n2026", singleDayRecords.first().secondaryLabel)
        assertEquals(15000.0, singleDayRecords.first().totalRevenue, 0.01)
        assertEquals(2000.0, singleDayRecords.first().totalProfit, 0.01)

        val countSeries = com.example.ui.screens.buildTransactionChartSeries(
            singleDayRecords,
            com.example.ui.screens.TransactionRecordPeriod.DAILY,
            com.example.ui.screens.TransactionChartMetric.COUNT
        )
        assertEquals(2, countSeries.points.size)
        assertEquals("0,00", countSeries.points[0].label)
        assertEquals("1,00", countSeries.points[1].label)
        assertEquals(14f, countSeries.points[1].x)
        assertEquals(15f, countSeries.maxX)

        val revSeries = com.example.ui.screens.buildTransactionChartSeries(
            singleDayRecords,
            com.example.ui.screens.TransactionRecordPeriod.DAILY,
            com.example.ui.screens.TransactionChartMetric.REVENUE
        )
        assertEquals("15rb", revSeries.points[1].label)

        // Multi-period aggregation: Daily (4), Monthly (3), Yearly (2)
        val all = listOf(trxNov14, trxNov10, trxOct20, trx2025)
        val daily = com.example.ui.screens.aggregateTransactionRecords(all, com.example.ui.screens.TransactionRecordPeriod.DAILY)
        val monthly = com.example.ui.screens.aggregateTransactionRecords(all, com.example.ui.screens.TransactionRecordPeriod.MONTHLY)
        val yearly = com.example.ui.screens.aggregateTransactionRecords(all, com.example.ui.screens.TransactionRecordPeriod.YEARLY)

        assertEquals(4, daily.size)
        assertEquals(3, monthly.size)
        assertEquals("Nov", monthly[0].primaryLabel)
        assertEquals(2, monthly[0].transactionCount)
        assertEquals(50000.0, monthly[0].totalRevenue, 0.01)
        assertEquals(12000.0, monthly[0].totalProfit, 0.01)

        assertEquals(2, yearly.size)
        assertEquals("2026", yearly[0].primaryLabel)
        assertEquals(3, yearly[0].transactionCount)
        assertEquals(100000.0, yearly[0].totalRevenue, 0.01)
        assertEquals(27000.0, yearly[0].totalProfit, 0.01)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify TransactionRecordReportSection renders inside LazyColumn in portrait without crash`() {
        val sampleTransactions = (1..20).map { idx ->
            val ts = java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.NOVEMBER, (idx % 20) + 1, 12, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            com.example.data.model.Transaction(
                id = idx.toLong(),
                invoiceNumber = "INV-$idx",
                cashierId = 1L,
                cashierName = "Admin",
                timestamp = ts,
                subtotal = 15000.0,
                grandTotal = 15000.0,
                amountPaid = 15000.0,
                profitAmount = 2000.0,
                totalItemsCount = 1
            )
        }

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = sampleTransactions,
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Maju"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_transaction_record_report").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_record_report_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_period_2026-11-14").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w800dp-h900dp")
    fun `verify TransactionRecordReportSection renders inside LazyColumn in wide mode without crash`() {
        val sampleTransactions = (1..20).map { idx ->
            val ts = java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.NOVEMBER, (idx % 20) + 1, 12, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            com.example.data.model.Transaction(
                id = idx.toLong(),
                invoiceNumber = "INV-$idx",
                cashierId = 1L,
                cashierName = "Admin",
                timestamp = ts,
                subtotal = 15000.0,
                grandTotal = 15000.0,
                amountPaid = 15000.0,
                profitAmount = 2000.0,
                totalItemsCount = 1
            )
        }

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = sampleTransactions,
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Maju"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_transaction_record_report").assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_period_2026-11-14").assertExists()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify TransactionRecordReportSection portrait LazyColumn tab and metric switching`() {
        val ts = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 14, 14, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trx = com.example.data.model.Transaction(
            id = 1L,
            invoiceNumber = "INV-1",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = ts,
            subtotal = 15000.0,
            grandTotal = 15000.0,
            amountPaid = 15000.0,
            profitAmount = 2000.0,
            totalItemsCount = 1
        )

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = listOf(trx),
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Maju"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("text_record_report_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_period_2026-11-14").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_record_period_monthly").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_period_2026-11").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_record_period_yearly").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_period_2026").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("segment_metric_revenue").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("segment_metric_profit").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify portrait sliding left navigation toolbar auto-hides on scroll and reveals via handle or toggle`() {
        var activeTab by mutableStateOf(com.example.ui.MainTab.POS)
        val adminTabs = listOf(
            com.example.ui.MainTab.POS,
            com.example.ui.MainTab.INVENTORY,
            com.example.ui.MainTab.REPORT,
            com.example.ui.MainTab.USERS,
            com.example.ui.MainTab.PRINTER,
            com.example.ui.MainTab.SETTINGS,
            com.example.ui.MainTab.CLOUD_HOSTING
        )

        composeTestRule.setContent {
            com.example.ui.screens.AdaptiveSlideNavigationContainer(
                tabs = adminTabs,
                currentTab = activeTab,
                storeName = "Warung Maju",
                userFullName = "Admin",
                userRole = com.example.data.model.UserRole.ADMIN,
                connectedPrinter = true,
                autoHideDelayMs = 0L,
                onTabSelected = { activeTab = it },
                onPrinterClick = {},
                onCloudReportClick = {},
                onLogoutClick = {}
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("test_scroll_list")
                ) {
                    items(40) { i ->
                        Text(
                            text = "Item Menu #$i",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        // Initially visible on the LEFT side (no bottom nav bar)
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertIsDisplayed()

        // Scroll down -> left navigation toolbar hides automatically & edge reveal handle appears
        composeTestRule.onNodeWithTag("test_scroll_list").performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertDoesNotExist()
        composeTestRule.onNodeWithTag("floating_nav_reveal_handle").assertIsDisplayed()

        // Swipe right on edge handle -> left toolbar slides back in
        composeTestRule.onNodeWithTag("floating_nav_reveal_handle").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertIsDisplayed()

        // Pin toolbar -> scrolling down keeps left toolbar visible
        composeTestRule.onNodeWithTag("btn_pin_nav_toolbar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_scroll_list").performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertIsDisplayed()

        // Clicking a tab updates currentTab
        composeTestRule.onNodeWithTag("nav_tab_REPORT").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.example.ui.MainTab.REPORT, activeTab)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w780dp-h360dp")
    fun `verify short landscape phone displays scrollable left navigation rail without clipping`() {
        var activeTab by mutableStateOf(com.example.ui.MainTab.POS)
        val adminTabs = listOf(
            com.example.ui.MainTab.POS,
            com.example.ui.MainTab.INVENTORY,
            com.example.ui.MainTab.REPORT,
            com.example.ui.MainTab.USERS,
            com.example.ui.MainTab.PRINTER,
            com.example.ui.MainTab.SETTINGS,
            com.example.ui.MainTab.CLOUD_HOSTING
        )

        composeTestRule.setContent {
            com.example.ui.screens.AdaptiveSlideNavigationContainer(
                tabs = adminTabs,
                currentTab = activeTab,
                storeName = "Warung Maju",
                userFullName = "Admin",
                userRole = com.example.data.model.UserRole.ADMIN,
                connectedPrinter = true,
                autoHideDelayMs = 0L,
                onTabSelected = { activeTab = it },
                onPrinterClick = {},
                onCloudReportClick = {},
                onLogoutClick = {}
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_CLOUD_HOSTING").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.example.ui.MainTab.CLOUD_HOSTING, activeTab)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w800dp-h900dp")
    fun `verify wide sliding navigation rail swipes closed and toggles via topbar button`() {
        var activeTab by mutableStateOf(com.example.ui.MainTab.POS)
        val adminTabs = listOf(
            com.example.ui.MainTab.POS,
            com.example.ui.MainTab.INVENTORY,
            com.example.ui.MainTab.REPORT,
            com.example.ui.MainTab.PRINTER
        )

        composeTestRule.setContent {
            com.example.ui.screens.AdaptiveSlideNavigationContainer(
                tabs = adminTabs,
                currentTab = activeTab,
                storeName = "Warung Maju",
                userFullName = "Admin",
                userRole = com.example.data.model.UserRole.ADMIN,
                connectedPrinter = false,
                autoHideDelayMs = 0L,
                onTabSelected = { activeTab = it },
                onPrinterClick = {},
                onCloudReportClick = {},
                onLogoutClick = {}
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertIsDisplayed()

        // Swipe left on wide rail -> hides rail and shows floating reveal handle
        composeTestRule.onNodeWithTag("sliding_nav_rail").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertDoesNotExist()
        composeTestRule.onNodeWithTag("floating_nav_reveal_handle").assertIsDisplayed()

        // Tap TopAppBar toggle button -> slides rail back in
        composeTestRule.onNodeWithTag("topbar_nav_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sliding_nav_rail").assertIsDisplayed()
    }

    @Test
    fun `verify TransactionReportMenuOption scoping filters transactions accurately`() {
        fun makeTs(year: Int, monthZero: Int, day: Int): Long = java.util.Calendar.getInstance().apply {
            set(year, monthZero, day, 15, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun trx(id: Long, ts: Long) = com.example.data.model.Transaction(
            id = id,
            invoiceNumber = "INV-$id",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = ts,
            subtotal = 20000.0,
            grandTotal = 20000.0,
            amountPaid = 20000.0,
            profitAmount = 5000.0,
            totalItemsCount = 1
        )

        val now = makeTs(2026, java.util.Calendar.NOVEMBER, 15)
        val tToday = trx(1L, now)
        val tSameMonthOtherDay = trx(2L, makeTs(2026, java.util.Calendar.NOVEMBER, 10))
        val tSameYearOtherMonth = trx(3L, makeTs(2026, java.util.Calendar.OCTOBER, 20))
        val tPrevYear = trx(4L, makeTs(2025, java.util.Calendar.DECEMBER, 25))
        val all = listOf(tToday, tSameMonthOtherDay, tSameYearOtherMonth, tPrevYear)

        assertEquals(listOf(1L), com.example.ui.screens.filterTransactionsByMenuOption(all, com.example.ui.screens.TransactionReportMenuOption.TODAY, now).map { it.id })
        assertEquals(listOf(1L, 2L), com.example.ui.screens.filterTransactionsByMenuOption(all, com.example.ui.screens.TransactionReportMenuOption.THIS_MONTH, now).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), com.example.ui.screens.filterTransactionsByMenuOption(all, com.example.ui.screens.TransactionReportMenuOption.THIS_YEAR, now).map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L), com.example.ui.screens.filterTransactionsByMenuOption(all, com.example.ui.screens.TransactionReportMenuOption.ALL, now).map { it.id })
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify Laporan Transaksi 4-option menu navigates to scoped report and back`() {
        var selectedOption by mutableStateOf<com.example.ui.screens.TransactionReportMenuOption?>(null)
        val ts = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 16, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trx = com.example.data.model.Transaction(
            id = 1L,
            invoiceNumber = "INV-1",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = ts,
            subtotal = 15000.0,
            grandTotal = 15000.0,
            amountPaid = 15000.0,
            profitAmount = 2000.0,
            totalItemsCount = 1
        )

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (selectedOption == null) {
                        com.example.ui.screens.TransactionReportMenuCard(
                            onSelectOption = { selectedOption = it },
                            onBackClick = {}
                        )
                    } else {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = listOf(trx),
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Kasir Pintar"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {},
                            initialPeriod = selectedOption!!.defaultPeriod,
                            menuOptionTitle = selectedOption!!.title,
                            onBackToMenu = { selectedOption = null }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("text_report_menu_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_report_today").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_report_this_month").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_report_this_year").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_report_all").assertIsDisplayed()

        // Tap "Laporan transaksi bulan ini" -> enters report screen
        composeTestRule.onNodeWithTag("menu_report_this_month").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_transaction_record_report").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_back_to_report_menu").performClick()
        composeTestRule.waitForIdle()

        // Back at menu
        composeTestRule.onNodeWithTag("card_transaction_report_menu").assertIsDisplayed()
    }

    @Test
    fun `verify online chrome web reports include Kasir Pintar menu and Harian Bulanan Tahunan chart UI`() {
        val requiredSnippets = listOf(
            "Laporan Transaksi",
            "Laporan transaksi hari ini",
            "Laporan transaksi bulan ini",
            "Laporan transaksi tahun ini",
            "Laporan semua transaksi",
            "Laporan Transaksi per Hari",
            "Catatan Transaksi per Nota Hari Ini",
            "daily-per-nota-section",
            "Catatan Transaksi per Nota",
            "kp-today-nota-toolbar",
            "Rincian Pesanan per Nota",
            "reports/daily_transactions",
            "shareDailyPerNota",
            "openKpTodayPerNota",
            "kp-date-picker",
            "kp-menu-date-picker",
            "setKpTargetDate",
            "stepKpTargetDate",
            "resetKpTargetDate",
            "Jml Transaksi",
            "Pendapatan",
            "Keuntungan",
            "Pengeluaran",
            "Laporan pengeluaran",
            "kp-metric-EXPENSE",
            "kp-expense-view",
            "daily-per-nota-expenses-box",
            "Bagikan",
            "#00796B",
            "#EAB308"
        )

        val templateHtml = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        requiredSnippets.forEach { snippet ->
            assertTrue("Template missing: $snippet", templateHtml.contains(snippet))
        }

        val candidateRoots = listOf(java.io.File("."), java.io.File(".."))
        val webRoot = candidateRoots.firstOrNull { java.io.File(it, "web-dashboard/owner.html").exists() }
        assertNotNull("Expected web-dashboard folder to exist in workspace", webRoot)

        listOf(
            "web-dashboard/laporan-online-rtdb.html",
            "web-dashboard/owner.html",
            "web-dashboard/index.html",
            "web-dashboard/rtdb-laporan.html",
            "laporan-online-rtdb.html",
            "owner.html",
            "index.html",
            "rtdb-laporan.html"
        ).forEach { relPath ->
            val file = java.io.File(webRoot, relPath)
            assertTrue("Missing web portal file: $relPath", file.exists())
            val content = file.readText()
            requiredSnippets.forEach { snippet ->
                assertTrue("$relPath missing: $snippet", content.contains(snippet))
            }
            if (relPath.endsWith("laporan-online-rtdb.html")) {
                assertTrue("$relPath missing: window.viewTransactionReceipt", content.contains("window.viewTransactionReceipt"))
            }
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify TODAY report lists each transaction per nota with detail, delete, and chart series`() {
        val ts1 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 9, 15, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val ts2 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 11, 45, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nota1 = com.example.data.model.Transaction(
            id = 101L,
            invoiceNumber = "INV-T1",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = ts1,
            subtotal = 15000.0,
            grandTotal = 15000.0,
            amountPaid = 15000.0,
            profitAmount = 3000.0,
            totalItemsCount = 2
        )
        val nota2 = com.example.data.model.Transaction(
            id = 102L,
            invoiceNumber = "INV-T2",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = ts2,
            subtotal = 25000.0,
            grandTotal = 25000.0,
            amountPaid = 25000.0,
            profitAmount = 6000.0,
            totalItemsCount = 3
        )

        val perNotaCountSeries = com.example.ui.screens.buildPerNotaChartSeries(
            listOf(nota2, nota1),
            com.example.ui.screens.TransactionChartMetric.COUNT
        )
        assertEquals(2, perNotaCountSeries.points.size)
        assertEquals("1,00", perNotaCountSeries.points[0].label)
        assertEquals("2,00", perNotaCountSeries.points[1].label)

        val perNotaRevSeries = com.example.ui.screens.buildPerNotaChartSeries(
            listOf(nota2, nota1),
            com.example.ui.screens.TransactionChartMetric.REVENUE
        )
        assertEquals("15rb", perNotaRevSeries.points[0].label)
        assertEquals("25rb", perNotaRevSeries.points[1].label)

        var openedReceipt: com.example.data.model.Transaction? = null
        var deletedNota: com.example.data.model.Transaction? = null

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = listOf(nota1, nota2),
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Maju"),
                            onDeletePeriod = {},
                            onOpenReceipt = { openedReceipt = it },
                            onDeleteSingleTransaction = { deletedNota = it },
                            initialPeriod = com.example.ui.screens.TransactionRecordPeriod.DAILY,
                            menuOptionTitle = com.example.ui.screens.TransactionReportMenuOption.TODAY.title
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_nota_INV-T2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_nota_INV-T1").performScrollTo().assertIsDisplayed()

        // Tap detail arrow on INV-T1 opens single-nota receipt
        composeTestRule.onNodeWithTag("btn_detail_nota_INV-T1").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("INV-T1", openedReceipt?.invoiceNumber)

        // Tap delete trash on INV-T2 shows confirm dialog and invokes single-nota delete callback
        composeTestRule.onNodeWithTag("btn_delete_nota_INV-T2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_confirm_delete_nota").performClick()
        composeTestRule.waitForIdle()
        assertEquals("INV-T2", deletedNota?.invoiceNumber)

        // Switching to Bulanan still aggregates into monthly period row
        composeTestRule.onNodeWithTag("tab_record_period_monthly").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_period_2026-11").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify Laporan Transaksi date selector navigates to previous date per-nota records and resets to today`() {
        val todayStart = com.example.ui.PosViewModel.getStartOfDay(0)
        val yesterdayStart = com.example.ui.PosViewModel.getStartOfDay(-1)
        var selectedDateMillis by mutableStateOf(todayStart)
        var selectedOption by mutableStateOf<com.example.ui.screens.TransactionReportMenuOption?>(null)

        val todayTrx = com.example.data.model.Transaction(
            id = 201L,
            invoiceNumber = "INV-HARI-INI",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = todayStart + 10 * 3600_000L,
            subtotal = 30000.0,
            grandTotal = 30000.0,
            amountPaid = 30000.0,
            profitAmount = 8000.0,
            totalItemsCount = 2
        )
        val yesterdayTrx = com.example.data.model.Transaction(
            id = 202L,
            invoiceNumber = "INV-KEMARIN",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = yesterdayStart + 14 * 3600_000L,
            subtotal = 45000.0,
            grandTotal = 45000.0,
            amountPaid = 45000.0,
            profitAmount = 12000.0,
            totalItemsCount = 3
        )
        val allHistory = listOf(todayTrx, yesterdayTrx)

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (selectedOption == null) {
                        com.example.ui.screens.TransactionReportMenuCard(
                            onSelectOption = { selectedOption = it },
                            onBackClick = {},
                            selectedDateMillis = selectedDateMillis,
                            onSelectedDateChange = { selectedDateMillis = it }
                        )
                    } else {
                        val activeOpt = selectedOption!!
                        val scoped = com.example.ui.screens.filterTransactionsByMenuOption(
                            allHistory,
                            activeOpt,
                            selectedDateMillis
                        )
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = scoped,
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Maju"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {},
                            initialPeriod = activeOpt.defaultPeriod,
                            menuOptionTitle = activeOpt.title,
                            onBackToMenu = { selectedOption = null },
                            selectedDateMillis = selectedDateMillis,
                            onSelectedDateChange = { selectedDateMillis = it }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        // Select today's report first -> sees today's nota
        composeTestRule.onNodeWithTag("menu_report_today").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_nota_INV-HARI-INI").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_nota_INV-KEMARIN").assertDoesNotExist()

        // Step to previous date -> sees yesterday's nota and hides today's nota
        composeTestRule.onNodeWithTag("btn_kp_prev_date").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_nota_INV-KEMARIN").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_nota_INV-HARI-INI").assertDoesNotExist()

        // Tap "Hari Ini" reset chip -> returns to today's nota
        composeTestRule.onNodeWithTag("btn_kp_today_date").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_nota_INV-HARI-INI").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_nota_INV-KEMARIN").assertDoesNotExist()
    }

    @Test
    fun `verify expense RTDB payload and ExpenseScreen period-category-search filters`() {
        val repo = com.example.data.repository.FirebaseSyncRepository(context)
        val todayStart = com.example.ui.PosViewModel.getStartOfDay(0)
        val yesterdayStart = com.example.ui.PosViewModel.getStartOfDay(-1)

        val expToday = com.example.data.model.Expense(
            id = 42L,
            title = "Beli Gas LPG",
            amount = 24000.0,
            category = "Operasional",
            timestamp = todayStart + 9 * 3600_000L,
            notes = "Tabung 3kg dapur",
            createdBy = "Siti Kasir"
        )
        val expYesterday = com.example.data.model.Expense(
            id = 43L,
            title = "Token Listrik",
            amount = 100000.0,
            category = "Utilitas",
            timestamp = yesterdayStart + 11 * 3600_000L,
            notes = "Meteran toko",
            createdBy = "Budi Admin"
        )

        val payload = repo.buildExpensePayloadMap(expToday)
        assertEquals("EXP-42", payload["id"])
        assertEquals("Beli Gas LPG", payload["title"])
        assertEquals(24000.0, payload["amount"])
        assertEquals("Siti Kasir", payload["createdBy"])
        assertEquals("Siti Kasir", payload["cashierName"])
        assertEquals(expToday.timestamp, payload["timestampMillis"])

        val list = listOf(expToday, expYesterday)
        val todayOnly = com.example.ui.screens.filterExpensesForScreen(
            expenses = list,
            periodFilter = com.example.ui.screens.ExpensePeriodFilter.TODAY,
            selectedDateMillis = todayStart,
            selectedCategory = "Semua",
            searchQuery = "",
            referenceNowMillis = todayStart + 12 * 3600_000L
        )
        assertEquals(listOf(42L), todayOnly.map { it.id })

        val customPrev = com.example.ui.screens.filterExpensesForScreen(
            expenses = list,
            periodFilter = com.example.ui.screens.ExpensePeriodFilter.CUSTOM_DATE,
            selectedDateMillis = yesterdayStart,
            selectedCategory = "Utilitas",
            searchQuery = "listrik",
            referenceNowMillis = todayStart + 12 * 3600_000L
        )
        assertEquals(listOf(43L), customPrev.map { it.id })
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify ExpenseScreenContent create sync and delete flows plus left nav EXPENSES tab`() {
        var activeTab by mutableStateOf(com.example.ui.MainTab.POS)
        val navTabs = listOf(
            com.example.ui.MainTab.POS,
            com.example.ui.MainTab.EXPENSES,
            com.example.ui.MainTab.REPORT
        )
        val initialExpenses = mutableStateOf(
            listOf(
                com.example.data.model.Expense(
                    id = 10L,
                    title = "Es Batu Kristal",
                    amount = 15000.0,
                    category = "Bahan/Kemasan",
                    notes = "2 pack",
                    createdBy = "Kasir 1"
                )
            )
        )
        var syncTriggered = false
        var savedTitle = ""
        var savedAmount = 0.0

        composeTestRule.setContent {
            com.example.ui.screens.AdaptiveSlideNavigationContainer(
                tabs = navTabs,
                currentTab = activeTab,
                storeName = "Warung Maju",
                userFullName = "Admin",
                userRole = com.example.data.model.UserRole.ADMIN,
                connectedPrinter = true,
                autoHideDelayMs = 0L,
                onTabSelected = { activeTab = it },
                onPrinterClick = {},
                onCloudReportClick = {},
                onLogoutClick = {}
            ) {
                if (activeTab == com.example.ui.MainTab.EXPENSES) {
                    com.example.ui.screens.ExpenseScreenContent(
                        expenses = initialExpenses.value,
                        isSyncing = false,
                        syncStatusText = "Otomatis tersinkron ke Laporan Web Chrome",
                        onAddExpense = { title, amount, cat, notes, ts ->
                            savedTitle = title
                            savedAmount = amount
                            initialExpenses.value = listOf(
                                com.example.data.model.Expense(
                                    id = 11L,
                                    title = title,
                                    amount = amount,
                                    category = cat,
                                    notes = notes,
                                    timestamp = ts,
                                    createdBy = "Admin"
                                )
                            ) + initialExpenses.value
                        },
                        onDeleteExpense = { exp ->
                            initialExpenses.value = initialExpenses.value.filterNot { it.id == exp.id }
                        },
                        onSyncNow = { syncTriggered = true },
                        onOpenWebChrome = {}
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_EXPENSES").performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.example.ui.MainTab.EXPENSES, activeTab)

        // Existing expense card displayed & manual sync trigger works
        composeTestRule.onNodeWithTag("card_expense_10").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_sync_expenses_now").performScrollTo().performClick()
        assertTrue(syncTriggered)

        // Create new expense via dialog -> triggers onAddExpense & updates list immediately
        composeTestRule.onNodeWithTag("btn_add_expense").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("input_expense_title").performTextInput("Minyak Goreng")
        composeTestRule.onNodeWithTag("input_expense_amount").performTextInput("35000")
        composeTestRule.onNodeWithTag("btn_save_expense").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Minyak Goreng", savedTitle)
        assertEquals(35000.0, savedAmount, 0.01)
        composeTestRule.onNodeWithTag("card_expense_11").performScrollTo().assertIsDisplayed()

        // Delete original item via confirmation dialog
        composeTestRule.onNodeWithTag("btn_delete_expense_10").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_delete_expense_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_expense_10").assertDoesNotExist()
    }

    @Test
    fun `verify FirebaseSyncRepository placeholder filtering and URL builders`() {
        val repoCompanion = com.example.data.repository.FirebaseSyncRepository.Companion

        assertTrue(repoCompanion.isPlaceholderCloudId(null))
        assertTrue(repoCompanion.isPlaceholderCloudId(""))
        assertTrue(repoCompanion.isPlaceholderCloudId("(default)"))
        assertTrue(repoCompanion.isPlaceholderCloudId("laporan-service"))
        assertTrue(repoCompanion.isPlaceholderCloudId("ai-studio-android-kasirpos-123"))
        assertTrue(repoCompanion.isPlaceholderCloudId("gen-lang-client-01"))
        org.junit.Assert.assertFalse(repoCompanion.isPlaceholderCloudId("kasir-toko-db"))
        org.junit.Assert.assertFalse(repoCompanion.isPlaceholderCloudId("https://kasir-toko.europe-west1.firebasedatabase.app/"))

        val customUrls = repoCompanion.buildRtdbUrlCandidates(
            databaseId = "https://kasir-toko.europe-west1.firebasedatabase.app/",
            projectId = "ai-studio-placeholder",
            path = "expenses/EXP-5"
        )
        assertEquals("https://kasir-toko.europe-west1.firebasedatabase.app/expenses/EXP-5.json", customUrls.first())
        assertTrue(customUrls.none { it.contains("ai-studio-placeholder") })

        val projectFallbackUrls = repoCompanion.buildRtdbUrlCandidates(
            databaseId = "(default)",
            projectId = "my-warung-pos",
            path = "/expenses/EXP-6.json"
        )
        assertTrue(projectFallbackUrls.any { it == "https://my-warung-pos-default-rtdb.asia-southeast1.firebasedatabase.app/expenses/EXP-6.json" })
        assertTrue(projectFallbackUrls.none { it.contains("(default)") })

        val relayUrl = repoCompanion.buildHostingRelayUrl(
            hostingUrl = "http://127.0.0.1:8080/owner.html?projectId=my-warung-pos&view=expenses#top",
            path = "/expenses/EXP-6.json"
        )
        assertEquals("http://127.0.0.1:8080/api/sync/expenses/EXP-6", relayUrl)
        org.junit.Assert.assertNull(repoCompanion.buildHostingRelayUrl("invalid-url", "expenses"))
    }

    @Test
    fun `verify syncExpense and deleteExpenseFromRtdb send valid HTTP requests to Hosting Relay`() {
        val capturedRequests = java.util.concurrent.CopyOnWriteArrayList<Triple<String, String, String>>()
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/sync/") { exchange ->
            val method = exchange.requestMethod
            val path = exchange.requestURI.path
            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            capturedRequests.add(Triple(method, path, body))
            val respBytes = """{"ok":true}""".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, respBytes.size.toLong())
            exchange.responseBody.use { it.write(respBytes) }
        }
        server.start()

        try {
            val port = server.address.port
            val hostingUrl = "http://127.0.0.1:$port/owner.html?view=expenses"
            val repo = com.example.data.repository.FirebaseSyncRepository(context)
            val expense = com.example.data.model.Expense(
                id = 77L,
                title = "Gas LPG 12kg",
                amount = 195000.0,
                category = "Operasional",
                notes = "Dapur utama",
                timestamp = 1763310000000L,
                createdBy = "Kasir Shift Pagi"
            )

            val synced = kotlinx.coroutines.runBlocking {
                repo.syncExpense(
                    expense = expense,
                    storeId = "store_main",
                    databaseId = "(default)",
                    projectId = "laporan-service",
                    hostingUrl = hostingUrl
                )
            }
            assertTrue(synced)

            val putExpense = capturedRequests.firstOrNull { it.first == "PUT" && it.second == "/api/sync/expenses/EXP-77" }
            assertNotNull("Expected PUT to /api/sync/expenses/EXP-77", putExpense)
            val expenseJson = org.json.JSONObject(putExpense!!.third)
            assertEquals("EXP-77", expenseJson.getString("id"))
            assertEquals("Gas LPG 12kg", expenseJson.getString("title"))
            assertEquals(195000.0, expenseJson.getDouble("amount"), 0.01)
            assertEquals("Operasional", expenseJson.getString("category"))
            assertEquals("Dapur utama", expenseJson.getString("notes"))

            val expectedDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getDefault()
            }.format(java.util.Date(expense.timestamp))

            val putDailyExp = capturedRequests.firstOrNull {
                it.first == "PUT" && it.second == "/api/sync/reports/daily_expenses/$expectedDateStr/EXP-77"
            }
            assertNotNull("Expected PUT to /api/sync/reports/daily_expenses/$expectedDateStr/EXP-77", putDailyExp)
            val dailyExpJson = org.json.JSONObject(putDailyExp!!.third)
            assertEquals("EXP-77", dailyExpJson.getString("id"))
            assertEquals("Gas LPG 12kg", dailyExpJson.getString("title"))

            val putCashFlow = capturedRequests.firstOrNull {
                it.first == "PUT" && it.second == "/api/sync/cash_flows/CF-EXP-77"
            }
            assertNotNull("Expected PUT to /api/sync/cash_flows/CF-EXP-77", putCashFlow)
            val cfJson = org.json.JSONObject(putCashFlow!!.third)
            assertEquals("OUT", cfJson.getString("type"))
            assertEquals("Gas LPG 12kg", cfJson.getString("description"))
            assertEquals(195000.0, cfJson.getDouble("amount"), 0.01)

            val putLastExpense = capturedRequests.firstOrNull { it.first == "PUT" && it.second == "/api/sync/realtime_stats/last_expense" }
            assertNotNull("Expected PUT to /api/sync/realtime_stats/last_expense", putLastExpense)
            val statsJson = org.json.JSONObject(putLastExpense!!.third)
            assertEquals("EXP-77", statsJson.getString("lastExpenseId"))
            assertEquals("Gas LPG 12kg", statsJson.getString("lastExpenseTitle"))

            val deleted = kotlinx.coroutines.runBlocking {
                repo.deleteExpenseFromRtdb(
                    expenseId = 77L,
                    databaseId = "(default)",
                    projectId = "laporan-service",
                    hostingUrl = hostingUrl,
                    timestampMillis = expense.timestamp
                )
            }
            assertTrue(deleted)
            assertTrue(capturedRequests.any { it.first == "DELETE" && it.second == "/api/sync/expenses/EXP-77" })
            assertTrue(capturedRequests.any { it.first == "DELETE" && it.second == "/api/sync/cash_flows/CF-EXP-77" })
            assertTrue(capturedRequests.any { it.first == "DELETE" && it.second == "/api/sync/reports/daily_expenses/$expectedDateStr/EXP-77" })
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `verify syncExpense returns false when relay returns 302 redirect or HTML login page`() {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/sync/") { exchange ->
            exchange.responseHeaders.add("Location", "/__cookie_check.html")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/__cookie_check.html") { exchange ->
            val html = "<!DOCTYPE html><html><body>Cookie Check Login Required</body></html>".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, html.size.toLong())
            exchange.responseBody.use { it.write(html) }
        }
        server.start()

        try {
            val port = server.address.port
            val repo = com.example.data.repository.FirebaseSyncRepository(context)
            val exp = com.example.data.model.Expense(
                id = 88L,
                title = "Beli Es Batu",
                amount = 25000.0,
                category = "Operasional",
                notes = "Shift sore",
                createdBy = "Kasir"
            )
            val ok = kotlinx.coroutines.runBlocking {
                repo.syncExpense(
                    expense = exp,
                    databaseId = "(default)",
                    projectId = "laporan-service",
                    hostingUrl = "http://127.0.0.1:$port"
                )
            }
            org.junit.Assert.assertFalse("Should NOT report sync success on 302 auth redirect", ok)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `verify LocalWebReportServer serves owner html and live Room reports including expenses and per-nota`() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val now = System.currentTimeMillis()
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))

        kotlinx.coroutines.runBlocking {
            db.storeProfileDao().insertStoreProfile(
                com.example.data.model.StoreProfile(
                    id = 1,
                    storeName = "WARUNG KOPI NUSANTARA",
                    address = "Jl. Sudirman No. 10",
                    phone = "0812000111"
                )
            )
            val trxId = db.transactionDao().insertTransaction(
                com.example.data.model.Transaction(
                    invoiceNumber = "INV-TEST-001",
                    cashierId = 1L,
                    cashierName = "Budi",
                    timestamp = now,
                    subtotal = 36000.0,
                    grandTotal = 36000.0,
                    paymentMethod = com.example.data.model.PaymentMethod.QRIS,
                    amountPaid = 36000.0,
                    customerName = "Andi",
                    profitAmount = 16000.0,
                    totalItemsCount = 2
                )
            )
            db.transactionDao().insertTransactionItems(
                listOf(
                    com.example.data.model.TransactionItem(
                        transactionId = trxId,
                        productId = 1L,
                        productName = "Kopi Susu Gula Aren",
                        unitPrice = 18000.0,
                        costPrice = 10000.0,
                        quantity = 2,
                        subtotal = 36000.0
                    )
                )
            )
            db.expenseDao().insertExpense(
                com.example.data.model.Expense(
                    title = "Beli Susu UHT",
                    amount = 75000.0,
                    category = "Bahan/Kemasan",
                    timestamp = now,
                    notes = "3 karton",
                    createdBy = "Budi"
                )
            )
            db.shiftClosingDao().insertShiftClosing(
                com.example.data.model.ShiftClosing(
                    closingTime = now,
                    cashierId = 1L,
                    cashierName = "Budi",
                    totalTransactions = 1,
                    totalCash = 0.0,
                    totalNonCash = 36000.0,
                    totalRevenue = 36000.0,
                    expectedCash = 0.0,
                    actualCash = 0.0,
                    notes = "Shift pagi aman"
                )
            )
            db.dailyReportDao().insertOrUpdateDailyReport(
                com.example.data.model.DailyReport(
                    date = todayStr,
                    dateTimestamp = now,
                    totalRevenue = 36000.0,
                    totalProfit = 16000.0,
                    totalTransactions = 1,
                    totalItemsSold = 2,
                    cashierNames = "Budi",
                    shiftCount = 1,
                    notes = "Rekap harian"
                )
            )
        }

        val webDirCandidates = listOf(
            java.io.File("src/main/assets/web"),
            java.io.File("app/src/main/assets/web")
        )
        val assetsWebDir = webDirCandidates.firstOrNull { java.io.File(it, "owner.html").isFile }

        try {
            com.example.service.LocalWebReportServer.stop()
            val port = com.example.service.LocalWebReportServer.start(
                context = context,
                database = db,
                preferredPort = 0,
                assetsFallbackDir = assetsWebDir
            )
            assertNotNull("LocalWebReportServer should bind cleanly", port)
            val baseUrl = "http://127.0.0.1:$port"

            fun httpGet(path: String): Pair<Int, String> {
                val conn = java.net.URL("$baseUrl$path").openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                return code to text
            }

            val (healthCode, healthBody) = httpGet("/api/health")
            assertEquals(200, healthCode)
            assertTrue(org.json.JSONObject(healthBody).getBoolean("ok"))

            val (expCode, expBody) = httpGet("/api/sync/expenses")
            assertEquals(200, expCode)
            val expJson = org.json.JSONObject(expBody)
            assertTrue(expJson.keys().asSequence().any { expJson.getJSONObject(it).getString("title") == "Beli Susu UHT" })

            val (dailyExpCode, dailyExpBody) = httpGet("/api/sync/reports/daily_expenses")
            assertEquals(200, dailyExpCode)
            val dailyExpJson = org.json.JSONObject(dailyExpBody)
            assertTrue(dailyExpJson.has(todayStr))
            val todayExpBucket = dailyExpJson.getJSONObject(todayStr)
            assertTrue(todayExpBucket.keys().asSequence().any { todayExpBucket.getJSONObject(it).getString("title") == "Beli Susu UHT" })

            val (cfCode, cfBody) = httpGet("/api/sync/cash_flows")
            assertEquals(200, cfCode)
            val cfJson = org.json.JSONObject(cfBody)
            assertTrue(cfJson.keys().asSequence().any {
                cfJson.getJSONObject(it).getString("description") == "Beli Susu UHT" &&
                    cfJson.getJSONObject(it).getString("type") == "OUT"
            })

            val (trxCode, trxBody) = httpGet("/api/sync/transactions")
            assertEquals(200, trxCode)
            val trxJson = org.json.JSONObject(trxBody)
            val invObj = trxJson.getJSONObject("INV-TEST-001")
            assertEquals("Andi", invObj.getString("customerName"))
            assertTrue(invObj.getJSONArray("items").length() == 1)

            val (dailyTrxCode, dailyTrxBody) = httpGet("/api/sync/reports/daily_transactions")
            assertEquals(200, dailyTrxCode)
            val dailyTrxJson = org.json.JSONObject(dailyTrxBody)
            assertTrue(dailyTrxJson.getJSONObject(todayStr).has("INV-TEST-001"))

            val (shiftCode, shiftBody) = httpGet("/api/sync/shift_reports")
            assertEquals(200, shiftCode)
            val shiftJson = org.json.JSONObject(shiftBody)
            assertTrue(shiftJson.length() >= 1)
            val firstShiftKey = shiftJson.keys().next()
            assertEquals(75000.0, shiftJson.getJSONObject(firstShiftKey).getDouble("totalExpenses"), 0.01)

            val (dailyCode, dailyBody) = httpGet("/api/sync/daily_reports")
            assertEquals(200, dailyCode)
            val dailyJson = org.json.JSONObject(dailyBody)
            assertEquals(75000.0, dailyJson.getJSONObject(todayStr).getDouble("totalExpenses"), 0.01)

            val (profileCode, profileBody) = httpGet("/api/sync/store_profile/store_main")
            assertEquals(200, profileCode)
            assertEquals("WARUNG KOPI NUSANTARA", org.json.JSONObject(profileBody).getString("storeName"))

            if (assetsWebDir != null) {
                val (htmlCode, htmlBody) = httpGet("/owner.html")
                assertEquals(200, htmlCode)
                assertTrue(htmlBody.contains("Portal Laporan Realtime Database"))
                assertTrue(htmlBody.contains("initFirebaseRealtimeSdk"))
            }
        } finally {
            com.example.service.LocalWebReportServer.stop()
            db.close()
        }
    }

    @Test
    fun `verify filterExpensesByMenuOption and aggregateTransactionRecords include expenses and netProfit`() {
        fun makeTs(year: Int, month: Int, day: Int, hour: Int = 10): Long {
            return java.util.Calendar.getInstance().apply {
                set(year, month, day, hour, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val now = makeTs(2026, java.util.Calendar.NOVEMBER, 15, 14)
        val eToday = com.example.data.model.Expense(
            id = 1L,
            title = "Beli Es Batu",
            amount = 15000.0,
            category = "Bahan/Kemasan",
            timestamp = now,
            createdBy = "Kasir A"
        )
        val eSameMonth = com.example.data.model.Expense(
            id = 2L,
            title = "Sabun Cuci",
            amount = 20000.0,
            category = "Operasional",
            timestamp = makeTs(2026, java.util.Calendar.NOVEMBER, 10, 9),
            createdBy = "Admin"
        )
        val eSameYear = com.example.data.model.Expense(
            id = 3L,
            title = "Token Listrik Oktober",
            amount = 100000.0,
            category = "Utilitas",
            timestamp = makeTs(2026, java.util.Calendar.OCTOBER, 20, 11),
            createdBy = "Admin"
        )
        val ePrevYear = com.example.data.model.Expense(
            id = 4L,
            title = "Servis Mesin 2025",
            amount = 250000.0,
            category = "Lainnya",
            timestamp = makeTs(2025, java.util.Calendar.DECEMBER, 5, 15),
            createdBy = "Owner"
        )
        val allExpenses = listOf(eToday, eSameMonth, eSameYear, ePrevYear)

        assertEquals(
            listOf(1L),
            com.example.ui.screens.filterExpensesByMenuOption(allExpenses, com.example.ui.screens.TransactionReportMenuOption.TODAY, now).map { it.id }
        )
        assertEquals(
            listOf(1L, 2L),
            com.example.ui.screens.filterExpensesByMenuOption(allExpenses, com.example.ui.screens.TransactionReportMenuOption.THIS_MONTH, now).map { it.id }
        )
        assertEquals(
            listOf(1L, 2L, 3L),
            com.example.ui.screens.filterExpensesByMenuOption(allExpenses, com.example.ui.screens.TransactionReportMenuOption.THIS_YEAR, now).map { it.id }
        )
        assertEquals(
            listOf(1L, 2L, 3L, 4L),
            com.example.ui.screens.filterExpensesByMenuOption(allExpenses, com.example.ui.screens.TransactionReportMenuOption.ALL, now).map { it.id }
        )

        val trxToday = com.example.data.model.Transaction(
            id = 10L,
            invoiceNumber = "INV-EXP-1",
            cashierId = 1L,
            cashierName = "Kasir A",
            timestamp = now,
            subtotal = 80000.0,
            grandTotal = 80000.0,
            amountPaid = 80000.0,
            profitAmount = 35000.0,
            totalItemsCount = 2
        )

        val dailyRecords = com.example.ui.screens.aggregateTransactionRecords(
            transactions = listOf(trxToday),
            period = com.example.ui.screens.TransactionRecordPeriod.DAILY,
            expenses = listOf(eToday, eSameMonth)
        )
        assertEquals(2, dailyRecords.size)
        val recNov15 = dailyRecords.first { it.periodKey == "2026-11-15" }
        assertEquals(80000.0, recNov15.totalRevenue, 0.01)
        assertEquals(35000.0, recNov15.totalProfit, 0.01)
        assertEquals(15000.0, recNov15.totalExpense, 0.01)
        assertEquals(1, recNov15.expenseCount)
        assertEquals(20000.0, recNov15.netProfit, 0.01)

        // Expense-only period on Nov 10 should still create a record
        val recNov10 = dailyRecords.first { it.periodKey == "2026-11-10" }
        assertEquals(0, recNov10.transactionCount)
        assertEquals(1, recNov10.expenseCount)
        assertEquals(20000.0, recNov10.totalExpense, 0.01)
        assertEquals(-20000.0, recNov10.netProfit, 0.01)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify Laporan Transaksi menu shows Laporan pengeluaran and opens TransactionExpenseReportPanel`() {
        val todayStart = com.example.ui.PosViewModel.getStartOfDay(0)
        val yesterdayStart = com.example.ui.PosViewModel.getStartOfDay(-1)
        var showExpenseReport by mutableStateOf(false)
        var selectedDateMillis by mutableStateOf(todayStart)
        var deletedExpense by mutableStateOf<com.example.data.model.Expense?>(null)
        var syncNowClicked = false
        var openWebClickedCount = 0

        val expToday = com.example.data.model.Expense(
            id = 501L,
            title = "Beli Susu Fresh",
            amount = 42000.0,
            category = "Bahan/Kemasan",
            timestamp = todayStart + 8 * 3600_000L,
            notes = "2 liter",
            createdBy = "Rina"
        )
        val expYesterday = com.example.data.model.Expense(
            id = 502L,
            title = "Bayar Kebersihan",
            amount = 25000.0,
            category = "Operasional",
            timestamp = yesterdayStart + 10 * 3600_000L,
            notes = "Iuran harian",
            createdBy = "Admin"
        )
        val trxToday = com.example.data.model.Transaction(
            id = 601L,
            invoiceNumber = "INV-PN-601",
            cashierId = 1L,
            cashierName = "Rina",
            timestamp = todayStart + 9 * 3600_000L,
            subtotal = 90000.0,
            grandTotal = 90000.0,
            amountPaid = 90000.0,
            profitAmount = 50000.0,
            totalItemsCount = 3
        )

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (!showExpenseReport) {
                        com.example.ui.screens.TransactionReportMenuCard(
                            onSelectOption = {},
                            onBackClick = {},
                            selectedDateMillis = selectedDateMillis,
                            onSelectedDateChange = { selectedDateMillis = it },
                            expenses = listOf(expToday, expYesterday),
                            onSelectExpenseReport = { showExpenseReport = true },
                            onAddExpenseClick = {},
                            isExpenseSyncing = false,
                            expenseSyncStatus = "2 pengeluaran otomatis tersinkron ke Web Chrome",
                            onOpenWebChromeExpenses = { openWebClickedCount++ }
                        )
                    } else {
                        com.example.ui.screens.TransactionExpenseReportPanel(
                            allTransactions = listOf(trxToday),
                            allExpenses = listOf(expToday, expYesterday),
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Toko Maju"),
                            selectedDateMillis = selectedDateMillis,
                            onSelectedDateChange = { selectedDateMillis = it },
                            onBackToMenu = { showExpenseReport = false },
                            onAddExpenseClick = {},
                            onDeleteExpense = { deletedExpense = it },
                            isExpenseSyncing = false,
                            expenseSyncStatus = "2 pengeluaran otomatis tersinkron ke Web Chrome",
                            onSyncExpensesNow = { syncNowClicked = true },
                            onOpenWebChromeExpenses = { openWebClickedCount++ }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_report_expenses").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("badge_menu_expense_autosync", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_open_web_expense_from_menu").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, openWebClickedCount)

        composeTestRule.onNodeWithTag("menu_report_expenses").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_report_expenses").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("card_transaction_expense_report").assertIsDisplayed()
        composeTestRule.onNodeWithTag("badge_trx_expense_autosync").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_sync_trx_expense_now").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(syncNowClicked)

        composeTestRule.onNodeWithTag("btn_open_web_expense_from_trx_report").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, openWebClickedCount)

        composeTestRule.onNodeWithTag("row_record_expense_501").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_expense_502").assertDoesNotExist()

        // Switch scope to ALL -> shows both today and yesterday expenses
        composeTestRule.onNodeWithTag("tab_expense_scope_all").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_expense_501").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_record_expense_502").performScrollTo().assertIsDisplayed()

        // Delete item triggers confirmation & callback
        composeTestRule.onNodeWithTag("btn_delete_record_expense_501").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_confirm_delete_trx_expense").performClick()
        composeTestRule.waitForIdle()
        assertEquals(501L, deletedExpense?.id)

        // Back to menu
        composeTestRule.onNodeWithTag("btn_back_expense_to_menu").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_transaction_report_menu").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify TransactionRecordReportSection per-nota shows integrated Laporan Pengeluaran and Pengeluaran chart segment`() {
        val todayStart = com.example.ui.PosViewModel.getStartOfDay(0)
        val trx = com.example.data.model.Transaction(
            id = 701L,
            invoiceNumber = "INV-701",
            cashierId = 1L,
            cashierName = "Budi",
            timestamp = todayStart + 10 * 3600_000L,
            subtotal = 50000.0,
            grandTotal = 50000.0,
            amountPaid = 50000.0,
            profitAmount = 22000.0,
            totalItemsCount = 2
        )
        val exp = com.example.data.model.Expense(
            id = 801L,
            title = "Beli Cup Plastik",
            amount = 12000.0,
            category = "Bahan/Kemasan",
            timestamp = todayStart + 11 * 3600_000L,
            notes = "2 pack",
            createdBy = "Budi"
        )
        var addExpenseTapped = false
        var syncPerNotaTapped = false
        var openWebPerNotaTapped = false

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = listOf(trx),
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Kopi"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {},
                            initialPeriod = com.example.ui.screens.TransactionRecordPeriod.DAILY,
                            menuOptionTitle = com.example.ui.screens.TransactionReportMenuOption.TODAY.title,
                            selectedDateMillis = todayStart,
                            allExpenses = listOf(exp),
                            onDeleteExpense = {},
                            onAddExpenseClick = { addExpenseTapped = true },
                            isExpenseSyncing = false,
                            expenseSyncStatus = "1 pengeluaran otomatis tersinkron ke Web Chrome",
                            onSyncExpensesNow = { syncPerNotaTapped = true },
                            onOpenWebChromeExpenses = { openWebPerNotaTapped = true }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("segment_metric_expense").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("segment_metric_expense").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("summary_per_nota_finance").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_per_nota_expenses").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("badge_per_nota_expense_autosync").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_sync_per_nota_expense_now").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(syncPerNotaTapped)

        composeTestRule.onNodeWithTag("btn_open_web_expense_per_nota").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(openWebPerNotaTapped)

        composeTestRule.onNodeWithTag("row_record_expense_801").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_add_expense_per_nota").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(addExpenseTapped)
    }

    @Test
    fun `verify buildChromeWebReportUrl includes trx-expenses view and date param and portals support autosync badge`() {
        val dateCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 14, 9, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val app = context as android.app.Application
        val vm = com.example.ui.PosViewModel(app)
        try {
            val url = vm.buildChromeWebReportUrl(
                view = "trx-expenses",
                dateMillis = dateCal.timeInMillis
            )
            assertTrue("Expected view=trx-expenses in $url", url.contains("view=trx-expenses"))
            assertTrue("Expected date=2026-11-14 in $url", url.contains("date=2026-11-14"))
        } finally {
            com.example.service.LocalWebReportServer.stop()
        }

        // Check bundled HTML template + web-dashboard HTML portals
        val tplHtml = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        assertTrue(tplHtml.contains("kp-exp-autosync-badge"))
        assertTrue(tplHtml.contains("trx-expenses"))

        val ownerFile = listOf(
            java.io.File("src/main/assets/web/owner.html"),
            java.io.File("app/src/main/assets/web/owner.html")
        ).firstOrNull { it.isFile }
        if (ownerFile != null) {
            val ownerHtml = ownerFile.readText(Charsets.UTF_8)
            assertTrue(ownerHtml.contains("kp-exp-autosync-badge"))
            assertTrue(ownerHtml.contains("trx-expenses"))
        }

        val rtdbFile = listOf(
            java.io.File("src/main/assets/web/laporan-online-rtdb.html"),
            java.io.File("app/src/main/assets/web/laporan-online-rtdb.html")
        ).firstOrNull { it.isFile }
        if (rtdbFile != null) {
            val rtdbHtml = rtdbFile.readText(Charsets.UTF_8)
            assertTrue(rtdbHtml.contains("kp-exp-autosync-badge"))
            assertTrue(rtdbHtml.contains("trx-expenses"))
        }
    }

    @Test
    fun `verify LocalWebReportServer emits PNA CORS header merges PATCH overlays and serves daily_expenses`() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        try {
            com.example.service.LocalWebReportServer.stop()
            val port = com.example.service.LocalWebReportServer.start(
                context = context,
                database = db,
                preferredPort = 0
            )
            assertNotNull("LocalWebReportServer must bind cleanly", port)
            val baseUrl = "http://127.0.0.1:$port"

            // Verify OPTIONS preflight includes Chrome Private Network Access header
            val preflightConn = java.net.URL("$baseUrl/api/sync/expenses").openConnection() as java.net.HttpURLConnection
            preflightConn.requestMethod = "OPTIONS"
            preflightConn.connectTimeout = 3000
            preflightConn.readTimeout = 3000
            assertEquals(204, preflightConn.responseCode)
            assertEquals("true", preflightConn.getHeaderField("Access-Control-Allow-Private-Network"))
            val allowHeaders = preflightConn.getHeaderField("Access-Control-Allow-Headers") ?: ""
            assertTrue("Expected Access-Control-Request-Private-Network in CORS allow headers", allowHeaders.contains("Access-Control-Request-Private-Network"))

            // Verify PATCH merges into existing relayOverlay entry instead of clobbering
            fun sendPatch(path: String, jsonBody: String): Int {
                val client = okhttp3.OkHttpClient()
                val mediaType = with(okhttp3.MediaType.Companion) { "application/json; charset=utf-8".toMediaType() }
                val body = with(okhttp3.RequestBody.Companion) { jsonBody.toRequestBody(mediaType) }
                val req = okhttp3.Request.Builder()
                    .url("$baseUrl$path")
                    .patch(body)
                    .build()
                client.newCall(req).execute().use { return it.code }
            }

            assertEquals(
                200,
                sendPatch(
                    "/api/sync/expenses/EXP-999",
                    """{"id":"EXP-999","title":"Beli Es Batu","category":"Bahan/Kemasan","amount":25000,"date":"$todayStr"}"""
                )
            )
            assertEquals(
                200,
                sendPatch(
                    "/api/sync/expenses/EXP-999",
                    """{"notes":"2 karung besar","syncedAt":"2026-11-17T08:00:00Z"}"""
                )
            )
            assertEquals(
                200,
                sendPatch(
                    "/api/sync/reports/daily_expenses/$todayStr/EXP-998",
                    """{"id":"EXP-998","title":"Gas LPG 3kg","category":"Operasional","amount":22000,"date":"$todayStr"}"""
                )
            )

            val expResp = java.net.URL("$baseUrl/api/sync/expenses").readText()
            val expJson = org.json.JSONObject(expResp)
            assertTrue("Expected merged EXP-999 in /api/sync/expenses", expJson.has("EXP-999"))
            val exp999 = expJson.getJSONObject("EXP-999")
            assertEquals("Beli Es Batu", exp999.getString("title"))
            assertEquals("2 karung besar", exp999.getString("notes"))
            assertEquals(25000.0, exp999.getDouble("amount"), 0.01)

            val dailyExpResp = java.net.URL("$baseUrl/api/sync/reports/daily_expenses").readText()
            val dailyExpJson = org.json.JSONObject(dailyExpResp)
            assertTrue("Expected date bucket $todayStr in /api/sync/reports/daily_expenses", dailyExpJson.has(todayStr))
            assertTrue(dailyExpJson.getJSONObject(todayStr).has("EXP-998"))
        } finally {
            com.example.service.LocalWebReportServer.stop()
            db.close()
        }
    }

    @Test
    fun `verify PosViewModel rewrites stale loopback port and appends localRelay plus portals support daily_expenses and CF-EXP dedup`() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            com.example.service.LocalWebReportServer.stop()
            val activePort = com.example.service.LocalWebReportServer.start(
                context = context,
                database = db,
                preferredPort = 0
            )
            assertNotNull(activePort)

            val vm = com.example.ui.PosViewModel(context)
            // Simulate stale saved loopback port 8080 when server is bound on activePort
            val rewritten = vm.resolveEffectiveHostingUrl("http://127.0.0.1:8080/owner.html")
            assertTrue("Expected rewritten URL to use active port $activePort, got $rewritten", rewritten.startsWith("http://127.0.0.1:$activePort/owner.html"))

            val externalBuilt = vm.buildReportUrl(
                rawBaseUrl = "https://my-custom-hosting.web.app/owner.html",
                view = "expenses"
            )
            assertTrue("Expected localRelay param in external URL $externalBuilt", externalBuilt.contains("localRelay="))
            assertTrue("Expected encoded active loopback port $activePort in $externalBuilt", externalBuilt.contains(android.net.Uri.encode("http://127.0.0.1:$activePort")))
            assertTrue("Expected view=expenses in $externalBuilt", externalBuilt.contains("view=expenses"))

            val chromeUrl = vm.buildChromeWebReportUrl(view = "trx-expenses")
            assertTrue("Expected localRelay in buildChromeWebReportUrl: $chromeUrl", chromeUrl.contains("localRelay="))
            assertTrue("Expected view=trx-expenses in buildChromeWebReportUrl: $chromeUrl", chromeUrl.contains("view=trx-expenses"))
        } finally {
            com.example.service.LocalWebReportServer.stop()
            db.close()
        }

        // Verify all web portal assets support multi-relay discovery, reports/daily_expenses, parseNum, and CF-EXP dedup
        val tplHtml = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        assertTrue(tplHtml.contains("reports/daily_expenses"))
        assertTrue(tplHtml.contains("getRelayBaseCandidates"))
        assertTrue(tplHtml.contains("normalizeExpDedupKey"))

        listOf(
            "src/main/assets/web/owner.html",
            "app/src/main/assets/web/owner.html"
        ).map { java.io.File(it) }.firstOrNull { it.isFile }?.let { file ->
            val ownerHtml = file.readText(Charsets.UTF_8)
            assertTrue(ownerHtml.contains("reports/daily_expenses"))
            assertTrue(ownerHtml.contains("getOriginRelayBases"))
            assertTrue(ownerHtml.contains("normalizeExpenseDedupKey"))
        }

        listOf(
            "src/main/assets/web/laporan-online-rtdb.html",
            "app/src/main/assets/web/laporan-online-rtdb.html"
        ).map { java.io.File(it) }.firstOrNull { it.isFile }?.let { file ->
            val rtdbHtml = file.readText(Charsets.UTF_8)
            assertTrue(rtdbHtml.contains("reports/daily_expenses"))
            assertTrue(rtdbHtml.contains("getRelayBaseCandidates"))
            assertTrue(rtdbHtml.contains("normalizeExpenseDedupKey"))
        }

        listOf(
            "src/main/assets/web/tutup-toko.html",
            "app/src/main/assets/web/tutup-toko.html"
        ).map { java.io.File(it) }.firstOrNull { it.isFile }?.let { file ->
            val tutupHtml = file.readText(Charsets.UTF_8)
            assertTrue(tutupHtml.contains("getTutupTokoRelayBases"))
        }
    }

    @Test
    fun `verify FirebaseSyncRepository RTDB URL candidates auth token helper and syncExpenseDetailed puts all expense paths`() {
        val candidates = com.example.data.repository.FirebaseSyncRepository.buildRtdbUrlCandidates(
            databaseId = null,
            projectId = "kasir-prod-123",
            path = "expenses/EXP-42",
            explicitDatabaseUrl = "https://kasir-prod-123-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        assertTrue(candidates.isNotEmpty())
        assertEquals(
            "https://kasir-prod-123-default-rtdb.asia-southeast1.firebasedatabase.app/expenses/EXP-42.json",
            candidates.first()
        )

        val placeholdersIgnored = com.example.data.repository.FirebaseSyncRepository.buildRtdbUrlCandidates(
            databaseId = "(default)",
            projectId = "ai-studio-placeholder",
            path = "reports/daily_expenses/2026-11-18/EXP-42",
            explicitDatabaseUrl = "gen-lang-client-123"
        )
        assertTrue(placeholdersIgnored.none { it.contains("ai-studio") || it.contains("gen-lang-client") || it.contains("(default)") })

        val baseUrl = "https://example-default-rtdb.asia-southeast1.firebasedatabase.app/expenses/EXP-42.json"
        assertEquals(baseUrl, com.example.data.repository.FirebaseSyncRepository.appendAuthTokenToRtdbUrl(baseUrl, null))
        assertEquals("$baseUrl?auth=tok%2B123", com.example.data.repository.FirebaseSyncRepository.appendAuthTokenToRtdbUrl(baseUrl, "tok+123"))

        val recordedRequests = java.util.Collections.synchronizedMap(linkedMapOf<String, String>())
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val bodyText = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
            if (exchange.requestMethod.equals("PUT", ignoreCase = true)) {
                recordedRequests[exchange.requestURI.path] = bodyText
            }
            val respBytes = "{}".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, respBytes.size.toLong())
            exchange.responseBody.use { it.write(respBytes) }
        }
        server.start()

        try {
            val port = server.address.port
            val tsMillis = 1763424000000L // deterministic timestamp
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(tsMillis))
            val expense = com.example.data.model.Expense(
                id = 42L,
                title = "Beli Es Batu",
                category = "Bahan Baku",
                amount = 35000.0,
                notes = "2 karung besar",
                timestamp = tsMillis
            )

            val repo = com.example.data.repository.FirebaseSyncRepository(context)
            val result = kotlinx.coroutines.runBlocking {
                repo.syncExpenseDetailed(
                    expense = expense,
                    storeId = "store_test",
                    databaseId = "http://127.0.0.1:$port",
                    projectId = null,
                    hostingUrl = null
                )
            }

            assertTrue("Expected rtdbOk=true", result.rtdbOk)
            assertTrue("Expected dailyRtdbOk=true", result.dailyRtdbOk)
            assertEquals(200, result.rtdbHttpCode)
            assertTrue(recordedRequests.containsKey("/expenses/EXP-42.json"))
            assertTrue(recordedRequests.containsKey("/reports/daily_expenses/$dateStr/EXP-42.json"))
            assertTrue(recordedRequests.containsKey("/cash_flows/CF-EXP-42.json"))
            assertTrue(recordedRequests.containsKey("/realtime_stats/last_expense.json"))

            val expJson = org.json.JSONObject(recordedRequests["/expenses/EXP-42.json"]!!)
            assertEquals("Beli Es Batu", expJson.getString("title"))
            assertEquals("Bahan Baku", expJson.getString("category"))
            assertEquals(35000.0, expJson.getDouble("amount"), 0.01)
            assertEquals("2 karung besar", expJson.getString("notes"))
            assertEquals(dateStr, expJson.getString("date"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `verify rules schema and all web Chrome transaction portals render RTDB expenses box`() {
        val rules = com.example.ui.screens.SourceCodeTemplates.REALTIME_DATABASE_RULES
        assertTrue(rules.contains("\"expenses\""))
        assertTrue(rules.contains("\"reports\""))
        assertTrue(rules.contains("\"daily_expenses\""))

        val schema = com.example.ui.screens.SourceCodeTemplates.REPORT_TABLES_SCHEMA
        assertTrue(schema.contains("totalExpenses"))
        assertTrue(schema.contains("reports/daily_expenses"))

        val tplHtml = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        assertTrue(tplHtml.contains("trx-expenses-box"))
        assertTrue(tplHtml.contains("kp-per-nota-expenses-box"))
        assertTrue(tplHtml.contains("openKpPeriodAlert"))

        listOf(
            "owner.html",
            "index.html",
            "rtdb-laporan.html"
        ).forEach { name ->
            val file = listOf("src/main/assets/web/$name", "app/src/main/assets/web/$name")
                .map { java.io.File(it) }
                .first { it.isFile }
            val html = file.readText(Charsets.UTF_8)
            assertTrue("$name missing trx-expenses-box", html.contains("trx-expenses-box"))
            assertTrue("$name missing trx-finance-summary", html.contains("trx-finance-summary"))
            assertTrue("$name missing kp-per-nota-expenses-box", html.contains("kp-per-nota-expenses-box"))
        }

        val rtdbFile = listOf(
            "src/main/assets/web/laporan-online-rtdb.html",
            "app/src/main/assets/web/laporan-online-rtdb.html"
        ).map { java.io.File(it) }.first { it.isFile }
        val rtdbHtml = rtdbFile.readText(Charsets.UTF_8)
        assertTrue(rtdbHtml.contains("trx-tab-expenses-box"))
        assertTrue(rtdbHtml.contains("trx-kpi-expenses"))
        assertTrue(rtdbHtml.contains("trx-kpi-netprofit"))
        assertTrue(rtdbHtml.contains("kp-per-nota-expenses-box"))
        assertTrue(rtdbHtml.contains("deepMergeRtdbNodes"))
        assertTrue(rtdbHtml.contains("expenses_by_date"))
    }

    @Test
    fun `verify syncExpenseDetailed falls back to realtime_stats when expenses node returns 401 and LocalWebReportServer serves realtime_stats expenses`() {
        val recordedRequests = java.util.Collections.synchronizedMap(linkedMapOf<String, String>())
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            val bodyText = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
            if (path.startsWith("/expenses") || path.startsWith("/reports/daily_expenses") || path.startsWith("/cash_flows")) {
                val errBytes = """{"error":"Permission denied"}""".toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(401, errBytes.size.toLong())
                exchange.responseBody.use { it.write(errBytes) }
            } else {
                if (exchange.requestMethod.equals("PUT", ignoreCase = true) || exchange.requestMethod.equals("PATCH", ignoreCase = true)) {
                    recordedRequests[path] = bodyText
                }
                val respBytes = "{}".toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, respBytes.size.toLong())
                exchange.responseBody.use { it.write(respBytes) }
            }
        }
        server.start()

        val tsMillis = 1763424000000L
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(tsMillis))
        try {
            val port = server.address.port
            val expense = com.example.data.model.Expense(
                id = 99L,
                title = "Beli Gas LPG",
                category = "Operasional",
                amount = 22000.0,
                notes = "1 tabung 3kg",
                timestamp = tsMillis
            )
            val repo = com.example.data.repository.FirebaseSyncRepository(context)
            val result = kotlinx.coroutines.runBlocking {
                repo.syncExpenseDetailed(
                    expense = expense,
                    storeId = "store_test",
                    databaseId = "http://127.0.0.1:$port",
                    projectId = null,
                    hostingUrl = null
                )
            }
            assertTrue("Expected rtdbOk=true via realtime_stats fallback even when /expenses is 401", result.rtdbOk)
            assertTrue(recordedRequests.containsKey("/realtime_stats/expenses/EXP-99.json"))
            assertTrue(recordedRequests.containsKey("/realtime_stats/expenses_by_date/$dateStr/EXP-99.json"))
            assertTrue(recordedRequests.containsKey("/realtime_stats/last_expense.json"))
        } finally {
            server.stop(0)
        }

        // Verify LocalWebReportServer serves realtime_stats expenses_by_date & expenses directly from Room
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val nowMs = System.currentTimeMillis()
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(nowMs))
            kotlinx.coroutines.runBlocking {
                db.expenseDao().insertExpense(
                    com.example.data.model.Expense(
                        id = 105L,
                        title = "Beli Cup & Sedotan",
                        category = "Bahan/Kemasan",
                        amount = 45000.0,
                        notes = "2 pack cup 16oz",
                        timestamp = nowMs,
                        createdBy = "Kasir Test"
                    )
                )
            }
            com.example.service.LocalWebReportServer.stop()
            val srvPort = com.example.service.LocalWebReportServer.start(context = context, database = db, preferredPort = 0)
            assertNotNull(srvPort)
            val statsText = java.net.URL("http://127.0.0.1:$srvPort/api/sync/realtime_stats").readText()
            val statsJson = org.json.JSONObject(statsText)
            assertTrue(statsJson.has("expenses"))
            assertTrue(statsJson.has("expenses_by_date"))
            assertTrue(statsJson.getJSONObject("expenses").has("EXP-105"))
            assertTrue(statsJson.getJSONObject("expenses_by_date").getJSONObject(todayStr).has("EXP-105"))
            assertTrue(statsJson.has("last_expense"))
            assertEquals("Beli Cup & Sedotan", statsJson.getJSONObject("last_expense").getString("lastExpenseTitle"))
        } finally {
            com.example.service.LocalWebReportServer.stop()
            db.close()
        }

        // Verify ONLINE_REPORT_RTDB_HTML & all owner/rtdb web portals support realtime_stats expense recovery
        val tplHtml = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        assertTrue(tplHtml.contains("saveNewExpense"))
        assertTrue(tplHtml.contains("deepMergeRtdbNodes"))
        assertTrue(tplHtml.contains("expenses_by_date"))
        assertTrue(tplHtml.contains("rtdb-rules-warning"))
    }

    @Test
    fun `verify full screen PaymentDialog keypad input uang pas clear backspace and non tunai confirm`() {
        var confirmedMethod: com.example.data.model.PaymentMethod? = null
        var confirmedPaid = 0.0
        var confirmedOrderType: com.example.data.model.OrderType? = null

        composeTestRule.setContent {
            com.example.ui.theme.MyApplicationTheme {
                com.example.ui.screens.PaymentDialog(
                    grandTotal = 20000.0,
                    initialOrderType = com.example.data.model.OrderType.DINE_IN,
                    initialTableNumber = "2",
                    onDismiss = {},
                    onConfirmPayment = { method, amountPaid, _, orderType, _, _ ->
                        confirmedMethod = method
                        confirmedPaid = amountPaid
                        confirmedOrderType = orderType
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("payment_header_total").assertTextEquals("Total : Rp 20.000")
        composeTestRule.onNodeWithTag("cash_amount_display").assertTextEquals("Rp 0")
        composeTestRule.onNodeWithTag("payment_change_display").assertTextEquals("Kembalian : Rp 0")
        composeTestRule.onNodeWithTag("confirm_payment_button").assertIsNotEnabled()

        // Input 5 -> 0 -> 000 => Rp 50.000 => Kembalian : Rp 30.000
        composeTestRule.onNodeWithTag("keypad_5").performClick()
        composeTestRule.onNodeWithTag("keypad_0").performClick()
        composeTestRule.onNodeWithTag("keypad_000").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cash_amount_display").assertTextEquals("Rp 50.000")
        composeTestRule.onNodeWithTag("payment_change_display").assertTextEquals("Kembalian : Rp 30.000")
        composeTestRule.onNodeWithTag("confirm_payment_button").assertIsEnabled()

        // Clear & input 2 -> 0 -> 000 => Rp 20.000 => Kembalian : Rp 0 (Uang Pas)
        composeTestRule.onNodeWithTag("keypad_clear").performClick()
        composeTestRule.onNodeWithTag("keypad_2").performClick()
        composeTestRule.onNodeWithTag("keypad_0").performClick()
        composeTestRule.onNodeWithTag("keypad_000").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cash_amount_display").assertTextEquals("Rp 20.000")
        composeTestRule.onNodeWithTag("payment_change_display").assertTextEquals("Kembalian : Rp 0 (Uang Pas)")
        composeTestRule.onNodeWithTag("confirm_payment_button").assertIsEnabled()

        // Backspace trims last zero -> Rp 2.000 -> Kurang : Rp 18.000 -> confirm disabled
        composeTestRule.onNodeWithTag("keypad_backspace").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cash_amount_display").assertTextEquals("Rp 2.000")
        composeTestRule.onNodeWithTag("payment_change_display").assertTextEquals("Kembalian : Rp 0")
        composeTestRule.onNodeWithTag("payment_shortage_display").assertTextEquals("Kurang : Rp 18.000")
        composeTestRule.onNodeWithTag("confirm_payment_button").assertIsNotEnabled()

        // Clear resets back to Rp 0
        composeTestRule.onNodeWithTag("keypad_clear").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cash_amount_display").assertTextEquals("Rp 0")

        // Tap Uang Pas banknote key -> Rp 20.000 & confirm payment
        composeTestRule.onNodeWithTag("keypad_uang_pas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cash_amount_display").assertTextEquals("Rp 20.000")
        composeTestRule.onNodeWithTag("payment_change_display").assertTextEquals("Kembalian : Rp 0 (Uang Pas)")
        composeTestRule.onNodeWithTag("confirm_payment_button").assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        assertEquals(com.example.data.model.PaymentMethod.TUNAI, confirmedMethod)
        assertEquals(20000.0, confirmedPaid, 0.01)
        assertEquals(com.example.data.model.OrderType.DINE_IN, confirmedOrderType)
    }

    @Test
    fun `verify PaymentDialog options dropdown switches to QRIS and TakeAway`() {
        var confirmedMethod: com.example.data.model.PaymentMethod? = null
        var confirmedOrderType: com.example.data.model.OrderType? = null

        composeTestRule.setContent {
            com.example.ui.theme.MyApplicationTheme {
                com.example.ui.screens.PaymentDialog(
                    grandTotal = 25000.0,
                    onDismiss = {},
                    onConfirmPayment = { method, _, _, orderType, _, _ ->
                        confirmedMethod = method
                        confirmedOrderType = orderType
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("payment_options_dropdown").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payment_method_QRIS").performClick()
        composeTestRule.onNodeWithTag("dialog_order_type_take_away").performClick()
        composeTestRule.waitForIdle()

        // Close options sheet and verify QRIS + TakeAway confirm works without typing cash
        composeTestRule.onNodeWithTag("payment_method_QRIS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payment_detail_apply_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payment_change_display").assertTextEquals("Kembalian : Rp 0")
        composeTestRule.onNodeWithTag("confirm_payment_button").assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        assertEquals(com.example.data.model.PaymentMethod.QRIS, confirmedMethod)
        assertEquals(com.example.data.model.OrderType.TAKE_AWAY, confirmedOrderType)
    }

    @Test
    fun `verify expense notes parity delete last_expense cleanup and two way RTDB reconciliation`() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val nowMs = System.currentTimeMillis()
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(nowMs))
            kotlinx.coroutines.runBlocking {
                db.expenseDao().insertExpense(
                    com.example.data.model.Expense(
                        id = 201L,
                        title = "Beli Es Batu",
                        category = "Bahan Baku",
                        amount = 15000.0,
                        notes = "",
                        timestamp = nowMs,
                        createdBy = "Kasir A"
                    )
                )
                db.shiftClosingDao().insertShiftClosing(
                    com.example.data.model.ShiftClosing(
                        id = 11L,
                        cashierId = 1L,
                        cashierName = "Kasir A",
                        totalTransactions = 2,
                        totalRevenue = 50000.0,
                        totalCash = 50000.0,
                        totalNonCash = 0.0,
                        expectedCash = 35000.0,
                        actualCash = 35000.0,
                        difference = 0.0,
                        closingTime = nowMs
                    )
                )
                db.dailyReportDao().insertOrUpdateDailyReport(
                    com.example.data.model.DailyReport(
                        date = todayStr,
                        dateTimestamp = nowMs,
                        totalRevenue = 50000.0,
                        totalProfit = 20000.0,
                        totalTransactions = 2,
                        totalItemsSold = 2,
                        totalCash = 50000.0,
                        cashierNames = "Kasir A"
                    )
                )
            }
            com.example.service.LocalWebReportServer.stop()
            val srvPort = com.example.service.LocalWebReportServer.start(context = context, database = db, preferredPort = 0)!!

            // 1. Blank expense notes must stay empty in cash_flows, shift_reports.expensesList, and daily_reports.expensesList
            val cashFlows = org.json.JSONObject(java.net.URL("http://127.0.0.1:$srvPort/api/sync/cash_flows").readText())
            assertEquals("", cashFlows.getJSONObject("CF-EXP-201").getString("notes"))

            val dailyReports = org.json.JSONObject(java.net.URL("http://127.0.0.1:$srvPort/api/sync/daily_reports").readText())
            val drExpArr = dailyReports.getJSONObject(todayStr).getJSONArray("expensesList")
            assertEquals(1, drExpArr.length())
            assertEquals("", drExpArr.getJSONObject(0).getString("notes"))

            val shiftReports = org.json.JSONObject(java.net.URL("http://127.0.0.1:$srvPort/api/sync/shift_reports").readText())
            val shiftExpArr = shiftReports.getJSONObject("SHIFT-11").getJSONArray("expensesList")
            assertEquals(1, shiftExpArr.length())
            assertEquals("", shiftExpArr.getJSONObject(0).getString("notes"))

            // 2. Seed a last_expense overlay pointing to EXP-201, then DELETE EXP-201 -> cash_flows & last_expense must be purged
            val putConn = (java.net.URL("http://127.0.0.1:$srvPort/api/sync/realtime_stats/last_expense").openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use {
                    it.write("""{"lastExpenseId":"EXP-201","lastExpenseTitle":"Beli Es Batu","lastExpenseAmount":15000}""".toByteArray())
                }
            }
            assertEquals(200, putConn.responseCode)
            putConn.disconnect()

            val delConn = (java.net.URL("http://127.0.0.1:$srvPort/api/sync/expenses/EXP-201").openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "DELETE"
            }
            assertEquals(200, delConn.responseCode)
            delConn.disconnect()

            val cashFlowsAfter = org.json.JSONObject(java.net.URL("http://127.0.0.1:$srvPort/api/sync/cash_flows").readText())
            org.junit.Assert.assertFalse("CF-EXP-201 should be purged on delete", cashFlowsAfter.has("CF-EXP-201"))
            val statsAfter = org.json.JSONObject(java.net.URL("http://127.0.0.1:$srvPort/api/sync/realtime_stats").readText())
            org.junit.Assert.assertFalse("EXP-201 must not remain in realtime_stats.expenses", statsAfter.getJSONObject("expenses").has("EXP-201"))
            assertEquals("", statsAfter.getJSONObject("last_expense").getString("lastExpenseId"))
        } finally {
            com.example.service.LocalWebReportServer.stop()
            db.close()
        }

        // 3. Verify deleteExpenseFromRtdb clears last_expense and reconcileExpensesWithRtdb prunes stale Room IDs
        val deletes = java.util.Collections.synchronizedList(mutableListOf<String>())
        val puts = java.util.Collections.synchronizedMap(linkedMapOf<String, String>())
        val mockRtdb = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        mockRtdb.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            when (exchange.requestMethod.uppercase()) {
                "GET" -> {
                    val body = when (path) {
                        "/expenses.json" -> """{"EXP-55":{"id":"EXP-55","localId":55,"title":"Stale Exp","amount":12000,"date":"2026-11-19"},"EXP-1763500000000":{"id":"EXP-1763500000000","title":"Web Exp","amount":8000,"notes":"-","date":"2026-11-19","timestampMillis":1763500000000}}"""
                        "/reports/daily_expenses.json" -> """{"2026-11-19":{"EXP-55":{"id":"EXP-55","amount":12000}}}"""
                        "/realtime_stats.json" -> """{"expenses":{"EXP-55":{"id":"EXP-55","amount":12000}}}"""
                        else -> "null"
                    }.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                "DELETE" -> {
                    deletes.add(path)
                    val ok = "null".toByteArray()
                    exchange.sendResponseHeaders(200, ok.size.toLong())
                    exchange.responseBody.use { it.write(ok) }
                }
                "PUT", "PATCH" -> {
                    puts[path] = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
                    val ok = "{}".toByteArray()
                    exchange.sendResponseHeaders(200, ok.size.toLong())
                    exchange.responseBody.use { it.write(ok) }
                }
                else -> exchange.sendResponseHeaders(200, 0)
            }
        }
        mockRtdb.start()
        try {
            val port = mockRtdb.address.port
            val repo = com.example.data.repository.FirebaseSyncRepository(context)
            val importedList = mutableListOf<com.example.data.model.Expense>()
            kotlinx.coroutines.runBlocking {
                repo.reconcileExpensesWithRtdb(
                    localExpenses = emptyList(),
                    databaseId = "http://127.0.0.1:$port"
                ) { _, parsed ->
                    val saved = parsed.copy(id = 301L)
                    importedList.add(saved)
                    saved
                }
            }
            assertTrue("Stale Room expense EXP-55 should be purged", deletes.contains("/expenses/EXP-55.json"))
            assertTrue("Stale cash_flow CF-EXP-55 should be purged", deletes.contains("/cash_flows/CF-EXP-55.json"))
            assertEquals("Web expense should be imported with blank note instead of '-'", 1, importedList.size)
            assertEquals("", importedList[0].notes)
            assertTrue("Canonical EXP-301 should be written after web import", puts.containsKey("/expenses/EXP-301.json"))
            assertTrue("Old web key EXP-1763500000000 should be cleaned up", deletes.contains("/expenses/EXP-1763500000000.json"))
            assertTrue("realtime_stats/last_expense should be updated", puts.containsKey("/realtime_stats/last_expense.json"))
        } finally {
            mockRtdb.stop(0)
        }

        // 4. Verify web portal templates enforce cleanExpNote & search-independent KPIs
        val tplHtml = com.example.ui.screens.SourceCodeTemplates.ONLINE_REPORT_RTDB_HTML
        assertTrue(tplHtml.contains("function cleanExpNote(raw)"))
        assertTrue(tplHtml.contains("const totalExpPn = sortedAllExps.reduce"))
        val rtdbHtml = java.io.File("src/main/assets/web/laporan-online-rtdb.html")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/assets/web/laporan-online-rtdb.html")
        if (rtdbHtml.exists()) {
            val txt = rtdbHtml.readText()
            assertTrue(txt.contains("function cleanExpNote(raw)"))
            assertTrue(txt.contains("const totalExpense = allPeriodExpenses.reduce"))
        }
        val ownerHtml = java.io.File("src/main/assets/web/owner.html")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/assets/web/owner.html")
        if (ownerHtml.exists()) {
            val txt = ownerHtml.readText()
            assertTrue(txt.contains("function cleanExpNote(raw)"))
            assertTrue(txt.contains("const sumExp = periodExpenses.reduce"))
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify Today transaction report formats TRX as Order and includes Indonesian day name`() {
        val nov19Cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 19, 14, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nov19Millis = nov19Cal.timeInMillis

        assertEquals("Order-20261119-001", com.example.ui.screens.formatOrderLabel("TRX-20261119-001"))
        assertEquals("Order-007", com.example.ui.screens.formatOrderLabel("#TRX-007"))
        assertEquals("Order INV-10", com.example.ui.screens.formatOrderLabel("INV-10"))
        assertEquals("Kamis", com.example.ui.screens.getKpDayName(nov19Millis))
        assertEquals("Kamis, 19 Nov 2026", com.example.ui.screens.formatKpDateShort(nov19Millis))

        val todayStart = com.example.ui.PosViewModel.getStartOfDay(0)
        val todayTrxTime = todayStart + 10 * 3600_000L + 15 * 60_000L
        val expectedTodayDay = com.example.ui.screens.getKpDayName(todayTrxTime)

        val todayTrx = com.example.data.model.Transaction(
            id = 501L,
            invoiceNumber = "TRX-20261119-001",
            cashierId = 1L,
            cashierName = "Admin",
            timestamp = todayTrxTime,
            subtotal = 30000.0,
            grandTotal = 30000.0,
            amountPaid = 50000.0,
            changeAmount = 20000.0,
            profitAmount = 12000.0,
            totalItemsCount = 2
        )

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.screens.TransactionRecordReportSection(
                            allTransactions = listOf(todayTrx),
                            storeProfile = com.example.data.model.StoreProfile(storeName = "Warung Maju"),
                            onDeletePeriod = {},
                            onOpenReceipt = {},
                            onDeleteSingleTransaction = {},
                            initialPeriod = com.example.ui.screens.TransactionRecordPeriod.DAILY,
                            menuOptionTitle = com.example.ui.screens.TransactionReportMenuOption.TODAY.title
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("row_record_nota_TRX-20261119-001").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Order-20261119-001").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("#TRX-20261119-001", substring = true).fetchSemanticsNodes().let { nodes ->
            assertTrue("Did not expect #TRX-20261119-001 in Today report UI", nodes.isEmpty())
        }
        composeTestRule.onAllNodesWithText(expectedTodayDay, substring = true).fetchSemanticsNodes().let { nodes ->
            assertTrue("Expected Indonesian day name '$expectedTodayDay' in Today report UI", nodes.isNotEmpty())
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify operational settings for categories, food and drink porsi or pcs units, and table count`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = com.example.ui.PosViewModel(app)
        vm.resetOperationalSettingsToDefaults()

        assertEquals(listOf("Makanan", "Minuman", "Snack"), vm.configuredCategories.value)
        assertEquals("porsi", vm.foodDefaultUnit.value)
        assertEquals("pcs", vm.drinkDefaultUnit.value)
        assertEquals(5, vm.tableCount.value)
        assertEquals("porsi", vm.defaultUnitForCategory("Makanan"))
        assertEquals("pcs", vm.defaultUnitForCategory("Minuman"))

        assertTrue(vm.addCategory("Dessert"))
        assertTrue(vm.configuredCategories.value.contains("Dessert"))
        assertTrue(vm.renameCategory("Dessert", "Pencuci Mulut"))
        assertTrue(vm.configuredCategories.value.contains("Pencuci Mulut"))
        assertTrue(vm.removeCategory("Pencuci Mulut"))
        assertTrue(!vm.configuredCategories.value.contains("Pencuci Mulut"))

        vm.setFoodDefaultUnit("pcs")
        vm.setDrinkDefaultUnit("porsi")
        assertEquals("pcs", vm.defaultUnitForCategory("Makanan"))
        assertEquals("porsi", vm.defaultUnitForCategory("Minuman"))

        vm.setTableCount(14)
        assertEquals(14, vm.tableCount.value)

        // Verify persistence across new ViewModel instance
        val vmReloaded = com.example.ui.PosViewModel(app)
        assertEquals("pcs", vmReloaded.foodDefaultUnit.value)
        assertEquals("porsi", vmReloaded.drinkDefaultUnit.value)
        assertEquals(14, vmReloaded.tableCount.value)

        // UI verification on StoreOperationalSettingsCardContent
        var uiFoodUnit by mutableStateOf("porsi")
        var uiDrinkUnit by mutableStateOf("pcs")
        var uiTableCount by mutableStateOf(5)
        var uiCategories by mutableStateOf(listOf("Makanan", "Minuman", "Snack"))

        composeTestRule.setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    com.example.ui.screens.StoreOperationalSettingsCardContent(
                        categories = uiCategories,
                        units = listOf("porsi", "pcs", "cup", "botol"),
                        foodDefaultUnit = uiFoodUnit,
                        drinkDefaultUnit = uiDrinkUnit,
                        tableCount = uiTableCount,
                        onAddCategory = { uiCategories = uiCategories + it },
                        onRenameCategory = { _, _ -> },
                        onRemoveCategory = { cat -> uiCategories = uiCategories - cat },
                        onSelectFoodUnit = { uiFoodUnit = it },
                        onApplyFoodUnitToProducts = {},
                        onSelectDrinkUnit = { uiDrinkUnit = it },
                        onApplyDrinkUnitToProducts = {},
                        onAddUnit = {},
                        onRemoveUnit = {},
                        onTableCountChange = { uiTableCount = it },
                        onResetDefaults = {
                            uiCategories = listOf("Makanan", "Minuman", "Snack")
                            uiFoodUnit = "porsi"
                            uiDrinkUnit = "pcs"
                            uiTableCount = 5
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("input_new_category").performScrollTo().performTextInput("Kopi")
        composeTestRule.onNodeWithTag("btn_add_category").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(uiCategories.contains("Kopi"))

        composeTestRule.onNodeWithTag("chip_food_unit_pcs").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("chip_drink_unit_porsi").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("chip_table_preset_15").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("pcs", uiFoodUnit)
        assertEquals("porsi", uiDrinkUnit)
        assertEquals(15, uiTableCount)

        composeTestRule.onNodeWithTag("btn_increase_table_count").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(16, uiTableCount)

        vm.resetOperationalSettingsToDefaults()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify PaymentDialog displays configured tableCount chips and selects Meja 12`() {
        var confirmedTable = ""
        composeTestRule.setContent {
            com.example.ui.screens.PaymentDialog(
                grandTotal = 25000.0,
                initialOrderType = com.example.data.model.OrderType.DINE_IN,
                initialTableNumber = "",
                tableCount = 12,
                onDismiss = {},
                onConfirmPayment = { _, _, _, _, tableNumber, _ ->
                    confirmedTable = tableNumber
                }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payment_options_dropdown").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_table_chips_row").performScrollTo().performScrollToIndex(11)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_table_chip_12").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payment_detail_apply_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("keypad_uang_pas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_payment_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Meja 12", confirmedTable)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `verify ProductFormDialog uses dropdowns for kategori and satuan`() {
        var savedName = ""
        var savedCategory = ""
        var savedUnit = ""

        composeTestRule.setContent {
            com.example.ui.screens.ProductFormDialog(
                title = "Tambah Barang Baru",
                initialProduct = null,
                availableCategories = listOf("Makanan", "Minuman", "Snack"),
                availableUnits = listOf("porsi", "pcs", "cup"),
                defaultUnitForCategory = { cat ->
                    if (cat.equals("Minuman", ignoreCase = true)) "pcs" else "porsi"
                },
                onDismiss = {},
                onSave = { name, cat, _, _, _, _, _, unit ->
                    savedName = name
                    savedCategory = cat
                    savedUnit = unit
                }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("product_name_input").performTextInput("Es Teh Manis")
        composeTestRule.onNodeWithTag("product_category_input").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dropdown_category_option_Minuman").performClick()
        composeTestRule.waitForIdle()

        // Default unit auto-switches to pcs when Minuman is picked, then user can change via Satuan dropdown
        composeTestRule.onNodeWithTag("product_unit_input").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dropdown_unit_option_cup").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("save_product_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Es Teh Manis", savedName)
        assertEquals("Minuman", savedCategory)
        assertEquals("cup", savedUnit)
    }
}

