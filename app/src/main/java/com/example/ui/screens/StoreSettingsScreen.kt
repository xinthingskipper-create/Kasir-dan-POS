package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
fun StoreSettingsScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val storeProfile by viewModel.storeProfile.collectAsState()

    var name by remember(storeProfile) { mutableStateOf(storeProfile.storeName) }
    var address by remember(storeProfile) { mutableStateOf(storeProfile.address) }
    var phone by remember(storeProfile) { mutableStateOf(storeProfile.phone) }
    var ownerPhone by remember(storeProfile) { mutableStateOf(storeProfile.ownerPhone) }
    var footer by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooter) }

    // Web Hosting & Database Configuration States
    val currentFirebaseProjectId by viewModel.firebaseProjectId.collectAsState()
    val currentFirebaseDatabaseId by viewModel.firebaseDatabaseId.collectAsState()
    val currentWebDashboardUrl by viewModel.webDashboardUrl.collectAsState()

    var inputProjectId by remember(currentFirebaseProjectId) { mutableStateOf(currentFirebaseProjectId) }
    var inputDatabaseId by remember(currentFirebaseDatabaseId) { mutableStateOf(currentFirebaseDatabaseId) }
    var inputWebUrl by remember(currentWebDashboardUrl) { mutableStateOf(currentWebDashboardUrl) }

    // Backup & Restore Dialog States
    var backupResultStats by remember { mutableStateOf<BackupStats?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showManualJsonDialog by remember { mutableStateOf(false) }
    var manualJsonInput by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Background Service & Notification States
    val isBgServiceEnabled by viewModel.isBackgroundServiceEnabled.collectAsState()
    val isBgServiceRunning by viewModel.isBackgroundServiceRunning.collectAsState()
    val isLowStockNotifEnabled by viewModel.isLowStockNotifEnabled.collectAsState()

    var hasNotifPermission by remember {
        mutableStateOf(com.example.service.PosNotificationHelper.hasNotificationPermission(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = com.example.service.PosNotificationHelper.hasNotificationPermission(context)
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
            Toast.makeText(context, "Izin notifikasi ditolak. Aktifkan di Pengaturan Aplikasi Android.", Toast.LENGTH_LONG).show()
        }
    }

    // File picker launcher for picking .json file
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- KARTU 1: PROFIL & STRUK TOKO ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pengaturan Profil & Struk Toko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Informasi ini akan dicetak pada struk thermal dan dijadikan tujuan pengiriman laporan ke Owner via WhatsApp.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Toko / Usaha") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("store_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Toko") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor Telepon Toko (Struk)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = { Text("Nomor WhatsApp Owner (Tujuan Laporan)") },
                        placeholder = { Text("Contoh: 081234567890") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("store_owner_phone_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = footer,
                        onValueChange = { footer = it },
                        label = { Text("Catatan Kaki Struk (Footer)") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateStoreProfile(name, address, phone, ownerPhone, footer)
                            Toast.makeText(context, "Profil toko & WhatsApp owner berhasil disimpan", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_store_settings_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Pengaturan Toko", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- KARTU 1B: LAYANAN LATAR BELAKANG & PUSH NOTIFIKASI STOK ---
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

                    // Status Pill Banner
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
                                    Text("Izinkan Notifikasi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle 1: Background Service
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

                    // Toggle 2: Low Stock Push Notifications
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

            // --- KARTU 2: PENGATURAN HOSTING & DATABASE WEB OWNER ---
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.selectTab(com.example.ui.MainTab.CLOUD_HOSTING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("btn_open_cloud_hosting_screen"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                    ) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Panel Hosting & Cadangan Cloud Lengkap", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.5.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Firebase Project ID Input
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

                    // 2. Real-time Database ID Input
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

                    // 3. URL Hosting Web Laporan Input
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

                    // Pilihan Cepat Template URL
                    Text(text = "Template URL Cepat:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable {
                                    val pid = inputProjectId.trim().ifBlank { viewModel.defaultFirebaseProjectId }
                                    inputWebUrl = "https://$pid.web.app"
                                }
                        ) {
                            Text(
                                text = "Firebase .web.app",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable {
                                    val pid = inputProjectId.trim().ifBlank { viewModel.defaultFirebaseProjectId }
                                    inputWebUrl = "https://$pid.firebaseapp.com"
                                }
                        ) {
                            Text(
                                text = "Firebaseapp.com",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable {
                                    inputWebUrl = viewModel.defaultHostingUrl
                                }
                        ) {
                            Text(
                                text = "Server AI Studio",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preview Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                Text(text = "Tautan Aktif Web Laporan Owner:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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

                    // Tombol Aksi: Simpan, Uji Buka, Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateWebHostingConfig(inputProjectId, inputDatabaseId, inputWebUrl)
                                Toast.makeText(context, "Pengaturan hosting web owner berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("btn_save_web_hosting")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Hosting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val targetUrl = if (inputWebUrl.startsWith("http://") || inputWebUrl.startsWith("https://")) {
                                        inputWebUrl
                                    } else {
                                        "https://$inputWebUrl"
                                    }
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "URL tidak valid: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Default", tint = Color(0xFF64748B))
                        }
                    }
                }
            }

            // --- KARTU 3: BACKUP & RESTORE DATA LOKAL (.JSON) ---
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
                        text = "File cadangan (.json) mencakup: seluruh katalog menu produk & harga modal, profil toko, riwayat transaksi kasir, laporan tutup kasir, dan akun staf.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tombol Backup
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
                                .height(46.dp)
                                .testTag("btn_export_backup_json"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBackingUp) "Memproses..." else "Backup (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Tombol Restore dari File
                        OutlinedButton(
                            onClick = {
                                pickJsonLauncher.launch("*/*")
                            },
                            enabled = !isRestoring,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_restore_backup_json"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Manual Paste Option
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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // --- DIALOG 1: HASIL BACKUP SELESAI ---
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
                    Text(text = "• ${stats.shiftCount} Riwayat Tutup Kasir", fontSize = 11.sp, color = Color(0xFF334155))
                    Text(text = "• ${stats.userCount} Akun Pengguna", fontSize = 11.sp, color = Color(0xFF334155))
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
                            // Fallback share text
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

    // --- DIALOG 2: KONFIRMASI RESTORE DATA ---
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
            title = {
                Text(text = "Konfirmasi Pemulihan Data (Restore)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin memulihkan data dari file .json cadangan ini?",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Data toko, menu produk, dan transaksi penjualan yang ada di aplikasi akan diperbarui dengan data dari file cadangan.",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626),
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
                                Toast.makeText(
                                    context,
                                    "Restore Berhasil: ${stats.productCount} Produk, ${stats.transactionCount} Transaksi dipulihkan!",
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

    // --- DIALOG 3: INPUT / TEMPEL TEKS JSON SECARA MANUAL ---
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
                        textStyle = androidx.compose.ui.text.TextStyle(
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
