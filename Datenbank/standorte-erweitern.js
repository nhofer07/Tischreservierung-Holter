db = db.getSiblingDB("tischreservierung")

// Das Skript kann mehrmals ausgeführt werden. Bestehende, im Room Builder
// bearbeitete Räume werden nicht überschrieben.
const standorte = [
  { _id: "STANDORT-WELS", name: "HOLTER Wels", adresse: "Sengerstraße 27", plz: "4600", ort: "Wels" },
  { _id: "STANDORT-LINZ", name: "HOLTER Linz", adresse: "Pummererstraße 10", plz: "4020", ort: "Linz" },
  { _id: "STANDORT-SALZBURG", name: "HOLTER Salzburg", adresse: "F.-W.-Scherer-Straße 40", plz: "5020", ort: "Salzburg" },
  { _id: "STANDORT-HALL", name: "HOLTER Hall in Tirol", adresse: "Heiligkreuzer Feld 44", plz: "6060", ort: "Hall in Tirol" },
  { _id: "STANDORT-PREMSTAETTEN", name: "HOLTER Premstätten", adresse: "Nordweg 5", plz: "8141", ort: "Premstätten" }
]

standorte.forEach(standort => db.standorte.updateOne(
  { _id: standort._id },
  { $set: standort },
  { upsert: true }
))

// Für jede Abteilung gibt es ein direkt nutzbares Testkonto. $setOnInsert
// schützt Änderungen, die später in der Mitarbeiterverwaltung gemacht werden.
const testBenutzer = [
  {
    _id: "USER-VERWALTUNG-WELS", vorname: "Eva", nachname: "Gruber",
    email: "eva.gruber@example.test", rolle: "USER",
    abteilungId: "ABT-VERWALTUNG", abteilungName: "Verwaltung",
    bevorzugterRaumId: "RAUM-WELS-EG-VERWALTUNG"
  },
  {
    _id: "USER-MKT-NICO", vorname: "Nico", nachname: "Hofer",
    email: "nhofa@holter.com", rolle: "USER",
    abteilungId: "ABT-MARKETING", abteilungName: "Marketing",
    bevorzugterRaumId: "RAUM-WELS-EG-MARKETING"
  }
]

testBenutzer.forEach(benutzer => db.benutzer.updateOne(
  { email: benutzer.email },
  { $setOnInsert: {
    ...benutzer,
    passwortHash: "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0",
    aktiv: true
  } },
  { upsert: true }
))

// Ein bestehendes Testkonto ohne Raum erhält eine sinnvolle Startauswahl.
db.benutzer.updateOne(
  { email: "nhofa@holter.com", $or: [{ bevorzugterRaumId: "" }, { bevorzugterRaumId: null }] },
  { $set: { bevorzugterRaumId: "RAUM-WELS-EG-MARKETING" } }
)

function standardElemente(tuerX, pflanzeX, pflanzeY) {
  return [
    { id: "WAND-OBEN", typ: "wand", x: 0, y: 0, breite: 100, hoehe: 2 },
    { id: "WAND-LINKS", typ: "wand", x: 0, y: 0, breite: 2, hoehe: 100 },
    { id: "WAND-RECHTS", typ: "wand", x: 98, y: 0, breite: 2, hoehe: 100 },
    { id: "WAND-UNTEN-L", typ: "wand", x: 0, y: 98, breite: tuerX, hoehe: 2 },
    { id: "WAND-UNTEN-R", typ: "wand", x: tuerX + 10, y: 98, breite: 90 - tuerX, hoehe: 2 },
    { id: "TUER-1", typ: "tuer", x: tuerX, y: 88, breite: 10, hoehe: 10, rotation: 0 },
    { id: "FENSTER-1", typ: "fenster", x: 35, y: 0, breite: 30, hoehe: 2 },
    { id: "PFLANZE-1", typ: "pflanze", x: pflanzeX, y: pflanzeY, breite: 6, hoehe: 8 },
    { id: "BESCHRIFTUNG-1", typ: "beschriftung", x: 5, y: 6, breite: 22, hoehe: 5, text: "Arbeitsbereich" }
  ]
}

