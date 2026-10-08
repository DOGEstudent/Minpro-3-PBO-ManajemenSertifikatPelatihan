package model;

import java.security.GeneralSecurityException;
import util.Kredensial;
import util.Validasi;

/** Akun hasil pembacaan DAO, tanpa password plaintext atau setter identitas/role. */
public final class Pengguna {
    private final String idPengguna, nama, usernameNormal;
    private final Role role;
    private final byte[] salt, hashPassword;
    private final int iterasi;
    public Pengguna(String id, String nama, String username, Role role, byte[] salt, byte[] hashPassword, int iterasi) {
        this.idPengguna = Validasi.idEntitas(id, "PGN");
        this.nama = Validasi.nama(nama, "Nama pengguna"); this.usernameNormal = Validasi.username(username);
        this.role = Validasi.wajib(role, "Role");
        if (salt == null || salt.length != 16 || hashPassword == null || hashPassword.length != 32 || iterasi != Kredensial.ITERASI)
            throw new IllegalArgumentException("Parameter hash akun tidak valid.");
        this.salt = salt.clone(); this.hashPassword = hashPassword.clone(); this.iterasi = iterasi;
    }
    public String getIdPengguna() { return idPengguna; }
    public String getNama() { return nama; }
    public String getUsernameNormal() { return usernameNormal; }
    public Role getRole() { return role; }
    public byte[] getSalt() { return salt.clone(); }
    public byte[] getHashPassword() { return hashPassword.clone(); }
    public int getIterasi() { return iterasi; }
    public boolean verifikasiPassword(char[] raw) throws GeneralSecurityException { return Kredensial.cocok(raw, salt, hashPassword); }
}
