import { Injectable } from '@angular/core';

export interface AppConfig {
  readonly apiBaseUrl: string;
  readonly keycloak: { readonly issuer: string; readonly clientId: string };
}

/**
 * Runtime configuration, loaded once at startup from /config.json instead of being baked into the build:
 * the same build can then run against any environment.
 */
@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private loaded: AppConfig | null = null;

  get value(): AppConfig {
    if (!this.loaded) {
      throw new Error('The application configuration has not been loaded yet');
    }
    return this.loaded;
  }

  async load(fetchFn: typeof fetch = (input, init) => fetch(input, init)): Promise<AppConfig> {
    const response = await fetchFn('config.json', { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`Cannot load config.json (HTTP ${response.status})`);
    }
    this.loaded = parseAppConfig(await response.json());
    return this.loaded;
  }

  /** Test seam: set the configuration without fetching it. */
  set(config: AppConfig): void {
    this.loaded = config;
  }
}

export function parseAppConfig(raw: unknown): AppConfig {
  const config = raw as Partial<AppConfig> | null;
  const apiBaseUrl = config?.apiBaseUrl;
  const issuer = config?.keycloak?.issuer;
  const clientId = config?.keycloak?.clientId;
  if (!isNonBlank(apiBaseUrl) || !isNonBlank(issuer) || !isNonBlank(clientId)) {
    throw new Error('config.json must define apiBaseUrl, keycloak.issuer and keycloak.clientId');
  }
  return {
    apiBaseUrl: apiBaseUrl.replace(/\/+$/, ''),
    keycloak: { issuer, clientId },
  };
}

function isNonBlank(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}