const standardRaeume = [
  { id: "RAUM-WELS-EG-VERWALTUNG", standortId: "STANDORT-WELS", name: "Servicebüro Wels", stockwerk: "EG", abteilungId: "ABT-VERWALTUNG", abteilungName: "Verwaltung", tuerX: 18 },
  { id: "RAUM-LINZ-OG1-VERWALTUNG", standortId: "STANDORT-LINZ", name: "Servicebüro Linz", stockwerk: "1. OG", abteilungId: "ABT-VERWALTUNG", abteilungName: "Verwaltung", tuerX: 66 },
  { id: "RAUM-SALZBURG-EG-IT", standortId: "STANDORT-SALZBURG", name: "IT Workspace Salzburg", stockwerk: "EG", abteilungId: "ABT-IT", abteilungName: "IT", tuerX: 18 },
  { id: "RAUM-SALZBURG-OG1-MARKETING", standortId: "STANDORT-SALZBURG", name: "Marketing Salzburg", stockwerk: "1. OG", abteilungId: "ABT-MARKETING", abteilungName: "Marketing", tuerX: 44 },
  { id: "RAUM-SALZBURG-EG-VERWALTUNG", standortId: "STANDORT-SALZBURG", name: "Verwaltung Salzburg", stockwerk: "EG", abteilungId: "ABT-VERWALTUNG", abteilungName: "Verwaltung", tuerX: 70 },
  { id: "RAUM-HALL-EG-IT", standortId: "STANDORT-HALL", name: "IT Workspace Hall", stockwerk: "EG", abteilungId: "ABT-IT", abteilungName: "IT", tuerX: 24 },
  { id: "RAUM-HALL-OG1-MARKETING", standortId: "STANDORT-HALL", name: "Marketing Hall", stockwerk: "1. OG", abteilungId: "ABT-MARKETING", abteilungName: "Marketing", tuerX: 52 },
  { id: "RAUM-HALL-EG-VERWALTUNG", standortId: "STANDORT-HALL", name: "Verwaltung Hall", stockwerk: "EG", abteilungId: "ABT-VERWALTUNG", abteilungName: "Verwaltung", tuerX: 72 },
  { id: "RAUM-PREMSTAETTEN-EG-IT", standortId: "STANDORT-PREMSTAETTEN", name: "IT Workspace Premstätten", stockwerk: "EG", abteilungId: "ABT-IT", abteilungName: "IT", tuerX: 20 },
  { id: "RAUM-PREMSTAETTEN-OG1-MARKETING", standortId: "STANDORT-PREMSTAETTEN", name: "Marketing Premstätten", stockwerk: "1. OG", abteilungId: "ABT-MARKETING", abteilungName: "Marketing", tuerX: 48 },
  { id: "RAUM-PREMSTAETTEN-EG-VERWALTUNG", standortId: "STANDORT-PREMSTAETTEN", name: "Verwaltung Premstätten", stockwerk: "EG", abteilungId: "ABT-VERWALTUNG", abteilungName: "Verwaltung", tuerX: 72 }
]

standardRaeume.forEach((raum, index) => db.raeume.updateOne(
  { _id: raum.id },
  { $setOnInsert: {
    standortId: raum.standortId,
    name: raum.name,
    stockwerk: raum.stockwerk,
    abteilungId: raum.abteilungId,
    abteilungName: raum.abteilungName,
    elemente: standardElemente(raum.tuerX, index % 2 === 0 ? 88 : 7, index % 3 === 0 ? 8 : 82),
    beschreibung: "Standardraum für die Arbeitsplatzreservierung"
  } },
  { upsert: true }
))

function standardTisch(raum, nummer, x, y) {
  const id = raum.id.replace("RAUM-", "TISCH-") + "-" + nummer
  const standortCode = raum.standortId.replace("STANDORT-", "").slice(0, 3)
  const abteilungCode = raum.abteilungName.slice(0, 3).toUpperCase()
  const ausstattung = nummer % 3 === 0
    ? ["Tastatur", "Maus", "2 Monitore", "Dockingstation"]
    : nummer % 2 === 0
      ? ["Tastatur", "Maus", "Monitor", "Dockingstation"]
      : ["Tastatur", "Maus", "Monitor", "USB-C Hub"]
  return {
    _id: id,
    tischnr: standortCode + "-" + abteilungCode + "-" + nummer,
    name: "Arbeitsplatz " + nummer,
    raumId: raum.id,
    standortId: raum.standortId,
    abteilungId: raum.abteilungId,
    abteilungName: raum.abteilungName,
    status: "frei",
    rotation: 0,
    breite: 12,
    hoehe: 11,
    position: { x: x, y: y },
    equipment: ausstattung
  }
}

const positionen = [[20, 30], [40, 30], [60, 30], [80, 30], [25, 63], [50, 63], [75, 63]]
standardRaeume.forEach(raum => positionen.forEach((position, index) => {
  const tisch = standardTisch(raum, index + 1, position[0], position[1])
  db.arbeitsplaetze.updateOne({ _id: tisch._id }, { $setOnInsert: tisch }, { upsert: true })
}))

print("HOLTER-Standorte und Standardräume wurden ergänzt.")
