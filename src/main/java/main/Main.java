package main;

import config.Koneksi;
import controller.SertifikatController;
import model.*;
import view.ConsoleView;
import java.time.Clock;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        try(ConsoleView view=ConsoleView.standar()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Clock clock=Clock.systemDefaultZone(); ManajemenData model=new ManajemenData(Koneksi.dariEnvironment(),clock);
                model.periksaKesiapan(); new SertifikatController(model,view,clock).jalankan();
            } catch(MasalahAplikasi | IllegalArgumentException | java.io.UncheckedIOException e) {
                view.tampil("Aplikasi belum dapat dijalankan. Periksa konfigurasi/koneksi dan jalankan SetupDatabase bila schema belum ada.");
                System.exit(1);
            } catch(ClassNotFoundException | LinkageError e) {
                view.tampil("Driver/dependensi JDBC tidak tersedia atau tidak kompatibel. Sertakan folder lib hasil build di samping JAR.");
                System.exit(1);
            }
        }
    }
}
