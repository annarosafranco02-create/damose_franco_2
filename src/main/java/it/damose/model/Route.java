package it.damose.model;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Route {
    private final String id;
    private final String name;
    private final String type;
    private final List<String> stopIds = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();

    public Route(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void addStopId(String stopId) {
        if (!stopIds.contains(stopId)) {
            stopIds.add(stopId);
        }
    }


    public List<String> getAllStopIds() {
        Set<String> stopIds = new HashSet<>();
        for (Trip trip : trips) {
            for (StopTime st : trip.getStopTimes()) {
                stopIds.add(st.getStopId());
            }
        }
        return new ArrayList<>(stopIds);
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
        for (String stopId : trip.getStopIds()) {
            addStopId(stopId);
        }
    }
    public List<Trip> getTrips() {
        return trips;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}