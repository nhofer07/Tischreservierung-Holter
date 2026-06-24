import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Arbeitsplatz, Benutzer, Raum, RaumAuswahl, Standort, Zeitraum } from './models';
import { ArbeitsplatzService } from './services/arbeitsplatz.service';
import { IntroComponent } from './components/intro/intro.component';
import { RoomOverviewComponent } from './components/room-overview/room-overview.component';

@Component({
  selector: 'app-root',
  imports: [CommonModule, IntroComponent, RoomOverviewComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  benutzer = signal<Benutzer | null>(null);
  standorte = signal<Standort[]>([]);
  raeume = signal<RaumAuswahl[]>([]);
  raum = signal<Raum | null>(null);
  ausgewaehlterTisch = signal<Arbeitsplatz | null>(null);
  aktiverSchritt = signal<'start' | 'raum'>('start');
  fehler = signal('');
  zeitraum = signal<Zeitraum>({
    datum: this.heutigesDatum(),
    beginn: '08:00',
    ende: '16:00'
  });

  constructor(private arbeitsplatzService: ArbeitsplatzService) {}

  ngOnInit(): void {
    this.arbeitsplatzService.getDemoBenutzer().subscribe((benutzer) => {
      this.benutzer.set(benutzer);
      this.arbeitsplatzService.getRaum(benutzer.bevorzugterRaumId, this.zeitraum()).subscribe((raum) => {
        this.raumSetzen(raum);
      });
    });

    this.arbeitsplatzService.getStandorte().subscribe((standorte) => this.standorte.set(standorte));
  }

  raumOeffnen(raumId: string): void {
    this.arbeitsplatzService.getRaum(raumId, this.zeitraum()).subscribe((raum) => {
      this.raumSetzen(raum);
      this.aktiverSchritt.set('raum');
    });
  }

  standortOeffnen(standortId: string): void {
    this.arbeitsplatzService.getRaeumeByStandort(standortId).subscribe((raeume) => {
      this.raeume.set(raeume);

      if (raeume.length > 0) {
        this.raumOeffnen(raeume[0].id);
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
    this.aktiverSchritt.set('start');
  }

  tischAuswaehlen(arbeitsplatz: Arbeitsplatz): void {
    this.ausgewaehlterTisch.set(arbeitsplatz);
    this.fehler.set('');
  }

  reservieren(): void {
    const benutzer = this.benutzer();
    const tisch = this.ausgewaehlterTisch();

    if (!benutzer || !tisch) {
      return;
    }

    this.arbeitsplatzService.reservieren(benutzer.id, tisch.id, this.zeitraum()).subscribe({
      next: (reservierterTisch) => {
        const raum = this.raum();
        if (!raum) {
          return;
        }

        this.raum.set({
          ...raum,
          arbeitsplaetze: raum.arbeitsplaetze.map((arbeitsplatz) =>
            arbeitsplatz.id === reservierterTisch.id ? reservierterTisch : arbeitsplatz
          )
        });
        this.ausgewaehlterTisch.set(reservierterTisch);
        this.fehler.set('');
      },
      error: () => this.fehler.set('Dieser Arbeitsplatz kann gerade nicht reserviert werden.')
    });
  }

  private heutigesDatum() {
    const heute = new Date();
    const monat = String(heute.getMonth() + 1).padStart(2, '0');
    const tag = String(heute.getDate()).padStart(2, '0');
    return `${heute.getFullYear()}-${monat}-${tag}`;
  }

  private raumSetzen(raum: Raum): void {
    this.raum.set(raum);
    this.ausgewaehlterTisch.set(raum.arbeitsplaetze[0]);

    this.arbeitsplatzService
      .getRaeumeByStandort(raum.standortId)
      .subscribe((raeume) => this.raeume.set(raeume));
  }
}
