# Tischreservierung HOLTER

DeskVision ist ein Prototyp fuer die Arbeitsplatzreservierung bei HOLTER. Benutzer:innen waehlen einen Standort und Raum, pruefen einen Zeitraum und reservieren einen freien Arbeitsplatz direkt in der Vogelperspektive.

## Technologien

- Angular 21 fuer die Weboberflaeche
- Microsoft Authentication Library (MSAL) mit Authorization Code Flow und PKCE
- Quarkus 3 mit RESTEasy Reactive und MongoDB Panache fuer das Backend
- Quarkus OIDC zur serverseitigen Prüfung von Microsoft-Entra-Tokens
- MongoDB 7 als NoSQL-Datenbank
- Docker Compose fuer die lokale MongoDB-Umgebung

## Projektstruktur

- `Frontend/`: Angular Components, Interfaces und Service fuer die REST-Aufrufe
- `Backend/`: Quarkus mit `boundary`, `repo`, `model` und `DTOs`
- `Datenbank/console.js`: Collections, Testdaten und Indizes
- `docker-compose.yml`: MongoDB-Image mit Port und Healthcheck
- `Dokumentation/`: Anforderungsdokument und Diplomarbeitsbeschreibung

## Umgesetzte Funktionen

- Standorte Wels und Linz mit jeweils einem IT- und Marketingraum
- 32 unterschiedlich angeordnete Arbeitsplaetze mit Position und Ausstattung
- Ausstattung als eingebettete Liste direkt am Arbeitsplatz
- Abteilung und AbteilungID direkt an Raum und Arbeitsplatz
- Benutzer-Testprofile mit Abteilung und bevorzugtem Raum
- Index fuer Abteilungen zur schnellen Suche
- Zeitraumpruefung direkt in der Raumansicht
- Equipment-Filter in der Raumansicht
- Reservierung ohne zusaetzliche Bestaetigungsseite
- Serverseitige Sperre: Nur Tische der eigenen Abteilung sind reservierbar
- Anzeige und Stornierung der eigenen Reservierungen
- Firmenlogin mit Microsoft Entra ID; lokaler Testlogin nur auf localhost
- Rollenabhaengige Navigation
- Room Builder fuer das Anlegen, Bearbeiten, Positionieren und Loeschen von Raeumen und Arbeitsplaetzen
- SVG-Grundrisseditor: Waende ziehen sowie Tueren, Fenster, Pflanzen, Klima, Saeulen, Feuerloescher und Sperrzonen platzieren und verschieben
- Rasterausrichtung, Wand-Andocken, Drehen, Duplizieren und Verwerfen ungespeicherter Grundrissaenderungen
- Undo/Redo, Mehrfachauswahl, Ausrichten, Ebenen, Zoom/Pan, Tastatur- und Touchbedienung
- Rechteckige und L-förmige Raumvorlagen sowie direkt bearbeitbare Beschriftungen
- Kollisionserkennung fuer Tische gegen Waende, Sperrzonen, Saeulen und andere Tische
- Neu angelegte Raeume werden direkt geladen und koennen ohne Seitenwechsel weiterbearbeitet werden
- Zentrale Equipment-Verwaltung mit Checkbox-Auswahl statt fehleranfaelliger Texteingabe
- Abteilungen anlegen und loeschen; fuer neue Abteilungen entsteht automatisch ein Testnutzer
- Admin-Uebersicht ueber die Reservierungen aller Mitarbeiter
- Reservierende Mitarbeiter werden direkt am belegten Tisch und im Detailbereich angezeigt
- Admins koennen aktive Reservierungen aus der Gesamtuebersicht stornieren
- Mitarbeiterkonten anlegen, deaktivieren, zuordnen und Passwort zuruecksetzen
- Standorte anlegen und bearbeiten
- Reservierungen suchen und nach Standort, Raum, Datum und Status filtern sowie als CSV exportieren
- Stornierungsgruende, Sicherheitsabfragen und Audit-Protokoll fuer Admin-Aktionen
- 14-Tage-Kalender, vergangene Reservierungen und Zeitraum bearbeiten
- Schnellwahl fuer Ganztag, Vormittag und Nachmittag sowie automatische Belegungsaktualisierung
- Eigene Reservierungen blau markieren und genaue Reservierungszeiten im Raumplan anzeigen
- Umschaltbare Namensanzeige: intern voller Name oder neutrale Anzeige `Belegt`
- Geschuetztes Loeschen fuer verwendete Abteilungen, Raeume und Equipment
- Admin-Aenderungen werden direkt in MongoDB gespeichert
- Keine Frontend-Demodaten: Ist MongoDB oder das Backend nicht erreichbar, wird ein Fehler angezeigt
- Zusammenhaengende Reservierung von einem frei waehlbaren Start- bis Enddatum
- Anzeige der nächsten zukünftigen Belegung eines Arbeitsplatzes
- Tische als Orientierung beim Bearbeiten des SVG-Grundrisses
- Persistentes Firmendesign mit Firmenname, Produktname, Logo, Farben, Vorschau, Ruecksetzen und Designverlauf fuer den Superadmin
- Mitarbeiter koennen durch den Superadmin sicher entfernt werden; das Konto wird deaktiviert und aus der Verwaltung ausgeblendet, waehrend die Reservierungshistorie erhalten bleibt

