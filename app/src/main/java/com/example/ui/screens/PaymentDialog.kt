package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.ui.theme.PosErrorRed
import com.example.ui.theme.PosSuccessGreen
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    var showDetailSheet by remember { mutableStateOf(false) }

    // Mulai dari kosong ("Rp 0") persis seperti tampilan kasir keypad penuh
    var cashInput by remember { mutableStateOf("") }

    val amountPaid = if (selectedMethod == PaymentMethod.TUNAI) {
        cashInput.toDoubleOrNull() ?: 0.0
    } else {
        grandTotal
    }

    val change = (amountPaid - grandTotal).coerceAtLeast(0.0)
    val isEnough = if (grandTotal <= 0.0) amountPaid >= 0.0 && (selectedMethod != PaymentMethod.TUNAI || cashInput.isNotEmpty()) else amountPaid >= grandTotal

    fun formatRpClean(value: Double): String {
        return BluetoothPrinterManager.formatRupiah(value)
            .replace(Regex("Rp\\s+"), "Rp ")
            .trim()
    }

    fun appendDigits(digits: String) {
        if (selectedMethod != PaymentMethod.TUNAI) {
            selectedMethod = PaymentMethod.TUNAI
            cashInput = ""
        }
        if (digits == ".") {
            if (!cashInput.contains(".")) {
                cashInput = if (cashInput.isEmpty()) "0." else "$cashInput."
            }
            return
        }
        if (cashInput.isEmpty() && (digits == "0" || digits == "000")) {
            return
        }
        val candidate = cashInput + digits
        if (candidate.length <= 12) {
            cashInput = candidate
        }
    }

    fun applyExactCash() {
        selectedMethod = PaymentMethod.TUNAI
        val exact = grandTotal.roundToInt().coerceAtLeast(0)
        cashInput = exact.toString()
    }

    val quickNominals = remember(grandTotal) {
        val exact = grandTotal.roundToInt().coerceAtLeast(0)
        val roundedUpTenK = if (exact > 0) (ceil(exact / 10000.0) * 10000).toInt() else 10000
        val candidates = linkedSetOf(exact, 10000, 20000, roundedUpTenK, 50000, 100000)
        candidates.filter { it > 0 }.take(5)
    }

    Dialog(
        onDismissRequest = {
            if (showDetailSheet) {
                showDetailSheet = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            if (showDetailSheet) {
                showDetailSheet = false
            } else {
                onDismiss()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 580.dp)
                ) {
                    // 1. TOP BAR: Back (Kiri), Total : Rp ... (Tengah), Check ✓ (Kanan)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("payment_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Kembali",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = "Total : ${formatRpClean(grandTotal)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .testTag("payment_header_total"),
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .padding(end = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isEnough) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable(enabled = isEnough) {
                                                onConfirmPayment(
                                                    selectedMethod,
                                                    amountPaid,
                                                    customerName,
                                                    selectedOrderType,
                                                    tableNumber,
                                                    notes
                                                )
                                            }
                                            .testTag("confirm_payment_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selesaikan Transaksi",
                                                tint = if (isEnough) {
                                                    MaterialTheme.colorScheme.onSecondary
                                                } else {
                                                    MaterialTheme.colorScheme.outline
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // 2. DISPLAY UTAMA NOMINAL BESAR (Rp 0 / Rp 20.000)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.30f)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("cash_amount_input")
                    ) {
                        // Pill kecil di kanan atas untuk ringkasan pelanggan / detail cepat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDetailSheet = true }
                                .testTag("payment_customer_pill")
                        ) {
                            Text(
                                text = customerName.ifBlank { "Pelanggan Umum" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayText = if (selectedMethod == PaymentMethod.TUNAI) {
                                if (cashInput.isEmpty() || amountPaid <= 0.0) "Rp 0" else formatRpClean(amountPaid)
                            } else {
                                formatRpClean(grandTotal)
                            }
                            val isMutedZero = selectedMethod == PaymentMethod.TUNAI && (cashInput.isEmpty() || amountPaid <= 0.0)
                            val dynamicFontSize = when {
                                displayText.length >= 14 -> 32.sp
                                displayText.length >= 11 -> 38.sp
                                else -> 46.sp
                            }

                            Text(
                                text = displayText,
                                fontSize = dynamicFontSize,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    selectedMethod != PaymentMethod.TUNAI -> MaterialTheme.colorScheme.secondary
                                    isMutedZero -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("cash_amount_display")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedMethod == PaymentMethod.TUNAI) {
                                if (cashInput.isNotEmpty() && amountPaid > 0.0) {
                                    Text(
                                        text = if (isEnough) {
                                            "Kembalian: ${formatRpClean(change)}"
                                        } else {
                                            "Kurang: ${formatRpClean((grandTotal - amountPaid).coerceAtLeast(0.0))}"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEnough) PosSuccessGreen else PosErrorRed
                                    )
                                } else {
                                    Text(
                                        text = "Ketuk angka atau ikon uang pas",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { showDetailSheet = true }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = paymentMethodIcon(selectedMethod),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${selectedMethod.label} • Nominal Pas",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // 3. BAND TENGAH: Quick Nominal Chips + Non Tunai + Dropdown Metode/Layanan
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(end = 2.dp)
                                ) {
                                    items(quickNominals) { amt ->
                                        val isExact = amt == grandTotal.roundToInt()
                                        val isChipActive = selectedMethod == PaymentMethod.TUNAI &&
                                            cashInput.toDoubleOrNull()?.roundToInt() == amt
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChipActive) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isChipActive) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                                }
                                            ),
                                            modifier = Modifier
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedMethod = PaymentMethod.TUNAI
                                                    cashInput = amt.toString()
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = if (isExact) "Uang Pas" else formatRpClean(amt.toDouble()),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isChipActive) {
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pill "Non Tunai" / "Tunai" bergaris hijau di kanan
                                val isNonCash = selectedMethod != PaymentMethod.TUNAI
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isNonCash) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isNonCash) {
                                                selectedMethod = PaymentMethod.TUNAI
                                            } else {
                                                selectedMethod = PaymentMethod.QRIS
                                                showDetailSheet = true
                                            }
                                        }
                                        .testTag("payment_non_tunai_toggle")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = if (isNonCash) "Tunai" else "Non Tunai",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNonCash) {
                                                MaterialTheme.colorScheme.onSecondary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            }
                                        )
                                    }
                                }
                            }

                            // Dropdown Selector Full-Width (seperti bar "D... v" di atas keypad)
                            val tableSuffix = if (selectedOrderType == OrderType.DINE_IN && tableNumber.isNotBlank()) {
                                " • Meja $tableNumber"
                            } else {
                                ""
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showDetailSheet = true }
                                    .testTag("payment_options_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${selectedOrderType.label}$tableSuffix • ${selectedMethod.label}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Ubah Layanan atau Metode Pembayaran",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // 4. KEYPAD NUMERIK 4x4 PRESISI SEPERTI GAMBAR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.54f)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Baris 1: 7 | 8 | 9 | C
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            KeypadTextButton("7", Modifier.weight(1f), "keypad_7") { appendDigits("7") }
                            KeypadTextButton("8", Modifier.weight(1f), "keypad_8") { appendDigits("8") }
                            KeypadTextButton("9", Modifier.weight(1f), "keypad_9") { appendDigits("9") }
                            KeypadTextButton("C", Modifier.weight(1f), "keypad_clear") {
                                selectedMethod = PaymentMethod.TUNAI
                                cashInput = ""
                            }
                        }

                        // Baris 2: 4 | 5 | 6 | [Backspace]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            KeypadTextButton("4", Modifier.weight(1f), "keypad_4") { appendDigits("4") }
                            KeypadTextButton("5", Modifier.weight(1f), "keypad_5") { appendDigits("5") }
                            KeypadTextButton("6", Modifier.weight(1f), "keypad_6") { appendDigits("6") }
                            KeypadIconButton(
                                icon = Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = "Hapus satu digit",
                                modifier = Modifier.weight(1f),
                                tag = "keypad_backspace"
                            ) {
                                selectedMethod = PaymentMethod.TUNAI
                                if (cashInput.isNotEmpty()) {
                                    cashInput = cashInput.dropLast(1)
                                }
                            }
                        }

                        // Baris 3: 1 | 2 | 3 | [Uang Pas Banknote]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            KeypadTextButton("1", Modifier.weight(1f), "keypad_1") { appendDigits("1") }
                            KeypadTextButton("2", Modifier.weight(1f), "keypad_2") { appendDigits("2") }
                            KeypadTextButton("3", Modifier.weight(1f), "keypad_3") { appendDigits("3") }
                            KeypadIconButton(
                                icon = Icons.Outlined.Payments,
                                contentDescription = "Uang Pas",
                                modifier = Modifier.weight(1f),
                                tag = "keypad_uang_pas"
                            ) {
                                applyExactCash()
                            }
                        }

                        // Baris 4: 0 | 000 | . | [Kosong]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            KeypadTextButton("0", Modifier.weight(1f), "keypad_0") { appendDigits("0") }
                            KeypadTextButton("000", Modifier.weight(1f), "keypad_000") { appendDigits("000") }
                            KeypadTextButton(".", Modifier.weight(1f), "keypad_dot") { appendDigits(".") }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog detail Layanan, Metode Pembayaran Non-Tunai, Pelanggan & QRIS
    if (showDetailSheet) {
        PaymentOptionsDetailDialog(
            grandTotal = grandTotal,
            selectedMethod = selectedMethod,
            selectedOrderType = selectedOrderType,
            tableNumber = tableNumber,
            customerName = customerName,
            notes = notes,
            onSelectMethod = { method ->
                selectedMethod = method
                if (method != PaymentMethod.TUNAI) {
                    cashInput = grandTotal.roundToInt().coerceAtLeast(0).toString()
                }
            },
            onSelectOrderType = { selectedOrderType = it },
            onTableNumberChange = { tableNumber = it },
            onCustomerNameChange = { customerName = it },
            onNotesChange = { notes = it },
            onDismiss = { showDetailSheet = false }
        )
    }
}

