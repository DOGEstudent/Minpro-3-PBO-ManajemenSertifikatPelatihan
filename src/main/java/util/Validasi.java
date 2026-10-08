package util;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/** Aturan field domain; keunikan lintas data dan hak akses diperiksa model/DB. */
public final class Validasi {
    public static final int BATAS_BARIS = 10_000;
    private Validasi() { }
    public static <T> T wajib(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " wajib diisi.");
        return value;
    }

    public static void baris(String raw) {
        if (raw == null) throw new IllegalArgumentException("Input wajib diisi.");
        if (raw.length() > BATAS_BARIS) throw new IllegalArgumentException("Input maksimal 10.000 karakter UTF-16.");
    }
    public static String teksDasar(String raw, String field, int min, int max) {
        baris(raw);
        for (int i = 0; i < raw.length();) {
            char ch = raw.charAt(i);
            if (Character.isSurrogate(ch) && !(Character.isHighSurrogate(ch) && i + 1 < raw.length() && Character.isLowSurrogate(raw.charAt(i + 1))))
                throw new IllegalArgumentException(field + " mengandung surrogate tidak berpasangan.");
            int cp = raw.codePointAt(i);
            if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT
                || ((Character.isWhitespace(cp) || Character.isSpaceChar(cp)) && cp != ' '))
                throw new IllegalArgumentException(field + " mengandung karakter kontrol/whitespace yang tidak diizinkan.");
            i += Character.charCount(cp);
        }
        String text = Normalizer.normalize(trimSpasi(raw), Normalizer.Form.NFC);
        int length = text.codePointCount(0, text.length());
        if (length < min || length > max) throw new IllegalArgumentException(field + " harus " + min + "–" + max + " karakter.");
        return text;
    }
    public static String trimSpasi(String raw) {
        baris(raw);
        int awal = 0, akhir = raw.length();
        while (awal < akhir && raw.charAt(awal) == ' ') awal++;
        while (akhir > awal && raw.charAt(akhir - 1) == ' ') akhir--;
        return raw.substring(awal, akhir);
    }
    public static String nama(String raw, String field) { return kategori(raw, field, 2, 80, true, true); }
    public static String judul(String raw) { return kategori(raw, "Nama pelatihan", 3, 100, false, true); }
    public static String platform(String raw) { return kategori(raw, "Platform", 2, 100, false, true); }
    public static String lokasi(String raw) { return kategori(raw, "Lokasi", 2, 100, false, false); }
    public static String ruangan(String raw) { return kategori(raw, "Ruangan", 2, 100, false, false); }
    private static String kategori(String raw, String field, int min, int max, boolean nama, boolean wajibHuruf) {
        String text = teksDasar(raw, field, min, max);
        boolean adaHuruf = false, adaDigit = false, sebelumHurufMark = false;
        String punctuation = nama ? ".'’- " : ".,:;'’-()/&+# ";
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i); int type = Character.getType(cp);
            boolean letter = Character.isLetter(cp);
            boolean mark = type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK;
            boolean digit = cp >= '0' && cp <= '9';
            if (mark && !sebelumHurufMark) throw new IllegalArgumentException(field + ": combining mark harus mengikuti huruf/mark.");
            if (!(letter || mark || (!nama && digit) || punctuation.indexOf(cp) >= 0))
                throw new IllegalArgumentException(field + " mengandung karakter yang tidak diizinkan.");
            adaHuruf |= letter; adaDigit |= digit;
            sebelumHurufMark = letter || mark;
            i += Character.charCount(cp);
        }
        if (wajibHuruf ? !adaHuruf : !(adaHuruf || adaDigit))
            throw new IllegalArgumentException(field + (wajibHuruf ? " wajib mengandung huruf." : " wajib mengandung huruf atau angka."));
        return text;
    }
    private static String ascii(String raw, String field, int min, int max) {
        String text = teksDasar(raw, field, min, max);
        if (text.chars().anyMatch(ch -> ch > 127)) throw new IllegalArgumentException(field + " harus ASCII.");
        return text;
    }
    public static String email(String raw) {
        String text = ascii(raw, "Email", 5, 100);
        String[] parts = text.split("@", -1);
        if (parts.length != 2 || parts[0].length() < 1 || parts[0].length() > 64
            || !parts[0].matches("[A-Za-z0-9._%+\\-]+") || parts[0].startsWith(".") || parts[0].endsWith(".") || parts[0].contains(".."))
            throw new IllegalArgumentException("Format email tidak valid.");
        String[] labels = parts[1].split("\\.", -1);
        if (labels.length < 2) throw new IllegalArgumentException("Domain email memerlukan minimal dua label.");
        for (String label : labels) if (label.length() < 1 || label.length() > 63 || !label.matches("[A-Za-z0-9-]+") || label.startsWith("-") || label.endsWith("-"))
            throw new IllegalArgumentException("Domain email tidak valid.");
        if (!labels[labels.length - 1].matches("[A-Za-z]{2,63}")) throw new IllegalArgumentException("Label terakhir domain email harus 2–63 huruf.");
        return text;
    }
    public static String emailNormal(String raw) { return email(raw).toLowerCase(Locale.ROOT); }
    public static String tautan(String raw) {
        String text = ascii(raw, "Tautan", 8, 200);
        if (text.indexOf(' ') >= 0) throw new IllegalArgumentException("Tautan tidak boleh mengandung spasi.");
        try {
            URI uri = new URI(text);
            if (!uri.isAbsolute() || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getHost().isEmpty() || uri.getRawUserInfo() != null
                || (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535)))
                throw new IllegalArgumentException("Tautan harus http/https dengan host dan port yang valid, tanpa user-info.");
            return text;
        } catch (URISyntaxException e) { throw new IllegalArgumentException("Format tautan tidak valid."); }
    }
    public static int bilangan(String raw, int min, int max) {
        String text = trimSpasi(raw);
        if (!text.matches("[+-]?[0-9]+")) throw integerError(min, max);
        try { return rentang(Integer.parseInt(text), min, max); }
        catch (NumberFormatException e) { throw integerError(min, max); }
    }
    public static int rentang(int value, int min, int max) {
        if (min > max || value < min || value > max) throw integerError(min, max);
        return value;
    }
    private static IllegalArgumentException integerError(int min, int max) { return new IllegalArgumentException("Masukkan bilangan bulat " + min + "–" + max + "."); }
    public static LocalDate tanggal(String raw, Clock clock) {
        String text = trimSpasi(raw);
        if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Tanggal harus YYYY-MM-DD.");
        try { return tanggal(LocalDate.parse(text), clock); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Tanggal kalender tidak valid."); }
    }
    public static LocalDate tanggal(LocalDate date, Clock clock) {
        Objects.requireNonNull(clock, "Clock wajib tersedia.");
        LocalDate today = LocalDate.now(clock);
        if (date == null || date.isBefore(LocalDate.of(2000, 1, 1)) || date.isAfter(today))
            throw new IllegalArgumentException("Tanggal terbit harus antara 2000-01-01 dan " + today + ".");
        return date;
    }
    private static void prefix(String prefix) {
        if (!("PST".equals(prefix) || "PLT".equals(prefix) || "SRT".equals(prefix) || "PGN".equals(prefix)))
            throw new IllegalArgumentException("Prefix ID tidak dikenal.");
    }
    public static String idPencarian(String raw, String prefix) {
        prefix(prefix);
        String text = ascii(raw, "ID", 1, 30).toUpperCase(Locale.ROOT);
        if (!text.matches(prefix + "[0-9]{3,}")) throw new IllegalArgumentException("Format ID harus " + prefix + " diikuti minimal tiga digit.");
        return text;
    }
    public static String idEntitas(String raw, String prefix) {
        String id = idPencarian(raw, prefix);
        try { if (Long.parseLong(id.substring(3)) < 1) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Nomor ID entitas harus positif dan dalam rentang long."); }
        return id;
    }
    public static String formatId(String prefix, long nomor) {
        prefix(prefix);
        if (nomor < 1) throw new IllegalArgumentException("Nomor ID harus positif.");
        return prefix + String.format(Locale.ROOT, "%03d", nomor);
    }
    public static String username(String raw) {
        String text = ascii(raw, "Username", 3, 30);
        if (!text.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Format username tidak valid.");
        return text.toLowerCase(Locale.ROOT);
    }
    public static void password(char[] raw) {
        if (raw == null || raw.length < 8 || raw.length > 128) throw new IllegalArgumentException("Password harus 8–128 karakter ASCII printable.");
        boolean bukanSpasi = false;
        for (char ch : raw) {
            if (ch < 32 || ch > 126) throw new IllegalArgumentException("Password harus ASCII printable.");
            bukanSpasi |= ch != ' ';
        }
        if (!bukanSpasi) throw new IllegalArgumentException("Password tidak boleh seluruhnya spasi.");
    }
    public static boolean konfirmasi(String raw) {
        String text = trimSpasi(raw);
        if (text.equalsIgnoreCase("y")) return true;
        if (text.equalsIgnoreCase("t")) return false;
        throw new IllegalArgumentException("Masukkan y atau t.");
    }
    public static boolean batal(String raw) { return "/batal".equals(raw); }
}
