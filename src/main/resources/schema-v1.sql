-- Schema v1: jalankan pada database yang dipilih, tanpa DROP/reset data.
CREATE TABLE IF NOT EXISTS pengguna (
 id_pengguna VARCHAR(30) COLLATE utf8mb4_bin PRIMARY KEY,
 nama VARCHAR(80) NOT NULL,
 username_normal VARCHAR(30) COLLATE utf8mb4_bin NOT NULL UNIQUE,
 role ENUM('ADMIN','PETUGAS_SERTIFIKAT') NOT NULL,
 salt BINARY(16) NOT NULL,
 hash_password BINARY(32) NOT NULL,
 iterasi INT NOT NULL,
 CONSTRAINT ck_pengguna_iterasi CHECK(iterasi=600000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS peserta (
 id_peserta VARCHAR(30) COLLATE utf8mb4_bin PRIMARY KEY,
 nama VARCHAR(80) NOT NULL,
 email VARCHAR(100) NOT NULL,
 email_normal VARCHAR(100) COLLATE utf8mb4_bin NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pelatihan (
 id_pelatihan VARCHAR(30) COLLATE utf8mb4_bin PRIMARY KEY,
 jenis ENUM('ONLINE','OFFLINE') NOT NULL,
 nama VARCHAR(100) NOT NULL,
 durasi SMALLINT NOT NULL,
 instruktur VARCHAR(80) NOT NULL,
 platform VARCHAR(100), tautan VARCHAR(200), lokasi VARCHAR(100), ruangan VARCHAR(100),
 CONSTRAINT ck_pelatihan_durasi CHECK(durasi BETWEEN 1 AND 500),
 CONSTRAINT ck_pelatihan_jenis CHECK(
 (jenis='ONLINE' AND platform IS NOT NULL AND tautan IS NOT NULL AND lokasi IS NULL AND ruangan IS NULL)
 OR (jenis='OFFLINE' AND lokasi IS NOT NULL AND ruangan IS NOT NULL AND platform IS NULL AND tautan IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sertifikat (
 id_sertifikat VARCHAR(30) COLLATE utf8mb4_bin PRIMARY KEY,
 peserta_id VARCHAR(30) COLLATE utf8mb4_bin NOT NULL,
 pelatihan_id VARCHAR(30) COLLATE utf8mb4_bin NOT NULL,
 pencatat_id VARCHAR(30) COLLATE utf8mb4_bin NOT NULL,
 tanggal_terbit DATE NOT NULL,
 CONSTRAINT uq_sertifikat_pasangan UNIQUE(peserta_id,pelatihan_id),
 CONSTRAINT fk_sertifikat_peserta FOREIGN KEY(peserta_id) REFERENCES peserta(id_peserta) ON UPDATE RESTRICT ON DELETE RESTRICT,
 CONSTRAINT fk_sertifikat_pelatihan FOREIGN KEY(pelatihan_id) REFERENCES pelatihan(id_pelatihan) ON UPDATE RESTRICT ON DELETE RESTRICT,
 CONSTRAINT fk_sertifikat_pengguna FOREIGN KEY(pencatat_id) REFERENCES pengguna(id_pengguna) ON UPDATE RESTRICT ON DELETE RESTRICT,
 CONSTRAINT ck_sertifikat_tanggal CHECK(tanggal_terbit>='2000-01-01')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS snapshot_sertifikat (
 sertifikat_id VARCHAR(30) COLLATE utf8mb4_bin PRIMARY KEY,
 nama_peserta VARCHAR(80) NOT NULL, email_peserta VARCHAR(100) NOT NULL,
 nama_pelatihan VARCHAR(100) NOT NULL,
 jenis_pelatihan ENUM('ONLINE','OFFLINE') NOT NULL,
 durasi SMALLINT NOT NULL, instruktur VARCHAR(80) NOT NULL,
 detail_pelaksanaan VARCHAR(1000) NOT NULL,
 CONSTRAINT fk_snapshot_sertifikat FOREIGN KEY(sertifikat_id) REFERENCES sertifikat(id_sertifikat) ON UPDATE RESTRICT ON DELETE CASCADE,
 CONSTRAINT ck_snapshot_durasi CHECK(durasi BETWEEN 1 AND 500),
 CONSTRAINT ck_snapshot_detail CHECK(CHAR_LENGTH(detail_pelaksanaan) BETWEEN 1 AND 1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS nomor_id (
 jenis CHAR(3) COLLATE utf8mb4_bin PRIMARY KEY,
 nomor_berikut BIGINT NOT NULL,
 CONSTRAINT ck_nomor_jenis CHECK(jenis IN ('PST','PLT','SRT')),
 CONSTRAINT ck_nomor_positif CHECK(nomor_berikut>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
