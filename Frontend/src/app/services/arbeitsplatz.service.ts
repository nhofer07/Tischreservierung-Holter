import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Abteilung, Arbeitsplatz, ArbeitsplatzRequest, AuditEintrag, Benutzer, BenutzerRequest, Raum, RaumAuswahl, RaumRequest, Reservierung, Standort, StandortRequest, Zeitraum } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ArbeitsplatzService {
  private apiUrl = 'http://127.0.0.1:8080/api';

  constructor(private http: HttpClient) {}

  getBenutzer(): Observable<Benutzer[]> {
    return this.http.get<Benutzer[]>(`${this.apiUrl}/benutzer`);
  }

  login(email: string, passwort: string): Observable<Benutzer> {
    return this.http.post<Benutzer>(`${this.apiUrl}/benutzer/login`, { email, passwort });
  }

  getStandorte(): Observable<Standort[]> {
    return this.http.get<Standort[]>(`${this.apiUrl}/standorte`);
  }

  getRaeumeByStandort(standortId: string): Observable<RaumAuswahl[]> {
    return this.http.get<RaumAuswahl[]>(`${this.apiUrl}/raeume/standort/${standortId}`);
  }

  getRaum(raumId: string, zeitraum: Zeitraum, benutzerId: string): Observable<Raum> {
    return this.http.get<Raum>(`${this.apiUrl}/raeume/${raumId}`, {
      params: {
        von: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
        bis: this.toIsoDateTime(zeitraum.datum, zeitraum.ende),
        benutzerId
      }
    });
  }

  reservieren(benutzerId: string, arbeitsplatzId: string, zeitraum: Zeitraum, wiederholungen = 0): Observable<Reservierung> {
    return this.http.post<Reservierung>(`${this.apiUrl}/reservierungen`, {
      benutzerId,
      arbeitsplatzId,
      reservierungAnfang: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
      reservierungEnde: this.toIsoDateTime(zeitraum.datum, zeitraum.ende),
      wiederholungen
    });
  }

  getReservierungen(benutzerId: string): Observable<Reservierung[]> {
    return this.http.get<Reservierung[]>(`${this.apiUrl}/reservierungen/benutzer/${benutzerId}`);
  }

  stornieren(reservierungId: string, benutzerId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/reservierungen/${reservierungId}`, {
      params: { benutzerId }
    });
  }

  reservierungAendern(reservierung: Reservierung, benutzerId: string, anfang: string, ende: string): Observable<Reservierung> {
    return this.http.put<Reservierung>(`${this.apiUrl}/reservierungen/${reservierung.id}`, {
      benutzerId, arbeitsplatzId: reservierung.arbeitsplatzId,
      reservierungAnfang: new Date(anfang).toISOString(), reservierungEnde: new Date(ende).toISOString(), wiederholungen: 0
    });
  }

  raumAnlegen(adminId: string, raum: RaumRequest): Observable<RaumAuswahl> {
    return this.http.post<RaumAuswahl>(`${this.apiUrl}/admin/raeume`, raum, { params: { adminId } });
  }

  raumAendern(adminId: string, raumId: string, raum: RaumRequest): Observable<RaumAuswahl> {
    return this.http.put<RaumAuswahl>(`${this.apiUrl}/admin/raeume/${raumId}`, raum, { params: { adminId } });
  }

  raumLoeschen(adminId: string, raumId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/raeume/${raumId}`, { params: { adminId } });
  }

  arbeitsplatzAnlegen(adminId: string, arbeitsplatz: ArbeitsplatzRequest): Observable<Arbeitsplatz> {
    return this.http.post<Arbeitsplatz>(`${this.apiUrl}/admin/arbeitsplaetze`, arbeitsplatz, { params: { adminId } });
  }

  arbeitsplatzAendern(adminId: string, arbeitsplatzId: string, arbeitsplatz: ArbeitsplatzRequest): Observable<Arbeitsplatz> {
    return this.http.put<Arbeitsplatz>(`${this.apiUrl}/admin/arbeitsplaetze/${arbeitsplatzId}`, arbeitsplatz, { params: { adminId } });
  }

  arbeitsplatzLoeschen(adminId: string, arbeitsplatzId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/arbeitsplaetze/${arbeitsplatzId}`, { params: { adminId } });
  }

  getAbteilungen(adminId: string): Observable<Abteilung[]> {
    return this.http.get<Abteilung[]>(`${this.apiUrl}/admin/abteilungen`, { params: { adminId } });
  }

  abteilungAnlegen(adminId: string, name: string): Observable<Abteilung> {
    return this.http.post<Abteilung>(`${this.apiUrl}/admin/abteilungen`, { name }, { params: { adminId } });
  }

  abteilungLoeschen(adminId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/abteilungen/${id}`, { params: { adminId } });
  }

  getEquipmentVerwaltung(adminId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/admin/equipment`, { params: { adminId } });
  }

  equipmentAnlegen(adminId: string, name: string): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/admin/equipment`, { name }, { params: { adminId } });
  }

  equipmentLoeschen(adminId: string, name: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/equipment/${encodeURIComponent(name)}`, { params: { adminId } });
  }

  getAlleReservierungen(adminId: string): Observable<Reservierung[]> {
    return this.http.get<Reservierung[]>(`${this.apiUrl}/admin/reservierungen`, { params: { adminId } });
  }

  reservierungAlsAdminStornieren(adminId: string, reservierungId: string, grund: string): Observable<void> {
    return this.http.request<void>('DELETE', `${this.apiUrl}/admin/reservierungen/${reservierungId}`, { body: { name: grund }, params: { adminId } });
  }

  getAuditLog(adminId: string): Observable<AuditEintrag[]> {
    return this.http.get<AuditEintrag[]>(`${this.apiUrl}/admin/audit`, { params: { adminId } });
  }

  getBenutzerVerwaltung(adminId: string): Observable<Benutzer[]> {
    return this.http.get<Benutzer[]>(`${this.apiUrl}/admin/benutzer`, { params: { adminId } });
  }

  benutzerAnlegen(adminId: string, request: BenutzerRequest): Observable<Benutzer> {
    return this.http.post<Benutzer>(`${this.apiUrl}/admin/benutzer`, request, { params: { adminId } });
  }

  benutzerAendern(adminId: string, id: string, request: BenutzerRequest): Observable<Benutzer> {
    return this.http.put<Benutzer>(`${this.apiUrl}/admin/benutzer/${id}`, request, { params: { adminId } });
  }

  passwortZuruecksetzen(adminId: string, id: string, passwort: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/admin/benutzer/${id}/passwort`, { passwort }, { params: { adminId } });
  }

  standortAnlegen(adminId: string, request: StandortRequest): Observable<Standort> {
    return this.http.post<Standort>(`${this.apiUrl}/admin/standorte`, request, { params: { adminId } });
  }

  standortAendern(adminId: string, id: string, request: StandortRequest): Observable<Standort> {
    return this.http.put<Standort>(`${this.apiUrl}/admin/standorte/${id}`, request, { params: { adminId } });
  }

  standortLoeschen(adminId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/standorte/${id}`, { params: { adminId } });
  }

  private toIsoDateTime(datum: string, zeit: string) {
    return new Date(`${datum}T${zeit}:00`).toISOString();
  }
}
