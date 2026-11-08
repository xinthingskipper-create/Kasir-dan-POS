package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudHostingConfig
import com.example.ui.PosViewModel

enum class CloudCategoryTab(val label: String, val icon: ImageVector) {
    SEMUA("Semua", Icons.Default.GridView),
    SOURCE_CODE("Source Code & Panduan Web", Icons.Default.Code),
    FIREBASE("Firebase Hosting & DB", Icons.Default.LocalFireDepartment),
    LAN("Web UI Komputer (LAN)", Icons.Default.Computer),
    DRIVE_GMAIL("Google Drive & Gmail", Icons.Default.Folder)
}

enum class SourceCodeSubTab(val label: String, val fileName: String, val badge: String) {
    TUTUP_TOKO_WEB("Web Tutup Toko (RTDB Only)", "tutup-toko.html", "Tutup Toko"),
    RTDB_WEB("Web Laporan RTDB Live", "rtdb-laporan.html", "Web RTDB"),
    RTDB_RULES("Rules Realtime DB (Foto)", "database.rules.json", "RTDB Rules"),
    REPORT_TABLES("Tabel Laporan (JSON)", "realtime_database_schema.json", "Skema Tabel"),
    GUIDE("Panduan Setup Firebase", "PANDUAN_SETUP_FIREBASE.md", "Panduan"),
    OWNER_PORTAL("Portal Owner (owner.html)", "owner.html", "Web Laporan"),
    CUSTOMER_WEB("Web Tracking Pelanggan (index.html)", "index.html", "Web Konsumen")
}

/**
 * Layar "Hosting & Cadangan Cloud"
 * Menyimpan konfigurasi ke Room Database (tabel cloud_hosting_config)
 * dengan antarmuka modern bernuansa gelap sesuai tangkapan layar.
 */
