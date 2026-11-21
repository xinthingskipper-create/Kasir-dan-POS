package com.example.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.Product
import com.example.ui.PosViewModel

enum class InventoryFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK
}

@Composable
fun InventoryScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val configuredUnits by viewModel.configuredUnits.collectAsState()
    val foodDefaultUnit by viewModel.foodDefaultUnit.collectAsState()
    val drinkDefaultUnit by viewModel.drinkDefaultUnit.collectAsState()
    val isBgServiceRunning by viewModel.isBackgroundServiceRunning.collectAsState()
    val isLowStockNotifEnabled by viewModel.isLowStockNotifEnabled.collectAsState()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.ensureBackgroundServiceStarted()
            viewModel.checkLowStockAndNotify(forceAlert = true) { critical, sent ->
                if (sent > 0) {
                    Toast.makeText(context, "Push notifikasi terkirim untuk $critical menu!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Izin notifikasi diperlukan untuk push notifikasi stok.", Toast.LENGTH_LONG).show()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentFilter by remember { mutableStateOf(InventoryFilter.ALL) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var stockAdjustProduct by remember { mutableStateOf<Product?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }

    // Summary counts
    val totalCount = allProducts.size
    val lowStockCount = allProducts.count { it.isLowStock }
    val outOfStockCount = allProducts.count { it.isOutOfStock }

    // Filtered products list
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory, currentFilter) {
        allProducts.filter { prod ->
            val matchCategory = selectedCategory == null || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (currentFilter) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW_STOCK -> prod.isLowStock
                InventoryFilter.OUT_OF_STOCK -> prod.isOutOfStock
            }
            matchCategory && matchSearch && matchFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Barang")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
            ) {
            // KPI Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InventoryStatCard(
                    title = "Total Barang",
                    count = totalCount,
                    bgColor = Color(0xFFEFF6FF),
                    textColor = Color(0xFF1E40AF),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentFilter = InventoryFilter.ALL }
                )
                InventoryStatCard(
                    title = "Stok Menipis",
                    count = lowStockCount,
                    bgColor = Color(0xFFFEF3C7),
                    textColor = Color(0xFF92400E),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentFilter = InventoryFilter.LOW_STOCK }
                )
                InventoryStatCard(
                    title = "Stok Habis",
                    count = outOfStockCount,
                    bgColor = Color(0xFFFEE2E2),
                    textColor = Color(0xFF991B1B),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentFilter = InventoryFilter.OUT_OF_STOCK }
                )
            }

            // Live Background Monitor & Push Notification Banner
            val criticalTotal = allProducts.count { it.isAvailable && (it.isLowStock || it.isOutOfStock) }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (criticalTotal > 0) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                border = BorderStroke(
                    1.dp,
                    if (criticalTotal > 0) Color(0xFFFDE68A) else Color(0xFFBBF7D0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("inventory_bg_monitor_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isLowStockNotifEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Status Notifikasi Stok",
                            tint = if (criticalTotal > 0) Color(0xFFD97706) else Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isBgServiceRunning) "🟢 Monitor Latar Belakang Aktif" else "⚪ Monitor Latar Belakang Nonaktif",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (criticalTotal > 0) Color(0xFF92400E) else Color(0xFF166534)
                            )
                            Text(
                                text = if (criticalTotal > 0) {
                                    "$criticalTotal menu makanan/minuman stok menipis atau habis"
                                } else {
                                    "Semua stok makanan & minuman aman"
                                },
                                fontSize = 10.5.sp,
                                color = if (criticalTotal > 0) Color(0xFFB45309) else Color(0xFF15803D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (criticalTotal > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !com.example.service.PosNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.checkLowStockAndNotify(forceAlert = true) { critical, sent ->
                                        val msg = if (sent > 0) {
                                            "Push notifikasi dikirim untuk $critical menu!"
                                        } else {
                                            "Tidak dapat mengirim notifikasi ($critical menu kritis)."
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_inventory_push_notif")
                        ) {
                            Text("Kirim Push Notif", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari inventaris atau SKU...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_search_input")
                )
            }

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && currentFilter == InventoryFilter.ALL,
                        onClick = {
                            selectedCategory = null
                            currentFilter = InventoryFilter.ALL
                        },
                        label = { Text("Semua") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.openStoreSettings(com.example.ui.StoreSettingsCategory.OPERATIONAL) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Atur Kategori & Satuan",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Atur Kategori & Satuan") },
                        modifier = Modifier.testTag("btn_inventory_open_category_settings")
                    )
                }
            }

            // Product List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada barang di kategori ini",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryProductCard(
                            product = product,
                            onEdit = { editingProduct = product },
                            onAdjustStock = { stockAdjustProduct = product },
                            onDelete = { deletingProduct = product }
                        )
                    }
                }
            }
        }
    }
}

    // Add Product Dialog
    if (showAddProductDialog) {
        ProductFormDialog(
            title = "Tambah Barang Baru",
            initialProduct = null,
            availableCategories = categories,
            availableUnits = configuredUnits,
            defaultUnitForCategory = { cat ->
                val norm = cat.trim().lowercase(java.util.Locale.ROOT)
                when {
                    norm.contains("makan") || norm.contains("food") -> foodDefaultUnit
                    norm.contains("minum") || norm.contains("drink") || norm.contains("kopi") || norm.contains("teh") -> drinkDefaultUnit
                    else -> viewModel.defaultUnitForCategory(cat)
                }
            },
            onDismiss = { showAddProductDialog = false },
            onSave = { name, cat, barcode, buyPrice, sellPrice, stock, minStock, unit ->
                viewModel.addProduct(name, cat, barcode, buyPrice, sellPrice, stock, minStock, unit)
                showAddProductDialog = false
            }
        )
    }

    // Edit Product Dialog
    editingProduct?.let { product ->
        ProductFormDialog(
            title = "Edit Data Barang",
            initialProduct = product,
            availableCategories = categories,
            availableUnits = configuredUnits,
            defaultUnitForCategory = { cat -> viewModel.defaultUnitForCategory(cat) },
            onDismiss = { editingProduct = null },
            onSave = { name, cat, barcode, buyPrice, sellPrice, stock, minStock, unit ->
                viewModel.updateProduct(
                    product.copy(
                        name = name,
                        category = cat,
                        barcode = barcode,
                        buyPrice = buyPrice,
                        sellPrice = sellPrice,
                        stock = stock,
                        minStockAlert = minStock,
                        unit = unit
                    )
                )
                editingProduct = null
            }
        )
    }

    // Adjust Stock Dialog
    stockAdjustProduct?.let { product ->
        StockAdjustmentDialog(
            product = product,
            onDismiss = { stockAdjustProduct = null },
            onConfirm = { newStock ->
                viewModel.adjustStock(product.id, newStock)
                stockAdjustProduct = null
            }
        )
    }

    // Delete Confirmation Dialog
    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Hapus Barang") },
            text = { Text("Apakah Anda yakin ingin menghapus '${product.name}' dari inventaris?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        deletingProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun InventoryStatCard(
    title: String,
    count: Int,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

@Composable
private fun InventoryProductCard(
    product: Product,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    val margin = product.sellPrice - product.buyPrice
    val marginPercent = if (product.buyPrice > 0) (margin / product.buyPrice * 100).toInt() else 0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name & Stock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kategori: ${product.category} • SKU/Barcode: ${product.barcode.ifBlank { "-" }}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                val (badgeBg, badgeText, badgeColor) = when {
                    product.isOutOfStock -> Triple(Color(0xFFFEE2E2), "Stok Habis (0)", Color(0xFF991B1B))
                    product.isLowStock -> Triple(Color(0xFFFEF3C7), "Menipis (${product.stock} ${product.unit})", Color(0xFF92400E))
                    else -> Triple(Color(0xFFDCFCE7), "${product.stock} ${product.unit}", Color(0xFF166534))
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price Details: Modal, Jual, Keuntungan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Harga Modal (HPP)", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(text = BluetoothPrinterManager.formatRupiah(product.buyPrice), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Harga Jual", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = BluetoothPrinterManager.formatRupiah(product.sellPrice),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Profit Margin", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = "+${BluetoothPrinterManager.formatRupiah(margin)} ($marginPercent%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAdjustStock) {
                    Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ubah Stok")
                }
                TextButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: Product?,
    availableCategories: List<String> = PosViewModel.DEFAULT_CATEGORIES,
    availableUnits: List<String> = PosViewModel.DEFAULT_UNITS,
    defaultUnitForCategory: (String) -> String = { cat ->
        if (cat.contains("minum", ignoreCase = true)) PosViewModel.DEFAULT_DRINK_UNIT else PosViewModel.DEFAULT_FOOD_UNIT
    },
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double, Int, Int, String) -> Unit
) {
    val defaultInitialCategory = initialProduct?.category ?: availableCategories.firstOrNull() ?: "Makanan"
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(defaultInitialCategory) }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var buyPriceStr by remember { mutableStateOf(initialProduct?.buyPrice?.toInt()?.toString() ?: "") }
    var sellPriceStr by remember { mutableStateOf(initialProduct?.sellPrice?.toInt()?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "10") }
    var minStockStr by remember { mutableStateOf(initialProduct?.minStockAlert?.toString() ?: "5") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: defaultUnitForCategory(defaultInitialCategory)) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var useCustomCategory by remember { mutableStateOf(false) }
    var useCustomUnit by remember { mutableStateOf(false) }

    val categoryOptions = remember(availableCategories, category) {
        val list = availableCategories.toMutableList()
        if (category.isNotBlank() && list.none { it.equals(category, ignoreCase = true) }) {
            list.add(category)
        }
        list
    }
    val unitOptions = remember(availableUnits, unit) {
        val list = availableUnits.toMutableList()
        if (unit.isNotBlank() && list.none { it.equals(unit, ignoreCase = true) }) {
            list.add(unit)
        }
        list
    }

    val dropdownFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dropdown Kategori
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dropdown_product_category")
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Kategori") },
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Kategori"
                                )
                            },
                            colors = dropdownFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryExpanded = true }
                                .testTag("product_category_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoryOptions.forEach { catOption ->
                                val selected = category.equals(catOption, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = catOption,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        category = catOption
                                        useCustomCategory = false
                                        categoryExpanded = false
                                        if (initialProduct == null) {
                                            unit = defaultUnitForCategory(catOption)
                                        }
                                    },
                                    modifier = Modifier.testTag("dropdown_category_option_$catOption")
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "+ Ketik Kategori Lain...",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    useCustomCategory = true
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("dropdown_category_custom")
                            )
                        }
                    }

                    // Dropdown Satuan (porsi/pcs)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dropdown_product_unit")
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Satuan") },
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Satuan"
                                )
                            },
                            colors = dropdownFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { unitExpanded = true }
                                .testTag("product_unit_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            unitOptions.forEach { unitOption ->
                                val selected = unit.equals(unitOption, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = unitOption,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        unit = unitOption
                                        useCustomUnit = false
                                        unitExpanded = false
                                    },
                                    modifier = Modifier.testTag("dropdown_unit_option_$unitOption")
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "+ Ketik Satuan Lain...",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    useCustomUnit = true
                                    unitExpanded = false
                                },
                                modifier = Modifier.testTag("dropdown_unit_custom")
                            )
                        }
                    }
                }

                if (useCustomCategory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Ketik Kategori Kustom") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_custom_category_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (useCustomUnit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Ketik Satuan Kustom") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_custom_unit_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / Kode SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text("Harga Modal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_buy_price_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text("Harga Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sell_price_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stok Sekarang") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it },
                        label = { Text("Peringatan Min.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val buy = buyPriceStr.toDoubleOrNull() ?: 0.0
                            val sell = sellPriceStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toIntOrNull() ?: 0
                            val minStock = minStockStr.toIntOrNull() ?: 5
                            if (name.isNotBlank()) {
                                onSave(name, category, barcode, buy, sell, stock, minStock, unit)
                            }
                        },
                        modifier = Modifier.testTag("save_product_button")
                    ) {
                        Text("Simpan Barang")
                    }
                }
            }
        }
    }
}

@Composable
fun StockAdjustmentDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var newStock by remember { mutableStateOf(product.stock) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Penyesuaian Stok Fisik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = product.name, fontSize = 13.sp, color = Color(0xFF64748B))

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (newStock > 0) newStock-- },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "$newStock", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { newStock++ },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(+5, +10, +25).forEach { add ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.clickable { newStock += add }
                        ) {
                            Text(text = "+$add", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(newStock) }) {
                        Text("Perbarui Stok")
                    }
                }
            }
        }
    }
}
