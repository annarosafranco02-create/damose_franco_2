package it.damose.data;

import it.damose.model.Route;

import java.io.*;
import java.util.*;

public class RouteLoader {

    public static Map<String, Route> loadRoutesFromResources() {
        Map<String, Route> routes = new HashMap<>();

        InputStream input = RouteLoader.class.getClassLoader().getResourceAsStream("data/rome_static_gtfs/routes.txt");

        if (input == null) {
            System.err.println("routes.txt non trovato nelle risorse!");
            return routes;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            br.readLine(); // salta intestazione
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length >= 3) {
                    String id = p[0];
                    String name = p[2].replace("\"", "");
                    String type = p[1].replace("\"", "");
                    routes.put(id, new Route(id, name, type));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("RouteLoader: " + routes.size() + " linee caricate");
        return routes;
    }
}