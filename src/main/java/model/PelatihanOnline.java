package model;

import util.Validasi;

public final class PelatihanOnline extends Pelatihan {
    private String platform, tautan;
    public PelatihanOnline(String id, String nama, int durasi, String instruktur, String platform, String tautan) {
        super(id, nama, durasi, instruktur);
        this.platform = Validasi.platform(platform); this.tautan = Validasi.tautan(tautan);
    }
    public String getPlatform() { return platform; }
    public String getTautan() { return tautan; }
    public void setPlatform(String platform) { this.platform = Validasi.platform(platform); }
    public void setTautan(String tautan) { this.tautan = Validasi.tautan(tautan); }
    @Override public JenisPelatihan getJenis() { return JenisPelatihan.ONLINE; }
    @Override public String getDetailPelaksanaan() { return super.getInfoDasar() + " | Platform: " + platform + " | Tautan: " + tautan; }
}
