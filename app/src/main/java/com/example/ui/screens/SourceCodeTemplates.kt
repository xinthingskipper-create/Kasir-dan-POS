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
          <p class="text-xs text-slate-400">Database: <span id="hdr-db-label" class="font-mono text-emerald-400 font-bold">foodrock-report-default-rtdb</span> • Bebas Akses via Google Chrome</p>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex items-center gap-2 flex-wrap">
        <button onclick="document.getElementById('kp-report-card').scrollIntoView({behavior:'smooth'})" class="px-3 py-1.5 bg-[#00796B] hover:bg-[#00695C] text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-teal-700/30 cursor-pointer">
          <span>📊 Laporan Transaksi</span>
        </button>
        <button onclick="openModalCreateTrx()" class="px-3.5 py-1.5 bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-500 hover:to-amber-500 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-orange-600/30 cursor-pointer">
          <span>➕ Catat Transaksi Baru</span>
        </button>
        <button onclick="openModalCreateExpense()" class="px-3 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-rose-600/30 cursor-pointer">
          <span>💸 Catat Pengeluaran</span>
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
    <div id="rtdb-rules-warning" class="hidden bg-amber-950/70 border border-amber-500/50 text-amber-200 rounded-2xl px-4 py-3 text-xs flex items-center justify-between gap-3 flex-wrap">
      <div>
        <span class="font-bold text-amber-300">⚠️ Sebagian node pengeluaran RTDB dibatasi Rules (<span id="rtdb-rules-code">401</span>):</span>
        <span>Membaca lewat mirror statistik &amp; Relay POS. Pastikan Rules Firebase di menu Hosting Cloud aplikasi sudah di-Publish.</span>
      </div>
      <button type="button" onclick="loadData()" class="px-3 py-1 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 border border-amber-400/40 text-amber-200 font-bold cursor-pointer">Coba Muat Ulang</button>
    </div>

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

    <!-- Catatan Transaksi per Nota (Always Visible pada Laporan Hari Ini / Periode Aktif) -->
    <div id="daily-per-nota-section" class="bg-slate-900 border border-teal-500/30 rounded-2xl overflow-hidden shadow-xl">
      <div class="p-4 sm:p-5 border-b border-slate-800 bg-gradient-to-r from-teal-950/60 via-slate-900 to-slate-900 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div class="space-y-1">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="inline-flex items-center gap-1.5 bg-teal-500/20 text-teal-300 border border-teal-500/40 text-[10px] font-bold px-2.5 py-0.5 rounded-full uppercase tracking-wider">
              Catatan Otomatis per Nota
            </span>
            <span id="daily-per-nota-date-badge" class="text-[11px] font-mono font-bold text-orange-400">-</span>
          </div>
          <h2 id="daily-per-nota-title" class="text-sm sm:text-base font-extrabold text-white flex items-center gap-2">
            <span>🧾</span>
            <span>Catatan Transaksi per Nota (Hari Ini)</span>
          </h2>
          <p class="text-xs text-slate-400">Setiap transaksi dan pengeluaran dicatat lengkap per struk/nota beserta pendapatan, keuntungan, pengeluaran, dan laba bersih.</p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <span id="daily-per-nota-count-pill" class="px-2.5 py-1 rounded-xl bg-slate-950 border border-slate-700 text-sky-300 text-xs font-mono font-bold">0 Nota</span>
          <span id="daily-per-nota-rev-pill" class="px-2.5 py-1 rounded-xl bg-slate-950 border border-emerald-500/30 text-emerald-400 text-xs font-mono font-bold">Pendapatan: Rp 0</span>
          <span id="daily-per-nota-profit-pill" class="px-2.5 py-1 rounded-xl bg-slate-950 border border-teal-500/30 text-teal-300 text-xs font-mono font-bold">Keuntungan: Rp 0</span>
          <span id="daily-per-nota-expense-pill" class="px-2.5 py-1 rounded-xl bg-slate-950 border border-rose-500/30 text-rose-400 text-xs font-mono font-bold">Pengeluaran: Rp 0</span>
          <span id="daily-per-nota-net-pill" class="px-2.5 py-1 rounded-xl bg-slate-950 border border-emerald-400/40 text-emerald-300 text-xs font-mono font-bold">Laba Bersih: Rp 0</span>
          <button type="button" onclick="shareDailyPerNota()" class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-bold transition flex items-center gap-1.5 cursor-pointer">
            <span>📤</span>
            <span>Salin / Bagikan</span>
          </button>
          <button type="button" onclick="openKpTodayPerNota()" class="px-3 py-1.5 bg-[#00796B] hover:bg-[#00695C] text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-teal-700/30 cursor-pointer">
            <span>📊</span>
            <span>Grafik Per-Nota</span>
          </button>
        </div>
      </div>

      <div class="p-3 bg-slate-950/70 border-b border-slate-800 flex flex-wrap items-center justify-between gap-2.5">
        <div class="relative flex-1 min-w-[210px] max-w-md">
          <input type="text" id="input-search-daily-nota" placeholder="Cari no. nota, menu, pelanggan, kasir..." class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-teal-500" />
          <span class="absolute right-3 top-2.5 text-xs text-slate-500">🔍</span>
        </div>
        <div class="flex items-center gap-1.5 flex-wrap">
          <button type="button" class="daily-nota-pay-btn px-2.5 py-1 text-xs font-bold rounded-lg bg-[#00796B] text-white cursor-pointer" data-pay="ALL">Semua</button>
          <button type="button" class="daily-nota-pay-btn px-2.5 py-1 text-xs font-semibold rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer" data-pay="CASH">💵 Tunai</button>
          <button type="button" class="daily-nota-pay-btn px-2.5 py-1 text-xs font-semibold rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer" data-pay="QRIS">📱 QRIS</button>
          <button type="button" class="daily-nota-pay-btn px-2.5 py-1 text-xs font-semibold rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer" data-pay="TRANSFER">🏦 Transfer</button>
          <button type="button" class="daily-nota-pay-btn px-2.5 py-1 text-xs font-semibold rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer" data-pay="EDC">💳 EDC</button>
        </div>
      </div>

      <div id="daily-per-nota-list" class="divide-y divide-slate-800/70 max-h-[480px] overflow-y-auto bg-slate-900/50">
        <div class="py-10 text-center text-xs text-slate-500">Memuat catatan transaksi per nota...</div>
      </div>
      <div id="daily-per-nota-expenses-box" class="border-t border-slate-800 bg-slate-950/60"></div>
    </div>

    <!-- Laporan Transaksi (Menu Pilihan + Grafik Area Kuning Harian/Bulanan/Tahunan + Laporan Pengeluaran) -->
    <div id="kp-report-card" class="bg-white text-slate-900 rounded-2xl border border-slate-200 shadow-xl overflow-hidden">
      <!-- Screen 1: Menu 5 Pilihan -->
      <div id="kp-menu-view">
        <div class="flex items-center justify-between px-4 py-3.5 border-b border-slate-200 bg-white">
          <div class="flex items-center gap-2">
            <span class="text-[#00796B] text-xl font-bold px-1">&lt;</span>
            <h2 class="text-lg font-bold text-[#00796B] tracking-tight">Laporan Transaksi</h2>
          </div>
          <span class="text-[11px] font-semibold text-slate-400">Harian • Bulanan • Tahunan • Pengeluaran</span>
        </div>
        <div class="px-4 py-2.5 bg-[#F0FDFA] border-b border-teal-100 flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-1.5 flex-wrap">
            <button type="button" onclick="stepKpTargetDate(-1)" title="Tanggal Sebelumnya" class="px-2.5 py-1.5 rounded-lg bg-white border border-teal-200 text-[#00796B] hover:bg-teal-50 text-xs font-bold cursor-pointer">◀ Seblm</button>
            <input type="date" id="kp-menu-date-picker" onchange="setKpTargetDate(this.value)" class="px-2.5 py-1 rounded-lg bg-white border border-teal-200 text-slate-800 text-xs font-semibold focus:outline-none focus:border-[#00796B]" />
            <button type="button" id="kp-menu-btn-next-date" onclick="stepKpTargetDate(1)" title="Tanggal Berikutnya" class="px-2.5 py-1.5 rounded-lg bg-white border border-teal-200 text-[#00796B] hover:bg-teal-50 text-xs font-bold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed">Brkt ▶</button>
            <button type="button" id="kp-menu-btn-today-date" onclick="resetKpTargetDate()" class="hidden px-2.5 py-1.5 rounded-lg bg-[#00796B] text-white text-xs font-bold hover:bg-[#00695C] cursor-pointer">Hari Ini</button>
          </div>
          <span id="kp-menu-date-badge" class="px-2.5 py-1 rounded-full bg-teal-100 text-[#00796B] text-[11px] font-bold">Hari Ini</span>
        </div>
        <div class="divide-y divide-slate-100 bg-white">
          <button type="button" onclick="selectKpMenuOption('TODAY')" class="w-full flex items-center gap-4 px-5 py-4 hover:bg-teal-50/60 transition text-left cursor-pointer">
            <svg class="w-7 h-7 shrink-0 text-[#00796B]" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="24.5" x2="21.5" y2="24.5"/><line x1="6.2" y1="24.5" x2="6.2" y2="17.2"/><line x1="11.2" y1="24.5" x2="11.2" y2="12.2"/><line x1="16.2" y1="24.5" x2="16.2" y2="20"/><polyline points="6.2,12.8 11.2,7.8 16.8,6.2"/><circle cx="6.2" cy="12.8" r="2" fill="white"/><circle cx="11.2" cy="7.8" r="2" fill="white"/><circle cx="17.3" cy="13.4" r="6" fill="white"/><line x1="21.6" y1="17.7" x2="25.2" y2="21.3"/></svg>
            <div class="flex-1 min-w-0">
              <span class="block text-[15px] font-semibold text-slate-900">Laporan transaksi hari ini</span>
              <span id="kp-menu-today-sublabel" class="block text-[11px] font-medium text-slate-500">Hari Ini</span>
            </div>
          </button>
          <button type="button" onclick="selectKpMenuOption('THIS_MONTH')" class="w-full flex items-center gap-4 px-5 py-4 hover:bg-teal-50/60 transition text-left cursor-pointer">
            <svg class="w-7 h-7 shrink-0 text-[#00796B]" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="24.5" x2="21.5" y2="24.5"/><line x1="6.2" y1="24.5" x2="6.2" y2="17.2"/><line x1="11.2" y1="24.5" x2="11.2" y2="12.2"/><line x1="16.2" y1="24.5" x2="16.2" y2="20"/><polyline points="6.2,12.8 11.2,7.8 16.8,6.2"/><circle cx="6.2" cy="12.8" r="2" fill="white"/><circle cx="11.2" cy="7.8" r="2" fill="white"/><circle cx="17.3" cy="13.4" r="6" fill="white"/><line x1="21.6" y1="17.7" x2="25.2" y2="21.3"/></svg>
            <span class="text-[15px] font-semibold text-slate-900 flex-1">Laporan transaksi bulan ini</span>
          </button>
          <button type="button" onclick="selectKpMenuOption('THIS_YEAR')" class="w-full flex items-center gap-4 px-5 py-4 hover:bg-teal-50/60 transition text-left cursor-pointer">
            <svg class="w-7 h-7 shrink-0 text-[#00796B]" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="24.5" x2="21.5" y2="24.5"/><line x1="6.2" y1="24.5" x2="6.2" y2="17.2"/><line x1="11.2" y1="24.5" x2="11.2" y2="12.2"/><line x1="16.2" y1="24.5" x2="16.2" y2="20"/><polyline points="6.2,12.8 11.2,7.8 16.8,6.2"/><circle cx="6.2" cy="12.8" r="2" fill="white"/><circle cx="11.2" cy="7.8" r="2" fill="white"/><circle cx="17.3" cy="13.4" r="6" fill="white"/><line x1="21.6" y1="17.7" x2="25.2" y2="21.3"/></svg>
            <span class="text-[15px] font-semibold text-slate-900 flex-1">Laporan transaksi tahun ini</span>
          </button>
          <button type="button" onclick="selectKpMenuOption('ALL')" class="w-full flex items-center gap-4 px-5 py-4 hover:bg-teal-50/60 transition text-left cursor-pointer">
            <svg class="w-7 h-7 shrink-0 text-[#00796B]" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="24.5" x2="21.5" y2="24.5"/><line x1="6.2" y1="24.5" x2="6.2" y2="17.2"/><line x1="11.2" y1="24.5" x2="11.2" y2="12.2"/><line x1="16.2" y1="24.5" x2="16.2" y2="20"/><polyline points="6.2,12.8 11.2,7.8 16.8,6.2"/><circle cx="6.2" cy="12.8" r="2" fill="white"/><circle cx="11.2" cy="7.8" r="2" fill="white"/><circle cx="17.3" cy="13.4" r="6" fill="white"/><line x1="21.6" y1="17.7" x2="25.2" y2="21.3"/></svg>
            <span class="text-[15px] font-semibold text-slate-900 flex-1">Laporan semua transaksi</span>
          </button>
          <button type="button" id="kp-menu-expense-btn" onclick="selectKpExpenseReport()" class="w-full flex items-center gap-4 px-5 py-4 hover:bg-rose-50/70 transition text-left cursor-pointer">
            <svg class="w-7 h-7 shrink-0 text-rose-600" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="6" width="22" height="16" rx="2.5"/><line x1="3" y1="11" x2="25" y2="11"/><path d="M16 17h5"/><path d="M19 15l2 2-2 2"/></svg>
            <div class="flex-1 min-w-0">
              <span class="block text-[15px] font-semibold text-slate-900">Laporan pengeluaran</span>
              <span id="kp-menu-expense-sublabel" class="block text-[11px] font-medium text-rose-600">0 catatan • Rp 0</span>
            </div>
            <span class="text-rose-500 text-lg font-bold">&rsaquo;</span>
          </button>
        </div>
      </div>

      <!-- Screen 2: Detail Harian/Bulanan/Tahunan + Grafik Kuning -->
      <div id="kp-detail-view" class="hidden">
        <div class="flex items-center justify-between px-4 py-3 border-b border-slate-200 bg-white relative">
          <div class="flex items-center gap-2 min-w-0">
            <button type="button" onclick="backToKpMenu()" class="w-9 h-9 rounded-full hover:bg-slate-100 flex items-center justify-center text-[#00796B] text-xl font-bold cursor-pointer">&lt;</button>
            <div class="min-w-0">
              <h2 id="kp-detail-title" class="text-base sm:text-lg font-bold text-[#00796B] truncate">Laporan Transaksi per Hari</h2>
              <p id="kp-detail-subtitle" class="text-[11px] font-medium text-slate-500 truncate">Laporan semua transaksi</p>
            </div>
          </div>
          <div class="relative">
            <button type="button" onclick="toggleKpOverflowMenu(event)" class="w-9 h-9 rounded-full hover:bg-slate-100 flex items-center justify-center text-[#00796B] text-lg font-bold cursor-pointer">⋮</button>
            <div id="kp-overflow-menu" class="hidden absolute right-0 mt-1 w-52 bg-white rounded-xl shadow-2xl border border-slate-200 py-1.5 z-30 text-xs">
              <button type="button" onclick="setKpPeriod('DAILY')" class="w-full px-4 py-2 text-left hover:bg-teal-50 font-semibold text-slate-800 flex items-center justify-between cursor-pointer"><span>Laporan per Hari</span><span id="kp-check-DAILY" class="text-[#00796B] font-bold">✓</span></button>
              <button type="button" onclick="setKpPeriod('MONTHLY')" class="w-full px-4 py-2 text-left hover:bg-teal-50 font-semibold text-slate-800 flex items-center justify-between cursor-pointer"><span>Laporan per Bulan</span><span id="kp-check-MONTHLY" class="text-[#00796B] font-bold hidden">✓</span></button>
              <button type="button" onclick="setKpPeriod('YEARLY')" class="w-full px-4 py-2 text-left hover:bg-teal-50 font-semibold text-slate-800 flex items-center justify-between cursor-pointer"><span>Laporan per Tahun</span><span id="kp-check-YEARLY" class="text-[#00796B] font-bold hidden">✓</span></button>
              <div class="my-1 border-t border-slate-100"></div>
              <button type="button" onclick="backToKpMenu()" class="w-full px-4 py-2 text-left hover:bg-slate-50 font-semibold text-[#00796B] cursor-pointer">Menu Pilihan Laporan</button>
            </div>
          </div>
        </div>
        <div id="kp-detail-date-bar" class="px-4 py-2.5 bg-[#F0FDFA] border-b border-teal-100 flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-1.5 flex-wrap">
            <button type="button" onclick="stepKpTargetDate(-1)" title="Tanggal Sebelumnya" class="px-2.5 py-1.5 rounded-lg bg-white border border-teal-200 text-[#00796B] hover:bg-teal-50 text-xs font-bold cursor-pointer">◀ Seblm</button>
            <input type="date" id="kp-date-picker" onchange="setKpTargetDate(this.value)" class="px-2.5 py-1 rounded-lg bg-white border border-teal-200 text-slate-800 text-xs font-semibold focus:outline-none focus:border-[#00796B]" />
            <button type="button" id="kp-btn-next-date" onclick="stepKpTargetDate(1)" title="Tanggal Berikutnya" class="px-2.5 py-1.5 rounded-lg bg-white border border-teal-200 text-[#00796B] hover:bg-teal-50 text-xs font-bold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed">Brkt ▶</button>
            <button type="button" id="kp-btn-today-date" onclick="resetKpTargetDate()" class="hidden px-2.5 py-1.5 rounded-lg bg-[#00796B] text-white text-xs font-bold hover:bg-[#00695C] cursor-pointer">Hari Ini</button>
          </div>
          <span id="kp-detail-date-badge" class="px-2.5 py-1 rounded-full bg-teal-100 text-[#00796B] text-[11px] font-bold">Hari Ini</span>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-12 divide-y lg:divide-y-0 lg:divide-x divide-slate-200 bg-white">
          <div class="lg:col-span-6 p-4 sm:p-5 flex flex-col justify-between bg-white">
            <div>
              <div class="grid grid-cols-4 rounded-lg overflow-hidden mb-5">
                <button type="button" id="kp-metric-COUNT" onclick="setKpMetric('COUNT')" class="py-2.5 px-1.5 text-[11px] sm:text-xs font-bold bg-[#00796B] text-white cursor-pointer text-center">Jml Transaksi</button>
                <button type="button" id="kp-metric-REVENUE" onclick="setKpMetric('REVENUE')" class="py-2.5 px-1.5 text-[11px] sm:text-xs font-semibold bg-[#CFD4DC] text-white cursor-pointer text-center border-l border-white/40">Pendapatan</button>
                <button type="button" id="kp-metric-PROFIT" onclick="setKpMetric('PROFIT')" class="py-2.5 px-1.5 text-[11px] sm:text-xs font-semibold bg-[#CFD4DC] text-white cursor-pointer text-center border-l border-white/40">Keuntungan</button>
                <button type="button" id="kp-metric-EXPENSE" onclick="setKpMetric('EXPENSE')" class="py-2.5 px-1.5 text-[11px] sm:text-xs font-semibold bg-[#CFD4DC] text-white cursor-pointer text-center border-l border-white/40">Pengeluaran</button>
              </div>
              <div id="kp-chart-container" class="w-full min-h-[220px] flex items-center justify-center"></div>
            </div>
            <div class="flex items-center justify-between pt-4 mt-4 border-t border-slate-100">
              <div class="flex items-center gap-2">
                <div class="w-6 h-6 rounded-md bg-[#00796B] text-white flex items-center justify-center text-xs font-bold">🧾</div>
                <span class="text-sm font-extrabold text-[#00796B] italic">Kasir Pintar</span>
              </div>
              <button type="button" onclick="shareKpReport()" class="flex items-center gap-2 px-3 py-1.5 rounded-full hover:bg-teal-50 cursor-pointer">
                <span class="w-7 h-7 rounded-full bg-[#00796B] text-white flex items-center justify-center text-xs font-bold">🔗</span>
                <span class="text-sm font-bold text-[#00796B]">Bagikan</span>
              </button>
            </div>
          </div>
          <div class="lg:col-span-6 bg-white flex flex-col">
            <div id="kp-today-nota-toolbar" class="hidden px-4 sm:px-5 py-3 bg-[#F0FDFA] border-b border-teal-100 space-y-2">
              <div class="flex items-center justify-between gap-2 flex-wrap">
                <div>
                  <div id="kp-today-nota-heading" class="text-xs font-extrabold text-[#00796B]">Catatan Transaksi &amp; Pesanan per Nota Hari Ini</div>
                  <div id="kp-today-nota-desc" class="text-[11px] text-slate-600">Catatan pernota pesanan lengkap menu, pelanggan, pendapatan &amp; keuntungan</div>
                </div>
                <div class="flex items-center gap-1.5">
                  <span id="kp-today-nota-count" class="px-2 py-0.5 rounded-full bg-teal-100 text-[#00796B] text-[11px] font-bold font-mono">0 Nota</span>
                  <span id="kp-today-items-count" class="px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 text-[11px] font-bold font-mono">0 Item</span>
                </div>
              </div>
              <input type="text" id="kp-today-nota-search" oninput="setKpTodayNotaSearch(this.value)" placeholder="Cari no. nota, pesanan menu, pelanggan, kasir..." class="w-full bg-white border border-teal-200 rounded-xl px-3 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-[#00796B]" />
            </div>
            <div id="kp-per-nota-finance-strip" class="hidden px-4 sm:px-5 py-2.5 bg-white border-b border-slate-200 grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
              <div class="rounded-lg bg-slate-50 px-2.5 py-1.5 border border-slate-200/80"><div class="text-[10px] text-slate-500">Pendapatan</div><div id="kp-pn-rev" class="font-extrabold font-mono text-slate-900">Rp 0</div></div>
              <div class="rounded-lg bg-teal-50/70 px-2.5 py-1.5 border border-teal-200/70"><div class="text-[10px] text-teal-700">Laba Kotor</div><div id="kp-pn-profit" class="font-extrabold font-mono text-[#00796B]">Rp 0</div></div>
              <div class="rounded-lg bg-rose-50 px-2.5 py-1.5 border border-rose-200/80"><div class="text-[10px] text-rose-700">Pengeluaran</div><div id="kp-pn-exp" class="font-extrabold font-mono text-rose-600">Rp 0</div></div>
              <div class="rounded-lg bg-emerald-50 px-2.5 py-1.5 border border-emerald-200/80"><div class="text-[10px] text-emerald-700">Laba Bersih</div><div id="kp-pn-net" class="font-extrabold font-mono text-emerald-700">Rp 0</div></div>
            </div>
            <div id="kp-records-list" class="bg-white divide-y divide-slate-200 max-h-[460px] overflow-y-auto flex-1"></div>
            <div id="kp-per-nota-expenses-box" class="hidden border-t border-rose-200 bg-rose-50/40"></div>
          </div>
        </div>
      </div>

      <!-- Screen 3: Laporan Pengeluaran -->
      <div id="kp-expense-view" class="hidden">
        <div class="flex items-center justify-between px-4 py-3 border-b border-slate-200 bg-white">
          <div class="flex items-center gap-2 min-w-0">
            <button type="button" onclick="backToKpMenu()" class="w-9 h-9 rounded-full hover:bg-slate-100 flex items-center justify-center text-[#00796B] text-xl font-bold cursor-pointer">&lt;</button>
            <div class="min-w-0">
              <h2 class="text-base sm:text-lg font-bold text-rose-700 truncate">Laporan Pengeluaran</h2>
              <p id="kp-exp-subtitle" class="text-[11px] font-medium text-slate-500 truncate">Catatan beban operasional &amp; pengurangan laba</p>
            </div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <span id="kp-exp-autosync-badge" class="hidden sm:inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-[#00796B] text-[10.5px] font-bold">● Auto-Sync Live</span>
            <button type="button" onclick="openModalCreateExpense()" class="flex items-center gap-1 px-2.5 py-1.5 rounded-full bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold cursor-pointer">
              <span>+ Catat</span>
            </button>
            <button type="button" onclick="shareKpExpenseReport()" class="flex items-center gap-1.5 px-3 py-1.5 rounded-full hover:bg-rose-50 text-rose-700 text-xs font-bold cursor-pointer">
              <span>🔗</span><span>Bagikan</span>
            </button>
          </div>
        </div>
        <div class="px-4 py-2.5 bg-rose-50/70 border-b border-rose-100 flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-1.5 flex-wrap">
            <button type="button" onclick="stepKpTargetDate(-1)" title="Tanggal Sebelumnya" class="px-2.5 py-1.5 rounded-lg bg-white border border-rose-200 text-rose-700 hover:bg-rose-50 text-xs font-bold cursor-pointer">◀ Seblm</button>
            <input type="date" id="kp-exp-date-picker" onchange="setKpTargetDate(this.value)" class="px-2.5 py-1 rounded-lg bg-white border border-rose-200 text-slate-800 text-xs font-semibold focus:outline-none focus:border-rose-600" />
            <button type="button" id="kp-exp-btn-next-date" onclick="stepKpTargetDate(1)" title="Tanggal Berikutnya" class="px-2.5 py-1.5 rounded-lg bg-white border border-rose-200 text-rose-700 hover:bg-rose-50 text-xs font-bold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed">Brkt ▶</button>
            <button type="button" id="kp-exp-btn-today-date" onclick="resetKpTargetDate()" class="hidden px-2.5 py-1.5 rounded-lg bg-rose-600 text-white text-xs font-bold hover:bg-rose-700 cursor-pointer">Hari Ini</button>
          </div>
          <span id="kp-exp-date-badge" class="px-2.5 py-1 rounded-full bg-rose-100 text-rose-700 text-[11px] font-bold">Hari Ini</span>
        </div>
        <div class="px-4 py-2.5 bg-white border-b border-slate-200 flex items-center gap-1.5 flex-wrap">
          <button type="button" id="kp-exp-scope-TODAY" onclick="setKpExpenseScope('TODAY')" class="px-3 py-1.5 rounded-lg text-xs font-bold bg-rose-600 text-white cursor-pointer">Per Tgl</button>
          <button type="button" id="kp-exp-scope-THIS_MONTH" onclick="setKpExpenseScope('THIS_MONTH')" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-700 cursor-pointer">Bulan</button>
          <button type="button" id="kp-exp-scope-THIS_YEAR" onclick="setKpExpenseScope('THIS_YEAR')" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-700 cursor-pointer">Tahun</button>
          <button type="button" id="kp-exp-scope-ALL" onclick="setKpExpenseScope('ALL')" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-700 cursor-pointer">Semua</button>
        </div>
        <div class="p-4 bg-slate-50 border-b border-slate-200 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
          <div class="rounded-xl bg-white border border-rose-200 p-3">
            <div class="text-[11px] text-slate-500">Total Pengeluaran</div>
            <div id="kp-exp-total-val" class="text-base font-extrabold font-mono text-rose-600 mt-0.5">Rp 0</div>
          </div>
          <div class="rounded-xl bg-white border border-slate-200 p-3">
            <div class="text-[11px] text-slate-500">Pendapatan</div>
            <div id="kp-exp-rev-val" class="text-base font-extrabold font-mono text-slate-900 mt-0.5">Rp 0</div>
          </div>
          <div class="rounded-xl bg-white border border-teal-200 p-3">
            <div class="text-[11px] text-teal-700">Keuntungan Kotor</div>
            <div id="kp-exp-profit-val" class="text-base font-extrabold font-mono text-[#00796B] mt-0.5">Rp 0</div>
          </div>
          <div class="rounded-xl bg-white border border-emerald-200 p-3">
            <div class="text-[11px] text-emerald-700">Laba Bersih</div>
            <div id="kp-exp-net-val" class="text-base font-extrabold font-mono text-emerald-700 mt-0.5">Rp 0</div>
          </div>
        </div>
        <div class="p-3 bg-white border-b border-slate-200">
          <input type="text" id="kp-expense-search" oninput="setKpExpenseSearch(this.value)" placeholder="Cari judul, kategori, catatan, atau kasir pengeluaran..." class="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-rose-600" />
        </div>
        <div id="kp-expense-list" class="divide-y divide-slate-200 max-h-[460px] overflow-y-auto bg-white"></div>
      </div>
    </div>

    <!-- Tabel Pengeluaran & Catatan Pengeluaran -->
    <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
        <div>
          <div class="flex items-center gap-2 flex-wrap">
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span>💸</span>
              <span>Laporan Pengeluaran & Catatan Pengeluaran</span>
            </h2>
            <span class="inline-flex items-center gap-1.5 bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 text-[10px] font-bold px-2.5 py-0.5 rounded-full">
              Auto-Sync Live RTDB
            </span>
          </div>
          <p class="text-xs text-slate-400">Rincian beban operasional, belanja bahan baku & catatan pengeluaran kasir</p>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <input type="text" id="search-expenses" oninput="filterExpensesTable()" placeholder="Cari pengeluaran / catatan..." class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-rose-500 w-44" />
          <button onclick="openModalCreateExpense()" class="px-2.5 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-lg text-xs font-bold transition cursor-pointer">➕ Catat</button>
          <button onclick="loadData()" class="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-bold transition cursor-pointer">🔄 Sinkron</button>
          <span id="expenses-count-badge" class="px-2.5 py-1 bg-rose-950 text-rose-300 border border-rose-800 rounded-lg text-xs font-mono font-bold">0 Catatan</span>
        </div>
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

    <!-- Tabel Rincian Transaksi Penjualan + Laporan Pengeluaran (Realtime Database) -->
    <div id="trx-report-card" class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-lg">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div>
          <h2 class="text-sm font-bold text-white flex items-center gap-2">
            <span>🧾</span>
            <span>Rincian Transaksi Penjualan</span>
          </h2>
          <p class="text-xs text-slate-400">Daftar nota transaksi penjualan &amp; laporan pengeluaran (Realtime Database) sesuai tanggal</p>
        </div>
        <span id="trx-count-badge" class="px-2.5 py-1 bg-sky-950 text-sky-400 border border-sky-800 rounded-lg text-xs font-mono font-bold">0 Nota</span>
      </div>
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
        <div class="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2">
          <div class="text-[10px] text-slate-400">Pendapatan Transaksi</div>
          <div id="trx-kpi-rev" class="font-extrabold font-mono text-emerald-400 mt-0.5">Rp 0</div>
        </div>
        <div class="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2">
          <div class="text-[10px] text-slate-400">Laba Kotor</div>
          <div id="trx-kpi-profit" class="font-extrabold font-mono text-teal-300 mt-0.5">Rp 0</div>
        </div>
        <div class="bg-rose-950/30 border border-rose-500/30 rounded-xl px-3 py-2">
          <div class="text-[10px] text-rose-300">Pengeluaran (RTDB)</div>
          <div id="trx-kpi-exp" class="font-extrabold font-mono text-rose-400 mt-0.5">Rp 0</div>
        </div>
        <div class="bg-emerald-950/30 border border-emerald-500/30 rounded-xl px-3 py-2">
          <div class="text-[10px] text-emerald-300">Laba Bersih</div>
          <div id="trx-kpi-net" class="font-extrabold font-mono text-emerald-300 mt-0.5">Rp 0</div>
        </div>
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
              <th class="py-2.5 px-3 text-right">Pendapatan</th>
              <th class="py-2.5 px-3 text-right">Keuntungan</th>
              <th class="py-2.5 px-3 text-center">Aksi</th>
            </tr>
          </thead>
          <tbody id="tbody-transactions" class="divide-y divide-slate-800/80">
            <tr><td colspan="9" class="py-6 text-center text-slate-500 font-sans">Memuat rincian transaksi...</td></tr>
          </tbody>
        </table>
      </div>
      <div id="trx-expenses-box" class="bg-rose-950/20 border border-rose-500/30 rounded-xl overflow-hidden"></div>
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

  <!-- MODAL: CATAT TRANSAKSI PER NOTA PESANAN BARU -->
  <div id="modal-create-trx" class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm hidden items-center justify-center p-4">
    <div class="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
      <div class="px-5 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/70">
        <h3 class="text-sm font-bold text-white flex items-center gap-2">
          <span>🧾</span>
          <span>Catat Transaksi &amp; Pesanan Per Nota</span>
        </h3>
        <button onclick="closeModalCreateTrx()" class="text-slate-400 hover:text-white text-lg cursor-pointer">&times;</button>
      </div>
      <div class="p-5 overflow-y-auto space-y-3 text-xs">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">No. Nota / Invoice:</label>
            <input type="text" id="m-trx-inv" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-orange-400 font-mono font-bold" />
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Metode Bayar:</label>
            <select id="m-trx-pm" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white">
              <option value="CASH">Tunai (CASH)</option>
              <option value="QRIS">QRIS</option>
              <option value="TRANSFER">Transfer Bank</option>
              <option value="EDC">Kartu / EDC</option>
            </select>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">Pelanggan:</label>
            <input type="text" id="m-trx-cust" value="Umum" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Kasir:</label>
            <input type="text" id="m-trx-cashier" value="Kasir Utama" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">Tipe Pesanan:</label>
            <select id="m-trx-ordertype" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white">
              <option value="DINE_IN">Makan di Tempat</option>
              <option value="TAKE_AWAY">Di Bungkus</option>
            </select>
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Meja (Opsional):</label>
            <input type="text" id="m-trx-table" placeholder="Contoh: A1" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
          </div>
        </div>
        <div>
          <label class="font-bold text-slate-300 block mb-1">Rincian Pesanan Menu (misal: Nasi Goreng x2, Es Teh x1):</label>
          <input type="text" id="m-trx-items" placeholder="Nasi Goreng x2, Es Teh x1" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">Total Pendapatan (Rp):</label>
            <input type="number" id="m-trx-total" value="0" min="0" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-emerald-400 font-mono font-bold" />
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Keuntungan (Rp):</label>
            <input type="number" id="m-trx-profit" value="0" min="0" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-teal-300 font-mono font-bold" />
          </div>
        </div>
      </div>
      <div class="p-4 border-t border-slate-800 bg-slate-950/70 flex justify-end gap-2.5">
        <button onclick="closeModalCreateTrx()" class="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold cursor-pointer">Batal</button>
        <button onclick="saveNewTrx()" class="px-5 py-2 bg-orange-600 hover:bg-orange-500 text-white rounded-xl text-xs font-bold shadow-md shadow-orange-600/30 cursor-pointer">💾 Simpan Nota Pesanan</button>
      </div>
    </div>
  </div>

  <!-- MODAL: CATAT PENGELUARAN & CATATAN PENGELUARAN -->
  <div id="modal-create-expense" class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm hidden items-center justify-center p-4">
    <div class="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
      <div class="px-5 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/70">
        <h3 class="text-sm font-bold text-white flex items-center gap-2">
          <span>💸</span>
          <span>Catat Pengeluaran &amp; Catatan Pengeluaran</span>
        </h3>
        <button onclick="closeModalCreateExpense()" class="text-slate-400 hover:text-white text-lg cursor-pointer">&times;</button>
      </div>
      <div class="p-5 overflow-y-auto space-y-3 text-xs">
        <div>
          <label class="font-bold text-slate-300 block mb-1">Judul / Keperluan Pengeluaran:</label>
          <input type="text" id="m-exp-title" placeholder="Contoh: Beli Gas LPG / Bahan Baku" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">Nominal (Rp):</label>
            <input type="number" id="m-exp-amount" value="0" min="0" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-rose-400 font-mono font-bold" />
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Kategori:</label>
            <select id="m-exp-category" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white">
              <option value="Bahan Baku">Bahan Baku</option>
              <option value="Operasional">Operasional</option>
              <option value="Gaji/Bonus">Gaji/Bonus</option>
              <option value="Listrik/Air">Listrik/Air</option>
              <option value="Lainnya">Lainnya</option>
            </select>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="font-bold text-slate-300 block mb-1">Tanggal:</label>
            <input type="date" id="m-exp-date" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono" />
          </div>
          <div>
            <label class="font-bold text-slate-300 block mb-1">Petugas / Kasir:</label>
            <input type="text" id="m-exp-cashier" value="Admin" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white" />
          </div>
        </div>
        <div>
          <label class="font-bold text-slate-300 block mb-1">Catatan Pengeluaran (Detail):</label>
          <textarea id="m-exp-notes" rows="2" placeholder="Catatan rincian pembelian atau keterangan..." class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white"></textarea>
        </div>
      </div>
      <div class="p-4 border-t border-slate-800 bg-slate-950/70 flex justify-end gap-2.5">
        <button onclick="closeModalCreateExpense()" class="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold cursor-pointer">Batal</button>
        <button onclick="saveNewExpense()" class="px-5 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold shadow-md shadow-rose-600/30 cursor-pointer">💾 Simpan Pengeluaran</button>
      </div>
    </div>
  </div>

  <!-- SCRIPT REALTIME DATABASE REST API -->
  <script>
    const DEFAULT_DB_URL = "https://foodrock-report-default-rtdb.asia-southeast1.firebasedatabase.app";
    function isPlaceholderDbToken(v) {
      if (!v) return true;
      return /^(\(default\)|laporan-service|ai-studio|gen-lang-client)/i.test(String(v).trim());
    }
    function buildRtdbBaseCandidates() {
      const candidates = [];
      try {
        const p = new URLSearchParams(window.location.search);
        const rawList = [p.get('db'), p.get('databaseId'), p.get('projectId')];
        rawList.forEach(raw => {
          const cand = (raw || '').trim().replace(/\/+$/, '');
          if (!cand || isPlaceholderDbToken(cand)) return;
          if (/^https?:\/\//i.test(cand)) {
            candidates.push(cand.split('?')[0].split('#')[0].replace(/\/+$/, ''));
          } else if (/\.(firebasedatabase\.app|firebaseio\.com)$/i.test(cand)) {
            candidates.push("https://" + cand);
          } else {
            candidates.push("https://" + cand + ".asia-southeast1.firebasedatabase.app");
            candidates.push("https://" + cand + ".firebaseio.com");
            if (!/-default-rtdb$/i.test(cand)) {
              candidates.push("https://" + cand + "-default-rtdb.asia-southeast1.firebasedatabase.app");
              candidates.push("https://" + cand + "-default-rtdb.firebaseio.com");
            }
          }
        });
      } catch (_) {}
      candidates.push(DEFAULT_DB_URL, "https://foodrock-report-default-rtdb.firebaseio.com");
      return Array.from(new Set(candidates.filter(Boolean)));
    }
    const RTDB_CANDIDATES = buildRtdbBaseCandidates();
    let activeRtdbBase = RTDB_CANDIDATES[0] || DEFAULT_DB_URL;
    const DB_URL = activeRtdbBase;
    (function updateHeaderDbLbl() {
      const lbl = document.getElementById('hdr-db-label');
      if (lbl && activeRtdbBase) {
        const hostName = activeRtdbBase.replace(/^https?:\/\//i, '').split('.')[0];
        if (hostName) lbl.textContent = hostName;
      }
    })();

    let cachedArchives = [];
    let cachedShifts = [];
    let cachedTransactions = [];
    let cachedDaily = [];
    let cachedExpenses = [];
    let rtdbPermissionDeniedPaths = [];

    function getLocalDateStr(d = new Date()) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return y + '-' + m + '-' + day;
    }

    function parseMillisSafe(rawVal) {
      if (rawVal == null || rawVal === '') return 0;
      const n = Number(rawVal);
      if (!isNaN(n) && isFinite(n) && n > 0) {
        return n < 100000000000 ? n * 1000 : n;
      }
      return 0;
    }

    function extractDateStr(item) {
      if (!item) return '';
      if (typeof item === 'number') {
        const ms = parseMillisSafe(item);
        if (ms > 0) return getLocalDateStr(new Date(ms));
        return '';
      }
      if (typeof item === 'string') {
        const s = item.trim();
        if (!s) return '';
        if (/^\d{10,13}$/.test(s)) {
          const ms = parseMillisSafe(s);
          if (ms > 0) return getLocalDateStr(new Date(ms));
        }
        if (/^\d{4}[-\/]\d{2}[-\/]\d{2}/.test(s)) {
          const norm = s.slice(0, 10).replace(/\//g, '-');
          if (s.length > 10 && /([Zz]|[+-]\d{2}:?\d{2})$/.test(s)) {
            const p = new Date(s);
            if (!isNaN(p.getTime())) return getLocalDateStr(p);
          }
          return norm;
        }
        const dmy = s.match(/^(\d{2})[-\/](\d{2})[-\/](\d{4})/);
        if (dmy) return dmy[3] + '-' + dmy[2] + '-' + dmy[1];
        const p = new Date(s);
        return !isNaN(p.getTime()) ? getLocalDateStr(p) : '';
      }
      if (item.date) {
        const fromDate = extractDateStr(String(item.date));
        if (fromDate) return fromDate;
      }
      if (item.timestampMillis) {
        const ms = parseMillisSafe(item.timestampMillis);
        if (ms > 0) {
          const d = new Date(ms);
          if (!isNaN(d.getTime())) return getLocalDateStr(d);
        }
      }
      return extractDateStr(item.timestamp || item.syncedAt || item.createdAt || item.updatedAt || '');
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

    let dailyNotaPayFilter = 'ALL';
    let dailyNotaSearchKeyword = '';

    function escHtml(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
    }

    function cleanExpNote(raw) {
      if (raw == null) return '';
      const s = String(raw).trim();
      return (!s || s === '-') ? '' : s;
    }

    function normalizePm(pmRaw) {
      const pm = String(pmRaw || 'CASH').toUpperCase();
      if (pm === 'TUNAI') return 'CASH';
      if (pm === 'DEBIT' || pm === 'CARD') return 'EDC';
      return pm;
    }

    function getTrxSortMillis(t) {
      if (t && t.timestampMillis && Number(t.timestampMillis) > 0) return Number(t.timestampMillis);
      const parsed = new Date((t && (t.timestamp || t.createdAt || t.date)) || 0).getTime();
      return isNaN(parsed) ? 0 : parsed;
    }

    function normalizeKpItems(rawItems) {
      if (Array.isArray(rawItems)) return rawItems.filter(it => it && typeof it === 'object');
      if (rawItems && typeof rawItems === 'object') return Object.values(rawItems).filter(it => it && typeof it === 'object');
      return [];
    }

    function openModalCreateTrx() {
      const now = new Date();
      const pad = n => String(n).padStart(2, '0');
      const code = 'INV-' + now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) + '-' + pad(now.getHours()) + pad(now.getMinutes()) + pad(now.getSeconds());
      const invEl = document.getElementById('m-trx-inv');
      if (invEl) invEl.value = code;
      const mEl = document.getElementById('modal-create-trx');
      if (mEl) { mEl.classList.remove('hidden'); mEl.classList.add('flex'); }
    }

    function closeModalCreateTrx() {
      const mEl = document.getElementById('modal-create-trx');
      if (mEl) { mEl.classList.add('hidden'); mEl.classList.remove('flex'); }
    }

    function openModalCreateExpense() {
      const tEl = document.getElementById('m-exp-title');
      const aEl = document.getElementById('m-exp-amount');
      const nEl = document.getElementById('m-exp-notes');
      const dEl = document.getElementById('m-exp-date');
      if (tEl) tEl.value = '';
      if (aEl) aEl.value = '';
      if (nEl) nEl.value = '';
      if (dEl) dEl.value = kpTargetDate || activeDateFilter || getLocalDateStr(new Date());
      const mEl = document.getElementById('modal-create-expense');
      if (mEl) { mEl.classList.remove('hidden'); mEl.classList.add('flex'); }
    }

    function closeModalCreateExpense() {
      const mEl = document.getElementById('modal-create-expense');
      if (mEl) { mEl.classList.add('hidden'); mEl.classList.remove('flex'); }
    }

    async function writePathRtdbAndRelay(path, payloadObj, method) {
      const bodyStr = JSON.stringify(payloadObj);
      const m = method || 'PUT';
      const tasks = [
        fetch((activeRtdbBase || DB_URL) + '/' + path + '.json', {
          method: m,
          headers: { 'Content-Type': 'application/json' },
          body: bodyStr
        }).catch(() => null)
      ];
      getRelayBaseCandidates().slice(0, 2).forEach(base => {
        tasks.push(
          fetch(base + '/api/sync/' + path, {
            method: m,
            headers: { 'Content-Type': 'application/json' },
            body: bodyStr
          }).catch(() => null)
        );
      });
      return Promise.all(tasks);
    }

    async function saveNewExpense() {
      const title = (document.getElementById('m-exp-title').value || '').trim();
      const amount = Number(document.getElementById('m-exp-amount').value) || 0;
      const category = document.getElementById('m-exp-category').value || 'Operasional';
      const dateStr = (document.getElementById('m-exp-date').value || kpTargetDate || getLocalDateStr(new Date())).trim();
      const cashier = (document.getElementById('m-exp-cashier').value || 'Admin').trim();
      const notes = (document.getElementById('m-exp-notes').value || '').trim();
      if (!title) { alert('Judul / keperluan pengeluaran harus diisi.'); return; }
      if (amount <= 0) { alert('Nominal pengeluaran harus lebih dari 0.'); return; }

      const now = new Date();
      const tsMillis = (dateStr === getLocalDateStr(now))
        ? now.getTime()
        : new Date(dateStr + 'T12:00:00').getTime();
      const isoStr = new Date(tsMillis).toISOString();
      const expKey = 'EXP-' + tsMillis;
      const cfKey = 'CF-' + expKey;

      const payload = {
        id: expKey,
        localId: tsMillis,
        title: title,
        amount: amount,
        category: category,
        notes: cleanExpNote(notes),
        createdBy: cashier,
        cashierName: cashier,
        date: dateStr,
        timestamp: isoStr,
        timestampMillis: tsMillis,
        syncedAt: isoStr,
        storeId: 'store_main'
      };
      const cfPayload = {
        id: cfKey,
        type: 'OUT',
        category: category,
        description: title,
        notes: cleanExpNote(notes),
        amount: amount,
        cashierName: cashier,
        date: dateStr,
        timestamp: isoStr,
        timestampMillis: tsMillis,
        storeId: 'store_main'
      };
      const statsLast = {
        lastExpenseId: expKey,
        lastExpenseTitle: title,
        lastExpenseAmount: amount,
        lastExpenseCategory: category,
        lastExpenseNotes: notes,
        lastExpenseBy: cashier,
        date: dateStr,
        timestampMillis: tsMillis,
        lastUpdated: isoStr
      };
      try {
        await Promise.all([
          writePathRtdbAndRelay('expenses/' + expKey, payload, 'PUT'),
          writePathRtdbAndRelay('reports/daily_expenses/' + dateStr + '/' + expKey, payload, 'PUT'),
          writePathRtdbAndRelay('realtime_stats/expenses/' + expKey, payload, 'PUT'),
          writePathRtdbAndRelay('realtime_stats/expenses_by_date/' + dateStr + '/' + expKey, payload, 'PUT'),
          writePathRtdbAndRelay('cash_flows/' + cfKey, cfPayload, 'PUT'),
          writePathRtdbAndRelay('realtime_stats/last_expense', statsLast, 'PUT')
        ]);
        closeModalCreateExpense();
        await loadData();
      } catch (e) {
        alert('Gagal menyimpan pengeluaran: ' + e.message);
      }
    }

    async function saveNewTrx() {
      const inv = (document.getElementById('m-trx-inv').value || ('INV-' + Date.now())).trim();
      const pm = document.getElementById('m-trx-pm').value || 'CASH';
      const cust = (document.getElementById('m-trx-cust').value || 'Umum').trim();
      const cashier = (document.getElementById('m-trx-cashier').value || 'Kasir Utama').trim();
      const orderType = document.getElementById('m-trx-ordertype').value || 'DINE_IN';
      const tableNum = (document.getElementById('m-trx-table').value || '').trim();
      const itemsText = (document.getElementById('m-trx-items').value || 'Pesanan').trim();
      const total = Number(document.getElementById('m-trx-total').value) || 0;
      const profit = Number(document.getElementById('m-trx-profit').value) || 0;
      if (total <= 0) { alert('Total pendapatan harus lebih dari 0.'); return; }

      const now = new Date();
      const dateStr = getLocalDateStr(now);
      const parsedItems = itemsText.split(',').map(s => s.trim()).filter(Boolean).map(chunk => {
        const m = chunk.match(/^(.*?)\s*[xX](\d+)$/);
        return {
          productName: m ? m[1].trim() : chunk,
          quantity: m ? (Number(m[2]) || 1) : 1,
          subtotal: 0
        };
      });
      if (parsedItems.length === 1) parsedItems[0].subtotal = total;
      const totalCount = parsedItems.reduce((s, it) => s + (Number(it.quantity) || 1), 0) || 1;

      const payload = {
        id: inv,
        invoiceNumber: inv,
        orderNumber: inv,
        customerName: cust,
        cashierName: cashier,
        orderType: orderType,
        tableNumber: tableNum,
        paymentMethod: pm,
        itemsSummary: itemsText,
        items: parsedItems,
        totalItemsCount: totalCount,
        subtotalAmount: total,
        totalAmount: total,
        grandTotal: total,
        profitAmount: profit,
        date: dateStr,
        timestampMillis: now.getTime(),
        timestamp: now.toISOString()
      };
      try {
        const docKey = encodeURIComponent(inv);
        await Promise.all([
          writePathRtdbAndRelay('transactions/' + docKey, payload, 'PUT'),
          writePathRtdbAndRelay('reports/daily_transactions/' + dateStr + '/' + docKey, payload, 'PUT')
        ]);
        closeModalCreateTrx();
        loadData();
      } catch (e) {
        alert('Gagal menyimpan nota: ' + e.message);
      }
    }

    function renderDailyPerNotaSection(filteredTrxs, filteredExps) {
      const container = document.getElementById('daily-per-nota-list');
      if (!container) return;

      const todayStr = getLocalDateStr(new Date());
      const titleEl = document.getElementById('daily-per-nota-title');
      const dateBadge = document.getElementById('daily-per-nota-date-badge');
      const countPill = document.getElementById('daily-per-nota-count-pill');
      const revPill = document.getElementById('daily-per-nota-rev-pill');
      const profitPill = document.getElementById('daily-per-nota-profit-pill');
      const expensePill = document.getElementById('daily-per-nota-expense-pill');
      const netPill = document.getElementById('daily-per-nota-net-pill');

      const periodLabel = !activeDateFilter
        ? 'Semua Tanggal'
        : (activeDateFilter === todayStr ? 'Hari Ini' : activeDateFilter);
      if (titleEl) {
        titleEl.innerHTML = '<span>🧾</span><span>Catatan Transaksi per Nota (' + escHtml(periodLabel) + ')</span>';
      }
      if (dateBadge) {
        dateBadge.textContent = activeDateFilter ? formatKpDateShort(activeDateFilter) : 'Semua Tanggal';
      }

      const expList = Array.isArray(filteredExps)
        ? filteredExps
        : (activeDateFilter ? cachedExpenses.filter(e => extractDateStr(e) === activeDateFilter) : cachedExpenses);
      const sortedExps = expList.slice().sort((a, b) => getTrxSortMillis(b) - getTrxSortMillis(a));
      const matchedExps = sortedExps.filter(e => {
        if (!dailyNotaSearchKeyword) return true;
        return String(e.title || '').toLowerCase().includes(dailyNotaSearchKeyword) ||
          String(e.category || '').toLowerCase().includes(dailyNotaSearchKeyword) ||
          String(e.notes || '').toLowerCase().includes(dailyNotaSearchKeyword) ||
          String(e.cashierName || '').toLowerCase().includes(dailyNotaSearchKeyword);
      });

      const baseList = (filteredTrxs || []).slice().sort((a, b) => getTrxSortMillis(b) - getTrxSortMillis(a));
      const matched = baseList.filter(t => {
        if (dailyNotaPayFilter !== 'ALL' && normalizePm(t.paymentMethod) !== dailyNotaPayFilter) return false;
        if (dailyNotaSearchKeyword) {
          const inv = String(t.invoiceNumber || t.id || '').toLowerCase();
          const ordLbl = formatOrderLabel(t.invoiceNumber || t.id || '').toLowerCase();
          const cust = String(t.customerName || '').toLowerCase();
          const cash = String(t.cashierName || '').toLowerCase();
          const sum = String(t.itemsSummary || '').toLowerCase();
          const nested = normalizeKpItems(t.items)
            .map(it => (it.productName || it.name || '') + ' ' + (it.notes || '')).join(' ').toLowerCase();
          if (!inv.includes(dailyNotaSearchKeyword) && !ordLbl.includes(dailyNotaSearchKeyword) &&
              !cust.includes(dailyNotaSearchKeyword) && !cash.includes(dailyNotaSearchKeyword) &&
              !sum.includes(dailyNotaSearchKeyword) && !nested.includes(dailyNotaSearchKeyword)) {
            return false;
          }
        }
        return true;
      });

      const totalRev = baseList.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
      const totalProf = baseList.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
      const totalExp = sortedExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
      const netProf = totalProf - totalExp;
      if (countPill) countPill.textContent = matched.length + ' Nota';
      if (revPill) revPill.textContent = 'Pendapatan: ' + formatRupiah(totalRev);
      if (profitPill) profitPill.textContent = 'Keuntungan: ' + formatRupiah(totalProf);
      if (expensePill) expensePill.textContent = 'Pengeluaran (' + sortedExps.length + '): ' + formatRupiah(totalExp);
      if (netPill) {
        netPill.textContent = 'Laba Bersih: ' + formatRupiah(netProf);
        netPill.className = netProf >= 0
          ? 'px-2.5 py-1 rounded-xl bg-slate-950 border border-emerald-400/40 text-emerald-300 text-xs font-mono font-bold'
          : 'px-2.5 py-1 rounded-xl bg-slate-950 border border-rose-500/40 text-rose-400 text-xs font-mono font-bold';
      }

      const expBox = document.getElementById('daily-per-nota-expenses-box');
      if (expBox) {
        if (matchedExps.length === 0) {
          expBox.innerHTML = "<div class='px-4 py-3 flex items-center justify-between text-xs text-slate-500'>" +
            "<span>💸 Belum ada catatan pengeluaran pada periode " + escHtml(periodLabel) + ".</span>" +
            "<span class='font-mono font-bold text-slate-400'>Rp 0</span>" +
          "</div>";
        } else {
          const matchedExpTotal = matchedExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
          expBox.innerHTML = "<div class='px-4 py-2.5 bg-rose-950/30 border-b border-rose-500/20 flex items-center justify-between gap-2'>" +
            "<span class='text-xs font-extrabold text-rose-300'>💸 Rincian Pengeluaran Periode Ini (" + matchedExps.length + " Catatan)</span>" +
            "<span class='text-xs font-mono font-black text-rose-400'>- " + formatRupiah(matchedExpTotal) + "</span>" +
          "</div>" +
          "<div class='divide-y divide-slate-800/70 max-h-60 overflow-y-auto'>" +
            matchedExps.map(e => {
              const dObj = getKpDateObj(e);
              const hm = String(dObj.getHours()).padStart(2, '0') + ':' + String(dObj.getMinutes()).padStart(2, '0');
              const dOnly = extractDateStr(e) || getLocalDateStr(dObj);
              const expDateLbl = formatKpDateShort(dOnly) || dOnly;
              const noteTxt = cleanExpNote(e.notes);
              return "<div class='px-4 py-2.5 flex items-center justify-between gap-3 hover:bg-slate-900/60'>" +
                "<div class='min-w-0 flex-1'>" +
                  "<div class='flex items-center gap-2 flex-wrap'>" +
                    "<span class='text-xs font-bold text-white'>" + escHtml(e.title || 'Pengeluaran') + "</span>" +
                    "<span class='px-2 py-0.5 rounded-full bg-rose-500/15 text-rose-300 border border-rose-500/30 text-[10px] font-bold'>" + escHtml(e.category || 'Operasional') + "</span>" +
                    "<span class='text-[10px] font-mono text-slate-400'>🕒 " + escHtml(expDateLbl) + " • " + hm + "</span>" +
                  "</div>" +
                  "<div class='text-[11px] text-slate-400 mt-0.5 truncate'>👤 " + escHtml(e.cashierName || 'Admin') + (noteTxt ? (' • ' + escHtml(noteTxt)) : '') + "</div>" +
                "</div>" +
                "<span class='text-xs font-black font-mono text-rose-400 shrink-0'>- " + formatRupiah(e.amount) + "</span>" +
              "</div>";
            }).join('') +
          "</div>";
        }
      }

      if (matched.length === 0) {
        container.innerHTML = "<div class='py-10 px-4 text-center text-slate-500 text-xs'>" +
          "<div class='text-3xl mb-2'>🧾</div>" +
          "<p class='font-bold text-slate-300'>Belum ada catatan transaksi per nota pada periode " + escHtml(periodLabel) + ".</p>" +
          "<p class='text-[11px] text-slate-500 mt-1'>Setiap nota transaksi dari kasir akan otomatis tampil rinci di sini.</p>" +
        "</div>";
        return;
      }

      container.innerHTML = matched.map(t => {
        const inv = String(t.invoiceNumber || t.id || '-');
        const safeInv = inv.replace(/'/g, "\\'");
        const dObj = getKpDateObj(t);
        const hhmm = String(dObj.getHours()).padStart(2, '0') + ':' + String(dObj.getMinutes()).padStart(2, '0');
        const dOnly = extractDateStr(t) || getLocalDateStr(dObj);
        const dayDateLbl = formatKpDateShort(dOnly) || ((getKpDayName(dObj) ? getKpDayName(dObj) + ', ' : '') + dOnly);
        const orderLabel = formatOrderLabel(inv);
        const pm = normalizePm(t.paymentMethod);
        const rev = Number(t.totalAmount || t.grandTotal) || 0;
        const prof = Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0;
        const cust = t.customerName || 'Pelanggan Umum';
        const cashier = t.cashierName || 'Kasir';
        const orderTypeLbl = t.orderType === 'TAKE_AWAY' ? 'Di Bungkus' : 'Makan di Tempat';
        const tableLbl = t.tableNumber ? ' • Meja ' + t.tableNumber : '';

        let pmClass = 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
        let pmIcon = '💵';
        if (pm === 'QRIS') { pmClass = 'bg-purple-500/15 text-purple-300 border-purple-500/30'; pmIcon = '📱'; }
        else if (pm === 'TRANSFER') { pmClass = 'bg-sky-500/15 text-sky-300 border-sky-500/30'; pmIcon = '🏦'; }
        else if (pm === 'EDC') { pmClass = 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30'; pmIcon = '💳'; }

        const itemArr = normalizeKpItems(t.items);
        let itemsHtml = '';
        if (itemArr.length > 0) {
          itemsHtml = "<div class='flex flex-wrap gap-1.5 mt-2'>" + itemArr.map(it => {
            const pName = escHtml(it.productName || it.name || 'Item');
            const qty = Number(it.quantity || it.qty) || 1;
            const sub = Number(it.subtotal != null ? it.subtotal : ((Number(it.unitPrice || it.price) || 0) * qty)) || 0;
            const notePart = it.notes ? " <span class='text-amber-300/80'>(" + escHtml(it.notes) + ")</span>" : '';
            return "<span class='inline-flex items-center gap-1 bg-slate-950 border border-slate-800 px-2 py-0.5 rounded-lg text-[11px] text-slate-200'>" +
              "<span class='font-bold text-teal-300'>" + qty + "x</span>" +
              "<span>" + pName + notePart + "</span>" +
              "<span class='text-slate-400 font-mono'>• " + formatRupiah(sub) + "</span>" +
            "</span>";
          }).join('') + "</div>";
        } else {
          itemsHtml = "<p class='text-xs text-slate-300 mt-1.5'>" + escHtml(t.itemsSummary || ((t.totalItemsCount || 1) + ' item pesanan')) + "</p>";
        }

        return "<div class='p-4 hover:bg-slate-800/35 transition flex flex-col md:flex-row md:items-center justify-between gap-3'>" +
          "<div class='min-w-0 flex-1'>" +
            "<div class='flex items-center gap-2 flex-wrap'>" +
              "<span class='font-mono font-extrabold text-sm text-orange-400'>" + escHtml(orderLabel) + "</span>" +
              "<span class='text-[11px] font-mono text-slate-300 bg-slate-950 border border-slate-800 px-2 py-0.5 rounded-md'>🕒 " + escHtml(dayDateLbl) + " • " + hhmm + "</span>" +
              "<span class='text-[10px] font-bold px-2 py-0.5 rounded-md border " + pmClass + "'>" + pmIcon + " " + escHtml(pm) + "</span>" +
              "<span class='text-[11px] text-slate-300'>👤 " + escHtml(cashier) + "</span>" +
              "<span class='text-[11px] text-slate-400'>• " + escHtml(cust) + " (" + escHtml(orderTypeLbl) + escHtml(tableLbl) + ")</span>" +
            "</div>" +
            itemsHtml +
          "</div>" +
          "<div class='flex items-center justify-between md:justify-end gap-4 shrink-0 border-t md:border-t-0 border-slate-800/70 pt-2.5 md:pt-0'>" +
            "<div class='text-left md:text-right'>" +
              "<div class='text-[10px] uppercase tracking-wider text-slate-400'>Pendapatan</div>" +
              "<div class='text-sm font-black font-mono text-emerald-400'>" + formatRupiah(rev) + "</div>" +
              "<div class='text-[10px] uppercase tracking-wider text-slate-400 mt-0.5'>Keuntungan</div>" +
              "<div class='text-xs font-bold font-mono text-teal-300'>" + formatRupiah(prof) + "</div>" +
            "</div>" +
            "<div class='flex items-center gap-1.5'>" +
              "<button type='button' onclick=\"openKpSingleNotaAlert('" + safeInv + "')\" class='px-2.5 py-1.5 text-[11px] font-bold rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 border border-sky-500/30 transition cursor-pointer whitespace-nowrap'>🧾 Detail Nota</button>" +
              "<button type='button' onclick=\"deleteKpSingleNota('" + safeInv + "')\" title='Hapus " + escHtml(orderLabel) + "' class='px-2.5 py-1.5 text-[11px] font-bold rounded-lg bg-rose-500/10 hover:bg-rose-500/25 text-rose-400 border border-rose-500/30 transition cursor-pointer'>🗑️</button>" +
            "</div>" +
          "</div>" +
        "</div>";
      }).join('');
    }

    function shareDailyPerNota() {
      const targetDate = activeDateFilter;
      const list = (targetDate ? cachedTransactions.filter(t => extractDateStr(t) === targetDate) : cachedTransactions)
        .slice()
        .sort((a, b) => getTrxSortMillis(b) - getTrxSortMillis(a));
      const expList = (targetDate ? cachedExpenses.filter(e => extractDateStr(e) === targetDate) : cachedExpenses)
        .slice()
        .sort((a, b) => getTrxSortMillis(b) - getTrxSortMillis(a));
      const rev = list.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
      const prof = list.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
      const totalExp = expList.reduce((s, e) => s + (Number(e.amount) || 0), 0);
      const net = prof - totalExp;
      const lines = list.slice(0, 30).map(t => {
        const d = getKpDateObj(t);
        const hm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
        const dayStr = getKpDayName(d);
        const itemsArr = normalizeKpItems(t.items);
        const itemsText = t.itemsSummary || (itemsArr.length ? itemsArr.map(it => (it.productName || it.name || 'Item') + ' x' + (it.quantity || it.qty || 1)).join(', ') : '-');
        return '- ' + formatOrderLabel(t.invoiceNumber || t.id) + ' (' + (dayStr ? dayStr + ', ' : '') + hm + ' • ' + normalizePm(t.paymentMethod) + '): ' + formatRupiah(t.totalAmount || t.grandTotal) + ' | Laba ' + formatRupiah(t.profitAmount != null ? t.profitAmount : t.profit) + ' [' + itemsText + ']';
      }).join('\n');
      const expLines = expList.slice(0, 15).map(e => '- ' + (e.title || 'Pengeluaran') + ' [' + (e.category || 'Operasional') + ']: -' + formatRupiah(e.amount)).join('\n');
      const msg = '🧾 Catatan Transaksi & Pengeluaran (' + (targetDate ? formatKpDateShort(targetDate) : 'Semua Tanggal') + ')\n' +
        'Total: ' + list.length + ' nota | ' + expList.length + ' pengeluaran\n' +
        'Pendapatan: ' + formatRupiah(rev) + '\nKeuntungan Kotor: ' + formatRupiah(prof) +
        '\nPengeluaran: ' + formatRupiah(totalExp) + '\nLaba Bersih: ' + formatRupiah(net) +
        '\n\nTRANSAKSI:\n' + (lines || 'Belum ada transaksi.') +
        (expLines ? ('\n\nPENGELUARAN:\n' + expLines) : '');
      if (navigator.share) navigator.share({ title: 'Catatan Transaksi & Pengeluaran', text: msg }).catch(() => {});
      else if (navigator.clipboard) { navigator.clipboard.writeText(msg); alert('Catatan transaksi & pengeluaran disalin!'); }
    }

    function openKpTodayPerNota() {
      kpState.menuOption = 'TODAY';
      kpState.period = 'DAILY';
      kpState.screen = 'DETAIL';
      renderKpReport();
      const el = document.getElementById('kp-report-card');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function renderTransactionsTable(transactions, expenses) {
      const exps = Array.isArray(expenses)
        ? expenses
        : (activeDateFilter ? cachedExpenses.filter(e => extractDateStr(e) === activeDateFilter) : cachedExpenses);
      const badge = document.getElementById('trx-count-badge');
      if (badge) badge.textContent = (transactions ? transactions.length : 0) + ' Nota • ' + exps.length + ' Pengeluaran';

      const sumRev = (transactions || []).reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
      const sumProf = (transactions || []).reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
      const sumExp = exps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
      const netProf = sumProf - sumExp;

      const kpiRev = document.getElementById('trx-kpi-rev');
      const kpiProf = document.getElementById('trx-kpi-profit');
      const kpiExp = document.getElementById('trx-kpi-exp');
      const kpiNet = document.getElementById('trx-kpi-net');
      if (kpiRev) kpiRev.textContent = formatRupiah(sumRev);
      if (kpiProf) kpiProf.textContent = formatRupiah(sumProf);
      if (kpiExp) kpiExp.textContent = '- ' + formatRupiah(sumExp);
      if (kpiNet) {
        kpiNet.textContent = formatRupiah(netProf);
        kpiNet.className = 'font-extrabold font-mono mt-0.5 ' + (netProf >= 0 ? 'text-emerald-300' : 'text-rose-400');
      }

      if (!transactions || transactions.length === 0) {
        document.getElementById('tbody-transactions').innerHTML = "<tr><td colspan='9' class='py-4 text-center text-slate-500'>Tidak ada transaksi pada tanggal ini (Mulai dari Rp 0).</td></tr>";
      } else {
        let html = "";
        transactions.forEach(t => {
          const inv = String(t.invoiceNumber || t.id || '-');
          const safeInv = inv.replace(/'/g, "\\'");
          const pm = normalizePm(t.paymentMethod);
          let pmBadge = "bg-amber-950 text-amber-300 border border-amber-800";
          if (pm === 'QRIS') pmBadge = "bg-purple-950 text-purple-300 border border-purple-800";
          else if (pm === 'TRANSFER') pmBadge = "bg-sky-950 text-sky-300 border border-sky-800";
          else if (pm === 'EDC') pmBadge = "bg-indigo-950 text-indigo-300 border border-indigo-800";

          const prof = Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0;
          const itemDesc = t.itemsSummary || (Array.isArray(t.items) && t.items.length ? t.items.map(it => (it.productName || it.name || 'Item') + ' x' + (it.quantity || it.qty || 1)).join(', ') : 'Pesanan');
          const timeTextRaw = (t.timestamp || t.createdAt) ? String(t.timestamp || t.createdAt).substring(0, 16).replace('T', ' ') : (t.date || '-');
          const dayPrefix = getKpDayName(getKpDateObj(t));
          const timeText = dayPrefix ? (dayPrefix + ', ' + timeTextRaw) : timeTextRaw;

          html += "<tr>" +
            "<td class='py-2 px-3 font-bold text-orange-400'>" + escHtml(formatOrderLabel(inv)) + "</td>" +
            "<td class='py-2 px-3 text-slate-400'>" + escHtml(timeText) + "</td>" +
            "<td class='py-2 px-3 text-white'>" + escHtml(t.cashierName || '-') + "</td>" +
            "<td class='py-2 px-3 text-slate-300'>" + escHtml(t.customerName || 'Umum') + "</td>" +
            "<td class='py-2 px-3 text-slate-200 font-sans'>" + escHtml(itemDesc) + "</td>" +
            "<td class='py-2 px-3 text-center'><span class='px-2 py-0.5 rounded text-[10px] font-bold " + pmBadge + "'>" + escHtml(pm) + "</span></td>" +
            "<td class='py-2 px-3 text-right font-bold text-emerald-400'>" + formatRupiah(t.totalAmount || t.grandTotal) + "</td>" +
            "<td class='py-2 px-3 text-right font-bold text-teal-300'>" + formatRupiah(prof) + "</td>" +
            "<td class='py-2 px-3 text-center whitespace-nowrap'>" +
              "<button type='button' onclick=\"openKpSingleNotaAlert('" + safeInv + "')\" class='px-2 py-1 rounded bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 text-[10px] font-bold mr-1 cursor-pointer'>Nota</button>" +
              "<button type='button' onclick=\"deleteKpSingleNota('" + safeInv + "')\" class='px-2 py-1 rounded bg-rose-500/15 hover:bg-rose-500/25 text-rose-400 text-[10px] font-bold cursor-pointer'>Hapus</button>" +
            "</td>" +
          "</tr>";
        });
        document.getElementById('tbody-transactions').innerHTML = html;
      }

      const trxExpBox = document.getElementById('trx-expenses-box');
      if (trxExpBox) {
        const sortedExps = exps.slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
        if (sortedExps.length === 0) {
          trxExpBox.innerHTML = "<div class='px-4 py-3 flex items-center justify-between text-xs text-slate-400'>" +
            "<span>💸 Laporan Pengeluaran (Realtime Database)</span>" +
            "<span class='font-mono text-slate-500'>0 catatan • Rp 0</span>" +
          "</div>";
        } else {
          trxExpBox.innerHTML = "<div class='px-4 py-2.5 bg-rose-950/50 border-b border-rose-500/30 flex items-center justify-between gap-2 flex-wrap'>" +
            "<div class='flex items-center gap-2'>" +
              "<span class='text-xs font-extrabold text-rose-300'>💸 Laporan Pengeluaran dalam Laporan Transaksi (Realtime Database)</span>" +
              "<span class='px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-200 border border-rose-500/30 text-[10px] font-mono font-bold'>" + sortedExps.length + " Catatan</span>" +
            "</div>" +
            "<span class='text-xs font-mono font-extrabold text-rose-300'>Total: - " + formatRupiah(sumExp) + "</span>" +
          "</div>" +
          "<div class='divide-y divide-slate-800/70 max-h-56 overflow-y-auto'>" +
            sortedExps.map(e => {
              const dObj = getKpDateObj(e);
              const hm = String(dObj.getHours()).padStart(2, '0') + ':' + String(dObj.getMinutes()).padStart(2, '0');
              const dt = extractDateStr(e) || getLocalDateStr(dObj);
              return "<div class='px-4 py-2.5 flex items-center justify-between gap-2 text-xs hover:bg-slate-800/40'>" +
                "<div class='min-w-0 flex-1'>" +
                  "<div class='flex items-center gap-1.5 flex-wrap'>" +
                    "<span class='font-bold text-white'>" + escHtml(e.title || 'Pengeluaran') + "</span>" +
                    "<span class='px-1.5 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-800 text-[10px] font-bold'>" + escHtml(e.category || 'Operasional') + "</span>" +
                  "</div>" +
                  "<div class='text-[11px] text-slate-400 mt-0.5'>" + escHtml(dt) + " " + hm + " • " + escHtml(e.cashierName || e.createdBy || 'Admin') + (cleanExpNote(e.notes) ? (' • Catatan: ' + escHtml(cleanExpNote(e.notes))) : '') + "</div>" +
                "</div>" +
                "<span class='font-mono font-extrabold text-rose-400 shrink-0'>- " + formatRupiah(e.amount) + "</span>" +
              "</div>";
            }).join('') +
          "</div>";
        }
      }
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
      const qEl = document.getElementById('search-expenses');
      const q = qEl ? (qEl.value || '').trim().toLowerCase() : '';
      const visible = (expenses || []).filter(e => {
        if (!q) return true;
        return String(e.title || '').toLowerCase().includes(q) ||
          String(e.category || '').toLowerCase().includes(q) ||
          String(e.notes || '').toLowerCase().includes(q) ||
          String(e.cashierName || '').toLowerCase().includes(q);
      });

      const badge = document.getElementById('expenses-count-badge');
      if (badge) badge.textContent = visible.length + ' Catatan';

      if (visible.length === 0) {
        document.getElementById('tbody-expenses').innerHTML = "<tr><td colspan='6' class='py-4 text-center text-slate-500'>Belum ada pengeluaran & catatan pengeluaran pada tanggal ini.</td></tr>";
        return;
      }
      let html = "";
      visible.forEach(e => {
        const dObj = getKpDateObj(e);
        const hm = String(dObj.getHours()).padStart(2, '0') + ':' + String(dObj.getMinutes()).padStart(2, '0');
        const dateLabel = (extractDateStr(e) || getLocalDateStr(dObj)) + ' ' + hm;
        const noteDisplay = cleanExpNote(e.notes) || '-';
        html += "<tr>" +
          "<td class='py-2 px-3 text-slate-400 whitespace-nowrap'>" + escHtml(dateLabel) + "</td>" +
          "<td class='py-2 px-3 font-sans font-bold text-white'>" + escHtml(e.title || '-') + "</td>" +
          "<td class='py-2 px-3 font-sans text-rose-300'>" + escHtml(e.category || 'Operasional') + "</td>" +
          "<td class='py-2 px-3 font-sans text-slate-300'>" + escHtml(noteDisplay) + "</td>" +
          "<td class='py-2 px-3 font-sans text-slate-300'>" + escHtml(e.cashierName || 'Kasir') + "</td>" +
          "<td class='py-2 px-3 text-right font-bold text-rose-400'>- " + formatRupiah(e.amount) + "</td>" +
        "</tr>";
      });
      document.getElementById('tbody-expenses').innerHTML = html;
    }

    function filterExpensesTable() {
      const targetDate = activeDateFilter;
      const filteredExps = targetDate ? cachedExpenses.filter(e => extractDateStr(e) === targetDate) : cachedExpenses;
      renderExpensesTable(filteredExps);
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

      renderDailyPerNotaSection(filteredTrxs, filteredExps);
      renderExpensesTable(filteredExps);
      renderShiftsTable(filteredShifts);
      renderTransactionsTable(filteredTrxs, filteredExps);
      renderArchiveTable(targetDate ? filteredArchs : cachedArchives);
      if (typeof renderKpReport === 'function') renderKpReport();
    }

    function filterByDate(dateVal) {
      if (!dateVal) {
        resetToTodayFilter();
        return;
      }
      activeDateFilter = dateVal;
      kpTargetDate = dateVal;
      applyFilterAndRender();
    }

    function resetToTodayFilter() {
      const todayStr = getLocalDateStr(new Date());
      activeDateFilter = todayStr;
      kpTargetDate = todayStr;
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

    function normalizeExpDedupKey(rawId) {
      return String(rawId || "").trim().replace(/^CF-(?=EXP-)/i, "");
    }

    function parseNumSafe(v) {
      if (typeof v === "number") return isFinite(v) ? v : 0;
      if (v == null || v === "") return 0;
      const direct = Number(v);
      if (isFinite(direct)) return direct;
      const cleaned = String(v).replace(/[^0-9,.-]/g, "").replace(/\.(?=\d{3}(\D|$))/g, "").replace(",", ".");
      const parsed = Number(cleaned);
      return isFinite(parsed) ? parsed : 0;
    }

    function normalizeRtdbValue(val) {
      if (!val || typeof val !== 'object') return null;
      if (Array.isArray(val)) {
        const out = {};
        val.forEach((item, idx) => {
          if (item && typeof item === 'object') {
            const key = String(item.id || item.invoiceNumber || idx);
            out[key] = item;
          }
        });
        return Object.keys(out).length ? out : null;
      }
      return val;
    }

    function deepMergeRtdbNodes(a, b) {
      const na = normalizeRtdbValue(a);
      const nb = normalizeRtdbValue(b);
      if (!na) return nb;
      if (!nb) return na;
      const out = Object.assign({}, na);
      Object.keys(nb).forEach(k => {
        const av = out[k];
        const bv = nb[k];
        if (av && bv && typeof av === 'object' && typeof bv === 'object' && !Array.isArray(av) && !Array.isArray(bv)) {
          out[k] = deepMergeRtdbNodes(av, bv);
        } else {
          out[k] = bv;
        }
      });
      return out;
    }

    let discoveredRelayBase = null;

    function getRelayBaseCandidates() {
      const clean = (v) => {
        if (!v) return null;
        const s = String(v).trim().replace(/\/+$/, "");
        return /^https?:\/\//i.test(s) ? s : null;
      };
      const qp = new URLSearchParams(window.location.search);
      const paramRelay = clean(qp.get("localRelay") || qp.get("relay") || qp.get("relayUrl"));
      if (paramRelay) {
        try { localStorage.setItem("pos_local_relay_url", paramRelay); } catch (_) {}
      }
      let storedRelay = null;
      try { storedRelay = clean(localStorage.getItem("pos_local_relay_url")); } catch (_) {}
      const list = [];
      if (discoveredRelayBase) list.push(discoveredRelayBase);
      if (paramRelay) list.push(paramRelay);
      if (window.location.protocol === "http:" || window.location.protocol === "https:") {
        list.push(window.location.origin);
      }
      if (storedRelay) list.push(storedRelay);
      if (!discoveredRelayBase && !paramRelay) {
        list.push("http://127.0.0.1:8080", "http://127.0.0.1:8765", "http://localhost:8080", "http://localhost:8765");
      }
      return Array.from(new Set(list.filter(Boolean).map(b => b.replace(/\/+$/, ""))));
    }

    async function fetchNodeMerged(nodePath) {
      const cleanPath = String(nodePath || "").replace(/^\/+/, "");
      const safeJson = (r) => {
        if (!r || !r.ok) return null;
        const ct = (r.headers.get("content-type") || "").toLowerCase();
        if (ct.indexOf("text/html") !== -1) return null;
        return r.json();
      };
      const rtdbFetch = (async () => {
        for (const base of RTDB_CANDIDATES) {
          try {
            const res = await fetch(base + "/" + cleanPath + ".json", { cache: "no-store" });
            if (res.status === 401 || res.status === 403) {
              rtdbPermissionDeniedPaths.push({ path: cleanPath, status: res.status });
              continue;
            }
            const parsed = await safeJson(res);
            if (parsed && typeof parsed === "object" && !parsed.error) {
              activeRtdbBase = base;
              return normalizeRtdbValue(parsed);
            }
          } catch (_) {}
        }
        return null;
      })();
      const relayFetch = (async () => {
        for (const base of getRelayBaseCandidates()) {
          try {
            const res = await fetch(base + "/api/sync/" + cleanPath, { cache: "no-store" });
            const parsed = await safeJson(res);
            if (parsed && typeof parsed === "object" && !parsed.error) {
              discoveredRelayBase = base;
              return normalizeRtdbValue(parsed);
            }
          } catch (_) {}
        }
        return null;
      })();
      const [a, b] = await Promise.all([rtdbFetch, relayFetch]);
      return deepMergeRtdbNodes(a, b);
    }

    async function loadData() {
      try {
        rtdbPermissionDeniedPaths = [];
        const [shifts, trxs, dailyTrxNodes, daily, flows, expensesData, dailyExpensesData, statsData] = await Promise.all([
          fetchNodeMerged("shift_reports"),
          fetchNodeMerged("transactions"),
          fetchNodeMerged("reports/daily_transactions"),
          fetchNodeMerged("daily_reports"),
          fetchNodeMerged("cash_flows"),
          fetchNodeMerged("expenses"),
          fetchNodeMerged("reports/daily_expenses"),
          fetchNodeMerged("realtime_stats")
        ]);

        cachedShifts = shifts ? Object.values(shifts).filter(s => s && typeof s === 'object').reverse() : [];

        // Gabungkan /transactions dan /reports/daily_transactions/{YYYY-MM-DD}/{invoice} tanpa duplikasi
        const trxMap = new Map();
        if (trxs && typeof trxs === 'object') {
          Object.entries(trxs).forEach(([k, v]) => {
            if (!v || typeof v !== 'object') return;
            const key = String(v.invoiceNumber || v.id || k);
            trxMap.set(key, Object.assign({ id: k }, v));
          });
        }
        if (dailyTrxNodes && typeof dailyTrxNodes === 'object') {
          Object.entries(dailyTrxNodes).forEach(([dateKey, byInv]) => {
            if (!byInv || typeof byInv !== 'object') return;
            Object.entries(byInv).forEach(([invKey, v]) => {
              if (!v || typeof v !== 'object') return;
              const key = String(v.invoiceNumber || v.id || invKey);
              const existing = trxMap.get(key) || {};
              trxMap.set(key, Object.assign({ id: invKey, date: v.date || dateKey }, v, existing));
            });
          });
        }
        cachedTransactions = Array.from(trxMap.values()).sort((a, b) => getTrxSortMillis(b) - getTrxSortMillis(a));

        // Pengeluaran & Catatan Pengeluaran: gabungkan seluruh sumber RTDB + Relay dengan prioritas ketat (/expenses > daily_expenses > realtime_stats > cash_flows > archives)
        const expMap = new Map();
        const upsertExp = (rawKey, v, fallbackDate, rank = 2) => {
          if (!v || typeof v !== 'object') return;
          const amt = parseNumSafe(v.amount);
          const cleanNote = cleanExpNote(v.notes);
          if (amt <= 0 && !v.title && !v.description && !cleanNote) return;
          const dedupKey = normalizeExpDedupKey(v.id || rawKey);
          if (!dedupKey) return;
          const existing = expMap.get(dedupKey);
          const prevRank = (existing && typeof existing._rank === 'number') ? existing._rank : -1;
          const nextObj = {
            id: v.id || rawKey,
            title: (rank >= prevRank ? (v.title || v.description || v.category) : null) || (existing && existing.title) || v.title || v.description || 'Pengeluaran',
            category: (rank >= prevRank ? v.category : null) || (existing && existing.category) || v.category || 'Operasional',
            notes: rank >= prevRank ? cleanNote : ((existing && existing.notes != null) ? existing.notes : cleanNote),
            amount: rank >= prevRank ? (v.amount != null ? amt : (existing && existing.amount) || 0) : ((existing && existing.amount) || amt),
            cashierName: (rank >= prevRank ? (v.createdBy || v.cashierName) : null) || (existing && existing.cashierName) || v.createdBy || v.cashierName || 'Admin',
            date: (rank >= prevRank ? (extractDateStr(v) || fallbackDate) : null) || (existing && existing.date) || extractDateStr(v) || fallbackDate || '',
            timestamp: (rank >= prevRank ? (v.timestamp || v.syncedAt) : null) || (existing && existing.timestamp) || v.timestamp || v.syncedAt || '',
            timestampMillis: (rank >= prevRank ? parseMillisSafe(v.timestampMillis) : null) || (existing && existing.timestampMillis) || parseMillisSafe(v.timestampMillis) || null,
            _rank: Math.max(prevRank, rank)
          };
          expMap.set(dedupKey, nextObj);
        };
        if (expensesData && typeof expensesData === 'object') {
          Object.entries(expensesData).forEach(([k, v]) => {
            upsertExp(k, v, '', 4);
          });
        }
        if (dailyExpensesData && typeof dailyExpensesData === 'object') {
          Object.entries(dailyExpensesData).forEach(([dateKey, dayMap]) => {
            if (!dayMap || typeof dayMap !== 'object') return;
            if (dayMap.amount != null && (dayMap.title || dayMap.category || dayMap.id)) {
              upsertExp(dateKey, dayMap, '', 3);
            } else {
              const dFallback = /^\d{4}-\d{2}-\d{2}$/.test(dateKey) ? dateKey : '';
              Object.entries(dayMap).forEach(([expKey, expVal]) => {
                upsertExp(expKey, expVal, dFallback, 3);
              });
            }
          });
        }
        // Fallback 3: Mirror pengeluaran dalam realtime_stats (expenses_by_date, expenses, last_expense)
        if (statsData && typeof statsData === 'object') {
          if (statsData.expenses_by_date && typeof statsData.expenses_by_date === 'object') {
            Object.entries(statsData.expenses_by_date).forEach(([dateKey, dayMap]) => {
              if (!dayMap || typeof dayMap !== 'object') return;
              const dFallback = /^\d{4}-\d{2}-\d{2}$/.test(dateKey) ? dateKey : '';
              Object.entries(dayMap).forEach(([expKey, expVal]) => {
                upsertExp(expKey, expVal, dFallback, 2);
              });
            });
          }
          if (statsData.expenses && typeof statsData.expenses === 'object') {
            Object.entries(statsData.expenses).forEach(([k, v]) => {
              upsertExp(k, v, '', 2);
            });
          }
          if (statsData.last_expense && typeof statsData.last_expense === 'object') {
            const le = statsData.last_expense;
            const leId = String(le.lastExpenseId || le.id || '').trim();
            const leAmt = parseNumSafe(le.lastExpenseAmount != null ? le.lastExpenseAmount : le.amount);
            const leTitle = String(le.lastExpenseTitle || le.title || '').trim();
            if ((leId || leAmt > 0) && (leAmt > 0 || leTitle)) {
              const k = normalizeExpDedupKey(leId || ('LASTEXP-' + (le.date || getLocalDateStr(new Date()))));
              if (!expMap.has(k)) {
                upsertExp(k, {
                  id: leId || k,
                  title: leTitle || 'Pengeluaran Terbaru',
                  amount: leAmt,
                  category: le.lastExpenseCategory || le.category || 'Operasional',
                  notes: cleanExpNote(le.lastExpenseNotes != null ? le.lastExpenseNotes : le.notes),
                  createdBy: le.lastExpenseBy || le.createdBy || 'Admin',
                  date: le.date || extractDateStr(le),
                  timestamp: le.lastUpdated || le.timestamp || '',
                  timestampMillis: le.timestampMillis || null
                }, le.date || '', 1);
              }
            }
          }
        }
        // Fallback 4: /cash_flows tipe OUT
        if (flows && typeof flows === 'object') {
          Object.entries(flows).forEach(([k, f]) => {
            if (!f || typeof f !== 'object') return;
            const fType = (f.type || 'OUT').toUpperCase();
            const dedupKey = normalizeExpDedupKey(f.id || k);
            if (fType === 'OUT' && !expMap.has(dedupKey)) {
              upsertExp(dedupKey, {
                id: f.id || k,
                title: f.description || f.category || 'Kas Keluar',
                category: f.category || 'Operasional',
                notes: cleanExpNote(f.notes),
                amount: f.amount,
                cashierName: f.cashierName || f.createdBy || 'Staff',
                date: f.date || extractDateStr(f),
                timestamp: f.timestamp || '',
                timestampMillis: f.timestampMillis || null
              }, '', 1);
            }
          });
        }
        // Fallback 5 & 6: expensesList atau rekap totalExpenses/expenseNotes dalam /daily_reports & /shift_reports
        const ingestReportExpenses = (rec, fallbackId) => {
          if (!rec || typeof rec !== 'object') return;
          const rDate = extractDateStr(rec) || '';
          const rawList = normalizeKpItems(rec.expensesList || rec.expenses);
          rawList.forEach((it, idx) => {
            upsertExp(it.id || ('REPEXP-' + fallbackId + '-' + idx), it, rDate, 0);
          });
          const hasDateItem = Array.from(expMap.values()).some(e => (extractDateStr(e) || e.date) === rDate);
          const repExpAmt = parseNumSafe(rec.totalExpenses);
          const repExpNotes = cleanExpNote(rec.expenseNotes);
          if (!hasDateItem && (repExpAmt > 0 || repExpNotes)) {
            upsertExp('REKAP-EXP-' + (rDate || fallbackId), {
              id: 'REKAP-EXP-' + (rDate || fallbackId),
              title: 'Rekap Pengeluaran (' + (rDate || fallbackId) + ')',
              category: 'Rekap Shift/Harian',
              notes: repExpNotes,
              amount: repExpAmt,
              createdBy: rec.cashierName || rec.cashierNames || rec.lastShiftCashier || 'Kasir',
              date: rDate,
              timestamp: rec.lastUpdated || rec.timestamp || (rDate ? rDate + 'T23:59:00Z' : ''),
              timestampMillis: rec.timestampMillis || rec.updatedAt || null
            }, rDate, 0);
          }
        };
        if (daily && typeof daily === 'object') {
          Object.entries(daily).forEach(([k, d]) => ingestReportExpenses(d, k));
        }
        if (shifts && typeof shifts === 'object') {
          Object.entries(shifts).forEach(([k, s]) => ingestReportExpenses(s, k));
        }
        cachedExpenses = Array.from(expMap.values()).sort((a, b) => getTrxSortMillis(b) - getTrxSortMillis(a));

        const warnEl = document.getElementById('rtdb-rules-warning');
        if (warnEl) {
          const deniedExp = rtdbPermissionDeniedPaths.find(x => x.path === 'expenses' || x.path === 'reports/daily_expenses');
          if (deniedExp && cachedExpenses.length === 0) {
            const codeEl = document.getElementById('rtdb-rules-code');
            if (codeEl) codeEl.textContent = String(deniedExp.status);
            warnEl.classList.remove('hidden');
          } else {
            warnEl.classList.add('hidden');
          }
        }

        // Susun daftar Arsip Laporan HANYA dari data Tutup Toko (/shift_reports, /daily_reports, dan /cash_flows)
        const archives = [];

        if (shifts) {
          Object.values(shifts).forEach(s => {
            if (!s || typeof s !== 'object') return;
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
            const isArchivedDoc = d.archivedByTutupToko === true ||
              Number(d.totalRevenue || d.grossRevenue || d.totalOmset) > 0 ||
              Number(d.totalTransactions || d.transactionCount) > 0 ||
              d.actualCashCount != null ||
              d.actualCash != null;
            if (!isArchivedDoc) return;
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
            if (!f || typeof f !== 'object') return;
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

    // --- MODUL LAPORAN TRANSAKSI (MENU PILIHAN + HARIAN/BULANAN/TAHUNAN + LAPORAN PENGELUARAN) ---
    var KP_DAYS = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
    var KP_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agt', 'Sep', 'Okt', 'Nov', 'Des'];
    let kpState = { screen: 'MENU', menuOption: 'ALL', period: 'DAILY', metric: 'COUNT', expenseScope: 'TODAY' };

    function formatOrderLabel(inv) {
      const raw = String(inv || '-').trim();
      if (!raw || raw === '-') return 'Order';
      const stripped = raw.replace(/^#/, '');
      if (/^TRX(?=[-_]|$)/i.test(stripped)) return stripped.replace(/^TRX/i, 'Order');
      if (/^Order\b/i.test(stripped)) return stripped;
      return 'Order ' + stripped;
    }

    function getKpDayName(val) {
      let dObj = null;
      if (val instanceof Date) dObj = val;
      else if (typeof val === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(val.trim())) {
        const p = val.trim().split('-').map(Number);
        dObj = new Date(p[0], p[1] - 1, p[2], 12, 0, 0);
      } else if (val && typeof val === 'object') {
        dObj = getKpDateObj(val);
      } else if (val) {
        dObj = new Date(val);
      }
      if (!dObj || isNaN(dObj.getTime())) return '';
      return KP_DAYS[dObj.getDay()] || '';
    }

    function getKpDateObj(t) {
      if (t && t.timestampMillis) {
        const ms = parseMillisSafe(t.timestampMillis);
        if (ms > 0) return new Date(ms);
      }
      const dStr = extractDateStr(t);
      if (dStr && /^\d{4}-\d{2}-\d{2}$/.test(dStr)) {
        const rawTs = (t && (t.timestamp || t.syncedAt || t.createdAt)) || '';
        if (rawTs) {
          const parsedRaw = new Date(rawTs);
          if (!isNaN(parsedRaw.getTime())) return parsedRaw;
        }
        const parts = dStr.split('-').map(Number);
        return new Date(parts[0], parts[1] - 1, parts[2], 12, 0, 0);
      }
      return new Date((t && t.timestamp) || Date.now());
    }

    let kpTodayNotaSearch = '';
    let kpExpenseSearch = '';
    let kpTargetDate = getLocalDateStr(new Date());

    function formatKpDateShort(dateStr) {
      if (!dateStr || !/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return dateStr || '';
      const parts = dateStr.split('-').map(Number);
      const dayName = KP_DAYS[new Date(parts[0], parts[1] - 1, parts[2], 12, 0, 0).getDay()] || '';
      return (dayName ? (dayName + ', ') : '') + parts[2] + ' ' + (KP_MONTHS[parts[1] - 1] || '') + ' ' + parts[0];
    }

    function syncKpDateControls() {
      const todayStr = getLocalDateStr(new Date());
      if (!kpTargetDate) kpTargetDate = todayStr;
      const isToday = kpTargetDate === todayStr;
      const formatted = formatKpDateShort(kpTargetDate);

      ['kp-menu-date-picker', 'kp-date-picker', 'kp-exp-date-picker'].forEach(id => {
        const inp = document.getElementById(id);
        if (inp) {
          inp.value = kpTargetDate;
          inp.max = todayStr;
        }
      });
      ['kp-menu-btn-next-date', 'kp-btn-next-date', 'kp-exp-btn-next-date'].forEach(id => {
        const btn = document.getElementById(id);
        if (btn) btn.disabled = isToday;
      });
      ['kp-menu-btn-today-date', 'kp-btn-today-date', 'kp-exp-btn-today-date'].forEach(id => {
        const btn = document.getElementById(id);
        if (btn) btn.classList.toggle('hidden', isToday);
      });
      ['kp-menu-date-badge', 'kp-detail-date-badge', 'kp-exp-date-badge'].forEach(id => {
        const badge = document.getElementById(id);
        if (badge) badge.textContent = isToday ? ('Hari Ini • ' + formatted) : ('Tgl Sebelumnya • ' + formatted);
      });
      const subLbl = document.getElementById('kp-menu-today-sublabel');
      if (subLbl) {
        subLbl.textContent = isToday ? ('Hari Ini (' + formatted + ')') : ('Tanggal dipilih: ' + formatted);
        subLbl.className = isToday ? 'block text-[11px] font-medium text-slate-500' : 'block text-[11px] font-semibold text-[#00796B]';
      }
      const expMenuSub = document.getElementById('kp-menu-expense-sublabel');
      if (expMenuSub) {
        const dayExps = filterKpItemsByScope(cachedExpenses, 'TODAY');
        const dayExpSum = dayExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
        expMenuSub.textContent = dayExps.length + ' catatan (' + formatted + ') • ' + formatRupiah(dayExpSum);
      }
    }

    function setKpTargetDate(dateStr) {
      const todayStr = getLocalDateStr(new Date());
      kpTargetDate = (dateStr && /^\d{4}-\d{2}-\d{2}$/.test(dateStr)) ? dateStr : todayStr;
      renderKpReport();
    }

    function stepKpTargetDate(deltaDays) {
      const base = (kpTargetDate && /^\d{4}-\d{2}-\d{2}$/.test(kpTargetDate)) ? kpTargetDate : getLocalDateStr(new Date());
      const parts = base.split('-').map(Number);
      const next = new Date(parts[0], parts[1] - 1, parts[2] + deltaDays, 12, 0, 0);
      const nextStr = getLocalDateStr(next);
      const todayStr = getLocalDateStr(new Date());
      if (nextStr > todayStr) return;
      kpTargetDate = nextStr;
      renderKpReport();
    }

    function resetKpTargetDate() {
      kpTargetDate = getLocalDateStr(new Date());
      renderKpReport();
    }

    function setKpTodayNotaSearch(val) {
      kpTodayNotaSearch = (val || '').trim().toLowerCase();
      renderKpReport();
    }

    function setKpExpenseSearch(val) {
      kpExpenseSearch = (val || '').trim().toLowerCase();
      renderKpReport();
    }

    function setKpExpenseScope(scope) {
      kpState.expenseScope = scope || 'TODAY';
      renderKpReport();
    }

    function filterKpItemsByScope(items, scope) {
      const targetStr = kpTargetDate || getLocalDateStr(new Date());
      const parts = targetStr.split('-').map(Number);
      const tY = parts[0], tM = parts[1] - 1, tD = parts[2];
      return (items || []).filter(item => {
        if (scope === 'ALL') return true;
        const dStr = extractDateStr(item);
        if (scope === 'TODAY' && dStr) return dStr === targetStr;
        const d = getKpDateObj(item);
        if (scope === 'TODAY') return d.getFullYear() === tY && d.getMonth() === tM && d.getDate() === tD;
        if (scope === 'THIS_MONTH') return d.getFullYear() === tY && d.getMonth() === tM;
        if (scope === 'THIS_YEAR') return d.getFullYear() === tY;
        return true;
      });
    }

    function filterKpTrxs() {
      return filterKpItemsByScope(cachedTransactions, kpState.menuOption);
    }

    function filterKpExpenses(scopeOverride) {
      return filterKpItemsByScope(cachedExpenses, scopeOverride || kpState.menuOption);
    }

    function aggregateKpRecords(list, period, expList) {
      const groups = new Map();
      const ensureGroup = (d) => {
        const y = d.getFullYear(), m = d.getMonth(), day = d.getDate();
        let key = String(y);
        if (period === 'DAILY') key = y + '-' + String(m + 1).padStart(2, '0') + '-' + String(day).padStart(2, '0');
        else if (period === 'MONTHLY') key = y + '-' + String(m + 1).padStart(2, '0');
        if (!groups.has(key)) {
          groups.set(key, {
            key: key, year: y, month: m, day: day,
            startTime: period === 'DAILY' ? new Date(y, m, day).getTime() : (period === 'MONTHLY' ? new Date(y, m, 1).getTime() : new Date(y, 0, 1).getTime()),
            primaryLabel: period === 'DAILY' ? String(day) : (period === 'MONTHLY' ? KP_MONTHS[m] : String(y)),
            secondaryLabel: period === 'DAILY' ? (KP_DAYS[d.getDay()].slice(0, 3) + ', ' + KP_MONTHS[m] + '<br>' + y) : (period === 'MONTHLY' ? String(y) : 'Tahun'),
            fullTitle: period === 'DAILY' ? (KP_DAYS[d.getDay()] + ', ' + day + ' ' + KP_MONTHS[m] + ' ' + y) : (period === 'MONTHLY' ? (KP_MONTHS[m] + ' ' + y) : ('Tahun ' + y)),
            transactions: [], expenses: [], revenue: 0, profit: 0, totalExpense: 0, netProfit: 0
          });
        }
        return groups.get(key);
      };

      (list || []).forEach(t => {
        const g = ensureGroup(getKpDateObj(t));
        g.transactions.push(t);
        g.revenue += Number(t.totalAmount || t.grandTotal) || 0;
        g.profit += Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0;
      });

      const resolvedExpenses = Array.isArray(expList) ? expList : filterKpExpenses();
      resolvedExpenses.forEach(e => {
        const g = ensureGroup(getKpDateObj(e));
        g.expenses.push(e);
        g.totalExpense += Number(e.amount) || 0;
      });

      const arr = Array.from(groups.values());
      arr.forEach(g => { g.netProfit = g.profit - g.totalExpense; });
      return arr.sort((a, b) => b.startTime - a.startTime);
    }

    function fmtKpNode(v, metric) {
      if (metric === 'COUNT') return (Number(v) || 0).toFixed(2).replace('.', ',');
      const r = Math.round(Number(v) || 0);
      if (r === 0) return '0';
      if (Math.abs(r) >= 1000000) return (r / 1000000).toFixed(1).replace('.', ',') + 'jt';
      if (Math.abs(r) >= 1000) return (r % 1000 === 0) ? (Math.round(r / 1000) + 'rb') : ((r / 1000).toFixed(1).replace('.', ',') + 'rb');
      return String(r);
    }

    function buildKpSvg(records, period, metric) {
      const chrono = records.slice().sort((a, b) => a.startTime - a.startTime);
      const valOf = r => metric === 'COUNT' ? r.transactions.length : (metric === 'REVENUE' ? r.revenue : (metric === 'EXPENSE' ? r.totalExpense : r.profit));
      const strokeHex = metric === 'EXPENSE' ? '#E11D48' : '#EAB308';
      const gradId = metric === 'EXPENSE' ? 'kpRoseTpl' : 'kpGoldTpl';
      const gradDef = metric === 'EXPENSE'
        ? '<linearGradient id="kpRoseTpl" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#E11D48" stop-opacity="0.42"/><stop offset="55%" stop-color="#F43F5E" stop-opacity="0.20"/><stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.02"/></linearGradient>'
        : '<linearGradient id="kpGoldTpl" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#EAB308" stop-opacity="0.48"/><stop offset="55%" stop-color="#F59E0B" stop-opacity="0.22"/><stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.02"/></linearGradient>';
      const points = [], ticks = [];
      let maxX = 15;
      if (chrono.length === 0) {
        [0, 3, 6, 9, 12, 15].forEach(x => ticks.push({ x: x, label: String(x) }));
      } else if (period === 'DAILY') {
        const sameMonth = new Set(chrono.map(r => r.year + '-' + r.month)).size === 1;
        if (sameMonth) {
          const maxD = Math.max.apply(null, chrono.map(r => r.day));
          maxX = Math.max(15, Math.floor((maxD + 2) / 3) * 3);
          if (chrono.length === 1) points.push({ x: 0, v: 0, lbl: fmtKpNode(0, metric) });
          chrono.forEach(r => points.push({ x: r.day, v: valOf(r), lbl: fmtKpNode(valOf(r), metric) }));
          const step = maxX <= 18 ? 3 : 5;
          for (let t = 0; t <= maxX; t += step) ticks.push({ x: t, label: String(t) });
        } else {
          if (chrono.length === 1) points.push({ x: 0, v: 0, lbl: fmtKpNode(0, metric) });
          const off = chrono.length === 1 ? 1 : 0;
          chrono.forEach((r, i) => points.push({ x: i + off, v: valOf(r), lbl: fmtKpNode(valOf(r), metric) }));
          maxX = Math.max(1, points[points.length - 1].x);
          const step = Math.max(1, Math.floor(chrono.length / 5));
          chrono.forEach((r, i) => { if (i % step === 0 || i === chrono.length - 1) ticks.push({ x: i + off, label: r.day + ' ' + KP_MONTHS[r.month] }); });
        }
      } else if (period === 'MONTHLY') {
        maxX = 12;
        if (chrono.length === 1) points.push({ x: 0, v: 0, lbl: fmtKpNode(0, metric) });
        chrono.forEach(r => points.push({ x: r.month + 1, v: valOf(r), lbl: fmtKpNode(valOf(r), metric) }));
        [0, 2, 4, 6, 8, 10, 12].forEach(m => ticks.push({ x: m, label: m === 0 ? '0' : KP_MONTHS[m - 1] }));
      } else {
        if (chrono.length === 1) {
          points.push({ x: 0, v: 0, lbl: fmtKpNode(0, metric) });
          points.push({ x: 1, v: valOf(chrono[0]), lbl: fmtKpNode(valOf(chrono[0]), metric) });
          maxX = 1.15;
          ticks.push({ x: 0, label: '0' }, { x: 1, label: String(chrono[0].year) });
        } else {
          chrono.forEach((r, i) => { points.push({ x: i, v: valOf(r), lbl: fmtKpNode(valOf(r), metric) }); ticks.push({ x: i, label: String(r.year) }); });
          maxX = Math.max(1, chrono.length - 1);
        }
      }
      const W = 440, H = 220, padL = 22, padR = 20, padT = 30, padB = 30, plotW = W - padL - padR, plotH = H - padT - padB;
      const rawMaxY = points.length ? Math.max.apply(null, points.map(p => p.v)) : 1;
      const maxY = rawMaxY <= 0 ? 1 : rawMaxY;
      const mapX = x => padL + (x / Math.max(0.1, maxX)) * plotW;
      const mapY = v => padT + plotH - (v / maxY) * plotH;
      let areaSvg = '', lineSvg = '', nodesSvg = '', ticksSvg = '';
      if (points.length >= 2) {
        const coords = points.map(p => mapX(p.x).toFixed(1) + ',' + mapY(p.v).toFixed(1)).join(' L ');
        const baseY = (padT + plotH).toFixed(1);
        areaSvg = '<path d="M ' + mapX(points[0].x).toFixed(1) + ',' + baseY + ' L ' + coords + ' L ' + mapX(points[points.length - 1].x).toFixed(1) + ',' + baseY + ' Z" fill="url(#' + gradId + ')" />';
        lineSvg = '<path d="M ' + coords + '" fill="none" stroke="' + strokeHex + '" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />';
      }
      points.forEach(p => {
        const cx = mapX(p.x).toFixed(1), cy = mapY(p.v).toFixed(1);
        nodesSvg += '<circle cx="' + cx + '" cy="' + cy + '" r="5.5" fill="#FFFFFF" stroke="' + strokeHex + '" stroke-width="3" />';
        nodesSvg += '<text x="' + cx + '" y="' + (Number(cy) - 10).toFixed(1) + '" text-anchor="middle" font-size="11" font-weight="600" fill="#1E293B">' + p.lbl + '</text>';
      });
      ticks.forEach(tk => {
        ticksSvg += '<text x="' + mapX(tk.x).toFixed(1) + '" y="' + (H - 8) + '" text-anchor="middle" font-size="11" fill="#94A3B8">' + tk.label + '</text>';
      });
      const emptyNote = points.length === 0 ? '<text x="' + (W / 2) + '" y="' + (H / 2) + '" text-anchor="middle" font-size="12" fill="#94A3B8">Belum ada data pada periode ini</text>' : '';
      return '<svg viewBox="0 0 ' + W + ' ' + H + '" class="w-full h-56 select-none overflow-visible">' +
        '<defs>' + gradDef + '</defs>' +
        areaSvg + lineSvg +
        '<line x1="' + padL + '" y1="' + (padT + plotH) + '" x2="' + (W - padR) + '" y2="' + (padT + plotH) + '" stroke="#CBD5E1" stroke-width="1.2"/>' +
        '<line x1="' + (W - padR) + '" y1="' + (padT - 8) + '" x2="' + (W - padR) + '" y2="' + (padT + plotH) + '" stroke="#CBD5E1" stroke-width="1.2"/>' +
        nodesSvg + ticksSvg + emptyNote + '</svg>';
    }

    function buildKpPerNotaExpenseSvg(expList) {
      const chrono = (expList || []).slice().sort((a, b) => getKpDateObj(a).getTime() - getKpDateObj(b).getTime());
      const points = [], ticks = [];
      let maxX = 5;
      if (chrono.length === 0) {
        [0, 1, 2, 3, 4, 5].forEach(x => ticks.push({ x: x, label: '#' + x }));
      } else {
        const single = chrono.length === 1;
        if (single) {
          points.push({ x: 0, v: 0, lbl: '0' });
          ticks.push({ x: 0, label: '0' });
        }
        const off = single ? 1 : 0;
        chrono.forEach((e, i) => {
          const amt = Number(e.amount) || 0;
          points.push({ x: i + off, v: amt, lbl: fmtKpNode(amt, 'EXPENSE') });
        });
        maxX = Math.max(1, points[points.length - 1].x);
        const step = Math.max(1, Math.floor(chrono.length / 4));
        chrono.forEach((e, i) => {
          if (i % step === 0 || i === chrono.length - 1) {
            const d = getKpDateObj(e);
            const hhmm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
            ticks.push({ x: i + off, label: hhmm });
          }
        });
      }
      const W = 440, H = 220, padL = 22, padR = 20, padT = 30, padB = 30, plotW = W - padL - padR, plotH = H - padT - padB;
      const rawMaxY = points.length ? Math.max.apply(null, points.map(p => p.v)) : 1;
      const maxY = rawMaxY <= 0 ? 1 : rawMaxY;
      const mapX = x => padL + (x / Math.max(0.1, maxX)) * plotW;
      const mapY = v => padT + plotH - (v / maxY) * plotH;
      let areaSvg = '', lineSvg = '', nodesSvg = '', ticksSvg = '';
      if (points.length >= 2) {
        const coords = points.map(p => mapX(p.x).toFixed(1) + ',' + mapY(p.v).toFixed(1)).join(' L ');
        const baseY = (padT + plotH).toFixed(1);
        areaSvg = '<path d="M ' + mapX(points[0].x).toFixed(1) + ',' + baseY + ' L ' + coords + ' L ' + mapX(points[points.length - 1].x).toFixed(1) + ',' + baseY + ' Z" fill="url(#kpRosePerNotaTpl)" />';
        lineSvg = '<path d="M ' + coords + '" fill="none" stroke="#E11D48" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />';
      }
      points.forEach(p => {
        const cx = mapX(p.x).toFixed(1), cy = mapY(p.v).toFixed(1);
        nodesSvg += '<circle cx="' + cx + '" cy="' + cy + '" r="5.5" fill="#FFFFFF" stroke="#E11D48" stroke-width="3" />';
        nodesSvg += '<text x="' + cx + '" y="' + (Number(cy) - 10).toFixed(1) + '" text-anchor="middle" font-size="11" font-weight="600" fill="#1E293B">' + p.lbl + '</text>';
      });
      ticks.forEach(tk => {
        ticksSvg += '<text x="' + mapX(tk.x).toFixed(1) + '" y="' + (H - 8) + '" text-anchor="middle" font-size="11" fill="#94A3B8">' + tk.label + '</text>';
      });
      const emptyNote = points.length === 0 ? '<text x="' + (W / 2) + '" y="' + (H / 2) + '" text-anchor="middle" font-size="12" fill="#94A3B8">Belum ada pengeluaran pada tanggal ini</text>' : '';
      return '<svg viewBox="0 0 ' + W + ' ' + H + '" class="w-full h-56 select-none overflow-visible">' +
        '<defs><linearGradient id="kpRosePerNotaTpl" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#E11D48" stop-opacity="0.42"/><stop offset="55%" stop-color="#F43F5E" stop-opacity="0.20"/><stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.02"/></linearGradient></defs>' +
        areaSvg + lineSvg +
        '<line x1="' + padL + '" y1="' + (padT + plotH) + '" x2="' + (W - padR) + '" y2="' + (padT + plotH) + '" stroke="#CBD5E1" stroke-width="1.2"/>' +
        '<line x1="' + (W - padR) + '" y1="' + (padT - 8) + '" x2="' + (W - padR) + '" y2="' + (padT + plotH) + '" stroke="#CBD5E1" stroke-width="1.2"/>' +
        nodesSvg + ticksSvg + emptyNote + '</svg>';
    }

    function buildKpPerNotaSvg(trxs, metric, expList) {
      if (metric === 'EXPENSE') return buildKpPerNotaExpenseSvg(expList);
      const chrono = trxs.slice().sort((a, b) => getKpDateObj(a).getTime() - getKpDateObj(b).getTime());
      const points = [], ticks = [];
      let maxX = 5;
      if (chrono.length === 0) {
        [0, 1, 2, 3, 4, 5].forEach(x => ticks.push({ x: x, label: '#' + x }));
      } else {
        const single = chrono.length === 1;
        if (single) {
          points.push({ x: 0, v: 0, lbl: fmtKpNode(0, metric) });
          ticks.push({ x: 0, label: '0' });
        }
        const off = single ? 1 : 0;
        chrono.forEach((t, i) => {
          const rev = Number(t.totalAmount || t.grandTotal) || 0;
          const prof = Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0;
          const v = metric === 'COUNT' ? (i + 1) : (metric === 'REVENUE' ? rev : prof);
          points.push({ x: i + off, v: v, lbl: fmtKpNode(v, metric) });
        });
        maxX = Math.max(1, points[points.length - 1].x);
        const step = Math.max(1, Math.floor(chrono.length / 4));
        chrono.forEach((t, i) => {
          if (i % step === 0 || i === chrono.length - 1) {
            const d = getKpDateObj(t);
            const hhmm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
            ticks.push({ x: i + off, label: hhmm });
          }
        });
      }
      const W = 440, H = 220, padL = 22, padR = 20, padT = 30, padB = 30, plotW = W - padL - padR, plotH = H - padT - padB;
      const rawMaxY = points.length ? Math.max.apply(null, points.map(p => p.v)) : 1;
      const maxY = rawMaxY <= 0 ? 1 : rawMaxY;
      const mapX = x => padL + (x / Math.max(0.1, maxX)) * plotW;
      const mapY = v => padT + plotH - (v / maxY) * plotH;
      let areaSvg = '', lineSvg = '', nodesSvg = '', ticksSvg = '';
      if (points.length >= 2) {
        const coords = points.map(p => mapX(p.x).toFixed(1) + ',' + mapY(p.v).toFixed(1)).join(' L ');
        const baseY = (padT + plotH).toFixed(1);
        areaSvg = '<path d="M ' + mapX(points[0].x).toFixed(1) + ',' + baseY + ' L ' + coords + ' L ' + mapX(points[points.length - 1].x).toFixed(1) + ',' + baseY + ' Z" fill="url(#kpGoldTpl)" />';
        lineSvg = '<path d="M ' + coords + '" fill="none" stroke="#EAB308" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />';
      }
      points.forEach(p => {
        const cx = mapX(p.x).toFixed(1), cy = mapY(p.v).toFixed(1);
        nodesSvg += '<circle cx="' + cx + '" cy="' + cy + '" r="5.5" fill="#FFFFFF" stroke="#EAB308" stroke-width="3" />';
        nodesSvg += '<text x="' + cx + '" y="' + (Number(cy) - 10).toFixed(1) + '" text-anchor="middle" font-size="11" font-weight="600" fill="#1E293B">' + p.lbl + '</text>';
      });
      ticks.forEach(tk => {
        ticksSvg += '<text x="' + mapX(tk.x).toFixed(1) + '" y="' + (H - 8) + '" text-anchor="middle" font-size="11" fill="#94A3B8">' + tk.label + '</text>';
      });
      const emptyNote = points.length === 0 ? '<text x="' + (W / 2) + '" y="' + (H / 2) + '" text-anchor="middle" font-size="12" fill="#94A3B8">Belum ada transaksi nota hari ini</text>' : '';
      return '<svg viewBox="0 0 ' + W + ' ' + H + '" class="w-full h-56 select-none overflow-visible">' +
        '<defs><linearGradient id="kpGoldTpl" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#EAB308" stop-opacity="0.48"/><stop offset="55%" stop-color="#F59E0B" stop-opacity="0.22"/><stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.02"/></linearGradient></defs>' +
        areaSvg + lineSvg +
        '<line x1="' + padL + '" y1="' + (padT + plotH) + '" x2="' + (W - padR) + '" y2="' + (padT + plotH) + '" stroke="#CBD5E1" stroke-width="1.2"/>' +
        '<line x1="' + (W - padR) + '" y1="' + (padT - 8) + '" x2="' + (W - padR) + '" y2="' + (padT + plotH) + '" stroke="#CBD5E1" stroke-width="1.2"/>' +
        nodesSvg + ticksSvg + emptyNote + '</svg>';
    }

    function renderKpExpenseView() {
      const scope = kpState.expenseScope || 'TODAY';
      const targetFormatted = formatKpDateShort(kpTargetDate || getLocalDateStr(new Date()));
      ['TODAY', 'THIS_MONTH', 'THIS_YEAR', 'ALL'].forEach(sc => {
        const btn = document.getElementById('kp-exp-scope-' + sc);
        if (!btn) return;
        btn.className = (sc === scope)
          ? 'px-3 py-1.5 rounded-lg text-xs font-bold bg-rose-600 text-white cursor-pointer'
          : 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-700 cursor-pointer';
      });
      const scopeSubtitles = {
        TODAY: 'Catatan pengeluaran tanggal ' + targetFormatted,
        THIS_MONTH: 'Rekap pengeluaran bulan berjalan',
        THIS_YEAR: 'Rekap pengeluaran tahun berjalan',
        ALL: 'Seluruh riwayat pengeluaran toko'
      };
      const subEl = document.getElementById('kp-exp-subtitle');
      if (subEl) subEl.textContent = scopeSubtitles[scope] || scopeSubtitles.TODAY;

      const scopedTrxs = filterKpItemsByScope(cachedTransactions, scope);
      const scopedExps = filterKpExpenses(scope).slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
      const visibleExps = scopedExps.filter(e => {
        if (!kpExpenseSearch) return true;
        return String(e.title || '').toLowerCase().includes(kpExpenseSearch) ||
          String(e.category || '').toLowerCase().includes(kpExpenseSearch) ||
          String(e.notes || '').toLowerCase().includes(kpExpenseSearch) ||
          String(e.cashierName || '').toLowerCase().includes(kpExpenseSearch);
      });

      const totalRev = scopedTrxs.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
      const totalProf = scopedTrxs.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
      const totalExp = scopedExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
      const netProf = totalProf - totalExp;

      const totalEl = document.getElementById('kp-exp-total-val');
      const revEl = document.getElementById('kp-exp-rev-val');
      const profEl = document.getElementById('kp-exp-profit-val');
      const netEl = document.getElementById('kp-exp-net-val');
      if (totalEl) totalEl.textContent = formatRupiah(totalExp);
      if (revEl) revEl.textContent = formatRupiah(totalRev);
      if (profEl) profEl.textContent = formatRupiah(totalProf);
      if (netEl) {
        netEl.textContent = formatRupiah(netProf);
        netEl.className = netProf >= 0
          ? 'text-base font-extrabold font-mono text-emerald-700 mt-0.5'
          : 'text-base font-extrabold font-mono text-rose-600 mt-0.5';
      }

      const listEl = document.getElementById('kp-expense-list');
      if (!listEl) return;
      if (!visibleExps.length) {
        const otherPeriodHint = (scope !== 'ALL' && !kpExpenseSearch && cachedExpenses.length > 0)
          ? ("<p class='mt-1.5 text-[11px] text-slate-500'>Ada <strong>" + cachedExpenses.length + "</strong> catatan pengeluaran tersimpan di periode lain.</p>" +
             "<button type='button' onclick=\"setKpExpenseScope('ALL')\" class='mt-2.5 px-3 py-1.5 rounded-lg bg-rose-600 text-white text-xs font-bold hover:bg-rose-700 cursor-pointer'>Lihat Semua Pengeluaran (" + cachedExpenses.length + ")</button>")
          : ("<button type='button' onclick='openModalCreateExpense()' class='mt-2.5 px-3 py-1.5 rounded-lg bg-rose-600 text-white text-xs font-bold hover:bg-rose-700 cursor-pointer'>➕ Catat Pengeluaran</button>");
        listEl.innerHTML = "<div class='p-8 text-center text-slate-400 text-xs'>" +
          "<div class='text-2xl mb-1'>💸</div>" +
          "<p class='font-semibold text-slate-600'>" + (kpExpenseSearch ? 'Pengeluaran tidak ditemukan.' : 'Belum ada catatan pengeluaran pada periode ini.') + "</p>" +
          otherPeriodHint +
        "</div>";
        return;
      }

      listEl.innerHTML = visibleExps.map(e => {
        const dObj = getKpDateObj(e);
        const hm = String(dObj.getHours()).padStart(2, '0') + ':' + String(dObj.getMinutes()).padStart(2, '0');
        const dateLbl = (extractDateStr(e) || getLocalDateStr(dObj)) + ' ' + hm;
        const noteTxt = cleanExpNote(e.notes);
        return "<div class='p-4 sm:px-5 flex items-center justify-between gap-3 hover:bg-rose-50/30'>" +
          "<div class='min-w-0 flex-1'>" +
            "<div class='flex items-center gap-2 flex-wrap'>" +
              "<span class='text-sm font-bold text-slate-900'>" + escHtml(e.title || 'Pengeluaran') + "</span>" +
              "<span class='px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 text-[10px] font-bold'>" + escHtml(e.category || 'Operasional') + "</span>" +
              "<span class='text-[11px] font-mono text-slate-400'>• " + escHtml(dateLbl) + "</span>" +
            "</div>" +
            "<div class='text-xs text-slate-500 mt-0.5'>👤 " + escHtml(e.cashierName || 'Admin') + (noteTxt ? (' • ' + escHtml(noteTxt)) : '') + "</div>" +
          "</div>" +
          "<span class='text-sm font-extrabold font-mono text-rose-600 shrink-0'>- " + formatRupiah(e.amount) + "</span>" +
        "</div>";
      }).join('');
    }

    function renderKpReport() {
      syncKpDateControls();
      const menuEl = document.getElementById('kp-menu-view');
      const detailEl = document.getElementById('kp-detail-view');
      const expenseEl = document.getElementById('kp-expense-view');
      if (!menuEl || !detailEl) return;
      if (kpState.screen === 'MENU') {
        menuEl.classList.remove('hidden');
        detailEl.classList.add('hidden');
        if (expenseEl) expenseEl.classList.add('hidden');
        return;
      }
      if (kpState.screen === 'EXPENSE') {
        menuEl.classList.add('hidden');
        detailEl.classList.add('hidden');
        if (expenseEl) expenseEl.classList.remove('hidden');
        renderKpExpenseView();
        return;
      }
      menuEl.classList.add('hidden');
      detailEl.classList.remove('hidden');
      if (expenseEl) expenseEl.classList.add('hidden');

      const todayStr = getLocalDateStr(new Date());
      const targetStr = kpTargetDate || todayStr;
      const isTargetToday = targetStr === todayStr;
      const targetFormatted = formatKpDateShort(targetStr);
      const isPerNota = kpState.menuOption === 'TODAY' && kpState.period === 'DAILY';

      const titles = { DAILY: 'Laporan Transaksi per Hari', MONTHLY: 'Laporan Transaksi per Bulan', YEARLY: 'Laporan Transaksi per Tahun' };
      const scopes = {
        TODAY: isTargetToday ? 'Laporan transaksi hari ini' : ('Catatan transaksi tanggal ' + targetFormatted),
        THIS_MONTH: 'Laporan transaksi bulan ini',
        THIS_YEAR: 'Laporan transaksi tahun ini',
        ALL: 'Laporan semua transaksi'
      };
      document.getElementById('kp-detail-title').textContent = titles[kpState.period];
      document.getElementById('kp-detail-subtitle').textContent = isPerNota
        ? (isTargetToday ? 'Laporan transaksi hari ini • Catatan per Nota & Pengeluaran' : ('Catatan transaksi tanggal ' + targetFormatted + ' • Per Nota & Pengeluaran'))
        : scopes[kpState.menuOption];
      const notaHeadingEl = document.getElementById('kp-today-nota-heading');
      const notaDescEl = document.getElementById('kp-today-nota-desc');
      if (notaHeadingEl) {
        notaHeadingEl.textContent = isTargetToday
          ? 'Catatan Transaksi & Pesanan per Nota Hari Ini'
          : ('Catatan Transaksi & Pesanan per Nota (' + targetFormatted + ')');
      }
      if (notaDescEl) {
        notaDescEl.textContent = isTargetToday
          ? 'Catatan pernota pesanan lengkap menu, pendapatan, keuntungan & pengeluaran'
          : ('Riwayat pesanan per nota & pengeluaran pada tanggal ' + targetFormatted);
      }

      ['DAILY', 'MONTHLY', 'YEARLY'].forEach(p => {
        const chk = document.getElementById('kp-check-' + p);
        if (chk) chk.classList.toggle('hidden', p !== kpState.period);
      });
      ['COUNT', 'REVENUE', 'PROFIT', 'EXPENSE'].forEach(m => {
        const btn = document.getElementById('kp-metric-' + m);
        if (!btn) return;
        const activeBg = m === 'EXPENSE' ? 'bg-rose-600 text-white' : 'bg-[#00796B] text-white';
        btn.className = (m === kpState.metric)
          ? ('py-2.5 px-1.5 text-[11px] sm:text-xs font-bold cursor-pointer text-center ' + activeBg)
          : 'py-2.5 px-1.5 text-[11px] sm:text-xs font-semibold bg-[#CFD4DC] text-white cursor-pointer text-center border-l border-white/40';
      });
      const filtered = filterKpTrxs();
      const filteredExps = filterKpExpenses();
      const listEl = document.getElementById('kp-records-list');
      const toolbarEl = document.getElementById('kp-today-nota-toolbar');
      const financeStripEl = document.getElementById('kp-per-nota-finance-strip');
      const perNotaExpBox = document.getElementById('kp-per-nota-expenses-box');
      if (toolbarEl) toolbarEl.classList.toggle('hidden', !isPerNota);
      if (financeStripEl) financeStripEl.classList.remove('hidden');
      if (perNotaExpBox) perNotaExpBox.classList.remove('hidden');

      function renderKpScopedExpensesBox(expsList, scopeLabel) {
        if (!perNotaExpBox) return;
        const sumExpBox = expsList.reduce((s, e) => s + (Number(e.amount) || 0), 0);
        if (expsList.length === 0) {
          perNotaExpBox.innerHTML = "<div class='px-4 sm:px-5 py-2.5 flex items-center justify-between text-xs text-slate-500'>" +
            "<span>💸 Laporan Pengeluaran (" + escHtml(scopeLabel) + ")</span>" +
            "<span class='font-mono font-bold text-slate-500'>0 catatan • Rp 0</span>" +
          "</div>";
          return;
        }
        perNotaExpBox.innerHTML = "<div class='px-4 sm:px-5 py-2.5 bg-rose-100/70 border-b border-rose-200 flex items-center justify-between gap-2'>" +
          "<span class='text-xs font-extrabold text-rose-800'>💸 Laporan Pengeluaran (" + expsList.length + " Catatan • RTDB)</span>" +
          "<span class='text-xs font-mono font-extrabold text-rose-700'>- " + formatRupiah(sumExpBox) + "</span>" +
        "</div>" +
        "<div class='divide-y divide-rose-200/70 max-h-52 overflow-y-auto bg-white'>" +
          expsList.map(e => {
            const dObj = getKpDateObj(e);
            const hm = String(dObj.getHours()).padStart(2, '0') + ':' + String(dObj.getMinutes()).padStart(2, '0');
            const dt = extractDateStr(e) || getLocalDateStr(dObj);
            return "<div class='px-4 sm:px-5 py-2 flex items-center justify-between gap-2'>" +
              "<div class='min-w-0 flex-1'>" +
                "<div class='text-xs font-bold text-slate-900 truncate'>" + escHtml(e.title || 'Pengeluaran') + "</div>" +
                "<div class='text-[11px] text-slate-500 truncate'>" + escHtml(e.category || 'Operasional') + " • " + escHtml(dt) + " " + hm + " • " + escHtml(e.cashierName || e.createdBy || 'Admin') + (cleanExpNote(e.notes) ? (' • ' + escHtml(cleanExpNote(e.notes))) : '') + "</div>" +
              "</div>" +
              "<span class='text-xs font-extrabold font-mono text-rose-600 shrink-0'>- " + formatRupiah(e.amount) + "</span>" +
            "</div>";
          }).join('') +
        "</div>";
      }

      if (isPerNota) {
        const sortedAllNotas = filtered.slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
        const sortedAllExps = filteredExps.slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
        const sortedNotas = sortedAllNotas.filter(t => {
          if (!kpTodayNotaSearch) return true;
          const invStr = String(t.invoiceNumber || t.id || '').toLowerCase();
          const ordStr = formatOrderLabel(t.invoiceNumber || t.id || '').toLowerCase();
          const custStr = String(t.customerName || '').toLowerCase();
          const cashStr = String(t.cashierName || '').toLowerCase();
          const sumStr = String(t.itemsSummary || '').toLowerCase();
          const itemStr = normalizeKpItems(t.items).map(it => (it.productName || it.name || '') + ' ' + (it.notes || '')).join(' ').toLowerCase();
          return invStr.includes(kpTodayNotaSearch) || ordStr.includes(kpTodayNotaSearch) ||
            custStr.includes(kpTodayNotaSearch) || cashStr.includes(kpTodayNotaSearch) ||
            sumStr.includes(kpTodayNotaSearch) || itemStr.includes(kpTodayNotaSearch);
        });
        const matchedExps = sortedAllExps.filter(e => {
          if (!kpTodayNotaSearch) return true;
          return String(e.title || '').toLowerCase().includes(kpTodayNotaSearch) ||
            String(e.category || '').toLowerCase().includes(kpTodayNotaSearch) ||
            String(e.notes || '').toLowerCase().includes(kpTodayNotaSearch) ||
            String(e.cashierName || e.createdBy || '').toLowerCase().includes(kpTodayNotaSearch);
        });
        const totalOrderedItems = sortedNotas.reduce((s, t) => {
          const itemArr = normalizeKpItems(t.items);
          const cnt = itemArr.length > 0
            ? itemArr.reduce((acc, it) => acc + (Number(it.quantity || it.qty) || 1), 0)
            : (Number(t.totalItemsCount) || 1);
          return s + cnt;
        }, 0);
        const countBadge = document.getElementById('kp-today-nota-count');
        const itemsBadge = document.getElementById('kp-today-items-count');
        if (countBadge) countBadge.textContent = sortedNotas.length + ' Nota';
        if (itemsBadge) itemsBadge.textContent = totalOrderedItems + ' Item';

        const totalRevPn = sortedAllNotas.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
        const totalProfPn = sortedAllNotas.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
        const totalExpPn = sortedAllExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
        const netProfPn = totalProfPn - totalExpPn;
        const pnRevEl = document.getElementById('kp-pn-rev');
        const pnProfEl = document.getElementById('kp-pn-profit');
        const pnExpEl = document.getElementById('kp-pn-exp');
        const pnNetEl = document.getElementById('kp-pn-net');
        if (pnRevEl) pnRevEl.textContent = formatRupiah(totalRevPn);
        if (pnProfEl) pnProfEl.textContent = formatRupiah(totalProfPn);
        if (pnExpEl) pnExpEl.textContent = formatRupiah(totalExpPn);
        if (pnNetEl) {
          pnNetEl.textContent = formatRupiah(netProfPn);
          pnNetEl.className = netProfPn >= 0 ? 'font-extrabold font-mono text-emerald-700' : 'font-extrabold font-mono text-rose-600';
        }

        renderKpScopedExpensesBox(matchedExps, targetFormatted);

        document.getElementById('kp-chart-container').innerHTML = buildKpPerNotaSvg(sortedAllNotas, kpState.metric, sortedAllExps);
        if (!sortedNotas.length) {
          listEl.innerHTML = "<div class='p-8 text-center text-slate-400 text-xs'>" +
            "<div class='text-2xl mb-1'>🧾</div>" +
            "<p class='font-semibold text-slate-600'>" + (kpTodayNotaSearch ? 'Nota pesanan tidak ditemukan.' : (isTargetToday ? 'Belum ada transaksi nota hari ini.' : ('Belum ada transaksi nota pada ' + targetFormatted + '.'))) + "</p>" +
            "<p class='mt-1'>" + (isTargetToday ? 'Setiap pesanan masuk hari ini otomatis dicatat lengkap per nota di sini.' : 'Pilih tanggal lain di atas untuk melihat arsip transaksi sebelumnya.') + "</p>" +
          "</div>";
          return;
        }
        const headerBanner = "<div class='px-4 sm:px-5 py-2 bg-slate-50 border-b border-slate-200 flex items-center justify-between text-[11px] font-bold text-[#00796B]'>" +
          "<span>" + (isTargetToday ? ('Catatan Transaksi per Nota Hari Ini • ' + targetFormatted) : ('Catatan Transaksi per Nota (' + targetFormatted + ')')) + "</span>" +
          "<span>" + sortedNotas.length + " Nota • " + totalOrderedItems + " Item Pesanan</span>" +
        "</div>";
        listEl.innerHTML = headerBanner + sortedNotas.map(t => {
          const invKey = String(t.invoiceNumber || t.id || '-');
          const orderLabel = formatOrderLabel(invKey);
          const d = getKpDateObj(t);
          const hhmm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
          const dOnly = extractDateStr(t) || getLocalDateStr(d);
          const dayDateLbl = formatKpDateShort(dOnly) || ((getKpDayName(d) ? getKpDayName(d) + ', ' : '') + dOnly);
          const pm = normalizePm(t.paymentMethod);
          const cust = t.customerName || 'Pelanggan Umum';
          const cashier = t.cashierName || 'Kasir';
          const orderTypeLbl = t.orderType === 'TAKE_AWAY' ? 'Di Bungkus' : 'Makan di Tempat';
          const tableLbl = t.tableNumber ? ' • Meja ' + t.tableNumber : '';
          const amt = Number(t.totalAmount || t.grandTotal) || 0;
          const prof = Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0;
          const safeKey = invKey.replace(/'/g, "\\'");
          const itemArr = normalizeKpItems(t.items);

          let pesananHtml = '';
          if (itemArr.length > 0) {
            pesananHtml = "<div class='mt-2 space-y-1 bg-slate-50 border border-slate-200/80 rounded-xl p-2.5'>" +
              "<div class='text-[10px] font-bold uppercase tracking-wider text-[#00796B]'>Rincian Pesanan per Nota:</div>" +
              itemArr.map(it => {
                const pName = escHtml(it.productName || it.name || 'Item');
                const qty = Number(it.quantity || it.qty) || 1;
                const sub = Number(it.subtotal != null ? it.subtotal : ((Number(it.unitPrice || it.price) || 0) * qty)) || 0;
                const noteTxt = it.notes ? " <span class='text-amber-700'>(" + escHtml(it.notes) + ")</span>" : '';
                return "<div class='flex items-center justify-between gap-2 text-xs text-slate-800'>" +
                  "<span class='min-w-0 truncate'><span class='font-extrabold text-[#00796B]'>" + qty + "x</span> " + pName + noteTxt + "</span>" +
                  "<span class='font-mono font-bold text-slate-700 shrink-0'>" + formatRupiah(sub) + "</span>" +
                "</div>";
              }).join('') +
            "</div>";
          } else {
            pesananHtml = "<div class='mt-2 bg-slate-50 border border-slate-200/80 rounded-xl px-2.5 py-2 text-xs text-slate-700'>" +
              "<span class='font-bold text-[#00796B]'>Pesanan:</span> " + escHtml(t.itemsSummary || ((t.totalItemsCount || 1) + ' item pesanan')) +
            "</div>";
          }
          const orderNotesHtml = t.notes
            ? "<div class='mt-1.5 text-[11px] text-amber-700 bg-amber-50 border border-amber-200/70 rounded-lg px-2 py-1'>📝 Catatan: " + escHtml(t.notes) + "</div>"
            : '';

          return "<div onclick=\"openKpSingleNotaAlert('" + safeKey + "')\" class='p-4 sm:px-5 flex items-start justify-between gap-3 hover:bg-teal-50/30 cursor-pointer'>" +
            "<div class='flex-1 min-w-0'>" +
              "<div class='flex items-center gap-1.5 flex-wrap'>" +
                "<span class='text-sm font-extrabold text-slate-900 font-mono'>" + escHtml(orderLabel) + "</span>" +
                "<span class='px-2 py-0.5 rounded-full bg-teal-50 text-[#00796B] border border-teal-200 text-[10px] font-bold'>" + escHtml(dayDateLbl) + " • " + hhmm + " • " + escHtml(pm) + "</span>" +
                "<span class='text-[11px] text-slate-600 font-medium'>👤 " + escHtml(cust) + " (" + escHtml(orderTypeLbl) + escHtml(tableLbl) + ")</span>" +
                "<span class='text-[11px] text-slate-400'>• Kasir: " + escHtml(cashier) + "</span>" +
              "</div>" +
              pesananHtml +
              orderNotesHtml +
              "<div class='mt-2.5 flex items-center gap-4 flex-wrap'>" +
                "<div><span class='text-[10px] italic text-slate-500'>Pendapatan: </span><span class='text-xs font-extrabold text-slate-900 font-mono'>" + formatRupiah(amt) + "</span></div>" +
                "<div><span class='text-[10px] italic text-slate-500'>Keuntungan: </span><span class='text-xs font-extrabold text-[#00796B] font-mono'>" + formatRupiah(prof) + "</span></div>" +
              "</div>" +
            "</div>" +
            "<div class='flex flex-col items-center gap-1 shrink-0 pt-0.5'>" +
              "<button type='button' onclick=\"event.stopPropagation();deleteKpSingleNota('" + safeKey + "')\" title='Hapus " + escHtml(orderLabel) + "' class='w-8 h-8 rounded-lg hover:bg-rose-50 text-rose-500 font-bold cursor-pointer'>🗑️</button>" +
              "<button type='button' onclick=\"event.stopPropagation();openKpSingleNotaAlert('" + safeKey + "')\" title='Lihat " + escHtml(orderLabel) + "' class='w-8 h-8 rounded-lg hover:bg-teal-50 text-[#00796B] font-bold cursor-pointer'>→</button>" +
            "</div>" +
          "</div>";
        }).join('');
        return;
      }

      const sortedScopeExps = filteredExps.slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
      const scopeRev = filtered.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
      const scopeProf = filtered.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
      const scopeExp = sortedScopeExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
      const scopeNet = scopeProf - scopeExp;
      const pnRevEl = document.getElementById('kp-pn-rev');
      const pnProfEl = document.getElementById('kp-pn-profit');
      const pnExpEl = document.getElementById('kp-pn-exp');
      const pnNetEl = document.getElementById('kp-pn-net');
      if (pnRevEl) pnRevEl.textContent = formatRupiah(scopeRev);
      if (pnProfEl) pnProfEl.textContent = formatRupiah(scopeProf);
      if (pnExpEl) pnExpEl.textContent = formatRupiah(scopeExp);
      if (pnNetEl) {
        pnNetEl.textContent = formatRupiah(scopeNet);
        pnNetEl.className = scopeNet >= 0 ? 'font-extrabold font-mono text-emerald-700' : 'font-extrabold font-mono text-rose-600';
      }
      renderKpScopedExpensesBox(sortedScopeExps, kpState.menuOption || targetFormatted);

      const records = aggregateKpRecords(filtered, kpState.period, filteredExps);
      document.getElementById('kp-chart-container').innerHTML = buildKpSvg(records, kpState.period, kpState.metric);
      if (!records.length) {
        listEl.innerHTML = "<div class='p-8 text-center text-slate-400 text-xs'>Belum ada record transaksi atau pengeluaran untuk kategori ini.</div>";
        return;
      }
      listEl.innerHTML = records.map(r => {
        const netCls = r.netProfit >= 0 ? 'text-emerald-700' : 'text-rose-600';
        return "<div class='p-4 sm:px-5 flex items-center justify-between gap-3 hover:bg-slate-50'>" +
          "<div class='min-w-[105px]'><div class='flex items-center gap-2'><span class='text-2xl font-extrabold text-slate-900 leading-none'>" + r.primaryLabel + "</span><span class='text-[11px] text-slate-700 font-medium leading-tight'>" + r.secondaryLabel + "</span></div><div class='text-xs text-slate-500 mt-1'>" + r.transactions.length + " transaksi • " + r.expenses.length + " pengeluaran</div></div>" +
          "<div class='flex-1 min-w-0 grid grid-cols-2 gap-x-3 gap-y-1'>" +
            "<div><div class='text-[10px] italic text-slate-500'>Pendapatan</div><div class='text-xs sm:text-sm font-extrabold text-slate-900 font-mono'>" + formatRupiah(r.revenue) + "</div></div>" +
            "<div><div class='text-[10px] italic text-slate-500'>Keuntungan</div><div class='text-xs sm:text-sm font-extrabold text-[#00796B] font-mono'>" + formatRupiah(r.profit) + "</div></div>" +
            "<div><div class='text-[10px] italic text-rose-600'>Pengeluaran</div><div class='text-xs sm:text-sm font-extrabold text-rose-600 font-mono'>- " + formatRupiah(r.totalExpense) + "</div></div>" +
            "<div><div class='text-[10px] italic text-slate-500'>Laba Bersih</div><div class='text-xs sm:text-sm font-extrabold font-mono " + netCls + "'>" + formatRupiah(r.netProfit) + "</div></div>" +
          "</div>" +
          "<div class='flex flex-col items-center gap-1 shrink-0'>" +
            "<button type='button' onclick=\"deleteKpPeriod('" + r.key + "','" + r.fullTitle + "')\" title='Hapus' class='w-8 h-8 rounded-lg hover:bg-rose-50 text-rose-500 font-bold cursor-pointer'>🗑️</button>" +
            "<button type='button' onclick=\"openKpPeriodAlert('" + r.key + "')\" title='Detail' class='w-8 h-8 rounded-lg hover:bg-teal-50 text-[#00796B] font-bold cursor-pointer'>→</button>" +
          "</div>" +
        "</div>";
      }).join('');
    }

    function selectKpMenuOption(opt) {
      kpState.menuOption = opt;
      kpState.period = (opt === 'THIS_YEAR') ? 'MONTHLY' : 'DAILY';
      kpState.screen = 'DETAIL';
      renderKpReport();
    }
    function selectKpExpenseReport() {
      kpState.screen = 'EXPENSE';
      renderKpReport();
    }
    function backToKpMenu() {
      kpState.screen = 'MENU';
      const ov = document.getElementById('kp-overflow-menu');
      if (ov) ov.classList.add('hidden');
      renderKpReport();
    }
    function toggleKpOverflowMenu(e) {
      if (e) e.stopPropagation();
      document.getElementById('kp-overflow-menu').classList.toggle('hidden');
    }
    function setKpPeriod(p) {
      kpState.period = p;
      document.getElementById('kp-overflow-menu').classList.add('hidden');
      renderKpReport();
    }
    function setKpMetric(m) { kpState.metric = m; renderKpReport(); }
    function shareKpExpenseReport() {
      const scope = kpState.expenseScope || 'TODAY';
      const scopedTrxs = filterKpItemsByScope(cachedTransactions, scope);
      const scopedExps = filterKpExpenses(scope).slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
      const rev = scopedTrxs.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
      const prof = scopedTrxs.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
      const totalExp = scopedExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
      const lines = scopedExps.slice(0, 25).map(e => '- ' + (e.title || 'Pengeluaran') + ' [' + (e.category || 'Operasional') + ']: -' + formatRupiah(e.amount)).join('\n');
      const txt = 'Laporan Pengeluaran (' + scope + ')\nTotal Pengeluaran: ' + formatRupiah(totalExp) + '\nPendapatan: ' + formatRupiah(rev) + '\nKeuntungan Kotor: ' + formatRupiah(prof) + '\nLaba Bersih: ' + formatRupiah(prof - totalExp) + '\n\n' + (lines || 'Belum ada pengeluaran.');
      if (navigator.share) navigator.share({ title: 'Laporan Pengeluaran', text: txt }).catch(() => {});
      else if (navigator.clipboard) { navigator.clipboard.writeText(txt); alert('Ringkasan laporan pengeluaran disalin!'); }
    }
    function shareKpReport() {
      const filtered = filterKpTrxs();
      const filteredExps = filterKpExpenses();
      let txt = '';
      if (kpState.menuOption === 'TODAY' && kpState.period === 'DAILY') {
        const targetStr = kpTargetDate || getLocalDateStr(new Date());
        const targetFormatted = formatKpDateShort(targetStr);
        const sortedNotas = filtered.slice().sort((a, b) => getKpDateObj(b).getTime() - getKpDateObj(a).getTime());
        const rev = sortedNotas.reduce((s, t) => s + (Number(t.totalAmount || t.grandTotal) || 0), 0);
        const prof = sortedNotas.reduce((s, t) => s + (Number(t.profitAmount != null ? t.profitAmount : t.profit) || 0), 0);
        const totalExp = filteredExps.reduce((s, e) => s + (Number(e.amount) || 0), 0);
        const lines = sortedNotas.slice(0, 25).map(t => {
          const d = getKpDateObj(t);
          const hhmm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
          const dayStr = getKpDayName(d);
          const itemsArr = normalizeKpItems(t.items);
          const itemsText = itemsArr.length > 0
            ? itemsArr.map(it => (it.productName || it.name || 'Item') + ' x' + (it.quantity || it.qty || 1)).join(', ')
            : (t.itemsSummary || '-');
          return '- ' + formatOrderLabel(t.invoiceNumber || t.id) + ' (' + (dayStr ? dayStr + ', ' : '') + hhmm + ' - ' + (t.paymentMethod || 'CASH') + '): ' + formatRupiah(t.totalAmount || t.grandTotal) + ' | Laba ' + formatRupiah(t.profitAmount != null ? t.profitAmount : t.profit) + ' [' + itemsText + ']';
        }).join('\n');
        const expLines = filteredExps.slice(0, 15).map(e => '- ' + (e.title || 'Pengeluaran') + ' [' + (e.category || 'Operasional') + ']: -' + formatRupiah(e.amount)).join('\n');
        txt = 'Laporan Transaksi Tanggal ' + targetFormatted + ' (PER NOTA)\nTotal Nota: ' + sortedNotas.length + '\nPendapatan: ' + formatRupiah(rev) + '\nKeuntungan Kotor: ' + formatRupiah(prof) + '\nPengeluaran: ' + formatRupiah(totalExp) + '\nLaba Bersih: ' + formatRupiah(prof - totalExp) + '\n\n' + (lines || 'Belum ada transaksi.') + (expLines ? ('\n\nPENGELUARAN:\n' + expLines) : '');
      } else {
        const records = aggregateKpRecords(filtered, kpState.period, filteredExps);
        const rev = records.reduce((s, r) => s + r.revenue, 0);
        const prof = records.reduce((s, r) => s + r.profit, 0);
        const totalExp = records.reduce((s, r) => s + r.totalExpense, 0);
        txt = 'Laporan Transaksi (' + kpState.period + ')\nPendapatan: ' + formatRupiah(rev) + '\nKeuntungan Kotor: ' + formatRupiah(prof) + '\nPengeluaran: ' + formatRupiah(totalExp) + '\nLaba Bersih: ' + formatRupiah(prof - totalExp);
      }
      if (navigator.share) navigator.share({ title: 'Laporan Transaksi', text: txt }).catch(() => {});
      else if (navigator.clipboard) { navigator.clipboard.writeText(txt); alert('Ringkasan laporan disalin!'); }
    }
    async function deleteKpSingleNota(invKey) {
      const target = cachedTransactions.find(t => String(t.invoiceNumber || t.id || '-') === invKey);
      if (!target) return;
      const amt = Number(target.totalAmount || target.grandTotal) || 0;
      if (!confirm('Hapus ' + formatOrderLabel(invKey) + ' (' + formatRupiah(amt) + ')?')) return;
      const docKey = encodeURIComponent(target.invoiceNumber || target.id);
      const dStr = extractDateStr(target);
      await Promise.all([
        fetch(DB_URL + '/transactions/' + docKey + '.json', { method: 'DELETE' }),
        dStr ? fetch(DB_URL + '/reports/daily_transactions/' + dStr + '/' + docKey + '.json', { method: 'DELETE' }) : Promise.resolve()
      ]);
      loadData();
    }
    function openKpSingleNotaAlert(invKey) {
      const target = cachedTransactions.find(t => String(t.invoiceNumber || t.id || '-') === invKey);
      if (!target) return;
      const d = getKpDateObj(target);
      const hhmm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
      const dOnly = extractDateStr(target) || getLocalDateStr(d);
      const dayDateLbl = formatKpDateShort(dOnly) || dOnly;
      const amt = Number(target.totalAmount || target.grandTotal) || 0;
      const prof = Number(target.profitAmount != null ? target.profitAmount : target.profit) || 0;
      const itemsArr = normalizeKpItems(target.items);
      const itemsText = itemsArr.length
        ? itemsArr.map(it => '- ' + (it.productName || it.name || 'Item') + ' x' + (it.quantity || it.qty || 1) + ' (' + formatRupiah(it.subtotal || it.total || 0) + ')').join('\n')
        : (target.itemsSummary || ('Total item: ' + (target.totalItemsCount || 1)));
      alert('Detail ' + formatOrderLabel(invKey) + '\nHari/Tanggal: ' + dayDateLbl + ' • ' + hhmm + '\nMetode: ' + (target.paymentMethod || 'CASH') + '\nPelanggan: ' + (target.customerName || 'Umum') + '\nPendapatan: ' + formatRupiah(amt) + '\nKeuntungan: ' + formatRupiah(prof) + '\n\nItem:\n' + itemsText);
    }
    async function deleteKpPeriod(key, title) {
      const records = aggregateKpRecords(filterKpTrxs(), kpState.period, filterKpExpenses());
      const target = records.find(r => r.key === key);
      if (!target || !confirm('Hapus ' + target.transactions.length + ' transaksi & ' + target.expenses.length + ' pengeluaran pada ' + title + '?')) return;
      await Promise.all([
        ...target.transactions.flatMap(t => {
          const docKey = encodeURIComponent(t.invoiceNumber || t.id);
          const dStr = extractDateStr(t);
          return [
            fetch(DB_URL + '/transactions/' + docKey + '.json', { method: 'DELETE' }),
            dStr ? fetch(DB_URL + '/reports/daily_transactions/' + dStr + '/' + docKey + '.json', { method: 'DELETE' }) : Promise.resolve()
          ];
        }),
        ...target.expenses.flatMap(e => {
          const expKey = encodeURIComponent(e.id);
          const dStr = extractDateStr(e);
          const base = activeRtdbBase || DB_URL;
          return [
            fetch(base + '/expenses/' + expKey + '.json', { method: 'DELETE' }),
            fetch(base + '/cash_flows/CF-' + expKey + '.json', { method: 'DELETE' }),
            fetch(base + '/cash_flows/' + expKey + '.json', { method: 'DELETE' }),
            fetch(base + '/realtime_stats/expenses/' + expKey + '.json', { method: 'DELETE' }),
            dStr ? fetch(base + '/reports/daily_expenses/' + dStr + '/' + expKey + '.json', { method: 'DELETE' }) : Promise.resolve(),
            dStr ? fetch(base + '/realtime_stats/expenses_by_date/' + dStr + '/' + expKey + '.json', { method: 'DELETE' }) : Promise.resolve()
          ];
        })
      ]);
      loadData();
    }
    function openKpPeriodAlert(key) {
      const records = aggregateKpRecords(filterKpTrxs(), kpState.period, filterKpExpenses());
      const rec = records.find(r => r.key === key);
      if (!rec) return;
      const lines = rec.transactions.slice(0, 15).map(t => formatOrderLabel(t.invoiceNumber || t.id) + ' - ' + formatRupiah(t.totalAmount || t.grandTotal)).join('\n');
      const expLines = rec.expenses.slice(0, 15).map(e => '- ' + (e.title || 'Pengeluaran') + ' (' + (e.category || 'Operasional') + '): -' + formatRupiah(e.amount)).join('\n');
      alert('Rincian ' + rec.fullTitle + ' (' + rec.transactions.length + ' transaksi • ' + rec.expenses.length + ' pengeluaran)\nPendapatan: ' + formatRupiah(rec.revenue) + '\nKeuntungan Kotor: ' + formatRupiah(rec.profit) + '\nPengeluaran: ' + formatRupiah(rec.totalExpense) + '\nLaba Bersih: ' + formatRupiah(rec.netProfit) + '\n\nTRANSAKSI:\n' + (lines || '-') + (expLines ? ('\n\nPENGELUARAN:\n' + expLines) : ''));
    }
    document.addEventListener('click', () => {
      const m = document.getElementById('kp-overflow-menu');
      if (m) m.classList.add('hidden');
    });

    const dailyNotaSearchEl = document.getElementById('input-search-daily-nota');
    if (dailyNotaSearchEl) {
      dailyNotaSearchEl.addEventListener('input', e => {
        dailyNotaSearchKeyword = (e.target.value || '').trim().toLowerCase();
        applyFilterAndRender();
      });
    }
    document.querySelectorAll('.daily-nota-pay-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        dailyNotaPayFilter = btn.getAttribute('data-pay') || 'ALL';
        document.querySelectorAll('.daily-nota-pay-btn').forEach(b => {
          const active = (b.getAttribute('data-pay') || 'ALL') === dailyNotaPayFilter;
          b.className = active
            ? 'daily-nota-pay-btn px-2.5 py-1 text-xs font-bold rounded-lg bg-[#00796B] text-white cursor-pointer'
            : 'daily-nota-pay-btn px-2.5 py-1 text-xs font-semibold rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer';
        });
        applyFilterAndRender();
      });
    });

    const searchInput = document.getElementById('search-date');
    if (searchInput) searchInput.value = activeDateFilter;
    try {
      const kpUrlParams = new URLSearchParams(window.location.search);
      const kpParamDate = (kpUrlParams.get('date') || '').trim();
      if (/^\d{4}-\d{2}-\d{2}$/.test(kpParamDate)) {
        kpTargetDate = kpParamDate;
        kpState.expenseScope = 'TODAY';
      }
      const kpParamView = (kpUrlParams.get('view') || '').trim();
      if (kpParamView === 'trx-expenses' || kpParamView === 'expenses') {
        selectKpExpenseReport();
        setTimeout(() => {
          const card = document.getElementById('kp-report-card');
          if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 150);
      } else if (kpParamView === 'transaksi') {
        renderKpReport();
        setTimeout(() => {
          const card = document.getElementById('kp-report-card');
          if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 150);
      }
    } catch (_) {}
    loadData();
    setInterval(loadData, 6000);
    setInterval(() => {
      const curDate = getLocalDateStr(new Date());
      if (curDate !== lastKnownDate) {
        const prevDate = lastKnownDate;
        lastKnownDate = curDate;
        if (kpTargetDate === prevDate) {
          kpTargetDate = curDate;
        }
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
    },
    "expenses": {
      ".read": true,
      ".write": true,
      ".indexOn": ["id", "date", "timestamp", "timestampMillis", "category", "createdBy"]
    },
    "reports": {
      ".read": true,
      ".write": true,
      "daily_transactions": {
        ".read": true,
        ".write": true,
        ".indexOn": ["date", "timestamp", "paymentMethod"]
      },
      "daily_expenses": {
        ".read": true,
        ".write": true,
        ".indexOn": ["id", "date", "timestamp", "timestampMillis", "category", "createdBy"]
      },
      "daily": {
        ".read": true,
        ".write": true,
        ".indexOn": ["date", "lastUpdated"]
      }
    },
    "cash_flows": {
      ".read": true,
      ".write": true,
      ".indexOn": ["date", "timestamp", "type"]
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
      "totalExpenses": 45000,
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
  },

  "// TABEL 6: CATATAN PENGELUARAN (EXPENSES & DAILY EXPENSES)": "expenses",
  "expenses": {
    "EXP-1": {
      "id": "EXP-1",
      "localId": 1,
      "title": "Beli Es Batu & Cup",
      "amount": 45000,
      "category": "Bahan/Kemasan",
      "notes": "2 karung es + 1 pack cup",
      "createdBy": "Siti Kasir",
      "cashierName": "Siti Kasir",
      "date": "2026-11-03",
      "timestamp": "2026-11-03T10:15:00Z",
      "timestampMillis": 1793700900000,
      "syncedAt": "2026-11-03T10:15:00Z",
      "storeId": "foodrock-main"
    }
  },

  "// TABEL 7: INDEKS LAPORAN PENGELUARAN HARIAN": "reports/daily_expenses",
  "reports": {
    "daily_expenses": {
      "2026-11-03": {
        "EXP-1": {
          "id": "EXP-1",
          "localId": 1,
          "title": "Beli Es Batu & Cup",
          "amount": 45000,
          "category": "Bahan/Kemasan",
          "notes": "2 karung es + 1 pack cup",
          "createdBy": "Siti Kasir",
          "date": "2026-11-03",
          "timestamp": "2026-11-03T10:15:00Z"
        }
      }
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

