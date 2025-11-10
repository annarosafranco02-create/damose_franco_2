package it.damose.model;

import java.util.ArrayList;
import java.util.List;

public class Stop {
    private final String id;
    private final String name;
    private final double lat;
    private final double lon;
    private final List<String> routeIds = new ArrayList<>();

    public Stop(String id, String name, double lat, double lon) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }

    public void addRoute(String routeId) {
        if (!routeIds.contains(routeId))
            routeIds.add(routeId);
    }

    public List<String> getRouteIds() {
        return routeIds;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}