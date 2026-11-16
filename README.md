<!-- Panduan proyek Android -->
# Ujian MAN untuk Android

Aplikasi kiosk untuk ujian online. Dibangun lewat GitHub Actions, tanpa perlu install apa pun di komputer.

## Persyaratan HP
- Android 8.0 (API 26) atau lebih baru.
- Izin yang diminta saat pertama dibuka: Tampil di atas aplikasi lain, Akses Jangan Ganggu, dan Notifikasi.

## Cara membuat APK
1. Buat repository baru di GitHub (sebaiknya Private).
2. Upload **seluruh isi** folder ini, termasuk folder `.github` dan file `build.env`.
3. Buka tab **Actions**, pilih **Build Android**, lalu klik **Run workflow**. Tunggu tanda centang hijau.
4. Klik run tersebut, lalu unduh artifact. Hasilnya berupa zip berisi `Ujian_MAN.apk`.

## Mengatur konfigurasi (build.env)
Semua pengaturan ada di file `build.env`. Ubah nilainya, lalu upload ulang dan jalankan build lagi.

| Kunci | Fungsi | Contoh |
|---|---|---|
| `APP_NAME` | Nama yang tampil di HP | `Ujian MAN` |
| `APP_ID` | ID unik aplikasi (package) | `id.dstech.ujianmankeerom` |
| `PASSWORD` | Password untuk masuk dan keluar | `MAN123` |
| `HOME_URL` | Alamat halaman ujian | `https://mankeerom.yzz.me/ujian` |
| `LOGIN_TITLE` | Judul di halaman login | `Ujian Online MAN Keerom` |
| `MIN_SDK` | Versi Android minimal | `26` |
| `TARGET_SDK` | Versi Android target | `34` |
| `LOGO` | File logo (PNG, sebaiknya persegi) untuk ikon dan halaman login | `logo.png` |
| `ARTIFACT_NAME` | Nama zip hasil download | `UjianMAN` |

Catatan:
- Spasi di nama file hasil build otomatis diganti `_`, misalnya `Ujian MAN` menjadi `Ujian_MAN.apk`.
- Mengubah `APP_ID` membuat aplikasi dianggap aplikasi baru di HP. Versi lama harus dihapus dulu.
- Format setiap baris: `KUNCI=NILAI`, tanpa tanda kutip.

## Cara kerja
- Setelah password benar, aplikasi masuk mode layar terkunci (pin layar) dan membuka halaman ujian.
- Saat keluar dengan password benar, alarm berbunyi, lalu aplikasi tertutup.
- Saat ujian berjalan, Jangan Ganggu dimatikan sementara dan dikembalikan setelah keluar.

## Catatan
- Kode ini belum diuji di perangkat asli. Jika build gagal, kirim pesan error dari tab Actions.
