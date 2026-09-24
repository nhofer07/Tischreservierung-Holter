import { Configuration, RedirectRequest } from '@azure/msal-browser';

// Client- und Tenant-ID sind öffentliche Kennungen der Entra-App.
// Ein Client-Secret darf niemals im Angular-Frontend gespeichert werden.
export const entraClientId = '8fe2707e-b93a-4657-ad94-d0440a067b8b';
export const entraTenantId = '14ddf41a-9afb-4810-bc22-227e34a9a007';
export const entraScopes = ['openid', 'profile', 'email', 'User.Read'];

export const msalConfig: Configuration = {
  auth: {
    clientId: entraClientId,
    authority: `https://login.microsoftonline.com/${entraTenantId}`,
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin
  },
  cache: {
    cacheLocation: 'sessionStorage'
  }
};

export const entraLoginRequest: RedirectRequest = {
  // Diese Berechtigungen entsprechen genau dem gelieferten Entra-Beispiel.
  scopes: entraScopes,
  prompt: 'select_account'
};

// Der lokale Login bleibt nur für Entwicklung und Schulpräsentationen vorhanden.
export const lokalerTestLoginAktiv = ['localhost', '127.0.0.1'].includes(window.location.hostname);
