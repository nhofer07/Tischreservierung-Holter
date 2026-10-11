db = db.getSiblingDB("tischreservierung")

// Bewusst getrennt vom Grund-Seed: Dieses Skript gestaltet nur die bekannten
// Standardräume neu. Hall in Tirol und selbst angelegte Räume bleiben unberührt.
function createFloorPlanElement(id, typ, x, y, breite, hoehe, rotation = 0, text = undefined) {
  const eintrag = { id, typ, x, y, breite, hoehe, rotation }
  if (text) eintrag.text = text
  return eintrag
}

function createExteriorWalls(tuerX, fensterX = 35, fensterBreite = 28) {
  return [
    createFloorPlanElement("WAND-OBEN-L", "wand", 0, 0, fensterX, 2),
    createFloorPlanElement("WAND-OBEN-R", "wand", fensterX + fensterBreite, 0, 100 - fensterX - fensterBreite, 2),
    createFloorPlanElement("FENSTER-1", "fenster", fensterX, 0, fensterBreite, 2),
    createFloorPlanElement("WAND-LINKS", "wand", 0, 0, 2, 100),
    createFloorPlanElement("WAND-RECHTS", "wand", 98, 0, 2, 100),
    createFloorPlanElement("WAND-UNTEN-L", "wand", 0, 98, tuerX, 2),
    createFloorPlanElement("WAND-UNTEN-R", "wand", tuerX + 10, 98, 90 - tuerX, 2),
    createFloorPlanElement("TUER-1", "tuer", tuerX, 88, 10, 10),
    createFloorPlanElement("FEUER-1", "feuerloescher", 3, 90, 4, 6),
    createFloorPlanElement("KLIMA-1", "klima", 86, 91, 9, 5)
  ]
}

function createItLayout(variante) {
  const tuerX = variante % 2 === 0 ? 14 : 72
  const basis = createExteriorWalls(tuerX, variante % 2 === 0 ? 28 : 48, 26)
  return basis.concat([
    createFloorPlanElement("WAND-TECHNIK", "wand", variante % 2 === 0 ? 70 : 28, 2, 2, 28),
    createFloorPlanElement("TUER-TECHNIK", "tuer", variante % 2 === 0 ? 62 : 20, 20, 8, 8, 270),
    createFloorPlanElement("SPERRE-TECHNIK", "gesperrt", variante % 2 === 0 ? 76 : 5, 7, 17, 15),
    createFloorPlanElement("PFLANZE-1", "pflanze", variante % 2 === 0 ? 88 : 7, 78, 7, 7),
    createFloorPlanElement("LABEL-1", "beschriftung", 5, 6, 22, 5, 0, "IT-Arbeitsbereich"),
    createFloorPlanElement("LABEL-2", "beschriftung", variante % 2 === 0 ? 74 : 4, 27, 20, 5, 0, "Technikzone")
  ])
}

function createMarketingLayout(variante) {
  const tuerX = variante % 2 === 0 ? 68 : 18
  const basis = createExteriorWalls(tuerX, variante % 2 === 0 ? 12 : 58, 25)
  return basis.concat([
    createFloorPlanElement("WAND-MEETING", "wand", variante % 2 === 0 ? 63 : 35, 2, 2, 31),
    createFloorPlanElement("FENSTER-MEETING", "fenster", variante % 2 === 0 ? 72 : 12, 0, 18, 2),
    createFloorPlanElement("PFLANZE-1", "pflanze", variante % 2 === 0 ? 88 : 6, 10, 8, 8),
    createFloorPlanElement("PFLANZE-2", "pflanze", variante % 2 === 0 ? 7 : 87, 80, 6, 6),
    createFloorPlanElement("SPERRE-MATERIAL", "gesperrt", variante % 2 === 0 ? 75 : 6, 37, 17, 10),
    createFloorPlanElement("LABEL-1", "beschriftung", 5, 6, 23, 5, 0, "Marketing Team"),
    createFloorPlanElement("LABEL-2", "beschriftung", variante % 2 === 0 ? 69 : 5, 32, 22, 5, 0, "Material & Druck")
  ])
}

