package com.smartcoffee;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Die Klasse Kaffeeautomat simuliert die physischen Ressourcen der Kaffeemaschine
 * (Kaffeebohnen-Bestand, Milch-Bestand, Tassenzähler sowie Defekt-Zustand).
 */
public class Kaffeeautomat {
    // Maximale und aktuelle Bestände
    private static final int MAX_KAFFEE = 2000; // in Gramm
    private static final int MAX_MILCH = 200;   // in Gramm

    private int kaffeeBestand;
    private int milchBestand;
    private int tassenAnzahl;
    private boolean defekt;

    private final List<String> historie;
    private final Random random;

    /**
     * Konstruktor: Initialisiert den Automaten mit vollen Beständen.
     */
    public Kaffeeautomat() {
        this.kaffeeBestand = MAX_KAFFEE;
        this.milchBestand = MAX_MILCH;
        this.tassenAnzahl = 0;
        this.defekt = false;
        this.historie = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Versucht, ein Getränk zuzubereiten und prüft dabei Zutatenbestand und Defekt-Zustand.
     * 
     * @param mitMilch true, wenn das Getränk Milch benötigt
     * @return true, wenn die Zubereitung erfolgreich war, sonst false
     */
    public synchronized boolean getraenkZubereiten(boolean mitMilch) {
        // Sicherheitsprüfung: Automat defekt?
        if (defekt) {
            System.out.println("[Kaffeeautomat]: Zubereitung abgebrochen – Automat ist defekt!");
            return false;
        }

        // Benötigte Mengen pro Tasse
        int kaffeeBedarf = 15; // 15g Kaffeebohnen pro Tasse
        int milchBedarf = mitMilch ? 50 : 0; // 50g Milch für Milchkaffee

        // Sicherheitsprüfung: Genügend Zutaten vorhanden?
        if (kaffeeBestand < kaffeeBedarf) {
            System.out.println("[Kaffeeautomat]: Nicht genügend Kaffeebohnen vorhanden!");
            return false;
        }
        if (milchBestand < milchBedarf) {
            System.out.println("[Kaffeeautomat]: Nicht genügend Milch vorhanden!");
            return false;
        }

        // Zutaten abziehen und Tasse zählen
        kaffeeBestand -= kaffeeBedarf;
        milchBestand -= milchBedarf;
        tassenAnzahl++;

        String getraenk = mitMilch ? "Kaffee mit Milch" : "Kaffee schwarz";
        historie.add("Zubereitet: " + getraenk);
        System.out.println("[Kaffeeautomat]: " + getraenk + " erfolgreich ausgegeben.");

        // Simulierte 10%-ige Chance auf einen technischen Defekt
        if (random.nextInt(100) < 10) {
            defekt = true;
            System.out.println("[Kaffeeautomat]: Warnung – Automat weist einen Defekt auf!");
        }

        return true;
    }

    /**
     * Füllt die Zutaten wieder vollständig auf und behebt eventuelle Defekte.
     */
    public synchronized void auffuellen() {
        this.kaffeeBestand = MAX_KAFFEE;
        this.milchBestand = MAX_MILCH;
        this.defekt = false;
        historie.add("Wartung durchgeführt: Bestände aufgefüllt, Defekt behoben.");
        System.out.println("[Kaffeeautomat]: Automat wurde erfolgreich gewartet.");
    }

    // Getter-Methoden für den Systemstatus
    public boolean isDefekt() { return defekt; }
    public int getKaffeeBestand() { return kaffeeBestand; }
    public int getMilchBestand() { return milchBestand; }
    public int getTassen() { return tassenAnzahl; }
    public List<String> getHistorie() { return new ArrayList<>(historie); }
}
