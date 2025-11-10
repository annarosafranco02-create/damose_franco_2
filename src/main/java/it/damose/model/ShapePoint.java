package it.damose.model;

/**
 * Modello che rappresenta un singolo punto GPS di un "shape" (percorso).
 */
public class ShapePoint implements Comparable<ShapePoint> {

    private final String shapeId;
    private final double lat;
    private final double lon;
    private final int sequence; // L'ordine del punto

    public ShapePoint(String shapeId, double lat, double lon, int sequence) {
        this.shapeId = shapeId;
        this.lat = lat;
        this.lon = lon;
        this.sequence = sequence;
    }

    public String getShapeId() { return shapeId; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public int getSequence() { return sequence; }

    @Override
    public int compareTo(ShapePoint other) {
        // Questo ci serve per ordinare i punti nella sequenza corretta
        return Integer.compare(this.sequence, other.sequence);
    }
}