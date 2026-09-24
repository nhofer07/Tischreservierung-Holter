import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { EntraAuthService } from './entra-auth.service';

export const entraInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('http://127.0.0.1:8080/api')) return next(request);

  const auth = inject(EntraAuthService);
  return from(auth.zugriffstoken()).pipe(
    switchMap((token) => next(token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request))
  );
};
