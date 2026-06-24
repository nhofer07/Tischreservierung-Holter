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
    name: "Teamraum Wels IT",
    stockwerk: "1. OG",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    beschreibung: "Statischer IT-Raum in Wels fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-WELS-EG-MARKETING",
    standortId: "STANDORT-WELS",
    name: "Marketingraum Wels",
    stockwerk: "EG",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    beschreibung: "Statischer Marketing-Raum in Wels fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    name: "Projektraum Linz IT",
    stockwerk: "EG",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    beschreibung: "Statischer IT-Raum in Linz fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-LINZ-OG2-MARKETING",
    standortId: "STANDORT-LINZ",
    name: "Marketingraum Linz",
    stockwerk: "2. OG",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    beschreibung: "Statischer Marketing-Raum in Linz fuer die Vogelperspektive"
  }
])

function arbeitsplatz(id, tischnr, raumId, standortId, abteilungId, abteilungName, x, y, status, equipment) {
  return {
    _id: id,
    tischnr: tischnr,
    raumId: raumId,
    standortId: standortId,
    abteilungId: abteilungId,
    abteilungName: abteilungName,
    name: abteilungName + " " + tischnr,
    status: status,
    position: { x: x, y: y },
    equipment: ["Tastatur", "Maus"].concat(equipment)
  }
}

db.arbeitsplaetze.insertMany([
  arbeitsplatz("WELS-TISCH-101", 101, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 20, 34, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("WELS-TISCH-102", 102, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 40, 34, "frei", ["Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("WELS-TISCH-103", 103, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 60, 34, "frei", ["2 Monitore", "Rollcontainer"]),
  arbeitsplatz("WELS-TISCH-104", 104, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 80, 34, "frei", ["Monitor"]),
  arbeitsplatz("WELS-TISCH-105", 105, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 20, 58, "frei", ["Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"]),
  arbeitsplatz("WELS-TISCH-106", 106, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 40, 58, "frei", ["2 Monitore", "Dockingstation", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("WELS-TISCH-107", 107, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 60, 58, "frei", ["Monitor", "Kopfhoerer", "USB-C Hub"]),
  arbeitsplatz("WELS-TISCH-108", 108, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 80, 58, "frei", ["Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"]),

  arbeitsplatz("WELS-MKT-301", 301, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 18, 30, "frei", ["2 Monitore", "Grafiktablett", "Dockingstation"]),
  arbeitsplatz("WELS-MKT-302", 302, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 38, 30, "frei", ["Monitor", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("WELS-MKT-303", 303, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 58, 30, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("WELS-MKT-304", 304, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 78, 30, "frei", ["Monitor", "Kopfhoerer"]),
  arbeitsplatz("WELS-MKT-305", 305, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 20, 61, "frei", ["2 Monitore", "Rollcontainer"]),
  arbeitsplatz("WELS-MKT-306", 306, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 40, 61, "frei", ["Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("WELS-MKT-307", 307, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 60, 61, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("WELS-MKT-308", 308, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 80, 61, "frei", ["Monitor", "USB-C Hub"]),

  arbeitsplatz("LINZ-TISCH-201", 201, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 20, 29, "frei", ["Monitor", "Dockingstation"]),
  arbeitsplatz("LINZ-TISCH-202", 202, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 40, 29, "frei", ["2 Monitore"]),
  arbeitsplatz("LINZ-TISCH-203", 203, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 60, 29, "frei", ["Monitor", "Rollcontainer", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("LINZ-TISCH-204", 204, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 80, 29, "frei", ["Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"]),
  arbeitsplatz("LINZ-TISCH-205", 205, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 20, 60, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("LINZ-TISCH-206", 206, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 40, 60, "frei", ["Monitor", "Kopfhoerer"]),
  arbeitsplatz("LINZ-TISCH-207", 207, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 60, 60, "frei", ["Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("LINZ-TISCH-208", 208, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 80, 60, "frei", ["2 Monitore", "Dockingstation"]),

  arbeitsplatz("LINZ-MKT-401", 401, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 20, 30, "frei", ["2 Monitore", "Dockingstation", "Grafiktablett"]),
  arbeitsplatz("LINZ-MKT-402", 402, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 40, 30, "frei", ["Monitor", "Hoehenverstellbarer Tisch"]),
  arbeitsplatz("LINZ-MKT-403", 403, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 60, 30, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("LINZ-MKT-404", 404, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 80, 30, "frei", ["Monitor", "Kopfhoerer"]),
  arbeitsplatz("LINZ-MKT-405", 405, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 20, 61, "frei", ["Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"]),
  arbeitsplatz("LINZ-MKT-406", 406, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 40, 61, "frei", ["2 Monitore", "Rollcontainer"]),
  arbeitsplatz("LINZ-MKT-407", 407, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 60, 61, "frei", ["Monitor", "USB-C Hub"]),
  arbeitsplatz("LINZ-MKT-408", 408, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 80, 61, "frei", ["2 Monitore", "Dockingstation", "Hoehenverstellbarer Tisch"])
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
    _id: "RES-2026-06-24-102",
    benutzerId: "USER-DEMO",
    arbeitsplatzId: "WELS-TISCH-102",
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    reservierungAnfang: ISODate("2026-06-24T06:00:00Z"),
    reservierungEnde: ISODate("2026-06-24T14:00:00Z"),
    status: "reserviert"
  },
  {
    _id: "RES-2026-06-24-106",
    benutzerId: "USER-DEMO",
    arbeitsplatzId: "WELS-TISCH-106",
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    reservierungAnfang: ISODate("2026-06-24T08:00:00Z"),
    reservierungEnde: ISODate("2026-06-24T12:00:00Z"),
    status: "reserviert"
  },
  {
    _id: "RES-2026-06-24-302",
    benutzerId: "USER-DEMO",
    arbeitsplatzId: "WELS-MKT-302",
    raumId: "RAUM-WELS-EG-MARKETING",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-MARKETING",
    reservierungAnfang: ISODate("2026-06-24T06:00:00Z"),
    reservierungEnde: ISODate("2026-06-24T10:00:00Z"),
    status: "reserviert"
  },
  {
    _id: "RES-2026-06-24-203",
    benutzerId: "USER-DEMO",
    arbeitsplatzId: "LINZ-TISCH-203",
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    reservierungAnfang: ISODate("2026-06-24T06:00:00Z"),
    reservierungEnde: ISODate("2026-06-24T14:00:00Z"),
    status: "reserviert"
  },
  {
    _id: "RES-2026-06-24-403",
    benutzerId: "USER-DEMO",
    arbeitsplatzId: "LINZ-MKT-403",
    raumId: "RAUM-LINZ-OG2-MARKETING",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-MARKETING",
    reservierungAnfang: ISODate("2026-06-24T11:00:00Z"),
    reservierungEnde: ISODate("2026-06-24T15:00:00Z"),
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
