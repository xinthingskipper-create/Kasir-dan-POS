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
sehingga total omset, uang fisik kas laci, dan pesanan dapat dipantau dari mana saja lewat browser HP atau laptop secara instan tanpa Firestore.

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

    val ONLINE_REPORT_RTDB_HTML = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Portal Laporan & Pencatatan Transaksi Online Chrome - Firebase RTDB</title>
  <!-- Tailwind CSS & Chart.js CDN -->
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <style>
    body { font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-mono { font-family: 'JetBrains Mono', monospace; }
  </style>
</head>
<body class="bg-slate-950 text-slate-100 min-h-screen selection:bg-orange-500 selection:text-white">

  <!-- TOP HEADER WITH LIVE RTDB STATUS & RECORDING ACTIONS -->
  <header class="bg-slate-900/95 border-b border-slate-800 p-4 sticky top-0 z-30 backdrop-blur-md">
    <div class="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-orange-600 to-amber-500 flex items-center justify-center font-bold text-white shadow-lg shadow-orange-500/20">🔥</div>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-base font-extrabold text-white">Laporan & Pencatatan Transaksi Online</h1>
            <span class="text-[10px] font-bold bg-orange-500/10 text-orange-400 border border-orange-500/30 px-2 py-0.5 rounded-full">RTDB Murni</span>
          </div>
          <p class="text-xs text-slate-400">Database: <span class="font-mono text-emerald-400 font-bold">foodrock-report-default-rtdb</span> • Bebas Akses via Google Chrome</p>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex items-center gap-2 flex-wrap">
        <button onclick="openModalCreateTrx()" class="px-3.5 py-1.5 bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-500 hover:to-amber-500 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-orange-600/30 cursor-pointer">
          <span>➕ Catat Transaksi Baru</span>
        </button>
        <button onclick="openModalCreateShift()" class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-indigo-600/20 cursor-pointer">
          <span>📋 Catat Tutup Toko</span>
        </button>
        <button onclick="loadData()" class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-bold border border-slate-700 transition cursor-pointer">
          <span>Perbarui Data</span>
        </button>
      </div>
    </div>
  </header>

  <!-- MAIN DASHBOARD -->
  <main class="max-w-7xl mx-auto p-4 sm:p-6 space-y-6">

    <!-- KPI Cards: Pendapatan Kotor, Pengeluaran, Kas Fisik Laci, Non-Tunai, Arsip Laporan -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
      <div class="bg-gradient-to-br from-slate-900 to-slate-900/80 border border-slate-800 p-5 rounded-2xl shadow-lg">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-bold text-emerald-400 uppercase tracking-wider">Pendapatan Kotor</span>
          <span class="text-lg">💰</span>
        </div>
        <div id="stat-revenue" class="text-2xl font-black text-white font-mono mt-1">Rp 0</div>
        <p class="text-[11px] text-slate-400 mt-2">Seluruh transaksi penjualan kotor</p>
      </div>

      <div class="bg-gradient-to-br from-slate-900 to-slate-900/80 border border-slate-800 p-5 rounded-2xl shadow-lg">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-bold text-rose-400 uppercase tracking-wider">Pengeluaran</span>
          <span class="text-lg">💸</span>
        </div>
        <div id="stat-expenses" class="text-2xl font-black text-rose-400 font-mono mt-1">Rp 0</div>
        <p id="stat-expenses-count" class="text-[11px] text-slate-400 mt-2">0 catatan pengeluaran</p>
      </div>

      <div class="bg-gradient-to-br from-slate-900 to-slate-900/80 border border-slate-800 p-5 rounded-2xl shadow-lg">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-bold text-amber-400 uppercase tracking-wider">Kas Fisik di Laci</span>
          <span class="text-lg">💵</span>
        </div>
        <div id="stat-cash" class="text-2xl font-black text-amber-400 font-mono mt-1">Rp 0</div>
        <p class="text-[11px] text-slate-400 mt-2">Uang fisik kasir di kasir POS</p>
      </div>

      <div class="bg-gradient-to-br from-slate-900 to-slate-900/80 border border-slate-800 p-5 rounded-2xl shadow-lg">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-bold text-purple-400 uppercase tracking-wider">Non-Tunai (QRIS & EDC)</span>
          <span class="text-lg">📱</span>
        </div>
        <div id="stat-noncash" class="text-2xl font-black text-purple-400 font-mono mt-1">Rp 0</div>
        <p class="text-[11px] text-slate-400 mt-2">QRIS, Transfer, dan Kartu Debit</p>
      </div>

      <div class="bg-gradient-to-br from-slate-900 to-slate-900/80 border border-slate-800 p-5 rounded-2xl shadow-lg">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-bold text-sky-400 uppercase tracking-wider">Arsip Laporan</span>
          <span class="text-lg">🗄️</span>
        </div>
        <div id="stat-archive" class="text-2xl font-black text-sky-400 font-mono mt-1">0 Dokumen</div>
        <p class="text-[11px] text-slate-400 mt-2">Kirim ke DB saat Tutup Toko</p>
      </div>
    </div>

    <!-- Laporan Transaksi Berdasarkan Metode Pembayaran -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg">
      <div class="flex items-center justify-between">
        <h2 class="text-sm font-bold text-white flex items-center gap-2">
          <span>💳</span>
          <span>Laporan Transaksi Pesanan Berdasarkan Metode Pembayaran</span>
        </h2>
        <span class="text-[10px] text-slate-400 font-mono">LIVE RTDB</span>
      </div>
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div class="bg-slate-950 p-3.5 rounded-xl border border-amber-500/30">
          <span class="text-[11px] font-bold text-amber-400">💵 Kas Tunai (Cash)</span>
          <div id="pm-cash" class="text-lg font-black text-amber-300 font-mono mt-1">Rp 0</div>
          <span id="pm-cash-count" class="text-[10px] text-slate-400">0 pesanan</span>
        </div>
        <div class="bg-slate-950 p-3.5 rounded-xl border border-purple-500/30">
          <span class="text-[11px] font-bold text-purple-400">📱 QRIS Digital</span>
          <div id="pm-qris" class="text-lg font-black text-purple-300 font-mono mt-1">Rp 0</div>
          <span id="pm-qris-count" class="text-[10px] text-slate-400">0 pesanan</span>
        </div>
        <div class="bg-slate-950 p-3.5 rounded-xl border border-sky-500/30">
          <span class="text-[11px] font-bold text-sky-400">🏦 Transfer Bank</span>
          <div id="pm-transfer" class="text-lg font-black text-sky-300 font-mono mt-1">Rp 0</div>
          <span id="pm-transfer-count" class="text-[10px] text-slate-400">0 pesanan</span>
        </div>
        <div class="bg-slate-950 p-3.5 rounded-xl border border-indigo-500/30">
          <span class="text-[11px] font-bold text-indigo-400">💳 Kartu / EDC</span>
          <div id="pm-edc" class="text-lg font-black text-indigo-300 font-mono mt-1">Rp 0</div>
          <span id="pm-edc-count" class="text-[10px] text-slate-400">0 pesanan</span>
        </div>
      </div>
    </div>

    <!-- Pencarian Tanggal Laporan & Transaksi -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-lg">
      <div class="flex items-center gap-2.5">
        <span class="text-xl">📅</span>
        <div>
          <h3 class="text-sm font-bold text-white">Pencarian Tanggal Laporan Tutup Kasir & Rincian Transaksi</h3>
          <p class="text-xs text-slate-400">Pilih tanggal untuk melihat laporan tutup kasir dan rincian transaksi penjualan</p>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-wrap">
        <button onclick="resetToTodayFilter()" class="px-3 py-1.5 bg-orange-600 hover:bg-orange-500 text-white rounded-xl text-xs font-bold cursor-pointer">Hari Ini (Reset Harian)</button>
        <input type="date" id="search-date" onchange="filterByDate(this.value)" class="bg-slate-950 border border-orange-500/50 rounded-xl px-3 py-1.5 text-xs text-white font-mono focus:outline-none cursor-pointer" title="Cari tanggal laporan tutup kasir & transaksi" />
        <button onclick="showAllDates()" class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold cursor-pointer">Semua Tanggal</button>
      </div>
    </div>

    <!-- Tabel Pengeluaran & Catatan Pengeluaran -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-sm font-bold text-white flex items-center gap-2">
            <span>💸</span>
            <span>Laporan Pengeluaran & Catatan Pengeluaran</span>
          </h2>
          <p class="text-xs text-slate-400">Rincian beban operasional, belanja bahan baku & catatan pengeluaran kasir</p>
        </div>
        <span id="expenses-count-badge" class="px-2.5 py-1 bg-rose-950 text-rose-300 border border-rose-800 rounded-lg text-xs font-mono font-bold">0 Catatan</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs font-mono">
          <thead class="text-slate-400 border-b border-slate-800 pb-2">
            <tr>
              <th class="py-2.5 px-3">Waktu</th>
              <th class="py-2.5 px-3 font-sans">Judul</th>
              <th class="py-2.5 px-3 font-sans">Kategori</th>
              <th class="py-2.5 px-3 font-sans">Catatan Pengeluaran</th>
              <th class="py-2.5 px-3 font-sans">Petugas</th>
              <th class="py-2.5 px-3 text-right">Nominal</th>
            </tr>
          </thead>
          <tbody id="tbody-expenses" class="divide-y divide-slate-800/80">
            <tr><td colspan="6" class="py-6 text-center text-slate-500 font-sans">Memuat data pengeluaran & catatan...</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tabel Arsip Laporan (Dikirim ke Database saat Tutup Toko) -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-lg">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 class="text-sm font-bold text-white flex items-center gap-2">
            <span>🗄️</span>
            <span>Arsip Laporan (Dikirim ke Database Saat Tutup Toko)</span>
          </h2>
          <p class="text-xs text-slate-400">Riwayat arsip laporan hanya dikirim ke database ketika tombol Tutup Toko ditekan</p>
        </div>
        <div class="flex items-center gap-2">
          <input type="text" id="search-archive" oninput="filterArchiveTable()" placeholder="Cari arsip laporan..." class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 w-44" />
          <button onclick="loadData()" class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-bold transition">
            🔄 Segarkan
          </button>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs font-mono">
          <thead class="text-slate-400 border-b border-slate-800 pb-2">
            <tr>
              <th class="py-2.5 px-3">Waktu Arsip</th>
              <th class="py-2.5 px-3">Tipe Dokumen</th>
              <th class="py-2.5 px-3">Kode / ID Dokumen</th>
              <th class="py-2.5 px-3">Petugas / Kasir</th>
              <th class="py-2.5 px-3 text-right">Pendapatan Kotor</th>
              <th class="py-2.5 px-3 text-right">Kas Fisik Laci</th>
              <th class="py-2.5 px-3 text-center">Status</th>
            </tr>
          </thead>
          <tbody id="tbody-archive" class="divide-y divide-slate-800/80">
            <tr><td colspan="7" class="py-6 text-center text-slate-500 font-sans">Memuat arsip laporan dari Realtime Database...</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tabel Laporan Tutup Shift Kasir -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg">
      <div class="flex items-center justify-between">
        <h2 class="text-sm font-bold text-white flex items-center gap-2">
          <span>📋</span>
          <span>Laporan Tutup Toko & Shift Kasir</span>
        </h2>
        <button onclick="openModalCreateShift()" class="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-bold transition">
          ➕ Catat Tutup Toko
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs font-mono">
          <thead class="text-slate-400 border-b border-slate-800 pb-2">
            <tr>
              <th class="py-2.5 px-3">Waktu Tutup</th>
              <th class="py-2.5 px-3">ID Shift</th>
              <th class="py-2.5 px-3">Kasir</th>
              <th class="py-2.5 px-3 text-right">Total Omset</th>
              <th class="py-2.5 px-3 text-right">Kas Fisik Laci</th>
              <th class="py-2.5 px-3 text-right">Non-Tunai</th>
            </tr>
          </thead>
          <tbody id="tbody-shifts" class="divide-y divide-slate-800/80">
            <tr><td colspan="6" class="py-6 text-center text-slate-500 font-sans">Memuat laporan tutup toko...</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tabel Rincian Transaksi Penjualan -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-sm font-bold text-white flex items-center gap-2">
            <span>🧾</span>
            <span>Rincian Transaksi Penjualan</span>
          </h2>
          <p class="text-xs text-slate-400">Daftar nota transaksi pesanan pelanggan sesuai pencarian tanggal</p>
        </div>
        <span id="trx-count-badge" class="px-2.5 py-1 bg-sky-950 text-sky-400 border border-sky-800 rounded-lg text-xs font-mono font-bold">0 Nota</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs font-mono">
          <thead class="text-slate-400 border-b border-slate-800 pb-2">
            <tr>
              <th class="py-2.5 px-3">No. Struk</th>
              <th class="py-2.5 px-3">Waktu Transaksi</th>
              <th class="py-2.5 px-3">Kasir</th>
              <th class="py-2.5 px-3">Pelanggan</th>
              <th class="py-2.5 px-3">Menu / Item Belanja</th>
              <th class="py-2.5 px-3 text-center">Metode</th>
              <th class="py-2.5 px-3 text-right">Total Belanja</th>
            </tr>
          </thead>
          <tbody id="tbody-transactions" class="divide-y divide-slate-800/80">
            <tr><td colspan="7" class="py-6 text-center text-slate-500 font-sans">Memuat rincian transaksi...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </main>

  <!-- MODAL: CATAT TUTUP TOKO & SHIFT -->
  <div id="modal-create-shift" class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm hidden items-center justify-center p-4">
    <div class="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
      <div class="px-5 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/70">
        <h3 class="text-sm font-bold text-white flex items-center gap-2">
          <span>📋</span>
          <span>Catat Laporan Tutup Toko & Shift Kasir</span>
        </h3>
        <button onclick="closeModalCreateShift()" class="text-slate-400 hover:text-white text-lg">&times;</button>
      </div>
      <div class="p-5 overflow-y-auto space-y-3.5 text-xs">
        <div>
          <label class="font-bold text-slate-300 block mb-1">ID Shift:</label>
          <input type="text" id="m-shift-id" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-indigo-400 font-mono font-bold" readonly />
        </div>
        <div>
          <label class="font-bold text-slate-300 block mb-1">Nama Kasir:</label>
          <input type="text" id="m-shift-cashier" value="Kasir Utama" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">Penjualan Tunai (Rp):</label>
            <input type="number" id="m-shift-cash" value="0" min="0" oninput="calcMShiftTotal()" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono" />
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Penjualan Non-Tunai (Rp):</label>
            <input type="number" id="m-shift-noncash" value="0" min="0" oninput="calcMShiftTotal()" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono" />
          </div>
        </div>
        <div class="p-3 bg-amber-950/40 border border-amber-500/40 rounded-xl space-y-1.5">
          <label class="font-bold text-amber-400 block">💵 UANG FISIK KAS DI LACI (Uang Fisik Kasir):</label>
          <input type="number" id="m-shift-actual-cash" value="0" min="0" class="w-full bg-slate-900 border border-amber-500/60 rounded-xl px-3 py-2 text-base text-amber-300 font-mono font-black" />
          <span class="text-[10px] text-slate-400 block">Laporan murni tanpa selisih sesuai instruksi.</span>
        </div>
        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800 flex items-center justify-between">
          <span class="font-bold text-slate-400">Total Omset Kotor:</span>
          <span id="m-shift-total-lbl" class="text-xl font-black text-emerald-400 font-mono">Rp 0</span>
        </div>
      </div>
      <div class="p-4 border-t border-slate-800 bg-slate-950/70 flex justify-end gap-2.5">
        <button onclick="closeModalCreateShift()" class="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold">Batal</button>
        <button onclick="saveNewShift()" class="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold shadow-md shadow-indigo-600/30">💾 Simpan Laporan ke Database</button>
      </div>
    </div>
  </div>

  <!-- SCRIPT REALTIME DATABASE REST API -->
  <script>
    const DB_URL = "https://foodrock-report-default-rtdb.asia-southeast1.firebasedatabase.app";
    let cachedArchives = [];
    let cachedShifts = [];
    let cachedTransactions = [];
    let cachedDaily = [];
    let cachedExpenses = [];

    function getLocalDateStr(d = new Date()) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return y + '-' + m + '-' + day;
    }

    function extractDateStr(item) {
      if (!item) return '';
      if (typeof item === 'string') {
        if (/^\d{4}-\d{2}-\d{2}/.test(item)) {
          if (item.length > 10 && /([Zz]|[+-]\d{2}:?\d{2})$/.test(item)) {
            const p = new Date(item);
            if (!isNaN(p.getTime())) return getLocalDateStr(p);
          }
          return item.substring(0, 10);
        }
        const p = new Date(item);
        return !isNaN(p.getTime()) ? getLocalDateStr(p) : '';
      }
      if (item.date && /^\d{4}-\d{2}-\d{2}/.test(String(item.date))) {
        return String(item.date).substring(0, 10);
      }
      if (item.timestampMillis) {
        const d = new Date(Number(item.timestampMillis));
        if (!isNaN(d.getTime())) return getLocalDateStr(d);
      }
      return extractDateStr(item.timestamp || item.createdAt || '');
    }

    let lastKnownDate = getLocalDateStr(new Date());
    let activeDateFilter = lastKnownDate; // Default: Hari Ini agar reset ke Rp 0 saat ganti tanggal

    function formatRupiah(num) {
      return "Rp " + (Number(num) || 0).toLocaleString("id-ID");
    }

    function openModalCreateShift() {
      const d = new Date();
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      document.getElementById('m-shift-id').value = "SHIFT-" + y + m + day + "-01";
      calcMShiftTotal();
      document.getElementById('modal-create-shift').classList.remove('hidden');
      document.getElementById('modal-create-shift').classList.add('flex');
    }

    function closeModalCreateShift() {
      document.getElementById('modal-create-shift').classList.add('hidden');
      document.getElementById('modal-create-shift').classList.remove('flex');
    }

    function calcMShiftTotal() {
      const cash = Number(document.getElementById('m-shift-cash').value) || 0;
      const noncash = Number(document.getElementById('m-shift-noncash').value) || 0;
      document.getElementById('m-shift-total-lbl').textContent = formatRupiah(cash + noncash);
    }

    async function saveNewShift() {
      const shiftId = document.getElementById('m-shift-id').value;
      const cashier = document.getElementById('m-shift-cashier').value;
      const cash = Number(document.getElementById('m-shift-cash').value) || 0;
      const noncash = Number(document.getElementById('m-shift-noncash').value) || 0;
      const actualCash = Number(document.getElementById('m-shift-actual-cash').value) || 0;
      const total = cash + noncash;
      const now = new Date().toISOString();
      const todayDate = getLocalDateStr(new Date());
      const todayExpenses = cachedExpenses.filter(e => extractDateStr(e) === todayDate);
      const totalExp = todayExpenses.reduce((sum, e) => sum + (Number(e.amount) || 0), 0);
      const expenseNotesSummary = todayExpenses.map(e => (e.title || e.category) + ': ' + formatRupiah(e.amount) + (e.notes && e.notes !== '-' ? ' (' + e.notes + ')' : '')).join(' | ');

      const payload = {
        shiftId: shiftId,
        cashierName: cashier,
        totalRevenue: total,
        cashSales: cash,
        nonCashSales: noncash,
        totalExpenses: totalExp,
        expenseNotes: expenseNotesSummary,
        actualCashCount: actualCash,
        systemExpectedCash: actualCash,
        date: todayDate,
        timestamp: now
      };

      const dailyPayload = {
        date: todayDate,
        grossRevenue: total,
        totalRevenue: total,
        totalCash: cash,
        totalNonCash: noncash,
        totalExpenses: totalExp,
        expenseNotes: expenseNotesSummary,
        actualCashCount: actualCash,
        archivedByTutupToko: true,
        lastUpdated: now
      };

      try {
        await Promise.all([
          fetch(DB_URL + "/shift_reports/" + shiftId + ".json", { method: 'PUT', body: JSON.stringify(payload) }),
          fetch(DB_URL + "/daily_reports/" + todayDate + ".json", { method: 'PATCH', body: JSON.stringify(dailyPayload) })
        ]);
        alert("Laporan Tutup Toko " + shiftId + " berhasil dikirim ke Realtime Database!");
        closeModalCreateShift();
        loadData();
      } catch (e) {
        alert("Gagal menyimpan shift: " + e.message);
      }
    }

    function renderShiftsTable(shifts) {
      if (!shifts || shifts.length === 0) {
        document.getElementById('tbody-shifts').innerHTML = "<tr><td colspan='6' class='py-4 text-center text-slate-500'>Tidak ada laporan tutup shift pada tanggal ini.</td></tr>";
        return;
      }
      let html = "";
      shifts.forEach(s => {
        html += "<tr>" +
          "<td class='py-2 px-3 text-slate-400'>" + (s.timestamp ? s.timestamp.substring(0, 16).replace('T', ' ') : (s.date || '-')) + "</td>" +
          "<td class='py-2 px-3 font-bold text-indigo-400'>" + (s.shiftId || '-') + "</td>" +
          "<td class='py-2 px-3 text-white'>" + (s.cashierName || '-') + "</td>" +
          "<td class='py-2 px-3 text-right font-bold text-emerald-400'>" + formatRupiah(s.totalRevenue) + "</td>" +
          "<td class='py-2 px-3 text-right font-bold text-amber-400'>" + formatRupiah(s.actualCashCount || s.actualCash || s.physicalCash) + "</td>" +
          "<td class='py-2 px-3 text-right text-purple-400'>" + formatRupiah(s.nonCashSales || s.totalNonCash) + "</td>" +
        "</tr>";
      });
      document.getElementById('tbody-shifts').innerHTML = html;
    }

    function renderTransactionsTable(transactions) {
      const badge = document.getElementById('trx-count-badge');
      if (badge) badge.textContent = (transactions ? transactions.length : 0) + ' Nota';

      if (!transactions || transactions.length === 0) {
        document.getElementById('tbody-transactions').innerHTML = "<tr><td colspan='7' class='py-4 text-center text-slate-500'>Tidak ada transaksi pada tanggal ini (Mulai dari Rp 0).</td></tr>";
        return;
      }
      let html = "";
      transactions.forEach(t => {
        const pmRaw = (t.paymentMethod || 'CASH').toUpperCase();
        const pm = pmRaw === 'TUNAI' ? 'CASH' : pmRaw;
        let pmBadge = "bg-amber-950 text-amber-300 border border-amber-800";
        if (pm === 'QRIS') pmBadge = "bg-purple-950 text-purple-300 border border-purple-800";
        else if (pm === 'TRANSFER') pmBadge = "bg-sky-950 text-sky-300 border border-sky-800";
        else if (pm === 'EDC') pmBadge = "bg-indigo-950 text-indigo-300 border border-indigo-800";

        html += "<tr>" +
          "<td class='py-2 px-3 font-bold text-orange-400'>" + (t.invoiceNumber || t.id || '-') + "</td>" +
          "<td class='py-2 px-3 text-slate-400'>" + (t.timestamp || t.createdAt ? (t.timestamp || t.createdAt).substring(0, 16).replace('T', ' ') : (t.date || '-')) + "</td>" +
          "<td class='py-2 px-3 text-white'>" + (t.cashierName || '-') + "</td>" +
          "<td class='py-2 px-3 text-slate-300'>" + (t.customerName || 'Umum') + "</td>" +
          "<td class='py-2 px-3 text-slate-200'>" + (t.itemsSummary || (t.items && t.items[0] ? t.items[0].name : 'Pesanan')) + "</td>" +
          "<td class='py-2 px-3 text-center'><span class='px-2 py-0.5 rounded text-[10px] font-bold " + pmBadge + "'>" + pm + "</span></td>" +
          "<td class='py-2 px-3 text-right font-bold text-emerald-400'>" + formatRupiah(t.totalAmount || t.grandTotal) + "</td>" +
        "</tr>";
      });
      document.getElementById('tbody-transactions').innerHTML = html;
    }

    function renderArchiveTable(archives) {
      if (!archives || archives.length === 0) {
        document.getElementById('tbody-archive').innerHTML = "<tr><td colspan='7' class='py-4 text-center text-slate-500'>Belum ada arsip laporan pada tanggal ini.</td></tr>";
        return;
      }
      let html = "";
      archives.forEach(a => {
        let typeBadge = "";
        if (a.type === 'SHIFT') typeBadge = "<span class='px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-950 text-indigo-400 border border-indigo-800'>Tutup Shift</span>";
        else if (a.type === 'DAILY') typeBadge = "<span class='px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-800'>Rekap Harian</span>";
        else typeBadge = "<span class='px-2 py-0.5 rounded text-[10px] font-bold bg-amber-950 text-amber-400 border border-amber-800'>Arus Kas</span>";

        html += "<tr>" +
          "<td class='py-2.5 px-3 text-slate-400'>" + (a.date ? a.date.substring(0, 16).replace('T', ' ') : '-') + "</td>" +
          "<td class='py-2.5 px-3'>" + typeBadge + "</td>" +
          "<td class='py-2.5 px-3 font-bold text-white'>" + a.id + "</td>" +
          "<td class='py-2.5 px-3 text-slate-300'>" + a.cashier + "</td>" +
          "<td class='py-2.5 px-3 text-right font-bold text-emerald-400'>" + formatRupiah(a.total) + "</td>" +
          "<td class='py-2.5 px-3 text-right font-bold text-amber-400'>" + (a.actualCash !== null && a.actualCash !== undefined ? formatRupiah(a.actualCash) : '-') + "</td>" +
          "<td class='py-2.5 px-3 text-center'><span class='px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-900/50 text-emerald-300'>Tersimpan</span></td>" +
        "</tr>";
      });
      document.getElementById('tbody-archive').innerHTML = html;
    }

    function renderExpensesTable(expenses) {
      const badge = document.getElementById('expenses-count-badge');
      if (badge) badge.textContent = (expenses ? expenses.length : 0) + ' Catatan';

      if (!expenses || expenses.length === 0) {
        document.getElementById('tbody-expenses').innerHTML = "<tr><td colspan='6' class='py-4 text-center text-slate-500'>Belum ada pengeluaran & catatan pengeluaran pada tanggal ini.</td></tr>";
        return;
      }
      let html = "";
      expenses.forEach(e => {
        html += "<tr>" +
          "<td class='py-2 px-3 text-slate-400'>" + (e.timestamp ? e.timestamp.substring(0, 16).replace('T', ' ') : (e.date || '-')) + "</td>" +
          "<td class='py-2 px-3 font-sans font-bold text-white'>" + (e.title || '-') + "</td>" +
          "<td class='py-2 px-3 font-sans text-rose-300'>" + (e.category || 'Operasional') + "</td>" +
          "<td class='py-2 px-3 font-sans text-slate-300'>" + (e.notes || '-') + "</td>" +
          "<td class='py-2 px-3 font-sans text-slate-300'>" + (e.cashierName || 'Kasir') + "</td>" +
          "<td class='py-2 px-3 text-right font-bold text-rose-400'>- " + formatRupiah(e.amount) + "</td>" +
        "</tr>";
      });
      document.getElementById('tbody-expenses').innerHTML = html;
    }

    function applyFilterAndRender() {
      const targetDate = activeDateFilter;
      const filteredShifts = targetDate ? cachedShifts.filter(s => extractDateStr(s) === targetDate) : cachedShifts;
      const filteredTrxs = targetDate ? cachedTransactions.filter(t => extractDateStr(t) === targetDate) : cachedTransactions;
      const filteredExps = targetDate ? cachedExpenses.filter(e => extractDateStr(e) === targetDate) : cachedExpenses;
      const filteredArchs = targetDate ? cachedArchives.filter(a => a.day === targetDate) : cachedArchives;

      let rev = 0, cash = 0, qris = 0, transfer = 0, edc = 0;
      let cCash = 0, cQris = 0, cTransfer = 0, cEdc = 0;

      filteredTrxs.forEach(t => {
        const amt = Number(t.totalAmount || t.grandTotal) || 0;
        rev += amt;
        const pm = (t.paymentMethod || 'CASH').toUpperCase();
        if (pm === 'CASH' || pm === 'TUNAI') { cash += amt; cCash++; }
        else if (pm === 'QRIS') { qris += amt; cQris++; }
        else if (pm === 'TRANSFER') { transfer += amt; cTransfer++; }
        else if (pm === 'EDC' || pm === 'DEBIT' || pm === 'CARD') { edc += amt; cEdc++; }
        else { cash += amt; cCash++; }
      });

      const totalExp = filteredExps.reduce((sum, e) => sum + (Number(e.amount) || 0), 0);
      const lastActualCash = filteredShifts.length > 0
        ? (Number(filteredShifts[0].actualCashCount || filteredShifts[0].actualCash || filteredShifts[0].physicalCash) || 0)
        : 0;

      document.getElementById('stat-revenue').textContent = formatRupiah(rev);
      const statExp = document.getElementById('stat-expenses');
      if (statExp) statExp.textContent = formatRupiah(totalExp);
      const statExpCount = document.getElementById('stat-expenses-count');
      if (statExpCount) statExpCount.textContent = filteredExps.length + ' catatan pengeluaran';
      document.getElementById('stat-cash').textContent = formatRupiah(lastActualCash > 0 ? lastActualCash : cash);
      document.getElementById('stat-noncash').textContent = formatRupiah(qris + transfer + edc);
      document.getElementById('stat-archive').textContent = filteredArchs.length + ' Dokumen';

      document.getElementById('pm-cash').textContent = formatRupiah(cash);
      document.getElementById('pm-cash-count').textContent = cCash + ' pesanan';
      document.getElementById('pm-qris').textContent = formatRupiah(qris);
      document.getElementById('pm-qris-count').textContent = cQris + ' pesanan';
      document.getElementById('pm-transfer').textContent = formatRupiah(transfer);
      document.getElementById('pm-transfer-count').textContent = cTransfer + ' pesanan';
      document.getElementById('pm-edc').textContent = formatRupiah(edc);
      document.getElementById('pm-edc-count').textContent = cEdc + ' pesanan';

      renderExpensesTable(filteredExps);
      renderShiftsTable(filteredShifts);
      renderTransactionsTable(filteredTrxs);
      renderArchiveTable(targetDate ? filteredArchs : cachedArchives);
    }

    function filterByDate(dateVal) {
      if (!dateVal) {
        resetToTodayFilter();
        return;
      }
      activeDateFilter = dateVal;
      applyFilterAndRender();
    }

    function resetToTodayFilter() {
      activeDateFilter = getLocalDateStr(new Date());
      const input = document.getElementById('search-date');
      if (input) input.value = activeDateFilter;
      applyFilterAndRender();
    }

    function showAllDates() {
      activeDateFilter = '';
      const input = document.getElementById('search-date');
      if (input) input.value = '';
      applyFilterAndRender();
    }

    function filterArchiveTable() {
      const q = (document.getElementById('search-archive').value || '').toLowerCase();
      if (!q) {
        renderArchiveTable(cachedArchives);
        return;
      }
      const filtered = cachedArchives.filter(a =>
        a.id.toLowerCase().includes(q) ||
        a.cashier.toLowerCase().includes(q) ||
        a.type.toLowerCase().includes(q) ||
        (a.date && a.date.toLowerCase().includes(q))
      );
      renderArchiveTable(filtered);
    }

    async function loadData() {
      try {
        const [shifts, trxs, daily, flows, expensesData] = await Promise.all([
          fetch(DB_URL + "/shift_reports.json").then(r => r.json()).catch(() => null),
          fetch(DB_URL + "/transactions.json").then(r => r.json()).catch(() => null),
          fetch(DB_URL + "/daily_reports.json").then(r => r.json()).catch(() => null),
          fetch(DB_URL + "/cash_flows.json").then(r => r.json()).catch(() => null),
          fetch(DB_URL + "/expenses.json").then(r => r.json()).catch(() => null)
        ]);

        cachedShifts = shifts ? Object.values(shifts).reverse() : [];
        cachedTransactions = trxs ? Object.values(trxs).reverse() : [];

        // Pengeluaran & Catatan Pengeluaran (gabungan /expenses dan /cash_flows OUT tanpa duplikasi)
        const expMap = new Map();
        if (expensesData && typeof expensesData === 'object') {
          Object.entries(expensesData).forEach(([k, v]) => {
            if (!v || typeof v !== 'object') return;
            expMap.set(k, {
              id: k,
              title: v.title || v.category || 'Pengeluaran',
              category: v.category || 'Operasional',
              notes: v.notes || v.description || '-',
              amount: Number(v.amount) || 0,
              cashierName: v.cashierName || 'Kasir',
              date: v.date || extractDateStr(v),
              timestamp: v.timestamp || ''
            });
          });
        }
        if (flows && typeof flows === 'object') {
          Object.entries(flows).forEach(([k, f]) => {
            if (!f || typeof f !== 'object') return;
            const fType = (f.type || 'OUT').toUpperCase();
            if (fType === 'OUT' && !expMap.has(k)) {
              expMap.set(k, {
                id: k,
                title: f.description || f.category || 'Kas Keluar',
                category: f.category || 'Operasional',
                notes: f.description || f.notes || '-',
                amount: Number(f.amount) || 0,
                cashierName: f.cashierName || 'Staff',
                date: f.date || extractDateStr(f),
                timestamp: f.timestamp || ''
              });
            }
          });
        }
        cachedExpenses = Array.from(expMap.values()).sort((a, b) => (b.timestamp || b.date || '').localeCompare(a.timestamp || a.date || ''));

        // Susun daftar Arsip Laporan HANYA dari data Tutup Toko (/shift_reports, /daily_reports, dan /cash_flows)
        const archives = [];

        if (shifts) {
          Object.values(shifts).forEach(s => {
            archives.push({
              type: 'SHIFT',
              id: s.shiftId || s.id || 'SHIFT-UNK',
              date: s.timestamp || s.date || '',
              day: extractDateStr(s),
              cashier: s.cashierName || 'Kasir',
              total: Number(s.totalRevenue) || 0,
              actualCash: Number(s.actualCashCount || s.actualCash || s.physicalCash) || 0
            });
          });
        }
        if (daily) {
          Object.entries(daily).forEach(([k, d]) => {
            if (!d || typeof d !== 'object') return;
            const dStr = extractDateStr(d.date || k);
            if (!dStr) return;
            archives.push({
              type: 'DAILY',
              id: d.id || ('REKAP-' + dStr),
              date: dStr,
              day: dStr,
              cashier: d.closedBy || d.cashierNames || 'Tutup Toko',
              total: Number(d.grossRevenue || d.totalRevenue || d.totalOmset) || 0,
              actualCash: Number(d.actualCashCount || d.actualCash || d.totalCash) || null
            });
          });
        }

        if (flows) {
          Object.values(flows).forEach(f => {
            archives.push({
              type: 'CASHFLOW',
              id: f.id || 'ARUS-KAS',
              date: f.date || f.timestamp || '',
              day: extractDateStr(f.date || f.timestamp || ''),
              cashier: f.cashierName || 'Staff',
              total: Number(f.amount) || 0,
              actualCash: null
            });
          });
        }

        archives.sort((a, b) => (b.day || b.date || '').localeCompare(a.day || a.date || ''));
        cachedArchives = archives;
        applyFilterAndRender();

      } catch (e) {
        console.error(e);
      }
    }

    const searchInput = document.getElementById('search-date');
    if (searchInput) searchInput.value = activeDateFilter;
    loadData();
    setInterval(loadData, 10000);
    setInterval(() => {
      const curDate = getLocalDateStr(new Date());
      if (curDate !== lastKnownDate) {
        lastKnownDate = curDate;
        resetToTodayFilter();
        loadData();
      }
    }, 5000);
  </script>
</body>
</html>
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
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Total Omset Shift</span>
          <div id="stat-shift-revenue" class="text-xl font-black text-amber-400 mt-1">Rp 0</div>
        </div>
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Uang Fisik Kasir (Kas Laci)</span>
          <div id="stat-actual-cash" class="text-xl font-black text-emerald-400 mt-1">Rp 0</div>
        </div>
        <div class="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
          <span class="text-xs text-slate-400">Kas Menurut Sistem</span>
          <div id="stat-expected-cash" class="text-xl font-black text-cyan-400 mt-1">Rp 0</div>
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

