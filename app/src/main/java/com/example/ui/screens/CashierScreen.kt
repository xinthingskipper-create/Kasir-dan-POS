package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.UserRole
import com.example.ui.CartItem
import com.example.ui.MainTab
import com.example.ui.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun CashierScreen(viewModel: PosViewModel) {
    val context = LocalContext.current

    val allProducts by viewModel.allProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val cart by viewModel.cart.collectAsState()
    val cartCount by viewModel.cartItemCount.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val isTaxEnabled by viewModel.isTaxEnabled.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showTutupKasirDialog by remember { mutableStateOf(false) }
    var orderType by remember { mutableStateOf(OrderType.DINE_IN) }
    var tableNumber by remember { mutableStateOf("") }

    // Filter products
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { prod ->
            val matchCategory = selectedCategory == null || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val netTotal = (cartSubtotal - discountAmount).coerceAtLeast(0.0)
    val taxValue = if (isTaxEnabled) netTotal * 0.11 else 0.0
    val grandTotal = netTotal + taxValue

    var phoneViewMode by remember { mutableStateOf("MENU") } // "MENU", "CART", "SPLIT"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val isUltraNarrow = maxWidth < 400.dp

        if (isWideScreen || phoneViewMode == "SPLIT") {
            // Tampilan 2 Kolom Berdampingan (Tablet / Layar Lebar Landscape, atau Mode Split pada HP)
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isWideScreen) {
                    // Quick Switcher header on phone when in split mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode 2 Kolom (Split)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { phoneViewMode = "MENU" },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Beralih ke Tab Tunggal", fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // KIRI: KATALOG MENU
                    CashierMenuCatalogPane(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategoryChange = { viewModel.setCategory(it) },
                        filteredProducts = filteredProducts,
                        cart = cart,
                        onAddToCart = { product ->
                            if (product.isOutOfStock) {
                                Toast.makeText(context, "Stok '${product.name}' sudah habis!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addToCart(product)
                            }
                        },
                        modifier = Modifier
                            .weight(if (isWideScreen) 1.15f else if (isUltraNarrow) 1.05f else 1.08f)
                            .fillMaxHeight()
                            .padding(
                                start = if (isWideScreen) 12.dp else 4.dp,
                                top = if (isWideScreen) 10.dp else 4.dp,
                                end = if (isWideScreen) 6.dp else 2.dp,
                                bottom = if (isWideScreen) 10.dp else 4.dp
                            )
                    )

                    // KANAN: LIST PESANAN & CHECKOUT
                    Card(
                        shape = RoundedCornerShape(
                            topStart = if (isWideScreen) 16.dp else 10.dp,
                            bottomStart = if (isWideScreen) 16.dp else 10.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(if (isWideScreen) 0.85f else if (isUltraNarrow) 0.95f else 0.92f)
                            .fillMaxHeight()
                            .padding(
                                top = if (isWideScreen) 10.dp else 4.dp,
                                end = if (isWideScreen) 10.dp else 4.dp,
                                bottom = if (isWideScreen) 10.dp else 4.dp
                            )
                    ) {
                        CashierOrderCartPane(
                            cart = cart,
                            cartCount = cartCount,
                            cartSubtotal = cartSubtotal,
                            discountAmount = discountAmount,
                            isTaxEnabled = isTaxEnabled,
                            taxValue = taxValue,
                            grandTotal = grandTotal,
                            orderType = orderType,
                            onOrderTypeChange = { orderType = it },
                            tableNumber = tableNumber,
                            onTableNumberChange = { tableNumber = it },
                            onClearCart = { viewModel.clearCart() },
                            onShowTutupKasir = { showTutupKasirDialog = true },
                            onShowDiscount = { showDiscountDialog = true },
                            onToggleTax = { viewModel.toggleTax(it) },
                            onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                            onRemoveItem = { id -> viewModel.removeFromCart(id) },
                            onCheckout = { showPaymentDialog = true },
                            showHeader = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isWideScreen) 12.dp else 6.dp)
                        )
                    }
                }
            }
        } else {
            // Tampilan Adaptif untuk HP Portrait (< 600dp): Tab Tunggal Penuh (Menu / Pesanan) + Floating Cart Bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top Segmented Switcher Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Menu
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { phoneViewMode = "MENU" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (phoneViewMode == "MENU") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (phoneViewMode == "MENU") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = if (phoneViewMode == "MENU") Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Katalog Menu",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phoneViewMode == "MENU") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Tab 2: Pesanan & Keranjang
                    Surface(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { phoneViewMode = "CART" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (phoneViewMode == "CART") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (phoneViewMode == "CART") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge(
                                            containerColor = if (phoneViewMode == "CART") Color.White else MaterialTheme.colorScheme.primary,
                                            contentColor = if (phoneViewMode == "CART") MaterialTheme.colorScheme.primary else Color.White
                                        ) {
                                            Text("$cartCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = if (phoneViewMode == "CART") Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (cartCount > 0) BluetoothPrinterManager.formatRupiah(grandTotal) else "Pesanan",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phoneViewMode == "CART") Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Tombol Split Mode (2 Kolom)
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { phoneViewMode = "SPLIT" },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VerticalSplit,
                                contentDescription = "Mode 2 Kolom",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Konten Tab Aktif
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (phoneViewMode == "MENU") {
                        Column(modifier = Modifier.fillMaxSize()) {
                            CashierMenuCatalogPane(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onCategoryChange = { viewModel.setCategory(it) },
                                filteredProducts = filteredProducts,
                                cart = cart,
                                onAddToCart = { product ->
                                    if (product.isOutOfStock) {
                                        Toast.makeText(context, "Stok '${product.name}' sudah habis!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addToCart(product)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            // Floating Bottom Cart Bar (Muncul saat ada item di keranjang)
                            if (cartCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { phoneViewMode = "CART" }
                                        .testTag("floating_cart_bar")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary) {
                                                        Text("$cartCount", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Total Pesanan", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                                Text(
                                                    text = BluetoothPrinterManager.formatRupiah(grandTotal),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { phoneViewMode = "CART" },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Lihat Pesanan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // CART VIEW (Layar Penuh pada HP)
                        Card(
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                        ) {
                            CashierOrderCartPane(
                                cart = cart,
                                cartCount = cartCount,
                                cartSubtotal = cartSubtotal,
                                discountAmount = discountAmount,
                                isTaxEnabled = isTaxEnabled,
                                taxValue = taxValue,
                                grandTotal = grandTotal,
                                orderType = orderType,
                                onOrderTypeChange = { orderType = it },
                                tableNumber = tableNumber,
                                onTableNumberChange = { tableNumber = it },
                                onClearCart = { viewModel.clearCart() },
                                onShowTutupKasir = { showTutupKasirDialog = true },
                                onShowDiscount = { showDiscountDialog = true },
                                onToggleTax = { viewModel.toggleTax(it) },
                                onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                                onRemoveItem = { id -> viewModel.removeFromCart(id) },
                                onCheckout = { showPaymentDialog = true },
                                showHeader = true,
                                onAddMoreItems = { phoneViewMode = "MENU" },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Tutup Kasir
    if (showTutupKasirDialog) {
        TutupKasirDialog(
            viewModel = viewModel,
            onDismiss = { showTutupKasirDialog = false },
            onSuccessClosed = {
                viewModel.selectTab(MainTab.REPORT)
            }
        )
    }

    // Modal Input Diskon
    if (showDiscountDialog) {
        DiscountInputDialog(
            currentDiscount = discountAmount,
            onDismiss = { showDiscountDialog = false },
            onApply = { newDiscount ->
                viewModel.setDiscount(newDiscount)
                showDiscountDialog = false
            }
        )
    }

    // Payment Dialog
    if (showPaymentDialog) {
        PaymentDialog(
            grandTotal = grandTotal,
            initialOrderType = orderType,
            initialTableNumber = tableNumber,
            onDismiss = { showPaymentDialog = false },
            onConfirmPayment = { method, amountPaid, customerName, chosenOrderType, chosenTableNumber, notes ->
                showPaymentDialog = false
                orderType = chosenOrderType
                tableNumber = chosenTableNumber
                viewModel.processPayment(
                    paymentMethod = method,
                    amountPaid = amountPaid,
                    customerName = customerName,
                    orderType = chosenOrderType,
                    tableNumber = chosenTableNumber,
                    notes = notes
                )
            }
        )
    }
}

@Composable
private fun CashierMenuCatalogPane(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    filteredProducts: List<Product>,
    cart: Map<Long, CartItem>,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Pencarian Menu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Cari menu / scan...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pos_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Kategori Menu Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("Semua", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Daftar Menu Makanan & Barang
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Menu tidak ditemukan",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val cartItem = cart[product.id]
                    CompactProductMenuCard(
                        product = product,
                        cartQty = cartItem?.quantity ?: 0,
                        onAddToCart = { onAddToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CashierOrderCartPane(
    cart: Map<Long, CartItem>,
    cartCount: Int,
    cartSubtotal: Double,
    discountAmount: Double,
    isTaxEnabled: Boolean,
    taxValue: Double,
    grandTotal: Double,
    orderType: OrderType,
    onOrderTypeChange: (OrderType) -> Unit,
    tableNumber: String,
    onTableNumberChange: (String) -> Unit,
    onClearCart: () -> Unit,
    onShowTutupKasir: () -> Unit,
    onShowDiscount: () -> Unit,
    onToggleTax: (Boolean) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCheckout: () -> Unit,
    showHeader: Boolean,
    onAddMoreItems: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (showHeader) {
            // Header List Pesanan + Tombol Tutup Kasir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                                    Text(text = "$cartCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pesanan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tombol Tutup Kasir
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier
                            .clickable { onShowTutupKasir() }
                            .testTag("tutup_kasir_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Tutup Kasir",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    if (cartCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClearCart,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Pesanan",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))
        }

        // Pilihan Tipe Pesanan: Makan di Tempat / Di Bungkus
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Makan di Tempat
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOrderTypeChange(OrderType.DINE_IN) }
                    .testTag("cashier_tab_dine_in"),
                shape = RoundedCornerShape(6.dp),
                color = if (orderType == OrderType.DINE_IN) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (orderType == OrderType.DINE_IN) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = if (orderType == OrderType.DINE_IN) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Makan di Tempat",
                        fontSize = 10.sp,
                        fontWeight = if (orderType == OrderType.DINE_IN) FontWeight.Bold else FontWeight.Medium,
                        color = if (orderType == OrderType.DINE_IN) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                    )
                }
            }

            // Di Bungkus
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOrderTypeChange(OrderType.TAKE_AWAY) }
                    .testTag("cashier_tab_take_away"),
                shape = RoundedCornerShape(6.dp),
                color = if (orderType == OrderType.TAKE_AWAY) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (orderType == OrderType.TAKE_AWAY) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TakeoutDining,
                        contentDescription = null,
                        tint = if (orderType == OrderType.TAKE_AWAY) Color(0xFFEA580C) else Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Di Bungkus",
                        fontSize = 10.sp,
                        fontWeight = if (orderType == OrderType.TAKE_AWAY) FontWeight.Bold else FontWeight.Medium,
                        color = if (orderType == OrderType.TAKE_AWAY) Color(0xFFEA580C) else Color(0xFF64748B)
                    )
                }
            }
        }

        if (orderType == OrderType.DINE_IN) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Meja:",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
                listOf("1", "2", "3", "4", "5").forEach { num ->
                    val isSelectedTable = tableNumber == "Meja $num"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelectedTable) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            onTableNumberChange(if (isSelectedTable) "" else "Meja $num")
                        }
                    ) {
                        Text(
                            text = num,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedTable) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (tableNumber.isNotBlank()) {
                    Text(
                        text = tableNumber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scrollable List Pesanan
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Belum Ada Pesanan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Silakan pilih menu makanan",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    if (onAddMoreItems != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onAddMoreItems,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Buka Katalog Menu", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(cart.values.toList(), key = { it.product.id }) { item ->
                    CompactOrderItemRow(
                        item = item,
                        onIncrease = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                        onDecrease = { onUpdateQuantity(item.product.id, item.quantity - 1) },
                        onRemove = { onRemoveItem(item.product.id) }
                    )
                }

                if (onAddMoreItems != null) {
                    item {
                        OutlinedButton(
                            onClick = onAddMoreItems,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .padding(top = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah Menu Lain", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- SUBTOTAL & RINGKASAN TAGIHAN ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFCBD5E1))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Subtotal Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Subtotal", fontSize = 11.sp, color = Color(0xFF64748B))
                Text(
                    text = BluetoothPrinterManager.formatRupiah(cartSubtotal),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Diskon Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowDiscount() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Diskon", fontSize = 11.sp, color = if (discountAmount > 0) Color(0xFFEF4444) else Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "(Ubah)", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = if (discountAmount > 0) "-${BluetoothPrinterManager.formatRupiah(discountAmount)}" else "Rp 0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (discountAmount > 0) Color(0xFFEF4444) else Color(0xFF64748B)
                )
            }

            // PPN 11% Toggle Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "PPN 11%", fontSize = 11.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isTaxEnabled,
                        onCheckedChange = onToggleTax,
                        modifier = Modifier.size(width = 34.dp, height = 20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                if (isTaxEnabled) {
                    Text(
                        text = BluetoothPrinterManager.formatRupiah(taxValue),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Color(0xFFE2E8F0))

            // Grand Total Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = BluetoothPrinterManager.formatRupiah(grandTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tombol Bayar / Checkout
        Button(
            onClick = onCheckout,
            enabled = cartCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("checkout_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (cartCount > 0) "Bayar (${cartCount})" else "Pilih Menu",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TutupKasirDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onSuccessClosed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.reportTransactions.collectAsState()
    val connectedPrinter by viewModel.printerManager.connectedDevice.collectAsState()

    val shiftTrxs = remember(transactions, currentUser) {
        transactions.filter { it.cashierId == currentUser?.id || currentUser?.role == UserRole.ADMIN }
    }

    val totalCount = shiftTrxs.size
    val totalCash = shiftTrxs.filter { it.paymentMethod == PaymentMethod.TUNAI }.sumOf { it.grandTotal }
    val totalNonCash = shiftTrxs.filter { it.paymentMethod != PaymentMethod.TUNAI }.sumOf { it.grandTotal }
    val totalRevenue = shiftTrxs.sumOf { it.grandTotal }

    var actualCashInput by remember { mutableStateOf(totalCash.toInt().toString()) }
    var notes by remember { mutableStateOf("") }
    var printReceipt by remember { mutableStateOf(connectedPrinter != null) }

    val actualCash = actualCashInput.toDoubleOrNull() ?: 0.0
    val difference = actualCash - totalCash

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockClock,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tutup Shift Kasir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kasir: ${currentUser?.fullName ?: "-"}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Batal")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shift Summary Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Transaksi Shift", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("$totalCount struk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Non-Tunai (QRIS/Debit)", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(BluetoothPrinterManager.formatRupiah(totalNonCash), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Uang Tunai di Laci (Seharusnya)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(BluetoothPrinterManager.formatRupiah(totalCash), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL OMSET SHIFT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(BluetoothPrinterManager.formatRupiah(totalRevenue), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Actual Cash in Drawer
                Text(
                    text = "Hitung Fisik Uang di Laci (Rp):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = actualCashInput,
                    onValueChange = { actualCashInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actual_cash_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Difference indicator
                val isBalanced = difference == 0.0
                val isMinus = difference < 0.0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isBalanced) Color(0xFFDCFCE7) else if (isMinus) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBalanced) "Selisih Kas:" else if (isMinus) "Selisih Kas (Kurang):" else "Selisih Kas (Lebih):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBalanced) Color(0xFF166534) else if (isMinus) Color(0xFF991B1B) else Color(0xFF92400E)
                        )
                        Text(
                            text = if (isBalanced) "Pas (Rp 0)" else BluetoothPrinterManager.formatRupiah(difference),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBalanced) Color(0xFF166534) else if (isMinus) Color(0xFF991B1B) else Color(0xFF92400E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Serah Terima (Opsional)") },
                    placeholder = { Text("Contoh: Kas kecil aman, shift pagi selesai") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Print receipt toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Cetak Struk Tutup Shift", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (connectedPrinter != null) "Printer: ${connectedPrinter?.name}" else "Printer Bluetooth belum terhubung",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Switch(
                        checked = printReceipt,
                        onCheckedChange = { printReceipt = it },
                        modifier = Modifier.size(width = 36.dp, height = 22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Sync Info Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Otomatis sinkron $totalCount transaksi & laporan shift ke Cloud Database saat konfirmasi.",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.closeShift(actualCash, notes, printReceipt)
                                Toast.makeText(
                                    context,
                                    "✅ Kasir ditutup! $totalCount transaksi otomatis disinkronkan ke Cloud Database.",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismiss()
                                onSuccessClosed()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_tutup_kasir_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Konfirmasi Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactProductMenuCard(
    product: Product,
    cartQty: Int,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddToCart)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = BluetoothPrinterManager.formatRupiah(product.sellPrice),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " / ${product.unit}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Stock Badge
                val (badgeBg, badgeText, badgeColor) = when {
                    product.isOutOfStock -> Triple(Color(0xFFFEE2E2), "Habis", Color(0xFF991B1B))
                    product.isLowStock -> Triple(Color(0xFFFEF3C7), "Sisa ${product.stock}", Color(0xFF92400E))
                    else -> Triple(Color(0xFFDCFCE7), "Stok ${product.stock}", Color(0xFF166534))
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Qty Indicator
            if (cartQty > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$cartQty",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = if (product.isOutOfStock) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah",
                            tint = if (product.isOutOfStock) Color(0xFF94A3B8) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOrderItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.product.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = BluetoothPrinterManager.formatRupiah(item.subtotal),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Counter Stepper (- / +)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onDecrease)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Kurang",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onIncrease)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscountInputDialog(
    currentDiscount: Double,
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    var discountInput by remember {
        mutableStateOf(if (currentDiscount > 0) currentDiscount.toInt().toString() else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Potongan Diskon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Nominal Diskon (Rp)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Discount, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { onApply(0.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155))
                    ) {
                        Text("Reset")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val d = discountInput.toDoubleOrNull() ?: 0.0
                            onApply(d)
                        }
                    ) {
                        Text("Terapkan")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    grandTotal: Double,
    initialOrderType: OrderType = OrderType.DINE_IN,
    initialTableNumber: String = "",
    onDismiss: () -> Unit,
    onConfirmPayment: (PaymentMethod, Double, String, OrderType, String, String) -> Unit
) {
    var selectedOrderType by remember { mutableStateOf(initialOrderType) }
    var tableNumber by remember { mutableStateOf(initialTableNumber) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var customerName by remember { mutableStateOf("Pelanggan Umum") }
    var notes by remember { mutableStateOf("") }

    var cashInput by remember { mutableStateOf(grandTotal.toInt().toString()) }

    val amountPaid = if (selectedMethod == PaymentMethod.TUNAI) {
        cashInput.toDoubleOrNull() ?: 0.0
    } else {
        grandTotal
    }

    val change = (amountPaid - grandTotal).coerceAtLeast(0.0)
    val isEnough = amountPaid >= grandTotal

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Batal")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grand total callout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total yang harus dibayar:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = BluetoothPrinterManager.formatRupiah(grandTotal),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tipe Layanan: Makan di Tempat / Di Bungkus
                Text(text = "Tipe Layanan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDineIn = selectedOrderType == OrderType.DINE_IN
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDineIn) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F5F9),
                        border = if (isDineIn) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedOrderType = OrderType.DINE_IN }
                            .testTag("dialog_order_type_dine_in")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = if (isDineIn) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Makan di Tempat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDineIn) MaterialTheme.colorScheme.primary else Color(0xFF334155)
                            )
                        }
                    }

                    val isTakeAway = selectedOrderType == OrderType.TAKE_AWAY
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTakeAway) Color(0xFFFFEDD5) else Color(0xFFF1F5F9),
                        border = if (isTakeAway) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEA580C)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedOrderType = OrderType.TAKE_AWAY }
                            .testTag("dialog_order_type_take_away")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TakeoutDining,
                                contentDescription = null,
                                tint = if (isTakeAway) Color(0xFFEA580C) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Di Bungkus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTakeAway) Color(0xFFEA580C) else Color(0xFF334155)
                            )
                        }
                    }
                }

                if (selectedOrderType == OrderType.DINE_IN) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tableNumber,
                        onValueChange = { tableNumber = it },
                        label = { Text("Nomor Meja (Contoh: Meja 1)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_table_number_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method Chips
                Text(text = "Pilih Metode Pembayaran:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        val isSelected = selectedMethod == method
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedMethod = method
                                    if (method != PaymentMethod.TUNAI) {
                                        cashInput = grandTotal.toInt().toString()
                                    }
                                }
                                .testTag("payment_method_${method.name}")
                        ) {
                            Text(
                                text = method.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Specific UI for Tunai vs Non-Tunai
                if (selectedMethod == PaymentMethod.TUNAI) {
                    Text(text = "Uang Diterima (Rp):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { cashInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_amount_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Cash Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickAmounts = listOf(grandTotal.toInt(), 20000, 50000, 100000)
                        quickAmounts.forEach { amt ->
                            val label = if (amt == grandTotal.toInt()) "Uang Pas" else "${amt / 1000}k"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { cashInput = amt.toString() }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kembalian calculation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isEnough) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnough) "Kembalian:" else "Uang Kurang:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnough) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                        Text(
                            text = if (isEnough)
                                BluetoothPrinterManager.formatRupiah(change)
                            else
                                "- ${BluetoothPrinterManager.formatRupiah(grandTotal - amountPaid)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isEnough) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                    }
                } else if (selectedMethod == PaymentMethod.QRIS) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Scan QRIS Kasir POS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Dukungan GoPay, OVO, Dana, BCA, Mandiri", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Customer Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nama Pelanggan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Payment
                Button(
                    onClick = {
                        onConfirmPayment(selectedMethod, amountPaid, customerName, selectedOrderType, tableNumber, notes)
                    },
                    enabled = isEnough,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_payment_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Selesaikan Transaksi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
