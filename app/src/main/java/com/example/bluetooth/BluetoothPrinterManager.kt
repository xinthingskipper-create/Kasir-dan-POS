package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PrinterDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _connectedDevice = MutableStateFlow<PrinterDevice?>(null)
    val connectedDevice: StateFlow<PrinterDevice?> = _connectedDevice.asStateFlow()

    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = try {
            bluetoothAdapter?.isEnabled == true
        } catch (t: Throwable) {
            false
        }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<PrinterDevice> {
        val list = mutableListOf<PrinterDevice>()
        try {
            if (bluetoothAdapter == null || !isBluetoothEnabled) return list

            val paired = bluetoothAdapter.bondedDevices
            paired?.forEach { device ->
                try {
                    list.add(
                        PrinterDevice(
                            name = try { device.name ?: "Perangkat Tanpa Nama" } catch (t: Throwable) { "Thermal Printer" },
                            address = device.address,
                            isConnected = _connectedDevice.value?.address == device.address
                        )
                    )
                } catch (t: Throwable) {
                    // Ignore single device error
                }
            }
        } catch (e: Throwable) {
            Log.e("BluetoothPrinter", "Error getting paired devices safely", e)
        }
        return list
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(address: String): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            _printStatus.value = "Bluetooth tidak didukung pada perangkat ini"
            return@withContext false
        }

        try {
            disconnect()
            val device = try {
                bluetoothAdapter.getRemoteDevice(address)
            } catch (t: Throwable) {
                _printStatus.value = "Alamat printer tidak valid: ${t.localizedMessage}"
                return@withContext false
            }

            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val newSocket = try {
                device.createRfcommSocketToServiceRecord(sppUuid)
            } catch (t: Throwable) {
                _printStatus.value = "Gagal membuat socket RFCOMM: ${t.localizedMessage}"
                return@withContext false
            }

            try {
                newSocket.connect()
            } catch (t: Throwable) {
                try { newSocket.close() } catch (ignored: Throwable) {}
                _printStatus.value = "Gagal terhubung: Pastikan printer aktif dan sudah di-pair di Android."
                return@withContext false
            }

            socket = newSocket
            outputStream = newSocket.outputStream

            val devName = try { device.name ?: "Thermal Printer" } catch (t: Throwable) { "Thermal Printer" }
            _connectedDevice.value = PrinterDevice(
                name = devName,
                address = device.address,
                isConnected = true
            )
            _printStatus.value = "Terhubung ke printer $devName"
            return@withContext true
        } catch (e: Throwable) {
            disconnect()
            _printStatus.value = "Gagal terhubung: ${e.localizedMessage ?: "Cek koneksi printer"}"
            return@withContext false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream = null
            socket = null
            _connectedDevice.value = null
        }
    }

    suspend fun printReceipt(
        profile: StoreProfile,
        transaction: Transaction,
        items: List<TransactionItem>,
        paperWidthCols: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null) {
            _printStatus.value = "Printer belum terhubung. Struk dapat dilihat via pratinjau digital."
            return@withContext false
        }

        try {
            val bytes = buildEscPosReceipt(profile, transaction, items, paperWidthCols)
            out.write(bytes)
            out.flush()
            _printStatus.value = "Berhasil mencetak struk #${transaction.invoiceNumber}"
            return@withContext true
        } catch (e: Exception) {
            _printStatus.value = "Kesalahan saat mencetak: ${e.localizedMessage}"
            return@withContext false
        }
    }

    suspend fun printShiftClosing(
        profile: StoreProfile,
        closing: com.example.data.model.ShiftClosing,
        paperWidthCols: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null) {
            _printStatus.value = "Printer belum terhubung"
            return@withContext false
        }

        try {
            val baos = ByteArrayOutputStream()
            baos.write(byteArrayOf(0x1B, 0x40)) // Init
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold On
            baos.write("${profile.storeName}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold Off
            baos.write("LAPORAN TUTUP KASIR / SHIFT\n".toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
            val time = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(closing.closingTime))
            baos.write(formatTwoColumns("Waktu:", time, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Kasir:", closing.cashierName, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Trx Selesai:", "${closing.totalTransactions} struk", paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Total Tunai:", formatRupiah(closing.totalCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Non-Tunai:", formatRupiah(closing.totalNonCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
            baos.write(formatTwoColumns("TOTAL OMSET:", formatRupiah(closing.totalRevenue), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Uang Dihitung:", formatRupiah(closing.actualCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Harus Ada:", formatRupiah(closing.expectedCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            val diffStr = if (closing.difference == 0.0) "Pas (Rp 0)" else formatRupiah(closing.difference)
            baos.write(formatTwoColumns("Selisih Kas:", diffStr, paperWidthCols).toByteArray(Charsets.US_ASCII))
            if (closing.notes.isNotBlank()) {
                baos.write("Catatan: ${closing.notes}\n".toByteArray(Charsets.US_ASCII))
            }
            baos.write("${"=".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write("Tanda Tangan Kasir,\n\n\n".toByteArray(Charsets.US_ASCII))
            baos.write("( ${closing.cashierName} )\n\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines & cut

            out.write(baos.toByteArray())
            out.flush()
            _printStatus.value = "Struk Tutup Kasir berhasil dicetak!"
            return@withContext true
        } catch (e: Exception) {
            _printStatus.value = "Gagal cetak tutup kasir: ${e.localizedMessage}"
            return@withContext false
        }
    }

    suspend fun printDailyReport(
        profile: StoreProfile,
        report: com.example.data.model.DailyReport,
        paperWidthCols: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null) {
            _printStatus.value = "Printer belum terhubung"
            return@withContext false
        }

        try {
            val baos = ByteArrayOutputStream()
            baos.write(byteArrayOf(0x1B, 0x40)) // ESC @ (Init)
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold On
            baos.write("${profile.storeName}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold Off
            baos.write("REKAP LAPORAN HARIAN (ROOM DB)\n".toByteArray(Charsets.US_ASCII))
            baos.write("================================\n".toByteArray(Charsets.US_ASCII))

            baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
            baos.write(formatTwoColumns("Tanggal:", report.date, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Kasir:", report.cashierNames, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Shift Selesai:", "${report.shiftCount} Shift", paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))

            // Financial Summary (Bold)
            baos.write(byteArrayOf(0x1B, 0x45, 0x01))
            baos.write(formatTwoColumns("TOTAL OMSET:", formatRupiah(report.totalRevenue), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("LABA BERSIH:", formatRupiah(report.totalProfit), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))

            baos.write(formatTwoColumns("Total Struk:", "${report.totalTransactions} Struk", paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Item Terjual:", "${report.totalItemsSold} Pcs", paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))

            // Payment Methods
            baos.write(formatTwoColumns("Tunai:", formatRupiah(report.totalCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Non-Tunai:", formatRupiah(report.totalNonCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("  - QRIS:", formatRupiah(report.totalQris), paperWidthCols).toByteArray(Charsets.US_ASCII))
            if (report.totalTransfer > 0) {
                baos.write(formatTwoColumns("  - Transfer:", formatRupiah(report.totalTransfer), paperWidthCols).toByteArray(Charsets.US_ASCII))
            }
            if (report.totalDiscount > 0) {
                baos.write(formatTwoColumns("Diskon:", "-${formatRupiah(report.totalDiscount)}", paperWidthCols).toByteArray(Charsets.US_ASCII))
            }
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))

            // Cash Drawer Audit
            baos.write(formatTwoColumns("Kas Sistem:", formatRupiah(report.expectedCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Kas Fisik:", formatRupiah(report.actualCash), paperWidthCols).toByteArray(Charsets.US_ASCII))
            val diffStr = if (report.cashDifference == 0.0) "Pas (Rp 0)" else formatRupiah(report.cashDifference)
            baos.write(formatTwoColumns("Selisih Kas:", diffStr, paperWidthCols).toByteArray(Charsets.US_ASCII))

            baos.write("${"=".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            if (report.topProductSummary.isNotBlank()) {
                baos.write("Menu Terlaris:\n".toByteArray(Charsets.US_ASCII))
                baos.write("${report.topProductSummary}\n".toByteArray(Charsets.US_ASCII))
                baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            }

            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write("Arsip Room Database SQLite\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines & cut

            out.write(baos.toByteArray())
            out.flush()
            _printStatus.value = "Laporan harian berhasil dicetak!"
            return@withContext true
        } catch (e: Exception) {
            _printStatus.value = "Gagal cetak laporan: ${e.localizedMessage}"
            return@withContext false
        }
    }

    suspend fun printOmsetReceipt(
        profile: StoreProfile,
        totalRevenue: Double,
        periodLabel: String,
        cashierName: String,
        paperWidthCols: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null) {
            _printStatus.value = "Printer belum terhubung"
            return@withContext false
        }
        try {
            val baos = ByteArrayOutputStream()
            baos.write(byteArrayOf(0x1B, 0x40)) // ESC @
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
            baos.write("${profile.storeName}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write("LAPORAN TOTAL OMSET\n".toByteArray(Charsets.US_ASCII))
            baos.write("================================\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
            baos.write(formatTwoColumns("Periode:", periodLabel, paperWidthCols).toByteArray(Charsets.US_ASCII))
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            baos.write(formatTwoColumns("Waktu Cetak:", dateStr, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Petugas:", cashierName, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
            baos.write(formatTwoColumns("TOTAL OMSET:", formatRupiah(totalRevenue), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write("================================\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write("Tersimpan di Cloud Hosting\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines & cut
            out.write(baos.toByteArray())
            out.flush()
            _printStatus.value = "Struk Total Omset berhasil dicetak!"
            return@withContext true
        } catch (e: Exception) {
            _printStatus.value = "Gagal cetak omset: ${e.localizedMessage}"
            return@withContext false
        }
    }

    suspend fun printProfitExpenseReceipt(
        profile: StoreProfile,
        grossProfit: Double,
        expenses: Double,
        netProfit: Double,
        grossSales: Double,
        periodLabel: String,
        cashierName: String,
        paperWidthCols: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null) {
            _printStatus.value = "Printer belum terhubung"
            return@withContext false
        }
        try {
            val baos = ByteArrayOutputStream()
            baos.write(byteArrayOf(0x1B, 0x40)) // ESC @
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
            baos.write("${profile.storeName}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write("LAPORAN KEUANGAN TOKO\n".toByteArray(Charsets.US_ASCII))
            baos.write("================================\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
            baos.write(formatTwoColumns("Periode:", periodLabel, paperWidthCols).toByteArray(Charsets.US_ASCII))
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            baos.write(formatTwoColumns("Waktu Cetak:", dateStr, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Petugas:", cashierName, paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Penjualan Kotor:", formatRupiah(grossSales), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
            baos.write(formatTwoColumns("LABA KOTOR:", formatRupiah(grossProfit), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write(formatTwoColumns("PENGELUARAN:", formatRupiah(expenses), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(paperWidthCols)}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
            baos.write(formatTwoColumns("LABA BERSIH:", formatRupiah(netProfit), paperWidthCols).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write("================================\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write("Arsip Tersimpan di Cloud Hosting\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines & cut
            out.write(baos.toByteArray())
            out.flush()
            _printStatus.value = "Struk Laporan Keuangan berhasil dicetak!"
            return@withContext true
        } catch (e: Exception) {
            _printStatus.value = "Gagal cetak struk: ${e.localizedMessage}"
            return@withContext false
        }
    }

    suspend fun printTestPage(profile: StoreProfile, paperWidthCols: Int = 32): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null) {
            _printStatus.value = "Printer belum terhubung"
            return@withContext false
        }

        try {
            val baos = ByteArrayOutputStream()
            baos.write(byteArrayOf(0x1B, 0x40)) // Init
            baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold On
            baos.write("${profile.storeName}\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold Off
            baos.write("TES CETAK BLUETOOTH ESC/POS\n".toByteArray(Charsets.US_ASCII))
            baos.write("Koneksi Berhasil!\n".toByteArray(Charsets.US_ASCII))
            baos.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
            val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
            baos.write("Waktu: $time\n".toByteArray(Charsets.US_ASCII))
            baos.write("Status: SIAP OPERASIONAL\n\n\n".toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x64, 0x03)) // Feed 3 lines & cut

            out.write(baos.toByteArray())
            out.flush()
            _printStatus.value = "Halaman tes cetak berhasil dikirim!"
            return@withContext true
        } catch (e: Exception) {
            _printStatus.value = "Gagal tes cetak: ${e.localizedMessage}"
            return@withContext false
        }
    }

    fun clearStatus() {
        _printStatus.value = null
    }

    companion object {
        private val indonesianFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        fun formatRupiah(amount: Double): String {
            return indonesianFormat.format(amount).replace("Rp", "Rp ")
        }

        fun buildEscPosReceipt(
            profile: StoreProfile,
            transaction: Transaction,
            items: List<TransactionItem>,
            width: Int = 32
        ): ByteArray {
            val baos = ByteArrayOutputStream()

            // Initialize ESC/POS
            baos.write(byteArrayOf(0x1B, 0x40))

            // Center Align
            baos.write(byteArrayOf(0x1B, 0x61, 0x01))

            // Store Name (Bold + Double Height)
            baos.write(byteArrayOf(0x1B, 0x45, 0x01))
            baos.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double size
            baos.write("${profile.storeName}\n".toByteArray(Charsets.US_ASCII))

            // Normal text
            baos.write(byteArrayOf(0x1D, 0x21, 0x00))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))
            baos.write("${profile.address}\n".toByteArray(Charsets.US_ASCII))
            baos.write("Telp: ${profile.phone}\n".toByteArray(Charsets.US_ASCII))
            baos.write("${"-".repeat(width)}\n".toByteArray(Charsets.US_ASCII))

            // Left Align for transaction meta
            baos.write(byteArrayOf(0x1B, 0x61, 0x00))
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(transaction.timestamp))
            baos.write(formatTwoColumns("No. Struk:", transaction.invoiceNumber, width).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Kasir:", transaction.cashierName, width).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Waktu:", dateStr, width).toByteArray(Charsets.US_ASCII))
            val orderTypeLabel = if (transaction.orderType == com.example.data.model.OrderType.DINE_IN) {
                if (transaction.tableNumber.isNotBlank()) "Makan di Tempat (M${transaction.tableNumber})" else "Makan di Tempat"
            } else {
                "Di Bungkus"
            }
            baos.write(formatTwoColumns("Tipe:", orderTypeLabel, width).toByteArray(Charsets.US_ASCII))
            if (transaction.customerName.isNotBlank() && transaction.customerName != "Pelanggan Umum") {
                baos.write(formatTwoColumns("Pelanggan:", transaction.customerName, width).toByteArray(Charsets.US_ASCII))
            }
            baos.write("${"-".repeat(width)}\n".toByteArray(Charsets.US_ASCII))

            // Items list
            for (item in items) {
                baos.write("${item.productName}\n".toByteArray(Charsets.US_ASCII))
                val detail = "  ${item.quantity} x ${formatRupiah(item.unitPrice)}"
                val subtotal = formatRupiah(item.subtotal)
                baos.write(formatTwoColumns(detail, subtotal, width).toByteArray(Charsets.US_ASCII))
            }
            baos.write("${"-".repeat(width)}\n".toByteArray(Charsets.US_ASCII))

            // Totals
            baos.write(formatTwoColumns("Subtotal:", formatRupiah(transaction.subtotal), width).toByteArray(Charsets.US_ASCII))
            if (transaction.discount > 0) {
                baos.write(formatTwoColumns("Diskon:", "-${formatRupiah(transaction.discount)}", width).toByteArray(Charsets.US_ASCII))
            }
            if (transaction.tax > 0) {
                baos.write(formatTwoColumns("PPN (11%):", formatRupiah(transaction.tax), width).toByteArray(Charsets.US_ASCII))
            }

            // Grand Total (Bold)
            baos.write(byteArrayOf(0x1B, 0x45, 0x01))
            baos.write(formatTwoColumns("TOTAL:", formatRupiah(transaction.grandTotal), width).toByteArray(Charsets.US_ASCII))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00))

            baos.write("${"-".repeat(width)}\n".toByteArray(Charsets.US_ASCII))

            // Payment Details
            baos.write(formatTwoColumns("Metode:", transaction.paymentMethod.label, width).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Bayar:", formatRupiah(transaction.amountPaid), width).toByteArray(Charsets.US_ASCII))
            baos.write(formatTwoColumns("Kembali:", formatRupiah(transaction.changeAmount), width).toByteArray(Charsets.US_ASCII))

            baos.write("${"=".repeat(width)}\n".toByteArray(Charsets.US_ASCII))

            // Center Align for Footer
            baos.write(byteArrayOf(0x1B, 0x61, 0x01))
            val footerLines = profile.receiptFooter.split("\n")
            for (line in footerLines) {
                baos.write("$line\n".toByteArray(Charsets.US_ASCII))
            }

            // Feed & paper cut
            baos.write(byteArrayOf(0x1B, 0x64, 0x04))

            return baos.toByteArray()
        }

        fun generateReceiptPlainText(
            profile: StoreProfile,
            transaction: Transaction,
            items: List<TransactionItem>,
            width: Int = 32
        ): String {
            val sb = StringBuilder()
            sb.appendLine(centerText(profile.storeName, width))
            sb.appendLine(centerText(profile.address, width))
            sb.appendLine(centerText("Telp: ${profile.phone}", width))
            sb.appendLine("-".repeat(width))

            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(transaction.timestamp))
            sb.append(formatTwoColumns("No. Struk:", transaction.invoiceNumber, width))
            sb.append(formatTwoColumns("Kasir:", transaction.cashierName, width))
            sb.append(formatTwoColumns("Waktu:", dateStr, width))
            val orderTypeLabel = if (transaction.orderType == com.example.data.model.OrderType.DINE_IN) {
                if (transaction.tableNumber.isNotBlank()) "Makan di Tempat (M${transaction.tableNumber})" else "Makan di Tempat"
            } else {
                "Di Bungkus"
            }
            sb.append(formatTwoColumns("Tipe:", orderTypeLabel, width))
            if (transaction.customerName.isNotBlank() && transaction.customerName != "Pelanggan Umum") {
                sb.append(formatTwoColumns("Pelanggan:", transaction.customerName, width))
            }
            sb.appendLine("-".repeat(width))

            for (item in items) {
                sb.appendLine(item.productName)
                val detail = "  ${item.quantity} x ${formatRupiah(item.unitPrice)}"
                val subtotal = formatRupiah(item.subtotal)
                sb.append(formatTwoColumns(detail, subtotal, width))
            }
            sb.appendLine("-".repeat(width))

            sb.append(formatTwoColumns("Subtotal:", formatRupiah(transaction.subtotal), width))
            if (transaction.discount > 0) {
                sb.append(formatTwoColumns("Diskon:", "-${formatRupiah(transaction.discount)}", width))
            }
            if (transaction.tax > 0) {
                sb.append(formatTwoColumns("PPN (11%):", formatRupiah(transaction.tax), width))
            }
            sb.append(formatTwoColumns("TOTAL:", formatRupiah(transaction.grandTotal), width))
            sb.appendLine("-".repeat(width))

            sb.append(formatTwoColumns("Metode:", transaction.paymentMethod.label, width))
            sb.append(formatTwoColumns("Bayar:", formatRupiah(transaction.amountPaid), width))
            sb.append(formatTwoColumns("Kembali:", formatRupiah(transaction.changeAmount), width))
            sb.appendLine("=".repeat(width))

            val footerLines = profile.receiptFooter.split("\n")
            for (line in footerLines) {
                sb.appendLine(centerText(line, width))
            }
            return sb.toString()
        }

        fun generateDailyReportPlainText(
            profile: StoreProfile,
            report: com.example.data.model.DailyReport,
            width: Int = 32
        ): String {
            val sb = StringBuilder()
            sb.appendLine(centerText(profile.storeName, width))
            sb.appendLine(centerText("REKAP LAPORAN HARIAN (ROOM DB)", width))
            sb.appendLine("=".repeat(width))
            sb.append(formatTwoColumns("Tanggal:", report.date, width))
            sb.append(formatTwoColumns("Kasir:", report.cashierNames, width))
            sb.append(formatTwoColumns("Total Shift:", "${report.shiftCount} Shift", width))
            sb.appendLine("-".repeat(width))
            sb.append(formatTwoColumns("TOTAL OMSET:", formatRupiah(report.totalRevenue), width))
            sb.append(formatTwoColumns("LABA BERSIH:", formatRupiah(report.totalProfit), width))
            sb.append(formatTwoColumns("Total Struk:", "${report.totalTransactions} Struk", width))
            sb.append(formatTwoColumns("Item Terjual:", "${report.totalItemsSold} Pcs", width))
            sb.appendLine("-".repeat(width))
            sb.append(formatTwoColumns("Tunai:", formatRupiah(report.totalCash), width))
            sb.append(formatTwoColumns("Non-Tunai:", formatRupiah(report.totalNonCash), width))
            sb.append(formatTwoColumns("  - QRIS:", formatRupiah(report.totalQris), width))
            if (report.totalTransfer > 0) {
                sb.append(formatTwoColumns("  - Transfer:", formatRupiah(report.totalTransfer), width))
            }
            if (report.totalDiscount > 0) {
                sb.append(formatTwoColumns("Diskon:", "-${formatRupiah(report.totalDiscount)}", width))
            }
            sb.appendLine("-".repeat(width))
            sb.append(formatTwoColumns("Kas Sistem:", formatRupiah(report.expectedCash), width))
            sb.append(formatTwoColumns("Kas Fisik:", formatRupiah(report.actualCash), width))
            val diffStr = if (report.cashDifference == 0.0) "Pas (Rp 0)" else formatRupiah(report.cashDifference)
            sb.append(formatTwoColumns("Selisih Kas:", diffStr, width))
            sb.appendLine("=".repeat(width))
            if (report.topProductSummary.isNotBlank()) {
                sb.appendLine("Menu Terlaris:")
                sb.appendLine(report.topProductSummary)
                sb.appendLine("-".repeat(width))
            }
            sb.appendLine(centerText("Arsip Room Database SQLite", width))
            return sb.toString()
        }

        private fun formatTwoColumns(left: String, right: String, totalWidth: Int): String {
            val spaceNeeded = totalWidth - left.length - right.length
            return if (spaceNeeded > 0) {
                "$left${" ".repeat(spaceNeeded)}$right\n"
            } else {
                "$left $right\n"
            }
        }

        private fun centerText(text: String, totalWidth: Int): String {
            if (text.length >= totalWidth) return text
            val padding = (totalWidth - text.length) / 2
            return " ".repeat(padding) + text
        }
    }
}
