# Baca sebelum menjalankan

1. Gunakan JDK 26 dan buka proyek Maven ini di Apache NetBeans.
2. Jalankan MySQL melalui Laragon (127.0.0.1:3306).
3. Koneksi MySQL: username `root`, password kosong. Ini berbeda dari login aplikasi.
4. Untuk pemasangan pertama, impor `database/db_sertifikat_pelatihan.sql` melalui phpMyAdmin. Database harus belum berisi tabel aplikasi. Alternatif: jalankan `main.SetupDatabase` setelah build. Pilih salah satu metode.
5. Clean and Build, lalu Run Project (`main.Main`). Pilih `1. Masuk`, isi username pada Output, kemudian password pada dialog bertanda bintang.

| Akun | Username | Password | Hak akses |
|---|---|---|---|
| Administrator | `admin` | `Admin2026!` | Seluruh peserta, pelatihan dan sertifikat |
| Petugas Sertifikat | `petugas` | `Petugas2026!` | Membaca peserta/pelatihan dan mengelola sertifikat |

Password peka huruf besar/kecil. Password telah diselaraskan pada database lokal dan dump pengumpulan. Setup tidak mengganti password database yang sudah terisi; gunakan dump final untuk pemasangan baru.

Data awal: dua peserta, dua pelatihan (Online dan Offline), satu sertifikat. ID dibuat otomatis. Pilih `0` untuk kembali/keluar sesuai menu dan `5` pada menu utama untuk logout. `/batal` membatalkan formulir yang belum disimpan. MySQL harus tetap aktif selama aplikasi digunakan.

Penjelasan PBO, MVC, diagram dan screenshot tersedia di README.md. File SQL ikut dikumpulkan di folder database.
