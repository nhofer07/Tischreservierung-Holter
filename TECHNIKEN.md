# Verwendete Techniken

Die Anwendung bleibt bewusst bei einfachen Angular- und Java-Grundlagen.

## Angular

- `signal`: Speichert den aktuellen Benutzer, Raum, Zeitraum und Meldungen. Wenn sich ein Wert aendert, aktualisiert Angular die Anzeige.
- `@Input` und `@Output`: Die vorhandenen Components erhalten Daten von `app.ts` und melden Benutzeraktionen wieder nach oben.
- `HttpClient`: Alle fachlichen Daten kommen ueber REST aus Quarkus. Im Frontend gibt es keine Ersatz- oder Fallback-Daten.
- Template-Forms mit `ngModel`: Login und Room Builder verwenden einfache Formularbindung ohne zusaetzliche Formularbibliothek.
- CSS Media Queries: Desktop und Mobilgeraete verwenden dieselben Components, aber passende Anordnungen.
- SVG-Grundriss: Der Room Builder zeichnet Waende mit Ziehen und platziert Tueren, Fenster, Pflanzen, Klimageraete, Saeulen, Feuerloescher und Sperrzonen per Klick. Elemente koennen verschoben, skaliert, gedreht, dupliziert und geloescht werden. Tueren und Fenster docken automatisch an einer nahen Wand an. Jedes Element bleibt ein einfaches Objekt mit Typ, Position, Breite und Hoehe. Dasselbe SVG wird in der Mitarbeiteransicht angezeigt.
- Editor-Historie: Vor einer Aenderung wird eine flache Kopie des Element-Arrays gespeichert. Rueckgaengig und Wiederholen halten hoechstens 30 Zustaende, damit der Speicherverbrauch klein bleibt.
- Mehrfachauswahl: Shift-Klick sammelt Element-IDs. Ausrichten und gemeinsames Verschieben arbeiten direkt auf diesen ausgewaehlten Objekten.
- SVG-Ansicht: Zoom und Verschieben veraendern nur den `viewBox`. Die gespeicherten Koordinaten bleiben deshalb immer Prozentwerte von 0 bis 100.
- Touch und Tastatur: Pointer Events funktionieren mit Maus, Stift und Touch. Entf, Escape, Pfeiltasten sowie Strg/Cmd+Z und Strg/Cmd+Y bilden die wichtigsten Schnellaktionen ab.

## Quarkus und MongoDB

- REST-Boundaries: Endpunkte nehmen Requests entgegen und geben DTOs zurueck.
- DTO Records: Login-, Raum- und Arbeitsplatzdaten werden als kleine, klar definierte Datenpakete uebertragen.
- Rollenpruefung im Backend: Admin-Aenderungen werden serverseitig nur akzeptiert, wenn der Benutzer die Rolle `ADMIN` hat.
- Passwort-Hash: Testpasswoerter stehen nicht im Klartext in MongoDB. Fuer ein echtes Produkt waere statt einfachem SHA-256 ein Verfahren wie bcrypt oder Argon2 und eine echte Session/JWT-Loesung erforderlich.
- Positionswerte: Tische speichern `x` und `y` als Prozentwerte. Dadurch bleibt derselbe Grundriss auf verschiedenen Bildschirmgroessen nutzbar.
- Raum-Elemente: Ein Raum speichert sein Array `elemente` direkt im MongoDB-Dokument. `PUT /api/admin/raeume/{id}` ersetzt den gespeicherten Grundriss; der naechste Raumabruf eines Mitarbeiters liefert deshalb sofort die Admin-Aenderung.
- Reservierungsinhaber: Bei der Zeitraumpruefung verbindet Quarkus die aktive Reservierung mit dem Benutzer. Das Arbeitsplatz-DTO liefert `reserviertVon`, damit Name und Belegung direkt im Grundriss sichtbar sind.
- Admin-Stornierung: `DELETE /api/admin/reservierungen/{id}` prueft zuerst die Admin-Rolle und setzt eine aktive Reservierung auf `storniert`.
- Mitarbeiterverwaltung: Admins koennen Konten anlegen, deaktivieren, Abteilung und bevorzugten Raum aendern sowie ein Passwort zuruecksetzen.
- Audit-Log: Wichtige Admin-Aenderungen werden mit Admin-ID, Aktion, Details und Zeitpunkt in `auditLog` gespeichert.
- Reservierungsserie: Der normale Reservierungsrequest kann bis zu elf woechentliche Wiederholungen enthalten. Vor dem Speichern werden alle Termine auf Konflikte geprueft.
- Automatische Aktualisierung: Eine geoeffnete Raumansicht prueft alle 30 Sekunden denselben Zeitraum erneut.
- Stammdaten: Abteilungen und Equipment liegen in eigenen Collections. Beim Erstellen einer Abteilung wird automatisch ein Testkonto mit der Rolle `USER` und dem Passwort `holter123` angelegt.
- Loeschschutz: Verwendetes Equipment, Abteilungen mit Raeumen oder Reservierungen sowie Raeume mit Reservierungen werden nicht unkontrolliert geloescht. Das Backend liefert stattdessen eine erklaerende Fehlermeldung.

## Sinnvolle spaetere Commits

1. Datenmodell, Login-Endpunkt und Testkonten
2. Admin-REST-Endpunkte fuer Raeume, Arbeitsplaetze und SVG-Elemente
3. Login-Oberflaeche und rollenbasierte Navigation
4. Room Builder fuer Raeume und Arbeitsplaetze
5. SVG-Editor fuer Waende, Tueren, Pflanzen und Sperrzonen
6. Abteilungen, Equipment und Admin-Reservierungsuebersicht
7. Visuelles HOLTER-Redesign und responsive Grundrisse
8. Dokumentation und Tests
