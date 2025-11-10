package it.damose.model;

public class VehiclePosition {

    private final String vehicleId;
    private final String routeId;
    private final double latitude;
    private final double longitude;
    private final float bearing;

    public VehiclePosition(String vehicleId, String routeId, double latitude, double longitude, float bearing) {
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getRouteId() {
        return routeId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "Vehicle " + vehicleId + " (Route " + routeId + ") @ " + latitude + "," + longitude;
    }
    public float getBearing() {
        return bearing;
    }
}
