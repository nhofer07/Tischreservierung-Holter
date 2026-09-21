import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { IntroComponent } from './components/intro/intro.component';
import { ReservationsComponent } from './components/reservations/reservations.component';
import { RoomOverviewComponent } from './components/room-overview/room-overview.component';
import { LoginComponent } from './components/login/login.component';
import { AdminBuilderComponent } from './components/admin-builder/admin-builder.component';
import { Arbeitsplatz, Benutzer, Raum, RaumAuswahl, Reservierung, Standort, Zeitraum } from './models';
import { ArbeitsplatzService } from './services/arbeitsplatz.service';

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
  laden = signal(false);
  private raumAnfrage = 0;
  zeitraum = signal<Zeitraum>({
    datum: this.heutigesDatum(),
    beginn: '08:00',
    ende: '16:00'
  });

  constructor(private arbeitsplatzService: ArbeitsplatzService) {}

  ngOnInit(): void {
    this.laden.set(true);
    this.standorteLaden();
    this.laden.set(false);
  }

  standorteLaden(): void {
    this.arbeitsplatzService.getStandorte().subscribe({
      next: (standorte) => this.standorte.set(standorte),
      error: () => this.fehler.set('Standorte konnten nicht geladen werden. Läuft MongoDB?')
    });
  }

  anmelden(zugang: { email: string; passwort: string }): void {
    this.laden.set(true);
    this.fehler.set('');
    this.arbeitsplatzService.login(zugang.email, zugang.passwort).subscribe({
      next: (benutzer) => {
        this.aktiverBenutzer.set(benutzer);
        this.aktiveAnsicht.set('start');
        this.laden.set(false);
        this.ersteRaumvorschauLaden(benutzer);
      },
      error: () => {
        this.fehler.set('E-Mail oder Passwort ist falsch.');
        this.laden.set(false);
      }
    });
  }

  abmelden(): void {
    this.aktiverBenutzer.set(null);
    this.raum.set(null);
    this.raeume.set([]);
    this.reservierungen.set([]);
    this.ausgewaehlterTisch.set(null);
    this.aktiveAnsicht.set('start');
    this.fehler.set('');
    this.nachricht.set('');
  }

  adminOeffnen(): void {
    if (this.istAdmin(this.aktiverBenutzer())) {
      this.aktiveAnsicht.set('admin');
    }
  }

  meldungAnzeigen(text: string): void {
    this.fehler.set('');
    this.nachricht.set(text);
  }

  fehlerAnzeigen(text: string): void {
    this.nachricht.set('');
    this.fehler.set(text);
  }

  benutzerWechseln(benutzerId: string): void {
    const benutzer = this.benutzer().find((eintrag) => eintrag.id === benutzerId);
    if (!benutzer) {
      return;
    }

    this.aktiverBenutzer.set(benutzer);
    this.aktiveAnsicht.set('start');
    this.fehler.set('');
    this.nachricht.set('');
    this.ersteRaumvorschauLaden(benutzer);
  }

  standortVorschauLaden(standortId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    const anfrage = ++this.raumAnfrage;
    this.raum.set(null);
    this.arbeitsplatzService.getRaeumeByStandort(standortId).subscribe({
      next: (raeume) => {
        if (anfrage !== this.raumAnfrage) return;
        const sichtbareRaeume = this.raeumeDerAbteilung(raeume, benutzer);
        this.raeume.set(sichtbareRaeume);
        const raum = this.passendenRaumFinden(sichtbareRaeume, benutzer);
        if (raum) {
          this.vorschauRaumLaden(raum.id);
        } else this.fehler.set('An diesem Standort gibt es keinen Raum für deine Abteilung.');
      },
      error: () => this.fehler.set('Räume konnten nicht geladen werden.')
    });
  }

  standortOeffnen(standortId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    const anfrage = ++this.raumAnfrage;
    this.arbeitsplatzService.getRaeumeByStandort(standortId).subscribe({
      next: (raeume) => {
        if (anfrage !== this.raumAnfrage) return;
        const sichtbareRaeume = this.raeumeDerAbteilung(raeume, benutzer);
        this.raeume.set(sichtbareRaeume);
        const raum = this.passendenRaumFinden(sichtbareRaeume, benutzer);
        if (raum) {
          this.raumOeffnen(raum.id);
        } else { this.fehler.set('An diesem Standort gibt es keinen Raum für deine Abteilung.'); this.laden.set(false); }
      },
      error: () => {
        this.fehler.set('Räume konnten nicht geladen werden.');
        this.laden.set(false);
      }
    });
  }

  raumOeffnen(raumId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.getRaum(raumId, this.zeitraum(), benutzer.id).subscribe({
      next: (raum) => {
        this.raumSetzen(raum);
        this.aktiveAnsicht.set('raum');
        this.laden.set(false);
      },
      error: () => {
        this.fehler.set('Der Raum konnte nicht geladen werden.');
        this.laden.set(false);
      }
    });
  }

  zeitraumPruefen(zeitraum: Zeitraum): void {
    const raum = this.raum();
    if (!raum) {
      return;
    }

    this.zeitraum.set(zeitraum);
    this.raumOeffnen(raum.id);
  }

  startOeffnen(): void {
    this.aktiveAnsicht.set('start');
    this.nachricht.set('');
  }

  reservierungenOeffnen(): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.aktiveAnsicht.set('reservierungen');
    this.reservierungenLaden();
  }

  tischAuswaehlen(arbeitsplatz: Arbeitsplatz): void {
    this.ausgewaehlterTisch.set(arbeitsplatz);
    this.fehler.set('');
    this.nachricht.set('');
  }

  reservieren(wiederholungen = 0): void {
    const benutzer = this.aktiverBenutzer();
    const tisch = this.ausgewaehlterTisch();

    if (!benutzer || !tisch || !tisch.reservierbar) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.reservieren(benutzer.id, tisch.id, this.zeitraum(), wiederholungen).subscribe({
      next: () => {
        this.nachricht.set(wiederholungen ? `Arbeitsplatz ${tisch.tischnr} wurde für ${wiederholungen + 1} Wochen reserviert.` : `Arbeitsplatz ${tisch.tischnr} wurde reserviert.`);
        const aktuellerRaum = this.raum();
        if (aktuellerRaum) {
          this.raumOeffnen(aktuellerRaum.id);
        }
      },
      error: () => {
        this.fehler.set('Die Reservierung ist nicht möglich. Zeitraum oder Abteilung prüfen.');
        this.laden.set(false);
      }
    });
  }

  stornieren(reservierungId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.stornieren(reservierungId, benutzer.id).subscribe({
      next: () => {
        this.nachricht.set('Reservierung wurde storniert.');
        this.reservierungenLaden();
      },
      error: () => {
        this.fehler.set('Reservierung konnte nicht storniert werden.');
        this.laden.set(false);
      }
    });
  }

  reservierungBearbeiten(aenderung: { reservierung: Reservierung; anfang: string; ende: string }): void {
    const benutzer = this.aktiverBenutzer(); if (!benutzer) return;
    this.laden.set(true);
    this.arbeitsplatzService.reservierungAendern(aenderung.reservierung, benutzer.id, aenderung.anfang, aenderung.ende).subscribe({
      next: () => { this.nachricht.set('Reservierungszeitraum wurde geändert.'); this.reservierungenLaden(); },
      error: () => { this.fehler.set('Zeitraum konnte nicht geändert werden. Bitte Überschneidungen prüfen.'); this.laden.set(false); }
    });
  }

  private reservierungenLaden(): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    this.laden.set(true);
    this.arbeitsplatzService.getReservierungen(benutzer.id).subscribe({
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

  private vorschauRaumLaden(raumId: string): void {
    const benutzer = this.aktiverBenutzer();
    if (!benutzer) {
      return;
    }

    if (!raumId) { this.raum.set(null); return; }
    const anfrage = ++this.raumAnfrage;
    this.arbeitsplatzService.getRaum(raumId, this.zeitraum(), benutzer.id).subscribe({
      next: (raum) => {
        if (anfrage !== this.raumAnfrage) return;
        this.raum.set(raum);
        this.ausgewaehlterTisch.set(null);
      },
      error: () => this.fehler.set('Der vorgeschlagene Raum konnte nicht geladen werden.')
    });
  }

  private ersteRaumvorschauLaden(benutzer: Benutzer): void {
    if (benutzer.bevorzugterRaumId) {
      this.vorschauRaumLaden(benutzer.bevorzugterRaumId);
      return;
    }
    const ersterStandort = this.standorte()[0];
    if (ersterStandort) this.standortVorschauLaden(ersterStandort.id);
  }

  private passendenRaumFinden(raeume: RaumAuswahl[], benutzer: Benutzer): RaumAuswahl | undefined {
    return raeume.find((raum) => raum.id === benutzer.bevorzugterRaumId)
      ?? raeume.find((raum) => raum.abteilungName === benutzer.abteilungName)
      ?? raeume[0];
  }

  private raeumeDerAbteilung(raeume: RaumAuswahl[], benutzer: Benutzer): RaumAuswahl[] {
    return benutzer.rolle === 'SUPERADMIN'
      ? raeume
      : raeume.filter((raum) => raum.abteilungName === benutzer.abteilungName);
  }

  istAdmin(benutzer: Benutzer | null): boolean {
    return benutzer?.rolle === 'ADMIN' || benutzer?.rolle === 'SUPERADMIN';
  }

  private heutigesDatum(): string {
    const heute = new Date();
    const monat = String(heute.getMonth() + 1).padStart(2, '0');
    const tag = String(heute.getDate()).padStart(2, '0');
    return `${heute.getFullYear()}-${monat}-${tag}`;
  }

  private raumSetzen(raum: Raum): void {
    const bisherigeId = this.ausgewaehlterTisch()?.id;
    this.raum.set(raum);
    this.ausgewaehlterTisch.set(raum.arbeitsplaetze.find((tisch) => tisch.id === bisherigeId)
      ?? raum.arbeitsplaetze.find((tisch) => tisch.reservierbar) ?? raum.arbeitsplaetze[0]);

    this.arbeitsplatzService
      .getRaeumeByStandort(raum.standortId)
      .subscribe((raeume) => this.raeume.set(this.raeumeDerAbteilung(raeume, this.aktiverBenutzer()!)));
  }
}
