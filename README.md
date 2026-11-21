# Kasir POS

## 1. Nama Aplikasi
**Kasir POS** (`com.aistudio.poskasir.wrytxq`)

## 2. Deskripsi
Aplikasi kasir (*Point of Sale*) Android modern berbasis native Kotlin dan Jetpack Compose dengan fitur:
- Transaksi kasir cepat (dukungan nomor meja dinamis, pembayaran tunai/QRIS/transfer, perhitungan kembalian otomatis, dan cetak struk Bluetooth ESC/POS).
- Manajemen inventaris & kategori produk (dropdown kategori & satuan `porsi`/`pcs`, notifikasi stok menipis).
- Pencatatan pengeluaran operasional & laporan harian lengkap.
- Mode laporan web lokal & sinkronisasi online (`web-dashboard/` dan Firebase Realtime Database / Firestore / AI Logic).
- Pengaturan toko, PIN admin/kasir, dan jadwal tutup toko otomatis.

## 3. Framework & Teknologi
- **Aplikasi Utama (Android Native):** Kotlin `2.2.10`, Jetpack Compose (Material Design 3), Android Gradle Plugin `9.1.1`, Gradle `9.3.1`, Room `2.7.0` (KSP `2.3.5`), Moshi, Retrofit/OkHttp, Firebase BOM `34.17.0` (Auth, AI Logic, App Check).
- **Dashboard Web Pendamping (`web-dashboard/`):** Node.js Express/HTTP static server (`server.js`) dan HTML laporan online.
- **Kompatibilitas Android:** `minSdk = 23` (Android 6.0 Marshmallow), `targetSdk = 36`, `compileSdk = 36.1`.

## 4. Cara Instalasi Dependency
- **Android:** Dependensi dikelola otomatis oleh Gradle melalui `gradle/libs.versions.toml` dan `app/build.gradle.kts`. Pastikan **JDK 21** dan **Android SDK (`platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`)** tersedia.
- **Web Dashboard Opsional (`web-dashboard/`):**
  ```bash
  cd web-dashboard
  npm install
  ```

## 5. Cara Menjalankan Aplikasi
- **Android Studio:** Buka root proyek di Android Studio, tunggu Gradle sync selesai, pilih device/emulator Android (API 23+), lalu klik **Run (`app`)**.
- **Web Dashboard Lokal (opsional):**
  ```bash
  cd web-dashboard
  node server.js
  ```

## 6. Cara Build Web
Direktori `web-dashboard/` menggunakan HTML/JS statis tanpa bundler terpisah sehingga siap dijalankan langsung atau di-deploy ke Firebase Hosting (`firebase.json` mengarah ke konfigurasi hosting proyek).

## 7. Cara Build APK Lokal
1. Salin `.env.example` menjadi `.env` (jika belum ada):
   ```bash
   cp .env.example .env
   ```
2. Pastikan `debug.keystore` tersedia (dapat didekodekan dari `debug.keystore.base64`):
   ```bash
   base64 -d debug.keystore.base64 > debug.keystore
   ```
3. Jalankan kompilasi APK debug menggunakan Gradle `9.3.1` (atau `./gradlew` jika wrapper tersedia):
   ```bash
   gradle :app:assembleDebug --no-daemon --no-configuration-cache
   ```
4. Output APK berada di:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

## 8. Konfigurasi Capacitor
Proyek utama ini **sudah dibangun secara native menggunakan Kotlin + Jetpack Compose** (bukan aplikasi web yang dibungkus WebView), sehingga **tidak menggunakan Capacitor**. Pendekatan native ini dipertahankan agar seluruh fitur perangkat keras dan background Android (printer thermal Bluetooth, foreground service, alarm tutup toko, Room database lokal, dan server laporan lokal) tetap berjalan utuh tanpa kehilangan fungsi.

## 9. Cara Menjalankan GitHub Actions
Tersedia workflow otomatis di `.github/workflows/`:
- **`.github/workflows/build-android-apk.yml`** (`Build Android APK`): Berjalan otomatis setiap `push` / `pull_request` ke branch `main` atau `master`, serta dapat dijalankan manual lewat tab **Actions → Build Android APK → Run workflow**.
- **`.github/workflows/build-apk.yml`**: Workflow kompatibilitas debug APK (`fahfile-debug-apk`).
- **`.github/workflows/build-release-apk.yml`**: Workflow manual (`workflow_dispatch`) untuk membuat **Release APK** dengan keystore produksi dari GitHub Secrets.

## 10. Cara Download APK dari GitHub Actions
1. Buka repositori Anda di GitHub dan masuk ke tab **Actions**.
2. Pilih workflow run terbaru yang berstatus hijau (berhasil).
3. Gulir ke bagian **Artifacts** di halaman ringkasan run.
4. Klik **`Android-APK-debug-v1.0-<commit_sha>`** (atau `fahfile-debug-apk`) untuk mengunduh file `.zip`, lalu ekstrak untuk mendapatkan `app-debug.apk`.

## 11. Cara Install APK di HP Android
1. Pindahkan `app-debug.apk` ke perangkat Android (via USB, Google Drive, atau unduh langsung di HP).
2. Ketuk file `app-debug.apk` melalui File Manager.
3. Jika diminta izin keamanan, aktifkan **Izinkan dari sumber ini / Install unknown apps**.
4. Ketuk **Install**, lalu buka aplikasi **Kasir POS** setelah instalasi selesai.

## 12. Cara Mengatasi Build Error Umum
- **Lisensi SDK / `platforms;android-36.1` belum terpasang:** Jalankan `yes | sdkmanager --licenses` lalu `sdkmanager "platform-tools" "build-tools;36.0.0" "platforms;android-36.1"`.
- **JDK tidak kompatibel:** Gunakan **JDK 21** (Temurin) untuk menjalankan Gradle `9.3.1` dan AGP `9.1.1`.
- **File `.env` atau `debug.keystore` tidak ada di CI:** Salin `.env.example` ke `.env` dan dekode `debug.keystore.base64` ke `debug.keystore`.
- **Kehabisan memori saat build CI:** Gunakan `-Dorg.gradle.jvmargs="-Xmx3g -Dfile.encoding=UTF-8 -Djava.awt.headless=true"` dan `-Dorg.gradle.workers.max=2`.

## 13. Cara Mengganti App ID (`applicationId`)
1. Ubah `applicationId` pada blok `defaultConfig` di `app/build.gradle.kts`:
   ```kotlin
   applicationId = "com.domainanda.namaplikasi"
   ```
2. Sesuaikan `package_name` di `app/google-services.json` (unduh ulang dari Firebase Console jika menggunakan Firebase dengan App ID baru).

## 14. Cara Mengganti Nama Aplikasi
Perbarui ketiga tempat berikut agar konsisten:
1. `app/src/main/res/values/strings.xml` (`<string name="app_name">...</string>`)
2. `settings.gradle.kts` (`rootProject.name = "..."`)
3. `metadata.json` (`"name": "..."`)

## 15. Cara Membuat Release APK
Jangan pernah menaruh keystore atau password ke dalam repositori Git. Untuk build release lewat `.github/workflows/build-release-apk.yml`, tambahkan **Repository Secrets** di GitHub (**Settings → Secrets and variables → Actions**):
- `ANDROID_KEYSTORE_BASE64`: isi file `.jks` yang sudah di-encode ke Base64 (`base64 -w 0 my-upload-key.jks`).
- `STORE_PASSWORD`: password keystore.
- `KEY_PASSWORD`: password key alias `upload`.

Setelah semua secret ditambahkan, jalankan **Actions → Build Android Release APK → Run workflow**.
