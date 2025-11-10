package it.damose.controller;

import it.damose.model.Route;
import it.damose.model.Stop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Gestisce i preferiti.
 * Se l'username è null (Ospite), i preferiti sono solo in memoria (sessione).
 * Se l'username è fornito, i preferiti sono persistenti su file.
 */
public class FavoritesManager {

    // Se username è null, operiamo in "modalità sessione"
    private final String username;
    private final boolean isSessionOnly;
    private final String favoritesFile;

    private final Set<String> favoriteStopIds = new HashSet<>();
    private final Set<String> favoriteRouteIds = new HashSet<>();

    /**
     * Inizializza il manager dei preferiti.
     * @param username L'username dell'utente loggato, o null per la modalità Ospite.
     */
    public FavoritesManager(String username) {
        this.username = username;

        if (this.username == null || this.username.trim().isEmpty()) {
            this.isSessionOnly = true;
            this.favoritesFile = null;
            System.out.println("FavoritesManager: Avviato in modalità OSPITE (solo sessione).");
        } else {
            this.isSessionOnly = false;
            // File personalizzato per utente
            this.favoritesFile = this.username + "_favorites.txt";
            System.out.println("FavoritesManager: Avviato per l'utente " + this.username);
            loadFavorites();
        }
    }
    public void addFavoriteStop(Stop stop) {
        if (stop != null && favoriteStopIds.add(stop.getId())) {
            saveFavorites();
        }
    }

    public void removeFavoriteStop(Stop stop) {
        if (stop != null && favoriteStopIds.remove(stop.getId())) {
            saveFavorites();
        }
    }

    public boolean isFavorite(Stop stop) {
        return stop != null && favoriteStopIds.contains(stop.getId());
    }

    public Set<String> getFavoriteStopIds() {
        return new HashSet<>(favoriteStopIds);
    }

    public void addFavoriteRoute(Route route) {
        if (route != null && favoriteRouteIds.add(route.getId())) {
            saveFavorites();
        }
    }

    public void removeFavoriteRoute(Route route) {
        if (route != null && favoriteRouteIds.remove(route.getId())) {
            saveFavorites();
        }
    }

    public boolean isFavorite(Route route) {

        return route != null && favoriteRouteIds.contains(route.getId());
    }

    public Set<String> getFavoriteRouteIds() {

        return new HashSet<>(favoriteRouteIds);
    }


    private void loadFavorites() {
        // Non caricare nulla se siamo in modalità sessione
        if (isSessionOnly) {
            return;
        }

        File file = new File(favoritesFile);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("STOP:")) {
                    favoriteStopIds.add(line.substring(5));
                } else if (line.startsWith("ROUTE:")) {
                    favoriteRouteIds.add(line.substring(6));
                }
            }
        } catch (IOException e) {
            System.err.println("Errore durante il caricamento dei preferiti: " + e.getMessage());
        }
    }

    private void saveFavorites() {
        // Logica ospite
        if (isSessionOnly) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(favoritesFile, StandardCharsets.UTF_8))) {

            for (String id : favoriteStopIds) {
                writer.write("STOP:" + id);
                writer.newLine();
            }

            for (String id : favoriteRouteIds) {
                writer.write("ROUTE:" + id);
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("Errore durante il salvataggio dei preferiti: " + e.getMessage());
        }
    }
}