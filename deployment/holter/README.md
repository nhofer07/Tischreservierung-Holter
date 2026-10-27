# DeskVision – Übergabe an HOLTER

Dieses Paket enthält Frontend, Backend und MongoDB als fertige Docker-Images.
Es benötigt weder Git, Java, Node.js noch ein Docker-Hub-Konto. Ziel ist zunächst
eine lokale Firmenabnahme unter **http://localhost:3000**. Alle drei Dienste laufen
auf einem Rechner mit einer gemeinsamen Datenbank. Für mehrere Mitarbeiter ist
später eine zentrale Installation durch die Firmen-IT vorgesehen.

## Voraussetzungen

- Docker Engine mit Compose auf Linux oder Docker Desktop mit Linux-Containern
  auf Windows/macOS; die Firmen-IT klärt die passende Docker-Lizenz.
- Die Prozessorarchitektur muss zur Angabe in `release.json` passen:
  `amd64` für Intel/AMD, `arm64` für Apple Silicon bzw. ARM-Server.
- Als Startwert 4 CPU-Kerne, 6 GB freier RAM und 5 GB freier Speicher zusätzlich
  zum Übergabearchiv; dies ist kein gemessener Produktionsbedarf.
- Internetzugriff auf Microsoft Entra für Anmeldung und Tokenprüfung, auch wenn
  die Images vollständig aus der Datei geladen werden.
- Microsoft-Konto mit Zugang zur konfigurierten Entra-App. Die ursprünglichen
  öffentlichen Tenant-/Client-IDs stehen in `.env.example`; deren Berechtigung
  und die Redirect-URI **http://localhost:3000** muss die Firmen-IT bestätigen.

Die Initialisierung erzeugt **keine Schul-Testkonten oder Beispielbuchungen**.
Sie legt eine IT-Abteilung, HOLTER Wels, einen leeren Einrichtungsraum und einen
Superadmin an. Weitere Daten werden über die Verwaltung eingerichtet.
Vorhandene Entwicklungsdaten werden nicht automatisch übernommen.

## 1. Entpacken und Images laden

Archiv samt `.sha256`-Datei in einen geschützten lokalen Ordner kopieren.
SHA-256 vor dem Entpacken prüfen: Linux mit `sha256sum -c DATEI.tar.gz.sha256`,
macOS mit `shasum -a 256 -c DATEI.tar.gz.sha256`; in PowerShell
`Get-FileHash .\DATEI.tar.gz -Algorithm SHA256` mit der Begleitdatei vergleichen.
Das Archiv lässt sich mit `tar -xzf DATEI.tar.gz` entpacken.
Danach im entpackten Paketordner:

```text
docker load --input images.tar
```

Es werden keine Images aus einer Registry nachgeladen (`pull_policy: never`).
Bei „image not found“ zuerst das passende `images.tar` laden.

## 2. Einmalig konfigurieren

Die E-Mail unten durch die **tatsächliche Microsoft-E-Mail des ersten Superadmins**
ersetzen. Sie muss dem Claim `email`, `preferred_username` oder `upn` des Kontos
entsprechen. Es wird kein Microsoft-Passwort gespeichert.

**Windows PowerShell**, AMD64-Paket:

```powershell
docker run --rm --network none --mount "type=bind,source=$($PWD.Path),target=/handoff" --workdir /handoff --env BOOTSTRAP_ADMIN_EMAIL=vorname.nachname@firma.at --entrypoint mongosh deskvision/mongodb:7-amd64 --nodb --quiet --file setup.js
```

**Linux/macOS**, AMD64-Paket:

```bash
docker run --rm --network none --user "$(id -u):$(id -g)" --env HOME=/tmp --mount "type=bind,source=$(pwd),target=/handoff" --workdir /handoff --env BOOTSTRAP_ADMIN_EMAIL=vorname.nachname@firma.at --entrypoint mongosh deskvision/mongodb:7-amd64 --nodb --quiet --file setup.js
```

Beim ARM64-Paket in diesem Einrichtungsbefehl `7-arm64` verwenden.
Der Befehl erstellt `.env` und zwei zufällige Passwörter im Ordner `secrets/`.
Er verweigert das Überschreiben bestehender Konfiguration. Den gesamten Ordner
nur für berechtigte Administratoren zugänglich halten, unter Windows über NTFS-Rechte.
Die Secret-Dateien werden von Compose als Dateien eingebunden; das ist kein
verschlüsselter Passwortspeicher. `.env`, `secrets/` und Backups nicht veröffentlichen.

Falls HOLTER eine eigene Entra-App verwendet, vor dem Start `ENTRA_TENANT_ID` und
`ENTRA_CLIENT_ID` in `.env` gemeinsam ändern. Die Images müssen dafür nicht neu
gebaut werden. Eine SPA-Registrierung mit Authorization Code Flow/PKCE und den
Berechtigungen aus dem bestehenden Projekt ist erforderlich. Kein Client-Secret
in der Browserkonfiguration ablegen.

## 3. Start und Abnahme

```text
docker compose up -d --wait --wait-timeout 300
docker compose ps
```

**http://localhost:3000** im Browser öffnen und „Mit Microsoft anmelden“ wählen.
Der konfigurierte Superadmin kann Räume, Arbeitsplätze und Mitarbeiter einrichten.
Der lokale Testlogin ist im Firmenpaket deaktiviert. Das Backend läuft im
Produktionsprofil und prüft die signierte Microsoft-Identität.

Abnahme: Superadmin anmelden, einen Tisch und einen Mitarbeiter anlegen, mit
diesem Mitarbeiter eine Buchung anlegen und stornieren. Danach die Container neu
starten und prüfen, dass die Stammdaten erhalten bleiben. Der Microsoft-Login
muss mit einem echten Firmenkonto durch HOLTER geprüft werden.

