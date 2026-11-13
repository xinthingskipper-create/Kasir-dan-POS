package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.MainTab
import com.example.ui.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: PosViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val connectedPrinter by viewModel.printerManager.connectedDevice.collectAsState()

    val showReceiptDialog by viewModel.receiptDialogVisible.collectAsState()
    val receiptTransaction by viewModel.activeReceiptTransaction.collectAsState()
    val receiptItems by viewModel.activeReceiptItems.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.ensureBackgroundServiceStarted()
            viewModel.checkLowStockAndNotify(forceAlert = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.ensureBackgroundServiceStarted()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !com.example.service.PosNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Role-based tabs
    val tabs = remember(currentUser) {
        if (currentUser?.role == UserRole.ADMIN) {
            listOf(
                MainTab.POS,
                MainTab.INVENTORY,
                MainTab.REPORT,
                MainTab.USERS,
                MainTab.PRINTER,
                MainTab.SETTINGS,
                MainTab.CLOUD_HOSTING
            )
        } else {
            listOf(
                MainTab.POS,
                MainTab.REPORT,
                MainTab.PRINTER
            )
        }
    }

    BackHandler {
        if (currentTab != MainTab.POS) {
            viewModel.selectTab(MainTab.POS)
        } else {
            showLogoutDialog = true
        }
    }

    // Adaptive container: adapts to compact portrait phones vs wide tablets/landscape
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet & Landscape layout: Compact Left NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.width(64.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = currentTab == tab
                                val (icon, label) = getTabIconAndLabel(tab, currentUser?.role)

                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name}")
                                )
                            }
                        }

                        // Logout at bottom of rail
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .size(36.dp)
                                .testTag("rail_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Keluar",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Main Content with compact TopBar
                Scaffold(
                    topBar = {
                        CompactTopAppBar(
                            storeName = storeProfile.storeName,
                            userFullName = currentUser?.fullName ?: "Kasir",
                            userRole = currentUser?.role,
                            connectedPrinter = connectedPrinter != null,
                            isWideScreen = true,
                            onPrinterClick = { viewModel.selectTab(MainTab.PRINTER) },
                            onCloudReportClick = { viewModel.selectTab(MainTab.CLOUD_HOSTING) },
                            onLogoutClick = { showLogoutDialog = true },
                            showLogoutButton = false // Already in rail
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainTabContent(currentTab = currentTab, viewModel = viewModel)
                    }
                }
            }
        } else {
            // Compact Phone Portrait layout: Compact TopBar + Ultra-slim Bottom Navigation Bar (52.dp)
            Scaffold(
                topBar = {
                    CompactTopAppBar(
                        storeName = storeProfile.storeName,
                        userFullName = currentUser?.fullName ?: "Kasir",
                        userRole = currentUser?.role,
                        connectedPrinter = connectedPrinter != null,
                        isWideScreen = false,
                        onPrinterClick = { viewModel.selectTab(MainTab.PRINTER) },
                        onCloudReportClick = { viewModel.selectTab(MainTab.CLOUD_HOSTING) },
                        onLogoutClick = { showLogoutDialog = true },
                        showLogoutButton = true
                    )
                },
                bottomBar = {
                    CompactBottomNavBar(
                        tabs = tabs,
                        currentTab = currentTab,
                        userRole = currentUser?.role,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MainTabContent(currentTab = currentTab, viewModel = viewModel)
                }
            }
        }
    }

    // Receipt Dialog Overlay
    if (showReceiptDialog && receiptTransaction != null) {
        ReceiptDialog(
            viewModel = viewModel,
            transaction = receiptTransaction!!,
            items = receiptItems,
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }

    // Logout Dialog Confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar Akun") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ${currentUser?.fullName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun MainTabContent(currentTab: MainTab, viewModel: PosViewModel) {
    when (currentTab) {
        MainTab.POS -> CashierScreen(viewModel = viewModel)
        MainTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
        MainTab.REPORT -> DailyReportScreen(viewModel = viewModel)
        MainTab.USERS -> UsersManagementScreen(viewModel = viewModel)
        MainTab.PRINTER -> PrinterScreen(viewModel = viewModel)
        MainTab.SETTINGS -> StoreSettingsScreen(viewModel = viewModel)
        MainTab.CLOUD_HOSTING -> CloudHostingScreen(viewModel = viewModel, onBack = { viewModel.selectTab(MainTab.REPORT) })
    }
}

/**
 * Compact, modern, space-efficient bottom navigation bar (52.dp height).
 * Saves vertical screen space while maintaining accessibility (>48dp touch target).
 */
