package com.example.ui.screens

/**
 * Berisi template teks lengkap Source Code untuk Hosting & Web:
 * 1. Panduan Setup Firebase Realtime Database
 * 2. Portal Tutup Toko (tutup-toko.html)
 * 3. Portal RTDB Live (rtdb-laporan.html)
 * 4. Rules Realtime Database (database.rules.json)
 */
object SourceCodeTemplates {

    val FIREBASE_SETUP_GUIDE = """
# 👑 Panduan Setup Firebase Realtime Database & Portal Laporan Web

Web Laporan Harian Pemilik & Web Laporan Tutup Toko terhubung langsung ke Firebase Realtime Database.
Setiap transaksi dan penutupan shift kasir di aplikasi POS Android otomatis tersinkron real-time,
sehingga omset, uang kas laci, selisih kas, dan pesanan dapat dipantau dari mana saja lewat browser HP atau laptop secara instan tanpa Firestore.

---

## 🚀 Langkah 1: Buat Proyek Firebase (Gratis)
1. Buka [https://console.firebase.google.com/](https://console.firebase.google.com/) di browser.
2. Login menggunakan akun Google Anda.
3. Klik tombol **"Add project"** (Tambahkan proyek).
4. Beri nama proyek Anda (misal: `kasirpos-laporan-toko` atau `foodrock-report`).
5. Pada pilihan Google Analytics, lewati atau nonaktifkan, lalu klik **Create project**.

---

## 🗄️ Langkah 2: Buat Firebase Realtime Database
1. Di menu navigasi sebelah kiri, klik **Build** > **Realtime Database**.
2. Klik tombol **"Create Database"**.
3. Pilih lokasi server: **`asia-southeast1` (Singapura)** untuk kecepatan maksimal dan latensi sangat rendah.
4. Pada pilihan Security Rules, pilih **Start in test mode**, lalu klik **Enable**.

---

## 🔒 Langkah 3: Pasang Aturan Keamanan (Rules)
1. Masuk ke tab **Rules** di bagian atas halaman Realtime Database.
2. Pasang aturan berikut:
   ```json
   {
     "rules": {
       ".read": true,
       ".write": true
     }
   }
   ```
3. Klik tombol **Publish** (Publikasikan).

---

## 🌐 Langkah 4: Deploy Web ke Firebase Hosting
Anda dapat mendeploy file `owner.html` dan `index.html` agar memiliki domain web publik gratis seperti `https://<project-id>.web.app`:
1. Pasang Firebase CLI di laptop/komputer Anda:
   ```bash
   npm install -g firebase-tools
   ```
2. Login ke akun Google Anda:
   ```bash
   firebase login
   ```
3. Inisialisasi dan deploy hosting di folder web:
   ```bash
   firebase init hosting
   firebase deploy --only hosting
   ```
4. Salin URL hosting yang dihasilkan (contoh: `https://kasirpos-laporan.web.app`) ke menu aplikasi POS Android di bawah.

---

## 📱 Langkah 5: Hubungkan ke Aplikasi Kasir POS
1. Buka layar ini (**Hosting & Cadangan Cloud**).
2. Masukkan **Firebase Project ID** Anda pada kolom di atas.
3. Masukkan **Database ID** (`(default)`).
4. Tekan tombol **"Simpan ke Room DB"**.
5. Tekan tombol **"Sinkron Sekarang"** untuk mengirim seluruh data transaksi kasir ke cloud.
""".trimIndent()

    val FIRESTORE_RULES_CODE = """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    // 1. Koleksi Transaksi Penjualan Kasir POS Real-Time
    match /transactions/{transactionId} {
      allow read, write: if true;
    }

    // 2. Koleksi Laporan Tutup Kasir & Shift
    match /shift_reports/{reportId} {
      allow read, write: if true;
    }

    // 3. Koleksi Live Real-Time Dashboard Stats
    match /realtime_stats/{statId} {
      allow read, write: if true;
    }

    // 4. Koleksi Profil & Informasi Toko
    match /store_profile/{storeId} {
      allow read, write: if true;
    }

    // 5. Koleksi Katalog Produk Online
    match /products/{productId} {
      allow read, write: if true;
    }

    // 6. Dokumen Ping Pemeriksaan Koneksi Cloud
    match /_ping_check/{docId} {
      allow read, write: if true;
    }
  }
}
""".trimIndent()

