package it.damose;

// Importa il tema
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import it.damose.ui.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Importa le classi per personalizzare i colori
import java.util.Map;
import java.util.HashMap;


public class Main {
    public static void main(String[] args) {
        // 1. Creiamo una mappa per le impostazioni personalizzate
        Map<String, String> newDefaults = new HashMap<>();

        // 2. Colore d'accento
        newDefaults.put( "@accentColor", "#D32F2F" );

        // 3. COLORE DI SFONDO
        newDefaults.put( "@background", "#FFEBEE" );

        FlatLaf.setGlobalExtraDefaults( newDefaults ); // Ora il tipo corrisponde

        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Errore nell'impostare il Look and Feel FlatLaf" );
        }

        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}