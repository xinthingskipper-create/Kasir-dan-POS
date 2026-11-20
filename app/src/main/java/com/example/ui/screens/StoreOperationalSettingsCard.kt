package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel

@Composable
fun StoreOperationalSettingsCard(viewModel: PosViewModel) {
    val context = LocalContext.current
    val configuredCategories by viewModel.configuredCategories.collectAsState()
    val configuredUnits by viewModel.configuredUnits.collectAsState()
    val foodDefaultUnit by viewModel.foodDefaultUnit.collectAsState()
    val drinkDefaultUnit by viewModel.drinkDefaultUnit.collectAsState()
    val tableCount by viewModel.tableCount.collectAsState()

    StoreOperationalSettingsCardContent(
        categories = configuredCategories,
        units = configuredUnits,
        foodDefaultUnit = foodDefaultUnit,
        drinkDefaultUnit = drinkDefaultUnit,
        tableCount = tableCount,
        onAddCategory = { newCat ->
            val ok = viewModel.addCategory(newCat)
            Toast.makeText(
                context,
                if (ok) "Kategori '$newCat' ditambahkan" else "Kategori kosong atau sudah ada",
                Toast.LENGTH_SHORT
            ).show()
        },
        onRenameCategory = { oldCat, newCat ->
            val ok = viewModel.renameCategory(oldCat, newCat)
            Toast.makeText(
                context,
                if (ok) "Kategori '$oldCat' diubah menjadi '$newCat'" else "Gagal mengubah nama kategori",
                Toast.LENGTH_SHORT
            ).show()
        },
        onRemoveCategory = { cat ->
            val ok = viewModel.removeCategory(cat)
            Toast.makeText(
                context,
                if (ok) "Kategori '$cat' dihapus" else "Minimal harus ada 1 kategori aktif",
                Toast.LENGTH_SHORT
            ).show()
        },
        onSelectFoodUnit = { unit ->
            viewModel.setFoodDefaultUnit(unit)
            Toast.makeText(context, "Satuan default Makanan: $unit", Toast.LENGTH_SHORT).show()
        },
        onApplyFoodUnitToProducts = { unit ->
            viewModel.applyUnitToCategoryProducts("Makanan", unit)
            Toast.makeText(context, "Semua menu Makanan diperbarui ke '$unit'", Toast.LENGTH_SHORT).show()
        },
        onSelectDrinkUnit = { unit ->
            viewModel.setDrinkDefaultUnit(unit)
            Toast.makeText(context, "Satuan default Minuman: $unit", Toast.LENGTH_SHORT).show()
        },
        onApplyDrinkUnitToProducts = { unit ->
            viewModel.applyUnitToCategoryProducts("Minuman", unit)
            Toast.makeText(context, "Semua menu Minuman diperbarui ke '$unit'", Toast.LENGTH_SHORT).show()
        },
        onAddUnit = { unit ->
            val ok = viewModel.addUnit(unit)
            Toast.makeText(
                context,
                if (ok) "Satuan '$unit' ditambahkan" else "Satuan kosong atau sudah ada",
                Toast.LENGTH_SHORT
            ).show()
        },
        onRemoveUnit = { unit ->
            val ok = viewModel.removeUnit(unit)
            Toast.makeText(
                context,
                if (ok) "Satuan '$unit' dihapus" else "Satuan default aktif tidak dapat dihapus",
                Toast.LENGTH_SHORT
            ).show()
        },
        onTableCountChange = { count ->
            viewModel.setTableCount(count)
        },
        onResetDefaults = {
            viewModel.resetOperationalSettingsToDefaults()
            Toast.makeText(context, "Kategori, satuan & meja dikembalikan ke default", Toast.LENGTH_SHORT).show()
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreOperationalSettingsCardContent(
    categories: List<String>,
    units: List<String>,
    foodDefaultUnit: String,
    drinkDefaultUnit: String,
    tableCount: Int,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onSelectFoodUnit: (String) -> Unit,
    onApplyFoodUnitToProducts: (String) -> Unit,
    onSelectDrinkUnit: (String) -> Unit,
    onApplyDrinkUnitToProducts: (String) -> Unit,
    onAddUnit: (String) -> Unit,
    onRemoveUnit: (String) -> Unit,
    onTableCountChange: (Int) -> Unit,
    onResetDefaults: () -> Unit
) {
    var newCategoryInput by remember { mutableStateOf("") }
    var newUnitInput by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var renameCategoryInput by remember { mutableStateOf("") }
    var tableCountInput by remember(tableCount) { mutableStateOf(tableCount.toString()) }

    val prioritizedUnits = remember(units) {
        val core = listOf("porsi", "pcs")
        val ordered = mutableListOf<String>()
        core.forEach { u ->
            if (ordered.none { it.equals(u, ignoreCase = true) }) ordered.add(u)
        }
        units.forEach { u ->
            val clean = u.trim()
            if (clean.isNotBlank() && ordered.none { it.equals(clean, ignoreCase = true) }) {
                ordered.add(clean)
            }
        }
        ordered
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("operational_settings_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pengaturan Kategori, Satuan & Meja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Atur kategori menu, satuan makanan/minuman (porsi/pcs) & jumlah meja kasir.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(14.dp))

            // 1. BAGIAN KATEGORI MENU
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kategori Menu (${categories.size})",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kategori ini muncul di halaman Kasir dan form tambah produk Inventaris.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    label = { Text("Tambah Kategori Baru") },
                    placeholder = { Text("Contoh: Dessert, Paket Hemat") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_new_category")
                )
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddCategory(trimmed)
                            newCategoryInput = ""
                        }
                    },
                    enabled = newCategoryInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_add_category")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Kategori", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.testTag("category_item_$cat")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            IconButton(
                                onClick = {
                                    editingCategory = cat
                                    renameCategoryInput = cat
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_edit_category_$cat")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ubah Kategori $cat",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (categories.size > 1) {
                                IconButton(
                                    onClick = { onRemoveCategory(cat) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_delete_category_$cat")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Kategori $cat",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. BAGIAN SATUAN MAKANAN & MINUMAN (PORSI / PCS)
            Text(
                text = "Satuan Makanan & Minuman (Porsi / Pcs)",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pilih satuan bawaan untuk Makanan dan Minuman (misal: porsi atau pcs) serta daftar satuan pilihan di produk.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Makanan Unit Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Satuan Makanan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD97706)
                        ) {
                            Text(
                                text = "Aktif: $foodDefaultUnit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("badge_food_default_unit")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        prioritizedUnits.forEach { unit ->
                            val selected = foodDefaultUnit.equals(unit, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectFoodUnit(unit) },
                                label = {
                                    Text(
                                        text = unit,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD97706),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_food_unit_$unit")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onApplyFoodUnitToProducts(foodDefaultUnit) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_apply_food_unit")
                    ) {
                        Text(
                            text = "Terapkan '$foodDefaultUnit' ke Semua Menu Makanan",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Minuman Unit Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalCafe,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Satuan Minuman",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2563EB)
                        ) {
                            Text(
                                text = "Aktif: $drinkDefaultUnit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("badge_drink_default_unit")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        prioritizedUnits.forEach { unit ->
                            val selected = drinkDefaultUnit.equals(unit, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectDrinkUnit(unit) },
                                label = {
                                    Text(
                                        text = unit,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2563EB),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_drink_unit_$unit")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onApplyDrinkUnitToProducts(drinkDefaultUnit) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_apply_drink_unit")
                    ) {
                        Text(
                            text = "Terapkan '$drinkDefaultUnit' ke Semua Menu Minuman",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tambah Satuan Kustom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newUnitInput,
                    onValueChange = { newUnitInput = it },
                    label = { Text("Tambah Satuan Kustom") },
                    placeholder = { Text("Contoh: gelas, kotak, mangkok") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_new_unit")
                )
                Button(
                    onClick = {
                        val trimmed = newUnitInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddUnit(trimmed)
                            newUnitInput = ""
                        }
                    },
                    enabled = newUnitInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_add_unit")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Satuan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (units.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.forEach { unit ->
                        val isProtected = unit.equals(foodDefaultUnit, ignoreCase = true) ||
                            unit.equals(drinkDefaultUnit, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = if (!isProtected && units.size > 1) 2.dp else 10.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = unit,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                if (!isProtected && units.size > 1) {
                                    IconButton(
                                        onClick = { onRemoveUnit(unit) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_delete_unit_$unit")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Satuan $unit",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(14.dp))

            // 3. BAGIAN JUMLAH MEJA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.TableRestaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Jumlah Meja (Makan di Tempat)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (tableCount > 0) "Menampilkan Meja 1 s/d Meja $tableCount di Kasir" else "Pilihan cepat meja dinonaktifkan (0 meja)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$tableCount Meja",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("text_active_table_count")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onTableCountChange((tableCount - 1).coerceAtLeast(0)) },
                    enabled = tableCount > 0,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_decrease_table_count")
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurangi Jumlah Meja")
                }

                OutlinedTextField(
                    value = tableCountInput,
                    onValueChange = { raw ->
                        val digitsOnly = raw.filter { it.isDigit() }.take(3)
                        tableCountInput = digitsOnly
                        val parsed = digitsOnly.toIntOrNull()
                        if (parsed != null) {
                            onTableCountChange(parsed.coerceIn(0, 100))
                        }
                    },
                    label = { Text("Jumlah Meja (0-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_table_count")
                )

                Button(
                    onClick = { onTableCountChange((tableCount + 1).coerceAtMost(100)) },
                    enabled = tableCount < 100,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_increase_table_count")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Jumlah Meja")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0, 5, 10, 15, 20, 30).forEach { preset ->
                    val isSelected = tableCount == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTableCountChange(preset) },
                        label = {
                            Text(
                                text = if (preset == 0) "0 (Off)" else "$preset Meja",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chip_table_preset_$preset")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onResetDefaults,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .testTag("btn_reset_operational_settings")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Kategori, Satuan & Meja ke Default", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Rename Category Dialog
    editingCategory?.let { oldCat ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Ubah Nama Kategori", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mengubah nama kategori juga akan memperbarui semua produk yang memakai kategori '$oldCat'.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = renameCategoryInput,
                        onValueChange = { renameCategoryInput = it },
                        label = { Text("Nama Kategori Baru") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rename_category")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameCategoryInput.trim()
                        if (trimmed.isNotEmpty() && !trimmed.equals(oldCat, ignoreCase = true)) {
                            onRenameCategory(oldCat, trimmed)
                        }
                        editingCategory = null
                    },
                    enabled = renameCategoryInput.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_rename_category")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