    val CUSTOMER_TRACKING_HTML = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Pelacakan Pesanan & Menu Pelanggan - Kasir POS</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; }</style>
</head>
<body class="bg-slate-900 text-slate-100 min-h-screen">
  <!-- Header -->
  <header class="bg-slate-800/80 backdrop-blur border-b border-slate-700 sticky top-0 z-20 px-4 py-3">
    <div class="max-w-md mx-auto flex items-center justify-between">
      <div class="flex items-center gap-2.5">
        <div class="w-9 h-9 rounded-xl bg-orange-600 flex items-center justify-center text-lg font-bold">🏪</div>
        <div>
          <h1 class="text-sm font-bold text-white leading-tight">Kasir POS Nusantara</h1>
          <p class="text-[11px] text-emerald-400 font-medium">● Pelacakan Pesanan Real-Time</p>
        </div>
      </div>
      <span class="text-xs px-2.5 py-1 rounded-full bg-slate-700 text-slate-300 font-semibold">Meja / Takeaway</span>
    </div>
  </header>

  <!-- Konten Utama -->
  <main class="max-w-md mx-auto p-4 space-y-4">
    <!-- Form Pelacakan No. Pesanan / Meja -->
    <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700 shadow-lg">
      <label class="block text-xs font-bold text-slate-300 mb-1.5">Lacak Status Pesanan Anda:</label>
      <div class="flex gap-2">
        <input id="input-search-order" type="text" placeholder="Masukkan No. Invoice / Meja (contoh: TRX-001)"
               class="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-orange-500">
        <button id="btn-track-order" class="bg-orange-600 hover:bg-orange-500 text-white font-bold px-4 py-2 rounded-xl text-xs transition">
          Lacak
        </button>
      </div>
    </div>

    <!-- Hasil Pelacakan -->
    <div id="order-status-card" class="bg-slate-800/90 p-4 rounded-2xl border border-slate-700 space-y-3">
      <div class="flex items-center justify-between border-b border-slate-700 pb-2">
        <div>
          <span class="text-[10px] text-slate-400">Nomor Pesanan:</span>
          <p id="lbl-invoice" class="font-mono text-xs font-bold text-orange-400">TRX-20261025-001</p>
        </div>
        <span id="badge-status" class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
          👨‍🍳 Sedang Diproses
        </span>
      </div>

      <div class="space-y-1 text-xs text-slate-300">
        <div class="flex justify-between">
          <span class="text-slate-400">Pemesan:</span>
          <span id="lbl-customer" class="font-semibold text-white">Bpk. Hendra (Meja 03)</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Rincian Menu:</span>
          <span id="lbl-items" class="font-medium text-right text-slate-200">Kopi Susu Gula Aren (2x), Mie Goreng Spesial (1x)</span>
        </div>
        <div class="flex justify-between border-t border-slate-700 pt-2 font-bold">
          <span>Total Pembayaran:</span>
          <span id="lbl-total" class="text-emerald-400 text-sm">Rp 54.000 (LUNAS)</span>
        </div>
      </div>
    </div>

    <!-- Katalog Menu Populer -->
    <div class="space-y-2">
      <h2 class="text-xs font-bold text-slate-400 uppercase tracking-wider">Menu Favorit Hari Ini:</h2>
      <div class="grid grid-cols-2 gap-2.5">
        <div class="bg-slate-800 p-3 rounded-xl border border-slate-700">
          <span class="text-xl">☕</span>
          <h3 class="text-xs font-bold text-white mt-1">Kopi Susu Aren</h3>
          <p class="text-[11px] font-bold text-orange-400">Rp 18.000</p>
        </div>
        <div class="bg-slate-800 p-3 rounded-xl border border-slate-700">
          <span class="text-xl">🍜</span>
          <h3 class="text-xs font-bold text-white mt-1">Mie Goreng Spesial</h3>
          <p class="text-[11px] font-bold text-orange-400">Rp 18.000</p>
        </div>
        <div class="bg-slate-800 p-3 rounded-xl border border-slate-700">
          <span class="text-xl">🍗</span>
          <h3 class="text-xs font-bold text-white mt-1">Ayam Geprek Bawang</h3>
          <p class="text-[11px] font-bold text-orange-400">Rp 23.000</p>
        </div>
        <div class="bg-slate-800 p-3 rounded-xl border border-slate-700">
          <span class="text-xl">🥤</span>
          <h3 class="text-xs font-bold text-white mt-1">Teh Tarik Dingin</h3>
          <p class="text-[11px] font-bold text-orange-400">Rp 12.000</p>
        </div>
      </div>
    </div>
  </main>
