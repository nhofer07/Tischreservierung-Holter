db = db.getSiblingDB("tischreservierung")

db.dropDatabase()

db.createCollection("benutzer")
db.createCollection("standorte")
db.createCollection("raeume")
db.createCollection("arbeitsplaetze")
db.createCollection("reservierungen")
db.createCollection("abteilungen")
db.createCollection("equipment")
db.createCollection("auditLog")

db.abteilungen.insertMany([
  { _id: "ABT-IT", name: "IT" },
  { _id: "ABT-MARKETING", name: "Marketing" },
  { _id: "ABT-VERWALTUNG", name: "Verwaltung" }
])

db.equipment.insertMany([
  { _id: "EQ-TASTATUR", name: "Tastatur" },
  { _id: "EQ-MAUS", name: "Maus" },
  { _id: "EQ-MONITOR", name: "Monitor" },
  { _id: "EQ-2-MONITORE", name: "2 Monitore" },
  { _id: "EQ-DOCK", name: "Dockingstation" },
  { _id: "EQ-HOEHE", name: "Höhenverstellbarer Tisch" },
  { _id: "EQ-HEADSET", name: "Kopfhörer" },
  { _id: "EQ-USB", name: "USB-C Hub" },
  { _id: "EQ-ROLL", name: "Rollcontainer" },
  { _id: "EQ-GRAFIK", name: "Grafiktablett" }
])

function standardElemente(tuerX, pflanzeX, pflanzeY) {
  return [
    { id: "WAND-OBEN", typ: "wand", x: 0, y: 0, breite: 100, hoehe: 2 },
    { id: "WAND-LINKS", typ: "wand", x: 0, y: 0, breite: 2, hoehe: 100 },
    { id: "WAND-RECHTS", typ: "wand", x: 98, y: 0, breite: 2, hoehe: 100 },
    { id: "WAND-UNTEN-L", typ: "wand", x: 0, y: 98, breite: tuerX, hoehe: 2 },
    { id: "WAND-UNTEN-R", typ: "wand", x: tuerX + 12, y: 98, breite: 88 - tuerX, hoehe: 2 },
    { id: "TUER-1", typ: "tuer", x: tuerX, y: 86, breite: 12, hoehe: 12 },
    { id: "PFLANZE-1", typ: "pflanze", x: pflanzeX, y: pflanzeY, breite: 6, hoehe: 8 }
  ]
}

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

db.benutzer.insertMany([
  {
    _id: "USER-IT-WELS",
    vorname: "Anna",
    nachname: "Leitner",
    email: "anna.leitner@example.test",
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    rolle: "USER",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
  },
  {
    _id: "USER-MKT-WELS",
    vorname: "Lukas",
    nachname: "Berger",
    email: "lukas.berger@example.test",
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    rolle: "USER",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    bevorzugterRaumId: "RAUM-WELS-EG-MARKETING"
  },
  {
    _id: "USER-IT-LINZ",
    vorname: "Miriam",
    nachname: "Hofer",
    email: "miriam.hofer@example.test",
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    rolle: "USER",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    bevorzugterRaumId: "RAUM-LINZ-EG-PROJEKT"
  },
  {
    _id: "USER-MKT-LINZ",
    vorname: "David",
    nachname: "Mayr",
    email: "david.mayr@example.test",
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    rolle: "USER",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    bevorzugterRaumId: "RAUM-LINZ-OG2-MARKETING"
  },
  {
    _id: "USER-VERWALTUNG-WELS",
    vorname: "Eva",
    nachname: "Gruber",
    email: "eva.gruber@example.test",
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    rolle: "USER",
    abteilungId: "ABT-VERWALTUNG",
    abteilungName: "Verwaltung",
    bevorzugterRaumId: "RAUM-WELS-EG-VERWALTUNG"
  },
  {
    _id: "USER-MKT-NICO",
    vorname: "Nico",
    nachname: "Hofer",
    email: "nhofa@holter.com",
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    rolle: "USER",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    bevorzugterRaumId: "RAUM-WELS-EG-MARKETING"
  },
  {
    _id: "ADMIN-HOLTER",
    vorname: "Sophie",
    nachname: "Admin",
    email: "admin@holter.test",
    passwortHash: "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
    rolle: "ADMIN",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
  },
  {
    _id: "SUPERADMIN-HOLTER",
    vorname: "Thomas",
    nachname: "Holter",
    email: "chef@holter.test",
    passwortHash: "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
    rolle: "SUPERADMIN",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
  }
])
db.benutzer.updateMany({}, { $set: { aktiv: true } })

