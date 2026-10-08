package model;

import config.Koneksi;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDate;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import util.Validasi;
import static model.MasalahAplikasi.Jenis.*;

/** Facade model: sesi/domain/otorisasi dan satu Connection per operasi. */
public final class ManajemenData {
    @FunctionalInterface interface PembukaKoneksi { Connection buka() throws SQLException; }
    private final PembukaKoneksi koneksi;
    private final Clock clock;
    private final PenggunaDAO akun=new PenggunaDAO();
    private final PesertaDAO pesertaDAO=new PesertaDAO();
    private final PelatihanDAO pelatihanDAO=new PelatihanDAO();
    private final SertifikatDAO sertifikatDAO;
    private final NomorDAO nomorDAO=new NomorDAO();
    private Sesi sesiAktif;
    public ManajemenData(Koneksi koneksi) { this(koneksi,Clock.systemDefaultZone()); }
    public ManajemenData(Koneksi koneksi,Clock clock) {
        this(clock,Validasi.wajib(koneksi,"Koneksi")::buka);
    }
    // Injeksi package-private untuk uji kegagalan I/O tanpa memperluas API aplikasi.
    ManajemenData(Clock clock,PembukaKoneksi koneksi) {
        this.koneksi=Validasi.wajib(koneksi,"Koneksi"); this.clock=Validasi.wajib(clock,"Clock"); this.sertifikatDAO=new SertifikatDAO(clock);
    }
    public synchronized void periksaKesiapan() {
        baca(c->{new SchemaDAO().verifikasi(c); new SeedDAO().verifikasiTerinisialisasi(c); return null;});
    }
    /** Password caller dikonsumsi dan dihapus, termasuk pada login gagal. */
    public synchronized Sesi login(String username,char[] password) {
        sesiAktif=null;
        try {
            String normal;
            try { normal=Validasi.username(username); Validasi.password(password); }
            catch(IllegalArgumentException e) { throw masalah(LOGIN,"Login gagal. Periksa username dan password."); }
            Pengguna pengguna=baca(c->akun.cariUsername(c,normal));
            if(pengguna==null || !pengguna.verifikasiPassword(password)) throw masalah(LOGIN,"Login gagal. Periksa username dan password.");
            sesiAktif=new Sesi(pengguna); return sesiAktif;
        } catch(GeneralSecurityException e) { throw masalah(TEKNIS,"Gangguan algoritma login. Akhiri aplikasi dan periksa konfigurasi."); }
        finally { if(password!=null) Arrays.fill(password,'\0'); }
    }
    public synchronized void logout(Sesi sesi) { izin(sesi,false); sesiAktif=null; }
    private void izin(Sesi sesi,boolean admin) {
        if(sesi==null || sesi!=sesiAktif) throw masalah(SESI,"Sesi tidak aktif. Silakan login.");
        if(admin && sesi.getPengguna().getRole()!=Role.ADMIN) throw masalah(AKSES,"Akses ditolak untuk role ini.");
    }
    public synchronized ArrayList<Peserta> semuaPeserta(Sesi sesi) { izin(sesi,false); return baca(pesertaDAO::semua); }
    public synchronized ArrayList<Pelatihan> semuaPelatihan(Sesi sesi) { izin(sesi,false); return baca(pelatihanDAO::semua); }
    public synchronized ArrayList<Sertifikat> semuaSertifikat(Sesi sesi) { izin(sesi,false); return baca(sertifikatDAO::semua); }
    public synchronized Peserta cariPeserta(Sesi sesi,String id) { izin(sesi,false); return baca(c->harusAda(pesertaDAO.cari(c,id,false))); }
    public synchronized Pelatihan cariPelatihan(Sesi sesi,String id) { izin(sesi,false); return baca(c->harusAda(pelatihanDAO.cari(c,id,false))); }
    public synchronized Sertifikat cariSertifikat(Sesi sesi,String id) { izin(sesi,false); return baca(c->harusAda(sertifikatDAO.cari(c,id,false))); }
    public synchronized ArrayList<Sertifikat> riwayatPeserta(Sesi sesi,String id) { izin(sesi,false); return baca(c->{harusAda(pesertaDAO.cari(c,id,false)); return sertifikatDAO.riwayatPeserta(c,id);}); }
    public synchronized ArrayList<Sertifikat> cariNamaSertifikat(Sesi sesi,String nama) { izin(sesi,false); return baca(c->sertifikatDAO.cariNama(c,nama)); }
    public synchronized Statistik statistik(Sesi sesi) {
        izin(sesi,false);
        return baca(c->{ArrayList<Pelatihan> p=pelatihanDAO.semua(c); int online=0; for(Pelatihan item:p) if(item.getJenis()==JenisPelatihan.ONLINE) online++;
            return new Statistik(pesertaDAO.semua(c).size(),online,p.size()-online,sertifikatDAO.semua(c).size());});
    }
    public synchronized Peserta tambahPeserta(Sesi sesi,String nama,String email) {
        izin(sesi,true); String n=domain(()->Validasi.nama(nama,"Nama peserta")), e=domain(()->Validasi.email(email));
        return transaksi(c->{long nomor=nomorDAO.kunciNomor(c,"PST");
            if(pesertaDAO.emailDipakai(c,e,null)) throw masalah(DUPLIKAT,"Email sudah digunakan peserta lain.");
            Peserta p=new Peserta(Validasi.formatId("PST",nomor),n,e); pesertaDAO.tambah(c,p); nomorDAO.majukan(c,"PST",nomor); return p;});
    }
    public synchronized Peserta ubahPeserta(Sesi sesi,String id,String nama,String email) {
        izin(sesi,true);
        return transaksi(c->{Peserta old=harusAda(pesertaDAO.cari(c,id,true)); Peserta p=new Peserta(old.getIdPeserta(),nama,email);
            if(pesertaDAO.emailDipakai(c,p.getEmail(),p.getIdPeserta())) throw masalah(DUPLIKAT,"Email sudah digunakan peserta lain.");
            satu(pesertaDAO.ubah(c,p)); return p;});
    }
    public synchronized void hapusPeserta(Sesi sesi,String id) {
        izin(sesi,true); transaksi(c->{Peserta p=harusAda(pesertaDAO.cari(c,id,true)); String penghalang=pesertaDAO.referensiPenghalang(c,p.getIdPeserta());
            if(penghalang!=null) throw masalah(REFERENSI,"Peserta masih dirujuk sertifikat "+penghalang+"."); satu(pesertaDAO.hapus(c,p.getIdPeserta())); return null;});
    }
    public synchronized Pelatihan tambahOnline(Sesi sesi,String nama,int durasi,String instruktur,String platform,String tautan) {
        izin(sesi,true); return tambahPelatihan(id->new PelatihanOnline(id,nama,durasi,instruktur,platform,tautan));
    }
    public synchronized Pelatihan tambahOffline(Sesi sesi,String nama,int durasi,String instruktur,String lokasi,String ruangan) {
        izin(sesi,true); return tambahPelatihan(id->new PelatihanOffline(id,nama,durasi,instruktur,lokasi,ruangan));
    }
    private Pelatihan tambahPelatihan(Function<String,Pelatihan> calon) {
        return transaksi(c->{long nomor=nomorDAO.kunciNomor(c,"PLT"); Pelatihan p=calon.apply(Validasi.formatId("PLT",nomor));
            pelatihanDAO.tambah(c,p); nomorDAO.majukan(c,"PLT",nomor); return p;});
    }
    public synchronized Pelatihan ubahOnline(Sesi sesi,String id,String nama,int durasi,String instruktur,String platform,String tautan) {
        izin(sesi,true); return ubahPelatihan(id,key->new PelatihanOnline(key,nama,durasi,instruktur,platform,tautan));
    }
    public synchronized Pelatihan ubahOffline(Sesi sesi,String id,String nama,int durasi,String instruktur,String lokasi,String ruangan) {
        izin(sesi,true); return ubahPelatihan(id,key->new PelatihanOffline(key,nama,durasi,instruktur,lokasi,ruangan));
    }
    private Pelatihan ubahPelatihan(String id,Function<String,Pelatihan> calon) {
        return transaksi(c->{Pelatihan old=harusAda(pelatihanDAO.cari(c,id,true)); Pelatihan p=calon.apply(old.getIdPelatihan()); satu(pelatihanDAO.ubah(c,p)); return p;});
    }
    public synchronized void hapusPelatihan(Sesi sesi,String id) {
        izin(sesi,true); transaksi(c->{Pelatihan p=harusAda(pelatihanDAO.cari(c,id,true)); String penghalang=pelatihanDAO.referensiPenghalang(c,p.getIdPelatihan());
            if(penghalang!=null) throw masalah(REFERENSI,"Pelatihan masih dirujuk sertifikat "+penghalang+"."); satu(pelatihanDAO.hapus(c,p.getIdPelatihan())); return null;});
    }
    public synchronized PratinjauSertifikat pratinjauBaru(Sesi sesi,String pesertaId,String pelatihanId,LocalDate tanggal) {
        return pratinjau(sesi,null,pesertaId,pelatihanId,tanggal);
    }
    public synchronized PratinjauSertifikat pratinjauKoreksi(Sesi sesi,String id,String pesertaId,String pelatihanId,LocalDate tanggal) {
        izin(sesi,false); String key=domain(()->Validasi.idPencarian(id,"SRT")); return pratinjau(sesi,key,pesertaId,pelatihanId,tanggal);
    }
    private PratinjauSertifikat pratinjau(Sesi sesi,String id,String pesertaId,String pelatihanId,LocalDate tanggal) {
        izin(sesi,false); LocalDate date=domain(()->Validasi.tanggal(tanggal,clock));
        return baca(c->{Sertifikat old=id==null?null:harusAda(sertifikatDAO.cari(c,id,false));
            Peserta p=harusAda(pesertaDAO.cari(c,pesertaId,false)); Pelatihan l=harusAda(pelatihanDAO.cari(c,pelatihanId,false));
            if(sertifikatDAO.pasanganDipakai(c,p.getIdPeserta(),l.getIdPelatihan(),id)) throw masalah(DUPLIKAT,"Pasangan peserta dan pelatihan sudah memiliki sertifikat.");
            return new PratinjauSertifikat(sesi,id,p,l,date,SnapshotSertifikat.dari(p,l),old);});
    }
    public synchronized Sertifikat simpanSertifikat(Sesi sesi,PratinjauSertifikat preview) {
        izin(sesi,false);
        if(preview==null || preview.getSesi()!=sesi) throw masalah(SESI,"Pratinjau bukan milik sesi aktif. Buat pratinjau ulang.");
        return transaksi(c->{Sertifikat old=preview.isKoreksi()?harusAda(sertifikatDAO.cari(c,preview.getIdSertifikat(),true)):null;
            if(preview.isKoreksi() && !preview.cocokLama(old)) throw konflik();
            Peserta p=harusAda(pesertaDAO.cari(c,preview.getPesertaId(),true)); Pelatihan l=harusAda(pelatihanDAO.cari(c,preview.getPelatihanId(),true));
            SnapshotSertifikat snapshot=SnapshotSertifikat.dari(p,l);
            if(!snapshot.equals(preview.getCalon())) throw konflik();
            LocalDate date=Validasi.tanggal(preview.getTanggal(),clock);
            if(sertifikatDAO.pasanganDipakai(c,p.getIdPeserta(),l.getIdPelatihan(),preview.getIdSertifikat())) throw masalah(DUPLIKAT,"Pasangan peserta dan pelatihan sudah memiliki sertifikat.");
            if(old==null) {
                long nomor=nomorDAO.kunciNomor(c,"SRT"); Sertifikat data=new Sertifikat(Validasi.formatId("SRT",nomor),p,l,sesi.getPengguna(),date,snapshot,clock);
                sertifikatDAO.tambah(c,data); nomorDAO.majukan(c,"SRT",nomor); return data;
            }
            Sertifikat data=new Sertifikat(old.getIdSertifikat(),p,l,old.getPencatat(),date,snapshot,clock); satu(sertifikatDAO.ubah(c,data)); return data;
        });
    }
    public synchronized void hapusSertifikat(Sesi sesi,String id) {
        izin(sesi,false); transaksi(c->{Sertifikat data=harusAda(sertifikatDAO.cari(c,id,true)); satu(sertifikatDAO.hapus(c,data.getIdSertifikat())); return null;});
    }
    private static <T> T harusAda(T data) { if(data==null) throw masalah(TIDAK_ADA,"Data tidak ditemukan."); return data; }
    private static void satu(int rows) { if(rows!=1) throw masalah(TIDAK_ADA,"Data berubah atau sudah tidak tersedia."); }
    private static MasalahAplikasi konflik() { return masalah(KONFLIK,"Data berubah sejak pratinjau. Buat pratinjau dan konfirmasi ulang."); }
    private static MasalahAplikasi masalah(MasalahAplikasi.Jenis jenis,String pesan) { return new MasalahAplikasi(jenis,pesan); }
    private static <T> T domain(Supplier<T> work) { try { return work.get(); } catch(IllegalArgumentException e) { throw masalah(VALIDASI,e.getMessage()); } }
    @FunctionalInterface private interface Operasi<T> { T jalankan(Connection c) throws SQLException; }
    private <T> T baca(Operasi<T> work) {
        try(Connection c=koneksi.buka()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setReadOnly(true); c.setAutoCommit(false);
            try { return work.jalankan(c); } finally { c.rollback(); }
        } catch(SQLException e) { throw sql(e); }
        catch(IllegalArgumentException e) { throw masalah(VALIDASI,e.getMessage()); }
    }
    private <T> T transaksi(Operasi<T> work) {
        Connection c=null;
        try {
            c=koneksi.buka(); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            T result;
            try { result=work.jalankan(c); }
            catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); }
                if(e instanceof SQLException s) throw sql(s);
                if(e instanceof IllegalArgumentException) throw masalah(VALIDASI,e.getMessage());
                throw (RuntimeException)e;
            }
            try { c.commit(); } catch(SQLException e) { throw masalah(AMBIGU,"Status penyimpanan belum dapat dipastikan. Periksa data setelah koneksi pulih sebelum mencoba lagi."); }
            return result;
        } catch(SQLException e) { throw sql(e); }
        finally { if(c!=null) try { c.close(); } catch(SQLException ignored) { /* Tidak mengubah hasil commit yang sudah pasti. */ } }
    }
    private static MasalahAplikasi sql(SQLException e) {
        if(e.getErrorCode()==1062) return masalah(DUPLIKAT,"Data unik sudah digunakan (email, ID, atau pasangan sertifikat).");
        if(e.getErrorCode()==1451 || e.getErrorCode()==1452) return masalah(REFERENSI,"Data masih dirujuk atau referensinya tidak tersedia.");
        if(e.getErrorCode()==1205 || e.getErrorCode()==1213 || "40001".equals(e.getSQLState())) return masalah(KONFLIK,"Transaksi berbenturan atau melewati batas tunggu. Coba ulang setelah memeriksa data.");
        if("22003".equals(e.getSQLState())) return masalah(VALIDASI,"Kapasitas ID habis atau counter tidak valid.");
        return masalah(TEKNIS,"Database tidak tersedia atau data tidak konsisten. Periksa koneksi dan integritas schema.");
    }
}
