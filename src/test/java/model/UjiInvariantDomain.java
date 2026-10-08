package model;

import java.time.*;
import util.Kredensial;

/** Akses package-private hanya dari source test; tidak membuka setter publik. */
public final class UjiInvariantDomain {
    public static void jalankan() {
        Clock clock=Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneId.of("Asia/Singapore"));
        Pengguna admin=new Pengguna("PGN001","Administrator","admin",Role.ADMIN,new byte[16],new byte[32],Kredensial.ITERASI);
        Peserta p=new Peserta("PST001","Andi Pratama","andi@example.com");
        Pelatihan on=new PelatihanOnline("PLT001","Dasar Java",16,"Budi Santoso","Zoom","https://example.com");
        SnapshotSertifikat snap=SnapshotSertifikat.dari(p,on);
        Sertifikat s=new Sertifikat("SRT001",p,on,admin,LocalDate.of(2026,10,8),snap,clock);
        Peserta other=new Peserta("PST002","Siti Rahma","siti@example.com");
        try { s.terapkanKoreksi(other,on,LocalDate.of(2026,10,9),SnapshotSertifikat.dari(other,on)); throw new AssertionError("Invalid correction accepted"); }
        catch (IllegalArgumentException expected) { }
        if(s.getPeserta()!=p || s.getSnapshot()!=snap || !s.getTanggalTerbit().equals(LocalDate.of(2026,10,8))) throw new AssertionError("Koreksi gagal mengubah sebagian objek");
        s.terapkanKoreksi(other,on,LocalDate.of(2026,10,7),SnapshotSertifikat.dari(other,on));
        if(!s.getIdSertifikat().equals("SRT001") || s.getPencatat()!=admin || s.getPeserta()!=other || !s.getRingkasan().contains("Siti Rahma")) throw new AssertionError("Koreksi tidak atomik/identitas berubah");
        Sesi first=new Sesi(admin), second=new Sesi(admin);
        if(first==second || first.getPengguna()!=admin || second.getPengguna()!=admin) throw new AssertionError("Sesi baru tidak berbeda identitas");
        System.out.println("PASS: 3 pemeriksaan invariant package-private (sesi aktif diuji pada runner facade)");
    }
}
