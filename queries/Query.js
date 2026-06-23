use tischreservierung

    db.benutzer.drop()
    db.standorte.drop()
    db.raeume.drop()
    db.arbeitsplaetze.drop()
    db.reservierungen.drop()

    db.createCollection("benutzer")
    db.createCollection("standorte")
    db.createCollection("raeume")
    db.createCollection("arbeitsplaetze")
    db.createCollection("reservierungen")

    db.standorte.insertMany([
        {
            _id: "STANDORT-WELS",
            name: "HOLTER Wels",
            adresse: "Sengerstrasse 27",
            plz: "4600",
            ort: "Wels"
            },
        {
            _id: "STANDORT-LINZ",
            name: "HOLTER Linz",
            adresse: "Industriezeile 1",
            plz: "4020",
            ort: "Linz"
            }
        ])

    db.raeume.insertMany([
        {
            _id: "RAUM-WELS-OG1-TEAM",
            standortId: "STANDORT-WELS",
            name: "Teamraum OG 1",
            stockwerk: "1. OG",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            beschreibung: "Statischer Testraum fuer die Vogelperspektive"
            },
        {
            _id: "RAUM-WELS-EG-VERKAUF",
            standortId: "STANDORT-WELS",
            name: "Verkaufsbereich EG",
            stockwerk: "EG",
            abteilungId: "ABT-VERKAUF",
            abteilungName: "Verkauf",
            beschreibung: "Reservierungsbereich fuer Verkauf"
            }
        ])

    db.arbeitsplaetze.insertMany([
        {
            _id: "TISCH-101",
            tischnr: 101,
            raumId: "RAUM-WELS-OG1-TEAM",
            standortId: "STANDORT-WELS",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            name: "Fensterplatz",
            status: "frei",
            position: { x: 18, y: 28 },
            equipment: ["2 Monitore", "Dockingstation", "Tastatur", "Maus"]
            },
        {
            _id: "TISCH-102",
            tischnr: 102,
            raumId: "RAUM-WELS-OG1-TEAM",
            standortId: "STANDORT-WELS",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            name: "Fokusplatz",
            status: "reserviert",
            position: { x: 58, y: 28 },
            equipment: ["Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"]
            },
        {
            _id: "TISCH-103",
            tischnr: 103,
            raumId: "RAUM-WELS-OG1-TEAM",
            standortId: "STANDORT-WELS",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            name: "Projektplatz",
            status: "frei",
            position: { x: 30, y: 68 },
            equipment: ["Whiteboard-Naehe", "2 Monitore", "Rollcontainer"]
            },
        {
            _id: "TISCH-104",
            tischnr: 104,
            raumId: "RAUM-WELS-OG1-TEAM",
            standortId: "STANDORT-WELS",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            name: "Kurzarbeitsplatz",
            status: "belegt",
            position: { x: 70, y: 68 },
            equipment: ["Monitor", "Kopfhoerer", "Netzwerkanschluss"]
            }
        ])

    db.benutzer.insertMany([
        {
            _id: "USER-MAX",
            vorname: "Max",
            nachname: "Mustermann",
            email: "max.mustermann@example.com",
            rolle: "angestellter",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
            },
        {
            _id: "USER-ANNA",
            vorname: "Anna",
            nachname: "Admin",
            email: "anna.admin@example.com",
            rolle: "admin",
            abteilungId: "ABT-IT",
            abteilungName: "IT",
            bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
            }
        ])

    db.reservierungen.insertMany([
        {
            _id: "RES-2026-06-22-102",
            benutzerId: "USER-MAX",
            arbeitsplatzId: "TISCH-102",
            raumId: "RAUM-WELS-OG1-TEAM",
            standortId: "STANDORT-WELS",
            abteilungId: "ABT-IT",
            reservierungAnfang: ISODate("2026-06-22T08:00:00Z"),
            reservierungEnde: ISODate("2026-06-22T16:00:00Z"),
            status: "reserviert"
            }
        ])

    db.benutzer.createIndex({ email: 1 }, { unique: true })
    db.benutzer.createIndex({ abteilungId: 1 })
    db.benutzer.createIndex({ bevorzugterRaumId: 1 })
    db.raeume.createIndex({ standortId: 1 })
    db.raeume.createIndex({ abteilungId: 1 })
    db.arbeitsplaetze.createIndex({ tischnr: 1 }, { unique: true })
    db.arbeitsplaetze.createIndex({ raumId: 1 })
    db.arbeitsplaetze.createIndex({ abteilungId: 1 })
    db.arbeitsplaetze.createIndex({ standortId: 1, raumId: 1, abteilungId: 1 })
    db.reservierungen.createIndex({ benutzerId: 1 })
    db.reservierungen.createIndex({ arbeitsplatzId: 1 })
    db.reservierungen.createIndex({ raumId: 1 })
    db.reservierungen.createIndex({ reservierungAnfang: 1, reservierungEnde: 1 })

    print("Datenbank wurde erfolgreich erstellt.")