db.raeume.insertMany([
  {
    _id: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    name: "Teamraum Wels IT",
    stockwerk: "1. OG",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    elemente: standardElemente(12, 89, 8),
    beschreibung: "Statischer IT-Raum in Wels fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-WELS-EG-MARKETING",
    standortId: "STANDORT-WELS",
    name: "Marketingraum Wels",
    stockwerk: "EG",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    elemente: standardElemente(70, 7, 8).concat([{ id: "SPERRE-1", typ: "gesperrt", x: 43, y: 43, breite: 14, hoehe: 12 }]),
    beschreibung: "Statischer Marketing-Raum in Wels fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    name: "Projektraum Linz IT",
    stockwerk: "EG",
    abteilungId: "ABT-IT",
    abteilungName: "IT",
    elemente: standardElemente(42, 90, 82),
    beschreibung: "Statischer IT-Raum in Linz fuer die Vogelperspektive"
  },
  {
    _id: "RAUM-LINZ-OG2-MARKETING",
    standortId: "STANDORT-LINZ",
    name: "Marketingraum Linz",
    stockwerk: "2. OG",
    abteilungId: "ABT-MARKETING",
    abteilungName: "Marketing",
    elemente: standardElemente(76, 6, 82),
    beschreibung: "Statischer Marketing-Raum in Linz fuer die Vogelperspektive"
  }
])

function arbeitsplatz(id, tischnr, raumId, standortId, abteilungId, abteilungName, x, y, status, equipment) {
  return {
    _id: id,
    tischnr: String(tischnr),
    raumId: raumId,
    standortId: standortId,
    abteilungId: abteilungId,
    abteilungName: abteilungName,
    name: abteilungName + " " + tischnr,
    status: status,
    rotation: 0,
    breite: 12,
    hoehe: 12,
    position: { x: x, y: y },
    equipment: ["Tastatur", "Maus"].concat(equipment)
  }
}

