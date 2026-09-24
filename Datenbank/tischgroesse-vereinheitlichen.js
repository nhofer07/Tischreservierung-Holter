// Einheitliche Tischgröße wie bei den Standardtischen in Wels IT.
// Positionen, Drehungen und Reservierungen bleiben erhalten.
db = db.getSiblingDB("tischreservierung")
const ergebnis = db.arbeitsplaetze.updateMany(
  { $or: [{ breite: { $ne: 12 } }, { hoehe: { $ne: 11 } }] },
  { $set: { breite: 12, hoehe: 11 } }
)
print(`${ergebnis.modifiedCount} Tische auf die Standardgröße 12 × 11 angepasst.`)
