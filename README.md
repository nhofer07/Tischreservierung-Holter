# Tischreservierung Holter

Dieses Repository enthaelt den Projektstand zum Meilenstein:

> Das Datenmodell fuer Benutzer:innen, Arbeitsplaetze und Reservierungen ist umgesetzt.

## Inhalt

- `Datenbank/console.js`: MongoDB-Skript zum Erstellen und Befuellen der Datenbank
- `Dokumentation/`: Dokumente zur Anforderungsanalyse und zum Projekt

## Datenmodell

Das Datenmodell wird in MongoDB umgesetzt. Das Skript erstellt folgende Collections:

- `benutzer`: speichert Benutzer:innen mit Kontaktdaten, Rolle und Abteilung
- `abteilungen`: speichert die organisatorischen Abteilungen
- `arbeitsplaetze`: speichert die verfuegbaren Arbeitsplaetze mit Tischnummer und Abteilung
- `ausstattungen`: speichert moegliche Ausstattungsmerkmale eines Arbeitsplatzes
- `arbeitsplatz_ausstattung`: verbindet Arbeitsplaetze mit Ausstattungen und Mengenangaben
- `reservierungen`: speichert Reservierungen mit Benutzer, Arbeitsplatz, Zeitraum und Status

## Beziehungen

- Ein:e Benutzer:in gehoert ueber `abteilungId` zu einer Abteilung.
- Ein Arbeitsplatz gehoert ueber `abteilungId` zu einer Abteilung.
- Eine Reservierung verweist ueber `benutzerId` auf eine:n Benutzer:in.
- Eine Reservierung verweist ueber `tischnr` auf einen Arbeitsplatz.
- Arbeitsplatz-Ausstattungen werden ueber `arbeitsplatz_ausstattung` abgebildet.

## Testdaten

Das Skript fuegt Beispielabteilungen, Ausstattungen, Arbeitsplaetze, Benutzer:innen und eine Reservierung ein. Dadurch kann das Datenmodell direkt nach dem Ausfuehren nachvollzogen und getestet werden.

## Indexe

Zur Verbesserung der Datenqualitaet und Abfragegeschwindigkeit werden Indexe angelegt:

- eindeutiger Index auf `benutzer.email`
- eindeutiger Index auf `arbeitsplaetze.tischnr`
- Indexe auf Reservierungen nach Benutzer, Arbeitsplatz und Zeitraum
- Indexe fuer die Arbeitsplatz-Ausstattungs-Zuordnung

## Ausfuehrung

Voraussetzung ist eine laufende MongoDB-Instanz und Zugriff auf die Mongo Shell.

```bash
mongosh < Datenbank/console.js
```

Nach erfolgreicher Ausfuehrung wird die Datenbank `tischreservierung` erstellt und mit den Testdaten befuellt.

## Meilensteinstatus

Der Meilenstein ist umgesetzt, weil das Datenmodell fuer Benutzer:innen, Arbeitsplaetze und Reservierungen inklusive Testdaten und grundlegender Indexe vorhanden ist.
