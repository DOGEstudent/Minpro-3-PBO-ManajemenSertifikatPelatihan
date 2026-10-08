package model;

import java.time.Clock;
import java.time.LocalDate;
import util.Validasi;

public final class Sertifikat implements Ringkasan {
    private final String idSertifikat;
    private final Pengguna pencatat;
    private final Clock clock;
    private Peserta peserta;
    private Pelatihan pelatihan;
    private LocalDate tanggalTerbit;
    private SnapshotSertifikat snapshot;
    public Sertifikat(String id, Peserta peserta, Pelatihan pelatihan, Pengguna pencatat, LocalDate tanggal, SnapshotSertifikat snapshot) {
        this(id, peserta, pelatihan, pencatat, tanggal, snapshot, Clock.systemDefaultZone());
    }
    public Sertifikat(String id, Peserta peserta, Pelatihan pelatihan, Pengguna pencatat, LocalDate tanggal, SnapshotSertifikat snapshot, Clock clock) {
        this.idSertifikat = Validasi.idEntitas(id, "SRT"); this.pencatat = Validasi.wajib(pencatat, "Pencatat");
        this.clock = Validasi.wajib(clock, "Clock");
        terapkanKoreksi(peserta, pelatihan, tanggal, snapshot);
    }
    public String getIdSertifikat() { return idSertifikat; }
    public Pengguna getPencatat() { return pencatat; }
    public Peserta getPeserta() { return peserta; }
    public Pelatihan getPelatihan() { return pelatihan; }
    public LocalDate getTanggalTerbit() { return tanggalTerbit; }
    public SnapshotSertifikat getSnapshot() { return snapshot; }
    // Hanya untuk objek lokal setelah model memvalidasi kandidat/commit DB terkonfirmasi.
    // Seluruh pemeriksaan selesai sebelum assignment pertama; tidak melakukan SQL.
    void terapkanKoreksi(Peserta peserta, Pelatihan pelatihan, LocalDate tanggal, SnapshotSertifikat snapshot) {
        Validasi.wajib(peserta, "Peserta"); Validasi.wajib(pelatihan, "Pelatihan"); Validasi.wajib(snapshot, "Snapshot");
        LocalDate valid = Validasi.tanggal(tanggal, clock);
        this.peserta = peserta; this.pelatihan = pelatihan; this.tanggalTerbit = valid; this.snapshot = snapshot;
    }
    @Override public String getRingkasan() {
        return idSertifikat + " | " + snapshot.getNamaPeserta() + " | " + snapshot.getNamaPelatihan() + " | " + tanggalTerbit;
    }
}
