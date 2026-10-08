package pengujian;

import config.Koneksi;
import controller.SertifikatController;
import model.*;
import view.ConsoleView;
import java.io.*;
import java.time.*;
import java.sql.*;
import java.util.*;

/** Skrip melewati controller/view nyata; fixture dibatasi schema test. */
public final class UjiCLI {
    private static int checks;
    private static Koneksi koneksi;
    private static final Clock CLOCK=Clock.systemDefaultZone();
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); System.out.println("PASS CLI "+(++checks)+": "+label); }
    private static String run(String script) {
        StringWriter output=new StringWriter();
        try(var view=new ConsoleView(new StringReader(script),output,false)) { new SertifikatController(new ManajemenData(koneksi,CLOCK),view,CLOCK).jalankan(); }
        String text=output.toString();
        if(text.contains("Exception") || text.contains("jdbc:") || text.contains("Admin2026!") || text.contains("Petugas2026!")) throw new AssertionError("Detail internal/password tercetak");
        return text;
    }
    private static String admin() { return "1\nadmin\nAdmin2026!\n"; }
    private static long nomor(String jenis) throws Exception {
        try(Connection c=koneksi.buka()) { c.setAutoCommit(false); try { return new NomorDAO().kunciNomor(c,jenis); } finally { c.rollback(); } }
    }
    public static void main(String[] args) throws Exception {
        try { jalankan(); } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void jalankan() throws Exception {
        koneksi=Koneksi.dariEnvironment();
        if(!koneksi.getDatabase().equals("db_sertifikat_pelatihan_test")) throw new IllegalArgumentException("CLI test hanya untuk schema _test.");
        var model=new ManajemenData(koneksi,CLOCK); model.periksaKesiapan();
        Sesi sesi=model.login("admin","Admin2026!".toCharArray());
        List<String> peserta=new ArrayList<>(), pelatihan=new ArrayList<>(), sertifikat=new ArrayList<>();
        try {
            String out=run("abc\n9999999999999\n1\nadmin\nPasswordSalah!\n"+admin()+"4\n5\n0\n");
            check(out.contains("Login berhasil") && out.contains("Logout berhasil") && out.contains("Peserta: 2"),"retry login/menu, statistik dan logout");
            out=run("1\npetugas\nPetugas2026!\n1\n3\n1\n0\n0\n");
            check(out.contains("Pilihan") && !out.contains("3. Tambah") && out.contains("Daftar peserta"),"menu Petugas menolak tambah master dan tetap dapat membaca");
            long np=nomor("PST");
            out=run(admin()+"1\n3\n11\nAulia1\nAulia@\nPeserta CLI\nabc\nANDI@EXAMPLE.COM\ncli-test@example.com\n0\n0\n");
            Peserta p=model.semuaPeserta(sesi).stream().filter(x->x.getEmail().equals("cli-test@example.com")).findFirst().orElseThrow(); peserta.add(p.getIdPeserta());
            check(out.contains("Ulangi email") && out.contains("Peserta disimpan"),"nama/email invalid dan duplikat diulang pada formulir");
            check(nomor("PST")==np+1,"ID peserta otomatis hanya satu kenaikan setelah sukses");
            long nl=nomor("PLT");
            out=run(admin()+"2\n3\n1\nJava CLI\nabc\n12abc\n1.5\n2147483648\n0\n7\n123\nInstruktur CLI\nZoom\nftp://example.com\nhttps://example.com/cli\n0\n0\n");
            Pelatihan l=model.semuaPelatihan(sesi).stream().filter(x->x.getNama().equals("Java CLI")).findFirst().orElseThrow(); pelatihan.add(l.getIdPelatihan());
            check(l.getDurasi()==7 && out.contains("Pelatihan disimpan"),"durasi teks/campuran/desimal/overflow dan URL invalid diulang");
            check(nomor("PLT")==nl+1,"ID pelatihan otomatis setelah input valid");
            String date=LocalDate.now(CLOCK).toString(), form=p.getIdPeserta()+"\n"+l.getIdPelatihan()+"\n"+date+"\n";
            long ns=nomor("SRT");
            out=run(admin()+"3\n3\n"+form+"t\n0\n0\n");
            check(out.contains("Pratinjau") && out.contains("Perubahan dibatalkan") && nomor("SRT")==ns,"konfirmasi t tidak menyimpan atau mengambil ID");
            out=run(admin()+"3\n3\n/batal\n0\n");
            check(out.contains("Formulir dibatalkan") && nomor("SRT")==ns,"/batal kembali ke menu utama tanpa mutasi");
            out=run(admin()+"3\n3\n"+form);
            check(out.contains("Input berakhir") && nomor("SRT")==ns,"EOF saat konfirmasi membatalkan preview");
            out=run(admin()+"3\n3\n123\nPST999999999\n"+p.getIdPeserta()+"\n"+l.getIdPelatihan()+"\n2099-01-01\n"+date+"\ny\n0\n0\n");
            Sertifikat s=model.semuaSertifikat(sesi).stream().filter(x->x.getPeserta().getIdPeserta().equals(p.getIdPeserta())).findFirst().orElseThrow(); sertifikat.add(s.getIdSertifikat());
            check(out.contains("Ulangi ID") && out.contains("Sertifikat disimpan") && nomor("SRT")==ns+1,"retry ID hilang/invalid dan tanggal masa depan; simpan tepat satu ID");
            model.ubahPeserta(sesi,p.getIdPeserta(),"Nama Sesudah","cli-test@example.com");
            out=run(admin()+"3\n2\n"+s.getIdSertifikat()+"\n6\nPeserta CLI\n7\n"+p.getIdPeserta()+"\n0\n0\n");
            check(out.contains("Snapshot peserta: Peserta CLI") && out.contains("Master terkini: Nama Sesudah"),"detail, pencarian, riwayat tetap memakai snapshot historis");
            var restart=new ManajemenData(koneksi); Sesi rs=restart.login("petugas","Petugas2026!".toCharArray());
            check(restart.cariSertifikat(rs,s.getIdSertifikat()).getSnapshot().getNamaPeserta().equals("Peserta CLI"),"instance baru membaca sertifikat permanen"); restart.logout(rs);
            out=run(admin()+"3\n4\n"+s.getIdSertifikat()+"\n"+form+"y\n0\n0\n");
            check(out.contains("Data lama — ID peserta:") && out.contains("Snapshot peserta: Peserta CLI") && out.contains("Snapshot peserta: Nama Sesudah"),"koreksi menampilkan snapshot lama dan calon lengkap");
            check(model.cariSertifikat(sesi,s.getIdSertifikat()).getSnapshot().getNamaPeserta().equals("Nama Sesudah") && nomor("SRT")==ns+1,"koreksi mengganti snapshot tanpa mengganti ID");
            out=run(admin()+"1\n5\n"+p.getIdPeserta()+"\ny\n0\n");
            check(model.cariPeserta(sesi,p.getIdPeserta())!=null && !out.contains("Peserta dihapus."),"penghapusan master yang dirujuk ditolak melalui CLI");
            out=run("1\npetugas\nPetugas2026!\n3\n5\n"+s.getIdSertifikat()+"\nt\n5\n"+s.getIdSertifikat()+"\ny\n0\n0\n");
            check(out.contains("Perubahan dibatalkan") && out.contains("Sertifikat dihapus."),"Petugas menghapus sertifikat setelah konfirmasi y"); sertifikat.clear();
            check(model.semuaSertifikat(sesi).size()==1,"hapus sertifikat permanen terverifikasi");
            out=run(admin()+"2\n4\n"+l.getIdPelatihan()+"\n2\nJava Offline CLI\n8\nInstruktur CLI\nGedung A\nA-101\n0\n0\n");
            check(model.cariPelatihan(sesi,l.getIdPelatihan()) instanceof PelatihanOffline && out.contains("Pelatihan disimpan"),"ubah Online menjadi Offline lewat controller");
            for(String suffix:List.of("a","b","c")) peserta.add(model.tambahPeserta(sesi,"Peserta Halaman","cli-page-"+suffix+"@example.com").getIdPeserta());
            out=run(admin()+"1\n1\nabc\n999\n2\n0\n0\n0\n");
            check(out.contains("Halaman 1/2") && out.contains("Halaman 2/2"),"pagination lima baris serta retry halaman invalid");
            out=run(admin()+"2\n5\n"+l.getIdPelatihan()+"\ny\n0\n1\n5\n"+p.getIdPeserta()+"\ny\n0\n0\n");
            check(out.contains("Pelatihan dihapus.") && out.contains("Peserta dihapus."),"hapus master bebas referensi setelah konfirmasi"); pelatihan.clear(); peserta.remove(p.getIdPeserta());
            check(run("").contains("Input berakhir"),"EOF sebelum login keluar terkendali");
        } finally {
            for(String id:sertifikat) model.hapusSertifikat(sesi,id);
            for(String id:pelatihan) model.hapusPelatihan(sesi,id);
            for(String id:peserta) model.hapusPeserta(sesi,id);
            model.logout(sesi);
        }
        System.out.println("PASS: "+checks+" pemeriksaan CLI/controller dengan MySQL");
    }
}
