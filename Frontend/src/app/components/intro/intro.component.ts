import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { Benutzer, Raum, RaumAuswahl, Standort } from '../../models';

@Component({
  selector: 'app-intro',
  imports: [CommonModule],
  templateUrl: './intro.component.html',
  styleUrl: './intro.component.css'
})
export class IntroComponent {
  standorteSignal = signal<Standort[]>([]);
  raumSignal = signal<Raum | null>(null);
  benutzerSignal = signal<Benutzer | null>(null);
  vorschauWurdeGeladen = signal(false);
  selectedStandortId = signal('');
  selectedAbteilung = signal('');
  raeumeSignal = signal<RaumAuswahl[]>([]);
  abteilungen = computed(() => [...new Set(this.raeumeSignal().filter((raum) => raum.standortId === this.selectedStandortId()).map((raum) => raum.abteilungName))]);

  @Input() set raeume(value: RaumAuswahl[]) { this.raeumeSignal.set(value); }
  @Output() abteilungAusgewaehlt = new EventEmitter<string>();

  @Output() standortAusgewaehlt = new EventEmitter<string>();
  @Output() standortOeffnen = new EventEmitter<string>();

  @Input() set standorte(value: Standort[]) {
    this.standorteSignal.set(value);
    if (!this.selectedStandortId() && value.length) this.selectedStandortId.set(value[0].id);
  }

  @Input() set raum(value: Raum | null) {
    this.raumSignal.set(value);
    this.vorschauWurdeGeladen.set(true);
    if (value) {
      this.selectedStandortId.set(value.standortId);
      this.selectedAbteilung.set(value.abteilungName);
    }
  }

  @Input() set benutzer(value: Benutzer | null) {
    this.benutzerSignal.set(value);
    if (!value) this.vorschauWurdeGeladen.set(false);
  }

  freieTische = computed(() =>
    this.raumSignal()?.arbeitsplaetze.filter((arbeitsplatz) => arbeitsplatz.status === 'frei').length ?? 0
  );

  reservierbareTische = computed(() =>
    this.raumSignal()?.arbeitsplaetze.filter((arbeitsplatz) => arbeitsplatz.reservierbar).length ?? 0
  );

  standortAuswaehlen(standortId: string): void {
    this.selectedStandortId.set(standortId);
    this.selectedAbteilung.set('');
    this.raumSignal.set(null);
    this.standortAusgewaehlt.emit(standortId);
  }

  abteilungAuswaehlen(name: string): void {
    this.selectedAbteilung.set(name);
    this.abteilungAusgewaehlt.emit(name);
  }

  openSelectedRoom(): void {
    this.standortOeffnen.emit(this.selectedStandortId());
  }

  hatBevorzugtenRaum(): boolean {
    return Boolean(this.benutzerSignal()?.bevorzugterRaumId);
  }
}
