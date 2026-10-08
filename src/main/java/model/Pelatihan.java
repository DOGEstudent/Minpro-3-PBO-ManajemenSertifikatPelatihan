package model;

import util.Validasi;

public abstract class Pelatihan implements Ringkasan {
    private final String idPelatihan;
    private String nama, instruktur;
    private int durasi;
    protected Pelatihan(String idPelatihan, String nama, int durasi, String instruktur) {
        this.idPelatihan = Validasi.idEntitas(idPelatihan, "PLT");
        this.nama = Validasi.judul(nama); this.durasi = Validasi.rentang(durasi, 1, 500);
        this.instruktur = Validasi.nama(instruktur, "Instruktur");
    }
    public String getIdPelatihan() { return idPelatihan; }
    public String getNama() { return nama; }
    public int getDurasi() { return durasi; }
    public String getInstruktur() { return instruktur; }
    public void setNama(String nama) { this.nama = Validasi.judul(nama); }
    public void setDurasi(int durasi) { this.durasi = Validasi.rentang(durasi, 1, 500); }
    public void setInstruktur(String instruktur) { this.instruktur = Validasi.nama(instruktur, "Instruktur"); }
    protected String getInfoDasar() { return "Durasi: " + durasi + " jam | Instruktur: " + instruktur; }
    public abstract JenisPelatihan getJenis();
    public abstract String getDetailPelaksanaan();
    public String getDetailPelaksanaan(boolean ringkas) {
        return ringkas ? getIdPelatihan() + " | " + getNama() + " | " + getJenis() : getDetailPelaksanaan();
    }
    @Override public String getRingkasan() { return getDetailPelaksanaan(true); }
}