@Composable
fun CloudHostingScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val roomConfig by viewModel.cloudHostingConfig.collectAsState()
    val isSyncing by viewModel.isCloudSyncing.collectAsState()

    var selectedTab by remember { mutableStateOf(CloudCategoryTab.SOURCE_CODE) }
    var selectedSourceCodeSubTab by remember { mutableStateOf(SourceCodeSubTab.RTDB_RULES) }

    // State input yang terikat ke entitas Room
    var useSeparateOwnerProject by remember(roomConfig) { mutableStateOf(roomConfig.useSeparateOwnerProject) }
    var ownerProjectId by remember(roomConfig) { mutableStateOf(roomConfig.ownerProjectId) }
    var ownerDatabaseId by remember(roomConfig) { mutableStateOf(roomConfig.ownerDatabaseId) }
    var ownerHostingUrl by remember(roomConfig) { mutableStateOf(roomConfig.ownerHostingUrl) }

    var enableConsumerWeb by remember(roomConfig) { mutableStateOf(roomConfig.enableConsumerWeb) }
    var consumerProjectId by remember(roomConfig) { mutableStateOf(roomConfig.consumerProjectId) }
    var consumerDatabaseId by remember(roomConfig) { mutableStateOf(roomConfig.consumerDatabaseId) }
    var consumerWebUrl by remember(roomConfig) { mutableStateOf(roomConfig.consumerWebUrl) }

    var enableEmailBackup by remember(roomConfig) { mutableStateOf(roomConfig.enableEmailBackup) }
    var backupEmail by remember(roomConfig) { mutableStateOf(roomConfig.backupEmail) }
    var autoBackupOnShiftClose by remember(roomConfig) { mutableStateOf(roomConfig.autoBackupOnShiftClose) }

    var enableLanServer by remember(roomConfig) { mutableStateOf(roomConfig.enableLanServer) }
    var lanPort by remember(roomConfig) { mutableStateOf(roomConfig.lanPort.toString()) }

    // Warna tema gelap sesuai tangkapan layar
    val bgDark = Color(0xFF0F121B)
    val cardDark = Color(0xFF161B26)
    val cardBorder = Color(0xFF263045)
    val innerDark = Color(0xFF11141E)
    val textWhite = Color(0xFFF8FAFC)
    val textGray = Color(0xFF94A3B8)
    val flameOrange = Color(0xFFEA580C)
    val activeCyan = Color(0xFF0284C7)

    fun saveToRoom() {
        val updated = roomConfig.copy(
            useSeparateOwnerProject = useSeparateOwnerProject,
            ownerProjectId = ownerProjectId.trim(),
            ownerDatabaseId = ownerDatabaseId.trim(),
            ownerHostingUrl = ownerHostingUrl.trim(),
            enableConsumerWeb = enableConsumerWeb,
            consumerProjectId = consumerProjectId.trim(),
            consumerDatabaseId = consumerDatabaseId.trim(),
            consumerWebUrl = consumerWebUrl.trim(),
            enableEmailBackup = enableEmailBackup,
            backupEmail = backupEmail.trim(),
            autoBackupOnShiftClose = autoBackupOnShiftClose,
            enableLanServer = enableLanServer,
            lanPort = lanPort.toIntOrNull() ?: 8080,
            updatedAt = System.currentTimeMillis()
        )
        viewModel.saveCloudHostingConfig(updated)
        try {
            Toast.makeText(context, "✅ Konfigurasi tersimpan di Room Database!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Top Header (Ultra-slim, neat, responsive) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2433),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A364F)),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBack() }
                                .testTag("btn_back_cloud_hosting")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = textWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hosting & Cadangan Cloud",
                                fontWeight = FontWeight.Bold,
                                color = textWhite,
                                fontSize = 14.5.sp
                            )
                            Text(
                                text = "Firebase RTDB, Web Dashboard, Drive & Gmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Badge: 🔥 RTDB Live
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C2D12).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, flameOrange.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RTDB Live",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDBA74)
                            )
                        }
                    }
                }
            }

            // --- 2. Category Tabs (Compact Pill Buttons) ---
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CloudCategoryTab.values()) { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) flameOrange else Color(0xFF1E2433),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isSelected) flameOrange else Color(0xFF2A364F)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = tab }
                                .testTag("tab_cloud_${tab.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else textGray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textGray
                                )
                            }
                        }
                    }
                }
            }

            // --- Menu Source Code & Panduan Web Hosting (Screenshot Match) ---
            if (selectedTab == CloudCategoryTab.SOURCE_CODE || selectedTab == CloudCategoryTab.SEMUA) {
                item {
                    SourceCodeViewerCard(
                        selectedSubTab = selectedSourceCodeSubTab,
                        onSelectSubTab = { selectedSourceCodeSubTab = it },
                        ownerHostingUrl = ownerHostingUrl,
                        ownerProjectId = ownerProjectId,
                        ownerDatabaseId = ownerDatabaseId,
                        cardDark = cardDark,
                        cardBorder = cardBorder,
                        innerDark = innerDark,
                        textWhite = textWhite,
                        textGray = textGray,
                        flameOrange = flameOrange,
                        activeCyan = activeCyan
                    )
                }
            }

            // --- 3. Card: 2. Web Laporan Harian Pemilik (Screenshot Primary Card) ---
            if (selectedTab == CloudCategoryTab.SEMUA || selectedTab == CloudCategoryTab.FIREBASE) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_web_laporan_owner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Card Header: Icon, Title, Subtitle, Portal Privat Badge (Compact)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E3A8A),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Column {
                                        Text(
                                            text = "2. Web Laporan Harian Pemilik",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textWhite
                                        )
                                        Text(
                                            text = "Portal privat omset, laba, kas laci, & audit toko",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textGray,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Portal Privat",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Inner Surface: Switch Section
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = innerDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF202738))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Gunakan Project / Database Terpisah untuk Owner",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textWhite
                                        )
                                        Text(
                                            text = "Aktifkan jika ingin memisahkan database laporan owner dari web konsumen",
                                            fontSize = 11.sp,
                                            color = textGray
                                        )
                                    }
                                    Switch(
                                        checked = useSeparateOwnerProject,
                                        onCheckedChange = { useSeparateOwnerProject = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = activeCyan,
                                            uncheckedThumbColor = Color(0xFF64748B),
                                            uncheckedTrackColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.testTag("switch_separate_owner_project")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Preset Chip: foodrock-report (Database dari Foto)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Konfigurasi Database Cloud:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textGray
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                    modifier = Modifier
                                        .clickable {
                                            ownerProjectId = "foodrock-report"
                                            ownerDatabaseId = "foodrock-report-default-rtdb"
                                            ownerHostingUrl = "https://foodrock-report.web.app"
                                            Toast.makeText(context, "✅ Preset foodrock-report diterapkan!", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("btn_preset_foodrock")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Gunakan foodrock-report (Foto)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE0F2FE)
                                        )
                                    }
                                }
                            }

                            // Input 1: Firebase Project ID Laporan Owner (Exact Screenshot Design)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = ownerProjectId,
                                    onValueChange = { ownerProjectId = it },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textWhite
                                    ),
                                    placeholder = { Text("laporan-service", color = Color(0xFF475569), fontSize = 18.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_owner_project_id"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite,
                                        unfocusedTextColor = textWhite,
                                        focusedContainerColor = innerDark,
                                        unfocusedContainerColor = innerDark,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF263045)
                                    )
                                )

                                // Floating Label Badge resting on top border matching screenshot
                                Surface(
                                    modifier = Modifier
                                        .padding(start = 14.dp)
                                        .offset(y = (-9).dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E2436),
                                    border = BorderStroke(1.dp, Color(0xFF2D374E))
                                ) {
                                    Text(
                                        text = "Firebase Project ID Laporan Owner",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Input 2: Real-time Database Instance ID (Floating Label)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = ownerDatabaseId,
                                    onValueChange = { ownerDatabaseId = it },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textWhite
                                    ),
                                    placeholder = { Text("contoh: foodrock-report-default-rtdb", color = Color(0xFF475569), fontSize = 14.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_owner_database_id"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite,
                                        unfocusedTextColor = textWhite,
                                        focusedContainerColor = innerDark,
                                        unfocusedContainerColor = innerDark,
                                        focusedBorderColor = activeCyan,
                                        unfocusedBorderColor = Color(0xFF263045)
                                    )
                                )

                                Surface(
                                    modifier = Modifier
                                        .padding(start = 14.dp)
                                        .offset(y = (-9).dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E2436),
                                    border = BorderStroke(1.dp, Color(0xFF2D374E))
                                ) {
                                    Text(
                                        text = "Firebase Realtime Database ID",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Real-time Database Live Status & Structure Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0C1A2E),
                                border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Real-time Database Sinkron Aktif",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6EE7B7)
                                            )
                                        }
                                        Text(
                                            text = "Node: /transactions, /shift_reports",
                                            fontSize = 10.sp,
                                            color = Color(0xFF93C5FD)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Setiap penjualan di kasir otomatis tersinkron real-time ke project $ownerProjectId tanpa delay.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Input 3: URL Hosting Web Laporan Owner
                            Text(
                                text = "URL Hosting Web Laporan Owner",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textGray,
                                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = ownerHostingUrl,
                                onValueChange = { ownerHostingUrl = it },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                placeholder = { Text("https://laporan-service.web.app", color = Color(0xFF475569)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_owner_hosting_url"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite,
                                    unfocusedTextColor = textWhite,
                                    focusedContainerColor = innerDark,
                                    unfocusedContainerColor = innerDark,
                                    focusedBorderColor = activeCyan,
                                    unfocusedBorderColor = Color(0xFF263045)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons inside Owner Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { saveToRoom() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_save_owner_config"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan ke Room DB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val fullUrl = "$ownerHostingUrl?projectId=$ownerProjectId&databaseId=$ownerDatabaseId"
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Tidak dapat membuka browser", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_open_owner_portal"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buka Web Owner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.testCloudConnection { ok, msg ->
                                            try {
                                                Toast.makeText(context, msg, if (ok) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                // ignore toast error
                                            }
                                        }
                                    },
                                    enabled = !isSyncing,
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("btn_test_connection_cloud_hosting"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1))
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cek Koneksi", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.syncAllToCloud { success, msg ->
                                            try {
                                                Toast.makeText(context, msg ?: "Sinkron selesai", if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                // ignore toast error
                                            }
                                        }
                                    },
                                    enabled = !isSyncing,
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("btn_sync_now_cloud"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSyncing) "Sinkron..." else "Sinkron", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val fullUrl = "$ownerHostingUrl?projectId=$ownerProjectId&databaseId=$ownerDatabaseId"
                                            clipboardManager.setText(AnnotatedString(fullUrl))
                                            Toast.makeText(context, "Link Web Owner disalin!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // Safe ignore
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_copy_owner_link"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salin Link Web", fontSize = 11.sp, color = textWhite)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val rulesText = "{\n  \"rules\": {\n    \"transactions\": { \".read\": \"auth != null\", \".write\": \"auth != null\" },\n    \"shift_reports\": { \".read\": \"auth != null\", \".write\": \"auth != null\" },\n    \"realtime_stats\": { \"today\": { \".read\": \"true\", \".write\": \"auth != null\" } }\n  }\n}"
                                            clipboardManager.setText(AnnotatedString(rulesText))
                                            Toast.makeText(context, "Aturan database.rules.json disalin!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // Safe ignore
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_copy_db_rules"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salin Rules DB", fontSize = 11.sp, color = textWhite)
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Card: 1. Web Menu & Pemesanan Konsumen ---
            if (selectedTab == CloudCategoryTab.SEMUA || selectedTab == CloudCategoryTab.FIREBASE) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF065F46),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.RestaurantMenu,
                                                contentDescription = null,
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Column {
                                        Text(
                                            text = "1. Web Menu Konsumen & Katalog Online",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textWhite
                                        )
                                        Text(
                                            text = "Katalog publik online menu makanan & minuman untuk pelanggan",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textGray,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "Portal Publik",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = innerDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Aktifkan Web Katalog Menu Konsumen",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textWhite
                                    )
                                    Switch(
                                        checked = enableConsumerWeb,
                                        onCheckedChange = { enableConsumerWeb = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF059669)
                                        )
                                    )
                                }
                            }

                            if (enableConsumerWeb) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = consumerWebUrl,
                                    onValueChange = { consumerWebUrl = it },
                                    label = { Text("URL Web Menu Konsumen") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite,
                                        unfocusedTextColor = textWhite,
                                        focusedContainerColor = innerDark,
                                        unfocusedContainerColor = innerDark
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // --- 5. Card: 3. Cadangan Cloud Google Drive & Gmail ---
            if (selectedTab == CloudCategoryTab.SEMUA || selectedTab == CloudCategoryTab.DRIVE_GMAIL) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF701A75),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = Color(0xFFE879F9),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "3. Cadangan Google Drive & Gmail",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textWhite
                                        )
                                        Text(
                                            text = "Kirim otomatis laporan omset harian dan backup .json ke email",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textGray
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF701A75).copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "Cadangan Otomatis",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF5D0FE),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = innerDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Kirim Laporan Otomatis ke Gmail Tiap Tutup Kasir",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textWhite
                                    )
                                    Switch(
                                        checked = enableEmailBackup,
                                        onCheckedChange = { enableEmailBackup = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFC026D3)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = backupEmail,
                                onValueChange = { backupEmail = it },
                                label = { Text("Email Penerima Laporan") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFE879F9))
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite,
                                    unfocusedTextColor = textWhite,
                                    focusedContainerColor = innerDark,
                                    unfocusedContainerColor = innerDark
                                )
                            )
                        }
                    }
                }
            }

            // --- 6. Card: 4. Web UI Komputer (LAN) ---
            if (selectedTab == CloudCategoryTab.SEMUA || selectedTab == CloudCategoryTab.LAN) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF831843),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Computer,
                                                contentDescription = null,
                                                tint = Color(0xFFF472B6),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "4. Server Web UI Jaringan Lokal (LAN)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textWhite
                                        )
                                        Text(
                                            text = "Akses kasir dari browser laptop/komputer dalam 1 Wi-Fi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textGray
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF831843).copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "Server LAN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBCFE8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = innerDark
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "URL Akses di Komputer / Laptop:",
                                        fontSize = 11.sp,
                                        color = textGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "http://${roomConfig.lanIpAddress}:$lanPort",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/**
 * Card khusus penampil Source Code & Panduan Web Hosting
 * Menampilkan sub-tab horizontal dan kotak kode gelap sesuai screenshot.
 */
