import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, computed, signal } from '@angular/core';
import { Arbeitsplatz, Benutzer, Raum, RaumAuswahl, Zeitraum } from '../../models';
import { DeskDetailsComponent } from '../desk-details/desk-details.component';

@Component({
  selector: 'app-room-overview',
  imports: [CommonModule, DeskDetailsComponent],
  templateUrl: './room-overview.component.html',
  styleUrl: './room-overview.component.css'
})
export class RoomOverviewComponent implements OnInit, OnDestroy {
  @Input({ required: true }) raum!: Raum;
  @Input({ required: true }) benutzer!: Benutzer;
  @Input() raeume: RaumAuswahl[] = [];
  @Input() ausgewaehlterTisch: Arbeitsplatz | null = null;
  @Input() fehler = '';

  @Output() tischAuswaehlen = new EventEmitter<Arbeitsplatz>();
  @Output() reservieren = new EventEmitter<number>();
  @Output() raumWechseln = new EventEmitter<string>();
  @Output() zeitraumPruefen = new EventEmitter<Zeitraum>();

  zeitraumSignal = signal<Zeitraum>({ datum: '', beginn: '08:00', ende: '16:00' });
  ausstattungFilter = signal<string[]>([]);
  wiederholungen = signal(0);
  vollbild = signal(false);
  private aktualisierung?: ReturnType<typeof setInterval>;

  ngOnInit(): void { this.aktualisierung = setInterval(() => this.pruefen(), 30000); }
  ngOnDestroy(): void { if (this.aktualisierung) clearInterval(this.aktualisierung); }

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

  get kannRaumWechseln(): boolean {
    return this.raeume.filter((eintrag) => eintrag.standortId === this.raum.standortId &&
      (this.benutzer.rolle === 'SUPERADMIN' || eintrag.abteilungName === this.raum.abteilungName)).length > 1;
  }

  get ausstattungen(): string[] {
    const namen = this.raum.arbeitsplaetze.flatMap((tisch) => tisch.equipment.map((item) => item.name));
    return [...new Set(namen)].sort();
  }

  get sichtbareTische(): Arbeitsplatz[] {
    const filter = this.ausstattungFilter();
    if (filter.length === 0) {
      return this.raum.arbeitsplaetze;
    }

    return this.raum.arbeitsplaetze.filter((tisch) =>
      filter.every((name) => tisch.equipment.some((item) => item.name === name))
    );
  }

  get zeitraumText() {
    const zeitraum = this.zeitraumSignal();
    const datum = zeitraum.datum.split('-').reverse().join('.');
    return `${datum} · ${zeitraum.beginn}–${zeitraum.ende}`;
  }

  zeitraumAendern(feld: keyof Zeitraum, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.zeitraumSignal.set({
      ...this.zeitraumSignal(),
      [feld]: input.value
    });
  }

  ausstattungAendern(name: string, aktiv: boolean): void {
    this.ausstattungFilter.update((filter) =>
      aktiv ? [...filter, name] : filter.filter((eintrag) => eintrag !== name)
    );
  }

  pruefen(): void {
    if (this.zeitraumGueltig()) {
      this.zeitraumPruefen.emit(this.zeitraumSignal());
    }
  }

  schnellzeit(typ: 'ganztag' | 'vormittag' | 'nachmittag'): void {
    const zeiten = typ === 'ganztag' ? ['08:00','16:00'] : typ === 'vormittag' ? ['08:00','12:00'] : ['12:00','16:00'];
    this.zeitraumSignal.set({ ...this.zeitraumSignal(), beginn:zeiten[0], ende:zeiten[1] });
    this.pruefen();
  }

  tooltip(tisch: Arbeitsplatz): string {
    if (!tisch.reserviertVon) return tisch.name;
    const von = new Date(tisch.reservierungAnfang).toLocaleTimeString('de-AT',{hour:'2-digit',minute:'2-digit'});
    const bis = new Date(tisch.reservierungEnde).toLocaleTimeString('de-AT',{hour:'2-digit',minute:'2-digit'});
    return `Reserviert von ${tisch.reserviertVon}, ${von}–${bis}`;
  }

  vollbildUmschalten(): void { this.vollbild.update((aktiv) => !aktiv); }

  private raumNeben(richtung: -1 | 1) {
    const standortRaeume = this.raeume.filter((raum) =>
      raum.standortId === this.raum.standortId &&
      (this.benutzer.rolle === 'SUPERADMIN' || raum.abteilungName === this.raum.abteilungName)
    );
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
