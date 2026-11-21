package com.example.ui.screens

import android.app.Activity
import android.app.PendingIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DriveBackupFile
import com.example.service.AutoStoreCloseManager
import com.example.service.DriveBackupFrequency
import com.example.ui.PosViewModel

private enum class PendingDriveAction {
    NONE,
    ENABLE_AUTO_BACKUP,
    BACKUP_NOW,
    FETCH_LIST
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoogleDriveBackupCard(viewModel: PosViewModel) {
    val context = LocalContext.current

    val driveConnected by viewModel.driveConnected.collectAsState()
    val driveEmail by viewModel.driveAccountEmail.collectAsState()
    val autoBackupEnabled by viewModel.driveAutoBackupEnabled.collectAsState()
    val frequency by viewModel.driveBackupFrequency.collectAsState()
    val backupHour by viewModel.driveBackupHour.collectAsState()
    val backupMinute by viewModel.driveBackupMinute.collectAsState()
    val wifiOnly by viewModel.driveBackupWifiOnly.collectAsState()
    val backupOnShiftClose by viewModel.driveBackupOnShiftClose.collectAsState()
    val keepCount by viewModel.driveBackupKeepCount.collectAsState()
    val lastStatus by viewModel.lastDriveBackupStatus.collectAsState()
    val lastFile by viewModel.lastDriveBackupFile.collectAsState()
    val isBackingUp by viewModel.isDriveBackingUp.collectAsState()
    val isRestoring by viewModel.isDriveRestoring.collectAsState()
    val isLoadingFiles by viewModel.isLoadingDriveFiles.collectAsState()
    val driveFiles by viewModel.driveBackupFiles.collectAsState()

    var pendingAction by remember { mutableStateOf(PendingDriveAction.NONE) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDriveFilesDialog by remember { mutableStateOf(false) }
    var filePendingRestore by remember { mutableStateOf<DriveBackupFile?>(null) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleDriveAuthorizationIntent(
                data = activityResult.data,
                onSuccess = { email ->
                    val label = if (email.isNotBlank()) email else "Akun Google"
                    Toast.makeText(context, "Google Drive terhubung ($label)", Toast.LENGTH_SHORT).show()
                    when (pendingAction) {
                        PendingDriveAction.ENABLE_AUTO_BACKUP -> {
                            viewModel.setDriveAutoBackupEnabled(true)
                        }
                        PendingDriveAction.BACKUP_NOW -> {
                            viewModel.backupToGoogleDriveNow(
                                onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() },
                                onNeedsReAuth = {},
                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                            )
                        }
                        PendingDriveAction.FETCH_LIST -> {
                            showDriveFilesDialog = true
                            viewModel.fetchGoogleDriveBackups(
                                onNeedsReAuth = {},
                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                            )
                        }
                        PendingDriveAction.NONE -> Unit
                    }
                    pendingAction = PendingDriveAction.NONE
                },
                onError = { err ->
                    pendingAction = PendingDriveAction.NONE
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            pendingAction = PendingDriveAction.NONE
            Toast.makeText(context, "Izin Google Drive dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPendingIntent(pendingIntent: PendingIntent) {
        try {
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            authLauncher.launch(request)
        } catch (t: Throwable) {
            pendingAction = PendingDriveAction.NONE
            Toast.makeText(context, "Tidak dapat membuka dialog izin Google: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_drive_backup_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (driveConnected) Color(0xFFDCFCE7) else Color(0xFFE0F2FE),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddToDrive,
                                contentDescription = "Ikon Google Drive",
                                tint = if (driveConnected) Color(0xFF15803D) else Color(0xFF0284C7),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Auto Backup Google Drive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cadangkan otomatis semua pengaturan & transaksi ke Google Drive",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (driveConnected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (driveConnected) "Terhubung" else "Belum Terhubung",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (driveConnected) Color(0xFF166534) else Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Account Connection Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (driveConnected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    if (driveConnected) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (driveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (driveConnected) Color(0xFF16A34A) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (driveConnected) "Akun Google Drive Aktif" else "Hubungkan Akun Google Drive",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (driveConnected) Color(0xFF166534) else Color(0xFF334155)
                            )
                            Text(
                                text = if (driveConnected) {
                                    driveEmail.ifBlank { "Izin Google Drive Aktif (Folder: Kasir POS Backup)" }
                                } else {
                                    "Simpan cadangan ke folder \"Kasir POS Backup\" di Drive Anda"
                                },
                                fontSize = 11.sp,
                                color = if (driveConnected) Color(0xFF15803D) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (driveConnected) {
                        OutlinedButton(
                            onClick = {
                                viewModel.disconnectGoogleDrive()
                                Toast.makeText(context, "Koneksi Google Drive diputuskan", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_disconnect_google_drive")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Putuskan Google Drive",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Putuskan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                pendingAction = PendingDriveAction.NONE
                                viewModel.startGoogleDriveAuthorization(
                                    onNeedsResolution = { pi -> launchPendingIntent(pi) },
                                    onAuthorized = { auth ->
                                        val label = auth.email.ifBlank { "Akun Google" }
                                        Toast.makeText(context, "Google Drive terhubung ($label)", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_connect_google_drive")
                        ) {
                            Text("Hubungkan", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(10.dp))

            // Main Toggle: Aktifkan Auto Backup Google Drive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Aktifkan Auto Backup Google Drive",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Otomatis unggah snapshot lengkap pengaturan & seluruh transaksi ke Google Drive.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !driveConnected) {
                            pendingAction = PendingDriveAction.ENABLE_AUTO_BACKUP
                            viewModel.startGoogleDriveAuthorization(
                                onNeedsResolution = { pi -> launchPendingIntent(pi) },
                                onAuthorized = {
                                    pendingAction = PendingDriveAction.NONE
                                    viewModel.setDriveAutoBackupEnabled(true)
                                    Toast.makeText(context, "Auto Backup Google Drive diaktifkan", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    pendingAction = PendingDriveAction.NONE
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            viewModel.setDriveAutoBackupEnabled(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Auto Backup Google Drive aktif (${frequency.label})" else "Auto Backup Google Drive dimatikan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF16A34A)),
                    modifier = Modifier.testTag("switch_drive_auto_backup")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frequency Chips
            Text(
                text = "Frekuensi Auto Backup:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DriveBackupFrequency.entries.forEach { freqItem ->
                    val selected = frequency == freqItem
                    FilterChip(
                        selected = selected,
                        onClick = {
                            viewModel.setDriveBackupFrequency(freqItem)
                            Toast.makeText(context, "Frekuensi: ${freqItem.label}", Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(
                                text = freqItem.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("chip_drive_freq_${freqItem.id}")
                    )
                }
            }
            Text(
                text = "• ${frequency.description}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Time Picker for Daily Scheduled modes
            if (frequency == DriveBackupFrequency.DAILY_SCHEDULED ||
                frequency == DriveBackupFrequency.REALTIME_AND_DAILY
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                val formattedTime = AutoStoreCloseManager.formatTime(backupHour, backupMinute)
                Surface(
                    onClick = { showTimePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("btn_pick_drive_backup_time")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Atur Jam Auto Backup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Jam Jadwal Auto Backup Harian",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pukul $formattedTime WIB",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Ubah Jam",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle: Wi-Fi Only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Hanya Auto Backup via Wi-Fi",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Hemat kuota data seluler saat mengunggah cadangan otomatis.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { viewModel.setDriveBackupWifiOnly(it) },
                    modifier = Modifier.testTag("switch_drive_wifi_only")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Toggle: Backup Otomatis Saat Tutup Shift / Tutup Toko
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Backup Saat Tutup Kasir / Tutup Toko",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Langsung cadangkan pengaturan & transaksi setiap kali kasir menutup shift/toko.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = backupOnShiftClose,
                    onCheckedChange = { viewModel.setDriveBackupOnShiftClose(it) },
                    modifier = Modifier.testTag("switch_drive_on_shift_close")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Retention Limit Chips
            Text(
                text = "Batas Riwayat File Backup di Drive:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5 to "5 File", 10 to "10 File", 20 to "20 File", 0 to "Semua").forEach { (limit, label) ->
                    val selected = keepCount == limit
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setDriveBackupKeepCount(limit) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("chip_drive_keep_$limit")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scope summary card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Data yang dicadangkan otomatis: Profil & Pengaturan Toko, Kategori/Satuan/Meja, Pengaturan Cloud & Jadwal, Katalog Produk, Seluruh Transaksi & Item, Pengeluaran, Tutup Shift, Akun Staf, & Laporan Harian.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (lastStatus.isNotBlank() || lastFile.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (lastStatus.startsWith("❌") || lastStatus.startsWith("⚠️")) Color(0xFFFEF2F2) else Color(0xFFEFF6FF),
                    border = BorderStroke(
                        1.dp,
                        if (lastStatus.startsWith("❌") || lastStatus.startsWith("⚠️")) Color(0xFFFECACA) else Color(0xFFBFDBFE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (lastStatus.isNotBlank()) {
                            Text(
                                text = lastStatus,
                                fontSize = 11.sp,
                                color = if (lastStatus.startsWith("❌") || lastStatus.startsWith("⚠️")) Color(0xFF991B1B) else Color(0xFF1E3A8A)
                            )
                        }
                        if (lastFile.isNotBlank()) {
                            Text(
                                text = "File terbaru: $lastFile",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        pendingAction = PendingDriveAction.BACKUP_NOW
                        viewModel.backupToGoogleDriveNow(
                            onSuccess = { msg ->
                                pendingAction = PendingDriveAction.NONE
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            onNeedsReAuth = { pi -> launchPendingIntent(pi) },
                            onError = { err ->
                                pendingAction = PendingDriveAction.NONE
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isBackingUp && !isRestoring,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_drive_backup_now")
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mengunggah...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup ke Google Drive Sekarang",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup ke Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        pendingAction = PendingDriveAction.FETCH_LIST
                        showDriveFilesDialog = true
                        viewModel.fetchGoogleDriveBackups(
                            onNeedsReAuth = { pi -> launchPendingIntent(pi) },
                            onError = { err ->
                                pendingAction = PendingDriveAction.NONE
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isBackingUp && !isRestoring,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_drive_restore_list")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Pulihkan dari Google Drive",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pulihkan Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Time Picker Dialog for Google Drive Schedule
    if (showTimePicker) {
        DriveBackupTimePickerDialog(
            initialHour = backupHour,
            initialMinute = backupMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                viewModel.setDriveBackupScheduleTime(h, m)
                showTimePicker = false
                Toast.makeText(
                    context,
                    "Jadwal Auto Backup Google Drive diatur ke ${AutoStoreCloseManager.formatTime(h, m)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Dialog Daftar File Backup di Google Drive
    if (showDriveFilesDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) showDriveFilesDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cadangan di Google Drive", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(
                        onClick = {
                            viewModel.fetchGoogleDriveBackups(
                                onNeedsReAuth = { pi -> launchPendingIntent(pi) },
                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                            )
                        },
                        enabled = !isLoadingFiles && !isRestoring
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang Daftar Drive")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Folder: Kasir POS Backup",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when {
                        isLoadingFiles || isRestoring -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isRestoring) "Mengunduh & memulihkan data..." else "Memuat daftar file Drive...",
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                        driveFiles.isEmpty() -> {
                            Text(
                                text = "Belum ada file cadangan di folder \"Kasir POS Backup\" Google Drive Anda.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(driveFiles, key = { it.id }) { file ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = file.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val kb = (file.sizeBytes / 1024L).coerceAtLeast(1L)
                                                val dateLabel = file.modifiedTime.take(16).replace("T", " ")
                                                Text(
                                                    text = "$kb KB • $dateLabel",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Button(
                                                onClick = { filePendingRestore = file },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = "Pulihkan ${file.name}",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pulihkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDriveFilesDialog = false },
                    enabled = !isRestoring
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Konfirmasi Restore dari Google Drive
    filePendingRestore?.let { targetFile ->
        AlertDialog(
            onDismissRequest = { filePendingRestore = null },
            title = { Text("Pulihkan dari Google Drive?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Anda akan memulihkan seluruh pengaturan toko, katalog menu, transaksi, pengeluaran, & laporan dari:",
                        fontSize = 12.sp
                    )
                    Text(
                        text = targetFile.name,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        filePendingRestore = null
                        viewModel.restoreFromGoogleDrive(
                            file = targetFile,
                            onSuccess = { stats ->
                                showDriveFilesDialog = false
                                Toast.makeText(
                                    context,
                                    "Pulih dari Drive: ${stats.productCount} Produk, ${stats.transactionCount} Transaksi, ${stats.expenseCount} Pengeluaran & seluruh pengaturan!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onNeedsReAuth = { pi -> launchPendingIntent(pi) },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    modifier = Modifier.testTag("btn_confirm_drive_restore")
                ) {
                    Text("Ya, Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { filePendingRestore = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriveBackupTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Atur Jam Auto Backup Drive",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Aplikasi akan mencadangkan seluruh pengaturan & transaksi ke Google Drive setiap hari pada jam ini.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(14.dp))
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                modifier = Modifier.testTag("btn_confirm_drive_backup_time")
            ) {
                Text("Simpan Jam", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
