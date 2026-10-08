package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public final class SchemaDAO {
    public static final String[] TABEL = {"pengguna", "peserta", "pelatihan", "sertifikat", "snapshot_sertifikat", "nomor_id"};
    public void siapkan(Connection server, String database) throws SQLException, IOException {
        if (!database.matches("db_sertifikat_pelatihan(?:_test)?")) throw new IllegalArgumentException("Schema tidak diizinkan.");
        try (Statement s = server.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4");
        }
        server.setCatalog(database);
        boolean adaTabel = false;
        try (ResultSet r = server.getMetaData().getTables(database, null, "%", new String[]{"TABLE"})) { adaTabel = r.next(); }
        if (adaTabel) { verifikasi(server); return; }
        String sql;
        try (var stream = SchemaDAO.class.getResourceAsStream("/schema-v1.sql")) {
            if (stream == null) throw new IOException("Resource schema-v1.sql tidak tersedia.");
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
        }
        // MySQL DDL auto-commit: bila terputus, schema parsial harus ditinjau, bukan direset.
        for (String command : sql.split(";")) if (!command.isBlank()) {
            try (Statement s = server.createStatement()) { s.executeUpdate(command); }
        }
        verifikasi(server);
    }
    public void verifikasi(Connection c) throws SQLException {
        Map<String, String> columns = Map.of(
            "pengguna", "id_pengguna,nama,username_normal,role,salt,hash_password,iterasi",
            "peserta", "id_peserta,nama,email,email_normal",
            "pelatihan", "id_pelatihan,jenis,nama,durasi,instruktur,platform,tautan,lokasi,ruangan",
            "sertifikat", "id_sertifikat,peserta_id,pelatihan_id,pencatat_id,tanggal_terbit",
            "snapshot_sertifikat", "sertifikat_id,nama_peserta,email_peserta,nama_pelatihan,jenis_pelatihan,durasi,instruktur,detail_pelaksanaan",
            "nomor_id", "jenis,nomor_berikut");
        for (String table : TABEL) {
            Set<String> actual = new HashSet<>();
            try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, "%")) {
                while (r.next()) actual.add(r.getString("COLUMN_NAME"));
            }
            if (!actual.equals(new HashSet<>(Arrays.asList(columns.get(table).split(",")))))
                throw new SQLException("Struktur schema parsial/tidak sesuai: " + table + ". Tidak ada reset otomatis.");
            try (PreparedStatement s = c.prepareStatement("SELECT ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?")) {
                s.setString(1, c.getCatalog()); s.setString(2, table);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next() || !"InnoDB".equals(r.getString(1)) || !r.getString(2).startsWith("utf8mb4"))
                        throw new SQLException("Engine/charset schema tidak sesuai: " + table);
                }
            }
        }
        Set<String> expected = Set.of("PRIMARY KEY:pengguna:PRIMARY", "PRIMARY KEY:peserta:PRIMARY",
            "PRIMARY KEY:pelatihan:PRIMARY", "PRIMARY KEY:sertifikat:PRIMARY", "PRIMARY KEY:snapshot_sertifikat:PRIMARY", "PRIMARY KEY:nomor_id:PRIMARY",
            "UNIQUE:pengguna:username_normal", "UNIQUE:peserta:email_normal", "UNIQUE:sertifikat:uq_sertifikat_pasangan",
            "FOREIGN KEY:sertifikat:fk_sertifikat_peserta", "FOREIGN KEY:sertifikat:fk_sertifikat_pelatihan", "FOREIGN KEY:sertifikat:fk_sertifikat_pengguna",
            "FOREIGN KEY:snapshot_sertifikat:fk_snapshot_sertifikat",
            "CHECK:pengguna:ck_pengguna_iterasi", "CHECK:pelatihan:ck_pelatihan_durasi", "CHECK:pelatihan:ck_pelatihan_jenis",
            "CHECK:sertifikat:ck_sertifikat_tanggal", "CHECK:snapshot_sertifikat:ck_snapshot_durasi", "CHECK:snapshot_sertifikat:ck_snapshot_detail",
            "CHECK:nomor_id:ck_nomor_jenis", "CHECK:nomor_id:ck_nomor_positif");
        Set<String> actualConstraints = new HashSet<>();
        try (PreparedStatement s = c.prepareStatement("SELECT CONSTRAINT_TYPE,TABLE_NAME,CONSTRAINT_NAME,ENFORCED FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=?")) {
            s.setString(1, c.getCatalog());
            try (ResultSet r = s.executeQuery()) { while (r.next()) {
                if (!"YES".equals(r.getString(4))) throw new SQLException("Constraint schema tidak aktif.");
                actualConstraints.add(r.getString(1) + ":" + r.getString(2) + ":" + r.getString(3));
            } }
        }
        if (!actualConstraints.equals(expected)) throw new SQLException("Struktur constraint schema tidak sesuai versi1. Tidak ada reset otomatis.");
    }
}
