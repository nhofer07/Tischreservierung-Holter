// Ausführung mit mongosh --nodb; benötigt nur das mitgelieferte MongoDB-Image.
const fs = require('fs');
const crypto = require('crypto');
const email = (process.env.BOOTSTRAP_ADMIN_EMAIL || '').trim().toLowerCase();

if (!/^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/.test(email)) {
  throw new Error(
    'BOOTSTRAP_ADMIN_EMAIL muss die echte Microsoft-E-Mail des ersten Administrators sein.',
  );
}
if (fs.existsSync('.env') || fs.existsSync('secrets')) {
  throw new Error(
    'Konfiguration existiert bereits. Bestehende Zugangsdaten werden nicht überschrieben.',
  );
}

const template = fs.readFileSync('.env.example', 'utf8');
fs.mkdirSync('secrets', { mode: 0o700 });
for (const name of ['root', 'app']) {
  // Verzeichnis 0700 schützt die Dateien auf dem Host. Die Dateien müssen im
  // Container auch für die Benutzer mongodb bzw. 10001 lesbar sein.
  fs.writeFileSync(`secrets/mongo-${name}-password.txt`, crypto.randomBytes(32).toString('hex'), {
    flag: 'wx',
    mode: 0o644,
  });
}
fs.writeFileSync(
  '.env',
  template.replace(/^BOOTSTRAP_ADMIN_EMAIL=.*$/m, `BOOTSTRAP_ADMIN_EMAIL=${email}`),
  {
    flag: 'wx',
    mode: 0o600,
  },
);
print(
  'Konfiguration und zufällige Datenbankpasswörter wurden erstellt. Keine Passwörter ausgeben oder weitergeben.',
);
