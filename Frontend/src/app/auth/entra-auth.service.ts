import { Injectable } from '@angular/core';
import { AccountInfo, InteractionRequiredAuthError, PublicClientApplication } from '@azure/msal-browser';
import { entraLoginRequest, entraScopes, msalConfig } from './entra-config';

@Injectable({ providedIn: 'root' })
export class EntraAuthService {
  private readonly msal = new PublicClientApplication(msalConfig);
  private initialisiert = false;
  private initialisierung: Promise<void> | null = null;

  async initialisieren(): Promise<boolean> {
    if (!this.initialisiert) {
      if (!this.initialisierung) {
        this.initialisierung = this.msalInitialisieren();
      }

      try {
        await this.initialisierung;
      } catch (fehler) {
        // Ein fehlgeschlagener Versuch darf den Login-Button nicht dauerhaft sperren.
        this.initialisierung = null;
        throw fehler;
      }
    }

    const konto = this.aktivesKonto();
    if (konto) this.msal.setActiveAccount(konto);
    return !!konto;
  }

  private async msalInitialisieren(): Promise<void> {
    await this.msal.initialize();

    try {
      const ergebnis = await this.msal.handleRedirectPromise();
      if (ergebnis?.account) this.msal.setActiveAccount(ergebnis.account);
    } catch (fehler) {
      // Der fehlerhafte Redirect wird entfernt, damit ein neuer Versuch möglich
      // ist. Der Fehler wird trotzdem weitergegeben und dadurch sichtbar.
      await this.msal.clearCache();
      history.replaceState({}, document.title, window.location.pathname + window.location.search);
      throw fehler;
    }

    this.initialisiert = true;
  }

  async anmelden(): Promise<void> {
    await this.initialisieren();
    await this.msal.loginRedirect(entraLoginRequest);
  }

  async abmelden(): Promise<void> {
    await this.initialisieren();
    const konto = this.aktivesKonto();
    if (!konto) return;
    await this.msal.logoutRedirect({ account: konto, postLogoutRedirectUri: window.location.origin });
  }

  async zugriffstoken(interaktiv = false): Promise<string | null> {
    await this.initialisieren();
    const konto = this.aktivesKonto();
    if (!konto) return null;

    try {
      const ergebnis = await this.msal.acquireTokenSilent({ account: konto, scopes: entraScopes });

      // Das signierte ID-Token bestätigt dem Backend die Identität. Seine
      // Zielgruppe ist unsere Entra-App und wird in Quarkus kontrolliert.
      return ergebnis.idToken;
    } catch (fehler) {
      if (interaktiv && fehler instanceof InteractionRequiredAuthError) {
        await this.msal.acquireTokenRedirect({ account: konto, scopes: entraScopes });
      }
      return null;
    }
  }

  istAngemeldet(): boolean {
    return !!this.aktivesKonto();
  }

  private aktivesKonto(): AccountInfo | null {
    return this.msal.getActiveAccount() ?? this.msal.getAllAccounts()[0] ?? null;
  }
}
