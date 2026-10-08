package model;

import java.security.GeneralSecurityException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Arrays;
import util.Kredensial;

public final class SeedDAO {
    public boolean isiJikaKosong(Connection c) throws SQLException, GeneralSecurityException {
        long total = 0;
        for (String table : SchemaDAO.TABEL) total += jumlah(c, table);
        if (total != 0) { verifikasiTerinisialisasi(c); return false; }
        akun(c, "PGN001", "Administrator", "admin", "ADMIN", "Admin2026!");
        akun(c, "PGN002", "Petugas Sertifikat", "petugas", "PETUGAS_SERTIFIKAT", "Petugas2026!");
        Peserta peserta = new Peserta("PST001", "Andi Pratama", "andi@example.com");
        tambahPeserta(c, peserta);
        tambahPeserta(c, new Peserta("PST002", "Siti Rahma", "siti@example.com"));
        PelatihanOnline online = new PelatihanOnline("PLT001", "Dasar Java", 16, "Budi Santoso", "Zoom", "https://example.com/pelatihan/java");
        PelatihanOffline offline = new PelatihanOffline("PLT002", "Praktik Basis Data", 12, "Dewi Lestari", "Laboratorium Informatika", "A-101");
        execute(c, "INSERT INTO pelatihan VALUES (?,?,?,?,?,?,?,?,?)", online.getIdPelatihan(), online.getJenis().name(), online.getNama(), online.getDurasi(), online.getInstruktur(), online.getPlatform(), online.getTautan(), null, null);
        execute(c, "INSERT INTO pelatihan VALUES (?,?,?,?,?,?,?,?,?)", offline.getIdPelatihan(), offline.getJenis().name(), offline.getNama(), offline.getDurasi(), offline.getInstruktur(), null, null, offline.getLokasi(), offline.getRuangan());
        execute(c, "INSERT INTO sertifikat VALUES (?,?,?,?,?)", "SRT001", "PST001", "PLT001", "PGN001", Date.valueOf(LocalDate.now()));
        SnapshotSertifikat snapshot = SnapshotSertifikat.dari(peserta, online);
        execute(c, "INSERT INTO snapshot_sertifikat VALUES (?,?,?,?,?,?,?,?)", "SRT001", snapshot.getNamaPeserta(), snapshot.getEmailPeserta(), snapshot.getNamaPelatihan(), snapshot.getJenisPelatihan().name(), snapshot.getDurasi(), snapshot.getInstruktur(), snapshot.getDetailPelaksanaan());
        execute(c, "INSERT INTO nomor_id VALUES (?,?)", "PST", 3L);
        execute(c, "INSERT INTO nomor_id VALUES (?,?)", "PLT", 3L);
        execute(c, "INSERT INTO nomor_id VALUES (?,?)", "SRT", 2L);
        verifikasiTerinisialisasi(c);
        return true;
    }
    public long jumlah(Connection c, String table) throws SQLException {
        if (!Arrays.asList(SchemaDAO.TABEL).contains(table)) throw new IllegalArgumentException("Tabel tidak dikenal.");
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) { r.next(); return r.getLong(1); }
    }
    public void verifikasiTerinisialisasi(Connection c) throws SQLException {
        if (jumlah(c, "pengguna") != 2 || jumlah(c, "nomor_id") != 3)
            throw new SQLException("Data setup parsial. Tidak menambah atau mereset data otomatis.");
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM pengguna WHERE (id_pengguna='PGN001' AND username_normal='admin' AND role='ADMIN') OR (id_pengguna='PGN002' AND username_normal='petugas' AND role='PETUGAS_SERTIFIKAT')")) {
            r.next(); if (r.getInt(1) != 2) throw new SQLException("Identitas akun setup tidak sesuai.");
        }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM sertifikat s LEFT JOIN snapshot_sertifikat h ON h.sertifikat_id=s.id_sertifikat WHERE h.sertifikat_id IS NULL")) {
            r.next(); if (r.getLong(1) != 0) throw new SQLException("Sertifikat tanpa snapshot terdeteksi.");
        }
        String[] master = {"peserta", "pelatihan", "sertifikat"};
        String[] id = {"id_peserta", "id_pelatihan", "id_sertifikat"};
        String[] prefix = {"PST", "PLT", "SRT"};
        for (int i = 0; i < master.length; i++) {
            try (PreparedStatement s = c.prepareStatement("SELECT nomor_berikut > COALESCE((SELECT MAX(CAST(SUBSTRING(" + id[i] + ",4) AS UNSIGNED)) FROM " + master[i] + "),0) FROM nomor_id WHERE jenis=?")) {
                s.setString(1, prefix[i]);
                try (ResultSet r = s.executeQuery()) { if (!r.next() || !r.getBoolean(1)) throw new SQLException("Counter tidak sesuai: " + prefix[i]); }
            }
        }
    }
    private void akun(Connection c, String id, String nama, String username, String role, String raw) throws SQLException, GeneralSecurityException {
        char[] password = raw.toCharArray(); byte[] salt = Kredensial.saltBaru(); byte[] hash = null;
        try {
            hash = Kredensial.hash(password, salt);
            Pengguna akun = new Pengguna(id, nama, username, Role.valueOf(role), salt, hash, Kredensial.ITERASI);
            execute(c, "INSERT INTO pengguna VALUES (?,?,?,?,?,?,?)", akun.getIdPengguna(), akun.getNama(), akun.getUsernameNormal(), akun.getRole().name(), akun.getSalt(), akun.getHashPassword(), akun.getIterasi());
        } finally {
            Arrays.fill(password, '\0'); Arrays.fill(salt, (byte) 0);
            if (hash != null) Arrays.fill(hash, (byte) 0);
        }
    }
    private void tambahPeserta(Connection c, Peserta peserta) throws SQLException {
        execute(c, "INSERT INTO peserta VALUES (?,?,?,?)", peserta.getIdPeserta(), peserta.getNama(), peserta.getEmail(), peserta.getEmailNormal());
    }
    private void execute(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
            if (s.executeUpdate() != 1) throw new SQLException("Jumlah baris seed tidak sesuai.");
        }
    }
}
