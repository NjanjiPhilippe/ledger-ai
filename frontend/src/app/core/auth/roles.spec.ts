import { fakeJwt } from '../../../testing/jwt';
import { decodeJwtPayload, hasRole, isRole, realmRoles } from './roles';

describe('hasRole', () => {
  it('follows the backend hierarchy: admin implies accountant implies viewer', () => {
    expect(hasRole(['admin'], 'viewer')).toBe(true);
    expect(hasRole(['admin'], 'accountant')).toBe(true);
    expect(hasRole(['accountant'], 'viewer')).toBe(true);
    expect(hasRole(['accountant'], 'admin')).toBe(false);
    expect(hasRole(['viewer'], 'accountant')).toBe(false);
  });

  it("is case-insensitive and ignores roles that are not the application's", () => {
    expect(hasRole(['ACCOUNTANT'], 'accountant')).toBe(true);
    expect(hasRole(['offline_access', 'default-roles-ledgerai'], 'viewer')).toBe(false);
    expect(hasRole([], 'viewer')).toBe(false);
  });

  it('isRole narrows only the three application roles', () => {
    expect(['viewer', 'accountant', 'admin', 'uma_authorization'].filter(isRole)).toEqual([
      'viewer',
      'accountant',
      'admin',
    ]);
  });
});

describe('decodeJwtPayload', () => {
  it('reads the claims, including non-ASCII ones (base64url, UTF-8)', () => {
    const token = fakeJwt({ preferred_username: 'amélie', sub: 'x' });

    expect(decodeJwtPayload(token)).toMatchObject({ preferred_username: 'amélie' });
  });

  it.each([null, '', 'not-a-jwt', 'a.%%%.c', `a.${btoa('"just a string"')}.c`])(
    'returns null for %j instead of throwing',
    (token) => {
      expect(decodeJwtPayload(token)).toBeNull();
    },
  );
});

describe('realmRoles', () => {
  it('reads realm_access.roles', () => {
    expect(realmRoles({ realm_access: { roles: ['viewer', 'offline_access'] } })).toEqual([
      'viewer',
      'offline_access',
    ]);
  });

  it.each([null, {}, { realm_access: {} }, { realm_access: { roles: 'admin' } }])(
    'gives no roles for %j',
    (payload) => {
      expect(realmRoles(payload)).toEqual([]);
    },
  );

  it('keeps only the string entries', () => {
    expect(realmRoles({ realm_access: { roles: ['viewer', 3, null] } })).toEqual(['viewer']);
  });
});
