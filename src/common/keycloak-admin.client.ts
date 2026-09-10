import { Injectable } from '@nestjs/common';

import { KeycloakSettingsService } from '../config';
import { OaErrorMapper, OaException } from '../errors';

export const KEYCLOAK_REQUEST_TIMEOUT_MS = 5_000;

/** Refresh this many seconds before the token actually expires. */
const EXPIRY_MARGIN_SECONDS = 10;

/** Used when Keycloak omits `expires_in`. */
const FALLBACK_TTL_SECONDS = 60;

interface CachedToken {
  readonly token: string;
  readonly expiresAt: number;
}

/**
 * Talks to the Keycloak Admin REST API on behalf of the service, not the
 * caller: every request authenticates with `KEYCLOAK_ADMIN` credentials, so a
 * caller's access token is never forwarded upstream.
 *
 * Shared by the roles and users modules — both need the same token handling,
 * timeout and failure mapping, and duplicating it would let the two drift.
 */
@Injectable()
export class KeycloakAdminClient {
  /** Admin credentials live in the `master` realm, not the app realm. */
  private readonly adminRealm = 'master';

  private cached?: CachedToken;

  /** In-flight token request, shared so concurrent calls fetch only once. */
  private pending?: Promise<string>;

  constructor(private readonly settings: KeycloakSettingsService) {}

  /** Authenticated call against `{adminRealmUrl}{path}`. */
  async request(path: string, init: RequestInit = {}): Promise<Response> {
    const token = await this.adminToken();

    try {
      return await fetch(`${this.settings.adminRealmUrl}${path}`, {
        ...init,
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
          ...(init.headers ?? {}),
        },
        signal: init.signal ?? AbortSignal.timeout(KEYCLOAK_REQUEST_TIMEOUT_MS),
      });
    } catch {
      throw this.upstream(503, 'Keycloak Admin API is unavailable.');
    }
  }

  /** Raises the caller-facing failure for a non-2xx Admin API response. */
  async expectSuccess(response: Response, description: string): Promise<void> {
    if (!response.ok) {
      throw this.upstream(response.status === 403 ? 403 : 502, description);
    }
  }

  async json<T = any>(response: Response, description: string): Promise<T> {
    try {
      return (await response.json()) as T;
    } catch {
      throw this.upstream(502, description);
    }
  }

  /** Failure reaching or understanding Keycloak, in the shared OA envelope. */
  upstream(status: number, description: string): OaException {
    return OaErrorMapper.fromStatus(status, description);
  }

  /** Drops the cached token so the next call authenticates again. */
  invalidateToken(): void {
    this.cached = undefined;
    this.pending = undefined;
  }

  private async adminToken(): Promise<string> {
    if (this.cached && Date.now() < this.cached.expiresAt) {
      return this.cached.token;
    }

    // Without this, a burst of concurrent requests would each open its own
    // token request against Keycloak.
    this.pending ??= this.fetchAdminToken().finally(() => {
      this.pending = undefined;
    });

    return this.pending;
  }

  private async fetchAdminToken(): Promise<string> {
    const { adminUser, adminPassword } = this.settings;

    if (!adminUser || !adminPassword) {
      throw this.upstream(503, 'Keycloak admin credentials are not configured.');
    }

    const body = new URLSearchParams({
      client_id: 'admin-cli',
      grant_type: 'password',
      username: adminUser,
      password: adminPassword,
    });

    let response: Response;
    try {
      response = await fetch(
        `${this.settings.serverUrl}/realms/${this.adminRealm}/protocol/openid-connect/token`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body,
          signal: AbortSignal.timeout(KEYCLOAK_REQUEST_TIMEOUT_MS),
        },
      );
    } catch {
      throw this.upstream(503, 'Keycloak authentication is unavailable.');
    }

    if (!response.ok) {
      throw this.upstream(502, 'Keycloak admin authentication failed.');
    }

    const payload = await this.json<{
      access_token?: unknown;
      expires_in?: unknown;
    }>(response, 'Could not decode the admin token response.');

    if (typeof payload.access_token !== 'string' || !payload.access_token) {
      throw this.upstream(502, 'Keycloak did not return an admin access token.');
    }

    const ttl =
      typeof payload.expires_in === 'number' && payload.expires_in > 0
        ? payload.expires_in
        : FALLBACK_TTL_SECONDS;

    this.cached = {
      token: payload.access_token,
      expiresAt:
        Date.now() + Math.max(ttl - EXPIRY_MARGIN_SECONDS, 1) * 1000,
    };

    return payload.access_token;
  }
}
