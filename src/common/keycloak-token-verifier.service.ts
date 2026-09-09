import { Injectable, Logger } from '@nestjs/common';

import { KeycloakSettingsService } from '../config';

/**
 * Node's fetch does not time out on its own, so a hung Keycloak would hold
 * every request open for minutes. Failing fast turns that into a 503.
 */
export const USER_INFO_TIMEOUT_MS = 5_000;

/** Subset of the OIDC UserInfo response the API relies on. */
export interface AuthenticatedUser {
  readonly sub: string;
  readonly email?: string;
  readonly preferredUsername?: string;
  readonly name?: string;
  readonly raw: Record<string, unknown>;
}

/**
 * Verifies a caller's access token by asking Keycloak, rather than validating
 * the JWT locally. Keycloak stays the authority on whether a token is still
 * valid, so a revoked or logged-out token is rejected immediately — local
 * signature checking would keep accepting it until expiry.
 */
@Injectable()
export class KeycloakTokenVerifierService {
  private readonly logger = new Logger(KeycloakTokenVerifierService.name);

  constructor(private readonly keycloak: KeycloakSettingsService) {}

  /** Resolves the caller for a valid token, or null when Keycloak rejects it. */
  async verify(accessToken: string): Promise<AuthenticatedUser | null> {
    const response = await fetch(this.keycloak.userInfoUrl, {
      method: 'GET',
      headers: { Authorization: `Bearer ${accessToken}` },
      signal: AbortSignal.timeout(USER_INFO_TIMEOUT_MS),
    });

    if (response.status === 401 || response.status === 403) {
      return null;
    }

    if (!response.ok) {
      // Keycloak is unreachable or broken — not the caller's fault, so this
      // must not surface as 401.
      this.logger.error(
        `UserInfo returned ${response.status} ${response.statusText}`,
      );
      throw new Error(
        `Keycloak UserInfo request failed with status ${response.status}`,
      );
    }

    const payload = (await response.json()) as Record<string, unknown>;
    const sub = payload.sub;

    if (typeof sub !== 'string' || sub.length === 0) {
      throw new Error('Keycloak UserInfo response is missing "sub"');
    }

    return {
      sub,
      email: asString(payload.email),
      preferredUsername: asString(payload.preferred_username),
      name: asString(payload.name),
      raw: payload,
    };
  }
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}
