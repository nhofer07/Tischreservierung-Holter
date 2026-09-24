import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FirmenEinstellung } from '../../models';
import { lokalerTestLoginAktiv } from '../../auth/entra-config';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  @Input() laden = false;
  @Input() fehler = '';
  @Input() firmenEinstellung: FirmenEinstellung = { firmenname: 'HOLTER', produktname: 'DeskVision', primaerfarbe: '#a51e2d', akzentfarbe: '#343638', logoUrl: '/holter-logo.png' };
  @Output() entraAnmelden = new EventEmitter<void>();
  @Output() testAnmelden = new EventEmitter<{ email: string; passwort: string }>();

  email = '';
  passwort = '';
  testLoginOffen = false;
  readonly lokalerTestLoginAktiv = lokalerTestLoginAktiv;

  login(): void {
    if (this.email.trim() && this.passwort) {
      this.testAnmelden.emit({ email: this.email.trim(), passwort: this.passwort });
    }
  }
}
