// Das offizielle MongoDB-Image führt dieses Skript nur auf einem neuen Volume aus.
// Keine Schul-Testkonten, Beispielreservierungen oder persönlichen Zuordnungen.
const fs = require('fs');
const database = db.getSiblingDB('tischreservierung');
const email = (process.env.BOOTSTRAP_ADMIN_EMAIL || '').trim().toLowerCase();
if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
  throw new Error('Die Microsoft-E-Mail des ersten Superadmins fehlt.');
}
const password = fs.readFileSync('/run/secrets/mongo_app_password', 'utf8').trim();
if (password.length < 32) throw new Error('Das Datenbankpasswort fehlt oder ist zu kurz.');
database.createUser({
  user: 'deskvision-app',
  pwd: password,
  roles: [{ role: 'readWrite', db: 'tischreservierung' }],
});

for (const collection of [
  'benutzer',
  'standorte',
  'raeume',
  'arbeitsplaetze',
  'reservierungen',
  'abteilungen',
  'equipment',
  'einstellungen',
  'auditLog',
  'designHistorie',
]) {
  database.createCollection(collection);
}

database.abteilungen.insertOne({ _id: 'ABT-IT', name: 'IT' });
database.standorte.insertOne({
  _id: 'STANDORT-WELS',
  name: 'HOLTER Wels',
  adresse: 'Sengerstrasse 27',
  ort: 'Wels',
});
database.raeume.insertOne({
  _id: 'RAUM-START',
  name: 'IT – Einrichtung',
  stockwerk: 'EG',
  standortId: 'STANDORT-WELS',
  abteilungId: 'ABT-IT',
  abteilungName: 'IT',
  elemente: [],
});
database.benutzer.insertOne({
  _id: 'SUPERADMIN-HOLTER',
  vorname: 'DeskVision',
  nachname: 'Administration',
  email,
  entraEmail: email,
  rolle: 'SUPERADMIN',
  aktiv: true,
  abteilungId: 'ABT-IT',
  abteilungName: 'IT',
  bevorzugterRaumId: 'RAUM-START',
});
database.einstellungen.insertMany([
  {
    _id: 'FIRMA',
    firmenname: 'HOLTER',
    produktname: 'DeskVision',
    primaerfarbe: '#a51e2d',
    akzentfarbe: '#343638',
    logoUrl: '/holter-logo.png',
  },
  { _id: 'RESERVIERUNG', maxWochen: 12 },
]);

database.benutzer.createIndex({ email: 1 }, { unique: true });
database.benutzer.createIndex({ entraEmail: 1 }, { unique: true, sparse: true });
database.benutzer.createIndex({ abteilungId: 1 });
database.benutzer.createIndex({ bevorzugterRaumId: 1 });
database.raeume.createIndex({ standortId: 1 });
database.raeume.createIndex({ abteilungId: 1 });
database.arbeitsplaetze.createIndex({ tischnr: 1 }, { unique: true });
database.arbeitsplaetze.createIndex({ raumId: 1 });
database.arbeitsplaetze.createIndex({ abteilungId: 1 });
database.arbeitsplaetze.createIndex({ standortId: 1, raumId: 1, abteilungId: 1 });
database.reservierungen.createIndex({ benutzerId: 1, reservierungAnfang: 1 });
database.reservierungen.createIndex({ arbeitsplatzId: 1 });
database.reservierungen.createIndex({ raumId: 1 });
database.reservierungen.createIndex({ reservierungAnfang: 1, reservierungEnde: 1 });
database.abteilungen.createIndex({ name: 1 }, { unique: true });
database.equipment.createIndex({ name: 1 }, { unique: true });
database.auditLog.createIndex({ zeitpunkt: -1 });
database.designHistorie.createIndex({ zeitpunkt: -1 });
print('Firmen-Datenbank mit einem Microsoft-Superadmin und leerem Einrichtungsraum erstellt.');
