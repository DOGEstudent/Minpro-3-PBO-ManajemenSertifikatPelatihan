package model;

import java.lang.reflect.*;
import java.time.Clock;
import util.Kredensial;

public final class UjiGuardSesi {
    public static void main(String[] args) throws Exception {
        ManajemenData model=new ManajemenData(Clock.systemDefaultZone(),()->{throw new AssertionError("Sesi invalid mencapai JDBC");});
        Sesi palsu=new Sesi(new Pengguna("PGN001","Administrator","admin",Role.ADMIN,new byte[16],new byte[32],Kredensial.ITERASI));
        int calls=0;
        for(Method method:ManajemenData.class.getMethods()) {
            Class<?>[] types=method.getParameterTypes(); if(types.length==0 || types[0]!=Sesi.class) continue;
            for(Sesi sesi:new Sesi[]{null,palsu}) {
                Object[] values=new Object[types.length]; values[0]=sesi;
                for(int i=1;i<types.length;i++) if(types[i]==int.class) values[i]=1;
                try { method.invoke(model,values); throw new AssertionError(method.getName()+" menerima sesi invalid"); }
                catch(InvocationTargetException e) { if(!(e.getCause() instanceof MasalahAplikasi masalah) || masalah.getJenis()!=MasalahAplikasi.Jenis.SESI) throw e; }
                calls++;
            }
        }
        if(calls<40) throw new AssertionError("Operasi sesi belum semua tercakup");
        System.out.println("PASS PENERIMAAN T101: "+calls+" panggilan seluruh API Sesi null/palsu ditolak sebelum JDBC");
    }
}
