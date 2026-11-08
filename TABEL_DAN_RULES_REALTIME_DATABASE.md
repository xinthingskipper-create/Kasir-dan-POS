# 📊 Rules & Struktur Tabel Laporan Firebase Realtime Database (`foodrock-report`)

Panduan ini dibuat khusus untuk database Firebase Realtime Database Anda:
- **Project ID**: `foodrock-report`
- **Database Instance**: `foodrock-report-default-rtdb`
- **URL Database**: `https://foodrock-report-default-rtdb.firebaseio.com` *(atau `https://foodrock-report-default-rtdb.asia-southeast1.firebasedatabase.app`)*

---

## 🔒 1. Rules Database (Menggantikan Rules Sementara yang Expired)

Pada foto yang Anda kirimkan, database Anda saat ini menggunakan aturan sementara yang akan kedaluwarsa pada **3 Desember 2026**:
```json
{
  "rules": {
    ".read": "now < 1796230800000",
    ".write": "now < 1796230800000"
  }
}
```

### 📋 Solusi: Salin Rules Permanen & Terstruktur Berikut Ini:
Salin kode JSON di bawah ini, lalu tempelkan (*paste*) langsung ke tab **Rules** di Firebase Console (layar yang terbuka di laptop Anda), lalu klik tombol **Publish**:

```json
{
  "rules": {
    "transactions": {
      ".read": true,
      ".write": true,
      ".indexOn": ["timestamp", "date", "storeId", "cashierName", "paymentMethod"],
      "$transactionId": {
        ".validate": "newData.hasChildren(['id', 'orderNumber', 'totalAmount', 'paymentMethod', 'date'])"
      }
    },
    "shift_reports": {
      ".read": true,
      ".write": true,
      ".indexOn": ["timestamp", "date", "storeId", "cashierName"],
      "$shiftId": {
        ".validate": "newData.hasChildren(['id', 'cashierName', 'totalRevenue', 'timestamp'])"
      }
    },
    "daily_reports": {
      ".read": true,
      ".write": true,
      ".indexOn": ["date", "lastUpdated"]
    },
    "realtime_stats": {
      ".read": true,
      ".write": true
    },
    "customer_tracking": {
      ".read": true,
      ".write": true,
      ".indexOn": ["orderNumber", "date", "status"]
    },
    "store_profile": {
      ".read": true,
      ".write": true
    },
    "products": {
      ".read": true,
      ".write": true,
      ".indexOn": ["category", "barcode", "name"]
    }
  }
}
```

> **Keunggulan Rules Ini**:
> 1. **Permanen**: Tidak memiliki tanggal kedaluwarsa (*no expiration*).
> 2. **Performa Tinggi**: Dilengkapi indeks `.indexOn` untuk query filter tanggal, kasir, dan metode pembayaran.
> 3. **Integritas Data**: Menggunakan `.validate` sehingga mencegah transaksi kosong atau corrupt.

---

## 🗄️ 2. Struktur Tabel Laporan (Skema Node JSON)

Dalam Firebase Realtime Database, tabel berbentuk pohon JSON (*JSON Tree*). Berikut adalah 5 tabel utama untuk pelaporan kasir dan pemantauan pemilik:

### 📑 Tabel 1: `transactions` (Laporan Detail Setiap Penjualan)
Menyimpan setiap nota transaksi kasir secara lengkap dan real-time.
- **Node Path**: `/transactions/{invoiceNumber}`
- **Struktur Field**:
  ```json
  {
    "id": "TRX-20261103-0001",
    "orderNumber": "TRX-20261103-0001",
    "customerName": "Budi Santoso",
    "orderType": "DINE_IN",
    "tableNumber": "Meja 04",
    "itemsSummary": "Nasi Goreng Spesial (2x), Es Teh Manis (2x)",
    "totalAmount": 65000,
    "profitAmount": 28000,
    "totalItemsCount": 4,
    "paymentMethod": "QRIS",
    "cashGiven": 65000,
    "changeAmount": 0,
    "cashierName": "Siti Kasir",
    "cashierRole": "Kasir",
    "timestamp": "2026-11-03T12:00:00Z",
    "date": "2026-11-03",
    "storeId": "foodrock-main",
    "notes": "Pedas sedang"
  }
  ```

---

