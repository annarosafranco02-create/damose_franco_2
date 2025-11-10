package it.damose.data;

import it.damose.model.StopTime;

import java.io.*;
import java.util.*;

public class StopTimesLoader {

    public static List<StopTime> loadStopTimesFromResources() {
        List<StopTime> stopTimes = new ArrayList<>();

        InputStream input = StopTimesLoader.class.getClassLoader()
                .getResourceAsStream("data/rome_static_gtfs/stop_times.txt");

        if (input == null) {
            System.err.println("stop_times.txt non trovato nelle risorse!");
            return stopTimes;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            br.readLine(); // salta intestazione
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length >= 4) {
                    String tripId = p[0];
                    String arrivalTime = p[1];
                    String stopId = p[3];
                    stopTimes.add(new StopTime(tripId, stopId, arrivalTime));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("StopTimesLoader: " + stopTimes.size() + " stop_times caricati");
        return stopTimes;
    }
}