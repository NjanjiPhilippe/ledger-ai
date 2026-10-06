export type Role = 'viewer' | 'accountant' | 'admin';

/** Same hierarchy as the backend: admin implies accountant, which implies viewer. */
const IMPLIED_ROLES: Record<Role, readonly Role[]> = {
  admin: ['admin', 'accountant', 'viewer'],
  accountant: ['accountant', 'viewer'],
  viewer: ['viewer'],
};

export function hasRole(userRoles: readonly string[], required: Role): boolean {
  return userRoles.some((raw) => {
    const role = raw.toLowerCase();
    return isRole(role) && IMPLIED_ROLES[role].includes(required);
  });
}

export function isRole(value: string): value is Role {
  return value === 'viewer' || value === 'accountant' || value === 'admin';
}

/** Reads the claims of a JWT without verifying it: the backend is the one that validates tokens. */
export function decodeJwtPayload(token: string | null): Record<string, unknown> | null {
  const payload = token?.split('.')[1];
  if (!payload) {
    return null;
  }
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = new TextDecoder().decode(Uint8Array.from(atob(padded), (c) => c.charCodeAt(0)));
    const parsed: unknown = JSON.parse(json);
    return typeof parsed === 'object' && parsed !== null
      ? (parsed as Record<string, unknown>)
      : null;
  } catch {
    return null;
  }
}

/** Keycloak puts the realm roles in the access token under realm_access.roles. */
export function realmRoles(payload: Record<string, unknown> | null): string[] {
  const realmAccess = payload?.['realm_access'] as { roles?: unknown } | undefined;
  const roles = realmAccess?.roles;
  return Array.isArray(roles) ? roles.filter((r): r is string => typeof r === 'string') : [];
}
