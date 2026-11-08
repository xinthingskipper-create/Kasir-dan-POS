package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Room Database untuk Laporan Harian (Daily Report) POS.
 * Menyimpan data rekapitulasi penjualan harian, laba, kas laci, dan rincian transaksi
 * secara permanen di SQLite lokal perangkat agar dapat diakses kapan saja secara offline.
 */
@Entity(
    tableName = "daily_reports",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val dateTimestamp: Long = System.currentTimeMillis(),
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalItemsSold: Int = 0,
    val totalCash: Double = 0.0,
    val totalNonCash: Double = 0.0,
    val totalQris: Double = 0.0,
    val totalTransfer: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalTax: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val cashDifference: Double = 0.0,
    val topProductSummary: String = "",
    val cashierNames: String = "",
    val shiftCount: Int = 0,
    val notes: String = "",
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
