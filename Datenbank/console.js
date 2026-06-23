db = db.getSiblingDB("tischreservierung")

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
    name: "Teamraum Wels",
    stockwerk: "1. OG",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    beschreibung: "Statischer Testraum fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    name: "Projektraum Linz",
    stockwerk: "EG",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    beschreibung: "Statischer Linz-Raum fuer die Vogelperspektive"
  }
])

db.arbeitsplaetze.insertMany([
  {
    _id: "WELS-TISCH-101",
    tischnr: 101,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 101",
    status: "frei",
    position: { x: 22, y: 34 },
    equipment: ["2 Monitore", "Dockingstation", "Tastatur", "Maus"]
  },
  {
    _id: "WELS-TISCH-102",
    tischnr: 102,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 102",
    status: "reserviert",
    position: { x: 40, y: 34 },
    equipment: ["Monitor", "Dockingstation"]
  },
  {
    _id: "WELS-TISCH-103",
    tischnr: 103,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 103",
    status: "frei",
    position: { x: 58, y: 34 },
    equipment: ["2 Monitore", "Rollcontainer"]
  },
  {
    _id: "WELS-TISCH-104",
    tischnr: 104,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 104",
    status: "frei",
    position: { x: 76, y: 34 },
    equipment: ["Monitor", "Tastatur", "Maus"]
  },
  {
    _id: "WELS-TISCH-105",
    tischnr: 105,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 105",
    status: "frei",
    position: { x: 22, y: 64 },
    equipment: ["Hoehenverstellbarer Tisch", "Monitor"]
  },
  {
    _id: "WELS-TISCH-106",
    tischnr: 106,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 106",
    status: "reserviert",
    position: { x: 40, y: 64 },
    equipment: ["2 Monitore", "Dockingstation"]
  },
  {
    _id: "WELS-TISCH-107",
    tischnr: 107,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 107",
    status: "frei",
    position: { x: 58, y: 64 },
    equipment: ["Monitor", "Kopfhoerer"]
  },
  {
    _id: "WELS-TISCH-108",
    tischnr: 108,
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Wels 108",
    status: "frei",
    position: { x: 76, y: 64 },
    equipment: ["Monitor", "Dockingstation"]
  },
  {
    _id: "LINZ-TISCH-201",
    tischnr: 201,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 201",
    status: "frei",
    position: { x: 24, y: 27 },
    equipment: ["Monitor", "Dockingstation"]
  },
  {
    _id: "LINZ-TISCH-202",
    tischnr: 202,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 202",
    status: "frei",
    position: { x: 24, y: 48 },
    equipment: ["2 Monitore", "Tastatur", "Maus"]
  },
  {
    _id: "LINZ-TISCH-203",
    tischnr: 203,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 203",
    status: "reserviert",
    position: { x: 24, y: 69 },
    equipment: ["Monitor", "Rollcontainer"]
  },
  {
    _id: "LINZ-TISCH-204",
    tischnr: 204,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 204",
    status: "frei",
    position: { x: 50, y: 27 },
    equipment: ["Hoehenverstellbarer Tisch", "Monitor"]
  },
  {
    _id: "LINZ-TISCH-205",
    tischnr: 205,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 205",
    status: "frei",
    position: { x: 50, y: 69 },
    equipment: ["2 Monitore", "Dockingstation"]
  },
  {
    _id: "LINZ-TISCH-206",
    tischnr: 206,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 206",
    status: "frei",
    position: { x: 76, y: 27 },
    equipment: ["Monitor", "Kopfhoerer"]
  },
  {
    _id: "LINZ-TISCH-207",
    tischnr: 207,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 207",
    status: "reserviert",
    position: { x: 76, y: 48 },
    equipment: ["Monitor", "Dockingstation"]
  },
  {
    _id: "LINZ-TISCH-208",
    tischnr: 208,
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    name: "Linz 208",
    status: "frei",
    position: { x: 76, y: 69 },
    equipment: ["2 Monitore", "Dockingstation"]
  }
])

db.benutzer.insertMany([
  {
    _id: "USER-DEMO",
    vorname: "Demo",
    nachname: "Benutzer",
    email: "demo@example.com",
    rolle: "angestellter",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
  }
])

db.reservierungen.insertMany([
  {
    _id: "RES-2026-06-23-102",
    benutzerId: "USER-DEMO",
    arbeitsplatzId: "WELS-TISCH-102",
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    reservierungAnfang: ISODate("2026-06-23T08:00:00Z"),
    reservierungEnde: ISODate("2026-06-23T16:00:00Z"),
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
