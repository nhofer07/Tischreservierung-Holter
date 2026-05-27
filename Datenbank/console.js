use tischreservierung

db.createCollection("benutzer")
db.createCollection("abteilungen")
db.createCollection("arbeitsplaetze")
db.createCollection("ausstattungen")
db.createCollection("arbeitsplatz_ausstattung")
db.createCollection("reservierungen")

// Abteilungen
db.abteilungen.insertMany([
  { name: "IT" },
  { name: "Softwareentwicklung" },
  { name: "Logistik" },
  { name: "Verkauf" }
])

// Ausstattungen
db.ausstattungen.insertMany([
  { bezeichnung: "Monitor" },
  { bezeichnung: "Höhenverstellbarer Tisch" },
  { bezeichnung: "Maus" },
  { bezeichnung: "Tastatur" },
  { bezeichnung: "Rollcontainer" },
  { bezeichnung: "Kopfhörer" },
  { bezeichnung: "Dockingstation" }
])

// IDs holen
const it = db.abteilungen.findOne({ name: "IT" })
const softwareentwicklung = db.abteilungen.findOne({ name: "Softwareentwicklung" })
const logistik = db.abteilungen.findOne({ name: "Logistik" })
const verkauf = db.abteilungen.findOne({ name: "Verkauf" })

const monitor = db.ausstattungen.findOne({ bezeichnung: "Monitor" })
const tischHoehenverstellbar = db.ausstattungen.findOne({ bezeichnung: "Höhenverstellbarer Tisch" })
const maus = db.ausstattungen.findOne({ bezeichnung: "Maus" })
const tastatur = db.ausstattungen.findOne({ bezeichnung: "Tastatur" })
const rollcontainer = db.ausstattungen.findOne({ bezeichnung: "Rollcontainer" })
const kopfhoerer = db.ausstattungen.findOne({ bezeichnung: "Kopfhörer" })
const dockingstation = db.ausstattungen.findOne({ bezeichnung: "Dockingstation" })

// Arbeitsplätze
db.arbeitsplaetze.insertMany([
  {
    tischnr: 101,
    abteilungId: it._id
  },
  {
    tischnr: 102,
    abteilungId: softwareentwicklung._id
  },
  {
    tischnr: 201,
    abteilungId: logistik._id
  },
  {
    tischnr: 301,
    abteilungId: verkauf._id
  }
])

// Ausstattung zu Arbeitsplätzen
db.arbeitsplatz_ausstattung.insertMany([
  {
    tischnr: 101,
    ausstattungId: monitor._id,
    anzahl: 2
  },
  {
    tischnr: 101,
    ausstattungId: maus._id,
    anzahl: 1
  },
  {
    tischnr: 101,
    ausstattungId: tastatur._id,
    anzahl: 1
  },
  {
    tischnr: 101,
    ausstattungId: dockingstation._id,
    anzahl: 1
  },
  {
    tischnr: 102,
    ausstattungId: monitor._id,
    anzahl: 2
  },
  {
    tischnr: 102,
    ausstattungId: tischHoehenverstellbar._id,
    anzahl: 1
  },
  {
    tischnr: 102,
    ausstattungId: dockingstation._id,
    anzahl: 1
  },
  {
    tischnr: 201,
    ausstattungId: rollcontainer._id,
    anzahl: 1
  },
  {
    tischnr: 201,
    ausstattungId: maus._id,
    anzahl: 1
  },
  {
    tischnr: 301,
    ausstattungId: monitor._id,
    anzahl: 1
  },
  {
    tischnr: 301,
    ausstattungId: kopfhoerer._id,
    anzahl: 1
  }
])

// Benutzer
db.benutzer.insertMany([
  {
    name: "Max",
    nachname: "Mustermann",
    abteilungId: it._id,
    email: "max.mustermann@example.com",
    telefonnummer: "+436641234567",
    adresse: "Musterstraße 1",
    plz: "4020",
    geburtstag: ISODate("2006-04-12"),
    rolle: "angestellter"
  },
  {
    name: "Anna",
    nachname: "Admin",
    abteilungId: softwareentwicklung._id,
    email: "anna.admin@example.com",
    telefonnummer: "+436641111111",
    adresse: "Beispielstraße 5",
    plz: "4040",
    geburtstag: ISODate("2005-09-20"),
    rolle: "admin"
  }
])

const max = db.benutzer.findOne({ email: "max.mustermann@example.com" })

// Reservierungen
db.reservierungen.insertMany([
  {
    benutzerId: max._id,
    tischnr: 101,
    reservierungAnfang: ISODate("2026-05-27T08:00:00Z"),
    reservierungEnde: ISODate("2026-05-27T16:00:00Z"),
    reservierungsbuchung: new Date(),
    status: "aktiv"
  }
])

// Sinnvolle Indexe
db.benutzer.createIndex({ email: 1 }, { unique: true })
db.arbeitsplaetze.createIndex({ tischnr: 1 }, { unique: true })
db.arbeitsplatz_ausstattung.createIndex({ tischnr: 1 })
db.arbeitsplatz_ausstattung.createIndex({ ausstattungId: 1 })
db.reservierungen.createIndex({ benutzerId: 1 })
db.reservierungen.createIndex({ tischnr: 1 })
db.reservierungen.createIndex({ reservierungAnfang: 1, reservierungEnde: 1 })

print("Datenbank wurde erfolgreich erstellt.")


