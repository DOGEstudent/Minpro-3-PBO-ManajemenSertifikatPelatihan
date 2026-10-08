# Database MySQL untuk pengumpulan

File [db_sertifikat_pelatihan.sql](db_sertifikat_pelatihan.sql) adalah dump database aplikasi, diekspor8 Oktober2026 dari MySQL8.4.10. Isinya enam tabel, constraint, dua akun aplikasi, dua peserta, dua pelatihan, satu sertifikat/snapshot dan tiga counter. Salt/hash biner menggunakan hex agar byte tetap utuh.

Dump tidak memuat database mahasiswa lain, akun server MySQL atau password root. Akun aplikasi admin/Admin2026! dan petugas/Petugas2026! adalah akses penilaian; database menyimpan hash, bukan password mentah. File tidak mengandung DROP TABLE/DROP DATABASE.

## Cara memakai

Pilih satu jalur pemasangan awal:

1. **Setup Java:** jalankan main.SetupDatabase sesuai README utama. Jalur ini memakai src/main/resources/schema-v1.sql dan seed tervalidasi.
2. **Impor SQL:** pada server MySQL yang belum memiliki schema db_sertifikat_pelatihan, impor file dump melalui phpMyAdmin: tab Import â†’ pilih file SQL â†’ jalankan. Dump sudah menyertakan CREATE DATABASE/USE. Alternatif, dari terminal MySQL pada folder proyek:

```sql
SOURCE database/db_sertifikat_pelatihan.sql;
```

Sesudah impor, jalankan aplikasi Java dengan DB_NAME default. Tidak perlu menjalankan kedua jalur atau mengimpor berulang. Dump menggunakan CREATE TABLE tanpa IF NOT EXISTS; hanya untuk pemasangan pada schema baru, bukan penggabungan/reset database yang sudah berisi data. Dump mempertahankan tanggal snapshot dan counter pada saat ekspor; setup Java membentuk data awal berdasarkan hari setup.

Lingkungan target adalah MySQL8.4 sesuai proyek. Import tidak memerlukan perubahan root/password default yang telah disepakati untuk komputer penulis. Pengguna komputer lain dapat mengatur environment koneksi dari README utama.

## Hasil pemeriksaan

Versi dump sebelum penggantian password diimpor pada instance audit port3307 yang terpisah. Seluruh nilai enam tabel dibandingkan dengan sumber dan identik, termasuk salt/hash. Login Admin/Petugas serta statistik berhasil; database aplikasi3306 tidak diubah. Instance audit dihentikan setelah selesai. Bukti operasional ada di audit lokal di luar repository pengumpulan.

Folder database dan file SQL ini perlu ditambahkan melalui Git bersama src, pom.xml, README dan docs. Datadir Laragon, cache Maven dan laporan penilaian mahasiswa tidak disertakan. Belum ada push GitHub yang diklaim.

Dump final telah diekspor ulang setelah perubahan password akun. Kredensial final juga digunakan pada seed Java dan petunjuk akses.
