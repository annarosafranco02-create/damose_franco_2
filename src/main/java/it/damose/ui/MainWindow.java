package it.damose.ui;

import it.damose.controller.*;
import it.damose.map.Mappa; // Importiamo il pannello Mappa
import it.damose.model.*;
import it.damose.realtime.RealtimeManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent; // Per la ricerca live
import javax.swing.event.DocumentListener; // Per la ricerca live
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Finestra principale (JFrame) dell'applicazione Rome Transit Tracker.
 * Contiene tutta l'interfaccia utente, inclusa la logica per il layout "dashboard".
 * Implementa ConnectionListener per reagire ai cambi di stato della rete.
 */
public class MainWindow extends JFrame implements ConnectionListener {

    // --- Controller (Backend) ---
    // Questi gestiscono tutta la logica di business.
    private final StopController controller;
    private final FavoritesManager favoritesManager;
    private final RealtimeManager realtimeManager;

    // --- Componenti UI (Frontend) ---
    // Componenti principali del layout
    private final DefaultListModel<Object> listModel = new DefaultListModel<>();
    private final JList<Object> resultsList = new JList<>(listModel);
    private final JTextArea detailArea = new JTextArea();
    private final JTextField searchField = new JTextField();

    // Il pannello della mappa, ora integrato in questa finestra
    private Mappa mappa;

    // Altri componenti UI
    private JButton btnToggleFavorite;
    private JButton btnShowFavorites;
    private JLabel lblConnectionStatus;
    private Timer statusClearTimer; // Timer per nascondere il label di stato

    // --- Gestione Stato Interno ---
    private Object currentSelectedObject; // L'oggetto (Stop o Route) selezionato nella lista
    private Route currentlySelectedRoute = null; // La linea selezionata, per filtrare la mappa
    private Timer searchDebounceTimer; // Timer per la ricerca live

    // --- Utilità ---
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

