// Nicht destruktive Beispielabfragen fuer MongoDB Compass oder mongosh.
// Die Datenbank selbst wird ausschliesslich mit Datenbank/console.js aufgebaut.

use tischreservierung

// Alle Standorte und Raeume anzeigen.
db.standorte.find({}).sort({ ort: 1 })
db.raeume.find({}).sort({ standortId: 1, abteilungName: 1 })

// Arbeitsplaetze einer Abteilung. Diese Abfrage nutzt den Abteilungsindex.
db.arbeitsplaetze.find({ abteilungId: "ABT-IT" }).sort({ raumId: 1, tischnr: 1 })

// Arbeitsplaetze mit hoehenverstellbarem Tisch.
db.arbeitsplaetze.find({ equipment: "Hoehenverstellbarer Tisch" })

// Raeume am Standort Wels mit der Anzahl ihrer Arbeitsplaetze.
db.arbeitsplaetze.aggregate([
    { $match: { standortId: "STANDORT-WELS" } },
    { $group: { _id: "$raumId", anzahlArbeitsplaetze: { $sum: 1 } } },
    { $sort: { _id: 1 } }
])

// Aktive Reservierungen eines Testprofils.
db.reservierungen.find({
    benutzerId: "USER-IT-WELS",
    status: "reserviert"
}).sort({ reservierungAnfang: 1 })

// Vorhandene Indizes kontrollieren.
db.arbeitsplaetze.getIndexes()
