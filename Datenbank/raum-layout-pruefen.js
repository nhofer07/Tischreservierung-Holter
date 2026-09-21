db = db.getSiblingDB("tischreservierung")

// Einfache Qualitätskontrolle für die im Room Builder gespeicherten Räume.
// Türen und Fenster dürfen Wände absichtlich schneiden und werden ausgelassen.
function gedrehteGroesse(breite, hoehe, rotation) {
  const winkel = (rotation || 0) * Math.PI / 180
  return {
    breite: Math.abs(breite * Math.cos(winkel)) + Math.abs(hoehe * Math.sin(winkel)),
    hoehe: Math.abs(breite * Math.sin(winkel)) + Math.abs(hoehe * Math.cos(winkel))
  }
}

function bereichVonTisch(tisch) {
  const groesse = gedrehteGroesse(tisch.breite || 12, tisch.hoehe || 12, tisch.rotation || 0)
  return {
    links: tisch.position.x - groesse.breite / 2,
    rechts: tisch.position.x + groesse.breite / 2,
    oben: tisch.position.y - groesse.hoehe / 2,
    unten: tisch.position.y + groesse.hoehe / 2
  }
}

function bereichVonElement(element) {
  const groesse = gedrehteGroesse(element.breite, element.hoehe, element.rotation || 0)
  const mitteX = element.x + element.breite / 2
  const mitteY = element.y + element.hoehe / 2
  return {
    links: mitteX - groesse.breite / 2,
    rechts: mitteX + groesse.breite / 2,
    oben: mitteY - groesse.hoehe / 2,
    unten: mitteY + groesse.hoehe / 2
  }
}

function ueberlappt(a, b, toleranz = 0.5) {
  return a.rechts - toleranz > b.links && a.links + toleranz < b.rechts &&
    a.unten - toleranz > b.oben && a.oben + toleranz < b.unten
}

const physischeElemente = ["wand", "gesperrt", "saeule", "pflanze", "klima", "feuerloescher"]
const fehler = []

db.raeume.find().forEach(raum => {
  const tische = db.arbeitsplaetze.find({ raumId: raum._id }).toArray()
  for (let i = 0; i < tische.length; i++) {
    for (let j = i + 1; j < tische.length; j++) {
      if (ueberlappt(bereichVonTisch(tische[i]), bereichVonTisch(tische[j]), 1)) {
        fehler.push(`${raum.name}: Tisch ${tische[i].tischnr} überlappt Tisch ${tische[j].tischnr}`)
      }
    }
    ;(raum.elemente || []).filter(element => physischeElemente.includes(element.typ)).forEach(element => {
      if (ueberlappt(bereichVonTisch(tische[i]), bereichVonElement(element), 0.8)) {
        fehler.push(`${raum.name}: Tisch ${tische[i].tischnr} überlappt ${element.typ} ${element.id}`)
      }
    })
  }
})

if (fehler.length === 0) {
  print("Layoutprüfung erfolgreich: keine ungewollten Überlappungen gefunden.")
} else {
  print(`Layoutprüfung: ${fehler.length} Überlappungen gefunden.`)
  fehler.forEach(eintrag => print("- " + eintrag))
}
