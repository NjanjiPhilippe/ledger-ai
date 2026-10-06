import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { AppConfigService } from '../config/app-config';
import { LanguageService } from '../i18n/language.service';
import { decodeJwtPayload, isRole, realmRoles, Role } from './roles';

/**
 * Sign-in with Keycloak: Authorization Code flow with PKCE (client "ledgerai-frontend"). Tokens are kept by
 * angular-oauth2-oidc in sessionStorage (its default), and the access token is the only thing sent to the API.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly router = inject(Router);
  private readonly config = inject(AppConfigService);
  private readonly language = inject(LanguageService);

  private readonly accessToken = signal<string | null>(null);

  readonly isAuthenticated = computed(() => this.accessToken() !== null);
  readonly roles = computed(() => realmRoles(decodeJwtPayload(this.accessToken())));
  /** The application's own roles: Keycloak also puts technical ones (offline_access, default-roles-…) in the token. */
  readonly appRoles = computed<Role[]>(() =>
    this.roles()
      .map((role) => role.toLowerCase())
      .filter(isRole),
  );
  readonly username = computed(() => {
    const name = decodeJwtPayload(this.accessToken())?.['preferred_username'];
    return typeof name === 'string' ? name : null;
  });

  /** Runs at startup: configures the client, completes a sign-in in progress, and keeps the token fresh. */
  async initialize(): Promise<void> {
    const { issuer, clientId } = this.config.value.keycloak;
    const origin = window.location.origin;
    this.oauth.configure({
      issuer,
      clientId,
      redirectUri: `${origin}/`,
      postLogoutRedirectUri: `${origin}/`,
      responseType: 'code',
      scope: 'openid profile',
      requireHttps: 'remoteOnly',
      clearHashAfterLogin: true,
    });
    this.oauth.events.subscribe(() => this.refreshState());

    await this.oauth.loadDiscoveryDocumentAndTryLogin();
    this.refreshState();
    if (this.isAuthenticated()) {
      this.oauth.setupAutomaticSilentRefresh();
      await this.restoreReturnUrl();
    }
  }

  /** The sign-in page of Keycloak opens in the language chosen in the application (OIDC "ui_locales"). */
  login(returnUrl = '/'): void {
    this.oauth.initCodeFlow(encodeURIComponent(returnUrl), { ui_locales: this.language.current() });
  }

  logout(): void {
    this.oauth.logOut();
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  private refreshState(): void {
    this.accessToken.set(this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null);
  }

  private async restoreReturnUrl(): Promise<void> {
    const state = this.oauth.state;
    if (!state) {
      return;
    }
    const target = decodeURIComponent(state);
    // Only in-app paths: the state comes back through the URL and must not become an open redirect.
    if (target.startsWith('/') && !target.startsWith('//')) {
      await this.router.navigateByUrl(target);
    }
  }
}
