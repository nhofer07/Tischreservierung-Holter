import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { Benutzer, Raum, Standort } from '../../models';

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
  selectedStandortId = signal('STANDORT-WELS');

  @Output() standortAusgewaehlt = new EventEmitter<string>();
  @Output() standortOeffnen = new EventEmitter<string>();

  @Input() set standorte(value: Standort[]) {
    this.standorteSignal.set(value);
  }

  @Input() set raum(value: Raum | null) {
    this.raumSignal.set(value);
    this.vorschauWurdeGeladen.set(true);
    if (value) {
      this.selectedStandortId.set(value.standortId);
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
    this.standortAusgewaehlt.emit(standortId);
  }

  openSelectedRoom(): void {
    this.standortOeffnen.emit(this.selectedStandortId());
  }

  hatBevorzugtenRaum(): boolean {
    return Boolean(this.benutzerSignal()?.bevorzugterRaumId);
  }
}
