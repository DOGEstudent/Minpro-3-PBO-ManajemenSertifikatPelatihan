package model;

import java.sql.*;

public final class NomorDAO {
    private static void jenis(String jenis) {
        if(!("PST".equals(jenis)||"PLT".equals(jenis)||"SRT".equals(jenis))) throw new IllegalArgumentException("Jenis counter tidak dikenal.");
    }
    public long kunciNomor(Connection c,String jenis) throws SQLException {
        jenis(jenis); AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("SELECT nomor_berikut FROM nomor_id WHERE jenis=? FOR UPDATE")) {
            s.setString(1,jenis);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next()) throw new SQLException("Counter tidak tersedia: " + jenis,"22000");
                long value=r.getLong(1);
                if(value<1 || value==Long.MAX_VALUE) throw new SQLException("Kapasitas ID habis atau counter tidak valid.","22003");
                return value;
            }
        }
    }
    public void majukan(Connection c,String jenis,long lama) throws SQLException {
        jenis(jenis); AturanDAO.transaksi(c);
        if(lama<1 || lama==Long.MAX_VALUE) throw new SQLException("Kapasitas ID habis atau counter tidak valid.","22003");
        try(PreparedStatement s=c.prepareStatement("UPDATE nomor_id SET nomor_berikut=? WHERE jenis=? AND nomor_berikut=?")) {
            s.setLong(1,lama+1); s.setString(2,jenis); s.setLong(3,lama); AturanDAO.satu(s.executeUpdate());
        }
    }
}
