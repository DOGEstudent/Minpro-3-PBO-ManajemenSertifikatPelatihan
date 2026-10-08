package model;

import java.util.Objects;
import util.Validasi;

/** Salinan immutable, tidak menyimpan referensi peserta/pelatihan mutable. */
public final class SnapshotSertifikat {
    private final String namaPeserta, emailPeserta, namaPelatihan, instruktur, detailPelaksanaan;
    private final JenisPelatihan jenisPelatihan;
    private final int durasi;
    public SnapshotSertifikat(String namaPeserta, String emailPeserta, String namaPelatihan,
            JenisPelatihan jenis, int durasi, String instruktur, String detail) {
        this.namaPeserta = Validasi.nama(namaPeserta, "Nama peserta snapshot");
        this.emailPeserta = Validasi.email(emailPeserta); this.namaPelatihan = Validasi.judul(namaPelatihan);
        this.jenisPelatihan = Validasi.wajib(jenis, "Jenis pelatihan"); this.durasi = Validasi.rentang(durasi, 1, 500);
        this.instruktur = Validasi.nama(instruktur, "Instruktur snapshot");
        // Detail hasil format mengandung pemisah | dan URI: bukan kategori TEKS input.
        this.detailPelaksanaan = Validasi.teksDasar(detail, "Detail snapshot", 1, 1000);
    }
    public static SnapshotSertifikat dari(Peserta peserta, Pelatihan pelatihan) {
        Validasi.wajib(peserta, "Peserta"); Validasi.wajib(pelatihan, "Pelatihan");
        return new SnapshotSertifikat(peserta.getNama(), peserta.getEmail(), pelatihan.getNama(),
            pelatihan.getJenis(), pelatihan.getDurasi(), pelatihan.getInstruktur(), pelatihan.getDetailPelaksanaan());
    }
    public String getNamaPeserta() { return namaPeserta; }
    public String getEmailPeserta() { return emailPeserta; }
    public String getNamaPelatihan() { return namaPelatihan; }
    public JenisPelatihan getJenisPelatihan() { return jenisPelatihan; }
    public int getDurasi() { return durasi; }
    public String getInstruktur() { return instruktur; }
    public String getDetailPelaksanaan() { return detailPelaksanaan; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SnapshotSertifikat s)) return false;
        return durasi == s.durasi && jenisPelatihan == s.jenisPelatihan && namaPeserta.equals(s.namaPeserta)
            && emailPeserta.equals(s.emailPeserta) && namaPelatihan.equals(s.namaPelatihan)
            && instruktur.equals(s.instruktur) && detailPelaksanaan.equals(s.detailPelaksanaan);
    }
    @Override public int hashCode() { return Objects.hash(namaPeserta, emailPeserta, namaPelatihan, jenisPelatihan, durasi, instruktur, detailPelaksanaan); }
}
