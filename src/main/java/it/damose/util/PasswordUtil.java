package it.damose.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Classe di utilità per l'hashing e la verifica delle password.
 */
public class PasswordUtil {

    // Algoritmo di hashing (sicuro e standard)
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Crea un hash della password
     * @param password La password in chiaro
     * @return L'hash della password in formato Base64
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Questo non dovrebbe mai succedere con SHA-256
            throw new RuntimeException("Errore critico di hashing", e);
        }
    }

    /**
     * Controlla se una password in chiaro corrisponde a un hash salvato
     * @param plainPassword La password in chiaro inserita dall'utente
     * @param hashedPassword L'hash salvato nel database (file)
     * @return true se corrispondono, false altrimenti
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        String hashOfPlain = hashPassword(plainPassword);
        return hashOfPlain.equals(hashedPassword);
    }
}