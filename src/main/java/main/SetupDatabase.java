package main;

import config.Koneksi;
import model.LayananSetup;

public final class SetupDatabase {
    private SetupDatabase() { }
    public static void main(String[] args) {
        try {
            Koneksi koneksi = Koneksi.dariEnvironment();
            boolean dibuat = new LayananSetup(koneksi).jalankan();
            System.out.println(dibuat ? "Schema siap; data awal berhasil disimpan." : "Schema/data sudah siap; seed tidak diulang.");
        } catch (Exception e) {
            // Jangan membocorkan JDBC URL/password melalui stack trace pada layar pengguna.
            System.err.println("Setup gagal. Periksa koneksi, struktur dan integritas schema. Data tidak direset otomatis.");
            if (e instanceof java.sql.SQLException sql) {
                String message = sql.getMessage();
                if (message != null && (message.startsWith("Data setup") || message.startsWith("Struktur") || message.startsWith("Identitas")
                    || message.startsWith("Counter") || message.startsWith("Sertifikat tanpa") || message.startsWith("Setup lain") || message.startsWith("Status penyimpanan")))
                    System.err.println(message);
                else System.err.println("Kode MySQL: " + sql.getErrorCode() + "; SQLState: " + sql.getSQLState());
            }
            System.exit(1);
        }
    }
}
