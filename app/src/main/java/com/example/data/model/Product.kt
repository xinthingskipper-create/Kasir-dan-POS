package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val barcode: String = "",
    val buyPrice: Double, // Harga Modal / HPP
    val sellPrice: Double, // Harga Jual
    val stock: Int,
    val minStockAlert: Int = 5,
    val unit: String = "pcs",
    val description: String = "",
    val isAvailable: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stock <= minStockAlert && stock > 0

    val isOutOfStock: Boolean
        get() = stock <= 0
}
