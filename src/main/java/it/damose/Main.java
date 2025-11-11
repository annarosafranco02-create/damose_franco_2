package it.damose;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import it.damose.ui.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        FlatLaf.setGlobalExtraDefaults( Collections.singletonMap( "@accentColor", "#D32F2F" ) );
        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Errore nell'impostare il Look and Feel FlatLaf" );
        }
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
