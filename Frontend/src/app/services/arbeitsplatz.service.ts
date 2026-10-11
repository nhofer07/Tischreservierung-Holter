import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Abteilung, Arbeitsplatz, ArbeitsplatzRequest, AuditEintrag, Benutzer, BenutzerRequest, FirmenDesignHistorie, FirmenEinstellung, Raum, RaumAuswahl, RaumRequest, Reservierung, Standort, StandortRequest, Zeitraum } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ArbeitsplatzService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<Benutzer[]> {
    return this.http.get<Benutzer[]>(`${this.apiUrl}/benutzer`);
  }

  loginWithTestAccount(email: string, passwort: string): Observable<Benutzer> {
    return this.http.post<Benutzer>(`${this.apiUrl}/benutzer/login`, { email, passwort });
  }

  getCurrentEntraUser(): Observable<Benutzer> {
    return this.http.get<Benutzer>(`${this.apiUrl}/benutzer/me`);
  }

  getLocations(): Observable<Standort[]> {
    return this.http.get<Standort[]>(`${this.apiUrl}/standorte`);
  }

  getBookingSettings(): Observable<{ maxWochen: number; buchbarBis: string }> {
    return this.http.get<{ maxWochen: number; buchbarBis: string }>(`${this.apiUrl}/einstellungen/reservierung`);
  }

  saveBookingSettings(adminId: string, maxWochen: number): Observable<{ maxWochen: number; buchbarBis: string }> {
    return this.http.put<{ maxWochen: number; buchbarBis: string }>(`${this.apiUrl}/admin/reservierungseinstellungen`, { maxWochen }, { params: { adminId } });
  }

  getCompanySettings(): Observable<FirmenEinstellung> {
    return this.http.get<FirmenEinstellung>(`${this.apiUrl}/einstellungen`);
  }

  saveCompanySettings(adminId: string, einstellung: FirmenEinstellung): Observable<FirmenEinstellung> {
    return this.http.put<FirmenEinstellung>(`${this.apiUrl}/admin/einstellungen`, einstellung, { params: { adminId } });
  }

  getBrandingHistory(adminId: string): Observable<FirmenDesignHistorie[]> {
    return this.http.get<FirmenDesignHistorie[]>(`${this.apiUrl}/admin/einstellungen/historie`, { params: { adminId } });
  }

  applyBrandingHistoryEntry(adminId: string, historieId: string): Observable<FirmenEinstellung> {
    return this.http.put<FirmenEinstellung>(`${this.apiUrl}/admin/einstellungen/historie/${historieId}`, {}, { params: { adminId } });
  }

  deleteBrandingHistoryEntry(adminId: string, historieId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/einstellungen/historie/${historieId}`, { params: { adminId } });
  }

  getRoomsByLocation(standortId: string): Observable<RaumAuswahl[]> {
    return this.http.get<RaumAuswahl[]>(`${this.apiUrl}/raeume/standort/${standortId}`);
  }

  getRoom(raumId: string, zeitraum: Zeitraum, benutzerId: string): Observable<Raum> {
    return this.http.get<Raum>(`${this.apiUrl}/raeume/${raumId}`, {
      params: {
        von: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
        bis: this.toIsoDateTime(zeitraum.endDatum, zeitraum.ende),
        benutzerId
      }
    });
  }

  createReservation(benutzerId: string, arbeitsplatzId: string, zeitraum: Zeitraum): Observable<Reservierung> {
    return this.http.post<Reservierung>(`${this.apiUrl}/reservierungen`, {
      benutzerId,
      arbeitsplatzId,
      reservierungAnfang: this.toIsoDateTime(zeitraum.datum, zeitraum.beginn),
      reservierungEnde: this.toIsoDateTime(zeitraum.endDatum, zeitraum.ende),
      wiederholungen: 0
    });
  }

  getReservations(benutzerId: string): Observable<Reservierung[]> {
    return this.http.get<Reservierung[]>(`${this.apiUrl}/reservierungen/benutzer/${benutzerId}`);
  }

  cancelReservation(reservierungId: string, benutzerId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/reservierungen/${reservierungId}`, {
      params: { benutzerId }
    });
  }

  updateReservation(reservierung: Reservierung, benutzerId: string, anfang: string, ende: string): Observable<Reservierung> {
    return this.http.put<Reservierung>(`${this.apiUrl}/reservierungen/${reservierung.id}`, {
      benutzerId, arbeitsplatzId: reservierung.arbeitsplatzId,
      reservierungAnfang: new Date(anfang).toISOString(), reservierungEnde: new Date(ende).toISOString(), wiederholungen: 0
    });
  }

  createRoom(adminId: string, raum: RaumRequest): Observable<RaumAuswahl> {
    return this.http.post<RaumAuswahl>(`${this.apiUrl}/admin/raeume`, raum, { params: { adminId } });
  }

  updateRoom(adminId: string, raumId: string, raum: RaumRequest): Observable<RaumAuswahl> {
    return this.http.put<RaumAuswahl>(`${this.apiUrl}/admin/raeume/${raumId}`, raum, { params: { adminId } });
  }

  deleteRoom(adminId: string, raumId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/raeume/${raumId}`, { params: { adminId } });
  }

  createWorkstation(adminId: string, arbeitsplatz: ArbeitsplatzRequest): Observable<Arbeitsplatz> {
    return this.http.post<Arbeitsplatz>(`${this.apiUrl}/admin/arbeitsplaetze`, arbeitsplatz, { params: { adminId } });
  }

  updateWorkstation(adminId: string, arbeitsplatzId: string, arbeitsplatz: ArbeitsplatzRequest): Observable<Arbeitsplatz> {
    return this.http.put<Arbeitsplatz>(`${this.apiUrl}/admin/arbeitsplaetze/${arbeitsplatzId}`, arbeitsplatz, { params: { adminId } });
  }

  deleteWorkstation(adminId: string, arbeitsplatzId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/arbeitsplaetze/${arbeitsplatzId}`, { params: { adminId } });
  }

  getDepartments(adminId: string): Observable<Abteilung[]> {
    return this.http.get<Abteilung[]>(`${this.apiUrl}/admin/abteilungen`, { params: { adminId } });
  }

  createDepartment(adminId: string, name: string): Observable<Abteilung> {
    return this.http.post<Abteilung>(`${this.apiUrl}/admin/abteilungen`, { name }, { params: { adminId } });
  }

  deleteDepartment(adminId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/abteilungen/${id}`, { params: { adminId } });
  }

  getEquipmentCatalog(adminId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/admin/equipment`, { params: { adminId } });
  }

  createEquipment(adminId: string, name: string): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/admin/equipment`, { name }, { params: { adminId } });
  }

  deleteEquipment(adminId: string, name: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/equipment/${encodeURIComponent(name)}`, { params: { adminId } });
  }

  getAllReservations(adminId: string): Observable<Reservierung[]> {
    return this.http.get<Reservierung[]>(`${this.apiUrl}/admin/reservierungen`, { params: { adminId } });
  }

  cancelReservationAsAdmin(adminId: string, reservierungId: string, grund: string): Observable<void> {
    return this.http.request<void>('DELETE', `${this.apiUrl}/admin/reservierungen/${reservierungId}`, { body: { name: grund }, params: { adminId } });
  }

  getAuditLog(adminId: string): Observable<AuditEintrag[]> {
    return this.http.get<AuditEintrag[]>(`${this.apiUrl}/admin/audit`, { params: { adminId } });
  }

  getManagedUsers(adminId: string): Observable<Benutzer[]> {
    return this.http.get<Benutzer[]>(`${this.apiUrl}/admin/benutzer`, { params: { adminId } });
  }

  createUser(adminId: string, request: BenutzerRequest): Observable<Benutzer> {
    return this.http.post<Benutzer>(`${this.apiUrl}/admin/benutzer`, request, { params: { adminId } });
  }

  updateUser(adminId: string, id: string, request: BenutzerRequest): Observable<Benutzer> {
    return this.http.put<Benutzer>(`${this.apiUrl}/admin/benutzer/${id}`, request, { params: { adminId } });
  }



  deleteUser(adminId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/benutzer/${id}`, { params: { adminId } });
  }

  createLocation(adminId: string, request: StandortRequest): Observable<Standort> {
    return this.http.post<Standort>(`${this.apiUrl}/admin/standorte`, request, { params: { adminId } });
  }

  updateLocation(adminId: string, id: string, request: StandortRequest): Observable<Standort> {
    return this.http.put<Standort>(`${this.apiUrl}/admin/standorte/${id}`, request, { params: { adminId } });
  }

  deleteLocation(adminId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/standorte/${id}`, { params: { adminId } });
  }

  private toIsoDateTime(datum: string, zeit: string) {
    return new Date(`${datum}T${zeit}:00`).toISOString();
  }
}
