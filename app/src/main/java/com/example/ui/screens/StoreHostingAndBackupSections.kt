package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.repository.BackupStats
import com.example.ui.MainTab
import com.example.ui.PosViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StoreWebHostingSettingsSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val currentFirebaseProjectId by viewModel.firebaseProjectId.collectAsState()
    val currentFirebaseDatabaseId by viewModel.firebaseDatabaseId.collectAsState()
    val currentWebDashboardUrl by viewModel.webDashboardUrl.collectAsState()

    var inputProjectId by remember(currentFirebaseProjectId) { mutableStateOf(currentFirebaseProjectId) }
    var inputDatabaseId by remember(currentFirebaseDatabaseId) { mutableStateOf(currentFirebaseDatabaseId) }
    var inputWebUrl by remember(currentWebDashboardUrl) { mutableStateOf(currentWebDashboardUrl) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("web_hosting_settings_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Hosting & Database Web Owner",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Project ID Firebase, Real-Time Database, & URL Hosting",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.selectTab(MainTab.CLOUD_HOSTING) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_open_cloud_hosting_screen"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Buka Panel Hosting & Cadangan Cloud Lengkap",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputProjectId,
                onValueChange = {
                    inputProjectId = it
                    if (inputWebUrl.startsWith("https://") && inputWebUrl.endsWith(".web.app")) {
                        inputWebUrl = "https://${it.trim()}.web.app"
                    }
                },
                label = { Text("Firebase Project ID Laporan Owner") },
                placeholder = { Text("Contoh: gen-lang-client-0076807780") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF2563EB))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_firebase_project_id"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputDatabaseId,
                onValueChange = { inputDatabaseId = it },
                label = { Text("Real-time Database Instance ID") },
                placeholder = { Text("Contoh: foodrock-report-default-rtdb") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = Color(0xFF059669))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_firebase_database_id"),
                shape = RoundedCornerShape(10.dp)
            )
            Text(
                text = "• Instance Realtime Database default: foodrock-report-default-rtdb",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputWebUrl,
                onValueChange = { inputWebUrl = it },
                label = { Text("URL Hosting Web Laporan") },
                placeholder = { Text("https://<project-id>.web.app") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color(0xFF7C3AED))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_web_hosting_url"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Template URL Cepat:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        val pid = inputProjectId.trim().ifBlank { viewModel.defaultFirebaseProjectId }
                        inputWebUrl = "https://$pid.web.app"
                    }
                ) {
                    Text(
                        text = "Firebase .web.app",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        val pid = inputProjectId.trim().ifBlank { viewModel.defaultFirebaseProjectId }
                        inputWebUrl = "https://$pid.firebaseapp.com"
                    }
                ) {
                    Text(
                        text = "Firebaseapp.com",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        inputWebUrl = viewModel.defaultHostingUrl
                    }
                ) {
                    Text(
                        text = "Server Web Lokal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tautan Aktif Web Laporan Owner:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = inputWebUrl.ifBlank { "Belum diatur" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        maxLines = 1
                    )
                    Text(
                        text = "Project: ${inputProjectId.ifBlank { "-" }}  •  DB: ${inputDatabaseId.ifBlank { "-" }}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateWebHostingConfig(inputProjectId, inputDatabaseId, inputWebUrl)
                        Toast.makeText(
                            context,
                            "Pengaturan hosting web owner berhasil disimpan!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("btn_save_web_hosting")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Hosting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val normalizedInput = com.example.util.BrowserLauncher.normalizeHttpUrl(inputWebUrl)
                            val targetUrl = viewModel.buildReportUrl(
                                rawBaseUrl = normalizedInput,
                                projectId = inputProjectId,
                                databaseId = inputDatabaseId
                            )
                            com.example.util.BrowserLauncher.openInChrome(context, targetUrl)
                        } catch (e: Exception) {
                            Toast.makeText(context, "URL tidak valid: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_test_open_web")
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uji Buka", fontSize = 12.sp)
                }

                IconButton(
                    onClick = {
                        viewModel.resetWebHostingConfig()
                        inputProjectId = viewModel.defaultFirebaseProjectId
                        inputDatabaseId = viewModel.defaultDatabaseId
                        inputWebUrl = viewModel.defaultHostingUrl
                        Toast.makeText(context, "Direset ke default", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Default", tint = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
fun StoreLocalBackupSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var backupResultStats by remember { mutableStateOf<BackupStats?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showManualJsonDialog by remember { mutableStateOf(false) }
    var manualJsonInput by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    val pickJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonContent = viewModel.readBackupJsonFromUri(uri)
                    pendingRestoreJson = jsonContent
                    showRestoreConfirmDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_restore_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Backup & Restore Lokal (.json)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Simpan atau pulihkan cadangan data toko dalam format .json",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "File cadangan (.json) mencakup: seluruh pengaturan toko (kategori, satuan, meja, hosting, jadwal), katalog menu produk, riwayat transaksi kasir, pengeluaran, laporan tutup kasir, dan akun staf.",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isBackingUp = true
                        viewModel.exportLocalBackup(
                            onSuccess = { stats ->
                                isBackingUp = false
                                backupResultStats = stats
                                Toast.makeText(context, "Cadangan berhasil dibuat!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                isBackingUp = false
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isBackingUp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_export_backup_json"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBackingUp) "Memproses..." else "Backup (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { pickJsonLauncher.launch("*/*") },
                    enabled = !isRestoring,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_restore_backup_json"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        manualJsonInput = ""
                        showManualJsonDialog = true
                    },
                    modifier = Modifier.testTag("btn_manual_json_restore")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Atau Tempel Teks JSON Manual", fontSize = 11.sp)
                }
            }
        }
    }

    backupResultStats?.let { stats ->
        AlertDialog(
            onDismissRequest = { backupResultStats = null },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF166534))
                    }
                }
            },
            title = {
                Text(text = "Backup Berhasil!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "File cadangan JSON telah berhasil dibuat:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stats.fileName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Ringkasan Data Tersimpan:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "• ${stats.productCount} Menu & Produk", fontSize = 11.sp, color = Color(0xFF334155))
                    Text(text = "• ${stats.transactionCount} Transaksi Penjualan", fontSize = 11.sp, color = Color(0xFF334155))
                    Text(text = "• ${stats.expenseCount} Catatan Pengeluaran", fontSize = 11.sp, color = Color(0xFF334155))
                    Text(text = "• ${stats.shiftCount} Riwayat Tutup Kasir & ${stats.dailyReportCount} Laporan Harian", fontSize = 11.sp, color = Color(0xFF334155))
                    Text(text = "• ${stats.userCount} Akun Pengguna & Seluruh Pengaturan Toko", fontSize = 11.sp, color = Color(0xFF334155))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val backupFile = File(stats.filePath)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                backupFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Backup Kasir POS - ${stats.fileName}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan File Backup JSON"))
                        } catch (e: Exception) {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, stats.jsonString)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Backup JSON"))
                        }
                    },
                    modifier = Modifier.testTag("btn_share_backup_file")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan / Simpan File")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(stats.jsonString))
                        Toast.makeText(context, "Teks JSON berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        backupResultStats = null
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin JSON")
                }
            }
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
            },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = Color(0xFFB45309))
                    }
                }
            },
            title = { Text(text = "Konfirmasi Pemulihan Data (Restore)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin memulihkan data dari file .json cadangan ini?",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Item dan pengaturan yang sudah Anda edit atau hapus secara lokal TIDAK akan dikembalikan maupun ditimpa. Hanya data yang belum diubah yang akan dipulihkan.",
                        fontSize = 11.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreJson ?: return@Button
                        showRestoreConfirmDialog = false
                        isRestoring = true
                        viewModel.restoreLocalBackup(
                            jsonString = json,
                            onSuccess = { stats ->
                                isRestoring = false
                                pendingRestoreJson = null
                                val protectedNote = if (stats.skippedEditedOrDeletedCount > 0 || stats.skippedSettingsCount > 0) {
                                    " (${stats.skippedEditedOrDeletedCount} item & ${stats.skippedSettingsCount} pengaturan yang diedit/dihapus dipertahankan)"
                                } else {
                                    ""
                                }
                                Toast.makeText(
                                    context,
                                    "Restore Berhasil: ${stats.productCount} Produk, ${stats.transactionCount} Transaksi dipulihkan$protectedNote",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onError = { err ->
                                isRestoring = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    Text("Ya, Pulihkan Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreJson = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showManualJsonDialog) {
        AlertDialog(
            onDismissRequest = { showManualJsonDialog = false },
            title = { Text("Tempel Teks JSON Cadangan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempelkan isi teks JSON cadangan yang telah Anda salin sebelumnya:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = manualJsonInput,
                        onValueChange = { manualJsonInput = it },
                        placeholder = { Text("{\"app\": \"KasirPOS\", ...}", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualJsonInput.isBlank()) {
                            Toast.makeText(context, "Teks JSON tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        } else {
                            showManualJsonDialog = false
                            pendingRestoreJson = manualJsonInput.trim()
                            showRestoreConfirmDialog = true
                        }
                    },
                    enabled = manualJsonInput.isNotBlank()
                ) {
                    Text("Periksa & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualJsonDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
