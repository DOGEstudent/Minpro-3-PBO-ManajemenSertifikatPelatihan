package pengujian;

import config.Koneksi;
import controller.SertifikatController;
import model.*;
import view.ConsoleView;
import java.io.*;
import java.time.*;
import java.sql.*;
import java.util.*;

public final class UjiPembatalan {
    private static Koneksi k;
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneId.of("Asia/Singapore"));
    private static int probes;
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    private static String run(String script) {
        StringWriter output=new StringWriter();
        try(var v=new ConsoleView(new StringReader(script),output,false)) { new SertifikatController(new ManajemenData(k,CLOCK),v,CLOCK).jalankan(); }
        probes++; return output.toString();
    }
    private static String login() { return "1\nadmin\nAdmin2026!\n"; }
    private static List<Long> counters() throws Exception {
        try(Connection c=k.buka()) { c.setAutoCommit(false); try { NomorDAO d=new NomorDAO(); return List.of(d.kunciNomor(c,"PST"),d.kunciNomor(c,"PLT"),d.kunciNomor(c,"SRT")); } finally { c.rollback(); } }
    }
    private record Form(String label,String prefix,List<String> fields) { }
    public static void main(String[] args) throws Exception {
        try { uji(); } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
    private static void uji() throws Exception {
        k=Koneksi.dariEnvironment(); if(!k.getDatabase().equals("db_sertifikat_pelatihan_test")) throw new IllegalArgumentException("Hanya schema test.");
        var m=new ManajemenData(k,CLOCK); Sesi s=m.login("admin","Admin2026!".toCharArray());
        Peserta p=m.tambahPeserta(s,"Peserta Batal","batal-original@example.com");
        Pelatihan a=m.tambahOnline(s,"Online Batal",7,"Instruktur Batal","Zoom","https://example.com");
        Pelatihan b=m.tambahOffline(s,"Offline Batal",8,"Instruktur Batal","Gedung A","A-101");
        Sertifikat original=m.cariSertifikat(s,"SRT001");
        try {
            List<Form> forms=List.of(
                new Form("peserta tambah","1\n3\n",List.of("Calon Peserta","batal-calon@example.com")),
                new Form("peserta ubah","1\n4\n",List.of(p.getIdPeserta(),"Calon Peserta","batal-calon@example.com")),
                new Form("online tambah","2\n3\n",List.of("1","Online Calon","9","Instruktur Calon","Meet","https://example.com/calon")),
                new Form("online ubah","2\n4\n",List.of(a.getIdPelatihan(),"1","Online Calon","9","Instruktur Calon","Meet","https://example.com/calon")),
                new Form("offline tambah","2\n3\n",List.of("2","Offline Calon","9","Instruktur Calon","Gedung B","B-101")),
                new Form("offline ubah","2\n4\n",List.of(b.getIdPelatihan(),"2","Offline Calon","9","Instruktur Calon","Gedung B","B-101")),
                new Form("sertifikat tambah","3\n3\n",List.of(p.getIdPeserta(),a.getIdPelatihan(),"2026-10-08")),
                new Form("sertifikat koreksi","3\n4\n",List.of("SRT001",p.getIdPeserta(),b.getIdPelatihan(),"2026-10-08")));
            List<Long> before=counters(); Statistik counts=m.statistik(s);
            for(Form form:forms) {
                for(int i=0;i<form.fields.size();i++) {
                    String prefix=login()+form.prefix+String.join("\n",form.fields.subList(0,i))+(i>0?"\n":"");
                    check(run(prefix+"/batal\n0\n").contains("Formulir dibatalkan."),form.label+" batal field "+i);
                    check(run(prefix).contains("Input berakhir."),form.label+" EOF field "+i);
                    check(before.equals(counters()) && counts.equals(m.statistik(s)),form.label+" batal/EOF tanpa mutasi counter/jumlah");
                    check(m.cariPeserta(s,p.getIdPeserta()).getNama().equals("Peserta Batal"),"peserta lama tetap");
                    check(m.cariPelatihan(s,a.getIdPelatihan()).getNama().equals("Online Batal") && m.cariPelatihan(s,b.getIdPelatihan()).getNama().equals("Offline Batal"),"pelatihan lama tetap");
                    Sertifikat after=m.cariSertifikat(s,"SRT001");
                    check(after.getSnapshot().equals(original.getSnapshot()) && after.getTanggalTerbit().equals(original.getTanggalTerbit()) && after.getPeserta().getIdPeserta().equals(original.getPeserta().getIdPeserta()) && after.getPelatihan().getIdPelatihan().equals(original.getPelatihan().getIdPelatihan()),"paket sertifikat lama tetap");
                }
            }
            System.out.println("PASS PENERIMAAN T110 /batal setiap posisi field tambah/ubah");
            List<String> confirmations=new ArrayList<>();
            confirmations.add("3\n3\n"+p.getIdPeserta()+"\n"+a.getIdPelatihan()+"\n2026-10-08\n");
            confirmations.add("3\n4\nSRT001\n"+p.getIdPeserta()+"\n"+b.getIdPelatihan()+"\n2026-10-08\n");
            confirmations.add("3\n5\nSRT001\n"); confirmations.add("1\n5\n"+p.getIdPeserta()+"\n"); confirmations.add("2\n5\n"+a.getIdPelatihan()+"\n"); confirmations.add("2\n5\n"+b.getIdPelatihan()+"\n");
            for(String form:confirmations) {
                check(run(login()+form+"t\n0\n0\n").contains("Perubahan dibatalkan"),"konfirmasi t");
                check(run(login()+form+"/batal\n0\n").contains("Formulir dibatalkan"),"konfirmasi /batal");
                check(run(login()+form).contains("Input berakhir"),"konfirmasi EOF");
                check(before.equals(counters()) && counts.equals(m.statistik(s)),"konfirmasi tidak memutasi");
            }
            System.out.println("PASS PENERIMAAN T111 t dan /batal pada preview tambah/koreksi/hapus");
            for(String prefix:List.of("","1\n","1\nadmin\n")) check(run(prefix).contains("Input berakhir"),"EOF tahap login");
            System.out.println("PASS PENERIMAAN T112 EOF setiap field/login/konfirmasi");
            StringWriter output=new StringWriter(); try(var v=new ConsoleView(new StringReader("yes\n1\n\nY\n"),output,false)) { check(v.konfirmasi("Simpan"),"Y sah setelah tiga retry"); }
            check(output.toString().split("Simpan ",-1).length==5,"empat prompt konfirmasi");
            System.out.println("PASS PENERIMAAN T113 yes/1/blank retry lalu Y");
            System.out.println("PASS: "+probes+" sesi pembatalan/EOF untuk T110-T113");
        } finally { m.hapusPelatihan(s,a.getIdPelatihan()); m.hapusPelatihan(s,b.getIdPelatihan()); m.hapusPeserta(s,p.getIdPeserta()); m.logout(s); }
    }
}
