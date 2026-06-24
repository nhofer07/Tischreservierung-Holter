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
  beginn: string;
  ende: string;
}
