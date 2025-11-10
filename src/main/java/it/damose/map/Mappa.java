package it.damose.map;

import it.damose.controller.ConnectionManager;
import it.damose.controller.StopController;
import it.damose.model.Route;
import it.damose.model.Stop;
import it.damose.model.ShapePoint; // Import per i percorsi fluidi
import it.damose.model.VehiclePosition;
import it.damose.realtime.RealtimeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D; // Import per disegnare le linee
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Mappa extends JPanel {
    // Coordinate di Roma
    private static final double ROMA_LAT = 41.8902;
    private static final double ROMA_LON = 12.4922;

    private TilesManager tilesManager;

    // Parametri mappa
    private int zoom = 13;
    private double centerLat = ROMA_LAT;
    private double centerLon = ROMA_LON;

    // Drag
    private Point dragStart;
    private double dragStartLat;
    private double dragStartLon;

    // Timer
    private Timer refreshTimer;

    // Controller
    private RealtimeManager realtimeManager;
    private StopController stopController;

    // Cache per bus statici (offline)
    private List<VehiclePosition> simulatedVehicleCache = new ArrayList<>();
    private long lastStaticCacheTime = 0;

    // Filtro per la linea selezionata
    private Route currentlyFilteredRoute = null;
    private Stop selectedStop = null;
    public Mappa() {
        tilesManager = new TilesManager();

        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.LIGHT_GRAY);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                dragStartLat = centerLat;
                dragStartLon = centerLon;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;

                    double metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(centerLat)) / Math.pow(2, zoom);
                    double deltaLat = dy * metersPerPixel / 111320.0;
                    double deltaLon = -dx * metersPerPixel / (111320.0 * Math.cos(Math.toRadians(centerLat)));

                    centerLat = dragStartLat + deltaLat;
                    centerLon = dragStartLon + deltaLon;

                    centerLat = Math.max(-85, Math.min(85, centerLat));
                    centerLon = Math.max(-180, Math.min(180, centerLon));

                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            int newZoom = zoom - notches;
            newZoom = Math.max(TilesManager.MIN_ZOOM, Math.min(TilesManager.MAX_ZOOM, newZoom));
            if (newZoom != zoom) {
                zoom = newZoom;
                repaint();
            }
        });
        // Timer per refresh periodico
        refreshTimer = new Timer(500, e -> repaint());
        refreshTimer.start();
    }

    // --- Metodi Pubblici di Configurazione ---

    public void setRealtimeManager(RealtimeManager manager) {
        this.realtimeManager = manager;
    }

    public void setStopController(StopController controller) {
        this.stopController = controller;
    }

    public void setFilteredRoute(Route route) {
        this.currentlyFilteredRoute = route;
        repaint(); // Forza ridisegno
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (tilesManager != null) {
            tilesManager.shutdown();
        }
    }

    public void centerOn(double lat, double lon) {
        this.centerLat = lat;
        this.centerLon = lon;
        repaint();
    }

    public void setZoom(int newZoom) {
        this.zoom = Math.max(TilesManager.MIN_ZOOM, Math.min(TilesManager.MAX_ZOOM, newZoom));
        repaint();
    }

    public int getZoom() {
        return this.zoom;
    }

    // --- Logica di Disegno Principale ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int width = getWidth();
        int height = getHeight();

        // --- 1. DISEGNO TILES ---
        int[] centerTile = TilesManager.latLonToTile(centerLat, centerLon, zoom);
        int centerTileX = centerTile[0];
        int centerTileY = centerTile[1];

        double[] centerTileLatLon = TilesManager.tileToLatLon(centerTileX, centerTileY, zoom);
        double[] centerTileLatLonNext = TilesManager.tileToLatLon(centerTileX + 1, centerTileY + 1, zoom);

        int offsetX = (int) ((centerLon - centerTileLatLon[1]) / (centerTileLatLonNext[1] - centerTileLatLon[1]) * TilesManager.getTileSize());
        int offsetY = (int) ((centerLat - centerTileLatLon[0]) / (centerTileLatLonNext[0] - centerTileLatLon[0]) * TilesManager.getTileSize());

        int tilesX = (width / TilesManager.getTileSize()) + 2;
        int tilesY = (height / TilesManager.getTileSize()) + 2;

        for (int i = -tilesX / 2; i <= tilesX / 2; i++) {
            for (int j = -tilesY / 2; j <= tilesY / 2; j++) {
                int tileX = centerTileX + i;
                int tileY = centerTileY + j;

                int maxTile = (1 << zoom) - 1;
                if (tileX < 0 || tileX > maxTile || tileY < 0 || tileY > maxTile) {
                    continue;
                }

                BufferedImage tile = tilesManager.getTile(zoom, tileX, tileY);
                int drawX = width / 2 + (i * TilesManager.getTileSize()) - offsetX;
                int drawY = height / 2 + (j * TilesManager.getTileSize()) - offsetY;

                if (tile != null) {
                    g2d.drawImage(tile, drawX, drawY, TilesManager.getTileSize(), TilesManager.getTileSize(), null);
                } else {
                    g2d.setColor(new Color(230, 230, 230));
                    g2d.fillRect(drawX, drawY, TilesManager.getTileSize(), TilesManager.getTileSize());
                    g2d.setColor(Color.GRAY);
                    g2d.drawRect(drawX, drawY, TilesManager.getTileSize(), TilesManager.getTileSize());
                }
            }
        }

        //2. DISEGNO PERCORSO (con fallback) ---
        drawRoutePath(g2d);

        // 3. DISEGNO VEICOLI (Logica Online/Offline) ---
        boolean isCurrentlyLive = (realtimeManager != null &&
                ConnectionManager.getInstance().isOnline() &&
                realtimeManager.getVehiclePositions().size() > 0);

        if (isCurrentlyLive) {
            drawVehicles(g2d); // Disegna bus live (blu)
        } else {
            drawStaticVehicles(g2d); // Disegna bus simulati (grigi)
        }
        drawSelectedStop(g2d);

        // --- 4. DISEGNO INFO ZOOM ---
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 10, 150, 60);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Zoom: " + zoom, 20, 30);
        g2d.drawString(String.format("Lat: %.4f", centerLat), 20, 50);
        g2d.drawString(String.format("Lon: %.4f", centerLon), 20, 65);
    }

    // --- Metodi di Disegno Specifici ---
    private void drawVehicles(Graphics2D g2d) {
        if (realtimeManager == null || stopController == null) {
            return;
        }

        Collection<VehiclePosition> vehicles = realtimeManager.getVehiclePositions();
        if (vehicles.isEmpty()) {
            return;
        }

        AffineTransform oldTransform = g2d.getTransform();
        g2d.setFont(new Font("Arial", Font.BOLD, 10));

        for (VehiclePosition vehicle : vehicles) {

            // Filtra i bus non appartenenti alla linea selezionata
            if (currentlyFilteredRoute != null &&
                    !vehicle.getRouteId().equals(currentlyFilteredRoute.getId())) {
                continue;
            }

            Point p = latLonToScreenPixel(vehicle.getLatitude(), vehicle.getLongitude());
            int x = p.x;
            int y = p.y;
            float bearing = vehicle.getBearing();

            Route route = stopController.getRouteById(vehicle.getRouteId());
            String routeName = (route != null) ? route.getName() : vehicle.getRouteId();

            g2d.translate(x, y);
            if (bearing != -1f) {
                g2d.rotate(Math.toRadians(bearing));
            }

            Polygon triangle = new Polygon();
            triangle.addPoint(0, -7);
            triangle.addPoint(-5, 5);
            triangle.addPoint(5, 5);

            g2d.setColor(Color.BLUE);
            g2d.fill(triangle);
            g2d.setColor(Color.BLACK);
            g2d.draw(triangle);

            g2d.setTransform(oldTransform);
            g2d.setColor(Color.BLACK);
            g2d.drawString(routeName, x + 8, y + 4);
        }
        g2d.setTransform(oldTransform);
    }

    private void drawStaticVehicles(Graphics2D g2d) {
        if (stopController == null) return;

        long now = System.currentTimeMillis();

        // Aggiorna la cache dei bus statici se è più vecchia di 10 sec
        if (now - lastStaticCacheTime > 10000) {
            lastStaticCacheTime = now;
            new Thread(() -> {
                // Esegui la simulazione (LENTA) in background
                simulatedVehicleCache = stopController.getSimulatedVehiclePositions();
            }).start();
        }

        AffineTransform oldTransform = g2d.getTransform();
        g2d.setFont(new Font("Arial", Font.BOLD, 10));

        synchronized (simulatedVehicleCache) {
            for (VehiclePosition vehicle : simulatedVehicleCache) {

                // Filtra i bus non appartenenti alla linea selezionata
                if (currentlyFilteredRoute != null &&
                        !vehicle.getRouteId().equals(currentlyFilteredRoute.getId())) {
                    continue;
                }

                Point p = latLonToScreenPixel(vehicle.getLatitude(), vehicle.getLongitude());
                int x = p.x;
                int y = p.y;
                float bearing = vehicle.getBearing();

                Route route = stopController.getRouteById(vehicle.getRouteId());
                String routeName = (route != null) ? route.getName() : vehicle.getRouteId();

                g2d.translate(x, y);
                if (bearing != -1f) {
                    g2d.rotate(Math.toRadians(bearing));
                }

                Polygon triangle = new Polygon();
                triangle.addPoint(0, -7);
                triangle.addPoint(-5, 5);
                triangle.addPoint(5, 5);

                g2d.setColor(Color.DARK_GRAY); // Grigi per statico
                g2d.fill(triangle);
                g2d.setColor(Color.BLACK);
                g2d.draw(triangle);

                g2d.setTransform(oldTransform);
                g2d.setColor(Color.BLACK);
                g2d.drawString(routeName, x + 8, y + 4);
            }
        }
        g2d.setTransform(oldTransform);
    }

    /**
     * Disegna la linea del percorso (con fallback).
     * Prova prima a usare il percorso fluido (da shapes.txt).
     * Se fallisce, ripiega sulle linee rette tra le fermate.
     */
    private void drawRoutePath(Graphics2D g2d) {
        if (currentlyFilteredRoute == null || stopController == null) {
            return; // Nessuna linea selezionata
        }

        // Impostiamo lo stile di disegno
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(255, 0, 0, 150)); // Rosso semi-trasparente

        // --- OPZIONE A: Prova il percorso corretto ---
        List<ShapePoint> shapePoints = stopController.getShapeForRoute(currentlyFilteredRoute);

        if (shapePoints.size() >= 2) {
            // SUCCESSO: Dati trovati, disegna il percorso fluido
            Path2D.Double path = new Path2D.Double();

            Point startPoint = latLonToScreenPixel(shapePoints.get(0).getLat(), shapePoints.get(0).getLon());
            path.moveTo(startPoint.x, startPoint.y);

            for (int i = 1; i < shapePoints.size(); i++) {
                Point nextPoint = latLonToScreenPixel(shapePoints.get(i).getLat(), shapePoints.get(i).getLon());
                path.lineTo(nextPoint.x, nextPoint.y);
            }
            g2d.draw(path);

        } else {
            // --- OPZIONE B: Fallback (linee rette tra le fermate) ---
            List<Stop> stops = stopController.getStopsForRoute(currentlyFilteredRoute);

            if (stops.size() >= 2) {
                Path2D.Double path = new Path2D.Double();

                Point startPoint = latLonToScreenPixel(stops.get(0).getLat(), stops.get(0).getLon());
                path.moveTo(startPoint.x, startPoint.y);

                for (int i = 1; i < stops.size(); i++) {
                    Point nextPoint = latLonToScreenPixel(stops.get(i).getLat(), stops.get(i).getLon());
                    path.lineTo(nextPoint.x, nextPoint.y);
                }
                g2d.draw(path);
            }
        }
    }

    /**
     * Converte una coordinata geografica (Lat, Lon) in un punto
     * (x, y) sullo schermo.
     */
    private Point latLonToScreenPixel(double lat, double lon) {
        int width = getWidth();
        int height = getHeight();

        int[] centerTile = TilesManager.latLonToTile(centerLat, centerLon, zoom);
        double[] centerTileLatLon = TilesManager.tileToLatLon(centerTile[0], centerTile[1], zoom);
        double[] centerTileLatLonNext = TilesManager.tileToLatLon(centerTile[0] + 1, centerTile[1] + 1, zoom);
        int offsetX = (int) ((centerLon - centerTileLatLon[1]) / (centerTileLatLonNext[1] - centerTileLatLon[1]) * TilesManager.getTileSize());
        int offsetY = (int) ((centerLat - centerTileLatLon[0]) / (centerTileLatLonNext[0] - centerTileLatLon[0]) * TilesManager.getTileSize());

        int centerTileScreenX = width / 2 - offsetX;
        int centerTileScreenY = height / 2 - offsetY;

        int[] vehicleTile = TilesManager.latLonToTile(lat, lon, zoom);
        double[] vehicleTileLatLon = TilesManager.tileToLatLon(vehicleTile[0], vehicleTile[1], zoom);
        double[] vehicleTileLatLonNext = TilesManager.tileToLatLon(vehicleTile[0] + 1, vehicleTile[1] + 1, zoom);
        int vehicleOffsetX = (int) ((lon - vehicleTileLatLon[1]) / (vehicleTileLatLonNext[1] - vehicleTileLatLon[1]) * TilesManager.getTileSize());
        int vehicleOffsetY = (int) ((lat - vehicleTileLatLon[0]) / (vehicleTileLatLonNext[0] - vehicleTileLatLon[0]) * TilesManager.getTileSize());

        int tileDiffX = vehicleTile[0] - centerTile[0];
        int tileDiffY = vehicleTile[1] - centerTile[1];

        int finalX = centerTileScreenX + (tileDiffX * TilesManager.getTileSize()) + vehicleOffsetX;
        int finalY = centerTileScreenY + (tileDiffY * TilesManager.getTileSize()) + vehicleOffsetY;

        return new Point(finalX, finalY);
    }
    public void setSelectedStop(Stop stop) {
        this.selectedStop = stop;
        repaint(); // Forza un ridisegno per mostrare l'evidenziazione
    }
    private void drawSelectedStop(Graphics2D g2d) {
        if (selectedStop == null) {
            return; // Niente da evidenziare
        }

        // Converti le coordinate della fermata in pixel
        Point p = latLonToScreenPixel(selectedStop.getLat(), selectedStop.getLon());
        int x = p.x;
        int y = p.y;

        // Disegniamo un "alone" (un cerchio giallo semitrasparente)
        g2d.setColor(new Color(255, 215, 0, 150)); // Giallo oro, semitrasparente
        int haloSize = 16;
        g2d.fillOval(x - haloSize / 2, y - haloSize / 2, haloSize, haloSize);

        // Disegniamo un punto centrale più scuro
        g2d.setColor(new Color(230, 100, 0)); // Arancione
        int dotSize = 6;
        g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
    }
}