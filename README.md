# MoviLish - Pemutar Video Sinema & Serial TV Cerdas

MoviLish adalah aplikasi pemutar video Android modern berbasis Jetpack Compose dan Kotlin dengan dukungan pemindaian pustaka film & serial TV cerdas, integrasi cloud, pemutaran offline, subtitle otomatis bilingual, dan kontrol gestur tingkat lanjut.

---

## 🚀 Cara Export Jadi File APK di GitHub

Aplikasi ini telah dilengkapi dengan **GitHub Actions Workflow** otomatis (`.github/workflows/build-apk.yml`) dan **Gradle Wrapper** (`gradlew` & `gradlew.bat`), sehingga Anda dapat langsung mengunduh file APK siap pasang ke HP Anda.

### 1. Push / Export Repository ke GitHub
1. Di AI Studio, pilih **Settings / Export** -> **Push to GitHub** atau unduh ZIP lalu push ke repository GitHub baru Anda.
2. Pastikan file branch utama adalah `main` atau `master`.

### 2. Dapatkan APK Otomatis dari GitHub Actions
1. Masuk ke halaman repository Anda di GitHub.
2. Buka tab **Actions** di menu atas.
3. Anda akan melihat alur kerja **Build Android APK** berjalan secara otomatis pada setiap push atau pull request.
4. Anda juga dapat menjalankannya kapan saja secara manual:
   - Klik **Build Android APK** di sidebar kiri.
   - Klik tombol **Run workflow**.
   - Pilih jenis APK (`both`, `debug`, atau `release`), lalu klik **Run workflow**.
5. Setelah build selesai (biasanya 2-3 menit), klik nama proses build tersebut.
6. Gulir ke bagian bawah pada tabel **Artifacts**, dan unduh **`MoviLish-APK.zip`**.
7. Ekstrak zip tersebut untuk mendapatkan file:
   - `MoviLish-v1.0-debug.apk` (siap install langsung di perangkat Android tanpa perlu sign tambahan)
   - `MoviLish-v1.0-release.apk`

### 3. Buat Rilis Otomatis (GitHub Release)
Untuk membuat rilis berlabel tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions akan secara otomatis membuat rilis baru di halaman **Releases** repository Anda dan melampirkan file APK yang bisa diunduh oleh siapa saja.

### 4. Build APK Sendiri di Komputer Lokal
Jika Anda memiliki Android Studio atau JDK 17/21 di PC:
- **Linux / macOS**:
  ```bash
  chmod +x ./gradlew
  ./gradlew assembleDebug
  ```
  File APK akan berada di `app/build/outputs/apk/debug/app-debug.apk`.
- **Windows (Command Prompt / PowerShell)**:
  ```cmd
  gradlew.bat assembleDebug
  ```

---

## 🎮 Panduan UI Tombol & Kontrol Gestur

Player video MoviLish dirancang dengan ergonomi layar sentuh responsif dan indikator HUD visual:

### 👆 Kontrol Gestur (Swipe & Tap)
- **Ketuk 1x di Layar**: Menampilkan / menyembunyikan panel kontrol (otomatis menghilang setelah 4,5 detik saat video berputar).
- **Ketuk 2x Sisi Kiri**: Memutar mundur **10 Detik** (`-10s`) dengan animasi HUD bulat dan efek ripple.
- **Ketuk 2x Sisi Kanan**: Memutar maju **10 Detik** (`+10s`) dengan animasi HUD bulat.
- **Geser Vertikal di Sisi Kiri**: Mengatur **Kecerahan Layar** (0% - 100%) dengan ikon matahari dan indikator persentase HUD.
- **Geser Vertikal di Sisi Kanan**: Mengatur **Volume Suara** (0% - 100%) dengan ikon speaker dan slider persentase HUD.
- **Geser Horizontal di Tengah**: **Pencarian Cepat / Scrubbing Timeline** secara presisi dengan tampilan waktu target dan penambahan/pengurangan detik (+/-s).

### 🔘 Tombol Kontrol Layar & Fitur
1. **Tombol Kunci Layar (Gembok di Kiri Layar)**:
   - Tekan untuk mengunci seluruh kontrol gestur layar (mencegah sentuhan tidak sengaja saat menonton).
   - Tampilan HUD "Layar Terkunci" akan muncul. Saat layar dikunci, mengetuk layar hanya akan menampilkan tombol gembok untuk membuka kembali dengan mudah.
2. **Tombol Rasio Layar (Aspect Ratio di Kanan Atas)**:
   - Siklus 4 mode rasio:
     - **Muat Layar (Fit)**: Mempertahankan aspek asli video tanpa memotong.
     - **Penuh Potong (Fill Crop)**: Memenuhi seluruh layar tanpa border hitam.
     - **16:9 Cinema**: Format layar lebar standar sinema.
     - **4:3 Standar**: Format TV klasik.
   - Disertai indikator HUD visual nama rasio yang aktif.
3. **Tombol Subtitle (Ikon CC di Kanan Atas)**:
   - Memilih trek subtitle lokal/bawaan, subtitle otomatis AI bilingual (Indonesia & Inggris), atau memuat file `.srt` / `.vtt` eksternal dari memori perangkat.
   - Pengaturan gaya: ukuran font, warna teks, opasitas latar belakang teks, dan sinkronisasi offset waktu (+/- milidetik).
4. **Tombol Kecepatan Putar (Ikon Speed)**:
   - Pilihan kecepatan: `0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`.
5. **Tombol Navigasi Tengah**:
   - Mundur 10 detik, Tombol Putar/Jeda (Play/Pause) berukuran besar (64dp), dan Maju 10 detik.
6. **Slider Timeline Halus**:
   - Navigasi seek instan tanpa lag dan tampilan durasi waktu berjalan yang presisi.
