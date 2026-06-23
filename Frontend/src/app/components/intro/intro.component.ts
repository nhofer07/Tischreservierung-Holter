import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { Raum, Standort } from '../../models';

@Component({
  selector: 'app-intro',
  imports: [CommonModule],
  templateUrl: './intro.component.html',
  styleUrl: './intro.component.css'
})
export class IntroComponent {
  standorteSignal = signal<Standort[]>([]);
  raumSignal = signal<Raum | null>(null);
  selectedStandortId = signal('STANDORT-WELS');

  @Output() raumOeffnen = new EventEmitter<string>();

  @Input() set standorte(value: Standort[]) {
    this.standorteSignal.set(value);
  }

  @Input() set raum(value: Raum | null) {
    this.raumSignal.set(value);
  }

  freieTische = computed(() =>
    this.raumSignal()?.arbeitsplaetze.filter((arbeitsplatz) => arbeitsplatz.status === 'frei').length ?? 6
  );

  raumName = computed(() =>
    this.selectedStandortId() === 'STANDORT-LINZ' ? 'Projektraum Linz' : 'Teamraum Wels'
  );

  previewLayout = computed(() =>
    this.selectedStandortId() === 'STANDORT-LINZ' ? 'linz-layout' : 'wels-layout'
  );

  onStandortChanged(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedStandortId.set(select.value);
  }

  openSelectedRoom(): void {
    const roomId = this.selectedStandortId() === 'STANDORT-LINZ'
      ? 'RAUM-LINZ-EG-PROJEKT'
      : 'RAUM-WELS-OG1-TEAM';

    this.raumOeffnen.emit(roomId);
  }
}
