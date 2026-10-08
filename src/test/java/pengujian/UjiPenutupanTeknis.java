package pengujian;

import config.Koneksi;
import controller.SertifikatController;
import model.*;
import util.*;
import view.ConsoleView;
import java.io.*;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Runner tambahan hanya pada instance fixture3307; tidak memakai Laragon aplikasi. */
public final class UjiPenutupanTeknis {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneId.of("Asia/Singapore"));
    private static Koneksi k; private static ManajemenData a,b; private static Sesi sa,sb;
    private static final List<String> ps=new ArrayList<>(),ls=new ArrayList<>(),ss=new ArrayList<>();
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    private static void pass(String id,String label) { System.out.println("PASS PENUTUPAN "+id+": "+label); }
    private static void reject(Runnable r,MasalahAplikasi.Jenis kind) { try { r.run(); throw new AssertionError("Operasi diterima"); } catch(MasalahAplikasi e) { check(e.getJenis()==kind,"Jenis penolakan "+e.getJenis()+" != "+kind); } }
    private static long nomor(String kind) throws Exception { try(Connection c=k.buka()) { c.setAutoCommit(false); try { return new NomorDAO().kunciNomor(c,kind); } finally { c.rollback(); } } }
    private static void setNomor(String kind,long n) throws Exception { try(Connection c=k.buka(); PreparedStatement s=c.prepareStatement("UPDATE nomor_id SET nomor_berikut=? WHERE jenis=?")) { s.setLong(1,n); s.setString(2,kind); check(s.executeUpdate()==1,"counter fixture"); } }
    private static Peserta peserta(String name,String email) { Peserta p=a.tambahPeserta(sa,name,email); ps.add(p.getIdPeserta()); return p; }
    private static Pelatihan online(String name) { Pelatihan p=a.tambahOnline(sa,name,7,"Instruktur Asal","Zoom","https://example.com/asal"); ls.add(p.getIdPelatihan()); return p; }
    private static Pelatihan offline(String name) { Pelatihan p=a.tambahOffline(sa,name,8,"Instruktur Asal","Gedung Asal","A-101"); ls.add(p.getIdPelatihan()); return p; }
    private static Sertifikat certificate(Peserta p,Pelatihan l,LocalDate d) { Sertifikat s=a.simpanSertifikat(sa,a.pratinjauBaru(sa,p.getIdPeserta(),l.getIdPelatihan(),d)); ss.add(s.getIdSertifikat()); return s; }
    private static String paket(Sertifikat s) { return s.getIdSertifikat()+"|"+s.getPeserta().getIdPeserta()+"|"+s.getPelatihan().getIdPelatihan()+"|"+s.getPencatat().getIdPengguna()+"|"+s.getTanggalTerbit()+"|"+s.getSnapshot().getNamaPeserta()+"|"+s.getSnapshot().getEmailPeserta()+"|"+s.getSnapshot().getNamaPelatihan()+"|"+s.getSnapshot().getJenisPelatihan()+"|"+s.getSnapshot().getDurasi()+"|"+s.getSnapshot().getInstruktur()+"|"+s.getSnapshot().getDetailPelaksanaan(); }
    private static String cli(String script) { StringWriter out=new StringWriter(); try(var v=new ConsoleView(new StringReader(script),out,false)) { new SertifikatController(new ManajemenData(k,CLOCK),v,CLOCK).jalankan(); } return out.toString(); }
    private static String admin() { return "1\nadmin\nAdmin2026!\n"; }
    public static void main(String[] args) throws Exception {
        try { uji(); } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void uji() throws Exception {
        check("3307".equals(System.getenv("DB_PORT")),"Runner memerlukan instance3307 khusus");
        k=Koneksi.dariEnvironment(); check(k.getDatabase().equals("db_sertifikat_pelatihan_test"),"Schema test wajib");
        a=new ManajemenData(k,CLOCK); b=new ManajemenData(k,CLOCK); a.periksaKesiapan();
        sa=a.login("admin","Admin2026!".toCharArray()); sb=b.login("petugas","Petugas2026!".toCharArray());
        try {
            String unknown=cli("1\ntidak-ada\nPasswordSalah!\n0\n"), wrong=cli("1\nadmin\nPasswordSalah!\n0\n");
            String error="Login gagal. Periksa username dan password.";
            check(unknown.contains(error) && wrong.contains(error) && !unknown.contains("Login berhasil"),"pesan login umum"); pass("T099","username hilang dan password salah lewat controller");
            Sesi s1=a.login("admin","Admin2026!".toCharArray()); a.logout(s1); sa=a.login("admin","Admin2026!".toCharArray());
            long np=nomor("PST"); reject(()->a.tambahPeserta(s1,"Sesi Lama","stale@example.com"),MasalahAplikasi.Jenis.SESI); check(nomor("PST")==np,"counter sesi lama"); pass("T100","s1 mutasi ditolak, s2 tetap aktif");
            Peserta p=peserta("Peserta Asal","teknis-asal@example.com"), q=peserta("Peserta Kedua","teknis-kedua@example.com");
            Pelatihan on=online("Online Asal"), off=offline("Offline Asal");
            Sertifikat c=certificate(p,on,LocalDate.of(2026,10,1)), d=certificate(q,off,LocalDate.of(2026,10,1));
            String old=paket(a.cariSertifikat(sa,c.getIdSertifikat()));
            a.simpanSertifikat(sa,a.pratinjauKoreksi(sa,c.getIdSertifikat(),p.getIdPeserta(),on.getIdPelatihan(),LocalDate.of(2026,10,1)));
            reject(()->a.pratinjauKoreksi(sa,c.getIdSertifikat(),q.getIdPeserta(),off.getIdPelatihan(),LocalDate.of(2026,10,2)),MasalahAplikasi.Jenis.DUPLIKAT);
            check(paket(a.cariSertifikat(sa,c.getIdSertifikat())).equals(old),"koreksi duplikat menjaga seluruh paket"); pass("T105","pasangan sendiri sah dan pasangan milik sertifikat lain ditolak");
            Sertifikat edit=b.simpanSertifikat(sb,b.pratinjauKoreksi(sb,c.getIdSertifikat(),p.getIdPeserta(),off.getIdPelatihan(),LocalDate.of(2026,10,2)));
            check(edit.getIdSertifikat().equals(c.getIdSertifikat()) && edit.getPencatat().getIdPengguna().equals("PGN001") && edit.getPelatihan().getIdPelatihan().equals(off.getIdPelatihan()) && edit.getTanggalTerbit().equals(LocalDate.of(2026,10,2)) && edit.getSnapshot().getJenisPelatihan()==JenisPelatihan.OFFLINE,"paket koreksi petugas"); pass("T108","Admin membuat, Petugas mengganti tanggal/pasangan; ID/pencatat tetap");
            SnapshotSertifikat before=SnapshotSertifikat.dari(a.cariPeserta(sa,p.getIdPeserta()),a.cariPelatihan(sa,on.getIdPelatihan()));
            reject(()->a.ubahPeserta(sa,p.getIdPeserta(),"Nama Calon","@"),MasalahAplikasi.Jenis.VALIDASI);
            reject(()->a.ubahOnline(sa,on.getIdPelatihan(),"Judul Calon",10,"Instruktur Calon","Meet","https://?"),MasalahAplikasi.Jenis.VALIDASI);
            reject(()->a.ubahOffline(sa,on.getIdPelatihan(),"Judul Calon",10,"Instruktur Calon","Gedung Calon","!"),MasalahAplikasi.Jenis.VALIDASI);
            old=paket(a.cariSertifikat(sa,c.getIdSertifikat()));
            reject(()->a.pratinjauKoreksi(sa,c.getIdSertifikat(),q.getIdPeserta(),on.getIdPelatihan(),LocalDate.of(2026,10,9)),MasalahAplikasi.Jenis.VALIDASI);
            check(before.equals(SnapshotSertifikat.dari(a.cariPeserta(sa,p.getIdPeserta()),a.cariPelatihan(sa,on.getIdPelatihan()))) && old.equals(paket(a.cariSertifikat(sa,c.getIdSertifikat()))),"semua field tetap setelah field terakhir invalid"); pass("T109","atomic paket master dan sertifikat pada field terakhir invalid");
            for(Pelatihan l:List.of(on,off)) check(l.getDetailPelaksanaan(false).equals(l.getDetailPelaksanaan()) && l.getDetailPelaksanaan(true).contains(l.getJenis().toString()) && !l.getDetailPelaksanaan(true).equals(l.getDetailPelaksanaan()),"overload "+l.getJenis()); pass("T116","kedua signature pada kedua subclass");
            SnapshotSertifikat asal=a.cariSertifikat(sa,c.getIdSertifikat()).getSnapshot();
            a.ubahPeserta(sa,p.getIdPeserta(),"Peserta Terbaru","teknis-terbaru@example.com");
            a.ubahOffline(sa,off.getIdPelatihan(),"Offline Terbaru",25,"Instruktur Terbaru","Gedung Terbaru","B-202");
            SnapshotSertifikat terbaru=SnapshotSertifikat.dari(a.cariPeserta(sa,p.getIdPeserta()),a.cariPelatihan(sa,off.getIdPelatihan()));
            check(!asal.equals(terbaru) && a.semuaSertifikat(sa).stream().filter(s->s.getIdSertifikat().equals(c.getIdSertifikat())).findFirst().orElseThrow().getSnapshot().equals(asal),"snapshot daftar tetap");
            check(a.cariNamaSertifikat(sa,"Peserta Asal").size()==1 && a.cariNamaSertifikat(sa,"Peserta Terbaru").isEmpty() && a.riwayatPeserta(sa,p.getIdPeserta()).get(0).getSnapshot().equals(asal),"snapshot filter dan riwayat");
            String out=cli(admin()+"1\n2\n"+p.getIdPeserta()+"\n0\n2\n2\n"+off.getIdPelatihan()+"\n0\n3\n1\n2\n"+c.getIdSertifikat()+"\n6\nPeserta Asal\n7\n"+p.getIdPeserta()+"\n0\n0\n");
            check(out.contains("Snapshot peserta: Peserta Asal | teknis-asal@example.com") && out.contains(asal.getDetailPelaksanaan()) && out.contains("Master terkini: Peserta Terbaru") && out.contains(terbaru.getDetailPelaksanaan()),"seluruh tampilan CLI snapshot/master"); pass("T118","semua field master berubah, daftar/detail/cari/riwayat snapshot lama");
            String form="3\n4\n"+c.getIdSertifikat()+"\n"+p.getIdPeserta()+"\n"+off.getIdPelatihan()+"\n2026-10-02\n";
            cli(admin()+form+"t\n0\n0\n"); check(a.cariSertifikat(sa,c.getIdSertifikat()).getSnapshot().equals(asal),"batal tetap snapshot A");
            cli(admin()+form+"y\n0\n0\n"); check(a.cariSertifikat(sa,c.getIdSertifikat()).getSnapshot().equals(terbaru),"y seluruh snapshot B"); pass("T119","batal mempertahankan A, y menyegarkan semua field B tanpa reference mutable");
            pagination(on);
            try(var v=new ConsoleView(new StringReader("9".repeat(10000)+"\n0\n"),new StringWriter(),false)) { check(v.pilih("Menu",0,1)==0,"10000 digit lalu0"); }
            try(var v=new ConsoleView(new StringReader("9".repeat(10001)+"\n0\n"),new StringWriter(),false)) { check(v.pilih("Menu",0,1)==0,"10001 karakter lalu0"); } pass("T125","input panjang ditolak, baris berikut tepat0");
            ManajemenData login=new ManajemenData(k,CLOCK);
            for(String raw:List.of("admin2026!","Admin2026?"," Admin2026!","Admin2026! ")) reject(()->login.login("admin",raw.toCharArray()),MasalahAplikasi.Jenis.LOGIN);
            Sesi good=login.login(" ADMIN ","Admin2026!".toCharArray()); login.logout(good); pass("T126","kapital/karakter/spasi password gagal; username trim/case sah");
            char[] pwd="PasswordSama2026!".toCharArray(); byte[] salt1=Kredensial.saltBaru(),salt2=Kredensial.saltBaru();
            Pengguna u1=new Pengguna("PGN998","Akun Pertama","pertama",Role.ADMIN,salt1,Kredensial.hash(pwd,salt1),Kredensial.ITERASI), u2=new Pengguna("PGN999","Akun Kedua","kedua",Role.ADMIN,salt2,Kredensial.hash(pwd,salt2),Kredensial.ITERASI);
            check(!Arrays.equals(u1.getSalt(),u2.getSalt()) && !Arrays.equals(u1.getHashPassword(),u2.getHashPassword()),"password sama salt/hash berbeda"); Arrays.fill(u1.getSalt(),(byte)0); Arrays.fill(u1.getHashPassword(),(byte)0); check(u1.verifikasiPassword(pwd) && u2.verifikasiPassword(pwd),"salinan defensif akun"); Arrays.fill(pwd,'\0'); pass("T129","dua akun password sama salt/hash berbeda dan immutable");
            try(Connection con=k.buka()) {
                for(String raw:List.of("' OR 1=1 --","admin'; DROP TABLE peserta; --")) { try { new PenggunaDAO().cariUsername(con,raw); throw new AssertionError("username SQL diterima"); } catch(IllegalArgumentException expected) { } }
                try { new PesertaDAO().cari(con,"PST001' OR 1=1",false); throw new AssertionError("ID SQL diterima"); } catch(IllegalArgumentException expected) { }
                check(new SertifikatDAO(CLOCK).cariNama(con,"' OR 1=1 --").isEmpty() && new PesertaDAO().semua(con).size()==ps.size()+2,"SQL payload literal, tidak mengubah data");
            } pass("T149","payload SQL login/ID ditolak, pencarian sebagai parameter literal");
            racePair(q,on); race999(); urutanNomor(p,on); kapasitas(); massal();
        } finally {
            for(String id:ss) a.hapusSertifikat(sa,id);
            for(String id:ls) a.hapusPelatihan(sa,id);
            for(String id:ps) a.hapusPeserta(sa,id);
            a.logout(sa); b.logout(sb);
        }
        try(Connection c=k.buka()) { check(new PesertaDAO().semua(c).size()==2 && new PelatihanDAO().semua(c).size()==2 && new SertifikatDAO(CLOCK).semua(c).size()==1,"fixture bersih"); }
        System.out.println("PASS: penutupan teknis; fixture bersih tanpa reset counter");
    }
    private static void pagination(Pelatihan l) {
        for(String suffix:List.of("Alpha","Beta","Gamma","Delta","Epsilon")) certificate(peserta("Pagination "+suffix,"pagination-"+suffix.toLowerCase()+"@example.com"),l,LocalDate.of(2026,10,8));
        for(int n:new int[]{0,1,5}) {
            String query=n==0?"Tidak Ada Hasil":n==1?"Pagination Alpha":"Pagination";
            String out=cli(admin()+"3\n6\n"+query+"\n0\n0\n");
            check(out.contains("— "+n+" data") && (n==0?out.contains("Belum ada data"):out.contains("Halaman 1/1")) && !out.contains("Halaman 2") && !out.contains("Halaman tujuan"),"pagination "+n);
        } pass("T114","fixture0/1/5 terpisah, enam data sebelumnya diuji pada UjiCLI");
    }
    private static void racePair(Peserta p,Pelatihan l) throws Exception {
        var first=a.pratinjauBaru(sa,p.getIdPeserta(),l.getIdPelatihan(),LocalDate.of(2026,10,8)); var second=b.pratinjauBaru(sb,p.getIdPeserta(),l.getIdPelatihan(),LocalDate.of(2026,10,8)); long before=nomor("SRT");
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Sertifikat>> futures=List.of(pool.submit(()->a.simpanSertifikat(sa,first)),pool.submit(()->b.simpanSertifikat(sb,second))); int ok=0,dupe=0;
            for(var f:futures) try { Sertifikat s=f.get(20,TimeUnit.SECONDS); ss.add(s.getIdSertifikat()); ok++; } catch(ExecutionException e) { if(e.getCause() instanceof MasalahAplikasi problem && problem.getJenis()==MasalahAplikasi.Jenis.DUPLIKAT) dupe++; else throw e; }
            check(ok==1 && dupe==1 && nomor("SRT")==before+1,"race pasangan satu commit dan counter kalah tetap");
        } pass("T144","race pasangan satu commit, satu duplikat, counter naik satu");
    }
    private static void race999() throws Exception {
        check(nomor("PST")<999,"counter fixture sebelum batas999"); setNomor("PST",999);
        var other=new ManajemenData(k,CLOCK); Sesi so=other.login("admin","Admin2026!".toCharArray());
        try(var pool=Executors.newFixedThreadPool(2)) {
            var one=pool.submit(()->a.tambahPeserta(sa,"Race Pertama","race-999-a@example.com")); var two=pool.submit(()->other.tambahPeserta(so,"Race Kedua","race-999-b@example.com"));
            Set<String> ids=new HashSet<>(); for(var f:List.of(one,two)) { Peserta p=f.get(20,TimeUnit.SECONDS); ids.add(p.getIdPeserta()); ps.add(p.getIdPeserta()); }
            check(ids.equals(Set.of("PST999","PST1000")) && nomor("PST")==1001,"race999 ke1000");
        } finally { other.logout(so); } pass("T145","dua transaksi berbeda mendapat999/1000, counter1001");
    }
    private static void urutanNomor(Peserta p,Pelatihan l) throws Exception {
        long n=nomor("PST"); reject(()->a.tambahPeserta(sa,"11","bad@example.com"),MasalahAplikasi.Jenis.VALIDASI);
        cli(admin()+"1\n3\n/batal\n0\n"); check(nomor("PST")==n,"PST gagal/batal"); Peserta first=peserta("Nomor Pertama","nomor-pst-a@example.com"); a.hapusPeserta(sa,first.getIdPeserta()); ps.remove(first.getIdPeserta()); Peserta next=peserta("Nomor Kedua","nomor-pst-b@example.com"); check(Long.parseLong(next.getIdPeserta().substring(3))==n+1,"PST hapus tidak reuse");
        n=nomor("PLT"); reject(()->a.tambahOnline(sa,"Nomor Uji",0,"Instruktur Uji","Zoom","https://example.com"),MasalahAplikasi.Jenis.VALIDASI); cli(admin()+"2\n3\n/batal\n0\n"); check(nomor("PLT")==n,"PLT gagal/batal"); Pelatihan x=online("Nomor Pertama"); a.hapusPelatihan(sa,x.getIdPelatihan()); ls.remove(x.getIdPelatihan()); Pelatihan y=offline("Nomor Kedua"); check(Long.parseLong(y.getIdPelatihan().substring(3))==n+1,"PLT hapus tidak reuse");
        Peserta unused=peserta("Nomor Sertifikat","nomor-srt@example.com"); n=nomor("SRT"); reject(()->a.pratinjauBaru(sa,unused.getIdPeserta(),l.getIdPelatihan(),LocalDate.of(2026,10,9)),MasalahAplikasi.Jenis.VALIDASI); a.pratinjauBaru(sa,unused.getIdPeserta(),l.getIdPelatihan(),LocalDate.of(2026,10,8)); check(nomor("SRT")==n,"SRT gagal/preview batal"); Sertifikat s=certificate(unused,l,LocalDate.of(2026,10,8)); a.hapusSertifikat(sa,s.getIdSertifikat()); ss.remove(s.getIdSertifikat()); Sertifikat t=certificate(unused,l,LocalDate.of(2026,10,8)); check(Long.parseLong(t.getIdSertifikat().substring(3))==n+1,"SRT hapus tidak reuse"); pass("T120","gagal/batal → sukses → hapus → sukses, ketiga prefix");
    }
    private static void kapasitas() throws Exception {
        Peserta p=peserta("Kapasitas Peserta","kapasitas@example.com"); Pelatihan l=online("Kapasitas Pelatihan"); var token=a.pratinjauBaru(sa,p.getIdPeserta(),l.getIdPelatihan(),LocalDate.of(2026,10,8)); Statistik before=a.statistik(sa);
        for(String kind:List.of("PST","PLT","SRT")) { long old=nomor(kind); try { setNomor(kind,Long.MAX_VALUE); reject(()->{switch(kind) { case "PST"->a.tambahPeserta(sa,"Kapasitas Gagal","kapasitas-gagal@example.com"); case "PLT"->a.tambahOnline(sa,"Kapasitas Gagal",1,"Instruktur Uji","Zoom","https://example.com"); case "SRT"->a.simpanSertifikat(sa,token); }},MasalahAplikasi.Jenis.VALIDASI); check(before.equals(a.statistik(sa)),"kapasitas tanpa mutasi "+kind); } finally { setNomor(kind,old); } }
        pass("T122","facade ketiga prefix menolak Long.MAX_VALUE tanpa mutasi");
    }
    private static void massal() throws Exception {
        List<Long> before=List.of(nomor("PST"),nomor("PLT"),nomor("SRT")); Statistik counts=a.statistik(sa);
        try(Connection c=k.buka()) {
            c.setAutoCommit(false); try {
                try(Statement st=c.createStatement()) { st.executeUpdate("UPDATE nomor_id SET nomor_berikut=200000"); }
                NomorDAO no=new NomorDAO(); PesertaDAO pd=new PesertaDAO(); PelatihanDAO ld=new PelatihanDAO(); SertifikatDAO sd=new SertifikatDAO(CLOCK);
                Set<String> pids=new HashSet<>(),lids=new HashSet<>(),sids=new HashSet<>();
                for(int i=0;i<1001;i++) {
                    long pn=no.kunciNomor(c,"PST"),ln=no.kunciNomor(c,"PLT"),sn=no.kunciNomor(c,"SRT");
                    Peserta p=new Peserta(Validasi.formatId("PST",pn),"Peserta Massal","massal-"+i+"@example.com"); Pelatihan l=new PelatihanOnline(Validasi.formatId("PLT",ln),"Pelatihan Massal",1,"Instruktur Massal","Zoom","https://example.com");
                    pd.tambah(c,p); ld.tambah(c,l); sd.tambah(c,new Sertifikat(Validasi.formatId("SRT",sn),p,l,sa.getPengguna(),LocalDate.of(2026,10,8),SnapshotSertifikat.dari(p,l),CLOCK));
                    no.majukan(c,"PST",pn); no.majukan(c,"PLT",ln); no.majukan(c,"SRT",sn); pids.add(p.getIdPeserta()); lids.add(l.getIdPelatihan()); sids.add(Validasi.formatId("SRT",sn));
                }
                check(pids.size()==1001 && lids.size()==1001 && sids.size()==1001 && no.kunciNomor(c,"SRT")==201001,"1001 insert tiap entitas dan counter");
                check(pd.semua(c).size()==counts.peserta()+1001 && ld.semua(c).size()==counts.pelatihanOnline()+counts.pelatihanOffline()+1001 && sd.semua(c).size()==counts.sertifikat()+1001,"roundtrip seluruh1001 entitas");
            } finally { c.rollback(); }
        }
        check(before.equals(List.of(nomor("PST"),nomor("PLT"),nomor("SRT"))) && counts.equals(a.statistik(sa)),"fixture massal rollback"); pass("T121","1001 insert/read PST/PLT/SRT, ID unik dan tidak terpotong, rollback fixture");
    }
}
