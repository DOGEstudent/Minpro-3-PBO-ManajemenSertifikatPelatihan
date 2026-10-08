package view;

/** Kontrol alur formulir; bukan kegagalan database. */
public final class InputBerakhir extends RuntimeException {
    private final boolean eof;
    public InputBerakhir(boolean eof) { this.eof=eof; }
    public boolean isEof() { return eof; }
}
