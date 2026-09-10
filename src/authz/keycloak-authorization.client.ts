import { HttpStatus, Injectable, Logger } from '@nestjs/common';

import { KeycloakSettingsService } from '../config';
import { OaErrorMapper } from '../errors';

/** Node's fetch has no timeout by default; fail fast instead of hanging a request. */
export const PERMISSION_CHECK_TIMEOUT_MS = 8_000;

/** RFC-defined grant type Keycloak uses for Authorization Services decisions. */
export const UMA_TICKET_GRANT_TYPE =
  'urn:ietf:params:oauth:grant-type:uma-ticket';

/**
 * Asks Keycloak Authorization Services whether a caller's token grants access
 * to a named resource, using the UMA-ticket grant on the standard token
 * endpoint. Keycloak is the **decision engine** — this client holds no local
 * role-to-resource table (Story 6.5, CAP-6).
 */
@Injectable()
export class KeycloakAuthorizationClient {
  private readonly logger = new Logger(KeycloakAuthorizationClient.name);

  constructor(private readonly keycloak: KeycloakSettingsService) {}

  /** Resolves `true` when Keycloak permits access, `false` when it denies it. */
  async checkPermission(accessToken: string, resource: string): Promise<boolean> {
    const form = new URLSearchParams({
      grant_type: UMA_TICKET_GRANT_TYPE,
      audience: this.keycloak.clientId,
      permission: resource,
    });

    let response: Response;
    try {
      response = await fetch(this.keycloak.tokenUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          Authorization: `Bearer ${accessToken}`,
        },
        body: form.toString(),
        signal: AbortSignal.timeout(PERMISSION_CHECK_TIMEOUT_MS),
      });
    } catch (cause) {
      this.logger.error('Keycloak authorization endpoint unreachable', cause as Error);
      throw OaErrorMapper.fromKeycloak(
        HttpStatus.SERVICE_UNAVAILABLE,
        'Could not reach Keycloak to evaluate this permission.',
        { error: 'keycloak_unreachable' },
      );
    }

    if (response.ok) {
      return true;
    }

    if (response.status === HttpStatus.FORBIDDEN) {
      return false;
    }

    // Any other status (e.g. Keycloak itself erroring) is not a permission
    // decision — do not silently treat it as allow or deny.
    const payload = await safeJson(response);
    const description =
      extractString(payload, 'error_description') ??
      `Keycloak returned an unexpected status (${response.status}) while evaluating this permission.`;
    throw OaErrorMapper.fromKeycloak(HttpStatus.SERVICE_UNAVAILABLE, description, payload);
  }
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
