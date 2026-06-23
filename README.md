# Tischreservierung Holter

Dieses Repository enthaelt den Projektstand fuer die Meilensteine:

> Das Datenmodell fuer Benutzer:innen, Arbeitsplaetze und Reservierungen ist umgesetzt.

> Die Arbeitsplatzdarstellung in Vogelperspektive ist umgesetzt und nutzbar.

## Inhalt

- `Datenbank/console.js`: MongoDB-Skript zum Erstellen und Befuellen der Datenbank
- `docker-compose.yml`: MongoDB-Container fuer die lokale Entwicklung
- `Backend/`: Quarkus REST-Backend fuer Arbeitsplatzdaten
- `Frontend/`: Angular-Oberflaeche fuer Einstieg, Standortauswahl und Raumuebersicht
- `Dokumentation/`: Dokumente zur Anforderungsanalyse und zum Projekt

## Datenmodell

Das Datenmodell wird in MongoDB umgesetzt. Das Skript erstellt folgende Collections:

- `benutzer`: speichert Benutzer:innen mit Rolle, Abteilung und bevorzugtem Raum
- `standorte`: speichert HOLTER-Standorte
- `raeume`: speichert Raeume mit Standort, Stockwerk und Abteilung
- `arbeitsplaetze`: speichert Tische mit Raum, Standort, Abteilung, Status, Position und Equipment
- `reservierungen`: speichert Reservierungen mit Benutzer, Arbeitsplatz, Raum, Standort, Zeitraum und Status

## Umgesetztes Feedback

- MongoDB-Image ist ueber `docker-compose.yml` eingebunden.
- Standorte und Raeume sind im Datenmodell vorhanden.
- Ausstattungen liegen direkt als Liste im Arbeitsplatz.
- Abteilungen sind direkt am Arbeitsplatz und Raum hinterlegt.
- Benutzer:innen haben einen bevorzugten Raum, der im Frontend vorgeschlagen wird.
- Raeume und Tische haben eine AbteilungID.
- IDs verwenden ein sprechendes Format wie `WELS-TISCH-101`, `LINZ-TISCH-201` oder `RAUM-WELS-OG1-TEAM`.
- Reservieren ist auf Tische der eigenen Abteilung begrenzt.
- Fuer die Reservierung ist keine extra Bestaetigungsseite vorgesehen.

## MongoDB starten

Voraussetzung ist Docker und Zugriff auf die Mongo Shell.

```bash
docker compose up -d
mongosh < Datenbank/console.js
```

Nach erfolgreicher Ausfuehrung wird die Datenbank `tischreservierung` erstellt und mit Testdaten befuellt.

## Backend

Das Backend ist ein Quarkus-Projekt und ist aehnlich wie die Pokemon-Uebung aufgebaut:

- `boundary`: REST-Endpunkte
- `model`: Mongo-Entity-/Model-Klassen fuer Benutzer, Standort, Raum, Arbeitsplatz, Position und Equipment
- `repo`: einfache Datenlogik
- `DTOs`: Datenobjekte fuer das Frontend

REST-Endpunkte:

- `GET /api/benutzer/demo`
- `GET /api/standorte`
- `GET /api/raeume/RAUM-WELS-OG1-TEAM`
- `GET /api/raeume/RAUM-LINZ-EG-PROJEKT`
- `POST /api/reservierungen`

Start:

```bash
cd Backend
./mvnw quarkus:dev
```

Falls kein Maven Wrapper vorhanden ist, kann alternativ Maven installiert und `mvn quarkus:dev` verwendet werden.

## Frontend

Das Frontend ist eine Angular-App. Es zeigt:

- kurze Einfuehrung zur Arbeitsplatzreservierung
- Standortauswahl
- unterschiedliche Demo-Raeume fuer Wels und Linz
- statischen Raum in Vogelperspektive mit 8 Tischen
- klickbare Arbeitsplaetze
- Status frei und reserviert
- Detailansicht mit Ausstattung und Abteilung

Die Angular-Oberflaeche ist in Components aufgeteilt:

- `intro`: Einstieg mit Standortauswahl
- `room-overview`: Raumplan in Vogelperspektive
- `desk-details`: Detailansicht zum ausgewaehlten Arbeitsplatz

Hinweis: Da MongoDB verwendet wird, sind die persistenten Models mit `@MongoEntity` annotiert. Bei einer SQL/JPA-Datenbank wuerde man stattdessen `@Entity` verwenden.

Start:

```bash
cd Frontend
npm install
npm start
```

Danach ist die Anwendung unter `http://localhost:4200` erreichbar.