</body>
</html>
""".trimIndent()

    val OWNER_PORTAL_HTML = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Portal Web Laporan Owner - Kasir POS Cloud</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; }</style>
</head>
<body class="bg-slate-50 text-slate-800 min-h-screen flex flex-col antialiased">
  <!-- Navbar -->
  <header class="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs px-4 py-3">
    <div class="max-w-7xl mx-auto flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white text-xl shadow-md">🏪</div>
        <div>
          <h1 class="text-base font-bold text-slate-900 leading-tight">Portal Laporan Owner</h1>
          <span class="text-xs text-emerald-600 font-semibold">● Online Real-Time Cloud Firestore</span>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button onclick="window.print()" class="px-3 py-1.5 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition">🖨️ Cetak PDF</button>
      </div>
    </div>
  </header>

  <!-- Dashboard Content -->
  <main class="max-w-7xl mx-auto px-4 py-6 w-full space-y-6">
    <!-- KPI Summary Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
        <span class="text-xs font-bold text-slate-400 uppercase">Omset Penjualan</span>
        <div id="stat-revenue" class="text-xl font-black text-slate-900 mt-1">Rp 0</div>
      </div>
      <div class="bg-white p-4 rounded-2xl border border-emerald-200 bg-emerald-50/20 shadow-xs">
        <span class="text-xs font-bold text-emerald-700 uppercase">Laba Bersih</span>
        <div id="stat-profit" class="text-xl font-black text-emerald-600 mt-1">Rp 0</div>
      </div>
      <div class="bg-white p-4 rounded-2xl border border-amber-200 bg-amber-50/20 shadow-xs">
        <span class="text-xs font-bold text-amber-700 uppercase">Kas Fisik Laci</span>
        <div id="stat-cash" class="text-xl font-black text-amber-600 mt-1">Rp 0</div>
      </div>
      <div class="bg-white p-4 rounded-2xl border border-blue-200 bg-blue-50/20 shadow-xs">
        <span class="text-xs font-bold text-blue-700 uppercase">Total Transaksi</span>
        <div id="stat-transactions" class="text-xl font-black text-blue-600 mt-1">0 Struk</div>
      </div>
    </div>

    <!-- Real-time Transactions Table -->
    <div class="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
      <div class="px-5 py-4 border-b border-slate-100 flex justify-between items-center">
        <h2 class="font-bold text-slate-900 text-sm">Daftar Transaksi Kasir Real-Time</h2>
        <span class="text-xs text-slate-500">Diperbarui otomatis via Firestore</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs">
          <thead class="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase">
            <tr>
              <th class="py-3 px-4">No. Invoice</th>
              <th class="py-3 px-4">Pelanggan</th>
              <th class="py-3 px-4">Kasir</th>
              <th class="py-3 px-4">Pesanan</th>
              <th class="py-3 px-4">Metode</th>
              <th class="py-3 px-4 text-right">Total</th>
            </tr>
          </thead>
          <tbody id="trx-table-body" class="divide-y divide-slate-100">
            <!-- Data transaksi disinkronkan real-time dari Firestore -->
          </tbody>
        </table>
      </div>
    </div>
  </main>
</body>
</html>
""".trimIndent()

    val REALTIME_DATABASE_RULES = """
{
  "rules": {
    "transactions": {
      ".read": true,
      ".write": true,
      ".indexOn": ["timestamp", "date", "storeId", "cashierName", "paymentMethod"],
      "${'$'}transactionId": {
        ".validate": "newData.hasChildren(['id', 'orderNumber', 'totalAmount', 'paymentMethod', 'date'])"
      }
    },
    "shift_reports": {
      ".read": true,
      ".write": true,
      ".indexOn": ["timestamp", "date", "storeId", "cashierName"],
      "${'$'}shiftId": {
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
""".trimIndent()

    val REPORT_TABLES_SCHEMA = """
{
  "// TABEL 1: TRANSAKSI PENJUALAN DETAIL": "transactions",
  "transactions": {
    "TRX-20261103-0001": {
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
  },

  "// TABEL 2: LAPORAN TUTUP KASIR (SHIFT REPORT)": "shift_reports",
  "shift_reports": {
    "SHIFT-20261103-01": {
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
      "notes": "Kas laci pas sesuai sistem",
      "date": "2026-11-03",
      "storeId": "foodrock-main",
      "status": "CLOSED"
    }
  },

  "// TABEL 3: REKAP OMSET HARIAN (DAILY REPORTS)": "daily_reports",
  "daily_reports": {
    "2026-11-03": {
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
  },

  "// TABEL 4: LIVE DASHBOARD OWNER (REALTIME STATS)": "realtime_stats",
  "realtime_stats": {
    "today": {
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
  },

  "// TABEL 5: PELACAKAN NOTA PUBLIK KONSUMEN": "customer_tracking",
  "customer_tracking": {
    "TRX-20261103-0001": {
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
  }
}
""".trimIndent()

    val RTDB_LAPORAN_HTML = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Realtime Database Laporan Kasir POS - Live Online</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; } .font-mono { font-family: 'JetBrains Mono', monospace; }</style>
