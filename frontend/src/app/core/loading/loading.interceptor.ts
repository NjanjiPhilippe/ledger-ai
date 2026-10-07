import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { isApiUrl } from '../auth/auth.interceptor';
import { AppConfigService } from '../config/app-config';
import { LoadingService } from './loading.service';

/** Marks the global loading bar busy for as long as a call to our API is in flight (success, error or cancel). */
export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const { apiBaseUrl } = inject(AppConfigService).value;
  if (!isApiUrl(request.url, apiBaseUrl)) {
    return next(request);
  }
  const loading = inject(LoadingService);
  loading.start();
  return next(request).pipe(finalize(() => loading.stop()));
};
