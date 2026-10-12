import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { IntroComponent } from './components/intro/intro.component';
import { ReservationsComponent } from './components/reservations/reservations.component';
import { RoomOverviewComponent } from './components/room-overview/room-overview.component';
import { LoginComponent } from './components/login/login.component';
import { AdminBuilderComponent } from './components/admin-builder/admin-builder.component';
import { Arbeitsplatz, Benutzer, FirmenEinstellung, Raum, RaumAuswahl, Reservierung, Standort, Zeitraum } from './models';
import { ArbeitsplatzService } from './services/arbeitsplatz.service';
import { EntraAuthService } from './auth/entra-auth.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, IntroComponent, RoomOverviewComponent, ReservationsComponent, LoginComponent, AdminBuilderComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  benutzer = signal<Benutzer[]>([]);
  aktiverBenutzer = signal<Benutzer | null>(null);
  standorte = signal<Standort[]>([]);
  raeume = signal<RaumAuswahl[]>([]);
  raum = signal<Raum | null>(null);
  reservierungen = signal<Reservierung[]>([]);
  ausgewaehlterTisch = signal<Arbeitsplatz | null>(null);
  aktiveAnsicht = signal<'start' | 'raum' | 'reservierungen' | 'admin'>('start');
  fehler = signal('');
  nachricht = signal('');
  testLoginAktiv = signal(false);
  laden = signal(false);
  firmenEinstellung = signal<FirmenEinstellung>({ firmenname: 'HOLTER', produktname: 'DeskVision', primaerfarbe: '#a51e2d', akzentfarbe: '#343638', logoUrl: '/holter-logo.png' });
  buchbarBis = signal('');
  loadBookingSettings(): void {
    this.arbeitsplatzService.getBookingSettings().subscribe({
      next: (wert) => this.buchbarBis.set(wert.buchbarBis),
      error: () => this.fehler.set('Buchungsgrenze konnte nicht geladen werden.')
    });
  }
  private raumAnfrage = 0;
  zeitraum = signal<Zeitraum>({
    datum: this.getCurrentDate(),
    endDatum: this.getCurrentDate(),
    beginn: '08:00',
    ende: '16:00'
  });

  constructor(private arbeitsplatzService: ArbeitsplatzService, private entraAuth: EntraAuthService) {}

  ngOnInit(): void {
    this.laden.set(true);
    this.loadCompanySettings();
    this.loadBookingSettings();
    this.loadRuntimeSettings();
    this.entraAuth.initialize()
      .then((angemeldet) => angemeldet ? this.loadCurrentEntraUser() : this.laden.set(false))
      .catch((fehler) => {
        console.error('Microsoft-Anmeldung konnte nicht initialisiert werden:', fehler);
        const grund = fehler?.errorMessage || fehler?.message || '';
        this.fehler.set(grund
          ? `Microsoft-Anmeldung fehlgeschlagen: ${grund}`
          : 'Microsoft-Anmeldung konnte nicht initialisiert werden. Bitte Verbindung und Entra-Konfiguration prüfen.');
        this.laden.set(false);
      });
  }

  private loadRuntimeSettings(): void {
    this.arbeitsplatzService.getRuntimeSettings().subscribe({
      next: (einstellung) => this.testLoginAktiv.set(einstellung.testLoginAktiv),
      error: () => this.testLoginAktiv.set(false)
    });
  }

  loadCompanySettings(): void {
    this.arbeitsplatzService.getCompanySettings().subscribe({
      next: (einstellung) => this.applyCompanyBranding(einstellung),
      error: () => this.applyCompanyBranding(this.firmenEinstellung())
    });
  }

  applyCompanyBranding(einstellung: FirmenEinstellung): void {
    this.firmenEinstellung.set(einstellung);
    document.documentElement.style.setProperty('--primary-color', einstellung.primaerfarbe);
    document.documentElement.style.setProperty('--accent-color', einstellung.akzentfarbe);
  }

  loadLocations(nachLaden?: () => void): void {
    this.arbeitsplatzService.getLocations().subscribe({
      next: (standorte) => { this.standorte.set(standorte); nachLaden?.(); },
      error: () => this.fehler.set('Standorte konnten nicht geladen werden. Läuft MongoDB?')
    });
  }

  loginWithTestAccount(zugang: { email: string; passwort: string }): void {
    this.laden.set(true);
    this.fehler.set('');
    this.arbeitsplatzService.loginWithTestAccount(zugang.email, zugang.passwort).subscribe({
      next: (benutzer) => this.completeLogin(benutzer),
      error: () => {
        this.fehler.set('E-Mail oder Passwort ist falsch.');
        this.laden.set(false);
      }
    });
  }

  loginWithMicrosoft(): void {
    this.laden.set(true);
    this.fehler.set('');
    this.entraAuth.login().catch(() => {
      this.fehler.set('Die Weiterleitung zu Microsoft Entra ID ist fehlgeschlagen.');
      this.laden.set(false);
    });
  }

  private loadCurrentEntraUser(): void {
    this.arbeitsplatzService.getCurrentEntraUser().subscribe({
      next: (benutzer) => this.completeLogin(benutzer),
      error: (error) => {
        this.fehler.set(error?.status === 404
          ? 'Dein Microsoft-Konto ist gültig, aber noch keinem Mitarbeiterprofil zugeordnet. Bitte die Entra-E-Mail in der Mitarbeiterverwaltung eintragen.'
          : 'Microsoft-Anmeldung war erfolgreich, aber das Mitarbeiterprofil konnte nicht geladen werden. Bitte Backend und E-Mail-Zuordnung prüfen.');
        this.laden.set(false);
      }
    });
  }

  private completeLogin(benutzer: Benutzer): void {
    this.aktiverBenutzer.set(benutzer);
    this.aktiveAnsicht.set('start');
    this.laden.set(false);
    this.loadLocations(() => this.loadInitialRoomPreview());
  }

  closeProfileOnOutsideClick(event: Event, profil: HTMLDetailsElement): void {
    if (event.target instanceof Node && !profil.contains(event.target)) {
      profil.open = false;
    }
  }

  logout(): void {
    const warEntraAnmeldung = this.entraAuth.isLoggedIn();
    this.aktiverBenutzer.set(null);
    this.raum.set(null);
    this.raeume.set([]);
    this.reservierungen.set([]);
    this.ausgewaehlterTisch.set(null);
    this.aktiveAnsicht.set('start');
    this.fehler.set('');
    this.nachricht.set('');
    if (warEntraAnmeldung) this.entraAuth.logout();
  }

  openAdmin(): void {
    if (this.isAdmin(this.aktiverBenutzer())) {
      this.aktiveAnsicht.set('admin');
    }
  }

  showSuccessMessage(text: string): void {
    this.fehler.set('');
    this.nachricht.set(text);
  }

  showErrorMessage(text: string): void {
    this.nachricht.set('');
    this.fehler.set(text);
  }

  switchUser(benutzerId: string): void {
    const benutzer = this.benutzer().find((eintrag) => eintrag.id === benutzerId);
    if (!benutzer) {
      return;
    }

    this.aktiverBenutzer.set(benutzer);
    this.aktiveAnsicht.set('start');
    this.fehler.set('');
    this.nachricht.set('');
    this.loadInitialRoomPreview();
  }

  loadLocationPreview(standortId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    const anfrage = ++this.raumAnfrage;
    this.raum.set(null);
    this.raeume.set([]);
    this.fehler.set('');
    this.arbeitsplatzService.getRoomsByLocation(standortId).subscribe({
      next: (raeume) => {
        if (anfrage !== this.raumAnfrage) return;
        this.raeume.set(raeume);
        if (!raeume.length) {
          this.fehler.set('An diesem Standort gibt es noch keine Räume.');
          return;
        }

        const eigeneAbteilung = raeume.find((raum) => raum.abteilungName === benutzer.abteilungName);
        this.loadRoomPreview((eigeneAbteilung ?? raeume[0]).id);
      },
      error: () => this.fehler.set('Räume konnten nicht geladen werden.')
    });
  }

  loadDepartmentPreview(abteilungName: string): void {
    ++this.raumAnfrage;
    this.raum.set(null);
    this.fehler.set('');
    const raum = this.raeume().find((eintrag) => eintrag.abteilungName === abteilungName);
    if (raum) this.loadRoomPreview(raum.id);
  }

  openLocation(standortId: string): void {
    const raum = this.raum();
    if (raum && raum.standortId === standortId) this.openRoom(raum.id);
  }

  openRoom(raumId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.getRoom(raumId, this.zeitraum(), benutzer.id).subscribe({
      next: (raum) => {
        this.setRoom(raum);
        this.aktiveAnsicht.set('raum');
        this.laden.set(false);
      },
      error: () => {
        this.fehler.set('Der Raum konnte nicht geladen werden.');
        this.laden.set(false);
      }
    });
  }

  checkAvailability(zeitraum: Zeitraum): void {
    const raum = this.raum();
    if (!raum) {
      return;
    }

    this.zeitraum.set(zeitraum);
    if (this.timeRangeStartsInPast(zeitraum)) {
      this.ausgewaehlterTisch.set(null);
      this.fehler.set('Reservierungen in der Vergangenheit sind nicht möglich.');
      return;
    }
    this.fehler.set('');
    this.openRoom(raum.id);
  }

  openHome(): void {
    if (this.aktiveAnsicht() !== 'start') this.loadInitialRoomPreview();
    this.aktiveAnsicht.set('start');
    this.nachricht.set('');
  }

  openWorkstationSearch(): void {
    this.aktiveAnsicht.set(this.raum() ? 'raum' : 'start');
    this.fehler.set('');
    this.nachricht.set('');
  }

  openReservations(): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.aktiveAnsicht.set('reservierungen');
    this.loadReservations();
  }

  selectDesk(arbeitsplatz: Arbeitsplatz): void {
    this.ausgewaehlterTisch.set(arbeitsplatz);
    this.fehler.set('');
    this.nachricht.set('');
  }

  clearDeskSelection(): void {
    this.ausgewaehlterTisch.set(null);
    this.fehler.set('');
  }

  createReservation(): void {
    const benutzer = this.aktiverBenutzer();
    const tisch = this.ausgewaehlterTisch();

    if (!benutzer || !tisch || !tisch.reservierbar) {
      return;
    }

    if (this.timeRangeStartsInPast(this.zeitraum())) {
      this.fehler.set('Reservierungen in der Vergangenheit sind nicht möglich.');
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.createReservation(benutzer.id, tisch.id, this.zeitraum()).subscribe({
      next: () => {
        this.nachricht.set(`Arbeitsplatz ${tisch.tischnr} wurde für den gewählten Zeitraum reserviert.`);
        const aktuellerRaum = this.raum();
        if (aktuellerRaum) {
          this.openRoom(aktuellerRaum.id);
        }
      },
      error: (error) => {
        this.fehler.set(error?.error?.details || error?.error?.message || 'Die Reservierung ist nicht möglich. Zeitraum oder Abteilung prüfen.');
        this.laden.set(false);
      }
    });
  }

  cancelReservation(reservierungId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.cancelReservation(reservierungId, benutzer.id).subscribe({
      next: () => {
        this.nachricht.set('Reservierung wurde storniert.');
        this.loadReservations();
      },
      error: () => {
        this.fehler.set('Reservierung konnte nicht storniert werden.');
        this.laden.set(false);
      }
    });
  }

  updateReservation(aenderung: { reservierung: Reservierung; anfang: string; ende: string }): void {
    const benutzer = this.aktiverBenutzer(); if (!benutzer) return;
    const anfang = new Date(aenderung.anfang);
    const ende = new Date(aenderung.ende);
    if (Number.isNaN(anfang.getTime()) || Number.isNaN(ende.getTime()) || anfang >= ende) {
      this.fehler.set('Bitte einen gültigen Zeitraum auswählen. Das Ende muss nach dem Beginn liegen.');
      return;
    }
    if (anfang.getTime() < Date.now()) {
      this.fehler.set('Reservierungen in der Vergangenheit sind nicht möglich.');
      return;
    }
    this.laden.set(true);
    this.arbeitsplatzService.updateReservation(aenderung.reservierung, benutzer.id, aenderung.anfang, aenderung.ende).subscribe({
      next: () => { this.nachricht.set('Reservierungszeitraum wurde geändert.'); this.loadReservations(); },
      error: (error) => { this.fehler.set(error?.error?.details || error?.error?.message || 'Zeitraum konnte nicht geändert werden. Bitte Buchungsgrenze und Überschneidungen prüfen.'); this.laden.set(false); }
    });
  }

  closeMessage(type: 'error' | 'success'): void {
    if (type === 'error') this.fehler.set('');
    else this.nachricht.set('');
  }

  private timeRangeStartsInPast(zeitraum: Zeitraum): boolean {
    const anfang = new Date(`${zeitraum.datum}T${zeitraum.beginn}`);
    return Number.isNaN(anfang.getTime()) || anfang.getTime() < Date.now();
  }

  private loadReservations(): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.getReservations(benutzer.id).subscribe({
      next: (reservierungen) => {
        this.reservierungen.set(reservierungen);
        this.laden.set(false);
      },
      error: () => {
        this.fehler.set('Reservierungen konnten nicht geladen werden.');
        this.laden.set(false);
      }
    });
  }

  private loadRoomPreview(raumId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    if (!raumId) { this.raum.set(null); return; }
    const anfrage = ++this.raumAnfrage;
    this.arbeitsplatzService.getRoom(raumId, this.zeitraum(), benutzer.id).subscribe({
      next: (raum) => {
        if (anfrage !== this.raumAnfrage) return;
        this.raum.set(raum);
        this.ausgewaehlterTisch.set(null);
      },
      error: () => this.fehler.set('Der vorgeschlagene Raum konnte nicht geladen werden.')
    });
  }

  private loadInitialRoomPreview(): void {
    const ersterStandort = this.standorte()[0];
    if (ersterStandort) this.loadLocationPreview(ersterStandort.id);
  }

  isAdmin(benutzer: Benutzer | null): boolean {
    return benutzer?.rolle === 'ADMIN' || benutzer?.rolle === 'SUPERADMIN';
  }

  private getCurrentDate(): string {
    const heute = new Date();
    const monat = String(heute.getMonth() + 1).padStart(2, '0');
    const tag = String(heute.getDate()).padStart(2, '0');
    return `${heute.getFullYear()}-${monat}-${tag}`;
  }

  private setRoom(raum: Raum): void {
    const bisherigeId = this.ausgewaehlterTisch()?.id;
    this.raum.set(raum);
    this.ausgewaehlterTisch.set(raum.arbeitsplaetze.find((tisch) => tisch.id === bisherigeId)
      ?? raum.arbeitsplaetze.find((tisch) => tisch.reservierbar) ?? raum.arbeitsplaetze[0]);

    this.arbeitsplatzService
      .getRoomsByLocation(raum.standortId)
      .subscribe((raeume) => this.raeume.set(raeume.filter((eintrag) => eintrag.abteilungName === raum.abteilungName)));
  }
}
