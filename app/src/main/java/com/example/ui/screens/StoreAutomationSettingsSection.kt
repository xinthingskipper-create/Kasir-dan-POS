package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.AutoStoreCloseManager
import com.example.service.PosNotificationHelper
import com.example.ui.PosViewModel
import java.util.Locale

@Composable
fun StoreAutomationSettingsSection(viewModel: PosViewModel) {
    val context = LocalContext.current

    val isBgServiceEnabled by viewModel.isBackgroundServiceEnabled.collectAsState()
    val isBgServiceRunning by viewModel.isBackgroundServiceRunning.collectAsState()
    val isLowStockNotifEnabled by viewModel.isLowStockNotifEnabled.collectAsState()

    val isAutoCloseEnabled by viewModel.isAutoCloseEnabled.collectAsState()
    val autoCloseHour by viewModel.autoCloseHour.collectAsState()
    val autoCloseMinute by viewModel.autoCloseMinute.collectAsState()
    val lastAutoCloseStatus by viewModel.lastAutoCloseStatus.collectAsState()
    var showAutoCloseTimePicker by remember { mutableStateOf(false) }

    var hasNotifPermission by remember {
        mutableStateOf(PosNotificationHelper.hasNotificationPermission(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = PosNotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            viewModel.ensureBackgroundServiceStarted()
            viewModel.checkLowStockAndNotify(forceAlert = true)
            Toast.makeText(context, "Izin notifikasi diaktifkan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Izin notifikasi ditolak. Aktifkan di Pengaturan Aplikasi Android.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- KARTU 1: LAYANAN LATAR BELAKANG & PUSH NOTIFIKASI STOK ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("background_notification_settings_card")
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
                            color = if (isBgServiceRunning) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isLowStockNotifEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = "Ikon Notifikasi Stok",
                                    tint = if (isBgServiceRunning) Color(0xFF16A34A) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Layanan Latar Belakang & Notifikasi Stok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pantau stok makanan/minuman & sinkron Cloud 24 jam",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBgServiceRunning) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isBgServiceRunning) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isBgServiceRunning) Color(0xFF16A34A) else Color(0xFF94A3B8),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBgServiceRunning) "🟢 Berjalan di Latar Belakang" else "⚪ Berhenti / Tidak Aktif",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBgServiceRunning) Color(0xFF166534) else Color(0xFF475569)
                            )
                        }
                        Text(
                            text = if (isBgServiceRunning) "Foreground Active" else "Standby",
                            fontSize = 10.sp,
                            color = if (isBgServiceRunning) Color(0xFF15803D) else Color(0xFF64748B)
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Izin notifikasi Android 13+ belum aktif",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("btn_request_notif_permission")
                            ) {
                                Text(
                                    "Izinkan Notifikasi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Layanan Latar Belakang Aplikasi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menjaga aplikasi POS, monitor stok, & sinkronisasi Cloud tetap aktif meski diminimalkan.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isBgServiceEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setBackgroundServiceEnabled(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Layanan latar belakang diaktifkan" else "Layanan latar belakang dihentikan",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF16A34A)),
                        modifier = Modifier.testTag("switch_background_service")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Push Notifikasi Stok Makanan & Minuman",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Kirim push notifikasi otomatis saat stok produk menipis (<= batas min) atau habis.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isLowStockNotifEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setLowStockNotifEnabled(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Push notifikasi stok diaktifkan" else "Push notifikasi stok dimatikan",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("switch_low_stock_notif")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.checkLowStockAndNotify(forceAlert = true) { critical, sent ->
                                val msg = when {
                                    critical == 0 -> "Semua stok makanan & minuman saat ini aman."
                                    sent > 0 -> "Push notifikasi dikirim untuk $critical menu kritis!"
                                    else -> "Ditemukan $critical menu kritis, periksa izin notifikasi perangkat."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_test_stock_notif")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Cek dan Kirim Notifikasi Stok",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cek & Kirim Notif Stok Sekarang", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- KARTU 2: TUTUP TOKO OTOMATIS BERJADWAL ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auto_store_close_settings_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isAutoCloseEnabled) Color(0xFFFEF2F2) else Color(0xFFF1F5F9),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockClock,
                                    contentDescription = "Ikon Tutup Toko Otomatis",
                                    tint = if (isAutoCloseEnabled) Color(0xFFDC2626) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tutup Toko Otomatis Terjadwal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Otomatis tutup toko & kirim laporan ke Database pada jam tertentu",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val formattedScheduleTime = AutoStoreCloseManager.formatTime(
                    autoCloseHour,
                    autoCloseMinute
                )
                val nextScheduleText = remember(isAutoCloseEnabled, autoCloseHour, autoCloseMinute, lastAutoCloseStatus) {
                    viewModel.getNextAutoCloseDescription()
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAutoCloseEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isAutoCloseEnabled) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isAutoCloseEnabled) {
                                "🟢 Aktif • Jadwal berikutnya: $nextScheduleText"
                            } else {
                                "⚪ Tutup Toko Otomatis Nonaktif"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoCloseEnabled) Color(0xFF166534) else Color(0xFF475569),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Aktifkan Tutup Toko Otomatis",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mengarsipkan omset & mengirim laporan harian ke Database Laporan secara otomatis setiap hari.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isAutoCloseEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setAutoCloseSchedule(enabled, autoCloseHour, autoCloseMinute)
                            Toast.makeText(
                                context,
                                if (enabled) {
                                    "Tutup toko otomatis dijadwalkan pukul $formattedScheduleTime"
                                } else {
                                    "Tutup toko otomatis dimatikan"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFDC2626)),
                        modifier = Modifier.testTag("switch_auto_close")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = { showAutoCloseTimePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("btn_pick_auto_close_time")
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
                                contentDescription = "Atur Jam Tutup Toko",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Jam Tutup Toko Otomatis",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Pukul $formattedScheduleTime",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 21, 22, 23).forEach { presetHour ->
                        val isPresetSelected = autoCloseHour == presetHour && autoCloseMinute == 0
                        FilterChip(
                            selected = isPresetSelected,
                            onClick = {
                                viewModel.setAutoCloseSchedule(
                                    enabled = isAutoCloseEnabled,
                                    hour = presetHour,
                                    minute = 0
                                )
                                Toast.makeText(
                                    context,
                                    "Jam tutup toko diatur ke ${String.format(Locale.US, "%02d:00", presetHour)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            label = {
                                Text(
                                    text = String.format(Locale.US, "%02d:00", presetHour),
                                    fontSize = 11.sp,
                                    fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("chip_auto_close_${presetHour}00"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (lastAutoCloseStatus.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastAutoCloseStatus,
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAutoCloseTimePicker) {
        AutoCloseTimePickerDialog(
            initialHour = autoCloseHour,
            initialMinute = autoCloseMinute,
            onDismiss = { showAutoCloseTimePicker = false },
            onConfirm = { pickedHour, pickedMinute ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.setAutoCloseSchedule(
                    enabled = true,
                    hour = pickedHour,
                    minute = pickedMinute
                )
                showAutoCloseTimePicker = false
                Toast.makeText(
                    context,
                    "Tutup toko otomatis aktif & dijadwalkan pukul ${AutoStoreCloseManager.formatTime(pickedHour, pickedMinute)}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoCloseTimePickerDialog(
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
                text = "Atur Jam Tutup Toko Otomatis",
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
                    text = "Pada jam ini, toko otomatis ditutup dan laporan dikirim ke Database Laporan.",
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
                modifier = Modifier.testTag("btn_confirm_auto_close_time")
            ) {
                Text("Simpan Jadwal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_auto_close_time")
            ) {
                Text("Batal")
            }
        }
    )
}
