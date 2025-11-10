package it.damose.data;

import it.damose.model.Route;
import it.damose.model.Trip;
import it.damose.model.StopTime;

import java.io.*;
import java.util.*;

public class TripLoader {

    public static List<Trip> loadTripsFromResources() {
        // Prima carica i trip base da trips.txt
        Map<String, Trip> tripMap = new HashMap<>();

        InputStream tripInput = TripLoader.class.getClassLoader().getResourceAsStream("data/rome_static_gtfs/trips.txt");
        if (tripInput == null) {
            System.err.println("trips.txt non trovato!");
            return new ArrayList<>();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(tripInput))) {
            String line = br.readLine(); // salta intestazione
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // Controlla che ci siano abbastanza colonne (shape_id è all'indice 6)
                if (p.length >= 7) {
                    String routeId = p[0].replace("\"", "").trim();
                    String tripId = p[2].replace("\"", "").trim();
                    String shapeId = p[6].replace("\"", "").trim(); // Leggi shape_id

                    // Chiama il nuovo costruttore di Trip (con 3 argomenti)
                    Trip trip = new Trip(tripId, routeId, shapeId);
                    tripMap.put(tripId, trip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("TripLoader: " + tripMap.size() + " trip base caricati");

        // Ora carica gli stop_times e associali ai trip
        InputStream stopTimesInput = TripLoader.class.getClassLoader().getResourceAsStream("data/rome_static_gtfs/stop_times.txt");
        if (stopTimesInput == null) {
            System.err.println("stop_times.txt non trovato!");
            return new ArrayList<>(tripMap.values());
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stopTimesInput))) {
            String line = br.readLine(); // salta intestazione
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length >= 4) {
                    String tripId = p[0].replace("\"", "").trim();
                    String arrivalTime = p[1].replace("\"", "").trim();
                    String stopId = p[3].replace("\"", "").trim();

                    // Trova il viaggio corrispondente
                    Trip trip = tripMap.get(tripId);
                    if (trip != null) {
                        // Aggiungi l'orario della fermata al viaggio
                        StopTime st = new StopTime(tripId, stopId, arrivalTime);
                        trip.addStopTime(st);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("TripLoader: stop_times caricati per i trip");
        return new ArrayList<>(tripMap.values());
    }

    /**
     * Collega i viaggi (Trips) alle rispettive linee (Routes).
     */
    public static void linkTripsToRoutes(List<Trip> trips, List<Route> routes) {
        Map<String, Route> routeMap = new HashMap<>();
        for (Route r : routes) {
            routeMap.put(r.getId(), r);
        }

        for (Trip t : trips) {
            Route r = routeMap.get(t.getRouteId());
            if (r != null) {
                r.addTrip(t);
            }
        }
    }
}