package it.damose.realtime;

import com.google.transit.realtime.GtfsRealtime;
import it.damose.model.RealtimeArrival;
import it.damose.model.VehiclePosition;

import javax.swing.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce il download e il parsing dei dati GTFS-Realtime (GTFS-RT)
 * per le posizioni dei veicoli e le previsioni di arrivo.
 */
public class RealtimeManager {

    // URL pubblici per i feed di Roma Mobilità
    private static final String VEHICLE_POSITIONS_URL = "https://romamobilita.it/sites/default/files/rome_rtgtfs_vehicle_positions_feed.pb";
    private static final String TRIP_UPDATES_URL = "https://romamobilita.it/sites/default/files/rome_rtgtfs_trip_updates_feed.pb";

    // Frequenza di aggiornamento (30 secondi)
    private static final int REFRESH_INTERVAL_MS = 30 * 1000;

    // Timer Swing per l'aggiornamento automatico
    private final Timer refreshTimer;

    // Mappe Thread-Safe per contenere i dati live
    private final Map<String, VehiclePosition> vehiclePositions;
    private final ConcurrentHashMap<String, List<RealtimeArrival>> realtimeArrivals;

    public RealtimeManager() {
        this.vehiclePositions = new ConcurrentHashMap<>();
        this.realtimeArrivals = new ConcurrentHashMap<>();

        // Imposta il Timer
        this.refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> updateRealtimeData());
        this.refreshTimer.setInitialDelay(0); // Esegui subito al primo avvio
    }

    /**
     * Avvia il timer per gli aggiornamenti automatici.
     */
    public void start() {
        refreshTimer.start();
    }

    /**
     * Ferma il timer.
     */
    public void stop() {
        refreshTimer.stop();
    }

    /**
     * @return Una collezione di tutte le VehiclePosition
     */
    public Collection<VehiclePosition> getVehiclePositions() {
        return vehiclePositions.values();
    }

    /**
     * Metodo principale (chiamato dal Timer)
     * Avvia in thread separati l'aggiornamento delle posizioni e dei trip.
     */
    private void updateRealtimeData() {
        System.out.println("RealtimeManager: Inizio aggiornamento dati live...");

        new Thread(() -> {
            fetchVehiclePositions();
            fetchTripUpdates();
        }).start();
    }

    /**
     * Scarica e parsa le posizioni dei veicoli (GPS)
     */
    private void fetchVehiclePositions() {
        try (InputStream input = openConnection(VEHICLE_POSITIONS_URL)) {
            if (input == null) return;

            GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(input);
            vehiclePositions.clear();

            int count = 0;
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (entity.hasVehicle()) {
                    GtfsRealtime.VehiclePosition gtfsPos = entity.getVehicle();

                    // Logica affollamento (OccupancyStatus) rimossa

                    String vehicleId = gtfsPos.getVehicle().getId();
                    String routeId = gtfsPos.getTrip().getRouteId();
                    double lat = gtfsPos.getPosition().getLatitude();
                    double lon = gtfsPos.getPosition().getLongitude();
                    float bearing = gtfsPos.getPosition().hasBearing() ? gtfsPos.getPosition().getBearing() : -1f;

                    if (vehicleId != null && !vehicleId.isEmpty() && routeId != null) {
                        VehiclePosition vp = new VehiclePosition(vehicleId, routeId, lat, lon, bearing);
                        vehiclePositions.put(vehicleId, vp);
                        count++;
                    }
                }
            }
            System.out.println("RealtimeManager: Aggiornamento completato. " + count + " veicoli live trovati.");
        } catch (Exception e) {
            System.err.println("RealtimeManager: Errore durante l'aggiornamento POSIZIONI: " + e.getMessage());
        }
    }

    /**
     * Scarica e parsa gli aggiornamenti delle corse (PREVISIONI)
     */
    private void fetchTripUpdates() {
        try (InputStream input = openConnection(TRIP_UPDATES_URL)) {
            if (input == null) return;

            GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(input);
            realtimeArrivals.clear();

            int count = 0;
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                    String routeId = tripUpdate.getTrip().getRouteId();

                    // Logica affollamento (OccupancyStatus) rimossa

                    // Itera sulle fermate di questo viaggio
                    for (GtfsRealtime.TripUpdate.StopTimeUpdate stopUpdate : tripUpdate.getStopTimeUpdateList()) {
                        String stopId = stopUpdate.getStopId();

                        if (stopUpdate.hasArrival()) {
                            long arrivalTime = stopUpdate.getArrival().getTime();
                            int delay = stopUpdate.getArrival().getDelay();

                            // Ignora previsioni passate
                            if (arrivalTime * 1000 > System.currentTimeMillis()) {
                                // Usiamo il costruttore a 3 argomenti (senza affollamento)
                                RealtimeArrival arrival = new RealtimeArrival(routeId, arrivalTime, delay);

                                realtimeArrivals.computeIfAbsent(stopId, k -> new ArrayList<>()).add(arrival);
                                count++;
                            }
                        }
                    }
                }
            }
            System.out.println("RealtimeManager: Aggiornamento completato. " + count + " previsioni live trovate.");

        } catch (Exception e) {
            System.err.println("RealtimeManager: Errore durante l'aggiornamento PREVISIONI: " + e.getMessage());
        }
    }

    /**
     * Metodo helper per aprire connessioni HTTP
     */
    private InputStream openConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Damose-Transit-Tracker/1.0");

        if (conn.getResponseCode() != 200) {
            System.err.println("RealtimeManager: Errore HTTP " + conn.getResponseCode() + " per " + urlString);
            return null;
        }
        return conn.getInputStream();
    }

    /**
     * Ottiene la lista di arrivi in tempo reale per una specifica fermata.
     * @param stopId L'ID della fermata
     * @return Una lista (ordinata) di arrivi, o una lista vuota.
     */
    public List<RealtimeArrival> getArrivalsForStop(String stopId) {
        List<RealtimeArrival> arrivals = realtimeArrivals.getOrDefault(stopId, Collections.emptyList());
        arrivals.sort(Comparator.comparingLong(RealtimeArrival::getArrivalTime));
        return arrivals;
    }
}