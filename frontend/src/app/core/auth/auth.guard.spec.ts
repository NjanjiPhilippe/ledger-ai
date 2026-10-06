import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, roleGuard } from './auth.guard';

describe('route guards', () => {
  const authenticated = signal(false);
  const roles = signal<string[]>([]);
  const login = vi.fn();
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/accounts' } as RouterStateSnapshot;

  beforeEach(() => {
    authenticated.set(false);
    roles.set([]);
    login.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: authenticated, roles, login } },
      ],
    });
  });

  const run = (guard: typeof authGuard) => TestBed.runInInjectionContext(() => guard(route, state));

  it('lets a signed-in user through', () => {
    authenticated.set(true);

    expect(run(authGuard)).toBe(true);
    expect(login).not.toHaveBeenCalled();
  });

  it('sends an anonymous user to the sign-in page and remembers where they were going', () => {
    expect(run(authGuard)).toBe(false);
    expect(login).toHaveBeenCalledWith('/accounts');
  });

  it('roleGuard sends an anonymous user to sign in as well', () => {
    expect(run(roleGuard('accountant'))).toBe(false);
    expect(login).toHaveBeenCalledWith('/accounts');
  });

  it('roleGuard lets through a role that is sufficient, through the hierarchy', () => {
    authenticated.set(true);
    roles.set(['admin']);

    expect(run(roleGuard('accountant'))).toBe(true);
  });

  it('roleGuard redirects an insufficient role to the forbidden page', () => {
    authenticated.set(true);
    roles.set(['viewer']);

    const result = run(roleGuard('accountant')) as UrlTree;

    expect(result.toString()).toBe(TestBed.inject(Router).createUrlTree(['/forbidden']).toString());
    expect(login).not.toHaveBeenCalled();
  });
});
