package model;

import java.sql.*;
import util.Validasi;

public final class PenggunaDAO {
    static final String[] KOLOM = {"id_pengguna","nama","username_normal","role","salt","hash_password","iterasi"};
    public Pengguna cariUsername(Connection c, String username) throws SQLException {
        return cari(c, "username_normal", Validasi.username(username));
    }
    public Pengguna cariId(Connection c, String id) throws SQLException {
        return cari(c, "id_pengguna", Validasi.idPencarian(id,"PGN"));
    }
    private Pengguna cari(Connection c, String column, String value) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT * FROM pengguna WHERE " + column + "=?")) {
            s.setString(1,value);
            try (ResultSet r=s.executeQuery()) { return r.next() ? dari(r,"") : null; }
        }
    }
    static Pengguna dari(ResultSet r,String prefix) throws SQLException {
        try { return new Pengguna(r.getString(prefix+"id_pengguna"),r.getString(prefix+"nama"),r.getString(prefix+"username_normal"),
            Role.valueOf(r.getString(prefix+"role")),r.getBytes(prefix+"salt"),r.getBytes(prefix+"hash_password"),r.getInt(prefix+"iterasi")); }
        catch (RuntimeException e) { throw AturanDAO.dataTidakValid(e); }
    }
}
