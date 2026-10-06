import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationInitStatus } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { appConfig } from './app.config';
import { AuthService } from './core/auth/auth.service';
import { AppConfigService } from './core/config/app-config';
import { LanguageService } from './core/i18n/language.service';

/**
 * Runs the real application initializer, which unit tests of the services never do. It caught a bug no other test
 * could: inject() called after an await fails with NG0203 and the application never starts.
 */
describe('application initializer', () => {
  const CONFIG = {
    apiBaseUrl: 'http://api.test',
    keycloak: { issuer: 'http://keycloak.test/realms/ledgerai', clientId: 'ledgerai-frontend' },
  };
  const oauth = {
    events: new Subject<unknown>(),
    state: undefined,
    configure: vi.fn(),
    loadDiscoveryDocumentAndTryLogin: vi.fn().mockResolvedValue(true),
    setupAutomaticSilentRefresh: vi.fn(),
    hasValidAccessToken: () => false,
    getAccessToken: () => '',
  };

  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => CONFIG }));
    TestBed.configureTestingModule({
      providers: [
        ...appConfig.providers,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OAuthService, useValue: oauth },
      ],
    });
  });

  afterEach(() => vi.unstubAllGlobals());

  it('loads the configuration, then the language, then signs in, and the application starts', async () => {
    await TestBed.inject(ApplicationInitStatus).donePromise;

    expect(TestBed.inject(AppConfigService).value.apiBaseUrl).toBe('http://api.test');
    expect(TestBed.inject(LanguageService).current()).toMatch(/^(en|fr)$/);
    expect(oauth.configure).toHaveBeenCalledWith(
      expect.objectContaining({ issuer: CONFIG.keycloak.issuer, clientId: 'ledgerai-frontend' }),
    );
    expect(TestBed.inject(AuthService).isAuthenticated()).toBe(false);
  });

  it('fails the startup, instead of running half configured, when config.json is missing', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 404 }));

    await expect(TestBed.inject(ApplicationInitStatus).donePromise).rejects.toThrow(/HTTP 404/);
  });
});
