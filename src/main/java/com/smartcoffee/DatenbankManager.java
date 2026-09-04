package com.smartcoffee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Der DatenbankManager verwaltet die SQLite-Datenbankverbindung und führt
 * alle Lese- und Schreiboperationen für Bestellungen, Zahlungen und den Münzbestand durch.
 * 
 * Sicherheitsaspekte:
 * - Verwendung von PreparedStatements zur Vermeidung von SQL-Injections.
 * - Sicheres Ressourcen-Management via Try-with-Resources.
 */
public class DatenbankManager {
    // Pfad zur SQLite-Datenbankdatei
    private static final String DB_URL = "jdbc:sqlite:coffee_system.db";

    /**
     * Konstruktor: Initialisiert die Datenbankverbindung und erstellt benötigte Tabellen,
     * falls diese noch nicht existieren.
     */
    public DatenbankManager() {
        tabellenInitialisieren();
    }

    /**
     * Erstellt die Tabellen 'Bestellungen', 'Münzbestand' und 'Zahlungen' in der Datenbank.
     * Befüllt auch den Münzbestand mit Standardwerten, falls die Tabelle leer ist.
     */
    private synchronized void tabellenInitialisieren() {
        String createBestellungen = "CREATE TABLE IF NOT EXISTS Bestellungen (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Kaffeeart TEXT NOT NULL, " +
                "MitMilch BOOLEAN NOT NULL, " +
                "Preis REAL NOT NULL, " +
                "Zeitstempel DATETIME DEFAULT CURRENT_TIMESTAMP)";

        String createMuenzbestand = "CREATE TABLE IF NOT EXISTS Münzbestand (" +
                "Münztyp REAL PRIMARY KEY, " +
                "Anzahl INTEGER NOT NULL CHECK (Anzahl >= 0))";

        String createZahlungen = "CREATE TABLE IF NOT EXISTS Zahlungen (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Bestellung_ID INTEGER NOT NULL, " +
                "Münztyp REAL NOT NULL, " +
                "Anzahl INTEGER NOT NULL, " +
                "FOREIGN KEY (Bestellung_ID) REFERENCES Bestellungen(ID))";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Tabellen erstellen
            stmt.execute(createBestellungen);
            stmt.execute(createMuenzbestand);
            stmt.execute(createZahlungen);

            // Prüfen, ob Münzbestand initialisiert werden muss
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Münzbestand")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Standard-Münzbestand eintragen (jeweils 20 Stück pro Münztyp)
                    double[] muenzen = {0.01, 0.02, 0.05, 0.10, 0.20, 0.50, 1.00, 2.00};
                    String insertSql = "INSERT INTO Münzbestand (Münztyp, Anzahl) VALUES (?, ?)";
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        for (double m : muenzen) {
                            pstmt.setDouble(1, m);
                            pstmt.setInt(2, 20); // 20 Münzen pro Typ zum Start
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[Fehler bei Datenbank-Initialisierung]: " + e.getMessage());
        }
    }

    /**
     * Speichert eine neue Bestellung in der Datenbank.
     * 
     * @param kaffeeart Name des Getränks (z. B. "Kaffee", "Kaffee mit Milch")
     * @param mitMilch   Ob das Getränk Milch enthält
     * @param preis     Der Preis in Euro
     * @return Generierte Bestellungs-ID oder -1 bei Fehler
     */
    public int bestellungSpeichern(String kaffeeart, boolean mitMilch, double preis) {
        String sql = "INSERT INTO Bestellungen(Kaffeeart, MitMilch, Preis) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, kaffeeart);
            pstmt.setBoolean(2, mitMilch);
            pstmt.setDouble(3, preis);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Generierte ID zurückgeben
                }
            }
        } catch (SQLException e) {
            System.err.println("[Fehler beim Speichern der Bestellung]: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Registriert eine gezahlte Münze für eine bestimmte Bestellung.
     * 
     * @param bestellungId ID der zugehörigen Bestellung
     * @param muenztyp     Der Münzwert (z. B. 0.50 oder 1.00)
     * @param anzahl       Anzahl der eingeworfenen Münzen
     */
    public void zahlungSpeichern(int bestellungId, double muenztyp, int anzahl) {
        String sql = "INSERT INTO Zahlungen(Bestellung_ID, Münztyp, Anzahl) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bestellungId);
            pstmt.setDouble(2, muenztyp);
            pstmt.setInt(3, anzahl);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Fehler beim Speichern der Zahlung]: " + e.getMessage());
        }
    }

    /**
     * Aktualisiert den Münzbestand in der Datenbank (Erhöhung oder Verringerung).
     * 
     * @param muenztyp Der Münzwert (z. B. 1.00)
     * @param delta    Veränderung der Anzahl (+1 bei Einwurf, -1 bei Wechselgeldabgabe)
     */
    public void muenzbestandAktualisieren(double muenztyp, int delta) {
        String sql = "UPDATE Münzbestand SET Anzahl = Anzahl + ? WHERE Münztyp = ? AND (Anzahl + ?) >= 0";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, delta);
            pstmt.setDouble(2, muenztyp);
            pstmt.setInt(3, delta);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Fehler beim Aktualisieren des Münzbestands]: " + e.getMessage());
        }
    }

    /**
     * Ruft den aktuellen Münzbestand sortiert nach Münzwert ab.
     * 
     * @return Liste der Anzahlen pro Münztyp als Strings
     */
    public List<String> getMuenzbestand() {
        List<String> bestand = new ArrayList<>();
        String sql = "SELECT Anzahl FROM Münzbestand ORDER BY Münztyp ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bestand.add(String.valueOf(rs.getInt("Anzahl")));
            }
        } catch (SQLException e) {
            System.err.println("[Fehler beim Lesen des Münzbestands]: " + e.getMessage());
        }
        return bestand;
    }
}
