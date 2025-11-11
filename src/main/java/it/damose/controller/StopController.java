package it.damose.controller;
import it.damose.model.VehiclePosition;
import it.damose.data.RouteLoader;
import it.damose.data.StopsLoader;
import it.damose.data.TripLoader;
import it.damose.model.Route;
import it.damose.model.Stop;
import it.damose.model.Trip;
import it.damose.model.StopTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.*;
import java.util.stream.Collectors;
import it.damose.data.ShapeLoader;
import it.damose.model.ShapePoint;
import java.util.Collections;
import java.util.List;



public class StopController {

    private final List<Stop> stops = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();
    private final Map<String, List<ShapePoint>> shapeMap = new HashMap<>();
    private final Map<String, Stop> stopMap = new HashMap<>();
    private final Map<String, Route> routeMap = new HashMap<>();
    private final Map<String, Trip> tripMap = new HashMap<>();

    public StopController(String s) {
        stopMap.putAll(StopsLoader.loadStopsFromResources());
        routeMap.putAll(RouteLoader.loadRoutesFromResources());
        trips.addAll(TripLoader.loadTripsFromResources());
        shapeMap.putAll(ShapeLoader.loadShapesFromResources());
        stops.addAll(stopMap.values());
        routes.addAll(routeMap.values());
        for (Trip trip : trips) {
            tripMap.put(trip.getId(), trip);
        }
        TripLoader.linkTripsToRoutes(trips, routes);
        StopsLoader.linkStopsToRoutes(stops, routes);

        System.out.println("Fermate caricate: " + stops.size());
        System.out.println("Linee caricate: " + routes.size());
        System.out.println("Viaggi caricati: " + trips.size());
        System.out.println("Percorsi (shapes) caricati: " + shapeMap.size());
    }

