package it.damose.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Trip {
    private final String id;
    private final String routeId;
    private final String shapeId; // Campo per il percorso

    // Inizializza le liste finali qui, così il costruttore è pulito
    private final List<String> stopIds = new ArrayList<>();
    private final List<StopTime> stopTimes = new ArrayList<>();

    // Costruttore che inizializza TUTTI i campi final richiesti
    public Trip(String id, String routeId, String shapeId) {
        this.id = id;
        this.routeId = routeId;
        this.shapeId = shapeId; // Questo risolve l'errore
    }

    public String getId() {
        return id; }

    public String getRouteId() {
        return routeId; }

    public String getShapeId() {
        return shapeId;
    }

    // Metodi esistenti (assicurati che siano così)
    public void addStopTime(StopTime st) {
        stopTimes.add(st);
        if (!stopIds.contains(st.getStopId())) {
            stopIds.add(st.getStopId());
        }
    }

    public List<StopTime> getStopTimes() {
        return stopTimes;
    }

    public List<String> getStopIds() {
        return stopIds;
    }
}