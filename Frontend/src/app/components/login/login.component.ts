import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  @Input() laden = false;
  @Input() fehler = '';
  @Output() anmelden = new EventEmitter<{ email: string; passwort: string }>();

  email = '';
  passwort = '';

  login(): void {
    if (this.email.trim() && this.passwort) {
      this.anmelden.emit({ email: this.email.trim(), passwort: this.passwort });
    }
  }
}
