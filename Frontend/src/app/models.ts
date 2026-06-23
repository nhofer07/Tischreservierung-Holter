export interface Equipment {
  name: string;
}

export interface Arbeitsplatz {
  id: string;
  tischnr: number;
  name: string;
  status: 'frei' | 'reserviert';
  abteilungId: string;
  abteilungName: string;
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
  arbeitsplaetze: Arbeitsplatz[];
}

export interface Standort {
  id: string;
  name: string;
  adresse: string;
  ort: string;
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
}
