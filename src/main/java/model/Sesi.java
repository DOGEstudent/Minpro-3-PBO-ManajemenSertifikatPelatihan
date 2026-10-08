package model;

import util.Validasi;

/** Hanya model yang membuat sesi baru setelah verifikasi akun. */
public final class Sesi {
    private final Pengguna pengguna;
    Sesi(Pengguna pengguna) { this.pengguna = Validasi.wajib(pengguna, "Pengguna sesi"); }
    public Pengguna getPengguna() { return pengguna; }
}
