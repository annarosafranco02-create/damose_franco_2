package it.damose.model;

/**
 * POJO (Modello) che rappresenta una singola previsione di arrivo in tempo reale
 * per una fermata.
 */
public class RealtimeArrival {

    private final String routeId;
    private final long arrivalTime; // Timestamp UNIX (in secondi)
    private final int delay; // Ritardo in secondi

    public RealtimeArrival(String routeId, long arrivalTime, int delay) {
        this.routeId = routeId;
        this.arrivalTime = arrivalTime;
        this.delay = delay;
    }

    public String getRouteId() {
        return routeId;
    }

    /**
     * @return L'orario di arrivo previsto, in timestamp UNIX (secondi).
     */
    public long getArrivalTime() {
        return arrivalTime;
    }

    /**
     * @return Il ritardo in secondi (positivo = ritardo, negativo = anticipo).
     */
    public int getDelay() {
        return delay;
    }
}