    /**
     * Costruttore della finestra principale.
     * Gestisce il login, inizializza i controller e costruisce la UI.
     */
    public MainWindow() {
        super("Rome Transit Tracker - Damose");

        // --- 1. Finestra di Login ---
        UserController userController = new UserController();
        LoginWindow loginDialog = new LoginWindow(this, userController);
        loginDialog.setVisible(true);

        String loggedInUsername = loginDialog.getLoggedInUsername();
        if (!loginDialog.didProceed()) {
            System.out.println("Accesso annullato. Chiusura applicazione.");
            System.exit(0);
        }

        // --- 2. Inizializzazione dei Controller ---
        controller = new StopController("src/main/resources/data/rome_static_gtfs");
        favoritesManager = new FavoritesManager(loggedInUsername);
        realtimeManager = new RealtimeManager();

        // Aggiorna il titolo della finestra in base all'utente
        if (loggedInUsername != null) {
            setTitle("Rome Transit Tracker - Utente: " + loggedInUsername);
        } else {
            setTitle("Rome Transit Tracker - Modalità Ospite");
        }

        // --- 3. Impostazioni Finestra ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Apri l'app massimizzata (tutto schermo)
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // --- 4. Costruzione Layout ---
        initLayout();
        loadAllData(); // Carica i dati iniziali nella lista

        // --- 5. Attivazione Gestori ---
        // Timer per la ricerca live "autocomplete"
        searchDebounceTimer = new Timer(300, e -> search());
        searchDebounceTimer.setRepeats(false);

        // Mettiamo in ascolto il ConnectionManager
        ConnectionManager.getInstance().addListener(this);
        ConnectionManager.getInstance().checkNow(); // Esegui il primo check (bloccante)

        // Se siamo online, avviamo subito i download
        if (ConnectionManager.getInstance().isOnline()) {
            realtimeManager.start();
        }

        // Aggiungiamo un "gancio" alla chiusura della finestra
        // per pulire le risorse (timer e cache)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (realtimeManager != null) {
                    realtimeManager.stop();
                }
                if (mappa != null) {
                    mappa.cleanup();
                }
            }
        });
    }

    /**
     * Inizializza tutti i componenti UI e li assembla nel layout "Dashboard".
     */
    private void initLayout() {

        // === PANNELLO SUPERIORE (Top) ===
        JPanel top = new JPanel(new BorderLayout(5, 5));

        // --- 1. TASTO HOME (a sinistra) ---
        JButton btnHome = new JButton("Home");
        btnHome.setFont(new Font("Arial", Font.BOLD, 12));
        btnHome.addActionListener(e -> {
            searchField.setText(""); // Svuota la barra di ricerca
            loadAllData(); // Ricarica la lista di tutte le fermate
        });

        JPanel homePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        homePanel.add(btnHome);
        homePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Margine a destra

        top.add(homePanel, BorderLayout.WEST); // Aggiungi a sinistra

        // --- 2. PANNELLO DI RICERCA (al centro) ---
        // Definito UNA SOLA VOLTA
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.add(searchField, BorderLayout.CENTER);
        JButton searchBtn = new JButton("Cerca");
        searchPanel.add(searchBtn, BorderLayout.EAST);
        top.add(searchPanel, BorderLayout.CENTER);

        // --- 3. PANNELLO PREFERITI (a destra) ---
        JPanel eastButtonsPanel = new JPanel(new GridLayout(1, 1, 5, 0));
        btnShowFavorites = new JButton("Preferiti");
        btnShowFavorites.setFont(new Font("Arial", Font.BOLD, 12));
        btnShowFavorites.addActionListener(e -> showFavorites());
        eastButtonsPanel.add(btnShowFavorites);
        top.add(eastButtonsPanel, BorderLayout.EAST);

        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(top, BorderLayout.NORTH); // Aggiungi il pannello superiore alla finestra

        // === PANNELLO SINISTRO (Left) ===

        // 1. Pannello Lista (in alto a sinistra)
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listPane = new JScrollPane(resultsList);

        // 2. Pannello Dettagli (in basso a sinistra)
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setBorder(BorderFactory.createTitledBorder("Dettaglio"));
        JScrollPane detailPane = new JScrollPane(detailArea);

        // Wrapper per bottone preferiti + dettagli
        JPanel detailWrapperPanel = new JPanel(new BorderLayout(0, 5));
        JPanel detailControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnToggleFavorite = new JButton("Aggiungi ai Preferiti");
        btnToggleFavorite.setFont(new Font("Arial", Font.BOLD, 12));
        btnToggleFavorite.setVisible(false);
        detailControlsPanel.add(btnToggleFavorite);
        detailWrapperPanel.add(detailControlsPanel, BorderLayout.NORTH);
        detailWrapperPanel.add(detailPane, BorderLayout.CENTER);

        // 3. Splitter Sinistro (Verticale)
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listPane, detailWrapperPanel);
        leftSplit.setDividerLocation(300);
        leftSplit.setResizeWeight(0.4);

        // === PANNELLO DESTRO (Right) ===

        // 4. Pannello Mappa
        mappa = new Mappa();
        mappa.setRealtimeManager(realtimeManager);
        mappa.setStopController(controller);

        // === SPLITTER PRINCIPALE (Main) ===

        // 5. Splitter Orizzontale (Sinistra | Destra)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, mappa);
        mainSplit.setDividerLocation(0.5);
        mainSplit.setResizeWeight(0.5);
        add(mainSplit, BorderLayout.CENTER);

        // === PANNELLO INFERIORE (Bottom) ===
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblConnectionStatus = new JLabel("Controllo connessione...");
        lblConnectionStatus.setFont(new Font("Arial", Font.ITALIC, 12));
        lblConnectionStatus.setVisible(false);
        statusPanel.add(lblConnectionStatus);
        add(statusPanel, BorderLayout.SOUTH);

        // Timer per nascondere il messaggio di stato
        statusClearTimer = new Timer(5000, e -> lblConnectionStatus.setVisible(false));
        statusClearTimer.setRepeats(false);

        // --- LISTENER RICERCA LIVE (Autocomplete) ---
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                triggerSearch();
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                triggerSearch();
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                // Non usato per campi di testo
            }
        });

        // --- ACTION LISTENERS ---
        // Bottone "Cerca" e tasto Invio (per confermare)
        ActionListener doSearch = e -> search();
        searchBtn.addActionListener(doSearch);
        searchField.addActionListener(doSearch);

        // Click singolo sulla lista
        resultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Object selected = resultsList.getSelectedValue();
                if (selected != null) {
                    showDetails(selected);
                } else {
                    btnToggleFavorite.setVisible(false);
                    currentSelectedObject = null;
                    detailArea.setText("");
                }
            }
        });

        // Doppio click sulla lista
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selected = resultsList.getSelectedValue();
                    if (selected != null) {
                        showOnMap(selected);
                    }
                }
            }
        });

        // Click sul bottone preferiti
        btnToggleFavorite.addActionListener(e -> toggleFavorite());
    }

    /**
     * Carica tutte le fermate nella lista (stato "Home").
     */
    private void loadAllData() {
        btnToggleFavorite.setVisible(false);
        currentSelectedObject = null;
        this.currentlySelectedRoute = null;
        if (mappa != null) {
            mappa.setFilteredRoute(null);
            mappa.setSelectedStop(null);
        }

        listModel.clear();
        detailArea.setText("Caricamento di tutte le fermate...");

        SwingUtilities.invokeLater(() -> {
            try {
                for (Stop s : controller.getStops()) {
                    listModel.addElement(s);
                }
                detailArea.setText("Dati caricati. Seleziona un elemento.\n\nSuggerimento: Doppio click su una fermata o linea per centrare la mappa.");
            } catch (Exception e) {
                detailArea.setText("Errore nel caricamento dati:\n" + e.getMessage());
            }
        });
    }

    /**
     * Chiamato ogni volta che il testo di ricerca cambia.
     * Riavvia il timer di "debounce" per evitare ricerche ad ogni tasto.
     */
    private void triggerSearch() {
        if (searchDebounceTimer != null) {
            searchDebounceTimer.restart();
        }
    }

    /**
     * Esegue la ricerca e aggiorna la lista con i risultati.
     */
    private void search() {
        btnToggleFavorite.setVisible(false);
        currentSelectedObject = null;
        this.currentlySelectedRoute = null;
        if (mappa != null) {
            mappa.setFilteredRoute(null);
            mappa.setSelectedStop(null);
        }

        String q = searchField.getText().trim();
        listModel.clear();

        if (q.isEmpty()) {
            loadAllData(); // Se la ricerca è vuota, ricarica tutto
            return;
        }

        // Non mostriamo "Ricerca in corso..." per la ricerca live
        // detailArea.setText("Ricerca in corso...");

        SwingUtilities.invokeLater(() -> {
            List<Route> routeResults = controller.searchRoutes(q);
            for (Route r : routeResults) listModel.addElement(r);

            List<Stop> stopResults = controller.searchStops(q);
            for (Stop s : stopResults) listModel.addElement(s);

            if (routeResults.isEmpty() && stopResults.isEmpty()) {
                detailArea.setText("Nessuna fermata o linea trovata per \"" + q + "\"");
            } else {
                detailArea.setText("Trovati " + listModel.getSize() + " risultati per \"" + q + "\".\n\nSuggerimento: Doppio click per centrare la mappa.");
                resultsList.setSelectedIndex(0);
            }
        });
    }

    /**
     * Mostra i dettagli per l'oggetto selezionato (Fermata o Linea) nel pannello dei dettagli.
     * Applica anche il filtro e l'evidenziazione alla mappa.
     */
    private void showDetails(Object obj) {
        currentSelectedObject = obj;
        updateFavoriteButtonState();
        btnToggleFavorite.setVisible(true);

        if (mappa != null) {
            if (obj instanceof Stop s) {
                this.currentlySelectedRoute = null;
                mappa.setSelectedStop(s); // Evidenzia fermata
                mappa.setFilteredRoute(null); // Rimuovi filtro linea
            } else if (obj instanceof Route r) {
                this.currentlySelectedRoute = r;
                mappa.setSelectedStop(null); // Rimuovi evidenziazione
                mappa.setFilteredRoute(r); // Filtra per linea
            }
        }

        // --- Costruzione del testo dei dettagli ---
        StringBuilder sb = new StringBuilder();

        if (obj instanceof Stop s) {
            // --- Dettagli per una FERMATA ---
            sb.append("═══════════════════════════════════════\n");
            sb.append("FERMATA: ").append(s.getName()).append("\n");
            sb.append("═══════════════════════════════════════\n");
            sb.append("ID: ").append(s.getId()).append("\n");
            sb.append("Coordinate: ").append(s.getLat()).append(", ").append(s.getLon()).append("\n\n");

            // --- Logica Arrivi (Online vs Offline) ---
            boolean isAppOnline = ConnectionManager.getInstance().isOnline();
            List<RealtimeArrival> liveArrivals = null;
            if (isAppOnline) {
                liveArrivals = realtimeManager.getArrivalsForStop(s.getId());
            }

            if (liveArrivals != null && !liveArrivals.isEmpty()) {
                // Se siamo ONLINE e abbiamo dati live
                sb.append("PROSSIMI ARRIVI (LIVE)\n");
                sb.append("───────────────────────────────────────\n");
                sb.append(String.format("%-16s %-6s %-15s\n", "LINEA", "ORA", "STATO"));
                sb.append("───────────────────────────────────────\n");

                int count = 0;
                for (RealtimeArrival ra : liveArrivals) {
                    Route route = controller.getRouteById(ra.getRouteId());
                    String routeName = (route != null) ? route.getName() : ra.getRouteId();
                    String time = timeFormatter.format(new Date(ra.getArrivalTime() * 1000));

                    String stato; // Traduci il ritardo in testo
                    if (ra.getDelay() > 60) {
                        stato = String.format("(+%d min)", ra.getDelay() / 60);
                    } else if (ra.getDelay() < -60) {
                        stato = String.format("(-%d min)", Math.abs(ra.getDelay()) / 60);
                    } else {
                        stato = "(In orario)";
                    }

                    sb.append(String.format("%-16s %-6s %-15s\n", routeName, time, stato));

                    if (++count >= 15) break; // Mostra al massimo 15 arrivi
                }
            } else {
                // Se siamo OFFLINE (o non ci sono dati live)
                sb.append("PROSSIMI ARRIVI PROGRAMMATI (OFFLINE)\n");
                sb.append("───────────────────────────────────────\n");
                List<StopTime> nextArrivals = controller.getNextArrivals(s, 15);
                if (nextArrivals.isEmpty()) sb.append("Nessun orario disponibile\n");
                else for (StopTime st : nextArrivals) sb.append(controller.getArrivalInfo(st)).append("\n");
            }

            // --- Logica Linee della Fermata ---
            sb.append("\nLINEE CHE SERVONO QUESTA FERMATA\n");
            sb.append("───────────────────────────────────────\n");
            List<Route> routes = controller.getRoutesForStop(s);
            if (routes != null && !routes.isEmpty()) {
                for (Route r : routes) sb.append("- ").append(r.getName()).append(" (").append(r.getId()).append(")\n");
            } else {
                sb.append("(Nessuna linea associata a questa fermata)\n");
            }

        } else if (obj instanceof Route r) {
            // --- Dettagli per una LINEA ---
            sb.append("═══════════════════════════════════════\n");
            sb.append("LINEA: ").append(r.getName()).append("\n");
            sb.append("═══════════════════════════════════════\n");
            sb.append("ID: ").append(r.getId()).append("\n\n");
            sb.append("FERMATE (in ordine)\n");
            sb.append("───────────────────────────────────────\n");
            List<Stop> stops = controller.getStopsForRoute(r);
            if (stops != null && !stops.isEmpty()) {
                int num = 1;
                for (Stop stop : stops) {
                    sb.append(num++).append(". ").append(stop.getName()).append(" (").append(stop.getId()).append(")\n   Lat: ").append(stop.getLat()).append(", Lon: ").append(stop.getLon()).append("\n");
                }
            } else {
                sb.append("(Nessuna fermata associata a questa linea)\n");
            }
        }

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0); // Torna all'inizio del testo
    }

    /**
     * Centra la mappa sull'elemento (chiamato dal doppio click).
     */
    private void showOnMap(Object obj) {
        if (mappa == null) return; // Sicurezza

        if (obj instanceof Stop stop) {
            mappa.setSelectedStop(stop); // Evidenzia la fermata
            mappa.centerOn(stop.getLat(), stop.getLon());
            mappa.setZoom(16); // Zoom alto
        } else if (obj instanceof Route route) {
            // (Il filtro è già stato impostato da showDetails)
            List<Stop> stops = controller.getStopsForRoute(route);
            if (stops != null && !stops.isEmpty()) {
                // Centra sulla prima fermata della linea
                mappa.centerOn(stops.get(0).getLat(), stops.get(0).getLon());
                mappa.setZoom(14); // Zoom medio
            }
        }
    }

    // --- Metodi per i Preferiti ---

    /**
     * Mostra solo gli elementi preferiti nella lista.
     */
    private void showFavorites() {
        listModel.clear();
        detailArea.setText("Caricamento preferiti...");
        btnToggleFavorite.setVisible(false);
        currentSelectedObject = null;
        if (mappa != null) {
            mappa.setFilteredRoute(null);
            mappa.setSelectedStop(null);
        }

        SwingUtilities.invokeLater(() -> {
            // Usa la ricerca per ID Esatto
            Set<String> stopIds = favoritesManager.getFavoriteStopIds();
            for (String id : stopIds) {
                Stop result = controller.getStopById(id);
                if (result != null) listModel.addElement(result);
            }
            Set<String> routeIds = favoritesManager.getFavoriteRouteIds();
            for (String id : routeIds) {
                Route result = controller.getRouteById(id);
                if (result != null) listModel.addElement(result);
            }

            if (listModel.isEmpty()) detailArea.setText("Non hai ancora aggiunto preferiti.");
            else {
                detailArea.setText("Trovati " + listModel.getSize() + " preferiti.");
                resultsList.setSelectedIndex(0);
            }
        });
    }

    /**
     * Aggiunge o rimuove l'elemento selezionato dai preferiti.
     */
    private void toggleFavorite() {
        if (currentSelectedObject == null) return;

        if (currentSelectedObject instanceof Stop s) {
            if (favoritesManager.isFavorite(s)) favoritesManager.removeFavoriteStop(s);
            else favoritesManager.addFavoriteStop(s);
        } else if (currentSelectedObject instanceof Route r) {
            if (favoritesManager.isFavorite(r)) favoritesManager.removeFavoriteRoute(r);
            else favoritesManager.addFavoriteRoute(r);
        }
        updateFavoriteButtonState(); // Aggiorna il testo del bottone
    }

    /**
     * Aggiorna il testo del bottone "Preferiti" (es. "Aggiungi" o "Rimuovi").
     */
    private void updateFavoriteButtonState() {
        if (currentSelectedObject == null) {
            btnToggleFavorite.setVisible(false);
            return;
        }
        boolean isFav = false;
        if (currentSelectedObject instanceof Stop s) isFav = favoritesManager.isFavorite(s);
        else if (currentSelectedObject instanceof Route r) isFav = favoritesManager.isFavorite(r);

        btnToggleFavorite.setText(isFav ? "Rimuovi dai Preferiti" : "Aggiungi ai Preferiti");
        btnToggleFavorite.setVisible(true);
    }

    // --- Metodo per il ConnectionListener ---

    /**
     * Chiamato da ConnectionManager quando lo stato della rete cambia.
     * Gestisce lo "switch automatico" online/offline.
     */
    @Override
    public void onConnectionStatusChanged(boolean isOnline, boolean isFirstCheck) {
        // Gli aggiornamenti della UI devono sempre avvenire sul thread di Swing
        SwingUtilities.invokeLater(() -> {
            String message;
            Color color;

            if (isOnline) {
                message = "Dispositivo ONLINE";
                color = new Color(0, 128, 0); // Verde scuro
                System.out.println("SWITCH: Avvio RealtimeManager");
                realtimeManager.start(); // Avvia gli aggiornamenti live
            } else {
                message = "Dispositivo OFFLINE";
                color = Color.RED;
                System.out.println("SWITCH: Fermo RealtimeManager");
                realtimeManager.stop(); // Ferma gli aggiornamenti live
            }

            // Aggiorna il label in basso
            lblConnectionStatus.setText(message);
            lblConnectionStatus.setForeground(color);
            lblConnectionStatus.setVisible(true);

            if (isFirstCheck) {
                // Se è il primo check all'avvio, nascondi dopo 5 sec
                statusClearTimer.restart();
            } else {
                // Se è un cambio di stato, mostra un popup di avviso
                JOptionPane.showMessageDialog(this,
                        "La connessione è cambiata.\nStato attuale: " + message,
                        "Stato Connessione",
                        JOptionPane.INFORMATION_MESSAGE);
                statusClearTimer.stop(); // Non nascondere l'avviso
            }

            // Forza un aggiornamento dei dettagli per riflettere il nuovo stato
            if (currentSelectedObject != null) {
                showDetails(currentSelectedObject);
            }
        });
    }
}