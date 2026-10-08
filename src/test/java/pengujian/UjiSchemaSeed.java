package pengujian;

import config.Koneksi;
import model.*;
import util.Kredensial;
import java.sql.*;
import java.util.Arrays;

public final class UjiSchemaSeed {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        System.out.println("PASS " + (++checks) + ": " + label);
    }
    private static String value(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) { if (!r.next()) throw new AssertionError("Row hilang"); return r.getString(1); }
    }
    private static void sql(Connection c, String query) throws SQLException { try (Statement s = c.createStatement()) { s.executeUpdate(query); } }
    private static void reject(Connection c, String query, int code, String label) throws SQLException {
        c.setAutoCommit(false);
        try { sql(c, query); throw new AssertionError("Tidak ditolak: " + label); }
        catch (SQLException e) { check(e.getErrorCode() == code, label + " (" + e.getErrorCode() + ")"); }
        finally { c.rollback(); c.setAutoCommit(true); }
    }
    public static void main(String[] args) throws Exception {
        try { jalankan(); }
        finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void jalankan() throws Exception {
        Koneksi k = Koneksi.dariEnvironment();
        if (!k.getDatabase().equals("db_sertifikat_pelatihan_test"))
            throw new IllegalArgumentException("Uji integrasi hanya boleh memakai DB_NAME=db_sertifikat_pelatihan_test.");
        // Setup schema harus eksplisit: profile uji tidak membuat schema hilang diam-diam.
        try(Connection tersedia=k.buka()) { new SchemaDAO().verifikasi(tersedia); }
        LayananSetup setup = new LayananSetup(k);
        boolean first = setup.jalankan();
        System.out.println(first ? "INFO: pemasangan pertama kosong; seed ditambahkan" : "INFO: fixture sudah terinisialisasi; setup tidak reseed");
        String before, counterAwal;
        try (Connection c = k.buka()) {
            new SchemaDAO().verifikasi(c);
            check(true, "enam tabel, kolom, engine dan charset sesuai");
            before = value(c, "SELECT HEX(hash_password) FROM pengguna WHERE id_pengguna='PGN001'");
            counterAwal = value(c,"SELECT nomor_berikut FROM nomor_id WHERE jenis='PST'");
            SeedDAO seed = new SeedDAO();
            check(seed.jumlah(c,"pengguna")==2 && seed.jumlah(c,"peserta")==2 && seed.jumlah(c,"pelatihan")==2 && seed.jumlah(c,"sertifikat")==1 && seed.jumlah(c,"snapshot_sertifikat")==1 && seed.jumlah(c,"nomor_id")==3, "jumlah dummy lengkap");
            check(value(c,"SELECT COUNT(DISTINCT HEX(salt)) FROM pengguna").equals("2"), "salt dua akun berbeda");
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT username_normal,salt,hash_password FROM pengguna ORDER BY id_pengguna")) {
                while (r.next()) {
                    char[] raw = (r.getString(1).equals("admin") ? "Admin2026!" : "Petugas2026!").toCharArray();
                    try { check(Kredensial.cocok(raw,r.getBytes(2),r.getBytes(3)), "hash akun " + r.getString(1)); }
                    finally { Arrays.fill(raw,'\0'); }
                }
            }
        }
        check(!setup.jalankan(), "setup kedua tidak reseed");
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = workers.submit(() -> new LayananSetup(k).jalankan());
            var b = workers.submit(() -> new LayananSetup(k).jalankan());
            check(!a.get() && !b.get(), "dua setup paralel selesai tanpa duplikasi");
        }
        try (Connection c = k.buka()) {
            check(before.equals(value(c,"SELECT HEX(hash_password) FROM pengguna WHERE id_pengguna='PGN001'")), "hash akun tidak diganti setup ulang");
            reject(c, "INSERT INTO peserta VALUES ('PST003','Duplikat','ANDI@example.com','andi@example.com')",1062,"email_normal unik");
            reject(c, "INSERT INTO sertifikat VALUES ('SRT002','PST001','PLT001','PGN002',CURRENT_DATE)",1062,"pasangan sertifikat unik");
            reject(c, "INSERT INTO sertifikat VALUES ('SRT002','PST999','PLT001','PGN002',CURRENT_DATE)",1452,"FK menolak peserta hilang");
            reject(c, "DELETE FROM peserta WHERE id_peserta='PST001'",1451,"RESTRICT peserta");
            reject(c, "DELETE FROM pelatihan WHERE id_pelatihan='PLT001'",1451,"RESTRICT pelatihan");
            reject(c, "DELETE FROM pengguna WHERE id_pengguna='PGN001'",1451,"RESTRICT pencatat");
            reject(c, "UPDATE pelatihan SET durasi=0 WHERE id_pelatihan='PLT001'",3819,"durasi minimal");
            reject(c, "UPDATE pelatihan SET durasi=501 WHERE id_pelatihan='PLT001'",3819,"durasi maksimal");
            reject(c, "UPDATE pelatihan SET lokasi='Lokasi' WHERE id_pelatihan='PLT001'",3819,"kombinasi field ONLINE");
            reject(c, "UPDATE pelatihan SET platform='Zoom' WHERE id_pelatihan='PLT002'",3819,"kombinasi field OFFLINE");
            reject(c, "UPDATE sertifikat SET tanggal_terbit='1999-12-31' WHERE id_sertifikat='SRT001'",3819,"batas bawah tanggal");
            reject(c, "UPDATE nomor_id SET nomor_berikut=0 WHERE jenis='PST'",3819,"counter positif");
            c.setAutoCommit(false);
            try {
                sql(c,"UPDATE peserta SET nama='Nama Baru' WHERE id_peserta='PST001'");
                check(value(c,"SELECT nama_peserta FROM snapshot_sertifikat WHERE sertifikat_id='SRT001'").equals("Andi Pratama"),"snapshot tidak mengikuti edit master");
                sql(c,"DELETE FROM sertifikat WHERE id_sertifikat='SRT001'");
                check(value(c,"SELECT COUNT(*) FROM snapshot_sertifikat").equals("0"),"CASCADE snapshot");
                check(value(c,"SELECT COUNT(*) FROM peserta").equals("2"),"hapus sertifikat tidak menghapus master");
            } finally { c.rollback(); c.setAutoCommit(true); }
            check(value(c,"SELECT COUNT(*) FROM sertifikat").equals("1") && value(c,"SELECT COUNT(*) FROM snapshot_sertifikat").equals("1"),"rollback mengembalikan parent dan snapshot");
            c.setAutoCommit(false);
            try {
                sql(c,"UPDATE nomor_id SET nomor_berikut=4 WHERE jenis='PST'");
                sql(c,"INSERT INTO peserta VALUES ('PST003','O’Neil 日本','unicode@example.com','unicode@example.com')");
                check(value(c,"SELECT nama FROM peserta WHERE id_peserta='PST003'").equals("O’Neil 日本"),"Unicode database roundtrip");
            } finally { c.rollback(); c.setAutoCommit(true); }
            check(value(c,"SELECT nomor_berikut FROM nomor_id WHERE jenis='PST'").equals(counterAwal) && value(c,"SELECT COUNT(*) FROM peserta").equals("2"),"rollback entitas dan counter bersama");
            c.setAutoCommit(false);
            try {
                sql(c,"DELETE FROM sertifikat"); sql(c,"DELETE FROM peserta"); sql(c,"DELETE FROM pelatihan"); sql(c,"DELETE FROM pengguna"); sql(c,"DELETE FROM nomor_id");
                Connection fault = (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement") && args[0].toString().startsWith("INSERT INTO snapshot_sertifikat"))
                        throw new SQLException("INJECTED: snapshot insert failure");
                    try { return method.invoke(c, args); }
                    catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                });
                try { new SeedDAO().isiJikaKosong(fault); throw new AssertionError("Fault not triggered"); }
                catch (SQLException e) { check(e.getMessage().startsWith("INJECTED"),"kegagalan seed di tengah penyimpanan terdeteksi"); }
            } finally { c.rollback(); c.setAutoCommit(true); }
            check(before.equals(value(c,"SELECT HEX(hash_password) FROM pengguna WHERE id_pengguna='PGN001'")) && value(c,"SELECT COUNT(*) FROM snapshot_sertifikat").equals("1"),"rollback seed parsial memulihkan seluruh fixture lama");
            c.setAutoCommit(false);
            try {
                sql(c,"DELETE FROM nomor_id WHERE jenis='PST'");
                try { new SeedDAO().isiJikaKosong(c); throw new AssertionError("Partial accepted"); }
                catch (SQLException expected) { check(expected.getMessage().contains("parsial"),"data setup parsial ditolak tanpa reseed"); }
            } finally { c.rollback(); c.setAutoCommit(true); }
        }
        try (Connection c = k.buka()) {
            check(before.equals(value(c,"SELECT HEX(hash_password) FROM pengguna WHERE id_pengguna='PGN001'")),"data seed bertahan pada koneksi baru");
            new SeedDAO().verifikasiTerinisialisasi(c);
        }
        System.out.println("PASS: " + checks + " pemeriksaan schema/seed (bukan seluruh 160 kasus)");
    }
}
