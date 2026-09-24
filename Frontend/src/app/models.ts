export interface Equipment {
  name: string;
}

export interface Arbeitsplatz {
  id: string;
  tischnr: string;
  name: string;
  status: 'frei' | 'reserviert';
  abteilungId: string;
  abteilungName: string;
  reservierbar: boolean;
  hinweis: string;
  reserviertVon: string;
  eigeneReservierung: boolean;
  reservierungAnfang: string;
  reservierungEnde: string;
  naechsteReservierungAnfang: string;
  naechsteReservierungEnde: string;
  rotation: number;
  breite: number;
  hoehe: number;
  x: number;
  y: number;
  equipment: Equipment[];
}

export interface Raum {
  id: string;
  name: string;
  stockwerk: string;
  standortId: string;
  abteilungId: string;
  abteilungName: string;
  elemente: RaumElement[];
  arbeitsplaetze: Arbeitsplatz[];
}

export interface RaumElement {
  id: string;
  typ: 'wand' | 'tuer' | 'fenster' | 'pflanze' | 'klima' | 'saeule' | 'feuerloescher' | 'gesperrt' | 'beschriftung';
  x: number;
  y: number;
  breite: number;
  hoehe: number;
  text?: string;
  rotation?: number;
}

export interface RaumAuswahl {
  id: string;
  name: string;
  standortId: string;
  abteilungName: string;
}

export interface Standort {
  id: string;
  name: string;
  adresse: string;
  ort: string;
}

export interface Zeitraum {
  datum: string;
  endDatum: string;
  beginn: string;
  ende: string;
}

export interface Benutzer {
  id: string;
  vorname: string;
  nachname: string;
  email: string;
  rolle: string;
  abteilungId: string;
  abteilungName: string;
  bevorzugterRaumId: string;
  aktiv: boolean;
}

export interface BenutzerRequest {
  vorname: string; nachname: string; email: string; rolle: string; abteilungId: string;
  abteilungName: string; bevorzugterRaumId: string; aktiv: boolean;
}

export interface StandortRequest { name: string; adresse: string; ort: string; }

export interface FirmenEinstellung {
  firmenname: string;
  produktname: string;
  primaerfarbe: string;
  akzentfarbe: string;
  logoUrl: string;
}

export interface FirmenDesignHistorie extends FirmenEinstellung {
  id: string;
  zeitpunkt: string;
}

export interface AuditEintrag { _id: string; adminId: string; aktion: string; details: string; zeitpunkt: string; }

export interface Reservierung {
  id: string;
  benutzerId: string;
  benutzerName: string;
  arbeitsplatzId: string;
  tischnr: string;
  arbeitsplatzName: string;
  raumId: string;
  raumName: string;
  standortId: string;
  standortName: string;
  reservierungAnfang: string;
  reservierungEnde: string;
  status: string;
  stornierungsgrund?: string;
}

export interface RaumRequest {
  name: string;
  stockwerk: string;
  standortId: string;
  abteilungId: string;
  abteilungName: string;
  elemente: RaumElement[];
}

export interface Abteilung {
  id: string;
  name: string;
}

export interface ArbeitsplatzRequest {
  tischnr: string;
  name: string;
  raumId: string;
  standortId: string;
  abteilungId: string;
  abteilungName: string;
  x: number;
  y: number;
  rotation: number;
  breite: number;
  hoehe: number;
  equipment: string[];
}
