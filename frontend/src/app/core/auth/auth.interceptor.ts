import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AppConfigService } from '../config/app-config';
import { AuthService } from './auth.service';

/** Adds the access token to calls to our API, and only to those: it must never leak to other hosts. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const { apiBaseUrl } = inject(AppConfigService).value;
  const token = inject(AuthService).getAccessToken();
  if (token && isApiUrl(request.url, apiBaseUrl)) {
    return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(request);
};

export function isApiUrl(url: string, apiBaseUrl: string): boolean {
  return url === apiBaseUrl || url.startsWith(`${apiBaseUrl}/`);
}
