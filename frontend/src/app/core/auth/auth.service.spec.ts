import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { fakeJwt } from '../../../testing/jwt';
import { TEST_CONFIG } from '../../../testing/app-config';
import { AppConfigService } from '../config/app-config';
import { AuthService } from './auth.service';

class FakeOAuthService {
  events = new Subject<unknown>();
  state: string | undefined;
  token: string | null = null;
  configure = vi.fn();
  loadDiscoveryDocumentAndTryLogin = vi.fn().mockResolvedValue(true);
  setupAutomaticSilentRefresh = vi.fn();
  initCodeFlow = vi.fn();
  logOut = vi.fn();
  hasValidAccessToken = () => this.token !== null;
  getAccessToken = () => this.token ?? '';
}

describe('AuthService', () => {
  let oauth: FakeOAuthService;
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };
  let service: AuthService;

  beforeEach(() => {
    oauth = new FakeOAuthService();
    router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: oauth },
        { provide: Router, useValue: router },
      ],
    });
    TestBed.inject(AppConfigService).set(TEST_CONFIG);
    service = TestBed.inject(AuthService);
  });

  it('configures the Authorization Code flow (PKCE) for the frontend client of the realm', async () => {
    await service.initialize();

    expect(oauth.configure).toHaveBeenCalledWith(
      expect.objectContaining({
        issuer: TEST_CONFIG.keycloak.issuer,
        clientId: 'ledgerai-frontend',
        responseType: 'code',
        redirectUri: `${window.location.origin}/`,
      }),
    );
  });

  it('is not authenticated, and does not schedule refreshes, without a valid token', async () => {
    await service.initialize();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.roles()).toEqual([]);
    expect(service.username()).toBeNull();
    expect(oauth.setupAutomaticSilentRefresh).not.toHaveBeenCalled();
  });

  it('exposes the user, the roles and only the application roles once signed in', async () => {
    oauth.token = fakeJwt({
      preferred_username: 'accountant',
      realm_access: { roles: ['ACCOUNTANT', 'offline_access', 'default-roles-ledgerai'] },
    });

    await service.initialize();

    expect(service.isAuthenticated()).toBe(true);
    expect(service.username()).toBe('accountant');
    expect(service.roles()).toContain('offline_access');
    expect(service.appRoles()).toEqual(['accountant']);
    expect(service.getAccessToken()).toBe(oauth.token);
    expect(oauth.setupAutomaticSilentRefresh).toHaveBeenCalled();
  });

  it('follows the token life cycle events (refresh, expiry, logout)', async () => {
    await service.initialize();
    expect(service.isAuthenticated()).toBe(false);

    oauth.token = fakeJwt({ realm_access: { roles: ['viewer'] } });
    oauth.events.next({ type: 'token_received' });
    expect(service.isAuthenticated()).toBe(true);

    oauth.token = null;
    oauth.events.next({ type: 'logout' });
    expect(service.isAuthenticated()).toBe(false);
  });

  it('brings the user back to the page they asked for after signing in', async () => {
    oauth.token = fakeJwt({ realm_access: { roles: ['viewer'] } });
    oauth.state = encodeURIComponent('/accounts?page=2');

    await service.initialize();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/accounts?page=2');
  });

  it.each(['https://evil.example/phish', '//evil.example', 'javascript:alert(1)'])(
    'never turns the returned state %s into a redirect',
    async (target) => {
      oauth.token = fakeJwt({ realm_access: { roles: ['viewer'] } });
      oauth.state = encodeURIComponent(target);

      await service.initialize();

      expect(router.navigateByUrl).not.toHaveBeenCalled();
    },
  );

  it('starts the code flow remembering the page to return to, and delegates the logout', () => {
    service.login('/accounts?x=1&y=2');
    service.logout();

    expect(oauth.initCodeFlow).toHaveBeenCalledWith(encodeURIComponent('/accounts?x=1&y=2'));
    expect(oauth.logOut).toHaveBeenCalled();
  });
});
