package pengujian;

import model.*;
import util.*;
import java.lang.reflect.Modifier;
import java.time.*;
import java.util.*;

/** Fixture domain tanpa JDBC. Bukan bukti retry CLI atau keunikan lintas database. */
public final class UjiDomain {
    private static int checks;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneId.of("Asia/Singapore"));
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    private static void reject(Runnable work, String label) {
        try { work.run(); throw new AssertionError("Diterima: " + label); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static PelatihanOnline online() { return new PelatihanOnline("PLT001", "Java 26", 16, "Budi Santoso", "Zoom", "https://example.com/a?x=1"); }
    private static PelatihanOffline offline() { return new PelatihanOffline("PLT002", "Basis Data", 12, "Dewi Lestari", "Gedung A", "A-101"); }
    private static Pengguna pengguna(String nama) {
        return new Pengguna("PGN001", nama, "ADMIN", Role.ADMIN, new byte[16], new byte[32], Kredensial.ITERASI);
    }
    public static void main(String[] args) throws Exception {
        nama(); teks(); emailUrl(); angkaTanggal(); identitasPassword(); snapshotPolymorphism();
        model.UjiInvariantDomain.jalankan();
        System.out.println("PASS: " + checks + " pemeriksaan validator/domain publik");
    }
    private static void nama() {
        Peserta p = new Peserta("PST001", "Andi Pratama", "andi@example.com"); PelatihanOnline on = online();
        String[] invalid = {null, "", " ", "11", "Andi123", "!!!", "A", "A".repeat(81), "A\u200bB", "A\tB", "A\nB", "A\u00a0B", "\u0301A", "A-\u0301B", "A\uD800B", "A\uDC00B", "A😀B", "A_B", "A\\B", "A$B"};
        for (String raw : invalid) {
            reject(() -> new Peserta("PST002", raw, "siti@example.com"), "constructor nama peserta");
            reject(() -> p.setNama(raw), "setter nama peserta");
            check(p.getNama().equals("Andi Pratama"), "nama peserta lama tetap");
            reject(() -> new PelatihanOnline("PLT003", "Java Dasar", 1, raw, "Zoom", "https://example.com"), "constructor instruktur");
            reject(() -> on.setInstruktur(raw), "setter instruktur");
            check(on.getInstruktur().equals("Budi Santoso"), "instruktur lama tetap");
            reject(() -> pengguna(raw), "constructor nama pengguna immutable");
        }
        for (String raw : new String[]{"O'Neill", "José", "Dewi-Lestari", "M. Ali", "محمد", "李明", "A".repeat(80)}) {
            p.setNama(raw); check(p.getNama().equals(raw), "nama sah disimpan");
            on.setInstruktur(raw); check(on.getInstruktur().equals(raw), "instruktur sah disimpan");
            check(pengguna(raw).getNama().equals(raw), "nama akun sah");
        }
        p.setNama("  Jose\u0301  "); check(p.getNama().equals("José"), "trim ASCII dan NFC");
        p.setNama("Andi  Pratama"); check(p.getNama().equals("Andi  Pratama"), "spasi internal dipertahankan");
        String supplementary = "\uD801\uDC00".repeat(80);
        p.setNama(supplementary); check(p.getNama().codePointCount(0,p.getNama().length())==80, "batas memakai code point, bukan UTF-16");
        reject(() -> p.setNama(supplementary + "A"), "81 code point");
    }
    private static void teks() {
        PelatihanOnline on = online(); PelatihanOffline off = offline();
        for (String raw : new String[]{"Java 26", "UI/UX Dasar", "C# Dasar", "C++ Dasar", "A & B (Dasar)", "J".repeat(3), "J".repeat(100)}) {
            on.setNama(raw); check(on.getNama().equals(raw), "judul sah");
        }
        on.setNama("Java 26");
        for (String raw : new String[]{null, "JJ", "J".repeat(101), "111", "<script>", "A_B", "A😀B", "A1\u0301", "A\u200bB"}) {
            reject(() -> new PelatihanOnline("PLT003",raw,1,"Budi Santoso","Zoom","https://example.com"), "constructor judul invalid");
            reject(() -> on.setNama(raw), "judul invalid"); check(on.getNama().equals("Java 26"), "judul lama tetap");
        }
        for (String raw : new String[]{"Zoom", "Meet 2", "Z".repeat(100)}) { on.setPlatform(raw); check(on.getPlatform().equals(raw), "platform sah"); }
        on.setPlatform("Zoom");
        for (String raw : new String[]{"Z", "Z".repeat(101), "11", "!!!", null}) {
            reject(() -> new PelatihanOnline("PLT003","Dasar Java",1,"Budi Santoso",raw,"https://example.com"), "constructor platform invalid");
            reject(() -> on.setPlatform(raw), "platform invalid"); check(on.getPlatform().equals("Zoom"), "platform lama tetap");
        }
        for (String raw : new String[]{"02", "A-101", "R".repeat(100)}) {
            off.setRuangan(raw); off.setLokasi(raw); check(off.getRuangan().equals(raw) && off.getLokasi().equals(raw), "lokasi/ruangan sah");
        }
        off.setLokasi("Gedung A"); off.setRuangan("A-101");
        for (String raw : new String[]{"R", "R".repeat(101), "!!!", null}) {
            reject(() -> new PelatihanOffline("PLT003","Dasar Java",1,"Budi Santoso",raw,"A-101"), "constructor lokasi invalid");
            reject(() -> new PelatihanOffline("PLT003","Dasar Java",1,"Budi Santoso","Gedung A",raw), "constructor ruangan invalid");
            reject(() -> off.setLokasi(raw), "lokasi invalid"); reject(() -> off.setRuangan(raw), "ruangan invalid");
            check(off.getLokasi().equals("Gedung A") && off.getRuangan().equals("A-101"), "field offline lama tetap");
        }
    }
    private static void emailUrl() {
        Peserta p = new Peserta("PST001", "Andi Pratama", "andi@example.com");
        for (String raw : new String[]{"andi+kelas@example.com", "A_B%kelas@sub-domain.example.COM", "a".repeat(64)+"@x.co"}) { p.setEmail(raw); check(p.getEmail().equals(raw), "email sah"); }
        p.setEmail("andi@example.com");
        for (String raw : new String[]{null, "@", "a..b@example.com", ".a@example.com", "a.@example.com", "a@localhost", "a@-domain.com", "a@domain-.com", "a b@example.com", "a@x.c", "a@x.12", "a@@x.com", "á@x.com", "a".repeat(65)+"@x.co", "a@"+"b".repeat(64)+".com", "a@x..com"}) {
            reject(() -> new Peserta("PST002","Siti Rahma",raw), "constructor email invalid");
            reject(() -> p.setEmail(raw), "email invalid"); check(p.getEmail().equals("andi@example.com"), "email lama tetap");
        }
        p.setEmail(" ANDI+Kelas@EXAMPLE.COM "); check(p.getEmailNormal().equals("andi+kelas@example.com"), "normalisasi email locale-independent");
        PelatihanOnline on = online();
        for (String raw : new String[]{"https://example.com/a?x=1", "http://localhost:8080", "HTTPS://EXAMPLE.COM", "https://x.com:65535", "https://x.com/a%20b#f"}) {
            on.setTautan(raw); check(on.getTautan().equals(raw), "URI sah tanpa akses jaringan");
        }
        on.setTautan("https://example.com");
        for (String raw : new String[]{null, "https://?", "https://#f", "ftp://example.com", "https://x.com:0", "https://x.com:65536", "https://u:p@x.com", "https://x.com/a b", "https://x.com/%ZZ", "https://例子.com", "https://x.com/"+"a".repeat(200), "https://x.com/\u200b"}) {
            reject(() -> new PelatihanOnline("PLT003","Dasar Java",1,"Budi Santoso","Zoom",raw), "constructor URL invalid");
            reject(() -> on.setTautan(raw), "URL invalid"); check(on.getTautan().equals("https://example.com"), "URL lama tetap");
        }
    }
    private static void angkaTanggal() {
        for (String raw : new String[]{"abc", "12abc", "1.5", "1e3", "0x10", "@#$", "2147483648", "-2147483649", "", "0", "-1", "501", "１２", "1\t", null})
            reject(() -> Validasi.bilangan(raw,1,500), "integer parsing/range");
        String[] raw = {"1","500","+12","0012"," 7 "}; int[] expected={1,500,12,12,7};
        for(int i=0;i<raw.length;i++) check(Validasi.bilangan(raw[i],1,500)==expected[i], "integer sah");
        Pelatihan on = online();
        for(int invalid : new int[]{Integer.MIN_VALUE,-1,0,501,Integer.MAX_VALUE}) {
            reject(() -> new PelatihanOffline("PLT003","Basis Data",invalid,"Budi Santoso","Gedung A","A-101"), "constructor durasi invalid");
            reject(() -> on.setDurasi(invalid), "setter durasi"); check(on.getDurasi()==16,"durasi lama tetap");
        }
        for(String date : new String[]{"1999-12-31","2026-10-09","2026-02-30","2026-2-03","2026-13-01","2026-00-01","2026-01-00","2025-02-29","２０２６-01-01",null})
            reject(() -> Validasi.tanggal(date,CLOCK), "tanggal tidak valid");
        for(String date : new String[]{"2000-01-01","2026-10-08","2024-02-29"}) check(Validasi.tanggal(date,CLOCK).equals(LocalDate.parse(date)),"tanggal sah");
        Clock edge = Clock.fixed(Instant.parse("2026-10-07T16:30:00Z"),ZoneId.of("Asia/Singapore"));
        check(Validasi.tanggal("2026-10-08",edge).equals(LocalDate.of(2026,10,8)),"hari ini mengikuti zona Clock");
    }
    private static void identitasPassword() throws Exception {
        check(Validasi.idPencarian(" pst001 ","PST").equals("PST001"),"ID trim/case");
        for(String raw : new String[]{"001","PSTxyz","PLT001","PST01", "PST١٢٣",null}) reject(() -> Validasi.idPencarian(raw,"PST"),"ID pencarian invalid");
        reject(() -> new Peserta("PST000","Andi Pratama","a@example.com"),"ID entitas nol");
        reject(() -> new Peserta("PST9223372036854775808","Andi Pratama","a@example.com"),"ID entitas overflow long");
        check(Validasi.formatId("PST",1).equals("PST001") && Validasi.formatId("PST",1000).equals("PST1000"),"padding minimum tiga digit");
        Set<String> ids=new HashSet<>(); for(long i=1;i<=1000;i++) ids.add(Validasi.formatId("PST",i)); check(ids.size()==1000,"1000 format ID unik (belum counter DAO)");
        reject(() -> Validasi.formatId("PST",0),"format ID nol");
        check(Validasi.username(" ADMIN ").equals("admin"),"username normal");
        for(String raw : new String[]{"ad","a".repeat(31),"admin!","ádmin",null}) reject(() -> Validasi.username(raw),"username invalid");
        for(String raw : new String[]{"12345678","a".repeat(128),"      ab"," password "}) { Validasi.password(raw.toCharArray()); check(true,"password sah"); }
        for(String raw : new String[]{"1234567","a".repeat(129),"        ","a\tb123456","ábcdefgh","😀abcdefg"}) reject(() -> Validasi.password(raw.toCharArray()),"password invalid");
        reject(() -> Validasi.password(null),"password null");
        check(Validasi.konfirmasi(" Y ") && !Validasi.konfirmasi("t"),"konfirmasi y/t");
        for(String raw : new String[]{"yes","1","",null}) reject(() -> Validasi.konfirmasi(raw),"konfirmasi invalid");
        check(Validasi.batal("/batal") && !Validasi.batal(" /batal "),"perintah batal persis");
        Validasi.baris("x".repeat(10000)); check(true,"batas raw10000");
        reject(() -> Validasi.baris("x".repeat(10001)),"batas raw10001");
        reject(() -> Validasi.nama(" ".repeat(10001)+"Ali","Nama"),"raw dibatasi sebelum trim");
        check(Modifier.isAbstract(Pelatihan.class.getModifiers()) && Modifier.isAbstract(Pelatihan.class.getMethod("getDetailPelaksanaan").getModifiers()),"abstract class/method");
        check(Arrays.stream(Sesi.class.getDeclaredConstructors()).noneMatch(c -> Modifier.isPublic(c.getModifiers())),"Sesi bukan constructor publik");
    }
    private static void snapshotPolymorphism() throws Exception {
        Peserta p = new Peserta("PST001","Andi Pratama","andi@example.com"); PelatihanOnline on=online(); PelatihanOffline off=offline();
        ArrayList<Pelatihan> pelatihan = new ArrayList<>(List.of(on,off));
        check(pelatihan.get(0).getDetailPelaksanaan().contains("Platform: Zoom") && pelatihan.get(1).getDetailPelaksanaan().contains("Ruangan: A-101"),"dispatch override melalui induk");
        check(on.getDetailPelaksanaan(false).equals(on.getDetailPelaksanaan()) && on.getDetailPelaksanaan(true).contains("ONLINE"),"kedua overload berjalan");
        SnapshotSertifikat snap=SnapshotSertifikat.dari(p,on); SnapshotSertifikat copy=SnapshotSertifikat.dari(p,on);
        check(snap != copy && snap.equals(copy) && snap.hashCode()==copy.hashCode(),"snapshot dibandingkan berdasarkan isi");
        Sertifikat sertifikat=new Sertifikat("SRT001",p,on,pengguna("Administrator"),LocalDate.of(2026,10,8),snap,CLOCK);
        String ringkasan=sertifikat.getRingkasan(); String detail=snap.getDetailPelaksanaan();
        p.setNama("Nama Baru"); p.setEmail("baru@example.com"); on.setNama("Java Lanjutan"); on.setPlatform("Meet"); on.setInstruktur("Dewi Lestari"); on.setDurasi(20);
        check(sertifikat.getRingkasan().equals(ringkasan) && snap.getDetailPelaksanaan().equals(detail) && snap.getEmailPeserta().equals("andi@example.com"),"perubahan master tidak mengubah snapshot/tampilan");
        check(!snap.equals(SnapshotSertifikat.dari(p,on)),"perubahan master terdeteksi lewat isi snapshot");
        ArrayList<Ringkasan> list=new ArrayList<>(List.of(p,on,sertifikat)); check(list.stream().allMatch(item -> !item.getRingkasan().isBlank()),"pemanggilan interface beberapa entitas");
        reject(() -> new Sertifikat("SRT002",p,on,pengguna("Administrator"),LocalDate.of(2026,10,9),snap,CLOCK),"constructor tanggal masa depan");
        reject(() -> new Sertifikat("SRT002",p,on,pengguna("Administrator"),LocalDate.of(2026,10,8),null,CLOCK),"sertifikat tanpa snapshot");
        reject(() -> SnapshotSertifikat.dari(null,on),"snapshot peserta null");
        reject(() -> new SnapshotSertifikat("Andi Pratama","andi@example.com","Java Dasar",JenisPelatihan.ONLINE,1,"Budi Santoso","x".repeat(1001)),"detail snapshot terlalu panjang");
        for(Class<?> clazz : new Class<?>[]{SnapshotSertifikat.class,Pengguna.class,Sesi.class})
            check(Arrays.stream(clazz.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).allMatch(f -> Modifier.isPrivate(f.getModifiers()) && Modifier.isFinal(f.getModifiers())),"immutable fields " + clazz.getSimpleName());
        byte[] salt=Kredensial.saltBaru(); char[] raw="Admin2026!".toCharArray(); byte[] hash=Kredensial.hash(raw,salt);
        Pengguna user=new Pengguna("PGN001","Administrator","admin",Role.ADMIN,salt,hash,Kredensial.ITERASI);
        Arrays.fill(salt,(byte)0); Arrays.fill(hash,(byte)0); Arrays.fill(user.getSalt(),(byte)0); Arrays.fill(user.getHashPassword(),(byte)0);
        try { check(user.verifikasiPassword(raw),"salinan defensif constructor/getter akun"); }
        finally { Arrays.fill(raw,'\0'); }
        check(user.getUsernameNormal().equals("admin") && user.getRole()==Role.ADMIN,"identitas/role akun tetap");
    }
}
