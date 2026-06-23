import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Arbeitsplatz, Benutzer, Raum, Standort } from './models';
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
  raum = signal<Raum | null>(null);
  ausgewaehlterTisch = signal<Arbeitsplatz | null>(null);
  aktiverSchritt = signal<'start' | 'raum'>('start');
  fehler = signal('');

  constructor(private arbeitsplatzService: ArbeitsplatzService) {}

  ngOnInit(): void {
    this.arbeitsplatzService.getDemoBenutzer().subscribe((benutzer) => {
      this.benutzer.set(benutzer);
      this.arbeitsplatzService.getRaum(benutzer.bevorzugterRaumId).subscribe((raum) => {
        this.raum.set(raum);
        this.ausgewaehlterTisch.set(raum.arbeitsplaetze[0]);
      });
    });

    this.arbeitsplatzService.getStandorte().subscribe((standorte) => this.standorte.set(standorte));
  }

  raumOeffnen(raumId: string): void {
    this.arbeitsplatzService.getRaum(raumId).subscribe((raum) => {
      this.raum.set(raum);
      this.ausgewaehlterTisch.set(raum.arbeitsplaetze[0]);
      this.aktiverSchritt.set('raum');
    });
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

    this.arbeitsplatzService.reservieren(benutzer.id, tisch.id).subscribe({
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
}
