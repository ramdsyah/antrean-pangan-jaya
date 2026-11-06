# Antrean Pangan Jaya

Aplikasi Android Native Kotlin + Jetpack Compose untuk menyimpan beberapa profil data dan membantu pengisian formulir pada situs resmi Pasar Jaya.

## Fitur versi awal
- Profil A, B, C, dan seterusnya.
- Tambah, ubah, pilih, dan hapus profil.
- Data profil disimpan terenkripsi menggunakan Android Keystore (AES/GCM).
- Halaman WebView untuk membuka situs resmi.
- Tombol isi otomatis hanya mengisi kolom formulir yang label/nama/placeholder-nya cocok.
- Tidak menekan tombol kirim, tidak mengisi CAPTCHA/OTP, dan tidak membuat nomor antrean.

## Build APK melalui GitHub
1. Unggah seluruh isi ZIP ini ke repository GitHub Anda, `ramdsyah/antrean-pangan-jaya`.
2. Pastikan folder `.github/workflows` ikut terunggah.
3. Buka tab **Actions** di GitHub dan aktifkan Actions jika diminta.
4. Pilih workflow **Build Android APK**, lalu klik **Run workflow**.
5. Setelah selesai dengan status hijau, buka hasil workflow dan unduh artifact `Antrean-Pangan-Jaya-debug-apk`.
6. Ekstrak ZIP artifact, lalu pasang APK di Android. Jika diminta, izinkan pemasangan dari sumber tersebut.

## Catatan penting
Situs Pasar Jaya dapat mengubah struktur formulir atau membatasi WebView. Fitur isi otomatis bersifat bantuan berdasarkan label kolom yang terdeteksi; periksa seluruh data sebelum melanjutkan. Jika formulir tidak cocok, isi manual melalui situs resmi. OTP, CAPTCHA, verifikasi, kuota, dan penerbitan nomor antrean tetap ditangani situs resmi.

Jangan memasukkan data pribadi asli ke issue, commit, atau file repository. Profil disimpan di penyimpanan privat aplikasi pada perangkat.
