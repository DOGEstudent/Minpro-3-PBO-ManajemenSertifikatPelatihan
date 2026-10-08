package pengujian;

import config.Koneksi;
import model.*;
import util.Validasi;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public final class UjiDAO {
    private static int checks;
    private static final PesertaDAO PESERTA=new PesertaDAO();
    private static final PelatihanDAO PELATIHAN=new PelatihanDAO();
    private static final SertifikatDAO SERTIFIKAT=new SertifikatDAO();
    private static final NomorDAO NOMOR=new NomorDAO();
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); System.out.println("PASS "+(++checks)+": "+label); }
    @FunctionalInterface private interface SqlWork { void run() throws Exception; }
    private static void reject(SqlWork work,String state,int code,String label) throws Exception {
        try { work.run(); throw new AssertionError("Diterima: "+label); }
        catch(SQLException e) { check((state==null || state.equals(e.getSQLState())) && (code==0 || e.getErrorCode()==code),label); }
    }
    private static void sql(Connection c,String query) throws SQLException { try(Statement s=c.createStatement()) { s.executeUpdate(query); } }
    private static String value(Connection c,String query) throws SQLException { try(Statement s=c.createStatement(); ResultSet r=s.executeQuery(query)) { r.next(); return r.getString(1); } }
    public static void main(String[] args) throws Exception {
        try { jalankan(); } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void jalankan() throws Exception {
        Koneksi k=Koneksi.dariEnvironment();
        if(!k.getDatabase().equals("db_sertifikat_pelatihan_test")) throw new IllegalArgumentException("DAO test hanya boleh schema _test.");
        new LayananSetup(k).jalankan();
        try(Connection c=k.buka()) {
            PenggunaDAO akun=new PenggunaDAO(); Pengguna admin=akun.cariUsername(c," ADMIN ");
            check(admin!=null && admin.getRole()==Role.ADMIN && admin.getIdPengguna().equals("PGN001"),"mapping akun dan username normal");
            check(akun.cariId(c,"PGN002").getRole()==Role.PETUGAS_SERTIFIKAT && akun.cariUsername(c,"tidak-ada")==null,"akun petugas/akun tidak ditemukan");
            check(PESERTA.semua(c).size()==2,"ArrayList peserta dari query");
            ArrayList<Pelatihan> list=PELATIHAN.semua(c);
            check(list.size()==2 && list.get(0) instanceof PelatihanOnline && list.get(1) instanceof PelatihanOffline,"ResultSet dipetakan menjadi dua subclass");
            check(list.get(0).getDetailPelaksanaan().contains("Platform") && list.get(1).getDetailPelaksanaan().contains("Ruangan"),"override objek hasil DAO berjalan");
            check(SERTIFIKAT.semua(c).size()==1 && SERTIFIKAT.cari(c,"srt001",false).getSnapshot().getNamaPeserta().equals("Andi Pratama"),"sertifikat + snapshot + master + pencatat satu query");
            check(SERTIFIKAT.riwayatPeserta(c,"PST001").size()==1 && SERTIFIKAT.riwayatPeserta(c,"PST002").isEmpty(),"riwayat berdasarkan ID peserta");
            check(SERTIFIKAT.cariNama(c,"andi").size()==1 && SERTIFIKAT.cariNama(c,"%_' OR 1=1").isEmpty(),"pencarian snapshot sebagai parameter literal");
            check(PESERTA.cari(c,"PST999",false)==null && PELATIHAN.cari(c,"PLT999",false)==null && SERTIFIKAT.cari(c,"SRT999",false)==null,"ID hilang menghasilkan null");
            check(PESERTA.emailDipakai(c,"ANDI@EXAMPLE.COM",null) && !PESERTA.emailDipakai(c,"andi@example.com","PST001"),"email case-insensitive dan pengecualian diri");
            check(SERTIFIKAT.pasanganDipakai(c,"PST001","PLT001",null) && !SERTIFIKAT.pasanganDipakai(c,"PST001","PLT001","SRT001"),"pasangan berdasarkan ID, bukan referensi objek");
            check(PESERTA.referensiPenghalang(c,"PST001").equals("SRT001") && PELATIHAN.referensiPenghalang(c,"PLT001").equals("SRT001"),"ID sertifikat penghalang tersedia");
            Peserta p=PESERTA.cari(c,"PST001",false); p.setNama("Nama Lokal");
            check(PESERTA.cari(c,"PST001",false).getNama().equals("Andi Pratama"),"mutasi objek hasil baca tidak otomatis tersimpan");
            reject(() -> NOMOR.kunciNomor(c,"PST"),"25000",0,"lock tanpa transaksi ditolak");
            reject(() -> PESERTA.tambah(c,new Peserta("PST999","Nama Valid","valid@example.com")),"25000",0,"mutasi tanpa transaksi ditolak");
            transaksiCrud(c,admin);
            transaksiIntegritas(c,admin);
            check(!c.isClosed() && c.getAutoCommit(),"DAO tidak menutup Connection caller");
        }
        concurrency(k);
        currentRead(k);
        try(Connection c=k.buka()) { new SeedDAO().verifikasiTerinisialisasi(c); check(PESERTA.semua(c).size()==2,"fixture bersih setelah tes; counter tidak diturunkan"); }
        System.out.println("PASS: "+checks+" pemeriksaan DAO/counter (bukan seluruh160 kasus)");
    }
    private static void transaksiCrud(Connection c,Pengguna admin) throws Exception {
        c.setAutoCommit(false);
        try {
            Peserta p=new Peserta("PST999","O'Neill 日本","oneil@example.com"); PESERTA.tambah(c,p);
            check(PESERTA.cari(c,"PST999",true).getNama().equals("O'Neill 日本"),"insert/read peserta Unicode dan apostrof");
            p.setNama("Nama Baru"); p.setEmail("baru@example.com"); check(PESERTA.ubah(c,p)==1 && PESERTA.cari(c,"PST999",false).getEmailNormal().equals("baru@example.com"),"update peserta");
            PelatihanOnline on=new PelatihanOnline("PLT999","Data; DROP TABLE peserta",5,"Budi Santoso","Meet 2","https://example.com/a?x=1"); PELATIHAN.tambah(c,on);
            check(PELATIHAN.cari(c,"PLT999",true).getNama().equals(on.getNama()) && PESERTA.semua(c).size()==3,"teks menyerupai SQL disimpan sebagai data");
            PelatihanOffline off=new PelatihanOffline("PLT999","Basis Data",7,"Dewi Lestari","Gedung A","02");
            check(PELATIHAN.ubah(c,off)==1 && PELATIHAN.cari(c,"PLT999",false) instanceof PelatihanOffline,"update jenis membersihkan field subclass sebelumnya");
            Sertifikat certificate=new Sertifikat("SRT999",p,off,admin,LocalDate.now(),SnapshotSertifikat.dari(p,off));
            SERTIFIKAT.tambah(c,certificate);
            check(SERTIFIKAT.cari(c,"SRT999",true).getSnapshot().equals(certificate.getSnapshot()),"insert parent dan snapshot");
            Peserta other=PESERTA.cari(c,"PST002",true); Pelatihan otherTraining=PELATIHAN.cari(c,"PLT001",true);
            // DAO tidak mengubah pencatat walaupun candidate datang dari akun berbeda.
            Pengguna petugas=new PenggunaDAO().cariId(c,"PGN002");
            Sertifikat candidate=new Sertifikat("SRT999",other,otherTraining,petugas,LocalDate.now(),SnapshotSertifikat.dari(other,otherTraining));
            check(SERTIFIKAT.ubah(c,candidate)==1,"update parent dan seluruh snapshot");
            Sertifikat reloaded=SERTIFIKAT.cari(c,"SRT999",false);
            check(reloaded.getPencatat().getIdPengguna().equals(admin.getIdPengguna()) && reloaded.getSnapshot().equals(candidate.getSnapshot()),"pencatat awal tetap dan snapshot diperbarui");
            check(SERTIFIKAT.hapus(c,"SRT999")==1 && value(c,"SELECT COUNT(*) FROM snapshot_sertifikat WHERE sertifikat_id='SRT999'").equals("0"),"delete sertifikat cascade snapshot");
            check(PELATIHAN.hapus(c,"PLT999")==1 && PESERTA.hapus(c,"PST999")==1,"delete master tidak dirujuk");
            check(PESERTA.hapus(c,"PST999")==0 && PELATIHAN.hapus(c,"PLT999")==0 && SERTIFIKAT.hapus(c,"SRT999")==0,"affected rows nol untuk target hilang");
            check(PESERTA.ubah(c,p)==0 && PELATIHAN.ubah(c,off)==0 && SERTIFIKAT.ubah(c,candidate)==0,"update target hilang tidak mengklaim sukses");
            PESERTA.tambah(c,new Peserta("PST1000","Nama Seribu","seribu@example.com")); PESERTA.tambah(c,new Peserta("PST999","Nama Sembilan","sembilan@example.com"));
            ArrayList<Peserta> order=PESERTA.semua(c); check(order.get(order.size()-2).getIdPeserta().equals("PST999") && order.get(order.size()-1).getIdPeserta().equals("PST1000"),"sort ID numerik999 sebelum1000");
        } finally { c.rollback(); c.setAutoCommit(true); }
        check(PESERTA.semua(c).size()==2 && PELATIHAN.semua(c).size()==2 && SERTIFIKAT.semua(c).size()==1,"rollback seluruh rangkaian CRUD");
    }
    private static void transaksiIntegritas(Connection c,Pengguna admin) throws Exception {
        c.setAutoCommit(false);
        try {
            reject(() -> PESERTA.tambah(c,new Peserta("PST999","Duplikat","ANDI@example.com")),null,1062,"race email ditahan UNIQUE DB");
            reject(() -> PESERTA.hapus(c,"PST001"),null,1451,"FK tetap menolak delete master dirujuk");
            Peserta current=PESERTA.cari(c,"PST001",true); current.setNama("Nama Baru"); PESERTA.ubah(c,current);
            check(SERTIFIKAT.cari(c,"SRT001",false).getSnapshot().getNamaPeserta().equals("Andi Pratama"),"DAO membaca snapshot lama sesudah update master");
            check(SERTIFIKAT.cariNama(c,"Nama Baru").isEmpty() && SERTIFIKAT.cariNama(c,"Andi").size()==1,"pencarian memakai nama snapshot");
            sql(c,"DELETE FROM snapshot_sertifikat WHERE sertifikat_id='SRT001'");
            reject(() -> SERTIFIKAT.semua(c),"22000",0,"parent tanpa snapshot dilaporkan");
            reject(() -> SERTIFIKAT.cariNama(c,"tidakada"),"22000",0,"filter nama tidak menyembunyikan snapshot hilang");
            c.rollback();
            Sertifikat duplicate=new Sertifikat("SRT998",PESERTA.cari(c,"PST001",false),PELATIHAN.cari(c,"PLT001",false),admin,LocalDate.now(),SERTIFIKAT.cari(c,"SRT001",false).getSnapshot());
            reject(() -> SERTIFIKAT.tambah(c,duplicate),null,1062,"pasangan duplicate ditahan DB lewat DAO");
            c.rollback();
            Sertifikat correction=new Sertifikat("SRT001",PESERTA.cari(c,"PST002",true),PELATIHAN.cari(c,"PLT001",true),admin,LocalDate.now(),SnapshotSertifikat.dari(PESERTA.cari(c,"PST002",false),PELATIHAN.cari(c,"PLT001",false)));
            Connection updateFault=(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
                if(method.getName().equals("prepareStatement") && args[0].toString().startsWith("UPDATE snapshot_sertifikat")) throw new SQLException("INJECTED","ZZ002");
                try { return method.invoke(c,args); } catch(java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
            });
            reject(() -> SERTIFIKAT.ubah(updateFault,correction),"ZZ002",0,"gagal koreksi snapshot diteruskan ke caller");
            c.rollback();
            check(SERTIFIKAT.cari(c,"SRT001",false).getPeserta().getIdPeserta().equals("PST001") && SERTIFIKAT.cari(c,"SRT001",false).getSnapshot().getNamaPeserta().equals("Andi Pratama"),"rollback koreksi memulihkan parent dan snapshot");
            sql(c,"UPDATE peserta SET nama='11' WHERE id_peserta='PST001'");
            reject(() -> PESERTA.semua(c),"22000",0,"data DB melanggar domain ditolak saat hydration");
            c.rollback();
            long before=NOMOR.kunciNomor(c,"PST"); NOMOR.majukan(c,"PST",before);
            check(NOMOR.kunciNomor(c,"PST")==before+1,"counter maju tepat satu");
            reject(() -> NOMOR.majukan(c,"PST",before),"40001",0,"counter stale ditolak");
            c.rollback(); check(NOMOR.kunciNomor(c,"PST")==before,"rollback counter mengembalikan nomor");
            sql(c,"UPDATE nomor_id SET nomor_berikut=9223372036854775807 WHERE jenis='PST'");
            reject(() -> NOMOR.kunciNomor(c,"PST"),"22003",0,"counter long maksimal tidak overflow");
            c.rollback();
            Peserta p=PESERTA.cari(c,"PST002",true); Pelatihan l=PELATIHAN.cari(c,"PLT002",true);
            Sertifikat data=new Sertifikat("SRT999",p,l,admin,LocalDate.now(),SnapshotSertifikat.dari(p,l));
            Connection fault=(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
                if(method.getName().equals("prepareStatement") && args[0].toString().startsWith("INSERT INTO snapshot_sertifikat")) throw new SQLException("INJECTED","ZZ001");
                try { return method.invoke(c,args); } catch(java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
            });
            reject(() -> SERTIFIKAT.tambah(fault,data),"ZZ001",0,"gagal child diteruskan ke caller");
            check(!c.getAutoCommit() && value(c,"SELECT COUNT(*) FROM sertifikat WHERE id_sertifikat='SRT999'").equals("1"),"DAO tidak commit/rollback diam-diam");
            c.rollback(); check(SERTIFIKAT.cari(c,"SRT999",false)==null,"rollback caller menghapus parent parsial");
        } finally { c.rollback(); c.setAutoCommit(true); }
    }
    private static void concurrency(Koneksi k) throws Exception {
        Queue<String> created=new ConcurrentLinkedQueue<>();
        try(Connection first=k.buka(); var worker=Executors.newSingleThreadExecutor()) {
            first.setAutoCommit(false); long a=NOMOR.kunciNomor(first,"PST");
            String idA=Validasi.formatId("PST",a); CountDownLatch started=new CountDownLatch(1);
            Future<String> second=worker.submit(()->{
                try(Connection c=k.buka()) {
                    c.setAutoCommit(false); started.countDown();
                    try {
                        long b=NOMOR.kunciNomor(c,"PST"); String idB=Validasi.formatId("PST",b);
                        PESERTA.tambah(c,new Peserta(idB,"Peserta Kedua","uji-"+idB.toLowerCase(Locale.ROOT)+"@example.com")); NOMOR.majukan(c,"PST",b); c.commit(); created.add(idB); return idB;
                    } catch(Exception e) { c.rollback(); throw e; }
                }
            });
            if(!started.await(5,TimeUnit.SECONDS)) throw new AssertionError("Worker tidak mulai");
            try { second.get(300,TimeUnit.MILLISECONDS); throw new AssertionError("Counter tidak menunggu lock"); }
            catch(TimeoutException expected) { check(true,"koneksi kedua menunggu counter row lock"); }
            try {
                PESERTA.tambah(first,new Peserta(idA,"Peserta Pertama","uji-"+idA.toLowerCase(Locale.ROOT)+"@example.com")); NOMOR.majukan(first,"PST",a); first.commit(); created.add(idA);
                String idB=second.get(10,TimeUnit.SECONDS);
                check(!idA.equals(idB) && Long.parseLong(idB.substring(3))==a+1,"dua transaksi commit mendapat ID berbeda berurutan");
                try(Connection c=k.buka()) { check(PESERTA.cari(c,idA,false)!=null && PESERTA.cari(c,idB,false)!=null,"hasil commit terbaca pada koneksi baru"); }
            } finally { first.rollback(); }
        } finally {
            try(Connection c=k.buka()) {
                c.setAutoCommit(false);
                try { for(String id:created) PESERTA.hapus(c,id); c.commit(); }
                catch(Exception e) { c.rollback(); throw e; }
            }
        }
        try(Connection c=k.buka()) {
            c.setAutoCommit(false);
            try { long next=NOMOR.kunciNomor(c,"PST"); long max=created.stream().mapToLong(id->Long.parseLong(id.substring(3))).max().orElseThrow(); check(next>max,"delete data tidak memakai ulang nomor commit"); }
            finally { c.rollback(); }
        }
    }
    private static void currentRead(Koneksi k) throws Exception {
        try(Connection old=k.buka(); Connection writer=k.buka()) {
            old.setAutoCommit(false);
            String original=SERTIFIKAT.cari(old,"SRT001",false).getSnapshot().getNamaPeserta();
            try {
                try(PreparedStatement s=writer.prepareStatement("UPDATE snapshot_sertifikat SET nama_peserta=? WHERE sertifikat_id='SRT001'")) { s.setString(1,"Snapshot Terbaru"); s.executeUpdate(); }
                check(SERTIFIKAT.cari(old,"SRT001",true).getSnapshot().getNamaPeserta().equals("Snapshot Terbaru"),"lock read mendapat snapshot terbaru walau read view lama sudah ada");
            } finally {
                old.rollback();
                try(PreparedStatement s=writer.prepareStatement("UPDATE snapshot_sertifikat SET nama_peserta=? WHERE sertifikat_id='SRT001'")) { s.setString(1,original); s.executeUpdate(); }
            }
        }
    }
}
