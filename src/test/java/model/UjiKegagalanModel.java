package model;

import config.Koneksi;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.List;

public final class UjiKegagalanModel {
    private static long counter(Koneksi k,String jenis) throws SQLException {
        try(Connection c=k.buka()) { c.setAutoCommit(false); try { return new NomorDAO().kunciNomor(c,jenis); } finally { c.rollback(); } }
    }
    public static void jalankan(Koneksi k,ManajemenData normal,Sesi admin,String pesertaId,String pelatihanId,List<String> cleanup) throws Exception {
        try { normal.semuaPeserta(new Sesi(admin.getPengguna())); throw new AssertionError("Sesi buatan diterima"); }
        catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.SESI) throw e; }
        long before=counter(k,"SRT");
        ManajemenData gagal=new ManajemenData(Clock.systemDefaultZone(),()->proxy(k.buka(),false));
        Sesi s=gagal.login("admin","Admin2026!".toCharArray());
        PratinjauSertifikat preview=gagal.pratinjauBaru(s,pesertaId,pelatihanId,LocalDate.now());
        try { gagal.simpanSertifikat(s,preview); throw new AssertionError("Kegagalan child tidak dilaporkan"); }
        catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.TEKNIS) throw e; }
        if(counter(k,"SRT")!=before || normal.semuaSertifikat(admin).size()!=1) throw new AssertionError("Parent/snapshot/counter parsial tersimpan");
        ManajemenData ambigu=new ManajemenData(Clock.systemDefaultZone(),()->proxy(k.buka(),true));
        Sesi a=ambigu.login("admin","Admin2026!".toCharArray());
        try { ambigu.tambahPeserta(a,"Peserta Ambigu","ambigu-model@example.com"); throw new AssertionError("Ambigu tidak dilaporkan"); }
        catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.AMBIGU) throw e; }
        boolean ditemukan=false;
        for(Peserta p:normal.semuaPeserta(admin)) if(p.getEmailNormal().equals("ambigu-model@example.com")) { cleanup.add(p.getIdPeserta()); ditemukan=true; }
        if(!ditemukan) throw new AssertionError("Fixture commit nyata tidak terbaca setelah exception commit diinjeksi");
        System.out.println("PASS: 3 pemeriksaan model internal: sesi buatan/kegagalan child/commit ambigu");
    }
    private static Connection proxy(Connection c,boolean commitAmbigu) {
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,method,args)->{
            if(commitAmbigu && method.getName().equals("commit")) { c.commit(); throw new SQLException("INJECTED commit result unknown","08S01"); }
            if(!commitAmbigu && method.getName().equals("prepareStatement") && args[0].toString().startsWith("INSERT INTO snapshot_sertifikat")) throw new SQLException("INJECTED child insert failure","ZZ001");
            try { return method.invoke(c,args); } catch(InvocationTargetException e) { throw e.getCause(); }
        });
    }
}
