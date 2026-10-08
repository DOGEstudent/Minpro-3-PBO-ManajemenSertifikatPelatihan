package model;

import java.sql.Connection;
import java.sql.SQLException;

/** Helper SQL lokal: DAO tidak commit/rollback atau menutup Connection caller. */
final class AturanDAO {
    private AturanDAO() { }
    static void transaksi(Connection c) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("Operasi mutasi/lock wajib memakai transaksi caller.", "25000");
    }
    static void satu(int rows) throws SQLException {
        if (rows != 1) throw new SQLException("Jumlah baris perubahan tidak sesuai; rollback transaksi diperlukan.", "40001");
    }
    static SQLException dataTidakValid(RuntimeException cause) {
        return new SQLException("Data database tidak sesuai kontrak domain.", "22000", cause);
    }
    static String kolom(String alias, String prefix, String... names) {
        return java.util.Arrays.stream(names).map(name -> alias + "." + name + " AS " + prefix + name)
            .collect(java.util.stream.Collectors.joining(","));
    }
}
