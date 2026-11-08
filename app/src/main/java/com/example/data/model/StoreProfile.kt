package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_profile")
data class StoreProfile(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "KASIR POS NUSANTARA",
    val address: String = "Jl. Merdeka No. 45, Jakarta Pusat",
    val phone: String = "0812-9876-5432",
    val ownerPhone: String = "081298765432",
    val receiptHeader: String = "STRUK PEMBELIAN RESMI",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan."
)