## Datenbank starten

Docker Desktop muss laufen. Danach im Projektordner:

```bash
docker compose up -d
cd Datenbank
mongosh mongodb://localhost:27018/tischreservierung --file console.js
cd ..
```

Das Skript muss aus dem Ordner `Datenbank` gestartet werden, damit die eingebundenen Dateien unter macOS und Windows gefunden werden. Es setzt die lokale Datenbank bewusst neu auf und erstellt unter anderem die Collections `benutzer`, `standorte`, `raeume`, `arbeitsplaetze`, `reservierungen`, `abteilungen`, `equipment` und `einstellungen`.

Wenn bestehende Räume und Reservierungen erhalten bleiben sollen, werden nur die
zusätzlichen HOLTER-Standorte und Standardräume ergänzt:

```bash
mongosh mongodb://localhost:27018/tischreservierung --file Datenbank/standorte-erweitern.js
```

Enthalten sind Wels, Linz, Salzburg, Hall in Tirol und Premstätten. Das
Erweiterungsskript kann mehrmals ausgeführt werden und überschreibt keine Räume,
die bereits im Room Builder bearbeitet wurden.

## Backend starten

```bash
cd Backend
./mvnw quarkus:dev
```

Das Backend laeuft unter `http://localhost:8080`. Die MongoDB-Verbindung steht in `Backend/src/main/resources/application.properties`.

Wichtige REST-Endpunkte:

- `GET /api/benutzer`
- `POST /api/benutzer/login`
- `GET /api/standorte`
- `GET /api/raeume/standort/{standortId}`
- `GET /api/raeume/{raumId}?datum=...&beginn=...&ende=...&benutzerId=...`
- `POST /api/reservierungen`
- `GET /api/reservierungen/benutzer/{benutzerId}`
- `DELETE /api/reservierungen/{id}?benutzerId=...`
- `POST|PUT|DELETE /api/admin/raeume...`
- `POST|PUT|DELETE /api/admin/arbeitsplaetze...`
- `GET|POST|DELETE /api/admin/abteilungen...`
- `GET|POST|DELETE /api/admin/equipment...`
- `GET /api/admin/reservierungen`
- `DELETE /api/admin/reservierungen/{id}`
- `GET|POST|PUT /api/admin/benutzer...`
- `PUT /api/admin/benutzer/{id}/passwort`
- `POST|PUT|DELETE /api/admin/standorte...`
- `GET /api/admin/audit`
- `PUT /api/reservierungen/{id}`

## Frontend starten

```bash
cd Frontend
npm install
npm start
```

Die Anwendung ist unter `http://localhost:3000` erreichbar. Dieser Port entspricht
der Redirect-URI aus dem bereitgestellten Entra-Beispiel.

## Microsoft-Entra-Login einrichten

Das Frontend verwendet die bereitgestellte Entra-App mit MSAL. Es gibt kein
Client-Secret im Browser. MSAL verwendet den Authorization Code Flow mit PKCE und
speichert die Anmeldung nur im `sessionStorage` des geöffneten Browser-Tabs.

In der Entra-App-Registrierung müssen einmalig folgende Einstellungen vorhanden sein:

