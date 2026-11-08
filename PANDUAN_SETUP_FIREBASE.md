# Panduan Lengkap Setup Firebase & Portal Owner Kasir POS

Dokumen ini berisi panduan terperinci langkah demi langkah untuk mengonfigurasi **Firebase Backend**, **Firestore Database**, **Aturan Keamanan (Rules)**, serta men-deploy **Portal Web Laporan Owner** ke **Firebase Hosting**.

---

## 📁 1. Struktur Berkas Terkait Firebase & Portal Owner

```
.
├── firebase.json                # Konfigurasi deploy Firebase Hosting, Firestore Rules & Emulator
├── firestore.rules              # Aturan keamanan database Firestore (Zero-Trust Security Rules)
├── firestore.indexes.json       # Indeks database Firestore
├── PANDUAN_SETUP_FIREBASE.md    # Panduan ini
└── web-dashboard/               # Folder Portal Web Owner (Single Page App)
    ├── index.html               # Halaman utama Portal Owner (Tailwind CSS, Chart.js, Firebase SDK)
    ├── server.js                # Server Node.js lokal untuk pengujian preview
    └── package.json             # Script npm untuk start dan deploy
```

---

## 🚀 2. Langkah Setup di Firebase Console

### Langkah 2.1: Buat Project Firebase
1. Buka [Firebase Console](https://console.firebase.google.com/) dan login dengan akun Google Anda.
2. Klik **"Add project"** (Tambah project).
3. Masukkan nama project Anda (misal: `tokopos-laporan` atau gunakan project ID Anda).
4. (Opsional) Google Analytics dapat diaktifkan atau dinonaktifkan sesuai kebutuhan.
5. Klik **"Create project"** dan tunggu hingga selesai.

### Langkah 2.2: Buat Database Cloud Firestore
1. Pada menu sebelah kiri Firebase Console, klik **Build** > **Firestore Database**.
2. Klik **"Create database"**.
3. Pilih **Database ID**:
   * Rekomendasi: Gunakan default `(default)` ATAU custom ID Anda.
4. Pilih lokasi server database (misal: `asia-southeast1` untuk Singapura / Indonesia agar latensi rendah).
5. Pilih **"Start in test mode"** atau **"Start in production mode"** (kita akan menimpa rules dengan `firestore.rules` resmi).
6. Klik **"Enable"**.

### Langkah 2.3: Aktifkan Firebase Authentication (Google Sign-In)
1. Pada menu kiri, klik **Build** > **Authentication**.
2. Klik tombol **"Get Started"**.
3. Di tab **Sign-in method**, pilih penyedia **Google**.
4. Aktifkan toggle **Enable**, pilih email dukungan proyek, lalu klik **Save**.
5. Catat **Web Client ID** yang digenerate oleh Firebase untuk keperluan autentikasi Android.

### Langkah 2.4: Daftarkan Aplikasi Android ke Firebase
1. Di halaman **Project Overview** (klik ikon gerigi ⚙️ > **Project settings**).
2. Di bagian **Your apps**, klik ikon **Android**.
3. Masukkan nama paket:
   * **Android package name:** `com.example` (sesuaikan dengan `namespace` aplikasi).
   * **App nickname:** `Kasir POS`.
   * **Debug signing certificate SHA-1:** Masukkan fingerprint SHA-1 dari keystore Anda.
4. Klik **Register app**.
5. Unduh file `google-services.json` dan letakkan di dalam folder `app/` di proyek Android.

---

## 🔒 3. Source Code: Aturan Keamanan Database (`firestore.rules`)

Pastikan file `firestore.rules` di root proyek berisi kode berikut:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // Validasi skema transaksi kasir POS
    function isValidTransaction(data) {
      return data.id is string
        && data.orderNumber is string
        && data.totalAmount is number
        && data.totalAmount >= 0
        && data.paymentMethod is string
        && data.cashierName is string
        && data.date is string
        && data.storeId is string
        && data.userId is string
        && data.userId == request.auth.uid;
    }

    // Validasi skema rekap tutup kasir (shift)
    function isValidShiftReport(data) {
      return data.id is string
        && data.cashierName is string
        && data.totalTransactions is number
        && data.totalTransactions >= 0
        && data.totalRevenue is number
        && data.totalRevenue >= 0
        && data.date is string
        && data.storeId is string
        && data.userId is string
        && data.userId == request.auth.uid;
    }

    // Validasi profil toko
    function isValidStoreProfile(data) {
      return data.storeName is string
        && data.address is string
        && data.phone is string
        && data.userId is string
        && data.userId == request.auth.uid;
    }

    // Koleksi Transaksi Penjualan
    match /transactions/{transactionId} {
      allow read: if true; // Diizinkan untuk portal web owner dan audit real-time
      allow create: if isAuthenticated() && isValidTransaction(request.resource.data);
      allow update: if isAuthenticated() && isValidTransaction(request.resource.data) && resource.data.userId == request.auth.uid;
      allow delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }

    // Koleksi Laporan Tutup Kasir / Shift
    match /shift_reports/{reportId} {
      allow read: if true; // Diizinkan untuk portal web owner
      allow create: if isAuthenticated() && isValidShiftReport(request.resource.data);
      allow update: if isAuthenticated() && isValidShiftReport(request.resource.data) && resource.data.userId == request.auth.uid;
      allow delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }

    // Koleksi Profil & Identitas Toko
    match /store_profile/{storeId} {
      allow read: if true;
      allow write: if isAuthenticated() && isValidStoreProfile(request.resource.data);
    }

    // Koleksi Katalog Produk (opsional)
    match /products/{productId} {
      allow read: if true;
      allow write: if isAuthenticated();
    }
  }
}
```

---

## ⚙️ 4. Source Code: Konfigurasi Firebase (`firebase.json`)

File `firebase.json` mengarahkan hosting ke folder `web-dashboard`:

```json
{
  "hosting": {
    "public": "web-dashboard",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ],
    "headers": [
      {
        "source": "**",
        "headers": [
          { "key": "Cache-Control", "value": "max-age=300" },
          { "key": "X-Content-Type-Options", "value": "nosniff" },
          { "key": "X-Frame-Options", "value": "SAMEORIGIN" }
        ]
      }
    ]
  },
  "firestore": [
    {
      "database": "ai-studio-android-kasirpos-6c79f90a-57f2-4e44-8393-a818a10f5d55",
      "rules": "firestore.rules",
      "indexes": "firestore.indexes.json"
    },
    {
      "database": "(default)",
      "rules": "firestore.rules",
      "indexes": "firestore.indexes.json"
    }
  ],
  "emulators": {
    "firestore": {
      "host": "127.0.0.1",
      "port": 8085
    },
    "auth": {
      "host": "127.0.0.1",
      "port": 9099
    },
    "hosting": {
      "host": "127.0.0.1",
      "port": 5000
    },
    "ui": {
      "enabled": false
    }
  }
}
```

---

## 🌐 5. Cara Deploy Portal Owner ke Firebase Hosting

Jalankan perintah ini melalui terminal di laptop / komputer Anda:

### 1. Pasang Firebase CLI
```bash
npm install -g firebase-tools
```

### 2. Login ke Akun Google
```bash
firebase login
```

### 3. Hubungkan ke Project Anda
```bash
firebase use --add
```
*(Pilih Project ID Firebase Anda dari daftar yang muncul)*

### 4. Deploy Hosting dan Rules Bersamaan
```bash
firebase deploy --only hosting,firestore:rules
```

Setelah deploy berhasil, Firebase akan memberikan URL hosting publik Anda:
* `https://<project-id>.web.app`
* `https://<project-id>.firebaseapp.com`

---

## 📱 6. Menghubungkan Aplikasi Android POS dengan Web Hosting

Setelah mendapatkan URL hosting dari Firebase:
1. Buka aplikasi **Kasir POS** di tablet / HP Android.
2. Masuk ke tab **Pengaturan** > **Pengaturan Toko**.
3. Gulir ke kartu **"Hosting & Database Web Owner"**.
4. Masukkan data:
   * **Firebase Project ID:** Masukkan Project ID Firebase Anda.
   * **Real-time / Database ID:** Masukkan `(default)` atau ID custom database Anda.
   * **URL Hosting Web Laporan:** Masukkan URL web Anda (misal `https://<project-id>.web.app`).
5. Klik **"Simpan & Terapkan"**.
6. Klik tombol **"Buka Web Owner"** untuk menguji koneksi langsung dari perangkat kasir.

> **Catatan Keamanan Staf Kasir:**
> Saat tombol "Buka Web Owner" ditekan dari layar Laporan Harian kasir, aplikasi otomatis menambahkan parameter `?role=cashier`. Portal web secara otomatis mengunci tombol **Keluar (Logout)** dan menyembunyikan konfigurasi sensitif agar staf kasir tidak dapat mengubah pengaturan toko.
