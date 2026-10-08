package model;

import java.time.LocalDate;

/** Token immutable dari model; tidak membawa Connection/objek master mutable. */
public final class PratinjauSertifikat implements Ringkasan {
    private final Sesi sesi;
    private final String idSertifikat, pesertaId, pelatihanId;
    private final LocalDate tanggal;
    private final SnapshotSertifikat calon, lama;
    private final String pesertaLama, pelatihanLama, pencatatLama;
    private final LocalDate tanggalLama;
    PratinjauSertifikat(Sesi sesi,String id,Peserta peserta,Pelatihan pelatihan,LocalDate tanggal,SnapshotSertifikat calon,Sertifikat lama) {
        this.sesi=sesi; this.idSertifikat=id; this.pesertaId=peserta.getIdPeserta(); this.pelatihanId=pelatihan.getIdPelatihan();
        this.tanggal=tanggal; this.calon=calon;
        this.lama=lama==null?null:lama.getSnapshot(); this.pesertaLama=lama==null?null:lama.getPeserta().getIdPeserta();
        this.pelatihanLama=lama==null?null:lama.getPelatihan().getIdPelatihan();
        this.pencatatLama=lama==null?null:lama.getPencatat().getIdPengguna(); this.tanggalLama=lama==null?null:lama.getTanggalTerbit();
    }
    Sesi getSesi() { return sesi; }
    boolean cocokLama(Sertifikat data) {
        return data!=null && idSertifikat.equals(data.getIdSertifikat()) && pesertaLama.equals(data.getPeserta().getIdPeserta())
            && pelatihanLama.equals(data.getPelatihan().getIdPelatihan()) && pencatatLama.equals(data.getPencatat().getIdPengguna())
            && tanggalLama.equals(data.getTanggalTerbit()) && lama.equals(data.getSnapshot());
    }
    public String getIdSertifikat() { return idSertifikat; }
    public String getPesertaId() { return pesertaId; }
    public String getPelatihanId() { return pelatihanId; }
    public LocalDate getTanggal() { return tanggal; }
    public SnapshotSertifikat getCalon() { return calon; }
    public SnapshotSertifikat getLama() { return lama; }
    public String getPesertaLama() { return pesertaLama; }
    public String getPelatihanLama() { return pelatihanLama; }
    public LocalDate getTanggalLama() { return tanggalLama; }
    public boolean isKoreksi() { return idSertifikat!=null; }
    @Override public String getRingkasan() { return (isKoreksi()?idSertifikat:"Sertifikat baru")+" | "+calon.getNamaPeserta()+" | "+calon.getNamaPelatihan()+" | "+tanggal; }
}
