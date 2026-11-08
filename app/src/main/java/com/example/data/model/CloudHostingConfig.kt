package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas Room Database untuk menyimpan konfigurasi Hosting & Cadangan Cloud
 * sesuai tata letak dan fitur di layar "Hosting & Cadangan Cloud".
 */
@Entity(tableName = "cloud_hosting_config")
data class CloudHostingConfig(
    @PrimaryKey val id: Int = 1,

    // --- 2. Web Laporan Harian Pemilik (Portal Privat) ---
    val useSeparateOwnerProject: Boolean = true,
    val ownerProjectId: String = "laporan-service",
    val ownerDatabaseId: String = "ai-studio-android-kasirpos-6c79f90a-57f2-4e44-8393-a818a10f5d55",
    val ownerHostingUrl: String = "https://ais-dev-wgdowcmyzzxhdk2ko2iu46-852851605479.asia-southeast1.run.app",

    // --- 1. Web Menu Konsumen (Katalog Publik) ---
    val enableConsumerWeb: Boolean = false,
    val consumerProjectId: String = "gen-lang-client-0076807780",
    val consumerDatabaseId: String = "(default)",
    val consumerWebUrl: String = "https://menu-konsumen.web.app",

    // --- 3. Google Drive & Gmail Backup ---
    val enableEmailBackup: Boolean = true,
    val backupEmail: String = "owner@gmail.com",
    val autoBackupOnShiftClose: Boolean = true,
    val lastBackupTimestamp: Long = 0L,

    // --- 4. Web UI Komputer (LAN) ---
    val enableLanServer: Boolean = true,
    val lanPort: Int = 8080,
    val lanIpAddress: String = "192.168.1.100",

    val updatedAt: Long = System.currentTimeMillis()
)
