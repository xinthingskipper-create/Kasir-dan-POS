package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank"),
    DEBIT("Kartu Debit")
}

enum class OrderType(val label: String) {
    DINE_IN("Makan di Tempat"),
    TAKE_AWAY("Di Bungkus")
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val cashierId: Long,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val amountPaid: Double,
    val changeAmount: Double = 0.0,
    val customerName: String = "Pelanggan Umum",
    val orderType: OrderType = OrderType.DINE_IN,
    val tableNumber: String = "",
    val notes: String = "",
    // --- Pelacakan Laporan & Status Sinkronisasi Database Otomatis ---
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val syncError: String? = null,
    val profitAmount: Double = 0.0,
    val totalItemsCount: Int = 0
)
