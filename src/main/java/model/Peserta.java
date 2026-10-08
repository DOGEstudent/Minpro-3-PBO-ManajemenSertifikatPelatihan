package model;

import util.Validasi;

public final class Peserta implements Ringkasan {
    private final String idPeserta;
    private String nama, email;
    public Peserta(String idPeserta, String nama, String email) {
        this.idPeserta = Validasi.idEntitas(idPeserta, "PST");
        this.nama = Validasi.nama(nama, "Nama peserta"); this.email = Validasi.email(email);
    }
    public String getIdPeserta() { return idPeserta; }
    public String getNama() { return nama; }
    public String getEmail() { return email; }
    public String getEmailNormal() { return Validasi.emailNormal(email); }
    public void setNama(String nama) { this.nama = Validasi.nama(nama, "Nama peserta"); }
    public void setEmail(String email) { this.email = Validasi.email(email); }
    @Override public String getRingkasan() { return idPeserta + " | " + nama + " | " + email; }
}
