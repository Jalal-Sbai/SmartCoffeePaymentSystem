package com.smartcoffee;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller-Klasse für die JavaFX-Benutzeroberfläche (main_view.fxml).
 * Verbindet die visuelle Oberfläche mit der Logik von Kaffeeautomat,
 * DatenbankManager und Muenzwechsler.
 */
public class GUIController {

    // FXML Benutzeroberflächen-Elemente (aus main_view.fxml)
    @FXML private Label lblKaffee;
    @FXML private Label lblMilch;
    @FXML private Label lblTassen;
    @FXML private Label lblStatus;
    @FXML private Label lblPayment;
    @FXML private TextArea txtLog;
    @FXML private VBox paymentPane;

    // Logik-Komponenten
    private final Kaffeeautomat automat = new Kaffeeautomat();
    private final DatenbankManager dbManager = new DatenbankManager();
    private Muenzwechsler wechsler;

    // Aktueller Zustand des Bestellvorgangs
    private double aktuellerPreis = 0.0;
    private double eingezahlterBetrag = 0.0;
    private boolean mitMilchBestellt = false;
    private int aktuelleBestellungId = -1;

    /**
     * Initialisierungsmethode von JavaFX. Wird nach dem Laden des FXML aufgerufen.
     */
    @FXML
    public void initialize() {
        wechsler = new Muenzwechsler(dbManager);
        statusAnzeigeAktualisieren();
        log("System bereit. Bitte wählen Sie ein Getränk.");
    }

    /**
     * Event-Handler: Bestellung "Kaffee schwarz" (1.50 Euro)
     */
    @FXML
    private void handleOrderKaffee() {
        bestellungStarten("Kaffee schwarz", 1.50, false);
    }

    /**
     * Event-Handler: Bestellung "Kaffee mit Milch" (2.00 Euro)
     */
    @FXML
    private void handleOrderKaffeeMilch() {
        bestellungStarten("Kaffee mit Milch", 2.00, true);
    }

    /**
     * Startet einen neuen Bestellvorgang und zeigt den Zahlungsbereich an.
     */
    private void bestellungStarten(String getraenkTyp, double preis, boolean milch) {
        if (automat.isDefekt()) {
            log("FEHLER: Der Automat ist defekt! Bitte betätigen Sie die Wartungstaste.");
            return;
        }

        if (automat.getKaffeeBestand() < 15 || (milch && automat.getMilchBestand() < 50)) {
            log("FEHLER: Nicht genügend Zutaten für " + getraenkTyp + " vorhanden!");
            return;
        }

        this.aktuellerPreis = preis;
        this.eingezahlterBetrag = 0.0;
        this.mitMilchBestellt = milch;

        // Bestellung in der Datenbank protokollieren
        this.aktuelleBestellungId = dbManager.bestellungSpeichern(getraenkTyp, milch, preis);

        // Zahlungsbereich anzeigen
        paymentPane.setVisible(true);
        zahlungsAnzeigeAktualisieren();
        log("Bestellung gestartet: " + getraenkTyp + " (" + String.format("%.2f", preis) + " €)");
    }

    /**
     * Event-Handler: Münzeinwurf (1c bis 2€)
     */
    @FXML
    private void handleCoin(ActionEvent event) {
        try {
            Button btn = (Button) event.getSource();
            if (btn.getUserData() == null) return;

            double muenzwert = Double.parseDouble(btn.getUserData().toString());

            // Betrag addieren und kaufmännisch runden
            eingezahlterBetrag = Math.round((eingezahlterBetrag + muenzwert) * 100.0) / 100.0;
            
            // Münze im Wechsler und DB registrieren
            wechsler.muenzeAnnehmen(muenzwert, aktuelleBestellungId);

            zahlungsAnzeigeAktualisieren();

            // Prüfen, ob ausreichend gezahlt wurde
            if (eingezahlterBetrag >= aktuellerPreis) {
                zahlungAbschliessen();
            }
        } catch (Exception e) {
            log("Fehler bei Münzverarbeitung: " + e.getMessage());
        }
    }

    /**
     * Schließt die Zahlung ab, gibt Wechselgeld heraus und bereitet das Getränk zu.
     */
    private void zahlungAbschliessen() {
        List<Double> wechselgeld = wechsler.wechselgeldBerechnen(eingezahlterBetrag, aktuellerPreis);

        if (wechselgeld == null && eingezahlterBetrag > aktuellerPreis) {
            log("FEHLER: Wechselgeld konnte nicht herausgegeben werden! Vorgang abgebrochen.");
            paymentPane.setVisible(false);
            return;
        }

        // Getränk zubereiten
        if (automat.getraenkZubereiten(mitMilchBestellt)) {
            log(">>> Zahlvorgang erfolgreich! Getränk wird ausgegeben.");
            if (wechselgeld != null && !wechselgeld.isEmpty()) {
                log("Entnehmen Sie Ihr Wechselgeld: " + wechselgeld + " €");
            }
        } else {
            log("FEHLER bei der Zubereitung! Bitte überprüfen Sie den Automatenstatus.");
        }

        paymentPane.setVisible(false);
        statusAnzeigeAktualisieren();
    }

    /**
     * Event-Handler: Beendet den aktuellen Zahlvorgang
     */
    @FXML
    private void handleCancelPayment() {
        paymentPane.setVisible(false);
        log("Zahlvorgang abgebrochen.");
    }

    /**
     * Event-Handler: Füllt den Automaten auf und repariert ihn bei Defekt.
     */
    @FXML
    private void handleAuffuellen() {
        automat.auffuellen();
        statusAnzeigeAktualisieren();
        log("Wartung durchgeführt: Zutaten aufgeführt & Automat bereit.");
    }

    /**
     * Aktualisiert die Textanzeige für den eingegebenen Münzbetrag.
     */
    private void zahlungsAnzeigeAktualisieren() {
        lblPayment.setText(String.format("Gezahlt: %.2f € / Preis: %.2f €", eingezahlterBetrag, aktuellerPreis));
    }

    /**
     * Aktualisiert die Statusanzeigen für Zutatenbestand und Maschinenzustand.
     */
    private void statusAnzeigeAktualisieren() {
        lblKaffee.setText("Kaffee: " + automat.getKaffeeBestand() + " g");
        lblMilch.setText("Milch: " + automat.getMilchBestand() + " g");
        lblTassen.setText("Tassen gesamt: " + automat.getTassen());

        if (automat.isDefekt()) {
            lblStatus.setText("Status: DEFEKT");
            lblStatus.setTextFill(javafx.scene.paint.Color.RED);
        } else {
            lblStatus.setText("Status: Bereit");
            lblStatus.setTextFill(javafx.scene.paint.Color.GREEN);
        }
    }

    /**
     * Schreibt eine Protokollmeldung in das Textfeld.
     */
    private void log(String nachricht) {
        txtLog.appendText(nachricht + "\n");
    }
}
