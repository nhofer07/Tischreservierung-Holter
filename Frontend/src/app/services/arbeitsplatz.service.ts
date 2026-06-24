import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Arbeitsplatz, Benutzer, Raum, RaumAuswahl, Standort, Zeitraum } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ArbeitsplatzService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getDemoBenutzer() {
    return this.http.get<Benutzer>(`${this.apiUrl}/benutzer/demo`);
  }

  getStandorte() {
    return this.http.get<Standort[]>(`${this.apiUrl}/standorte`);
  }

  getRaeumeByStandort(standortId: string) {
    return this.http.get<RaumAuswahl[]>(`${this.apiUrl}/raeume/standort/${standortId}`);
  }

  getRaum(raumId: string, zeitraum: Zeitraum) {
    return this.http.get<Raum>(`${this.apiUrl}/raeume/${raumId}`, {
      params: {
        von: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
        bis: this.toIsoDateTime(zeitraum.datum, zeitraum.ende)
      }
    });
  }

  reservieren(benutzerId: string, arbeitsplatzId: string, zeitraum: Zeitraum) {
    return this.http.post<Arbeitsplatz>(`${this.apiUrl}/reservierungen`, {
      benutzerId,
      arbeitsplatzId,
      reservierungAnfang: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
      reservierungEnde: this.toIsoDateTime(zeitraum.datum, zeitraum.ende)
    });
  }

  private toIsoDateTime(datum: string, zeit: string) {
    return new Date(`${datum}T${zeit}:00`).toISOString();
  }
}