</head>
<body class="bg-slate-950 text-slate-100 min-h-screen">
  <!-- Top Navigation & RTDB Connection Status -->
  <header class="bg-slate-900/90 border-b border-slate-800 sticky top-0 z-30 px-4 py-3 backdrop-blur">
    <div class="max-w-7xl mx-auto flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-orange-600 flex items-center justify-center text-xl shadow-lg">🔥</div>
        <div>
          <h1 class="text-base font-bold text-white flex items-center gap-2">
            Firebase Realtime DB Report
            <span class="text-[10px] px-2 py-0.5 rounded-full bg-orange-950 border border-orange-500/50 text-orange-400 font-bold">RTDB LIVE</span>
          </h1>
          <p class="text-xs text-slate-400">Database: <span class="text-amber-400 font-mono">foodrock-report-default-rtdb</span></p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <span id="badge-connection" class="flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-950 text-emerald-400 border border-emerald-800">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          Terhubung Realtime
        </span>
      </div>
    </div>
  </header>

  <!-- Live Tutup Kasir / Shift Audit Card -->
  <main class="max-w-7xl mx-auto p-4 space-y-6">
    <div class="bg-gradient-to-r from-slate-900 to-slate-850 p-6 rounded-2xl border border-slate-800 shadow-xl">
      <div class="flex items-center justify-between mb-4">
        <div>
          <span class="text-xs font-bold text-amber-400 uppercase tracking-wider">Audit Kas Laci & Shift Kasir</span>
          <h2 class="text-lg font-bold text-white">Status Tutup Kasir Terbaru</h2>
        </div>
        <span class="text-xs px-3 py-1 bg-emerald-900/60 text-emerald-300 rounded-full font-bold">AUTO PUSH RTDB</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Total Omset Shift</span>
          <div id="stat-shift-revenue" class="text-xl font-black text-amber-400 mt-1">Rp 0</div>
        </div>
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Uang Fisik Kasir</span>
          <div id="stat-actual-cash" class="text-xl font-black text-emerald-400 mt-1">Rp 0</div>
        </div>
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Kas Seharusnya (Sistem)</span>
          <div id="stat-expected-cash" class="text-xl font-black text-cyan-400 mt-1">Rp 0</div>
        </div>
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Selisih Kas Laci</span>
          <div id="stat-cash-diff" class="text-xl font-black text-slate-300 mt-1">Rp 0</div>
        </div>
      </div>
    </div>
  </main>
</body>
</html>
""".trimIndent()

    val TUTUP_TOKO_HTML = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Laporan Tutup Toko & Kasir - Realtime Database Live</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; } .font-mono { font-family: 'JetBrains Mono', monospace; }</style>
</head>
<body class="bg-slate-950 text-slate-100 min-h-screen">
  <!-- Portal Laporan Tutup Toko 100% Realtime Database Murni (Tanpa Firestore) -->
  <!-- Silakan buka dan jalankan file web-dashboard/tutup-toko.html -->
</body>
</html>
""".trimIndent()
}

