import { AppConfigService, parseAppConfig } from './app-config';

const VALID = {
  apiBaseUrl: 'http://localhost:8085/',
  keycloak: { issuer: 'http://localhost:8180/realms/ledgerai', clientId: 'ledgerai-frontend' },
};

describe('parseAppConfig', () => {
  it('accepts a complete configuration and drops the trailing slash of the API URL', () => {
    expect(parseAppConfig(VALID).apiBaseUrl).toBe('http://localhost:8085');
  });

  it.each([
    ['null', null],
    ['an empty object', {}],
    ['a blank API URL', { ...VALID, apiBaseUrl: '  ' }],
    ['no issuer', { ...VALID, keycloak: { clientId: 'x' } }],
    ['no client id', { ...VALID, keycloak: { issuer: 'http://x' } }],
  ])('rejects %s with a message that names what is missing', (_label, raw) => {
    expect(() => parseAppConfig(raw)).toThrow(
      /apiBaseUrl, keycloak\.issuer and keycloak\.clientId/,
    );
  });
});

describe('AppConfigService', () => {
  it('refuses to be read before it is loaded', () => {
    expect(() => new AppConfigService().value).toThrow(/not been loaded/);
  });

  it('loads config.json once and exposes it', async () => {
    const service = new AppConfigService();
    const fetchFn = vi.fn().mockResolvedValue({ ok: true, json: async () => VALID });

    await service.load(fetchFn as unknown as typeof fetch);

    expect(fetchFn).toHaveBeenCalledWith('config.json', { cache: 'no-store' });
    expect(service.value.keycloak.clientId).toBe('ledgerai-frontend');
  });

  it('fails loudly when config.json cannot be fetched', async () => {
    const fetchFn = vi.fn().mockResolvedValue({ ok: false, status: 404 });

    await expect(new AppConfigService().load(fetchFn as unknown as typeof fetch)).rejects.toThrow(
      /HTTP 404/,
    );
  });
});
