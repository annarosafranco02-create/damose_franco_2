package it.damose.model;

public class StopTime {
    private final String tripId;
    private final String stopId;
    private final String arrivalTime;

    public StopTime(String tripId, String stopId, String arrivalTime) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.arrivalTime = arrivalTime;
    }

    public String getTripId() { return tripId; }
    public String getStopId() { return stopId; }
    public String getArrivalTime() { return arrivalTime; }
}
