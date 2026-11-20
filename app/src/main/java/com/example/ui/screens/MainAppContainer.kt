package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.model.UserRole
import com.example.ui.MainTab
import com.example.ui.PosViewModel

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
                MainTab.EXPENSES,
                MainTab.REPORT,
                MainTab.USERS,
                MainTab.PRINTER,
                MainTab.SETTINGS,
                MainTab.CLOUD_HOSTING
            )
        } else {
            listOf(
                MainTab.POS,
                MainTab.EXPENSES,
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

    AdaptiveSlideNavigationContainer(
        tabs = tabs,
        currentTab = currentTab,
        storeName = storeProfile.storeName,
        userFullName = currentUser?.fullName ?: "Kasir",
        userRole = currentUser?.role,
        connectedPrinter = connectedPrinter != null,
        onTabSelected = { viewModel.selectTab(it) },
        onPrinterClick = { viewModel.selectTab(MainTab.PRINTER) },
        onCloudReportClick = { viewModel.selectTab(MainTab.CLOUD_HOSTING) },
        onLogoutClick = { showLogoutDialog = true }
    ) {
        MainTabContent(currentTab = currentTab, viewModel = viewModel)
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
        MainTab.EXPENSES -> ExpenseScreen(viewModel = viewModel)
        MainTab.REPORT -> DailyReportScreen(viewModel = viewModel)
        MainTab.USERS -> UsersManagementScreen(viewModel = viewModel)
        MainTab.PRINTER -> PrinterScreen(viewModel = viewModel)
        MainTab.SETTINGS -> StoreSettingsScreen(viewModel = viewModel)
        MainTab.CLOUD_HOSTING -> CloudHostingScreen(
            viewModel = viewModel,
            onBack = { viewModel.selectTab(MainTab.REPORT) }
        )
    }
}
