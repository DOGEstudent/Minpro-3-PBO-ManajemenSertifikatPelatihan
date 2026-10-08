package model;

import util.Validasi;

public final class PelatihanOffline extends Pelatihan {
    private String lokasi, ruangan;
    public PelatihanOffline(String id, String nama, int durasi, String instruktur, String lokasi, String ruangan) {
        super(id, nama, durasi, instruktur);
        this.lokasi = Validasi.lokasi(lokasi); this.ruangan = Validasi.ruangan(ruangan);
    }
    public String getLokasi() { return lokasi; }
    public String getRuangan() { return ruangan; }
    public void setLokasi(String lokasi) { this.lokasi = Validasi.lokasi(lokasi); }
    public void setRuangan(String ruangan) { this.ruangan = Validasi.ruangan(ruangan); }
    @Override public JenisPelatihan getJenis() { return JenisPelatihan.OFFLINE; }
    @Override public String getDetailPelaksanaan() { return super.getInfoDasar() + " | Lokasi: " + lokasi + " | Ruangan: " + ruangan; }
}
