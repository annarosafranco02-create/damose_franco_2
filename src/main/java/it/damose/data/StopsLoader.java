package it.damose.data;

import it.damose.model.Stop;
import it.damose.model.Route;

import java.io.*;
import java.util.*;

public class StopsLoader {

    public static Map<String, Stop> loadStopsFromResources() {
        Map<String, Stop> stops = new HashMap<>();

        InputStream input = StopsLoader.class.getClassLoader().getResourceAsStream("data/rome_static_gtfs/stops.txt");

        if (input == null) {
            System.err.println("stops.txt non trovato nelle risorse!");
            return stops;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            br.readLine(); // salta intestazione
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length >= 6) {
                    String id = p[0];
                    String name = p[2].replace("\"", "");
                    double lat = Double.parseDouble(p[4]);
                    double lon = Double.parseDouble(p[5]);
                    stops.put(id, new Stop(id, name, lat, lon));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("StopsLoader: " + stops.size() + " fermate caricate");
        return stops;
    }


    public static void linkStopsToRoutes(List<Stop> stops, List<Route> routes) {
        Map<String, Stop> stopMap = new HashMap<>();
        for (Stop s : stops) {
            stopMap.put(s.getId(), s);
        }

        for (Route r : routes) {
            for (String stopId : r.getAllStopIds()) {
                Stop s = stopMap.get(stopId);
                if (s != null) {
                    s.addRoute(r.getId());
                }
            }
        }
    }

}
