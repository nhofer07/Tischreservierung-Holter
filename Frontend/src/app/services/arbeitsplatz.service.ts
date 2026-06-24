import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Arbeitsplatz, Raum, RaumAuswahl, Standort, Zeitraum } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ArbeitsplatzService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getStandorte(): Observable<Standort[]> {
    return this.http.get<Standort[]>(`${this.apiUrl}/standorte`);
  }

  getRaeumeByStandort(standortId: string): Observable<RaumAuswahl[]> {
    return this.http.get<RaumAuswahl[]>(`${this.apiUrl}/raeume/standort/${standortId}`);
  }

  getRaum(raumId: string, zeitraum: Zeitraum): Observable<Raum> {
    return this.http.get<Raum>(`${this.apiUrl}/raeume/${raumId}`, {
      params: {
        von: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
        bis: this.toIsoDateTime(zeitraum.datum, zeitraum.ende)
      }
    });
  }

  reservieren(arbeitsplatzId: string, zeitraum: Zeitraum): Observable<Arbeitsplatz> {
    return this.http.post<Arbeitsplatz>(`${this.apiUrl}/reservierungen`, {
      arbeitsplatzId,
      reservierungAnfang: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
      reservierungEnde: this.toIsoDateTime(zeitraum.datum, zeitraum.ende)
    });
  }

  private toIsoDateTime(datum: string, zeit: string) {
    return new Date(`${datum}T${zeit}:00`).toISOString();
  }
}
