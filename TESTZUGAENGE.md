# Testkonten und Anmeldedaten

## Microsoft Entra ID

Die App bietet **Mit Microsoft anmelden** und auf localhost zusätzlich einen einklappbaren Testlogin an. Folgende Microsoft-Konten sind in den Startdaten bereits einem Mitarbeiterprofil zugeordnet:

| Microsoft-Konto | Mitarbeiterprofil | Rolle | Abteilung |
|---|---|---|---|
| `n.hofer@HTBLALeonding.onmicrosoft.com` | Sophie Admin (`admin@holter.test`) | Admin / Abteilungsleitung | IT |
| `d.bernecker@HTBLALeonding.onmicrosoft.com` | Thomas Holter (`chef@holter.test`) | Superadmin | IT |

Diese Konten verwenden ihr eigenes Microsoft-Passwort und gegebenenfalls MFA. Microsoft-Passwörter liegen nicht im Projekt vor. Die unten genannten lokalen Testpasswörter gelten nicht für die Microsoft-Anmeldung.

Weitere Personen benötigen ein gültiges Microsoft-Konto in der konfigurierten Entra-Anwendung sowie ein aktives Mitarbeiterprofil mit passender `email` oder `entraEmail` in MongoDB. Ein Superadmin kann die Microsoft-E-Mail in der Mitarbeiterverwaltung beim jeweiligen Profil eintragen. Die Angaben beziehen sich auf `Datenbank/console.js`; bestehende Datenbanken können davon abweichen.

## Lokale Test-Anmeldedaten

Diese Konten sind in den Startdaten enthalten. Auf localhost lässt sich **Lokalen Testzugang verwenden** öffnen. In Docker muss dafür ausdrücklich das lokale Demo-Profil gestartet werden (Befehl unten). Im Standardprofil bleibt der Firmenlogin geschützt.

| Name | E-Mail / Profilkennung | Testpasswort | Rolle | Abteilung |
|---|---|---|---|---|
| Anna Leitner | `anna.leitner@example.test` | `holter123` | Mitarbeiter | IT |
| Lukas Berger | `lukas.berger@example.test` | `holter123` | Mitarbeiter | Marketing |
| Miriam Hofer | `miriam.hofer@example.test` | `holter123` | Mitarbeiter | IT |
| David Mayr | `david.mayr@example.test` | `holter123` | Mitarbeiter | Marketing |
| Eva Gruber | `eva.gruber@example.test` | `holter123` | Mitarbeiter | Verwaltung |
| Nico Hofer | `nhofa@holter.com` | `holter123` | Mitarbeiter | Marketing |
| Sophie Admin | `admin@holter.test` | `admin123` | Admin / Abteilungsleitung | IT |
| Thomas Holter | `chef@holter.test` | `admin123` | Superadmin | IT |

Automatisch angelegte Abteilungs-Testprofile und neue Mitarbeiterprofile erhalten für den lokalen Testlogin das Passwort `holter123`. Die entfernte administrative Passwort-Zurücksetzung bleibt entfernt.

### Bestehende lokale Datenbank

Bei der Prüfung am 11.10.2026 verwendet das vorhandene Adminprofil bereits die E-Mail `n.hofer@htblaleonding.onmicrosoft.com`. Der lokale Testlogin funktioniert dort mit dieser E-Mail und `admin123`. Das Profil `nhofa@holter.com` ist derzeit deaktiviert. Diese bestehenden Einstellungen bleiben beim Container-Update erhalten; die Tabelle oben beschreibt die Startdaten einer neuen Datenbank.

## Anwendung starten

Docker Desktop starten, im Projektordner ausführen:

```bash
docker compose up -d
```

Anschließend `http://localhost:3000` öffnen und mit Microsoft anmelden. Jeder Rechner verwendet seine eigene Datenbank; für gemeinsame Reservierungen wird eine zentrale Instanz benötigt.

Für die lokalen Testzugänge:

```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d
```

Danach auf `http://localhost:3000` den lokalen Testzugang öffnen und ein Konto aus der Tabelle verwenden. Dieses Profil ist ausschließlich für lokale Entwicklung und Schulpräsentationen vorgesehen.
