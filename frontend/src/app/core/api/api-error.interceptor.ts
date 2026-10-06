import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { catchError, throwError } from 'rxjs';
import { isApiUrl } from '../auth/auth.interceptor';
import { AuthService } from '../auth/auth.service';
import { AppConfigService } from '../config/app-config';
import { ToastService } from '../toast/toast.service';
import { ApiProblem, toApiProblem } from './api-problem';

/** Set to true on a request whose error the caller shows itself (for example next to a form field). */
export const HANDLE_ERRORS_LOCALLY = new HttpContextToken<boolean>(() => false);

/**
 * Turns every failed call to our API into a translated toast, and rethrows so the caller can still react.
 * The title is translated from the kind of problem; the backend's own message (English) is only added where it
 * is meant for users, never for server errors, which can carry internals.
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const { apiBaseUrl } = inject(AppConfigService).value;
  const toasts = inject(ToastService);
  const transloco = inject(TranslocoService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        isApiUrl(request.url, apiBaseUrl) &&
        !request.context.get(HANDLE_ERRORS_LOCALLY)
      ) {
        const problem = toApiProblem(error);
        toasts.error(transloco.translate(`errors.${problem.kind}.title`), detailOf(problem));
        if (problem.kind === 'unauthorized') {
          auth.login(router.url);
        }
      }
      return throwError(() => error);
    }),
  );
};

const MAX_FIELD_ERRORS_SHOWN = 3;

function detailOf(problem: ApiProblem): string | undefined {
  switch (problem.kind) {
    case 'validation':
      return problem.fieldErrors.length > 0
        ? problem.fieldErrors
            .slice(0, MAX_FIELD_ERRORS_SHOWN)
            .map((e) => `${e.field}: ${e.message}`)
            .join(' · ')
        : (problem.message ?? undefined);
    case 'conflict':
    case 'gateway':
    case 'notFound':
      return problem.message ?? undefined;
    default:
      return undefined;
  }
}
