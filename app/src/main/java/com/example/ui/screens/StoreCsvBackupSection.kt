package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.PosViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StoreCsvBackupSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var csvExportStats by remember { mutableStateOf<BackupStats?>(null) }
    var pendingRestoreCsv by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showManualCsvDialog by remember { mutableStateOf(false) }
    var manualCsvInput by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    val pickCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = viewModel.readBackupJsonFromUri(uri)
                    pendingRestoreCsv = content
                    showRestoreConfirmDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca file CSV: ${e.message}", Toast.LENGTH_LONG).show()
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
            .testTag("csv_backup_restore_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "Ikon Backup CSV Offline",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Backup & Restore Offline (.csv)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ekspor & pulihkan pengaturan + transaksi tanpa internet, siap dibagikan ke Excel/WA",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Format .csv universal (kompatibel Excel & Google Sheets) mencakup seluruh pengaturan toko, kategori/satuan/meja, hosting, jadwal otomatis, produk, transaksi & item, pengeluaran, tutup shift, akun staf, & laporan harian.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF166534),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isExporting = true
                        viewModel.exportOfflineCsvBackup(
                            onSuccess = { stats ->
                                isExporting = false
                                csvExportStats = stats
                                Toast.makeText(
                                    context,
                                    "Backup CSV offline siap dibagikan!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { error ->
                                isExporting = false
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isExporting && !isRestoring,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_export_backup_csv")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Membuat CSV...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Backup & Bagikan CSV Offline",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup & Bagikan CSV", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { pickCsvLauncher.launch("*/*") },
                    enabled = !isExporting && !isRestoring,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_restore_backup_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Pilih File CSV untuk Restore",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore (.csv)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        manualCsvInput = ""
                        showManualCsvDialog = true
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_manual_csv_restore")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Atau Tempel Teks CSV Manual", fontSize = 11.sp)
                }
            }
        }
    }

    // Dialog hasil backup CSV siap dibagikan
    csvExportStats?.let { stats ->
        AlertDialog(
            onDismissRequest = { csvExportStats = null },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF166534))
                    }
                }
            },
            title = {
                Text(
                    text = "Backup CSV Offline Berhasil!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "File CSV cadangan siap disimpan atau dibagikan offline/online:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Ringkasan Data Tersimpan di CSV:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "• ${stats.productCount} Menu & Produk", fontSize = 11.sp)
                    Text(text = "• ${stats.transactionCount} Transaksi Penjualan", fontSize = 11.sp)
                    Text(text = "• ${stats.expenseCount} Catatan Pengeluaran", fontSize = 11.sp)
                    Text(text = "• ${stats.shiftCount} Riwayat Tutup Shift & ${stats.dailyReportCount} Laporan Harian", fontSize = 11.sp)
                    Text(text = "• ${stats.userCount} Akun Staf & Seluruh Pengaturan Toko", fontSize = 11.sp)
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
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Backup CSV Kasir POS - ${stats.fileName}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Bagikan / Simpan File Backup CSV")
                            )
                        } catch (e: Exception) {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, stats.csvString)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Backup CSV"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    modifier = Modifier.testTag("btn_share_csv_file")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan File CSV")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(stats.csvString))
                        Toast.makeText(context, "Teks CSV berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        csvExportStats = null
                    },
                    modifier = Modifier.testTag("btn_copy_csv_text")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin CSV")
                }
            }
        )
    }

    // Dialog konfirmasi restore dari file CSV
    if (showRestoreConfirmDialog && pendingRestoreCsv != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreCsv = null
            },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = Color(0xFFB45309))
                    }
                }
            },
            title = { Text(text = "Konfirmasi Restore dari CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin memulihkan seluruh pengaturan dan transaksi dari file .csv cadangan ini?",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Item dan pengaturan yang sudah Anda edit atau hapus secara lokal TIDAK akan dikembalikan maupun ditimpa. Hanya data/pengaturan yang belum diubah yang akan dipulihkan.",
                        fontSize = 11.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val csv = pendingRestoreCsv ?: return@Button
                        showRestoreConfirmDialog = false
                        isRestoring = true
                        viewModel.restoreOfflineCsvBackup(
                            csvString = csv,
                            onSuccess = { stats ->
                                isRestoring = false
                                pendingRestoreCsv = null
                                val protectedNote = if (stats.skippedEditedOrDeletedCount > 0 || stats.skippedSettingsCount > 0) {
                                    " (${stats.skippedEditedOrDeletedCount} item & ${stats.skippedSettingsCount} pengaturan yang diedit/dihapus dipertahankan)"
                                } else {
                                    ""
                                }
                                Toast.makeText(
                                    context,
                                    "Restore CSV Berhasil: ${stats.productCount} Produk, ${stats.transactionCount} Transaksi dipulihkan$protectedNote",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onError = { err ->
                                isRestoring = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.testTag("btn_confirm_csv_restore")
                ) {
                    Text("Ya, Pulihkan CSV")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreCsv = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog tempel teks CSV secara manual
    if (showManualCsvDialog) {
        AlertDialog(
            onDismissRequest = { showManualCsvDialog = false },
            title = { Text("Tempel Teks CSV Cadangan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempelkan isi teks CSV cadangan KasirPOS yang telah Anda salin sebelumnya:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualCsvInput,
                        onValueChange = { manualCsvInput = it },
                        placeholder = { Text("META_HEADER,app,version,...", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("input_manual_csv_text"),
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
                        if (manualCsvInput.isBlank()) {
                            Toast.makeText(context, "Teks CSV tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        } else {
                            showManualCsvDialog = false
                            pendingRestoreCsv = manualCsvInput.trim()
                            showRestoreConfirmDialog = true
                        }
                    },
                    enabled = manualCsvInput.isNotBlank(),
                    modifier = Modifier.testTag("btn_submit_manual_csv")
                ) {
                    Text("Periksa & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualCsvDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
