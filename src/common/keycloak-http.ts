import { KeycloakDependencyError } from "./errors";

export interface TokenResponse {
  token_type: string;
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  refresh_expires_in?: number;
}

async function withTimeout<T>(
  timeoutMs: number,
  run: (signal: AbortSignal) => Promise<T>,
): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await run(controller.signal);
  } finally {
    clearTimeout(timeout);
  }
}

/**
 * Requests an OAuth token from Keycloak (password / refresh / client_credentials
 * grants). Extracted from the original KeycloakClient so both the login flow and
 * the Admin API client share the same timeout + error-mapping behaviour.
 */
export async function requestToken(
  url: URL,
  body: URLSearchParams,
  timeoutMs: number,
): Promise<TokenResponse> {
  try {
    return await withTimeout(timeoutMs, async (signal) => {
      const response = await fetch(url, {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body,
        signal,
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new KeycloakDependencyError(
          response.status === 401 ? "invalid_credentials" : "upstream_rejected",
          response.status,
        );
      }
      return payload as TokenResponse;
    });
  } catch (error) {
    if (error instanceof KeycloakDependencyError) throw error;
    throw new KeycloakDependencyError("unavailable");
  }
}
