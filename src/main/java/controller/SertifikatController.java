package controller;

import model.*;
import view.*;
import util.Validasi;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/** Alur UI tanpa SQL; semua baca/mutasi tersimpan melalui facade model. */
public final class SertifikatController {
    private final ManajemenData model;
    private final ConsoleView view;
    private final Clock clock;
    private Sesi sesi;
    private boolean berjalan=true;
    public SertifikatController(ManajemenData model,ConsoleView view,Clock clock) { this.model=model; this.view=view; this.clock=clock; }
    public void jalankan() {
        view.pembukaan();
        try {
            while(berjalan) {
                try { if(sesi==null) login(); else menuUtama(); }
                catch(InputBerakhir e) { if(e.isEof()) { view.tampil("Input berakhir. Formulir yang belum disimpan dibatalkan."); berjalan=false; } else view.tampil("Formulir dibatalkan."); }
                catch(MasalahAplikasi e) {
                    view.tampil(e.getMessage());
                    if(e.getJenis()==MasalahAplikasi.Jenis.SESI) sesi=null;
                    if(e.getJenis()==MasalahAplikasi.Jenis.TEKNIS) { view.tampil("Aplikasi diakhiri. Periksa koneksi/schema lalu jalankan kembali."); berjalan=false; }
                }
            }
        } finally {
            if(sesi!=null) { try { model.logout(sesi); } catch(MasalahAplikasi ignored) { } sesi=null; }
            view.tampil("Sesi ditutup. Data yang sudah disimpan tetap tersimpan.");
        }
    }
    private void login() {
        view.menu("LOGIN", "1. Masuk", "0. Keluar");
        if(view.pilih("Pilihan",0,1)==0) { berjalan=false; return; }
        String username=view.field("Username",Validasi::username);
        sesi=model.login(username,view.password("Password"));
        view.tampil("Login berhasil: "+sesi.getPengguna().getNama()+" ["+sesi.getPengguna().getRole()+"].");
    }
    private void menuUtama() {
        view.tampil("\nMENU UTAMA — "+sesi.getPengguna().getUsernameNormal());
        view.menu("Pilih tindakan", "1. Peserta", "2. Pelatihan", "3. Sertifikat", "4. Statistik", "5. Logout", "0. Keluar");
        switch(view.pilih("Pilihan",0,1,2,3,4,5)) {
            case 0 -> berjalan=false;
            case 1 -> menuPeserta(); case 2 -> menuPelatihan(); case 3 -> menuSertifikat();
            case 4 -> { Statistik s=model.statistik(sesi); view.tampil("Peserta: "+s.peserta()); view.tampil("Online: "+s.pelatihanOnline()); view.tampil("Offline: "+s.pelatihanOffline()); view.tampil("Sertifikat: "+s.sertifikat()); }
            case 5 -> { model.logout(sesi); sesi=null; view.tampil("Logout berhasil."); }
        }
    }
    private boolean admin() { return sesi.getPengguna().getRole()==Role.ADMIN; }
    private void menuPeserta() {
        while(true) {
            view.menu("PESERTA", admin()?new String[]{"1. Daftar", "2. Cari/detail", "3. Tambah", "4. Ubah", "5. Hapus", "0. Kembali"}:new String[]{"1. Daftar", "2. Cari/detail", "0. Kembali"});
            int pilihan=view.pilih("Pilihan",admin()?new int[]{0,1,2,3,4,5}:new int[]{0,1,2});
            switch(pilihan) {
                case 0 -> { return; }
                case 1 -> daftar("Daftar peserta",model.semuaPeserta(sesi));
                case 2 -> detailPeserta(peserta("ID peserta"));
                case 3 -> formulirPeserta(null);
                case 4 -> { Peserta p=peserta("ID peserta yang diubah"); detailPeserta(p); formulirPeserta(p); }
                case 5 -> { Peserta p=peserta("ID peserta yang dihapus"); detailPeserta(p); if(view.konfirmasi("Hapus peserta "+p.getIdPeserta()+"?")) { model.hapusPeserta(sesi,p.getIdPeserta()); view.tampil("Peserta dihapus."); } else batal(); }
            }
        }
    }
    private void formulirPeserta(Peserta lama) {
        String nama=view.field("Nama peserta",raw->Validasi.nama(raw,"Nama peserta"));
        while(true) {
            String email=view.field("Email",Validasi::email);
            try {
                Peserta p=lama==null?model.tambahPeserta(sesi,nama,email):model.ubahPeserta(sesi,lama.getIdPeserta(),nama,email);
                view.tampil("Peserta disimpan: "+ringkasan(p)); return;
            } catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.DUPLIKAT) throw e; view.tampil(e.getMessage()+" Ulangi email."); }
        }
    }
    private void menuPelatihan() {
        while(true) {
            view.menu("PELATIHAN", admin()?new String[]{"1. Daftar", "2. Cari/detail", "3. Tambah", "4. Ubah", "5. Hapus", "0. Kembali"}:new String[]{"1. Daftar", "2. Cari/detail", "0. Kembali"});
            int pilihan=view.pilih("Pilihan",admin()?new int[]{0,1,2,3,4,5}:new int[]{0,1,2});
            switch(pilihan) {
                case 0 -> { return; }
                case 1 -> daftar("Daftar pelatihan",model.semuaPelatihan(sesi));
                case 2 -> detailPelatihan(pelatihan("ID pelatihan"));
                case 3 -> formulirPelatihan(null);
                case 4 -> { Pelatihan p=pelatihan("ID pelatihan yang diubah"); detailPelatihan(p); formulirPelatihan(p); }
                case 5 -> { Pelatihan p=pelatihan("ID pelatihan yang dihapus"); detailPelatihan(p); if(view.konfirmasi("Hapus pelatihan "+p.getIdPelatihan()+"?")) { model.hapusPelatihan(sesi,p.getIdPelatihan()); view.tampil("Pelatihan dihapus."); } else batal(); }
            }
        }
    }
    private void formulirPelatihan(Pelatihan lama) {
        view.menu("JENIS PELATIHAN", "1. Online", "2. Offline"); int jenis=view.pilih("Jenis",1,2);
        String nama=view.field("Nama pelatihan",Validasi::judul);
        int durasi=view.field("Durasi (jam)",raw->Validasi.bilangan(raw,1,500));
        String instruktur=view.field("Instruktur",raw->Validasi.nama(raw,"Instruktur"));
        Pelatihan p;
        if(jenis==1) {
            String platform=view.field("Platform",Validasi::platform), tautan=view.field("Tautan",Validasi::tautan);
            p=lama==null?model.tambahOnline(sesi,nama,durasi,instruktur,platform,tautan):model.ubahOnline(sesi,lama.getIdPelatihan(),nama,durasi,instruktur,platform,tautan);
        } else {
            String lokasi=view.field("Lokasi",Validasi::lokasi), ruangan=view.field("Ruangan",Validasi::ruangan);
            p=lama==null?model.tambahOffline(sesi,nama,durasi,instruktur,lokasi,ruangan):model.ubahOffline(sesi,lama.getIdPelatihan(),nama,durasi,instruktur,lokasi,ruangan);
        }
        view.tampil("Pelatihan disimpan: "+ringkasan(p));
    }
    private void menuSertifikat() {
        while(true) {
            view.menu("SERTIFIKAT", "1. Daftar", "2. Detail", "3. Tambah", "4. Koreksi", "5. Hapus", "6. Cari nama", "7. Riwayat peserta", "0. Kembali");
            switch(view.pilih("Pilihan",0,1,2,3,4,5,6,7)) {
                case 0 -> { return; }
                case 1 -> daftar("Daftar sertifikat (snapshot)",model.semuaSertifikat(sesi));
                case 2 -> detailSertifikat(sertifikat("ID sertifikat"));
                case 3 -> formulirSertifikat(null);
                case 4 -> { Sertifikat s=sertifikat("ID sertifikat yang dikoreksi"); detailSertifikat(s); formulirSertifikat(s); }
                case 5 -> { Sertifikat s=sertifikat("ID sertifikat yang dihapus"); detailSertifikat(s); if(view.konfirmasi("Hapus sertifikat "+s.getIdSertifikat()+"?")) { model.hapusSertifikat(sesi,s.getIdSertifikat()); view.tampil("Sertifikat dihapus."); } else batal(); }
                case 6 -> { String kata=view.field("Nama peserta/pelatihan",raw->Validasi.teksDasar(raw,"Pencarian",1,100)); daftar("Hasil pencarian snapshot",model.cariNamaSertifikat(sesi,kata)); }
                case 7 -> { Peserta p=peserta("ID peserta untuk riwayat"); view.tampil("Master terkini: "+p.getNama()); daftar("Riwayat sertifikat (snapshot)",model.riwayatPeserta(sesi,p.getIdPeserta())); }
            }
        }
    }
    private void formulirSertifikat(Sertifikat lama) {
        while(true) {
            var peserta=model.semuaPeserta(sesi); var pelatihan=model.semuaPelatihan(sesi);
            if(peserta.isEmpty() || pelatihan.isEmpty()) { view.tampil("Tambahkan peserta dan pelatihan terlebih dahulu melalui Admin."); return; }
            daftar("Pilihan peserta",peserta); Peserta p=peserta("ID peserta pilihan");
            daftar("Pilihan pelatihan",pelatihan); Pelatihan l=pelatihan("ID pelatihan pilihan");
            LocalDate tanggal=view.field("Tanggal terbit YYYY-MM-DD",raw->Validasi.tanggal(raw,clock));
            try {
                PratinjauSertifikat preview=lama==null?model.pratinjauBaru(sesi,p.getIdPeserta(),l.getIdPelatihan(),tanggal):model.pratinjauKoreksi(sesi,lama.getIdSertifikat(),p.getIdPeserta(),l.getIdPelatihan(),tanggal);
                if(preview.isKoreksi()) {
                    view.tampil("Data lama — ID peserta: "+preview.getPesertaLama()+" | ID pelatihan: "+preview.getPelatihanLama()+" | Tanggal: "+preview.getTanggalLama());
                    detailSnapshot(preview.getLama());
                }
                view.tampil("Pratinjau: "+ringkasan(preview)); detailSnapshot(preview.getCalon());
                view.tampil("ID peserta: "+preview.getPesertaId()+" | ID pelatihan: "+preview.getPelatihanId());
                view.tampil("Pencatat operasi: "+sesi.getPengguna().getNama()+"; pencatat awal sertifikat tetap saat koreksi.");
                if(!view.konfirmasi("Simpan sertifikat?")) { batal(); return; }
                Sertifikat s=model.simpanSertifikat(sesi,preview); view.tampil("Sertifikat disimpan: "+ringkasan(s)); return;
            } catch(MasalahAplikasi e) {
                if(e.getJenis()!=MasalahAplikasi.Jenis.DUPLIKAT && e.getJenis()!=MasalahAplikasi.Jenis.KONFLIK && e.getJenis()!=MasalahAplikasi.Jenis.TIDAK_ADA) throw e;
                view.tampil(e.getMessage()+" Ulangi pilihan dan pratinjau.");
            }
        }
    }
    private <T> T pilihData(Supplier<T> baca) {
        while(true) try { return baca.get(); }
        catch(MasalahAplikasi e) { if(e.getJenis()!=MasalahAplikasi.Jenis.TIDAK_ADA && e.getJenis()!=MasalahAplikasi.Jenis.VALIDASI) throw e; view.tampil(e.getMessage()+" Ulangi ID."); }
    }
    private Peserta peserta(String prompt) { return pilihData(()->model.cariPeserta(sesi,view.field(prompt,raw->Validasi.idPencarian(raw,"PST")))); }
    private Pelatihan pelatihan(String prompt) { return pilihData(()->model.cariPelatihan(sesi,view.field(prompt,raw->Validasi.idPencarian(raw,"PLT")))); }
    private Sertifikat sertifikat(String prompt) { return pilihData(()->model.cariSertifikat(sesi,view.field(prompt,raw->Validasi.idPencarian(raw,"SRT")))); }
    private String ringkasan(Ringkasan data) { return data.getRingkasan(); }
    private void daftar(String judul,List<? extends Ringkasan> data) {
        view.tampil("\n"+judul+" — "+data.size()+" data");
        if(data.isEmpty()) { view.tampil("Belum ada data yang cocok."); return; }
        int total=(data.size()+4)/5, halaman=1;
        while(true) {
            view.tampil("Halaman "+halaman+"/"+total);
            java.util.ArrayList<String[]> rows=new java.util.ArrayList<>();
            for(int i=(halaman-1)*5;i<Math.min(halaman*5,data.size());i++) rows.add(kolom(i+1,data.get(i)));
            Ringkasan contoh=data.get(0);
            if(contoh instanceof Peserta) view.tabel(new String[]{"No","ID","Nama peserta","Email"},new int[]{3,8,24,30},rows);
            else if(contoh instanceof Pelatihan) view.tabel(new String[]{"No","ID","Pelatihan","Jenis","Jam"},new int[]{3,8,30,7,3},rows);
            else if(contoh instanceof Sertifikat) view.tabel(new String[]{"No","ID","Peserta (snapshot)","Pelatihan (snapshot)","Terbit"},new int[]{3,8,21,21,10},rows);
            else view.tabel(new String[]{"No","Ringkasan"},new int[]{3,65},rows);
            if(total==1) return;
            int next=view.pilih("Halaman tujuan (0=kembali)",pilihanHalaman(total)); if(next==0) return; halaman=next;
        }
    }
    private String[] kolom(int nomor,Ringkasan data) {
        String no=Integer.toString(nomor);
        if(data instanceof Peserta p) return new String[]{no,p.getIdPeserta(),p.getNama(),p.getEmail()};
        if(data instanceof Pelatihan p) return new String[]{no,p.getIdPelatihan(),p.getNama(),p.getJenis().toString(),Integer.toString(p.getDurasi())};
        if(data instanceof Sertifikat s) return new String[]{no,s.getIdSertifikat(),s.getSnapshot().getNamaPeserta(),s.getSnapshot().getNamaPelatihan(),s.getTanggalTerbit().toString()};
        return new String[]{no,ringkasan(data)};
    }
    private static int[] pilihanHalaman(int total) { int[] pilihan=new int[total+1]; for(int i=0;i<=total;i++) pilihan[i]=i; return pilihan; }
    private void detailPeserta(Peserta p) { view.tampil("ID peserta: "+p.getIdPeserta()); view.tampil("Nama: "+p.getNama()); view.tampil("Email: "+p.getEmail()); }
    private void detailPelatihan(Pelatihan p) { view.tampil("ID pelatihan: "+p.getIdPelatihan()+" | "+p.getNama()+" | "+p.getJenis()); view.tampil(p.getDetailPelaksanaan(false)); }
    private void detailSertifikat(Sertifikat s) {
        view.tampil("ID sertifikat: "+s.getIdSertifikat()+" | Tanggal: "+s.getTanggalTerbit());
        view.tampil("ID peserta: "+s.getPeserta().getIdPeserta()+" | ID pelatihan: "+s.getPelatihan().getIdPelatihan());
        detailSnapshot(s.getSnapshot()); view.tampil("Pencatat awal: "+s.getPencatat().getNama()+" ("+s.getPencatat().getUsernameNormal()+")");
    }
    private void detailSnapshot(SnapshotSertifikat s) {
        view.tampil("Snapshot peserta: "+s.getNamaPeserta()+" | "+s.getEmailPeserta());
        view.tampil("Snapshot pelatihan: "+s.getNamaPelatihan()+" | "+s.getJenisPelatihan()+" | "+s.getDurasi()+" jam | "+s.getInstruktur());
        view.tampil(s.getDetailPelaksanaan());
    }
    private void batal() { view.tampil("Perubahan dibatalkan; data tidak disimpan."); }
}
