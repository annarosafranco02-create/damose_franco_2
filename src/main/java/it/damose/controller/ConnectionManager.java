package it.damose.controller;

import javax.swing.Timer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitora lo stato della connessione
 * e notifica le altre parti dell'app (come MainWindow).
 */
public class ConnectionManager {

    private static ConnectionManager instance;
    private final Timer connectionTimer;
    private boolean isOnline = false; // Parte da offline per forzare un primo check
    private boolean isFirstCheck = true;

    private final List<ConnectionListener> listeners = new ArrayList<>();

    // Controlla la connessione ogni 15 secondi
    private static final int CHECK_INTERVAL_MS = 15 * 1000;

    private ConnectionManager() {
        // Il timer si occupa dei controlli periodici IN BACKGROUND
        connectionTimer = new Timer(CHECK_INTERVAL_MS, e -> checkNowInBackground());

        // Il primo check all'avvio è manuale (lo fa MainWindow),
        // quindi il timer automatico parte dopo 15 secondi.
        connectionTimer.setInitialDelay(CHECK_INTERVAL_MS);
        connectionTimer.start();
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /**
     * Controlla attivamente la connessione a Internet.
     */
    public void checkNow() {
        boolean previousState = isOnline;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("romamobilita.it", 80), 3000);
            isOnline = true;
            System.out.println("DEBUG ConnectionManager: checkNow() = ONLINE");
        } catch (IOException e) {
            isOnline = false;
            System.out.println("DEBUG ConnectionManager: checkNow() = OFFLINE (" + e.getMessage() + ")");
        }

        if (previousState != isOnline || isFirstCheck) {
            notifyListeners();
            isFirstCheck = false;
        }

    }

    /**
     * Chiamato dal Timer per eseguire il check in un thread separato,
     * in modo da non bloccare mai l'interfaccia utente.
     */
    private void checkNowInBackground() {
        // Avviamo il check bloccante in un thread tutto suo
        new Thread(() -> checkNow()).start();
    }


    /**
     * Informa tutti i "listener" (come MainWindow) che lo stato è cambiato.
     */
    private void notifyListeners() {
        for (ConnectionListener listener : listeners) {
            // Sarà poi MainWindow a gestire l'aggiornamento della UI
            listener.onConnectionStatusChanged(isOnline, isFirstCheck);
        }
    }

    public void addListener(ConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * @return true se l'ultimo check ha rilevato una connessione, false altrimenti.
     */
    public boolean isOnline() {
        return isOnline;
    }
}