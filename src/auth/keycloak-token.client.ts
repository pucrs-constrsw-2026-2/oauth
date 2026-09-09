import { HttpStatus, Injectable, Logger } from '@nestjs/common';

import { KeycloakSettingsService } from '../config';
import { OaErrorMapper } from '../errors';

/** Node's fetch has no timeout by default; fail fast instead of hanging a request. */
export const TOKEN_REQUEST_TIMEOUT_MS = 8_000;

/** Fields this service cares about from a Keycloak token endpoint response. */
export interface KeycloakTokenResult {
  readonly tokenType: string;
  readonly accessToken: string;
  readonly expiresIn: number;
  readonly refreshToken: string;
  /** Only present when Keycloak returns `refresh_expires_in` (NFR5). */
  readonly refreshExpiresIn?: number;
}

/**
 * Talks to Keycloak's OIDC token endpoint on behalf of `/login` (2.1) and
 * `/refresh` (2.2). Client credentials always come from
 * `KeycloakSettingsService` — a caller-supplied `client_id` is never read,
 * used, or logged (SPEC constraint).
 */
@Injectable()
export class KeycloakTokenClient {
  private readonly logger = new Logger(KeycloakTokenClient.name);

  constructor(private readonly keycloak: KeycloakSettingsService) {}

  /** `grant_type=password` — used by `POST /login`. */
  async passwordGrant(username: string, password: string): Promise<KeycloakTokenResult> {
    return this.requestToken({ grant_type: 'password', username, password });
  }

  /** `grant_type=refresh_token` — used by `POST /refresh`. */
  async refreshGrant(refreshToken: string): Promise<KeycloakTokenResult> {
    return this.requestToken({
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
    });
  }

  private async requestToken(
    fields: Record<string, string>,
  ): Promise<KeycloakTokenResult> {
    const form = new URLSearchParams({
      client_id: this.keycloak.clientId,
      client_secret: this.keycloak.clientSecret,
      ...fields,
    });

    let response: Response;
    try {
      response = await fetch(this.keycloak.tokenUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: form.toString(),
        signal: AbortSignal.timeout(TOKEN_REQUEST_TIMEOUT_MS),
      });
    } catch (cause) {
      this.logger.error('Keycloak token endpoint unreachable', cause as Error);
      throw OaErrorMapper.fromKeycloak(
        HttpStatus.UNAUTHORIZED,
        'Could not reach Keycloak to authenticate this request.',
        { error: 'keycloak_unreachable' },
      );
    }

    const payload = await safeJson(response);

    if (!response.ok) {
      const description =
        extractString(payload, 'error_description') ??
        `Keycloak rejected the request (status ${response.status}).`;
      // Structural (missing/blank field, wrong content-type) failures are
      // rejected before this client is ever called — anything Keycloak itself
      // rejects here is a credentials/token problem, so the API answers 401
      // regardless of Keycloak's own raw status (AC #3 of 2.1 / AC #3 of 2.2).
      throw OaErrorMapper.fromKeycloak(HttpStatus.UNAUTHORIZED, description, payload);
    }

    return mapTokenResult(payload);
  }
}

function mapTokenResult(payload: unknown): KeycloakTokenResult {
  const tokenType = extractString(payload, 'token_type');
  const accessToken = extractString(payload, 'access_token');
  const refreshToken = extractString(payload, 'refresh_token');
  const expiresIn = extractNumber(payload, 'expires_in');
  const refreshExpiresIn = extractNumber(payload, 'refresh_expires_in');

  if (!tokenType || !accessToken || !refreshToken || expiresIn === undefined) {
    throw OaErrorMapper.fromKeycloak(
      HttpStatus.UNAUTHORIZED,
      'Keycloak returned an incomplete token response.',
      isRecord(payload) ? payload : { raw: payload },
    );
  }

  return {
    tokenType,
    accessToken,
    expiresIn,
    refreshToken,
    refreshExpiresIn,
  };
}

async function safeJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function extractString(value: unknown, key: string): string | undefined {
  if (!isRecord(value)) {
    return undefined;
  }
  const field = value[key];
  return typeof field === 'string' && field.length > 0 ? field : undefined;
}

function extractNumber(value: unknown, key: string): number | undefined {
  if (!isRecord(value)) {
    return undefined;
  }
  const field = value[key];
  return typeof field === 'number' ? field : undefined;
}
