import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Benutzer, Reservierung } from '../../models';

@Component({
  selector: 'app-reservations',
  imports: [CommonModule, FormsModule],
  templateUrl: './reservations.component.html',
  styleUrl: './reservations.component.css'
})
export class ReservationsComponent {
  @Input({ required: true }) benutzer!: Benutzer;
  @Input() reservierungen: Reservierung[] = [];
  @Input() laden = false;

  @Output() stornieren = new EventEmitter<string>();
  @Output() arbeitsplatzSuchen = new EventEmitter<void>();
  @Output() bearbeiten = new EventEmitter<{ reservierung: Reservierung; anfang: string; ende: string }>();

  ansicht: 'liste' | 'kalender' = 'liste';
  bearbeiteteId = '';
  bearbeitungAnfang = '';
  bearbeitungEnde = '';

  get aktiveReservierungen(): Reservierung[] {
    const jetzt = Date.now();
    return this.reservierungen.filter((r) => r.status === 'reserviert' && new Date(r.reservierungEnde).getTime() >= jetzt);
  }

  get vergangeneReservierungen(): Reservierung[] {
    const jetzt = Date.now();
    return this.reservierungen.filter((r) => r.status !== 'reserviert' || new Date(r.reservierungEnde).getTime() < jetzt);
  }

  get kalenderTage(): { datum: Date; reservierungen: Reservierung[] }[] {
    const start = new Date(); start.setHours(0,0,0,0);
    return Array.from({length:14}, (_, index) => {
      const datum = new Date(start); datum.setDate(start.getDate() + index);
      const tagesEnde = new Date(datum); tagesEnde.setDate(datum.getDate() + 1);
      return { datum, reservierungen:this.aktiveReservierungen.filter((r) =>
        new Date(r.reservierungAnfang) < tagesEnde && new Date(r.reservierungEnde) > datum
      ) };
    });
  }

  bearbeitungStarten(reservierung: Reservierung): void {
    this.bearbeiteteId = reservierung.id;
    this.bearbeitungAnfang = this.fuerEingabe(reservierung.reservierungAnfang);
    this.bearbeitungEnde = this.fuerEingabe(reservierung.reservierungEnde);
  }

  bearbeitungSpeichern(reservierung: Reservierung): void {
    this.bearbeiten.emit({ reservierung, anfang:this.bearbeitungAnfang, ende:this.bearbeitungEnde });
    this.bearbeiteteId = '';
  }

  private fuerEingabe(iso: string): string {
    const datum = new Date(iso); const lokal = new Date(datum.getTime() - datum.getTimezoneOffset() * 60000);
    return lokal.toISOString().slice(0,16);
  }

  datum(isoDatum: string): string {
    return new Intl.DateTimeFormat('de-AT', {
      weekday: 'short',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(isoDatum));
  }

  uhrzeit(isoDatum: string): string {
    return new Intl.DateTimeFormat('de-AT', {
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(isoDatum));
  }

  vergangenerStatus(reservierung: Reservierung): string {
    return reservierung.status === 'reserviert' ? 'abgelaufen' : reservierung.status;
  }
}
