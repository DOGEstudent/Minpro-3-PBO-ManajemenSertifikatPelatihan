package pengujian;

import view.*;
import util.Validasi;
import java.io.*;
import java.util.Arrays;

public final class UjiConsoleView {
    private static int checks;
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); checks++; }
    private static ConsoleView view(String input,StringWriter output) { return new ConsoleView(new StringReader(input),output,false); }
    public static void main(String[] args) {
        StringWriter out=new StringWriter();
        try(var v=view("abc\n12abc\n1.5\n2147483648\n0\n7\n",out)) {
            check(v.field("Durasi",s->Validasi.bilangan(s,1,500))==7,"angka invalid mengulang sampai valid");
            check(out.toString().split("Durasi: ",-1).length==7,"enam prompt tanpa melompati baris");
        }
        try(var v=view("11\nAulia1\nAulia@\nAulia Rahman\n",new StringWriter())) { check(v.field("Nama",s->Validasi.nama(s,"Nama")).equals("Aulia Rahman"),"nama angka/campuran/simbol ditolak"); }
        try(var v=view("3\n2\n",new StringWriter())) { check(v.pilih("Menu",0,2,4)==2,"pilihan dalam rentang tetapi tidak tersedia ditolak"); }
        try(var v=view("1\u20280\n2\n",new StringWriter())) { check(v.pilih("Menu",0,1,2)==2,"separator Unicode tidak menjadi perintah menu terpisah"); }
        try(var v=view("Nama\u0085Tambahan\nNama\u2029Tambahan\nNama Valid\n",new StringWriter())) { check(v.field("Nama",s->Validasi.nama(s,"Nama")).equals("Nama Valid"),"NEL/paragraph separator ditolak dalam field"); }
        out=new StringWriter();
        try(var v=view("\n7\n",out)) { check(v.pilih("Menu",7)==7 && out.toString().split("Menu: ",-1).length==3,"baris kosong tetap divalidasi tanpa dilewati"); }
        try(var v=view("yes\n Y \nT\n",new StringWriter())) { check(v.konfirmasi("Simpan"),"retry konfirmasi y"); check(!v.konfirmasi("Simpan"),"konfirmasi t"); }
        try(var v=view("x".repeat(10000)+"\n",new StringWriter())) { check(v.field("Baris",s->s).length()==10000,"batas baris diterima"); }
        try(var v=view("x".repeat(10001)+"\n7\n",new StringWriter())) { check(v.pilih("Menu",7)==7,"kelebihan satu karakter dibuang seluruh baris"); }
        try(var v=view("x".repeat(500000)+"\n7\n",new StringWriter())) { check(v.pilih("Menu",7)==7,"baris sangat panjang tidak merusak input berikut"); }
        try(var v=view("1\r\n2\r3",new StringWriter())) { check(v.pilih("Menu",1)==1,"CRLF"); check(v.pilih("Menu",2)==2,"CR"); check(v.pilih("Menu",3)==3,"baris terakhir tanpa newline"); }
        try(var v=view("",new StringWriter())) { try { v.pilih("Menu",0); throw new AssertionError("EOF diterima"); } catch(InputBerakhir e) { check(e.isEof(),"EOF dibedakan dari batal"); } }
        try(var v=view("/batal\n",new StringWriter())) { try { v.field("Nama",s->s); throw new AssertionError("batal diterima"); } catch(InputBerakhir e) { check(!e.isEof(),"pembatalan field"); } }
        try(var v=view("/batal\n",new StringWriter())) { try { v.password("Password"); throw new AssertionError("batal password diterima"); } catch(InputBerakhir e) { check(!e.isEof(),"pembatalan password"); } }
        out=new StringWriter();
        try(var v=view("short\nAdmin2026!\n",out)) { char[] raw=v.password("Password"); check(Arrays.equals(raw,"Admin2026!".toCharArray()),"password retry dan tidak diubah"); Arrays.fill(raw,'\0'); check(!out.toString().contains("Admin2026!"),"aplikasi tidak mencetak password input"); v.pembukaan(); check(!out.toString().contains("Menyiapkan tampilan"),"animasi dimatikan pada pengujian"); }
        System.out.println("PASS: "+checks+" pemeriksaan ConsoleView");
    }
}
