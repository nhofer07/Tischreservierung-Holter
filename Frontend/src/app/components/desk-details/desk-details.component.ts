import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Arbeitsplatz, Raum } from '../../models';

@Component({
  selector: 'app-desk-details',
  imports: [CommonModule],
  templateUrl: './desk-details.component.html',
  styleUrl: './desk-details.component.css'
})
export class DeskDetailsComponent {
  @Input({ required: true }) tisch!: Arbeitsplatz;
  @Input({ required: true }) raum!: Raum;
  @Input() fehler = '';
  @Output() reservieren = new EventEmitter<void>();
}
