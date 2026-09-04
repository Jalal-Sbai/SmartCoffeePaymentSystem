package com.smartcoffee;

import java.util.*;

/**
 * Die Klasse Muenzwechsler ist für die Verarbeitung von Münzeinzahlungen
 * und die exakte Berechnung von Wechselgeld verantwortlich.
 * 
 * Sicherheit:
 * Um Rundungsfehler bei Fließkommazahlen (float/double) zu vermeiden,
 * werden alle Geldbeträge intern in Cent (Integer) umgerechnet.
 */
public class Muenzwechsler {
    private final DatenbankManager dbManager;

    // Erlaubte Münzwerte in Euro
    private static final double[] MUENZ_WERTE = { 0.01, 0.02, 0.05, 0.10, 0.20, 0.50, 1.00, 2.00 };

    /**
     * Konstruktor
     * @param dbManager Instanz des DatenbankManagers zur Bestandskontrolle
     */
    public Muenzwechsler(DatenbankManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Berechnet das Wechselgeld mithilfe eines Greedy-Algorithmus.
     * 
     * @param eingezahlt Gezahlter Betrag in Euro
     * @param preis      Produktpreis in Euro
     * @return Liste der herauszugebenden Münzen oder null, falls Wechselgeld nicht passend herausgegeben werden kann
     */
    public List<Double> wechselgeldBerechnen(double eingezahlt, double preis) {
        // Umrechnung in Cent zur Vermeidung von Fließkomma-Rundungsfehlern
        long eingezahltCent = Math.round(eingezahlt * 100.0);
        long preisCent = Math.round(preis * 100.0);

        if (eingezahltCent < preisCent) {
            return null; // Noch nicht ausreichend gezahlt
        }

        long restCent = eingezahltCent - preisCent;
        if (restCent == 0) {
            return new ArrayList<>(); // Kein Wechselgeld erforderlich (passend gezahlt)
        }

        // Aktuellen Münzbestand aus der Datenbank laden
        List<String> bestandStrings = dbManager.getMuenzbestand();
        Map<Long, Integer> muenzBestandMap = new TreeMap<>(Collections.reverseOrder());

        for (int i = 0; i < MUENZ_WERTE.length; i++) {
            long coinCent = Math.round(MUENZ_WERTE[i] * 100.0);
            int anzahl = (i < bestandStrings.size()) ? Integer.parseInt(bestandStrings.get(i)) : 0;
            muenzBestandMap.put(coinCent, anzahl);
        }

        List<Double> ausgabeMuenzen = new ArrayList<>();

        // Greedy-Berechnung vom höchsten zum niedrigsten Münzwert
        for (Map.Entry<Long, Integer> entry : muenzBestandMap.entrySet()) {
            long coinCent = entry.getKey();
            int verfuegbar = entry.getValue();

            while (restCent >= coinCent && verfuegbar > 0) {
                ausgabeMuenzen.add(coinCent / 100.0);
                restCent -= coinCent;
                verfuegbar--;
            }
        }

        // Wenn der Rest nicht exakt 0 Cent erreicht, kann kein Wechselgeld herausgegeben werden
        if (restCent > 0) {
            return null;
        }

        // Münzbestand in der Datenbank verringern
        for (double coin : ausgabeMuenzen) {
            dbManager.muenzbestandAktualisieren(coin, -1);
        }

        return ausgabeMuenzen;
    }

    /**
     * Registriert die Annahme einer Münze im System und in der Datenbank.
     * 
     * @param muenztyp     Der Wert der Münze (z. B. 0.50 oder 2.00)
     * @param bestellungId Die zugehörige Bestellungs-ID
     */
    public void muenzeAnnehmen(double muenztyp, int bestellungId) {
        // Gültigkeit des Münzwerts prüfen
        boolean gueltig = false;
        for (double w : MUENZ_WERTE) {
            if (Math.abs(w - muenztyp) < 0.001) {
                gueltig = true;
                break;
            }
        }

        if (!gueltig) {
            System.err.println("[Muenzwechsler]: Ungültige Münze eingeworfen: " + muenztyp);
            return;
        }

        // In Datenbank protokollieren und Bestand um 1 erhöhen
        dbManager.zahlungSpeichern(bestellungId, muenztyp, 1);
        dbManager.muenzbestandAktualisieren(muenztyp, 1);
    }
}