### 📑 Tabel 2: `shift_reports` (Laporan Tutup Kasir / Audit Laci)
Menyimpan rekap setiap kasir melakukan pergantian giliran kerja atau tutup toko.
- **Node Path**: `/shift_reports/{shiftId}`
- **Struktur Field**:
  ```json
  {
    "id": "SHIFT-20261103-01",
    "shiftNumber": "Shift-Pagi",
    "cashierName": "Siti Kasir",
    "cashierRole": "Kasir Pagi",
    "startTime": "2026-11-03T07:00:00Z",
    "endTime": "2026-11-03T15:00:00Z",
    "startingCash": 200000,
    "cashSales": 450000,
    "qrisSales": 320000,
    "transferSales": 150000,
    "totalRevenue": 920000,
    "totalProfit": 410000,
    "expectedCashInDrawer": 650000,
    "actualCashInDrawer": 650000,
    "cashDifference": 0,
    "transactionCount": 18,
    "notes": "Kas laci pas",
    "date": "2026-11-03",
    "storeId": "foodrock-main",
    "status": "CLOSED"
  }
  ```

---

### 📑 Tabel 3: `daily_reports` (Rekap Omset Harian Otomatis)
Menyimpan ringkasan omset dan laba per tanggal agar grafik dan rekap laporan bulanan owner termuat sangat cepat.
- **Node Path**: `/daily_reports/{YYYY-MM-DD}`
- **Struktur Field**:
  ```json
  {
    "date": "2026-11-03",
    "storeId": "foodrock-main",
    "totalRevenue": 920000,
    "totalProfit": 410000,
    "totalTransactions": 18,
    "totalCash": 450000,
    "totalQris": 320000,
    "totalTransfer": 150000,
    "lastUpdated": "2026-11-03T15:00:00Z"
  }
  ```

---

### 📑 Tabel 4: `realtime_stats` (Monitor Langsung / Live Ticker Pemilik)
Digunakan oleh Web Owner untuk menampilkan transaksi terbaru secara instan begitu kasir menekan tombol Simpan & Cetak.
- **Node Path**: `/realtime_stats/today`
- **Struktur Field**:
  ```json
  {
    "lastTransactionAmount": 65000,
    "lastInvoiceNumber": "TRX-20261103-0001",
    "lastCustomerName": "Budi Santoso",
    "lastPaymentMethod": "QRIS",
    "lastCashierName": "Siti Kasir",
    "todayRevenue": 920000,
    "todayTransactionsCount": 18,
    "lastUpdated": "2026-11-03T12:00:00Z",
    "date": "2026-11-03"
  }
  ```

---

### 📑 Tabel 5: `customer_tracking` (Tabel Publik Pelacakan Nota Konsumen)
Tabel khusus yang hanya berisi data pesanan konsumen tanpa menampilkan laba/profit toko, untuk diakses oleh pelanggan di `index.html`.
- **Node Path**: `/customer_tracking/{orderNumber}`
- **Struktur Field**:
  ```json
  {
    "orderNumber": "TRX-20261103-0001",
    "customerName": "Budi Santoso",
    "orderType": "DINE_IN",
    "itemsSummary": "Nasi Goreng Spesial (2x), Es Teh Manis (2x)",
    "totalAmount": 65000,
    "status": "SELESAI",
    "paymentStatus": "LUNAS",
    "date": "2026-11-03",
    "timestamp": "2026-11-03T12:00:00Z"
  }
  ```

---

### 📑 Tabel 6: `products` (Katalog Inventaris & Menu)
Menyimpan daftar produk, harga modal, harga jual, dan stok barang toko.
- **Node Path**: `/products/{productId}`
- **Struktur Field**:
  ```json
  {
    "id": "PRD-001",
    "name": "Nasi Goreng Spesial",
    "category": "Makanan",
    "buyPrice": 15000,
    "sellPrice": 25000,
    "stock": 48,
    "minStockAlert": 10,
    "unit": "porsi",
    "barcode": "899123456001"
  }
  ```

---

## 💡 3. Cara Memasukkan Struktur Tabel ke Firebase Console
Anda bisa mengisi struktur tabel ini dengan dua cara mudah:
1. **Otomatis dari Kasir**: Cukup masukkan Project ID `foodrock-report` di menu **Hosting & Cadangan Cloud** aplikasi kasir POS. Setiap transaksi baru akan otomatis membuat node-node di atas!
2. **Import JSON Sekaligus**:
   - Di Firebase Console pada laptop Anda, klik tab **Data** (sebelah tab **Rules**).
   - Klik ikon titik tiga (⋮) di sebelah kanan atas root database.
   - Pilih **Import JSON**.
   - Unggah berkas `realtime_database_schema.json` yang telah disediakan.
