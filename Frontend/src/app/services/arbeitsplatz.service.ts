import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Arbeitsplatz, Benutzer, Raum, Standort } from '../models';

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

  getRaum(raumId: string) {
    return this.http.get<Raum>(`${this.apiUrl}/raeume/${raumId}`);
  }

  reservieren(benutzerId: string, arbeitsplatzId: string) {
    return this.http.post<Arbeitsplatz>(`${this.apiUrl}/reservierungen`, {
      benutzerId,
      arbeitsplatzId
    });
  }
}