1. Unter **Authentication** eine Plattform **Single-page application** anlegen.
2. Als lokale Redirect-URI `http://localhost:3000` eintragen.
3. Unter **API permissions** muss die im gelieferten Beispiel verwendete
   delegierte Microsoft-Graph-Berechtigung `User.Read` vorhanden sein.
4. Falls die Firmenrichtlinie es verlangt, muss ein Administrator dieser
   Berechtigung einmalig zustimmen.

Für eine spätere Produktivadresse muss zusätzlich deren HTTPS-Redirect-URI in Entra
eingetragen werden. Client-ID, Tenant-ID und Berechtigungen stehen bewusst gesammelt in
`Frontend/src/app/auth/entra-config.ts`. Diese IDs sind öffentliche Kennungen; ein
Client-Secret gehört ausschließlich auf einen Server und wird für diesen PKCE-Flow
nicht benötigt.

Nach der Microsoft-Anmeldung prüft Quarkus Signatur, Aussteller und Zielgruppe des
signierten Identitätstokens. Anschließend wird die
Entra-E-Mail mit dem Feld `email` in der MongoDB-Collection `benutzer` verglichen.
Abteilung, bevorzugter Raum und Rolle kommen ausschließlich aus MongoDB. Damit kann
ein Benutzer seine Adminrolle nicht über Daten aus dem Browser bestimmen.

Vor dem ersten Firmenlogin muss ein Superadmin deshalb in der Mitarbeiterverwaltung
die echte Microsoft-E-Mail-Adresse beim passenden Mitarbeiter eintragen. Existiert
noch keine Zuordnung, zeigt die Anmeldung eine verständliche Meldung und gewährt
keinen Zugriff.

Für die lokale Entwicklung ist das Entra-Konto
`d.bernecker@HTBLALeonding.onmicrosoft.com` dem Superadmin-Testprofil zugeordnet.
Die getrennte Eigenschaft `entraEmail` erhält dabei den bisherigen lokalen Login
`chef@holter.test`.

Das Entra-Konto `n.hofer@HTBLALeonding.onmicrosoft.com` ist dem normalen
Admin-Testprofil zugeordnet. Der lokale Login `admin@holter.test` bleibt erhalten.

Im Quarkus-Produktionsprofil (`prod`) sind die fachlichen `/api/*`-Endpunkte durch
Entra geschützt. Zusätzlich wird geprüft, ob eine mitgesendete Benutzer-ID zum
Microsoft-Konto des Tokens gehört. Auf `localhost` bleibt der bisherige Testlogin
einklappbar verfügbar, damit Entwicklung und Schulpräsentationen auch mit den
Testkonten möglich bleiben.

Testkonten:

- Mitarbeiter: `anna.leitner@example.test` / `holter123`
- Admin: `admin@holter.test` / `admin123`

Weitere Mitarbeiterkonten aus `Datenbank/console.js` verwenden ebenfalls das Passwort `holter123`.

## Aufbau kurz erklaert

Beim Start ruft `app.ts` ueber den `ArbeitsplatzService` Benutzer und Standorte vom Quarkus-Backend ab und speichert sie in Angular Signals. Die Components erhalten die aktuellen Daten ueber Inputs und melden Klicks oder Aenderungen ueber Outputs an `app.ts` zurueck. Quarkus liest und schreibt ausschliesslich in MongoDB und prueft beim Reservieren Zeitraum, Ueberschneidungen und die Abteilung des aktiven Benutzers.

Der Raumeditor ist mit SVG umgesetzt. Waende, Tueren, Pflanzen und Sperrzonen werden als einfache Geometrieobjekte im Feld `elemente` des Raums gespeichert. Der Admin bearbeitet genau diese Objekte; Mitarbeiter erhalten sie beim normalen REST-Abruf desselben Raums und sehen daher den aktuellen Grundriss. Arbeitsplaetze bleiben eigene MongoDB-Dokumente und werden mit Prozentkoordinaten ueber dem SVG dargestellt.

MongoDB verwendet keine JPA-Annotation `@Entity`. Die persistenten Dokumentklassen sind mit `@MongoEntity` annotiert, weil das Projekt eine dokumentenorientierte Datenbank verwendet.

Eine kurze Erklaerung der verwendeten Angular- und Backend-Techniken sowie eine moegliche spaetere Commit-Aufteilung steht in `TECHNIKEN.md`.
