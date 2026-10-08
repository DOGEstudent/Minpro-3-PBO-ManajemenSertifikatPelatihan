package pengujian;

import config.Koneksi;
import model.*;
import java.time.LocalDate;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static model.MasalahAplikasi.Jenis.*;

public final class UjiManajemenData {
    private static int checks;
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); System.out.println("PASS "+(++checks)+": "+label); }
    private static void reject(Runnable work,MasalahAplikasi.Jenis jenis,String label) {
        try { work.run(); throw new AssertionError("Diterima: "+label); }
        catch(MasalahAplikasi e) { check(e.getJenis()==jenis,label+" ["+e.getJenis()+"]"); }
    }
    private static Sesi login(ManajemenData m,String user,String password) { return m.login(user,password.toCharArray()); }
    private static long nomor(Koneksi k,String prefix) throws Exception {
        try(Connection c=k.buka()) { c.setAutoCommit(false); try { return new NomorDAO().kunciNomor(c,prefix); } finally { c.rollback(); } }
    }
    public static void main(String[] args) throws Exception {
        try { jalankan(); } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void jalankan() throws Exception {
        Koneksi k=Koneksi.dariEnvironment();
        if(!k.getDatabase().equals("db_sertifikat_pelatihan_test")) throw new IllegalArgumentException("Model test hanya untuk schema _test.");
        ManajemenData a=new ManajemenData(k), b=new ManajemenData(k), other=new ManajemenData(k);
        a.periksaKesiapan();
        reject(()->a.semuaPeserta(null),SESI,"baca tanpa sesi ditolak");
        char[] wrong="Admin2026! ".toCharArray(); reject(()->a.login("admin",wrong),LOGIN,"password tidak di-trim");
        check(new String(wrong).chars().allMatch(ch->ch==0),"password gagal dihapus dari array caller");
        reject(()->a.login("admin!","Admin2026!".toCharArray()),LOGIN,"username invalid tetap pesan login umum");
        reject(()->a.login("tidak-ada","Admin2026!".toCharArray()),LOGIN,"akun tidak ditemukan tetap pesan login umum");
        char[] raw="Admin2026!".toCharArray(); Sesi old=a.login(" ADMIN ",raw);
        check(new String(raw).chars().allMatch(ch->ch==0) && old.getPengguna().getRole()==Role.ADMIN,"login normal dan password sah dihapus");
        a.logout(old); Sesi admin=login(a,"admin","Admin2026!");
        reject(()->a.semuaPeserta(old),SESI,"sesi lama ditolak setelah akun sama login ulang");
        Sesi petugas=login(b,"petugas","Petugas2026!"), adminOther=login(other,"admin","Admin2026!");
        reject(()->a.semuaPeserta(adminOther),SESI,"sesi milik model lain ditolak");
        check(b.semuaPeserta(petugas).size()==2 && b.semuaPelatihan(petugas).size()==2,"petugas dapat membaca master");
        reject(()->b.tambahPeserta(petugas,"Peserta Baru","baru@example.com"),AKSES,"petugas tidak dapat tambah master");
        reject(()->b.ubahPeserta(petugas,"PST002","Nama Baru","siti@example.com"),AKSES,"petugas tidak dapat ubah master");
        reject(()->b.hapusPelatihan(petugas,"PLT002"),AKSES,"petugas tidak dapat hapus master");
        reject(()->b.hapusPeserta(petugas,"PST002"),AKSES,"petugas tidak dapat hapus peserta");
        reject(()->b.tambahOnline(petugas,"Java Dasar",1,"Budi Santoso","Zoom","https://example.com"),AKSES,"petugas tidak dapat tambah Online");
        reject(()->b.tambahOffline(petugas,"Java Dasar",1,"Budi Santoso","Gedung A","A-101"),AKSES,"petugas tidak dapat tambah Offline");
        reject(()->b.ubahOnline(petugas,"PLT001","Java Dasar",1,"Budi Santoso","Zoom","https://example.com"),AKSES,"petugas tidak dapat ubah Online");
        reject(()->b.ubahOffline(petugas,"PLT002","Java Dasar",1,"Budi Santoso","Gedung A","A-101"),AKSES,"petugas tidak dapat ubah Offline");
        reject(()->a.hapusPeserta(admin,"PST001"),REFERENSI,"hapus peserta dirujuk menyebut penghalang");
        reject(()->a.hapusPelatihan(admin,"PLT001"),REFERENSI,"hapus pelatihan dirujuk ditolak");
        reject(()->a.cariPeserta(admin,"PST999999"),TIDAK_ADA,"target hilang ditolak");
        long sebelum=nomor(k,"PST");
        reject(()->a.tambahPeserta(admin,"11","invalid@example.com"),VALIDASI,"nama numerik ditolak pada facade");
        reject(()->a.tambahPeserta(admin,"Nama Valid","ANDI@EXAMPLE.COM"),DUPLIKAT,"email duplikat lintas objek ditolak");
        check(nomor(k,"PST")==sebelum && a.semuaPeserta(admin).size()==2,"gagal validasi/duplikasi tidak mengonsumsi counter");
        long beforeTraining=nomor(k,"PLT");
        reject(()->a.tambahOnline(admin,"Java Dasar",0,"Budi Santoso","Zoom","https://example.com"),VALIDASI,"durasi invalid di facade rollback");
        reject(()->a.tambahOnline(admin,"Java Dasar",1,"Budi Santoso","Zoom","https://?"),VALIDASI,"URL invalid di facade rollback");
        check(nomor(k,"PLT")==beforeTraining,"validasi pelatihan gagal tidak mengonsumsi nomor");
        reject(()->b.pratinjauBaru(petugas,"PST002","PLT001",LocalDate.now().plusDays(1)),VALIDASI,"tanggal masa depan ditolak sebelum preview");
        reject(()->b.pratinjauBaru(petugas,"PST999999","PLT001",LocalDate.now()),TIDAK_ADA,"referensi preview hilang ditolak");
        List<String> ps=new ArrayList<>(), ls=new ArrayList<>(), ss=new ArrayList<>();
        try {
            Peserta p=a.tambahPeserta(admin,"Peserta Baru","model-baru@example.com"); ps.add(p.getIdPeserta());
            Peserta second=a.tambahPeserta(admin,"Peserta Kedua","model-kedua@example.com"); ps.add(second.getIdPeserta());
            Pelatihan on=a.tambahOnline(admin,"Java Lanjutan",20,"Budi Santoso","Zoom","https://example.com/java"); ls.add(on.getIdPelatihan());
            Pelatihan off=a.tambahOffline(admin,"Praktik Database",12,"Dewi Lestari","Gedung A","A-101"); ls.add(off.getIdPelatihan());
            check(a.cariPeserta(admin,p.getIdPeserta()).getNama().equals("Peserta Baru") && a.cariPelatihan(admin,on.getIdPelatihan()) instanceof PelatihanOnline,"tambah master commit dan dapat dibaca kembali");
            a.ubahOnline(admin,on.getIdPelatihan(),"Java Lanjutan",21,"Budi Santoso","Meet","https://example.com/java");
            check(a.cariPelatihan(admin,on.getIdPelatihan()).getDurasi()==21,"update pelatihan melalui model");
            a.ubahOffline(admin,off.getIdPelatihan(),"Praktik Database",13,"Dewi Lestari","Gedung B","02");
            long sBefore=nomor(k,"SRT");
            PratinjauSertifikat preview=b.pratinjauBaru(petugas,p.getIdPeserta(),on.getIdPelatihan(),LocalDate.now());
            check(nomor(k,"SRT")==sBefore && b.semuaSertifikat(petugas).size()==1,"preview tidak menyimpan parent/snapshot atau mengonsumsi ID");
            other.ubahPeserta(adminOther,p.getIdPeserta(),"Nama Terbaru","model-baru@example.com");
            reject(()->b.simpanSertifikat(petugas,preview),KONFLIK,"master berubah sejak preview ditolak");
            check(nomor(k,"SRT")==sBefore,"preview konflik tidak mengonsumsi counter");
            PratinjauSertifikat fresh=b.pratinjauBaru(petugas,p.getIdPeserta(),on.getIdPelatihan(),LocalDate.now());
            Sertifikat cert=b.simpanSertifikat(petugas,fresh); ss.add(cert.getIdSertifikat());
            check(cert.getPencatat().getIdPengguna().equals("PGN002") && cert.getSnapshot().getNamaPeserta().equals("Nama Terbaru"),"pencatat dari sesi Petugas dan snapshot sumber terkunci");
            reject(()->b.simpanSertifikat(petugas,fresh),DUPLIKAT,"preview tambah yang disimpan ulang tidak menduplikasi pasangan");
            reject(()->b.pratinjauBaru(petugas,p.getIdPeserta(),on.getIdPelatihan(),LocalDate.now()),DUPLIKAT,"preview pasangan terpakai ditolak");
            PratinjauSertifikat first=a.pratinjauKoreksi(admin,cert.getIdSertifikat(),second.getIdPeserta(),off.getIdPelatihan(),LocalDate.now());
            PratinjauSertifikat competing=b.pratinjauKoreksi(petugas,cert.getIdSertifikat(),p.getIdPeserta(),off.getIdPelatihan(),LocalDate.now());
            a.simpanSertifikat(admin,first);
            reject(()->b.simpanSertifikat(petugas,competing),KONFLIK,"sertifikat dikoreksi petugas lain sejak preview ditolak");
            check(a.cariSertifikat(admin,cert.getIdSertifikat()).getPencatat().getIdPengguna().equals("PGN002"),"koreksi Admin tetap mempertahankan pencatat awal");
            PratinjauSertifikat snapshotRefresh=a.pratinjauKoreksi(admin,cert.getIdSertifikat(),second.getIdPeserta(),off.getIdPelatihan(),LocalDate.now());
            other.ubahOffline(adminOther,off.getIdPelatihan(),"Praktik Database",14,"Dewi Lestari","Gedung B","02");
            reject(()->a.simpanSertifikat(admin,snapshotRefresh),KONFLIK,"detail pelatihan berubah sejak preview ditolak");
            a.simpanSertifikat(admin,a.pratinjauKoreksi(admin,cert.getIdSertifikat(),second.getIdPeserta(),off.getIdPelatihan(),LocalDate.now()));
            check(a.cariSertifikat(admin,cert.getIdSertifikat()).getSnapshot().getDurasi()==14,"koreksi pasangan sama menyegarkan snapshot");
            PratinjauSertifikat token=b.pratinjauBaru(petugas,p.getIdPeserta(),on.getIdPelatihan(),LocalDate.now());
            b.logout(petugas); Sesi next=login(b,"petugas","Petugas2026!");
            reject(()->b.simpanSertifikat(next,token),SESI,"preview sesi lama ditolak meskipun akun sama");
            other.ubahPeserta(adminOther,second.getIdPeserta(),"Nama Sesudahnya","model-kedua@example.com");
            check(a.cariSertifikat(admin,cert.getIdSertifikat()).getSnapshot().getNamaPeserta().equals("Peserta Kedua"),"edit master tidak menyegarkan sertifikat tersimpan");
            check(a.cariNamaSertifikat(admin,"Peserta Kedua").size()==1 && a.riwayatPeserta(admin,second.getIdPeserta()).size()==1,"pencarian/riwayat historis melalui model");
            Statistik stat=a.statistik(admin); check(stat.peserta()==4 && stat.pelatihanOnline()==2 && stat.pelatihanOffline()==2 && stat.sertifikat()==2,"statistik konsisten pada transaksi read-only");
            ManajemenData restart=new ManajemenData(k); reject(()->restart.semuaPeserta(admin),SESI,"instance baru tidak menerima sesi lama");
            Sesi kembali=login(restart,"admin","Admin2026!"); check(restart.cariSertifikat(kembali,cert.getIdSertifikat()).getSnapshot().getDurasi()==14,"data commit bertahan pada model/koneksi baru");
            long beforeDelete=nomor(k,"SRT"); b.hapusSertifikat(next,cert.getIdSertifikat()); ss.remove(cert.getIdSertifikat());
            check(nomor(k,"SRT")==beforeDelete,"hapus sertifikat tidak menurunkan counter");
            concurrency(a,admin,other,adminOther,ps);
            model.UjiKegagalanModel.jalankan(k,a,admin,p.getIdPeserta(),on.getIdPelatihan(),ps);
        } finally {
            for(String id:ss) a.hapusSertifikat(admin,id);
            for(String id:ls) a.hapusPelatihan(admin,id);
            for(String id:ps) a.hapusPeserta(admin,id);
        }
        ManajemenData gagal=new ManajemenData(new Koneksi("127.0.0.1",1,"db_sertifikat_pelatihan_test","root",""));
        reject(()->login(gagal,"admin","Admin2026!"),TEKNIS,"koneksi gagal menjadi pesan teknis tanpa sesi");
        reject(()->gagal.semuaPeserta(null),SESI,"koneksi gagal tidak membuka akses baca");
        reject(()->a.login("admin","WrongPassword!".toCharArray()),LOGIN,"login gagal mencabut sesi aktif");
        reject(()->a.semuaPeserta(admin),SESI,"sesi dicabut oleh login gagal");
        Sesi finalSession=login(a,"admin","Admin2026!"); check(a.semuaPeserta(finalSession).size()==2 && a.semuaSertifikat(finalSession).size()==1,"fixture kembali bersih tanpa reset counter");
        System.out.println("PASS: "+checks+" pemeriksaan facade login/sesi/role/transaksi/preview");
    }
    private static void concurrency(ManajemenData a,Sesi sa,ManajemenData b,Sesi sb,List<String> cleanup) throws Exception {
        try(var executor=Executors.newFixedThreadPool(2)) {
            Future<Peserta> first=executor.submit(()->a.tambahPeserta(sa,"Peserta Serentak","serentak-model@example.com"));
            Future<Peserta> second=executor.submit(()->b.tambahPeserta(sb,"Peserta Serentak","serentak-model@example.com"));
            int sukses=0, duplikat=0;
            for(Future<Peserta> f:List.of(first,second)) {
                try { Peserta p=f.get(15,TimeUnit.SECONDS); cleanup.add(p.getIdPeserta()); sukses++; }
                catch(ExecutionException e) { if(e.getCause() instanceof MasalahAplikasi m && m.getJenis()==DUPLIKAT) duplikat++; else throw e; }
            }
            check(sukses==1 && duplikat==1,"dua facade tambah email sama: satu commit, satu ditolak");
        }
    }
}
