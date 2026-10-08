package model;

import config.Koneksi;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.*;

public final class LayananSetup {
    private final Koneksi koneksi;
    public LayananSetup(Koneksi koneksi) { this.koneksi = koneksi; }
    public boolean jalankan() throws SQLException, IOException, GeneralSecurityException {
        try (Connection c = koneksi.bukaServer()) {
            String lock = "setup:" + koneksi.getDatabase();
            try (PreparedStatement s = c.prepareStatement("SELECT GET_LOCK(?,10)")) {
                s.setString(1, lock);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next() || r.getInt(1) != 1) throw new SQLException("Setup lain masih berjalan. Coba kembali.");
                }
            }
            try {
                new SchemaDAO().siapkan(c, koneksi.getDatabase());
                c.setAutoCommit(false);
                boolean seeded;
                try { seeded = new SeedDAO().isiJikaKosong(c); }
                catch (SQLException | GeneralSecurityException | RuntimeException e) {
                    try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                    throw e;
                }
                try { c.commit(); }
                catch (SQLException e) {
                    throw new SQLException("Status penyimpanan setup belum dapat dipastikan. Periksa data sebelum mencoba ulang.", e);
                }
                return seeded;
            } finally {
                try (PreparedStatement s = c.prepareStatement("SELECT RELEASE_LOCK(?)")) { s.setString(1, lock); s.execute(); }
            }
        }
    }
}
