import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Arbeitsplatz, Raum } from '../../models';
import { DeskDetailsComponent } from '../desk-details/desk-details.component';

@Component({
  selector: 'app-room-overview',
  imports: [CommonModule, DeskDetailsComponent],
  templateUrl: './room-overview.component.html',
  styleUrl: './room-overview.component.css'
})
export class RoomOverviewComponent {
  @Input({ required: true }) raum!: Raum;
  @Input() ausgewaehlterTisch: Arbeitsplatz | null = null;
  @Input() fehler = '';

  @Output() zurueck = new EventEmitter<void>();
  @Output() tischAuswaehlen = new EventEmitter<Arbeitsplatz>();
  @Output() reservieren = new EventEmitter<void>();
}