Bei Problemen:

```text
docker compose logs --tail 100 backend frontend mongodb
docker compose restart
```

MongoDB und Backend veröffentlichen keine Host-Ports. Die Oberfläche ist zunächst
nur lokal gebunden. Passwortdateien müssen für die Container lesbar sein; ihre
übergeordneten Ordner auf dem Host bleiben geschützt.

## 4. Stoppen und Daten erhalten

```text
docker compose down
docker compose up -d --wait --wait-timeout 300
```

Das benannte Volume bleibt bestehen. **`docker compose down -v` löscht die Daten.**
Die Init-Datei läuft nur mit einem neuen Volume. Eine nachträgliche Änderung von
`BOOTSTRAP_ADMIN_EMAIL` oder Passwortdateien ändert bestehende Datenbankkonten
nicht. Adminzuordnungen in der Verwaltung ändern; Passwortrotation mit der IT planen.

## 5. Backup

Vor Updates und regelmäßig sichern. Die Anwendung wird für eine konsistente
Sicherung kurz gestoppt; MongoDB läuft weiter. Die folgenden Befehle funktionieren
auch in PowerShell, da keine binäre Shell-Umleitung verwendet wird:

```text
docker compose stop frontend backend
docker compose exec -T mongodb sh -ec 'mongodump --host 127.0.0.1 --username "$MONGO_INITDB_ROOT_USERNAME" --password "$(cat /run/secrets/mongo_root_password)" --authenticationDatabase admin --db tischreservierung --archive=/tmp/deskvision-backup.archive.gz --gzip'
docker compose cp mongodb:/tmp/deskvision-backup.archive.gz ./deskvision-backup.archive.gz
docker compose exec -T mongodb rm /tmp/deskvision-backup.archive.gz
docker compose up -d --wait --wait-timeout 300
```

Bei einem Fehler die Anwendung mit dem letzten Befehl wieder starten. Sicherung
mit Datum außerhalb dieses Ordners auf geschütztem Speicher ablegen. Das Archiv
enthält personenbezogene Daten; `.env`, `secrets/` und die verwendete Releaseversion
zusätzlich getrennt und geschützt sichern. Die Passwortdateien sind nicht im Dump.

## 6. Wiederherstellen

**Ersetzt die vorhandenen Anwendungsdaten.** Zuerst eine Sicherung des aktuellen
Stands erstellen. Beim Wechsel auf einen neuen Rechner zunächst mit diesem Paket
und neuen/gesicherten Secrets konfigurieren und `docker compose up -d --wait`
ausführen. Anschließend den gewünschten Dump als `deskvision-backup.archive.gz`
im Paketordner bereitstellen und:

```text
docker compose stop frontend backend
docker compose cp ./deskvision-backup.archive.gz mongodb:/tmp/deskvision-restore.archive.gz
docker compose exec -T mongodb sh -ec 'mongorestore --host 127.0.0.1 --username "$MONGO_INITDB_ROOT_USERNAME" --password "$(cat /run/secrets/mongo_root_password)" --authenticationDatabase admin --archive=/tmp/deskvision-restore.archive.gz --gzip --nsInclude="tischreservierung.*" --drop'
docker compose exec -T mongodb rm /tmp/deskvision-restore.archive.gz
docker compose up -d --wait --wait-timeout 300
```

Die Collections aus dem Dump werden ersetzt. Zusätzliche Collections, die nicht
im Dump enthalten sind, entfernt `--drop` nicht. Das Verfahren setzt denselben
Anwendungsschema-Stand voraus. Datenbank-Benutzer werden nicht aus dem Dump geladen;
die bei der Initialisierung erzeugten Datenbank-Zugangsdaten bleiben gültig.

## 7. Update und Rückweg

1. Aktuellen Stand sichern; bisheriges Paket aufbewahren.
2. Neues Paket separat entpacken und dessen `images.tar` laden.
3. Die neue `compose.yml` und Init-/Setup-Dateien nach Prüfung in den **bisherigen
   Installationsordner** übernehmen; `.env` und `secrets/` behalten.
4. `DESKVISION_VERSION` in der bisherigen `.env` auf die neue Version setzen.
5. `docker compose up -d --wait --wait-timeout 300` ausführen und abnehmen.

Projektname `deskvision-holter` und Volume-Zuordnung unverändert lassen. Nicht
zwei Installationen mit derselben Datenbank gleichzeitig betreiben.
Zum Rückweg die alte Compose-Datei und Versionsnummer verwenden und die alten
Images laden. Falls sich das Datenbankschema geändert hat, ist zusätzlich der
Dump vor dem Update erforderlich; Datenänderungen nach diesem Backup gehen bei
der Wiederherstellung verloren. Schemaänderungen brauchen eigene Migrationshinweise.

## Spätere zentrale Installation

Die Firmen-IT bestimmt Server, internen DNS-Namen und HTTPS-Reverse-Proxy. Auf
dem gleichen Server kann der Proxy `http://127.0.0.1:3000` erreichen. Erst dann
die endgültige HTTPS-Adresse als SPA-Redirect-URI in Entra registrieren.
`DESKVISION_BIND_ADDRESS=127.0.0.1` bleibt für einen Proxy auf demselben Host passend;
bei einem externen Proxy muss die IT Bind-Adresse, Firewall und Zugriffsweg festlegen.
Für eine zentrale Installation das separate Paket bereitstellen und Mitarbeiter
über eine gemeinsame Adresse zugreifen lassen. Private Images können später
auch über eine Firmen-Registry verteilt werden.

Technische Grundlagen: [MongoDB-Image und Initialisierung](https://hub.docker.com/_/mongo),
[Compose-Secrets](https://docs.docker.com/compose/how-tos/use-secrets/).
