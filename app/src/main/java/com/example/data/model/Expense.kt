package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas Room Database untuk mencatat Pengeluaran / Biaya Operasional Toko.
 * Digunakan untuk menghitung Laba Bersih = Laba Kotor - Pengeluaran.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "Operasional", // Operasional, Utilitas, Gaji, Sewa, Bahan/Kemasan, Lain-lain
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdBy: String = "Admin"
)
