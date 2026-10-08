# Pemeriksaan aplikasi

## Pemeriksaan otomatis

Runner Java pada Maven melempar assertion bila pemeriksaan gagal. Gunakan hasil runner PASS dan BUILD SUCCESS; Surefire menampilkan nol test karena pengujian dijalankan melalui exec-maven-plugin.

| Runner | Fokus |
|---|---|
| UjiKredensial | Hash/salt dan password salah |
| UjiDomain + UjiInvariantDomain | Constructor/setter, ID, angka, teks, email, URL dan tanggal |
| UjiConsoleView | Retry input, pembatalan, EOF dan password tanpa pencetakan aplikasi |
| UjiGuardSesi | Semua API menolak sesi kosong/palsu sebelum JDBC |
| UjiSchemaSeed | Schema, constraint, identitas akun dan setup idempotent |
| UjiDAO | CRUD, FK, UNIQUE, counter dan rollback |
| UjiManajemenData | Hak akses, transaksi, snapshot dan konflik preview |
| UjiCLI | Alur input, menu, role dan operasi data |
| UjiPenerimaanField | Field tidak valid dan hasil penyimpanan |
| UjiPembatalan | Batal/EOF pada formulir dan konfirmasi |

Profil integrasi hanya boleh memakai db_sertifikat_pelatihan_test; data aplikasi tidak dipakai sebagai fixture. Cara menjalankan tersedia di README.

Pemeriksaan teknis tambahan sebelumnya mencakup ID999 ke1000, race counter/pasangan sertifikat, transaksi rollback, snapshot setelah perubahan master, serta persistensi setelah proses/server dimulai ulang. Data fixture dipisahkan dari database aplikasi.

## Pengamatan NetBeans oleh pemilik proyek

Screenshot asli tanggal 8 Oktober 2026 disertakan di README dan docs/screenshots. Terlihat login Admin, dialog password bertanda bintang, daftar peserta/pelatihan/sertifikat, detail snapshot, statistik, logout dan login Petugas. Saat ID sertifikat diisi 1, aplikasi menolak format dan menerima SRT001 pada percobaan berikutnya.

Bukti screenshot tersebut tidak mencakup seluruh operasi CRUD atau perubahan master. Verifikasi CRUD/snapshot/otorisasi juga dilakukan melalui runner integrasi. Screenshot login Petugas membuktikan keberhasilan login; pembatasan menu dan API diperiksa otomatis. Screenshot dibuat sebelum penggantian password final tanpa perubahan tampilan atau role.

## Database pengumpulan

Dump awal telah diuji impor pada MySQL terpisah dan enam tabel identik, termasuk salt/hash. Dump final diekspor ulang setelah penggantian password. Akun final dan hash dump diperiksa ulang terhadap aplikasi/database lokal. Password final tersedia di BACA-SEBELUM-MENJALANKAN.md.

Repository GitHub, tampilan README setelah push dan waktu pengiriman link harus diperiksa ketika pengumpulan dilakukan. Keberhasilan build lokal tidak membuktikan link telah terkirim.
