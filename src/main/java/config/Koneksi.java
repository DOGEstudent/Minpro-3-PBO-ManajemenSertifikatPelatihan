package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Koneksi {
    private final String host, database, user, password;
    private final int port;

    public Koneksi(String host, int port, String database, String user, String password) {
        if (host == null || !host.matches("[A-Za-z0-9.-]+") || port < 1 || port > 65535)
            throw new IllegalArgumentException("Host/port database tidak valid.");
        if (database == null || !database.matches("db_sertifikat_pelatihan(?:_test)?"))
            throw new IllegalArgumentException("Nama database harus schema aplikasi atau schema test.");
        if (user == null || user.isBlank() || password == null)
            throw new IllegalArgumentException("Konfigurasi akun database tidak valid.");
        this.host = host; this.port = port; this.database = database;
        this.user = user; this.password = password;
    }
    public static Koneksi dariEnvironment() {
        return new Koneksi(env("DB_HOST", "127.0.0.1"), Integer.parseInt(env("DB_PORT", "3306")),
            env("DB_NAME", "db_sertifikat_pelatihan"), env("DB_USER", "root"), env("DB_PASSWORD", ""));
    }
    private static String env(String key, String fallback) {
        String value = System.getenv(key); return value == null ? fallback : value;
    }
    public String getDatabase() { return database; }
    public Connection buka() throws SQLException { return bukaKe(database); }
    public Connection bukaServer() throws SQLException { return bukaKe(""); }
    private Connection bukaKe(String schema) throws SQLException {
        // Maven exec menjalankan runner pada classloader terpisah; inisialisasi
        // driver setiap classloader, termasuk sesudah runner sebelumnya ditutup.
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("Driver JDBC tidak tersedia. Sertakan folder lib.", "08001", e); }
        Properties p = new Properties();
        p.setProperty("user", user); p.setProperty("password", password);
        p.setProperty("sslMode", "REQUIRED"); p.setProperty("characterEncoding", "UTF-8");
        p.setProperty("connectTimeout", "5000"); p.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + schema, p);
    }
}
