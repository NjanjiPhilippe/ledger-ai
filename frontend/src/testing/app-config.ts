import { AppConfig } from '../app/core/config/app-config';

export const TEST_CONFIG: AppConfig = {
  apiBaseUrl: 'http://api.test',
  keycloak: { issuer: 'http://keycloak.test/realms/ledgerai', clientId: 'ledgerai-frontend' },
};
