package pengujian;

import config.Koneksi;
import util.Kredensial;
import java.util.Arrays;

public final class UjiKredensial {
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        byte[] salt = Kredensial.saltBaru(); char[] password = "Admin2026!".toCharArray();
        byte[] hash = Kredensial.hash(password, salt);
        check(salt.length == 16 && hash.length == 32, "salt/hash length");
        check(Kredensial.cocok(password, salt, hash), "password sah");
        char[] wrong = "Admin2026! ".toCharArray();
        try { check(!Kredensial.cocok(wrong, salt, hash), "password raw tidak di-trim"); }
        finally { Arrays.fill(wrong, '\0'); }
        try { new Koneksi("127.0.0.1", 3306, "mysql", "root", ""); throw new AssertionError("system schema allowed"); }
        catch (IllegalArgumentException expected) { }
        Arrays.fill(password, '\0'); Arrays.fill(hash, (byte) 0);
        System.out.println("PASS: 4 pemeriksaan kredensial/konfigurasi");
    }
}