@Composable
private fun KeypadTextButton(
    label: String,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KeypadIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun paymentMethodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.TUNAI -> Icons.Outlined.Payments
    PaymentMethod.QRIS -> Icons.Default.QrCode
    PaymentMethod.TRANSFER -> Icons.Default.AccountBalance
    PaymentMethod.DEBIT -> Icons.Default.CreditCard
}

@Composable
private fun PaymentOptionsDetailDialog(
    grandTotal: Double,
    selectedMethod: PaymentMethod,
    selectedOrderType: OrderType,
    tableNumber: String,
    customerName: String,
    notes: String,
    onSelectMethod: (PaymentMethod) -> Unit,
    onSelectOrderType: (OrderType) -> Unit,
    onTableNumberChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail Pembayaran & Layanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Metode Pembayaran
                Text(
                    text = "Metode Pembayaran",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        val isSelected = selectedMethod == method
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectMethod(method) }
                                .testTag("payment_method_${method.name}")
                        ) {
                            Text(
                                text = method.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                if (selectedMethod == PaymentMethod.QRIS) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QRIS",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan QRIS • ${BluetoothPrinterManager.formatRupiah(grandTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Mendukung GoPay, OVO, Dana, ShopeePay & Mobile Banking",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Tipe Layanan
                Text(
                    text = "Tipe Layanan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDineIn = selectedOrderType == OrderType.DINE_IN
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDineIn) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isDineIn) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectOrderType(OrderType.DINE_IN) }
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
                                tint = if (isDineIn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Makan di Tempat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDineIn) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val isTakeAway = selectedOrderType == OrderType.TAKE_AWAY
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTakeAway) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isTakeAway) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectOrderType(OrderType.TAKE_AWAY) }
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
                                tint = if (isTakeAway) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Di Bungkus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTakeAway) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (selectedOrderType == OrderType.DINE_IN) {
                    OutlinedTextField(
                        value = tableNumber,
                        onValueChange = onTableNumberChange,
                        label = { Text("Nomor Meja (Contoh: Meja 1)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_table_number_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Nama Pelanggan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Catatan Pesanan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("payment_detail_apply_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = "Terapkan",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}
