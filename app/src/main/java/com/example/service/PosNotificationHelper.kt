package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Product
import java.util.concurrent.ConcurrentHashMap

/**
 * Mengelola saluran notifikasi (Notification Channels), push notifikasi peringatan
 * stok makanan/minuman yang menipis atau habis, serta notifikasi layanan latar belakang.
 */
object PosNotificationHelper {

    const val CHANNEL_LOW_STOCK_ID = "pos_low_stock_alerts"
    const val CHANNEL_BACKGROUND_ID = "pos_background_service"

    const val NOTIFICATION_ID_FOREGROUND = 1001
    const val NOTIFICATION_ID_LOW_STOCK_SUMMARY = 2001

    const val EXTRA_OPEN_TAB = "EXTRA_OPEN_TAB"
    const val TAB_INVENTORY = "INVENTORY"

    // Menyimpan stok terakhir yang sudah dikirimkan notifikasi agar tidak spam berulang untuk nilai stok sama
    private val lastNotifiedStockById = ConcurrentHashMap<Long, Int>()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val stockChannel = NotificationChannel(
                CHANNEL_LOW_STOCK_ID,
                "Peringatan Stok Makanan & Minuman",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notifikasi otomatis saat stok makanan, minuman, atau menu akan habis"
                enableVibration(true)
                enableLights(true)
            }

            val bgChannel = NotificationChannel(
                CHANNEL_BACKGROUND_ID,
                "Layanan Latar Belakang POS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga pemantauan stok dan sinkronisasi data tetap berjalan di latar belakang"
                setShowBadge(false)
            }

