package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_closings")
data class ShiftClosing(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val closingTime: Long = System.currentTimeMillis(),
    val cashierId: Long,
    val cashierName: String,
    val totalTransactions: Int,
    val totalCash: Double,
    val totalNonCash: Double,
    val totalRevenue: Double,
    val expectedCash: Double,
    val actualCash: Double,
    val difference: Double = actualCash - expectedCash,
    val notes: String = ""
)