@Composable
fun SourceCodeViewerCard(
    selectedSubTab: SourceCodeSubTab,
    onSelectSubTab: (SourceCodeSubTab) -> Unit,
    ownerHostingUrl: String,
    ownerProjectId: String,
    ownerDatabaseId: String,
    cardDark: Color,
    cardBorder: Color,
    innerDark: Color,
    textWhite: Color,
    textGray: Color,
    flameOrange: Color,
    activeCyan: Color
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val codeContent = when (selectedSubTab) {
        SourceCodeSubTab.TUTUP_TOKO_WEB -> SourceCodeTemplates.TUTUP_TOKO_HTML
        SourceCodeSubTab.RTDB_WEB -> SourceCodeTemplates.RTDB_LAPORAN_HTML
        SourceCodeSubTab.RTDB_RULES -> SourceCodeTemplates.REALTIME_DATABASE_RULES
        SourceCodeSubTab.REPORT_TABLES -> SourceCodeTemplates.REPORT_TABLES_SCHEMA
        SourceCodeSubTab.GUIDE -> SourceCodeTemplates.FIREBASE_SETUP_GUIDE
        SourceCodeSubTab.OWNER_PORTAL -> SourceCodeTemplates.OWNER_PORTAL_HTML
        SourceCodeSubTab.CUSTOMER_WEB -> SourceCodeTemplates.CUSTOMER_TRACKING_HTML
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_source_code_viewer"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark),
        border = BorderStroke(1.5.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // --- Sub-Tab Bar matching user's screenshot (Compact) ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("row_source_code_tabs"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(SourceCodeSubTab.values()) { subTab ->
                    val isSelected = selectedSubTab == subTab
                    Column(
                        modifier = Modifier
                            .clickable { onSelectSubTab(subTab) }
                            .padding(top = 2.dp)
                            .testTag("subtab_${subTab.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = subTab.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) flameOrange else textGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .height(2.5.dp)
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFA78BFA),
                                        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.5.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Code Box Container ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF090D16),
                border = BorderStroke(1.dp, Color(0xFF1E2638))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header of the code container: File badge + Copy & Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedSubTab.fileName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E2436)
                            ) {
                                Text(
                                    text = selectedSubTab.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Salin Kode Button
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(codeContent))
                                    try {
                                        Toast.makeText(
                                            context,
                                            "✅ Kode ${selectedSubTab.fileName} disalin ke clipboard!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        // safe ignore
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFF1F5F9)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_copy_source_code")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Kode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Preview Button for HTML portals
                            if (selectedSubTab == SourceCodeSubTab.TUTUP_TOKO_WEB || selectedSubTab == SourceCodeSubTab.RTDB_WEB || selectedSubTab == SourceCodeSubTab.OWNER_PORTAL || selectedSubTab == SourceCodeSubTab.CUSTOMER_WEB) {
                                Button(
                                    onClick = {
                                        val base = if (ownerHostingUrl.startsWith("http")) {
                                            ownerHostingUrl.removeSuffix("/").removeSuffix("/owner.html").removeSuffix("/index.html").removeSuffix("/rtdb-laporan.html").removeSuffix("/tutup-toko.html")
                                        } else {
                                            "http://localhost:3000"
                                        }
                                        val fileName = when (selectedSubTab) {
                                            SourceCodeSubTab.TUTUP_TOKO_WEB -> "tutup-toko.html"
                                            SourceCodeSubTab.RTDB_WEB -> "rtdb-laporan.html"
                                            SourceCodeSubTab.OWNER_PORTAL -> "owner.html"
                                            else -> "index.html"
                                        }
                                        val urlToOpen = "$base/$fileName"
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "URL Web: $urlToOpen", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7C2D12),
                                        contentColor = Color(0xFFFED7AA)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_preview_portal")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        tint = Color(0xFFF97316),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buka Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Code Viewer Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 460.dp)
                            .background(Color(0xFF05070D), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF161E2E)), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = codeContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

