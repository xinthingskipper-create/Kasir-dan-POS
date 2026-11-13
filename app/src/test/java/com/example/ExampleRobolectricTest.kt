package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.service.PosNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

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
        assertTrue(html.contains("/expenses.json"))
        assertTrue(html.contains("archivedByTutupToko"))
        assertTrue(!html.contains("dailyTrxAgg"))
    }
}
