/**
 * Wire shape from the brief. The hyphenated names are deliberate: they are how
 * the brief specifies the payload, and they differ from Keycloak's own
 * `firstName` / `lastName`.
 */
export interface UserRepresentation {
  id: string;
  username: string;
  'first-name'?: string;
  'last-name'?: string;
  enabled: boolean;
}

/** Subset of Keycloak's user representation this service reads or writes. */
export interface KeycloakUser {
  id?: string;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
}

export interface CreateUserBody {
  username?: unknown;
  password?: unknown;
  'first-name'?: unknown;
  'last-name'?: unknown;
}

export interface UpdateUserBody {
  'first-name'?: unknown;
  'last-name'?: unknown;
  enabled?: unknown;
}

export interface ChangePasswordBody {
  password?: unknown;
}

/** Attributes `PUT /users/{id}` accepts; `username` is intentionally absent. */
export const UPDATABLE_USER_FIELDS = ['first-name', 'last-name', 'enabled'] as const;

export function toRepresentation(user: KeycloakUser): UserRepresentation {
  return {
    id: user.id ?? '',
    username: user.username ?? '',
    'first-name': user.firstName,
    'last-name': user.lastName,
    enabled: user.enabled ?? false,
  };
}
