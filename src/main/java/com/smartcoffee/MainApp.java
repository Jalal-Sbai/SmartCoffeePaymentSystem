package com.smartcoffee;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Hauptklasse der Anwendung Smart Coffee & Payment System.
 * Erbt von javafx.application.Application und stellt den Einstiegspunkt dar.
 */
public class MainApp extends Application {

    /**
     * Start-Methode des JavaFX-Lebenszyklus.
     * Lädt die Benutzeroberfläche aus der Datei 'main_view.fxml' und zeigt das Fenster an.
     * 
     * @param primaryStage Das Hauptfenster der Anwendung
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // FXML-Datei für die Benutzeroberfläche laden
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_view.fxml"));
            Parent root = loader.load();

            // Fenster-Titel und Szene (Größe: 600x450 Pixel) festlegen
            primaryStage.setTitle("Smart Coffee & Payment System");
            primaryStage.setScene(new Scene(root, 600, 450));
            primaryStage.setResizable(false); // Feste Fenstergröße für saubere Darstellung
            
            // Fenster anzeigen
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("[Fehler beim Starten der JavaFX-Anwendung]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hauptmethode (Main), die beim Ausführen des Programms gestartet wird.
     * 
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        // Startet die JavaFX-Laufzeitumgebung und ruft start() auf
        launch(args);
    }
}
