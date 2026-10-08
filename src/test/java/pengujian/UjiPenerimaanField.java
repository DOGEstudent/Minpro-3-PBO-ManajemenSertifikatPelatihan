package pengujian;

import config.Koneksi;
import controller.SertifikatController;
import model.*;
import util.*;
import view.ConsoleView;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.sql.*;
import java.util.*;

/** Fixture expected diturunkan dari spesifikasi, bukan dari hasil validator. */
public final class UjiPenerimaanField {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneId.of("Asia/Singapore"));
    private static Koneksi k;
    private static ManajemenData m;
    private static Sesi sesi;
    private record Kasus(String id,String field,String raw,boolean sah) { }
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    private static boolean diterima(Runnable r) {
        try { r.run(); return true; }
        catch(IllegalArgumentException e) { return false; }
        catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.VALIDASI) throw e; return false; }
    }
    private static String[] nilai(Kasus t,boolean fallback) {
        String[] v={"Peserta Uji","penerimaan@example.com","Java Uji","7","Instruktur Uji","Zoom","https://example.com","Gedung A","A-101","2026-10-08"};
        int i=switch(t.field) { case "peserta"->0; case "email"->1; case "judul"->2; case "durasi"->3; case "instruktur"->4; case "platform"->5; case "tautan"->6; case "lokasi"->7; case "ruangan"->8; case "tanggal"->9; default->throw new AssertionError(t.field); };
        if(!fallback || t.sah) v[i]=t.raw;
        return v;
    }
    private static String field(Kasus t,String[] v) {
        return switch(t.field) { case "peserta"->v[0]; case "email"->v[1]; case "judul"->v[2]; case "durasi"->v[3]; case "instruktur"->v[4]; case "platform"->v[5]; case "tautan"->v[6]; case "lokasi"->v[7]; case "ruangan"->v[8]; case "tanggal"->v[9]; default->throw new AssertionError(); };
    }
    private static boolean orang(Kasus t) { return t.field.equals("peserta") || t.field.equals("email"); }
    private static boolean offline(Kasus t) { return t.field.equals("lokasi") || t.field.equals("ruangan"); }
    private static Pelatihan latihan(Kasus t,String id,String[] v) {
        int durasi=Validasi.bilangan(v[3],1,500);
        return offline(t)?new PelatihanOffline(id,v[2],durasi,v[4],v[7],v[8]):new PelatihanOnline(id,v[2],durasi,v[4],v[5],v[6]);
    }
    private static String get(Kasus t,Object obj) {
        return switch(t.field) {
            case "peserta"->((Peserta)obj).getNama(); case "email"->((Peserta)obj).getEmail();
            case "judul"->((Pelatihan)obj).getNama(); case "durasi"->Integer.toString(((Pelatihan)obj).getDurasi());
            case "instruktur"->((Pelatihan)obj).getInstruktur(); case "platform"->((PelatihanOnline)obj).getPlatform();
            case "tautan"->((PelatihanOnline)obj).getTautan(); case "lokasi"->((PelatihanOffline)obj).getLokasi(); case "ruangan"->((PelatihanOffline)obj).getRuangan(); default->throw new AssertionError();
        };
    }
    private static void setter(Kasus t,Object obj) {
        switch(t.field) {
            case "peserta"->((Peserta)obj).setNama(t.raw); case "email"->((Peserta)obj).setEmail(t.raw);
            case "judul"->((Pelatihan)obj).setNama(t.raw); case "durasi"->((Pelatihan)obj).setDurasi(Validasi.bilangan(t.raw,1,500));
            case "instruktur"->((Pelatihan)obj).setInstruktur(t.raw); case "platform"->((PelatihanOnline)obj).setPlatform(t.raw);
            case "tautan"->((PelatihanOnline)obj).setTautan(t.raw); case "lokasi"->((PelatihanOffline)obj).setLokasi(t.raw); case "ruangan"->((PelatihanOffline)obj).setRuangan(t.raw);
            default->throw new AssertionError();
        }
    }
    private static Object tambah(Kasus t,String[] v) {
        if(orang(t)) return m.tambahPeserta(sesi,v[0],v[1]);
        int d=Validasi.bilangan(v[3],1,500);
        return offline(t)?m.tambahOffline(sesi,v[2],d,v[4],v[7],v[8]):m.tambahOnline(sesi,v[2],d,v[4],v[5],v[6]);
    }
    private static Object ubah(Kasus t,String id,String[] v) {
        if(orang(t)) return m.ubahPeserta(sesi,id,v[0],v[1]);
        int d=Validasi.bilangan(v[3],1,500);
        return offline(t)?m.ubahOffline(sesi,id,v[2],d,v[4],v[7],v[8]):m.ubahOnline(sesi,id,v[2],d,v[4],v[5],v[6]);
    }
    private static String id(Object o) { return o instanceof Peserta p?p.getIdPeserta():((Pelatihan)o).getIdPelatihan(); }
    private static Object baca(Kasus t,String id) { return orang(t)?m.cariPeserta(sesi,id):m.cariPelatihan(sesi,id); }
    private static void hapus(Kasus t,String id) { if(orang(t)) m.hapusPeserta(sesi,id); else m.hapusPelatihan(sesi,id); }
    private static long nomor(String jenis) throws Exception {
        try(Connection c=k.buka()) { c.setAutoCommit(false); try { return new NomorDAO().kunciNomor(c,jenis); } finally { c.rollback(); } }
    }
    private static String formulir(Kasus t) {
        String[] v=nilai(t,true); List<String> lines=new ArrayList<>();
        if(orang(t)) { lines.add(v[0]); lines.add(v[1]); }
        else { lines.add(offline(t)?"2":"1"); lines.add(v[2]); lines.add(v[3]); lines.add(v[4]); lines.add(offline(t)?v[7]:v[5]); lines.add(offline(t)?v[8]:v[6]); }
        if(!t.sah) {
            int pos=switch(t.field) { case "peserta"->0; case "email"->1; case "judul"->1; case "durasi"->2; case "instruktur"->3; case "platform","lokasi"->4; case "tautan","ruangan"->5; default->throw new AssertionError(); };
            lines.add(pos,t.raw);
        }
        return String.join("\n",lines)+"\n";
    }
    private static String cli(String script) {
        StringWriter output=new StringWriter();
        try(var view=new ConsoleView(new StringReader("1\nadmin\nAdmin2026!\n"+script),output,false)) { new SertifikatController(new ManajemenData(k,CLOCK),view,CLOCK).jalankan(); }
        String text=output.toString(); check(!text.contains("Input berakhir") && !text.contains("Exception") && !text.contains("Admin2026!"),"CLI tidak terputus/membocorkan password"); return text;
    }
    private static void master(Kasus t) throws Exception {
        String[] raw=nilai(t,false), normal=nilai(t,true);
        check(diterima(()->{if(orang(t)) new Peserta("PST999999",raw[0],raw[1]); else latihan(t,"PLT999999",raw);})==t.sah,t.id+" constructor");
        Object obj=orang(t)?new Peserta("PST999999","Peserta Uji","penerimaan@example.com"):latihan(new Kasus(t.id,t.field,"",false),"PLT999999",nilai(new Kasus(t.id,t.field,"",false),true));
        String lama=get(t,obj);
        check(diterima(()->setter(t,obj))==t.sah,t.id+" setter"); check(t.sah || get(t,obj).equals(lama),t.id+" setter atomik");
        String prefix=orang(t)?"PST":"PLT", expected=t.field.equals("durasi")?Integer.toString(Integer.parseInt(field(t,normal))):field(t,normal);
        List<String> cleanup=new ArrayList<>();
        try {
            long before=nomor(prefix);
            final Object[] added={null};
            check(diterima(()->added[0]=tambah(t,raw))==t.sah,t.id+" model tambah");
            if(added[0]!=null) cleanup.add(id(added[0]));
            check(nomor(prefix)==before+(t.sah?1:0),t.id+" counter tambah invalid tetap");
            if(added[0]!=null) { check(get(t,baca(t,id(added[0]))).equals(expected),t.id+" roundtrip tambah"); hapus(t,id(added[0])); cleanup.clear(); }
            Object base=tambah(new Kasus(t.id,t.field,"",false),nilai(new Kasus(t.id,t.field,"",false),true)); String target=id(base); cleanup.add(target);
            String old=get(t,base);
            check(diterima(()->ubah(t,target,raw))==t.sah,t.id+" model ubah");
            check(get(t,baca(t,target)).equals(t.sah?expected:old),t.id+" roundtrip/atomic ubah");
            hapus(t,target); cleanup.clear();
            String predicted=Validasi.formatId(prefix,nomor(prefix)); cleanup.add(predicted);
            String menu=orang(t)?"1":"2", output=cli(menu+"\n3\n"+formulir(t)+"0\n0\n");
            check(output.contains(orang(t)?"Peserta disimpan:":"Pelatihan disimpan:"),t.id+" CLI tambah selesai");
            check(get(t,baca(t,predicted)).equals(expected),t.id+" CLI tambah/retry roundtrip");
            output=cli(menu+"\n4\n"+predicted+"\n"+formulir(t)+"0\n0\n");
            check(output.contains(orang(t)?"Peserta disimpan:":"Pelatihan disimpan:") && get(t,baca(t,predicted)).equals(expected),t.id+" CLI ubah/retry roundtrip");
        } finally { for(String target:cleanup) { try { hapus(t,target); } catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.TIDAK_ADA) throw e; } } }
    }
    private static void tanggal(Kasus t) throws Exception {
        Clock clock=t.id.equals("T094") || t.id.equals("T095")?Clock.fixed(Instant.parse("2028-03-01T00:00:00Z"),ZoneId.of("Asia/Singapore")):CLOCK;
        check(diterima(()->Validasi.tanggal(t.raw,clock))==t.sah,t.id+" kalender");
        ManajemenData dates=new ManajemenData(k,clock); Sesi ds=dates.login("admin","Admin2026!".toCharArray());
        try {
            final PratinjauSertifikat[] preview={null}; long before=nomor("SRT");
            check(diterima(()->preview[0]=dates.pratinjauBaru(ds,"PST002","PLT002",Validasi.tanggal(t.raw,clock)))==t.sah,t.id+" model tanggal");
            if(t.sah) { Sertifikat saved=dates.simpanSertifikat(ds,preview[0]); try { check(dates.cariSertifikat(ds,saved.getIdSertifikat()).getTanggalTerbit().toString().equals(t.raw),t.id+" tanggal roundtrip DAO"); } finally { dates.hapusSertifikat(ds,saved.getIdSertifikat()); } }
            else check(nomor("SRT")==before,t.id+" counter tanggal invalid tetap");
            if(clock==CLOCK) {
                String date=t.sah?t.raw:t.raw+"\n2026-10-08"; String predicted=Validasi.formatId("SRT",nomor("SRT"));
                String output=cli("3\n3\nPST002\nPLT002\n"+date+"\ny\n0\n0\n");
                try { check(output.contains("Sertifikat disimpan:") && m.cariSertifikat(sesi,predicted).getTanggalTerbit().toString().equals(t.sah?t.raw:"2026-10-08"),t.id+" tanggal CLI/retry"); } finally { m.hapusSertifikat(sesi,predicted); }
            }
        } finally { dates.logout(ds); }
    }
    public static void main(String[] args) throws Exception {
        try {
            k=Koneksi.dariEnvironment(); if(!k.getDatabase().equals("db_sertifikat_pelatihan_test")) throw new IllegalArgumentException("Hanya schema test.");
            m=new ManajemenData(k,CLOCK); m.periksaKesiapan(); sesi=m.login("admin","Admin2026!".toCharArray());
            int count=0;
            try(var reader=new BufferedReader(new InputStreamReader(Objects.requireNonNull(UjiPenerimaanField.class.getResourceAsStream("/penerimaan-field.tsv")),StandardCharsets.UTF_8))) {
                for(String line;(line=reader.readLine())!=null;) {
                    String[] c=line.split("\t",-1); Kasus t=new Kasus(c[0],c[1],new String(Base64.getDecoder().decode(c[2]),StandardCharsets.UTF_8),Boolean.parseBoolean(c[3]));
                    if(t.field.equals("pengguna")) check(diterima(()->new Pengguna("PGN999999",t.raw,"admin",Role.ADMIN,new byte[16],new byte[32],Kredensial.ITERASI))==t.sah,t.id+" nama akun immutable");
                    else if(t.field.equals("tanggal")) tanggal(t);
                    else master(t);
                    System.out.println("PASS PENERIMAAN "+t.id+" "+t.field); count++;
                }
            }
            check(count==95,"95 fixture wajib dijalankan"); check(m.semuaPeserta(sesi).size()==2 && m.semuaPelatihan(sesi).size()==2 && m.semuaSertifikat(sesi).size()==1,"fixture kembali bersih");
            m.logout(sesi); System.out.println("PASS: 95 spesifikasi field T001-T095; jalur sesuai scope, fixture bersih");
        } finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
}
