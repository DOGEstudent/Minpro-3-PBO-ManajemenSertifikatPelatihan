package model;

import java.sql.*;
import java.util.ArrayList;
import util.Validasi;

public final class PelatihanDAO {
    static final String[] KOLOM={"id_pelatihan","jenis","nama","durasi","instruktur","platform","tautan","lokasi","ruangan"};
    public ArrayList<Pelatihan> semua(Connection c) throws SQLException {
        ArrayList<Pelatihan> list=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM pelatihan ORDER BY CAST(SUBSTRING(id_pelatihan,4) AS UNSIGNED),id_pelatihan"); ResultSet r=s.executeQuery()) {
            while(r.next()) list.add(dari(r,""));
        }
        return list;
    }
    public Pelatihan cari(Connection c,String id,boolean kunci) throws SQLException {
        if(kunci) AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM pelatihan WHERE id_pelatihan=?"+(kunci?" FOR UPDATE":""))) {
            s.setString(1,Validasi.idPencarian(id,"PLT")); try(ResultSet r=s.executeQuery()) { return r.next()?dari(r,""):null; }
        }
    }
    public void tambah(Connection c,Pelatihan p) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("INSERT INTO pelatihan(jenis,nama,durasi,instruktur,platform,tautan,lokasi,ruangan,id_pelatihan) VALUES (?,?,?,?,?,?,?,?,?)")) {
            isi(s,p); AturanDAO.satu(s.executeUpdate());
        }
    }
    public int ubah(Connection c,Pelatihan p) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("UPDATE pelatihan SET jenis=?,nama=?,durasi=?,instruktur=?,platform=?,tautan=?,lokasi=?,ruangan=? WHERE id_pelatihan=?")) { isi(s,p); return s.executeUpdate(); }
    }
    public int hapus(Connection c,String id) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("DELETE FROM pelatihan WHERE id_pelatihan=?")) { s.setString(1,Validasi.idPencarian(id,"PLT")); return s.executeUpdate(); }
    }
    public String referensiPenghalang(Connection c,String id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT id_sertifikat FROM sertifikat WHERE pelatihan_id=? ORDER BY CAST(SUBSTRING(id_sertifikat,4) AS UNSIGNED) LIMIT 1")) {
            s.setString(1,Validasi.idPencarian(id,"PLT")); try(ResultSet r=s.executeQuery()) { return r.next()?r.getString(1):null; }
        }
    }
    private void isi(PreparedStatement s,Pelatihan p) throws SQLException {
        s.setString(1,p.getJenis().name()); s.setString(2,p.getNama()); s.setInt(3,p.getDurasi()); s.setString(4,p.getInstruktur());
        if(p instanceof PelatihanOnline on) { s.setString(5,on.getPlatform()); s.setString(6,on.getTautan()); s.setNull(7,Types.VARCHAR); s.setNull(8,Types.VARCHAR); }
        else if(p instanceof PelatihanOffline off) { s.setNull(5,Types.VARCHAR); s.setNull(6,Types.VARCHAR); s.setString(7,off.getLokasi()); s.setString(8,off.getRuangan()); }
        else throw new IllegalArgumentException("Subclass pelatihan tidak didukung DAO.");
        s.setString(9,p.getIdPelatihan());
    }
    static Pelatihan dari(ResultSet r,String prefix) throws SQLException {
        try {
            String id=r.getString(prefix+"id_pelatihan"), nama=r.getString(prefix+"nama"), instruktur=r.getString(prefix+"instruktur"); int durasi=r.getInt(prefix+"durasi");
            return switch(JenisPelatihan.valueOf(r.getString(prefix+"jenis"))) {
                case ONLINE -> new PelatihanOnline(id,nama,durasi,instruktur,r.getString(prefix+"platform"),r.getString(prefix+"tautan"));
                case OFFLINE -> new PelatihanOffline(id,nama,durasi,instruktur,r.getString(prefix+"lokasi"),r.getString(prefix+"ruangan"));
            };
        } catch(RuntimeException e) { throw AturanDAO.dataTidakValid(e); }
    }
}
