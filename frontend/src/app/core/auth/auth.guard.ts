import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { hasRole, Role } from './roles';

/** Signed-in users only: anyone else is sent to Keycloak and brought back to the page they asked for. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return true;
  }
  auth.login(state.url);
  return false;
};

/** Requires a role (or a higher one). This is a convenience for the UI: the backend enforces roles. */
export const roleGuard =
  (required: Role): CanActivateFn =>
  (_route, state) => {
    const auth = inject(AuthService);
    if (!auth.isAuthenticated()) {
      auth.login(state.url);
      return false;
    }
    return hasRole(auth.roles(), required) ? true : inject(Router).createUrlTree(['/forbidden']);
  };