function createAdministrationLayout(variante) {
  const tuerX = variante % 2 === 0 ? 42 : 72
  const basis = createExteriorWalls(tuerX, variante % 2 === 0 ? 22 : 52, 24)
  return basis.concat([
    createFloorPlanElement("WAND-RUHE", "wand", variante % 2 === 0 ? 67 : 31, 2, 2, 35),
    createFloorPlanElement("TUER-RUHE", "tuer", variante % 2 === 0 ? 59 : 23, 27, 8, 8, 270),
    createFloorPlanElement("PFLANZE-1", "pflanze", variante % 2 === 0 ? 88 : 6, 78, 7, 7),
    createFloorPlanElement("SAEULE-1", "saeule", variante % 2 === 0 ? 48 : 52, 56, 5, 5),
    createFloorPlanElement("SPERRE-ABLAGE", "gesperrt", variante % 2 === 0 ? 76 : 7, 8, 16, 9),
    createFloorPlanElement("LABEL-1", "beschriftung", 5, 6, 24, 5, 0, "Verwaltung"),
    createFloorPlanElement("LABEL-2", "beschriftung", variante % 2 === 0 ? 72 : 5, 21, 22, 5, 0, "Ruhiger Bereich")
  ])
}

const raumGestaltungen = [
  { id: "RAUM-WELS-OG1-TEAM", elemente: createItLayout(1), anordnung: "inseln" },
  { id: "RAUM-WELS-EG-MARKETING", elemente: createMarketingLayout(1), anordnung: "team" },
  { id: "RAUM-WELS-EG-VERWALTUNG", elemente: createAdministrationLayout(1), anordnung: "reihen" },
  { id: "RAUM-LINZ-EG-PROJEKT", elemente: createItLayout(2), anordnung: "projekt" },
  { id: "RAUM-LINZ-OG2-MARKETING", elemente: createMarketingLayout(2), anordnung: "team" },
  { id: "RAUM-LINZ-OG1-VERWALTUNG", elemente: createAdministrationLayout(2), anordnung: "reihen" },
  { id: "RAUM-SALZBURG-EG-IT", elemente: createItLayout(3), anordnung: "inseln" },
  { id: "RAUM-SALZBURG-OG1-MARKETING", elemente: createMarketingLayout(3), anordnung: "team" },
  { id: "RAUM-SALZBURG-EG-VERWALTUNG", elemente: createAdministrationLayout(3), anordnung: "reihen" },
  { id: "RAUM-PREMSTAETTEN-EG-IT", elemente: createItLayout(4), anordnung: "projekt" },
  { id: "RAUM-PREMSTAETTEN-OG1-MARKETING", elemente: createMarketingLayout(4), anordnung: "team" },
  { id: "RAUM-PREMSTAETTEN-EG-VERWALTUNG", elemente: createAdministrationLayout(4), anordnung: "reihen" }
]

const anordnungen = {
  inseln: [
    [15, 42, 0], [33, 42, 0], [15, 68, 180], [33, 68, 180],
    [55, 42, 0], [73, 42, 0], [55, 68, 180], [73, 68, 180],
    [88, 43, 90], [88, 70, 90], [44, 84, 0]
  ],
  team: [
    [15, 56, 90], [15, 74, 90], [36, 42, 0], [54, 42, 0],
    [36, 69, 180], [54, 69, 180], [76, 56, 270], [76, 74, 270],
    [90, 44, 90], [90, 70, 90], [45, 84, 0]
  ],
  reihen: [
    [14, 45, 0], [33, 45, 0], [52, 45, 0], [71, 45, 0],
    [19, 69, 180], [38, 69, 180], [57, 69, 180], [76, 69, 180],
    [89, 43, 90], [89, 70, 90], [47, 84, 0]
  ],
  projekt: [
    [15, 42, 90], [15, 69, 90], [36, 42, 0], [54, 42, 0],
    [36, 69, 180], [54, 69, 180], [76, 42, 270], [76, 69, 270],
    [90, 43, 90], [90, 70, 90], [45, 84, 0]
  ]
}

raumGestaltungen.forEach(gestaltung => {
  const raum = db.raeume.findOne({ _id: gestaltung.id })
  if (!raum) return
  db.raeume.updateOne(
    { _id: gestaltung.id },
    { $set: { elemente: gestaltung.elemente, beschreibung: "Realistisch gestalteter HOLTER-Büroraum" } }
  )

  const tische = db.arbeitsplaetze.find({ raumId: gestaltung.id }).toArray().sort((a, b) => {
    const nummerA = parseInt(String(a.tischnr).replace(/\D/g, "")) || 0
    const nummerB = parseInt(String(b.tischnr).replace(/\D/g, "")) || 0
    return nummerA - nummerB
  })
  const positionen = anordnungen[gestaltung.anordnung]
  tische.forEach((tisch, index) => {
    const position = positionen[index % positionen.length]
    db.arbeitsplaetze.updateOne(
      { _id: tisch._id },
      { $set: {
        position: { x: position[0], y: position[1] },
        rotation: position[2],
        breite: 12,
        hoehe: 11
      } }
    )
  })
})

print("12 Standardräume außerhalb von Hall in Tirol wurden realistisch gestaltet.")
