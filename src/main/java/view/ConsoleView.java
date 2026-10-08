package view;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Function;
import util.Validasi;

/** Satu jalur input per instance: Console atau Scanner, tidak dicampur. */
public final class ConsoleView implements AutoCloseable {
    private final Console console;
    private final Scanner scanner;
    private final PrintWriter out;
    private final boolean animasi;
    private boolean dialogPassword;
    public ConsoleView(Reader input,Writer output,boolean animasi) {
        this.console=null; this.scanner=new Scanner(new PembacaBarisTerbatas(input)).useDelimiter("(?<=\\n)");
        this.out=new PrintWriter(output,true); this.animasi=animasi;
    }
    private ConsoleView(Console console,boolean animasi) {
        this.console=console; this.scanner=null; this.out=console.writer(); this.animasi=animasi;
    }
    public static ConsoleView standar() {
        Console console=System.console();
        if(console!=null && console.isTerminal()) return new ConsoleView(console,!"0".equals(System.getenv("APP_ANIMASI")));
        ConsoleView view=new ConsoleView(new InputStreamReader(System.in,StandardCharsets.UTF_8),new OutputStreamWriter(System.out,StandardCharsets.UTF_8),false);
        view.dialogPassword="1".equals(System.getenv("APP_PASSWORD_DIALOG"));
        return view;
    }
    public void tampil(String text) { out.println(text); out.flush(); }
    public void menu(String judul,String... pilihan) {
        tampil("\n"+judul);
        tampil("-".repeat(Math.min(60,judul.length())));
        for(String item:pilihan) tampil(item);
    }
    /** Sel panjang dibungkus, sehingga nilai tidak dipotong atau disembunyikan. */
    public void tabel(String[] header,int[] lebar,java.util.List<String[]> baris) {
        if(header.length!=lebar.length) throw new IllegalArgumentException("Kolom tabel tidak sesuai.");
        StringBuilder garis=new StringBuilder("+");
        for(int n:lebar) { if(n<1) throw new IllegalArgumentException("Lebar kolom invalid."); garis.append("-".repeat(n+2)).append('+'); }
        tampil(garis.toString()); barisTabel(header,lebar); tampil(garis.toString());
        for(String[] row:baris) { if(row.length!=header.length) throw new IllegalArgumentException("Baris tabel tidak sesuai."); barisTabel(row,lebar); tampil(garis.toString()); }
    }
    private void barisTabel(String[] row,int[] lebar) {
        int[][] teks=new int[row.length][]; int jumlah=1;
        for(int i=0;i<row.length;i++) { teks[i]=row[i].codePoints().toArray(); jumlah=Math.max(jumlah,(teks[i].length+lebar[i]-1)/lebar[i]); }
        for(int baris=0;baris<jumlah;baris++) {
            StringBuilder line=new StringBuilder("|");
            for(int i=0;i<row.length;i++) {
                int awal=Math.min(teks[i].length,baris*lebar[i]), panjang=Math.min(lebar[i],teks[i].length-awal);
                line.append(' ').append(new String(teks[i],awal,panjang)).append(" ".repeat(lebar[i]-panjang+1)).append('|');
            }
            tampil(line.toString());
        }
    }
    public void pembukaan() {
        tampil("=================================================");
        tampil(" SISTEM MANAJEMEN SERTIFIKAT PELATIHAN");
        tampil("=================================================");
        if(animasi) {
            long batas=System.nanoTime()+600_000_000L;
            for(char frame:new char[]{'|','/','-','\\'}) {
                if(System.nanoTime()>=batas) break;
                out.print("\rMenyiapkan tampilan login ["+frame+"]"); out.flush();
                try { Thread.sleep(90); } catch(InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            out.println("\rTampilan login siap.                 "); out.flush();
        }
        tampil("Gunakan /batal untuk membatalkan formulir; EOF mengakhiri aplikasi.");
    }
    private String baris(String prompt) {
        while(true) {
            String raw;
            if(console!=null) raw=console.readLine("%s: ",prompt);
            else {
                out.print(prompt+": "); out.flush();
                // Token berakhir pada LF pembaca; separator Unicode tetap di field untuk ditolak validator.
                raw=scanner.hasNext()?scanner.next():null;
                if(raw!=null) raw=raw.substring(0,raw.length()-1);
                if(raw==null && scanner.ioException()!=null) throw new UncheckedIOException(scanner.ioException());
            }
            if(raw==null) throw new InputBerakhir(true);
            try { Validasi.baris(raw); return raw; }
            catch(IllegalArgumentException e) { tampil(e.getMessage()); }
        }
    }
    public <T> T field(String prompt,Function<String,T> validator) {
        while(true) {
            String raw=baris(prompt);
            if(Validasi.batal(raw)) throw new InputBerakhir(false);
            try { return validator.apply(raw); }
            catch(IllegalArgumentException e) { tampil(e.getMessage()); }
        }
    }
    public int pilih(String prompt,int... pilihan) {
        int min=Arrays.stream(pilihan).min().orElseThrow(), max=Arrays.stream(pilihan).max().orElseThrow();
        return field(prompt,raw->{int n=Validasi.bilangan(raw,min,max);
            if(Arrays.stream(pilihan).noneMatch(p->p==n)) throw new IllegalArgumentException("Pilihan tidak tersedia pada menu ini."); return n;});
    }
    public boolean konfirmasi(String prompt) { return field(prompt+" (y/t)",Validasi::konfirmasi); }
    public char[] password(String prompt) {
        while(true) {
            char[] raw;
            if(dialogPassword) { tampil("Masukkan password pada dialog bertanda bintang. Pilih Batal untuk kembali."); raw=DialogPassword.baca(prompt); }
            else raw=console==null?baris(prompt).toCharArray():console.readPassword("%s: ",prompt);
            if(raw==null) throw new InputBerakhir(true);
            if(Arrays.equals(raw,new char[]{'/','b','a','t','a','l'})) { Arrays.fill(raw,'\0'); throw new InputBerakhir(false); }
            try { Validasi.password(raw); return raw; }
            catch(IllegalArgumentException e) { Arrays.fill(raw,'\0'); tampil(e.getMessage()); }
        }
    }
    @Override public void close() { if(scanner!=null) scanner.close(); out.flush(); }
}
