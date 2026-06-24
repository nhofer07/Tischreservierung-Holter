import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { Arbeitsplatz, Raum, RaumAuswahl, Zeitraum } from '../../models';
import { DeskDetailsComponent } from '../desk-details/desk-details.component';

@Component({
  selector: 'app-room-overview',
  imports: [CommonModule, DeskDetailsComponent],
  templateUrl: './room-overview.component.html',
  styleUrl: './room-overview.component.css'
})
export class RoomOverviewComponent {
  @Input({ required: true }) raum!: Raum;
  @Input() raeume: RaumAuswahl[] = [];
  @Input() ausgewaehlterTisch: Arbeitsplatz | null = null;
  @Input() fehler = '';

  @Output() tischAuswaehlen = new EventEmitter<Arbeitsplatz>();
  @Output() reservieren = new EventEmitter<void>();
  @Output() raumWechseln = new EventEmitter<string>();
  @Output() zeitraumPruefen = new EventEmitter<Zeitraum>();

  zeitraumSignal = signal<Zeitraum>({ datum: '', beginn: '08:00', ende: '16:00' });

  @Input({ required: true }) set zeitraum(value: Zeitraum) {
    this.zeitraumSignal.set(value);
  }

  zeitraumGueltig = computed(() => {
    const zeitraum = this.zeitraumSignal();
    return !!zeitraum.datum && !!zeitraum.beginn && !!zeitraum.ende && zeitraum.beginn < zeitraum.ende;
  });

  get vorherigerRaum() {
    return this.raumNeben(-1);
  }

  get naechsterRaum() {
    return this.raumNeben(1);
  }

  get aktuelleAbteilung() {
    return this.raum.abteilungName;
  }

  get zeitraumText() {
    const zeitraum = this.zeitraumSignal();
    return `${zeitraum.datum} · ${zeitraum.beginn}-${zeitraum.ende}`;
  }

  zeitraumAendern(feld: keyof Zeitraum, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.zeitraumSignal.set({
      ...this.zeitraumSignal(),
      [feld]: input.value
    });
  }

  pruefen(): void {
    if (this.zeitraumGueltig()) {
      this.zeitraumPruefen.emit(this.zeitraumSignal());
    }
  }

  private raumNeben(richtung: -1 | 1) {
    const standortRaeume = this.raeume.filter((raum) => raum.standortId === this.raum.standortId);
    if (standortRaeume.length === 0) {
      return {
        id: this.raum.id,
        name: this.raum.name,
        standortId: this.raum.standortId,
        abteilungName: this.raum.abteilungName
      };
    }

    const aktuellerIndex = standortRaeume.findIndex((raum) => raum.id === this.raum.id);
    const index = aktuellerIndex === -1 ? 0 : aktuellerIndex;
    return standortRaeume[(index + richtung + standortRaeume.length) % standortRaeume.length];
  }
}
