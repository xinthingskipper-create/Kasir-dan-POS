package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    viewModel: PosViewModel,
    transaction: Transaction,
    items: List<TransactionItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val connectedPrinter by viewModel.printerManager.connectedDevice.collectAsState()
    val printStatus by viewModel.printerManager.printStatus.collectAsState()

    val plainText = BluetoothPrinterManager.generateReceiptPlainText(
        storeProfile,
        transaction,
        items
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sukses",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transaksi Berhasil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Thermal receipt paper styling card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Store Header
                        Text(
                            text = storeProfile.storeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = storeProfile.address,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Telp: ${storeProfile.phone}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color(0xFFCBD5E1)
                        )

                        // Meta details
                        ReceiptLineMonospace("No. Struk", transaction.invoiceNumber)
                        val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
                            .format(Date(transaction.timestamp))
                        ReceiptLineMonospace("Waktu", dateFormatted)
                        ReceiptLineMonospace("Kasir", transaction.cashierName)
                        val orderTypeDisplay = if (transaction.orderType == com.example.data.model.OrderType.DINE_IN) {
                            if (transaction.tableNumber.isNotBlank()) "Makan di Tempat (Meja ${transaction.tableNumber})" else "Makan di Tempat"
                        } else {
                            "Di Bungkus (Take Away)"
                        }
                        ReceiptLineMonospace("Tipe", orderTypeDisplay)
                        if (transaction.customerName.isNotBlank() && transaction.customerName != "Pelanggan Umum") {
                            ReceiptLineMonospace("Pelanggan", transaction.customerName)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color(0xFFCBD5E1)
                        )

                        // Itemized list
                        for (item in items) {
                            Text(
                                text = item.productName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "  ${item.quantity}x @ ${BluetoothPrinterManager.formatRupiah(item.unitPrice)}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = BluetoothPrinterManager.formatRupiah(item.subtotal),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color(0xFFCBD5E1)
                        )

                        // Totals
                        ReceiptLineMonospace("Subtotal", BluetoothPrinterManager.formatRupiah(transaction.subtotal))
                        if (transaction.discount > 0) {
                            ReceiptLineMonospace("Diskon", "-${BluetoothPrinterManager.formatRupiah(transaction.discount)}")
                        }
                        if (transaction.tax > 0) {
                            ReceiptLineMonospace("PPN (11%)", BluetoothPrinterManager.formatRupiah(transaction.tax))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = BluetoothPrinterManager.formatRupiah(transaction.grandTotal),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color(0xFFCBD5E1)
                        )

                        // Payment info
                        ReceiptLineMonospace("Metode", transaction.paymentMethod.label)
                        ReceiptLineMonospace("Bayar", BluetoothPrinterManager.formatRupiah(transaction.amountPaid))
                        ReceiptLineMonospace("Kembali", BluetoothPrinterManager.formatRupiah(transaction.changeAmount))

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = storeProfile.receiptFooter,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (printStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = printStatus ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Printer Connection status pill
                Box(
                    modifier = Modifier
                        .background(
                            if (connectedPrinter != null) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (connectedPrinter != null)
                            "Printer: ${connectedPrinter?.name} (Terhubung)"
                        else
                            "Printer Bluetooth belum terhubung",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (connectedPrinter != null) Color(0xFF166534) else Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Print via Bluetooth & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.printCurrentActiveReceipt() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_bluetooth_receipt_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Struk")
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(plainText))
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, plainText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("done_receipt_button")
                ) {
                    Text("Transaksi Selesai")
                }
            }
        }
    }
}

@Composable
private fun ReceiptLineMonospace(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
