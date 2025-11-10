package it.damose.map;


import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.util.concurrent.*;

public class TilesManager {

    private static final String TILE_SERVER = "https://tile.openstreetmap.org";
    private static final String TILES_DIR = "tiles";
    private static final int TILE_SIZE = 256;

    // Zoomlimits per evitare download eccessivi
    public static final int MIN_ZOOM = 10;
    public static final int MAX_ZOOM = 16;

    private final ExecutorService downloadExecutor;
    private final ConcurrentHashMap<String, BufferedImage> cache;

    public TilesManager() {
        this.downloadExecutor = Executors.newFixedThreadPool(4);
        this.cache = new ConcurrentHashMap<>();

        // Crea la cartella tiles se non esiste
        try {
            Files.createDirectories(Paths.get(TILES_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        /**
         * Ottiene un tile dalla cache, dal disco o lo scarica
         */
        public BufferedImage getTile(int zoom, int x, int y) {
        // Applica restrizioni zoom
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        String key = getTileKey(zoom, x, y);

        // Controlla cache in memoria
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // Controlla se esiste su disco
        File tileFile = getTileFile(zoom, x, y);
        if (tileFile.exists()) {
            try {
                BufferedImage img = ImageIO.read(tileFile);
                cache.put(key, img);
                return img;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Scarica in modo asincrono
        downloadTileAsync(zoom, x, y);

        return null; // Restituisce null, verrà caricato successivamente
    }

        /**
         * Scarica un tile in modo asincrono
         */
        private void downloadTileAsync(int zoom, int x, int y) {
        downloadExecutor.submit(() -> downloadTile(zoom, x, y));
    }

        /**
         * Scarica un tile dal server OSM
         */
        private void downloadTile(int zoom, int x, int y) {
        String key = getTileKey(zoom, x, y);

        // Evita download duplicati
        if (cache.containsKey(key)) {
            return;
        }

        try {
            File tileFile = getTileFile(zoom, x, y);

            // Crea le directory necessarie
            tileFile.getParentFile().mkdirs();

            // Scarica il tile
            String urlString = String.format("%s/%d/%d/%d.png", TILE_SERVER, zoom, x, y);
            URL url = new URL(urlString);

            // Rispetta le policy di OSM: aggiungi User-Agent
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "JavaMapViewer/1.0");

            // Leggi l'immagine
            BufferedImage img = ImageIO.read(conn.getInputStream());

            if (img != null) {
                // Salva su disco
                ImageIO.write(img, "png", tileFile);

                // Aggiungi alla cache
                cache.put(key, img);
            }

            // Rispetta i limiti di rate: piccola pausa
            Thread.sleep(100);

        } catch (Exception e) {
            System.err.println("Errore scaricamento tile " + zoom + "/" + x + "/" + y + ": " + e.getMessage());
        }
    }

    /**
     * Converte coordinate lat/lon in coordinate tile
     */
    public static int[] latLonToTile(double lat, double lon, int zoom) {
    int x = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
    int y = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
    return new int[]{x, y};
    }

    /**
     * Converte coordinate tile in lat/lon (angolo in alto a sinistra)
     */
    public static double[] tileToLatLon(int x, int y, int zoom) {
    double n = Math.PI - 2 * Math.PI * y / (1 << zoom);
    double lat = Math.toDegrees(Math.atan(Math.sinh(n)));
    double lon = x / (double) (1 << zoom) * 360.0 - 180.0;
    return new double[]{lat, lon};
    }

    /**
     * Restituisce la chiave univoca per un tile
     */
    private String getTileKey(int zoom, int x, int y) {
    return zoom + "" + x + "" + y;
    }

    /**
     * Restituisce il file per un tile
     */
    private File getTileFile(int zoom, int x, int y) {
    return new File(TILES_DIR + File.separator + zoom + File.separator + x + File.separator + y + ".png");
    }


    /**
     * Pulisce le risorse
     */

    public void shutdown() {
        downloadExecutor.shutdown();
        try {
            downloadExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Ottiene la dimensione di un tile
     */
    public static int getTileSize() {
    return TILE_SIZE;
    }

}
