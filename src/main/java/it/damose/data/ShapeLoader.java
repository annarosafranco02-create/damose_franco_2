package it.damose.data;

import it.damose.model.ShapePoint;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShapeLoader {

    /**
     * Carica tutti i punti di shape da shapes.txt.
     *
     * @return Una Mappa dove la Key è lo shape_id e il Valore è la lista
     * (ordinata) di punti GPS per quel percorso.
     */
    public static Map<String, List<ShapePoint>> loadShapesFromResources() {
        // Usiamo una HashMap temporanea per raggruppare i punti
        Map<String, List<ShapePoint>> shapes = new HashMap<>();

        InputStream input = ShapeLoader.class.getClassLoader().getResourceAsStream("data/rome_static_gtfs/shapes.txt");

        if (input == null) {
            System.err.println("shapes.txt non trovato nelle risorse!");
            return shapes;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            br.readLine(); // Salta l'intestazione (header)
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length >= 4) {
                    String shapeId = p[0].replace("\"", "");
                    double lat = Double.parseDouble(p[1]);
                    double lon = Double.parseDouble(p[2]);
                    int sequence = Integer.parseInt(p[3]);

                    ShapePoint sp = new ShapePoint(shapeId, lat, lon, sequence);

                    // Aggiunge il punto alla lista giusta, creando la lista se non esiste
                    shapes.computeIfAbsent(shapeId, k -> new ArrayList<>()).add(sp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ora, ordina tutti i punti di ogni shape in base alla loro sequenza
        // Questo è fondamentale per disegnare la linea nell'ordine corretto
        for (List<ShapePoint> points : shapes.values()) {
            Collections.sort(points);
        }

        System.out.println("ShapeLoader: " + shapes.size() + " percorsi (shapes) caricati");
        return shapes;
    }
}