db.arbeitsplaetze.insertMany([
  arbeitsplatz("WELS-TISCH-101", 101, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 20, 34, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("WELS-TISCH-102", 102, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 40, 34, "frei", ["Monitor", "Dockingstation", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("WELS-TISCH-103", 103, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 60, 34, "frei", ["2 Monitore", "Rollcontainer"]),
  arbeitsplatz("WELS-TISCH-104", 104, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 80, 34, "frei", ["Monitor"]),
  arbeitsplatz("WELS-TISCH-105", 105, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 20, 58, "frei", ["Höhenverstellbarer Tisch", "Monitor", "Dockingstation"]),
  arbeitsplatz("WELS-TISCH-106", 106, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 40, 58, "frei", ["2 Monitore", "Dockingstation", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("WELS-TISCH-107", 107, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 60, 58, "frei", ["Monitor", "Kopfhörer", "USB-C Hub"]),
  arbeitsplatz("WELS-TISCH-108", 108, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 80, 58, "frei", ["Monitor", "Dockingstation", "Höhenverstellbarer Tisch"]),

  arbeitsplatz("WELS-MKT-301", 301, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 18, 30, "frei", ["2 Monitore", "Grafiktablett", "Dockingstation"]),
  arbeitsplatz("WELS-MKT-302", 302, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 38, 30, "frei", ["Monitor", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("WELS-MKT-303", 303, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 58, 30, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("WELS-MKT-304", 304, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 78, 30, "frei", ["Monitor", "Kopfhörer"]),
  arbeitsplatz("WELS-MKT-305", 305, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 20, 61, "frei", ["2 Monitore", "Rollcontainer"]),
  arbeitsplatz("WELS-MKT-306", 306, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 40, 61, "frei", ["Monitor", "Dockingstation", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("WELS-MKT-307", 307, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 60, 61, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("WELS-MKT-308", 308, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 80, 61, "frei", ["Monitor", "USB-C Hub"]),

  arbeitsplatz("LINZ-TISCH-201", 201, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 20, 29, "frei", ["Monitor", "Dockingstation"]),
  arbeitsplatz("LINZ-TISCH-202", 202, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 40, 29, "frei", ["2 Monitore"]),
  arbeitsplatz("LINZ-TISCH-203", 203, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 60, 29, "frei", ["Monitor", "Rollcontainer", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("LINZ-TISCH-204", 204, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 80, 29, "frei", ["Höhenverstellbarer Tisch", "Monitor", "Dockingstation"]),
  arbeitsplatz("LINZ-TISCH-205", 205, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 20, 60, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("LINZ-TISCH-206", 206, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 40, 60, "frei", ["Monitor", "Kopfhörer"]),
  arbeitsplatz("LINZ-TISCH-207", 207, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 60, 60, "frei", ["Monitor", "Dockingstation", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("LINZ-TISCH-208", 208, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 80, 60, "frei", ["2 Monitore", "Dockingstation"]),

  arbeitsplatz("LINZ-MKT-401", 401, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 20, 30, "frei", ["2 Monitore", "Dockingstation", "Grafiktablett"]),
  arbeitsplatz("LINZ-MKT-402", 402, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 40, 30, "frei", ["Monitor", "Höhenverstellbarer Tisch"]),
  arbeitsplatz("LINZ-MKT-403", 403, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 60, 30, "frei", ["2 Monitore", "Dockingstation"]),
  arbeitsplatz("LINZ-MKT-404", 404, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 80, 30, "frei", ["Monitor", "Kopfhörer"]),
  arbeitsplatz("LINZ-MKT-405", 405, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 20, 61, "frei", ["Höhenverstellbarer Tisch", "Monitor", "Dockingstation"]),
  arbeitsplatz("LINZ-MKT-406", 406, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 40, 61, "frei", ["2 Monitore", "Rollcontainer"]),
  arbeitsplatz("LINZ-MKT-407", 407, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 60, 61, "frei", ["Monitor", "USB-C Hub"]),
  arbeitsplatz("LINZ-MKT-408", 408, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 80, 61, "frei", ["2 Monitore", "Dockingstation", "Höhenverstellbarer Tisch"])
])

function heuteUm(stunde) {
  const datum = new Date()
  datum.setHours(stunde, 0, 0, 0)
  return datum
}

db.reservierungen.insertMany([
  {
    _id: "RES-DEMO-102",
    benutzerId: "USER-IT-WELS",
    arbeitsplatzId: "WELS-TISCH-102",
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    reservierungAnfang: heuteUm(8),
    reservierungEnde: heuteUm(16),
    status: "reserviert"
  },
  {
    _id: "RES-DEMO-106",
    benutzerId: "USER-IT-LINZ",
    arbeitsplatzId: "WELS-TISCH-106",
    raumId: "RAUM-WELS-OG1-TEAM",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-IT",
    reservierungAnfang: heuteUm(10),
    reservierungEnde: heuteUm(14),
    status: "reserviert"
  },
  {
    _id: "RES-DEMO-302",
    benutzerId: "USER-MKT-WELS",
    arbeitsplatzId: "WELS-MKT-302",
    raumId: "RAUM-WELS-EG-MARKETING",
    standortId: "STANDORT-WELS",
    abteilungId: "ABT-MARKETING",
    reservierungAnfang: heuteUm(8),
    reservierungEnde: heuteUm(12),
    status: "reserviert"
  },
  {
    _id: "RES-DEMO-203",
    benutzerId: "USER-IT-LINZ",
    arbeitsplatzId: "LINZ-TISCH-203",
    raumId: "RAUM-LINZ-EG-PROJEKT",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-IT",
    reservierungAnfang: heuteUm(8),
    reservierungEnde: heuteUm(16),
    status: "reserviert"
  },
  {
    _id: "RES-DEMO-403",
    benutzerId: "USER-MKT-LINZ",
    arbeitsplatzId: "LINZ-MKT-403",
    raumId: "RAUM-LINZ-OG2-MARKETING",
    standortId: "STANDORT-LINZ",
    abteilungId: "ABT-MARKETING",
    reservierungAnfang: heuteUm(13),
    reservierungEnde: heuteUm(17),
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
db.reservierungen.createIndex({ benutzerId: 1, reservierungAnfang: 1 })
db.reservierungen.createIndex({ arbeitsplatzId: 1 })
db.reservierungen.createIndex({ raumId: 1 })
db.reservierungen.createIndex({ reservierungAnfang: 1, reservierungEnde: 1 })
db.abteilungen.createIndex({ name: 1 }, { unique: true })
db.equipment.createIndex({ name: 1 }, { unique: true })
db.auditLog.createIndex({ zeitpunkt: -1 })

load("Datenbank/standorte-erweitern.js")
load("Datenbank/raeume-realistisch-gestalten.js")

print("Datenbank wurde erfolgreich erstellt.")