            nm.createNotificationChannel(stockChannel)
            nm.createNotificationChannel(bgChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun resetTracking() {
        lastNotifiedStockById.clear()
    }

    fun buildForegroundServiceNotification(
        context: Context,
        storeName: String,
        lowStockCount: Int,
        outOfStockCount: Int,
        syncSummary: String = "Pemantauan stok & sinkron aktif"
    ): Notification {
        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (lowStockCount > 0 || outOfStockCount > 0) {
                putExtra(EXTRA_OPEN_TAB, TAB_INVENTORY)
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, PosBackgroundService::class.java).apply {
            action = PosBackgroundService.ACTION_STOP
            putExtra(PosBackgroundService.EXTRA_FROM_NOTIFICATION, true)
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val checkNowIntent = Intent(context, PosBackgroundService::class.java).apply {
            action = PosBackgroundService.ACTION_CHECK_STOCK
        }
        val checkNowPendingIntent = PendingIntent.getService(
            context,
            103,
            checkNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusLine = when {
            outOfStockCount > 0 && lowStockCount > 0 ->
                "⚠️ $outOfStockCount menu habis, $lowStockCount menu menipis"
            outOfStockCount > 0 ->
                "❌ $outOfStockCount menu stok HABIS — segera restok"
            lowStockCount > 0 ->
                "⚠️ $lowStockCount menu makanan/minuman stok menipis"
            else ->
                "🟢 $syncSummary"
        }

        val cleanStore = storeName.ifBlank { "Kasir POS" }

        return NotificationCompat.Builder(context, CHANNEL_BACKGROUND_ID)
            .setSmallIcon(R.drawable.ic_pos_logo)
            .setContentTitle("$cleanStore • Layanan Latar Belakang")
            .setContentText(statusLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusLine))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Cek Stok", checkNowPendingIntent)
            .addAction(0, "Hentikan", stopPendingIntent)
            .build()
    }

    /**
     * Memeriksa daftar produk dan mengirimkan push notifikasi untuk produk makanan, minuman,
     * atau menu lainnya yang stoknya menipis (<= minStockAlert) atau habis (<= 0).
     *
     * @return Jumlah produk kritis yang memicu notifikasi (0 jika tidak ada perubahan atau tidak ada stok menipis).
     */
    fun notifyLowStockIfNeeded(
        context: Context,
        storeName: String,
        allProducts: List<Product>,
        forceAlert: Boolean = false
    ): Int {
        ensureChannels(context)

        // Bersihkan produk yang sudah direstok di atas batas minimum
        allProducts.forEach { prod ->
            if (!prod.isAvailable || (!prod.isLowStock && !prod.isOutOfStock)) {
                lastNotifiedStockById.remove(prod.id)
            }
        }

        val criticalProducts = allProducts.filter { prod ->
            prod.isAvailable && (prod.isLowStock || prod.isOutOfStock)
        }

        if (criticalProducts.isEmpty()) {
            // Hapus notifikasi lama jika semua stok sudah aman
            try {
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_LOW_STOCK_SUMMARY)
            } catch (_: Throwable) {}
            return 0
        }

        // Tentukan item mana yang perlu memicu notifikasi baru
        val newlyTriggered = if (forceAlert) {
            criticalProducts
        } else {
            criticalProducts.filter { prod ->
                val prevStock = lastNotifiedStockById[prod.id]
                prevStock == null || prod.stock < prevStock
            }
        }

        if (newlyTriggered.isEmpty()) {
            return 0
        }

        if (!hasNotificationPermission(context)) {
            return 0
        }

        // Catat level stok terbaru agar tidak spam berulang untuk nilai yang sama
        criticalProducts.forEach { prod ->
            lastNotifiedStockById[prod.id] = prod.stock
        }

        val openInventoryIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_INVENTORY)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            201,
            openInventoryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val outOfStock = criticalProducts.filter { it.isOutOfStock }
        val lowStock = criticalProducts.filter { it.isLowStock }

        val foodOrDrinkCount = criticalProducts.count {
            it.category.equals("Makanan", ignoreCase = true) ||
                it.category.equals("Minuman", ignoreCase = true)
        }

        val title = when {
            outOfStock.isNotEmpty() && lowStock.isNotEmpty() ->
                "🚨 Stok Habis (${outOfStock.size}) & Menipis (${lowStock.size})"
            outOfStock.isNotEmpty() ->
                "❌ Stok Habis: ${outOfStock.size} Menu"
            foodOrDrinkCount > 0 ->
                "⚠️ Stok Makanan & Minuman Akan Habis (${lowStock.size})"
            else ->
                "⚠️ Peringatan Stok Menipis (${lowStock.size} Menu)"
        }

        val sortedCritical = criticalProducts.sortedWith(compareBy<Product> { it.stock }.thenBy { it.name })
        val firstItem = sortedCritical.first()
        val firstStatus = if (firstItem.stock <= 0) "HABIS" else "sisa ${firstItem.stock} ${firstItem.unit}"

        val shortBody = if (sortedCritical.size == 1) {
            "[${firstItem.category}] ${firstItem.name} $firstStatus (batas min ${firstItem.minStockAlert} ${firstItem.unit}). Segera restok!"
        } else {
            val previewNames = sortedCritical.take(3).joinToString(", ") {
                if (it.stock <= 0) "${it.name} (HABIS)" else "${it.name} (${it.stock})"
            }
            val extra = if (sortedCritical.size > 3) " +${sortedCritical.size - 3} lainnya" else ""
            "$previewNames$extra"
        }

        val cleanStore = storeName.ifBlank { "Kasir POS" }
        val builder = NotificationCompat.Builder(context, CHANNEL_LOW_STOCK_ID)
            .setSmallIcon(R.drawable.ic_pos_logo)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setSubText(cleanStore)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (sortedCritical.size == 1) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(shortBody))
        } else {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText("$cleanStore • Ketuk untuk buka Inventaris")
            sortedCritical.take(6).forEach { prod ->
                val badge = if (prod.stock <= 0) "❌ HABIS" else "⚠️ sisa ${prod.stock} ${prod.unit}"
                inboxStyle.addLine("[${prod.category}] ${prod.name}: $badge")
            }
            if (sortedCritical.size > 6) {
                inboxStyle.addLine("...dan ${sortedCritical.size - 6} menu lainnya")
            }
            builder.setStyle(inboxStyle)
        }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LOW_STOCK_SUMMARY, builder.build())
            newlyTriggered.size
        } catch (_: SecurityException) {
            0
        }
    }
}
