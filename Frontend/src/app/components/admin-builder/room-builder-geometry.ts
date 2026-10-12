export interface Flaeche {
  links: number;
  oben: number;
  breite: number;
  hoehe: number;
}

export function begrenzen(wert: number, minimum: number, maximum: number): number {
  return Math.max(minimum, Math.min(maximum, wert));
}

export function gedrehteFlaeche(
  mitteX: number,
  mitteY: number,
  breite: number,
  hoehe: number,
  rotation = 0
): Flaeche {
  const winkel = rotation * Math.PI / 180;
  const gedrehteBreite = Math.abs(breite * Math.cos(winkel)) + Math.abs(hoehe * Math.sin(winkel));
  const gedrehteHoehe = Math.abs(breite * Math.sin(winkel)) + Math.abs(hoehe * Math.cos(winkel));
  return {
    links: mitteX - gedrehteBreite / 2,
    oben: mitteY - gedrehteHoehe / 2,
    breite: gedrehteBreite,
    hoehe: gedrehteHoehe
  };
}

export function ueberlappt(a: Flaeche, b: Flaeche, abstand = 0): boolean {
  return a.links + a.breite + abstand > b.links &&
    a.links - abstand < b.links + b.breite &&
    a.oben + a.hoehe + abstand > b.oben &&
    a.oben - abstand < b.oben + b.hoehe;
}

export function flaechenAbstand(a: Flaeche, b: Flaeche): number {
  const x = Math.max(a.links - (b.links + b.breite), b.links - (a.links + a.breite), 0);
  const y = Math.max(a.oben - (b.oben + b.hoehe), b.oben - (a.oben + a.hoehe), 0);
  return Math.hypot(x, y);
}
