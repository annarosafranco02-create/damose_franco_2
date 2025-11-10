package it.damose.controller;

import it.damose.util.PasswordUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Gestisce la logica di autenticazione e registrazione degli utenti.
 * Salva i dati su "users.json".
 */
public class UserController {

    private static final String USERS_FILE = "users.json";
    private Map<String, String> userDatabase; // Memorizza username -> hashedPassword

    public UserController() {
        loadUsers();
    }

    /**
     * Prova a registrare un nuovo utente.
     * @return 0 = Successo, 1 = Utente già esistente, 2 = Password non valida
     */
    public int register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return 2;
        }
        if (password == null || password.trim().isEmpty()) {
            return 2;
        }

        if (userDatabase.containsKey(username.toLowerCase())) {
            return 1; // Utente già esistente
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        userDatabase.put(username.toLowerCase(), hashedPassword);
        saveUsers();

        return 0; // Successo
    }

    /**
     * Prova a loggare un utente.
     * @return true se il login ha successo, false altrimenti
     */
    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String hashedPassword = userDatabase.get(username.toLowerCase());
        if (hashedPassword == null) {
            return false; // Utente non trovato
        }

        // Controlla la password
        return PasswordUtil.checkPassword(password, hashedPassword);
    }


    private void loadUsers() {
        userDatabase = new HashMap<>();
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                return; // Nessun utente da caricare
            }

            String content = new String(Files.readAllBytes(Paths.get(USERS_FILE)), StandardCharsets.UTF_8);
            if (content.isEmpty()) {
                return;
            }

            JSONObject json = new JSONObject(new JSONTokener(content));
            for (String key : json.keySet()) {
                userDatabase.put(key, json.getString(key));
            }

        } catch (IOException e) {
            System.err.println("Errore caricamento " + USERS_FILE + ": " + e.getMessage());
        }
    }

    private void saveUsers() {
        JSONObject json = new JSONObject(userDatabase);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, StandardCharsets.UTF_8))) {
            writer.write(json.toString(4)); // 4 = indentazione
        } catch (IOException e) {
            System.err.println("Errore salvataggio " + USERS_FILE + ": " + e.getMessage());
        }
    }
}