@Composable
private fun CompactBottomNavBar(
    tabs: List<MainTab>,
    currentTab: MainTab,
    userRole: UserRole?,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("compact_bottom_nav_bar")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            val totalRequiredWidth = (tabs.size * 70).dp
            val shouldScroll = maxWidth < totalRequiredWidth

            if (shouldScroll) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        val (icon, label) = getTabIconAndLabel(tab, userRole)

                        Box(
                            modifier = Modifier
                                .width(74.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTabSelected(tab) }
                                .testTag("nav_tab_${tab.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = label,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        val (icon, label) = getTabIconAndLabel(tab, userRole)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTabSelected(tab) }
                                .testTag("nav_tab_${tab.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = label,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ultra-slim, polished TopAppBar (~38dp on phones, ~42dp on tablets) for clean,
 * modern, and un-cluttered header space across all form factors.
 */
@Composable
private fun CompactTopAppBar(
    storeName: String,
    userFullName: String,
    userRole: UserRole?,
    connectedPrinter: Boolean,
    isWideScreen: Boolean,
    onPrinterClick: () -> Unit,
    onCloudReportClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showLogoutButton: Boolean
) {
    val barHeight = if (isWideScreen) 48.dp else 44.dp
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = primaryColor,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.94f),
                            primaryColor
                        )
                    )
                )
                .padding(horizontal = if (isWideScreen) 12.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // --- SISI KIRI: IDENTITAS TOKO & KASIR ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Mini Store Icon Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(if (isWideScreen) 28.dp else 24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isWideScreen) 16.dp else 14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isWideScreen) 8.dp else 6.dp))

                    if (isWideScreen) {
                        // Tablet layout: Store Name + Role Chip + Cashier Name in single elegant row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = storeName.ifBlank { "Kasir Pintar" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Role Chip
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (userRole == UserRole.ADMIN) Color(0xFFF59E0B).copy(alpha = 0.25f)
                                else Color(0xFF38BDF8).copy(alpha = 0.22f),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (userRole == UserRole.ADMIN) Color(0xFFFDE68A).copy(alpha = 0.5f)
                                    else Color(0xFFBAE6FD).copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = if (userRole == UserRole.ADMIN) "ADMIN" else "KASIR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }

                            Text(
                                text = "• $userFullName",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Phone layout: Compact Store Name & Cashier info
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = storeName.ifBlank { "Kasir Pintar" },
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$userFullName • ${if (userRole == UserRole.ADMIN) "Admin" else "Kasir"}",
                                fontSize = 9.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // --- SISI KANAN: STATUS BADGES & AKSI CEPAT ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Web Laporan Shortcut Badge (Pure Realtime DB) - Khusus Admin
                    if (userRole == UserRole.ADMIN) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onCloudReportClick)
                                .testTag("topbar_cloud_report_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    horizontal = if (isWideScreen) 9.dp else 7.dp,
                                    vertical = if (isWideScreen) 4.5.dp else 3.5.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isWideScreen) 14.dp else 12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isWideScreen) "Web Laporan" else "Web",
                                    fontSize = if (isWideScreen) 10.5.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 2. Printer Status Indicator Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(
                            0.5.dp,
                            if (connectedPrinter) Color(0xFF4ADE80).copy(alpha = 0.4f)
                            else Color(0xFFF87171).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onPrinterClick)
                            .testTag("topbar_printer_indicator")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = if (isWideScreen) 9.dp else 6.dp,
                                vertical = if (isWideScreen) 4.5.dp else 3.5.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (connectedPrinter) Color(0xFF4ADE80) else Color(0xFFFCA5A5))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Printer",
                                tint = Color.White,
                                modifier = Modifier.size(if (isWideScreen) 14.dp else 12.dp)
                            )
                            if (isWideScreen) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (connectedPrinter) "Siap" else "Off",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (connectedPrinter) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                )
                            }
                        }
                    }

                    // 3. Optional Logout Action (Phone Only)
                    if (showLogoutButton) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFFFCA5A5).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onLogoutClick)
                                .testTag("topbar_logout_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Keluar",
                                        tint = Color(0xFFFECACA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTabIconAndLabel(tab: MainTab, userRole: UserRole?): Pair<ImageVector, String> {
    return when (tab) {
        MainTab.POS -> Icons.Default.PointOfSale to "Kasir"
        MainTab.INVENTORY -> Icons.Default.Inventory2 to "Inventaris"
        MainTab.REPORT -> (if (userRole == UserRole.ADMIN) Icons.Default.Assessment else Icons.Default.ReceiptLong) to
                (if (userRole == UserRole.ADMIN) "Laporan" else "Riwayat")
        MainTab.USERS -> Icons.Default.People to "Staf"
        MainTab.PRINTER -> Icons.Default.Print to "Printer"
        MainTab.SETTINGS -> Icons.Default.Storefront to "Toko"
        MainTab.CLOUD_HOSTING -> Icons.Default.Cloud to "Hosting"
    }
}
