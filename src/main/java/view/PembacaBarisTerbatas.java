package view;

import java.io.*;
import util.Validasi;

/** Batasi buffer sebelum Scanner; buang sisa baris terlalu panjang sampai newline. */
final class PembacaBarisTerbatas extends Reader {
    private final PushbackReader sumber;
    private String baris="";
    private int posisi;
    PembacaBarisTerbatas(Reader sumber) { this.sumber=new PushbackReader(new BufferedReader(sumber),1); }
    @Override public int read(char[] buffer,int offset,int length) throws IOException {
        java.util.Objects.checkFromIndexSize(offset,length,buffer.length);
        if(length==0) return 0;
        if(posisi>=baris.length()) {
            StringBuilder b=new StringBuilder(); boolean ada=false;
            int ch;
            while((ch=sumber.read())!=-1) {
                ada=true;
                if(ch=='\n') break;
                if(ch=='\r') { int next=sumber.read(); if(next!=-1 && next!='\n') sumber.unread(next); break; }
                if(b.length()<=Validasi.BATAS_BARIS) b.append((char)ch);
            }
            if(!ada) return -1;
            baris=b.append('\n').toString(); posisi=0;
        }
        int n=Math.min(length,baris.length()-posisi); baris.getChars(posisi,posisi+n,buffer,offset); posisi+=n; return n;
    }
    @Override public void close() throws IOException { sumber.close(); }
}
