package model;

import java.sql.*;
import java.util.ArrayList;
import util.Validasi;

public final class PesertaDAO {
    static final String[] KOLOM={"id_peserta","nama","email","email_normal"};
    public ArrayList<Peserta> semua(Connection c) throws SQLException {
        ArrayList<Peserta> list=new ArrayList<>();
        try (PreparedStatement s=c.prepareStatement("SELECT * FROM peserta ORDER BY CAST(SUBSTRING(id_peserta,4) AS UNSIGNED),id_peserta"); ResultSet r=s.executeQuery()) {
            while(r.next()) list.add(dari(r,""));
        }
        return list;
    }
    public Peserta cari(Connection c,String id,boolean kunci) throws SQLException {
        if(kunci) AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM peserta WHERE id_peserta=?"+(kunci?" FOR UPDATE":""))) {
            s.setString(1,Validasi.idPencarian(id,"PST"));
            try(ResultSet r=s.executeQuery()) { return r.next()?dari(r,""):null; }
        }
    }
    public void tambah(Connection c,Peserta p) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("INSERT INTO peserta(id_peserta,nama,email,email_normal) VALUES (?,?,?,?)")) {
            s.setString(1,p.getIdPeserta()); s.setString(2,p.getNama()); s.setString(3,p.getEmail()); s.setString(4,p.getEmailNormal()); AturanDAO.satu(s.executeUpdate());
        }
    }
    public int ubah(Connection c,Peserta p) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("UPDATE peserta SET nama=?,email=?,email_normal=? WHERE id_peserta=?")) {
            s.setString(1,p.getNama()); s.setString(2,p.getEmail()); s.setString(3,p.getEmailNormal()); s.setString(4,p.getIdPeserta()); return s.executeUpdate();
        }
    }
    public int hapus(Connection c,String id) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("DELETE FROM peserta WHERE id_peserta=?")) { s.setString(1,Validasi.idPencarian(id,"PST")); return s.executeUpdate(); }
    }
    public boolean emailDipakai(Connection c,String email,String kecualiId) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM peserta WHERE email_normal=? AND (? IS NULL OR id_peserta<>?) LIMIT 1")) {
            s.setString(1,Validasi.emailNormal(email));
            String id=kecualiId==null?null:Validasi.idPencarian(kecualiId,"PST"); s.setString(2,id); s.setString(3,id);
            try(ResultSet r=s.executeQuery()) { return r.next(); }
        }
    }
    public String referensiPenghalang(Connection c,String id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT id_sertifikat FROM sertifikat WHERE peserta_id=? ORDER BY CAST(SUBSTRING(id_sertifikat,4) AS UNSIGNED) LIMIT 1")) {
            s.setString(1,Validasi.idPencarian(id,"PST")); try(ResultSet r=s.executeQuery()) { return r.next()?r.getString(1):null; }
        }
    }
    static Peserta dari(ResultSet r,String prefix) throws SQLException {
        try {
            Peserta p=new Peserta(r.getString(prefix+"id_peserta"),r.getString(prefix+"nama"),r.getString(prefix+"email"));
            if(!p.getEmailNormal().equals(r.getString(prefix+"email_normal"))) throw new IllegalArgumentException("Normalisasi email tidak sesuai.");
            return p;
        } catch(RuntimeException e) { throw AturanDAO.dataTidakValid(e); }
    }
}
