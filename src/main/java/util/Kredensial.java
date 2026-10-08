package util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Kredensial {
    public static final int ITERASI = 600_000;
    private Kredensial() { }
    public static byte[] saltBaru() {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt); return salt;
    }
    public static byte[] hash(char[] password, byte[] salt) throws GeneralSecurityException {
        Validasi.password(password);
        if (password == null || salt == null || salt.length != 16)
            throw new IllegalArgumentException("Parameter kredensial tidak valid.");
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERASI, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    public static boolean cocok(char[] password, byte[] salt, byte[] expected) throws GeneralSecurityException {
        byte[] actual = hash(password, salt);
        try { return MessageDigest.isEqual(actual, expected); }
        finally { java.util.Arrays.fill(actual, (byte) 0); }
    }
}
