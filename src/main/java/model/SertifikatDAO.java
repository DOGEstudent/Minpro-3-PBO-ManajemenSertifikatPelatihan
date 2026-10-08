package model;

import java.sql.*;
import java.time.Clock;
import java.util.ArrayList;
import util.Validasi;

public final class SertifikatDAO {
    private static final String[] SNAPSHOT={"sertifikat_id","nama_peserta","email_peserta","nama_pelatihan","jenis_pelatihan","durasi","instruktur","detail_pelaksanaan"};
    private static final String SELECT="SELECT s.*,"+AturanDAO.kolom("h","h_",SNAPSHOT)+","+AturanDAO.kolom("p","p_",PesertaDAO.KOLOM)
        +","+AturanDAO.kolom("l","l_",PelatihanDAO.KOLOM)+","+AturanDAO.kolom("u","u_",PenggunaDAO.KOLOM)
        +" FROM sertifikat s LEFT JOIN snapshot_sertifikat h ON h.sertifikat_id=s.id_sertifikat"
        +" LEFT JOIN peserta p ON p.id_peserta=s.peserta_id LEFT JOIN pelatihan l ON l.id_pelatihan=s.pelatihan_id"
        +" LEFT JOIN pengguna u ON u.id_pengguna=s.pencatat_id";
    private static final String ORDER=" ORDER BY CAST(SUBSTRING(s.id_sertifikat,4) AS UNSIGNED),s.id_sertifikat";
    private final Clock clock;
    public SertifikatDAO() { this(Clock.systemDefaultZone()); }
    public SertifikatDAO(Clock clock) { this.clock=Validasi.wajib(clock,"Clock"); }
    public ArrayList<Sertifikat> semua(Connection c) throws SQLException { return baca(c,"",null); }
    public Sertifikat cari(Connection c,String id,boolean kunci) throws SQLException {
        String normalized=Validasi.idPencarian(id,"SRT");
        if(kunci) {
            AturanDAO.transaksi(c);
            // Lock hanya parent, supaya tidak mengubah urutan lock master pada facade.
            try(PreparedStatement s=c.prepareStatement("SELECT id_sertifikat FROM sertifikat WHERE id_sertifikat=? FOR UPDATE")) {
                s.setString(1,normalized); try(ResultSet r=s.executeQuery()) { if(!r.next()) return null; }
            }
        }
        ArrayList<Sertifikat> hasil=baca(c," WHERE s.id_sertifikat=?",normalized,kunci);
        return hasil.isEmpty()?null:hasil.get(0);
    }
    public ArrayList<Sertifikat> riwayatPeserta(Connection c,String idPeserta) throws SQLException {
        return baca(c," WHERE s.peserta_id=?",Validasi.idPencarian(idPeserta,"PST"));
    }
    public ArrayList<Sertifikat> cariNama(Connection c,String kata) throws SQLException {
        String text=Validasi.teksDasar(kata,"Pencarian",1,100);
        ArrayList<Sertifikat> list=new ArrayList<>();
        // LOCATE tidak menganggap %/_ sebagai wildcard; input tetap parameter SQL.
        // h NULL ikut dibaca agar kerusakan snapshot tidak dilewatkan oleh filter.
        try(PreparedStatement s=c.prepareStatement(SELECT+" WHERE h.sertifikat_id IS NULL OR LOCATE(LOWER(?),LOWER(h.nama_peserta))>0 OR LOCATE(LOWER(?),LOWER(h.nama_pelatihan))>0"+ORDER)) {
            s.setString(1,text); s.setString(2,text); try(ResultSet r=s.executeQuery()) { while(r.next()) list.add(dari(r)); }
        }
        return list;
    }
    public boolean pasanganDipakai(Connection c,String pesertaId,String pelatihanId,String kecualiId) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM sertifikat WHERE peserta_id=? AND pelatihan_id=? AND (? IS NULL OR id_sertifikat<>?) LIMIT 1")) {
            s.setString(1,Validasi.idPencarian(pesertaId,"PST")); s.setString(2,Validasi.idPencarian(pelatihanId,"PLT"));
            String id=kecualiId==null?null:Validasi.idPencarian(kecualiId,"SRT"); s.setString(3,id); s.setString(4,id);
            try(ResultSet r=s.executeQuery()) { return r.next(); }
        }
    }
    public void tambah(Connection c,Sertifikat data) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("INSERT INTO sertifikat(id_sertifikat,peserta_id,pelatihan_id,pencatat_id,tanggal_terbit) VALUES (?,?,?,?,?)")) {
            s.setString(1,data.getIdSertifikat()); s.setString(2,data.getPeserta().getIdPeserta()); s.setString(3,data.getPelatihan().getIdPelatihan());
            s.setString(4,data.getPencatat().getIdPengguna()); s.setDate(5,Date.valueOf(data.getTanggalTerbit())); AturanDAO.satu(s.executeUpdate());
        }
        try(PreparedStatement s=c.prepareStatement("INSERT INTO snapshot_sertifikat(nama_peserta,email_peserta,nama_pelatihan,jenis_pelatihan,durasi,instruktur,detail_pelaksanaan,sertifikat_id) VALUES (?,?,?,?,?,?,?,?)")) {
            isiSnapshot(s,data); AturanDAO.satu(s.executeUpdate());
        }
    }
    public int ubah(Connection c,Sertifikat data) throws SQLException {
        AturanDAO.transaksi(c);
        int rows;
        try(PreparedStatement s=c.prepareStatement("UPDATE sertifikat SET peserta_id=?,pelatihan_id=?,tanggal_terbit=? WHERE id_sertifikat=?")) {
            s.setString(1,data.getPeserta().getIdPeserta()); s.setString(2,data.getPelatihan().getIdPelatihan());
            s.setDate(3,Date.valueOf(data.getTanggalTerbit())); s.setString(4,data.getIdSertifikat()); rows=s.executeUpdate();
        }
        if(rows==0) return 0;
        AturanDAO.satu(rows);
        try(PreparedStatement s=c.prepareStatement("UPDATE snapshot_sertifikat SET nama_peserta=?,email_peserta=?,nama_pelatihan=?,jenis_pelatihan=?,durasi=?,instruktur=?,detail_pelaksanaan=? WHERE sertifikat_id=?")) {
            isiSnapshot(s,data); AturanDAO.satu(s.executeUpdate());
        }
        return rows;
    }
    public int hapus(Connection c,String id) throws SQLException {
        AturanDAO.transaksi(c);
        try(PreparedStatement s=c.prepareStatement("DELETE FROM sertifikat WHERE id_sertifikat=?")) { s.setString(1,Validasi.idPencarian(id,"SRT")); return s.executeUpdate(); }
    }
    private void isiSnapshot(PreparedStatement s,Sertifikat data) throws SQLException {
        SnapshotSertifikat h=data.getSnapshot(); s.setString(1,h.getNamaPeserta()); s.setString(2,h.getEmailPeserta());
        s.setString(3,h.getNamaPelatihan()); s.setString(4,h.getJenisPelatihan().name()); s.setInt(5,h.getDurasi());
        s.setString(6,h.getInstruktur()); s.setString(7,h.getDetailPelaksanaan()); s.setString(8,data.getIdSertifikat());
    }
    private ArrayList<Sertifikat> baca(Connection c,String where,String parameter) throws SQLException {
        return baca(c,where,parameter,false);
    }
    private ArrayList<Sertifikat> baca(Connection c,String where,String parameter,boolean kunci) throws SQLException {
        ArrayList<Sertifikat> list=new ArrayList<>();
        // Current read pada parent/snapshot: tidak terjebak read view lama di
        // REPEATABLE READ. Master dikunci terpisah oleh facade sesuai urutannya.
        try(PreparedStatement s=c.prepareStatement(SELECT+where+ORDER+(kunci?" FOR UPDATE OF s,h":""))) {
            if(parameter!=null) s.setString(1,parameter);
            try(ResultSet r=s.executeQuery()) { while(r.next()) list.add(dari(r)); }
        }
        return list;
    }
    private Sertifikat dari(ResultSet r) throws SQLException {
        if(r.getString("h_sertifikat_id")==null || r.getString("p_id_peserta")==null || r.getString("l_id_pelatihan")==null || r.getString("u_id_pengguna")==null)
            throw new SQLException("Integritas sertifikat bermasalah: snapshot atau referensi hilang.","22000");
        try {
            SnapshotSertifikat h=new SnapshotSertifikat(r.getString("h_nama_peserta"),r.getString("h_email_peserta"),r.getString("h_nama_pelatihan"),
                JenisPelatihan.valueOf(r.getString("h_jenis_pelatihan")),r.getInt("h_durasi"),r.getString("h_instruktur"),r.getString("h_detail_pelaksanaan"));
            return new Sertifikat(r.getString("id_sertifikat"),PesertaDAO.dari(r,"p_"),PelatihanDAO.dari(r,"l_"),PenggunaDAO.dari(r,"u_"),r.getDate("tanggal_terbit").toLocalDate(),h,clock);
        } catch(RuntimeException e) { throw AturanDAO.dataTidakValid(e); }
    }
}
