package model;

/** Pesan domain aman untuk controller; detail JDBC tidak ditampilkan ke pengguna. */
public final class MasalahAplikasi extends RuntimeException {
    public enum Jenis { VALIDASI, LOGIN, SESI, AKSES, TIDAK_ADA, DUPLIKAT, REFERENSI, KONFLIK, TEKNIS, AMBIGU }
    private final Jenis jenis;
    public MasalahAplikasi(Jenis jenis,String pesan) { super(pesan); this.jenis=jenis; }
    public Jenis getJenis() { return jenis; }
}
