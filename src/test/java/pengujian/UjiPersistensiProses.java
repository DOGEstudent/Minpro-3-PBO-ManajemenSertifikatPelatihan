package pengujian;

import config.Koneksi;
import model.*;
import java.nio.file.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class UjiPersistensiProses {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneId.of("Asia/Singapore"));
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    private static void reject(Runnable work,MasalahAplikasi.Jenis jenis) { try { work.run(); throw new AssertionError("Diterima"); } catch(MasalahAplikasi e) { check(e.getJenis()==jenis,"jenis "+e.getJenis()); } }
    private static String packet(Sertifikat s) { SnapshotSertifikat x=s.getSnapshot(); return s.getIdSertifikat()+"|"+s.getPeserta().getIdPeserta()+"|"+s.getPelatihan().getIdPelatihan()+"|"+s.getPencatat().getIdPengguna()+"|"+s.getTanggalTerbit()+"|"+x.getNamaPeserta()+"|"+x.getEmailPeserta()+"|"+x.getNamaPelatihan()+"|"+x.getJenisPelatihan()+"|"+x.getDurasi()+"|"+x.getInstruktur()+"|"+x.getDetailPelaksanaan(); }
    private static String counters(Koneksi k) throws Exception { try(Connection c=k.buka(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT jenis,nomor_berikut FROM nomor_id ORDER BY jenis")) { StringBuilder b=new StringBuilder(); while(r.next()) b.append(r.getString(1)).append(':').append(r.getLong(2)).append(';'); return b.toString(); } }
    public static void main(String[] args) throws Exception {
        try { run(args); } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void run(String[] args) throws Exception {
        check("3307".equals(System.getenv("DB_PORT")),"hanya fixture3307"); Koneksi k=Koneksi.dariEnvironment(); check(k.getDatabase().equals("db_sertifikat_pelatihan_test"),"schema test");
        Path work=Path.of(args[1]).toAbsolutePath().normalize(); check(work.startsWith(Path.of("D:/MK/PBO/Asleb/UntukCodex/_audit").toAbsolutePath().normalize()),"folder audit wajib"); Path file=work.resolve("persistensi.properties");
        var m=new ManajemenData(k,CLOCK); reject(()->m.semuaPeserta(null),MasalahAplikasi.Jenis.SESI); Sesi admin=m.login("admin","Admin2026!".toCharArray());
        if(args[0].equals("buat")) {
            Peserta p=m.tambahPeserta(admin,"Persistensi Peserta","persistensi@example.com"); Pelatihan l=m.tambahOnline(admin,"Persistensi Java",7,"Instruktur Persistensi","Zoom","https://example.com/persistensi");
            Sertifikat s=m.simpanSertifikat(admin,m.pratinjauBaru(admin,p.getIdPeserta(),l.getIdPelatihan(),LocalDate.of(2026,10,8)));
            m.ubahPeserta(admin,p.getIdPeserta(),"Persistensi Terbaru","persistensi-baru@example.com"); m.ubahOnline(admin,l.getIdPelatihan(),"Java Terbaru",9,"Instruktur Terbaru","Meet","https://example.com/baru");
            Properties state=new Properties(); state.setProperty("peserta",p.getIdPeserta()); state.setProperty("pelatihan",l.getIdPelatihan()); state.setProperty("sertifikat",s.getIdSertifikat()); state.setProperty("packet",packet(s)); state.setProperty("counters",counters(k)); state.setProperty("jumlah",m.statistik(admin).toString());
            try(OutputStream out=Files.newOutputStream(file)) { state.store(out,"Fixture proses baru dan restart server; hanya schema test3307"); }
            m.logout(admin); System.out.println("PASS proses buat: commit master/sertifikat/snapshot, counter disimpan, sesi ditutup"); return;
        }
        Properties state=new Properties(); try(InputStream in=Files.newInputStream(file)) { state.load(in); }
        if(args[0].equals("gangguan")) {
            Files.writeString(work.resolve("siap-stop.txt"),"Login sah pada fixture3307");
            long deadline=System.nanoTime()+60_000_000_000L;
            while(!Files.exists(work.resolve("server-mati.txt"))) { if(System.nanoTime()>deadline) throw new AssertionError("Server belum dihentikan"); Thread.sleep(100); }
            reject(()->m.semuaPeserta(admin),MasalahAplikasi.Jenis.TEKNIS);
            reject(()->m.tambahPeserta(admin,"Tidak Tersimpan","gangguan-tidak-tersimpan@example.com"),MasalahAplikasi.Jenis.TEKNIS);
            reject(()->m.login("admin","Admin2026!".toCharArray()),MasalahAplikasi.Jenis.TEKNIS);
            reject(()->m.semuaPeserta(admin),MasalahAplikasi.Jenis.SESI);
            System.out.println("PASS gangguan login/read/mutasi: MySQL fixture benar-benar mati, tanpa sukses palsu; sesi dicabut oleh login gagal"); return;
        }
        check(packet(m.cariSertifikat(admin,state.getProperty("sertifikat"))).equals(state.getProperty("packet")),"snapshot/ID/pencatat/tanggal utuh");
        check(m.cariPeserta(admin,state.getProperty("peserta")).getNama().equals("Persistensi Terbaru") && m.cariPelatihan(admin,state.getProperty("pelatihan")).getNama().equals("Java Terbaru"),"master terbaru terpisah dari snapshot");
        check(counters(k).equals(state.getProperty("counters")) && m.statistik(admin).toString().equals(state.getProperty("jumlah")),"counter/jumlah tidak berubah atau reseed");
        check(m.cariNamaSertifikat(admin,"Persistensi Peserta").size()==1 && m.cariNamaSertifikat(admin,"Persistensi Terbaru").isEmpty(),"pencarian historis setelah restart");
        if(args[0].equals("baca")) { m.logout(admin); System.out.println("PASS proses baca: proses/koneksi/sesi baru, data/snapshot/counter identik, tanpa reseed"); }
        else if(args[0].equals("bersih")) { m.hapusSertifikat(admin,state.getProperty("sertifikat")); m.hapusPelatihan(admin,state.getProperty("pelatihan")); m.hapusPeserta(admin,state.getProperty("peserta")); check(m.semuaPeserta(admin).size()==2 && m.semuaPelatihan(admin).size()==2 && m.semuaSertifikat(admin).size()==1,"fixture bersih"); m.logout(admin); System.out.println("PASS proses bersih: fixture kembali dummy, counter tidak diturunkan"); }
        else throw new IllegalArgumentException("Mode fixture tidak dikenal");
    }
}
