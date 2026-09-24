// Ergänzt die aktuelle Login-Struktur, ohne Räume oder Reservierungen zu verändern.
// Das Skript darf bei älteren lokalen Datenbanken mehrmals ausgeführt werden.

const mitarbeiterHash = "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0"
const adminHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9"

db.benutzer.updateMany(
  { rolle: "USER" },
  { $set: { passwortHash: mitarbeiterHash, aktiv: true } }
)

db.benutzer.updateOne(
  { _id: "ADMIN-HOLTER" },
  { $set: {
    vorname: "Sophie", nachname: "Admin", email: "admin@holter.test",
    passwortHash: adminHash, rolle: "ADMIN", aktiv: true,
    abteilungId: "ABT-IT", abteilungName: "IT",
    bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
  } },
  { upsert: true }
)

db.benutzer.updateOne(
  { _id: "SUPERADMIN-HOLTER" },
  { $set: {
    vorname: "Thomas", nachname: "Holter", email: "chef@holter.test",
    passwortHash: adminHash, rolle: "SUPERADMIN", aktiv: true,
    abteilungId: "ABT-IT", abteilungName: "IT",
    bevorzugterRaumId: "RAUM-WELS-OG1-TEAM"
  } },
  { upsert: true }
)

print("Login-Daten wurden ergänzt. Räume und Reservierungen blieben unverändert.")
