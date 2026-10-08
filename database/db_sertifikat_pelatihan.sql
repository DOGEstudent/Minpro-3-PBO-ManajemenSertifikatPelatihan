
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `db_sertifikat_pelatihan` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `db_sertifikat_pelatihan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nomor_id` (
  `jenis` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `nomor_berikut` bigint NOT NULL,
  PRIMARY KEY (`jenis`),
  CONSTRAINT `ck_nomor_jenis` CHECK ((`jenis` in (_utf8mb4'PST',_utf8mb4'PLT',_utf8mb4'SRT'))),
  CONSTRAINT `ck_nomor_positif` CHECK ((`nomor_berikut` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `nomor_id` WRITE;
/*!40000 ALTER TABLE `nomor_id` DISABLE KEYS */;
INSERT INTO `nomor_id` VALUES ('PLT',3),('PST',3),('SRT',2);
/*!40000 ALTER TABLE `nomor_id` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pelatihan` (
  `id_pelatihan` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `jenis` enum('ONLINE','OFFLINE') NOT NULL,
  `nama` varchar(100) NOT NULL,
  `durasi` smallint NOT NULL,
  `instruktur` varchar(80) NOT NULL,
  `platform` varchar(100) DEFAULT NULL,
  `tautan` varchar(200) DEFAULT NULL,
  `lokasi` varchar(100) DEFAULT NULL,
  `ruangan` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_pelatihan`),
  CONSTRAINT `ck_pelatihan_durasi` CHECK ((`durasi` between 1 and 500)),
  CONSTRAINT `ck_pelatihan_jenis` CHECK ((((`jenis` = _utf8mb4'ONLINE') and (`platform` is not null) and (`tautan` is not null) and (`lokasi` is null) and (`ruangan` is null)) or ((`jenis` = _utf8mb4'OFFLINE') and (`lokasi` is not null) and (`ruangan` is not null) and (`platform` is null) and (`tautan` is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `pelatihan` WRITE;
/*!40000 ALTER TABLE `pelatihan` DISABLE KEYS */;
INSERT INTO `pelatihan` VALUES ('PLT001','ONLINE','Dasar Java',16,'Budi Santoso','Zoom','https://example.com/pelatihan/java',NULL,NULL),('PLT002','OFFLINE','Praktik Basis Data',12,'Dewi Lestari',NULL,NULL,'Laboratorium Informatika','A-101');
/*!40000 ALTER TABLE `pelatihan` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pengguna` (
  `id_pengguna` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `nama` varchar(80) NOT NULL,
  `username_normal` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `role` enum('ADMIN','PETUGAS_SERTIFIKAT') NOT NULL,
  `salt` binary(16) NOT NULL,
  `hash_password` binary(32) NOT NULL,
  `iterasi` int NOT NULL,
  PRIMARY KEY (`id_pengguna`),
  UNIQUE KEY `username_normal` (`username_normal`),
  CONSTRAINT `ck_pengguna_iterasi` CHECK ((`iterasi` = 600000))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `pengguna` WRITE;
/*!40000 ALTER TABLE `pengguna` DISABLE KEYS */;
INSERT INTO `pengguna` VALUES ('PGN001','Administrator','admin','ADMIN',0xA78A12EEB267F31067CF9408632477E6,0x9AEAB28E6FC263EC36B3A73FF4C1A40DF8591B0DBB8C64E7DC22E850E93E518A,600000),('PGN002','Petugas Sertifikat','petugas','PETUGAS_SERTIFIKAT',0xFD4471E433916BA0EC56DE5657F4D8F3,0x7DA2CD93CAFED626896B39AD5513D20C48595EF3B8D2503AE54D832463C4B974,600000);
/*!40000 ALTER TABLE `pengguna` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `peserta` (
  `id_peserta` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `nama` varchar(80) NOT NULL,
  `email` varchar(100) NOT NULL,
  `email_normal` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id_peserta`),
  UNIQUE KEY `email_normal` (`email_normal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `peserta` WRITE;
/*!40000 ALTER TABLE `peserta` DISABLE KEYS */;
INSERT INTO `peserta` VALUES ('PST001','Andi Pratama','andi@example.com','andi@example.com'),('PST002','Siti Rahma','siti@example.com','siti@example.com');
/*!40000 ALTER TABLE `peserta` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sertifikat` (
  `id_sertifikat` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `peserta_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `pelatihan_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `pencatat_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `tanggal_terbit` date NOT NULL,
  PRIMARY KEY (`id_sertifikat`),
  UNIQUE KEY `uq_sertifikat_pasangan` (`peserta_id`,`pelatihan_id`),
  KEY `fk_sertifikat_pelatihan` (`pelatihan_id`),
  KEY `fk_sertifikat_pengguna` (`pencatat_id`),
  CONSTRAINT `fk_sertifikat_pelatihan` FOREIGN KEY (`pelatihan_id`) REFERENCES `pelatihan` (`id_pelatihan`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sertifikat_pengguna` FOREIGN KEY (`pencatat_id`) REFERENCES `pengguna` (`id_pengguna`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sertifikat_peserta` FOREIGN KEY (`peserta_id`) REFERENCES `peserta` (`id_peserta`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_sertifikat_tanggal` CHECK ((`tanggal_terbit` >= _utf8mb4'2000-01-01'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sertifikat` WRITE;
/*!40000 ALTER TABLE `sertifikat` DISABLE KEYS */;
INSERT INTO `sertifikat` VALUES ('SRT001','PST001','PLT001','PGN001','2026-10-08');
/*!40000 ALTER TABLE `sertifikat` ENABLE KEYS */;
UNLOCK TABLES;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `snapshot_sertifikat` (
  `sertifikat_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `nama_peserta` varchar(80) NOT NULL,
  `email_peserta` varchar(100) NOT NULL,
  `nama_pelatihan` varchar(100) NOT NULL,
  `jenis_pelatihan` enum('ONLINE','OFFLINE') NOT NULL,
  `durasi` smallint NOT NULL,
  `instruktur` varchar(80) NOT NULL,
  `detail_pelaksanaan` varchar(1000) NOT NULL,
  PRIMARY KEY (`sertifikat_id`),
  CONSTRAINT `fk_snapshot_sertifikat` FOREIGN KEY (`sertifikat_id`) REFERENCES `sertifikat` (`id_sertifikat`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_snapshot_detail` CHECK ((char_length(`detail_pelaksanaan`) between 1 and 1000)),
  CONSTRAINT `ck_snapshot_durasi` CHECK ((`durasi` between 1 and 500))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `snapshot_sertifikat` WRITE;
/*!40000 ALTER TABLE `snapshot_sertifikat` DISABLE KEYS */;
INSERT INTO `snapshot_sertifikat` VALUES ('SRT001','Andi Pratama','andi@example.com','Dasar Java','ONLINE',16,'Budi Santoso','Platform: Zoom | Tautan: https://example.com/pelatihan/java');
/*!40000 ALTER TABLE `snapshot_sertifikat` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