    public List<Stop> getStops() {
        return stops;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public List<StopTime> getNextArrivals(Stop stop, int limit) {
        if (stop == null) {
            return Collections.emptyList();
        }

        List<StopTime> allArrivals = new ArrayList<>();

        // Raccogli tutti gli stopTimes per questa fermata
        for (Trip trip : tripMap.values()) {
            for (StopTime st : trip.getStopTimes()) {
                if (st.getStopId().equals(stop.getId())) {
                    allArrivals.add(st);
                }
            }
        }

        // Ordina per orario di arrivo
        allArrivals.sort(Comparator.comparing(StopTime::getArrivalTime));

        // Restituisci solo i prossimi N arrivi
        return allArrivals.stream().limit(limit).collect(Collectors.toList());
    }

    // Metodo helper per ottenere info complete
    public String getArrivalInfo(StopTime st) {
        Trip trip = tripMap.get(st.getTripId());
        if (trip == null) return "N/A";

        Route route = routeMap.get(trip.getRouteId());
        String routeName = (route != null) ? route.getName() : trip.getRouteId();

        return routeName + " - " + st.getArrivalTime();
    }

    public List<Route> searchRoutes(String query) {
        List<Route> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (Route r : routes) {
            if (r.getName().toLowerCase().contains(q) || r.getId().equalsIgnoreCase(q)) {
                results.add(r);
            }
        }
        return results;
    }

    public List<Stop> searchStops(String query) {
        List<Stop> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (Stop s : stops) {
            if (s.getName().toLowerCase().contains(q) || s.getId().equalsIgnoreCase(q)) {
                results.add(s);
            }
        }
        return results;
    }

    public List<Stop> getStopsForRoute(Route route) {
        if (route == null || route.getAllStopIds() == null) {
            return Collections.emptyList();
        }

        List<Stop> result = new ArrayList<>();
        for (String stopId : route.getAllStopIds()) {
            Stop s = stopMap.get(stopId);
            if (s != null) result.add(s);
        }
        return result;
    }

    public List<Route> getRoutesForStop(Stop stop) {
        if (stop == null) {
            return Collections.emptyList();
        }

        System.out.println("=== DEBUG getRoutesForStop ===");
        System.out.println("Fermata cercata: " + stop.getName() + " (ID: '" + stop.getId() + "')");

        // Cerca se l'ID esiste in qualche modo negli stopTimes
        Set<String> allStopIds = new HashSet<>();
        int count = 0;
        for (Trip trip : tripMap.values()) {
            for (StopTime st : trip.getStopTimes()) {
                allStopIds.add(st.getStopId());
                count++;
                if (count > 1000) break; // Limita per non crashare
            }
            if (count > 1000) break;
        }

        System.out.println("Primi 20 stopId negli stopTimes: ");
        allStopIds.stream().limit(20).forEach(id -> System.out.println("  - " + id));

        // Cerca match parziali
        System.out.println("\nCerca match parziali con '" + stop.getId() + "':");
        allStopIds.stream()
                .filter(id -> id.contains(stop.getId()) || stop.getId().contains(id))
                .limit(10)
                .forEach(id -> System.out.println("  POSSIBILE MATCH: " + id));

        System.out.println("========================\n");

        // Logica originale
        Set<String> routeIds = new HashSet<>();

        for (Trip trip : tripMap.values()) {
            List<StopTime> stopTimes = trip.getStopTimes();
            if (stopTimes != null) {
                for (StopTime st : stopTimes) {
                    if (st.getStopId().equals(stop.getId())) {
                        routeIds.add(trip.getRouteId());
                        break;
                    }
                }
            }
        }

        List<Route> result = new ArrayList<>();
        for (String routeId : routeIds) {
            Route r = routeMap.get(routeId);
            if (r != null) {
                result.add(r);
            }
        }

        result.sort(Comparator.comparing(Route::getName));
        return result;
    }
    public Route getRouteById(String id) {
        return routeMap.get(id);
    }
    private long timeToSeconds(String time) {
        // Gestisce orari "impossibili" (es. 24:01:00)
        if (time.startsWith("24")) time = time.replace("24", "00");
        if (time.startsWith("25")) time = time.replace("25", "01");

        try {
            String[] parts = time.split(":");
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            return (hours * 3600) + (minutes * 60) + seconds;
        } catch (Exception e) {
            return -1;
        }
    }
    private float calculateBearing(Stop s1, Stop s2) {
        double lat1 = Math.toRadians(s1.getLat());
        double lon1 = Math.toRadians(s1.getLon());
        double lat2 = Math.toRadians(s2.getLat());
        double lon2 = Math.toRadians(s2.getLon());

        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float) (bearing + 360) % 360; // Normalizza a 0-360
    }
    public List<VehiclePosition> getSimulatedVehiclePositions() {
        List<VehiclePosition> simulatedVehicles = new ArrayList<>();

        // 1. Ottieni l'orario corrente (in secondi da mezzanotte)
        java.time.LocalTime now = java.time.LocalTime.now();
        long nowInSeconds = now.toSecondOfDay();

        // 2. Itera su tutti i viaggi
        for (Trip trip : trips) {
            List<StopTime> stopTimes = trip.getStopTimes();
            if (stopTimes.size() < 2) continue; // Viaggio non valido

            // 3. Itera sulle "gambe" del viaggio (da Stop A a Stop B)
            for (int i = 0; i < stopTimes.size() - 1; i++) {
                StopTime st1 = stopTimes.get(i);
                StopTime st2 = stopTimes.get(i + 1);

                long time1 = timeToSeconds(st1.getArrivalTime());
                long time2 = timeToSeconds(st2.getArrivalTime());

                // 4. Controlla se il bus è "adesso" tra queste due fermate
                if (time1 != -1 && time2 != -1 && time1 <= nowInSeconds && time2 >= nowInSeconds) {

                    // Se viene trovato, si calcola la posizione.
                    Stop s1 = stopMap.get(st1.getStopId());
                    Stop s2 = stopMap.get(st2.getStopId());

                    if (s1 == null || s2 == null) continue;

                    // 5. Calcola a che percentuale del tragitto si trova
                    double segmentDuration = time2 - time1;
                    double timeElapsed = nowInSeconds - time1;
                    double progress = (segmentDuration == 0) ? 0 : (timeElapsed / segmentDuration);

                    // 6. Interpola le coordinate
                    double lat = s1.getLat() + (s2.getLat() - s1.getLat()) * progress;
                    double lon = s1.getLon() + (s2.getLon() - s1.getLon()) * progress;

                    // 7. Calcola la direzione
                    float bearing = calculateBearing(s1, s2);

                    // 8. Crea il nostro oggetto VehiclePosition (riutilizziamo il modello!)
                    // Usiamo l'ID del viaggio come ID del veicolo per la simulazione
                    VehiclePosition vp = new VehiclePosition(trip.getId(), trip.getRouteId(), lat, lon, bearing);
                    simulatedVehicles.add(vp);

                    break;
                }
            }
        }
        return simulatedVehicles;
    }
    // IN StopController.java
    public List<ShapePoint> getShapeForRoute(Route route) {
        if (route == null || route.getTrips().isEmpty()) {
            System.out.println("DEBUG SHAPE: No, la rotta o i suoi viaggi sono nulli."); // DEBUG
            return Collections.emptyList();
        }

        String shapeId = null;
        for (Trip trip : route.getTrips()) {
            if (trip.getShapeId() != null && !trip.getShapeId().isEmpty()) {
                shapeId = trip.getShapeId();
                break;
            }
        }

        if (shapeId == null) {
            System.out.println("DEBUG SHAPE: No. La linea " + route.getName() + " non ha uno shape_id nei suoi viaggi."); // DEBUG
            return Collections.emptyList();
        }

        List<ShapePoint> points = shapeMap.getOrDefault(shapeId, Collections.emptyList());
        if (points.isEmpty()) {
            System.out.println("DEBUG SHAPE: No. Trovato shape_id " + shapeId + " ma non ci sono punti in shapes.txt"); // DEBUG
        } else {
            System.out.println("DEBUG SHAPE: OK! Trovati " + points.size() + " punti per lo shape " + shapeId); // DEBUG
        }

        return points;
    }
    public Stop getStopById(String id) {
        return stopMap.get(id); // stopMap usa già l'ID come chiave
    }
}