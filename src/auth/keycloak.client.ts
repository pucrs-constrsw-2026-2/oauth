import { Injectable } from '@nestjs/common';
import { KeycloakDependencyError } from '../common/errors';

export interface TokenResponse {
  token_type: string;
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  refresh_expires_in?: number;
}

@Injectable()
export class KeycloakClient {
  private readonly baseUrl = process.env.KEYCLOAK_URL ?? 'http://localhost:8080';
  private readonly realm = process.env.KEYCLOAK_REALM ?? 'closed-cras';
  private readonly timeoutMs = Number(process.env.KEYCLOAK_TIMEOUT_MS ?? 5000);

  async login(username: string, password: string): Promise<TokenResponse> {
    return this.requestToken(new URL(`/realms/${this.realm}/protocol/openid-connect/token`, this.baseUrl), new URLSearchParams({
      grant_type: 'password',
      client_id: process.env.KEYCLOAK_CLIENT_ID ?? 'bff',
      client_secret: process.env.KEYCLOAK_CLIENT_SECRET ?? '',
      username,
      password,
    }));
  }

  async refresh(refreshToken: string): Promise<TokenResponse> {
    return this.requestToken(new URL(`/realms/${this.realm}/protocol/openid-connect/token`, this.baseUrl), new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: process.env.KEYCLOAK_CLIENT_ID ?? 'bff',
      client_secret: process.env.KEYCLOAK_CLIENT_SECRET ?? '',
      refresh_token: refreshToken,
    }));
  }

  private async requestToken(url: URL, body: URLSearchParams): Promise<TokenResponse> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const response = await fetch(url, { method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' }, body, signal: controller.signal });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new KeycloakDependencyError(response.status === 401 ? 'invalid_credentials' : 'upstream_rejected', response.status);
      }
      return payload as TokenResponse;
    } catch (error) {
      if (error instanceof KeycloakDependencyError) throw error;
      throw new KeycloakDependencyError('unavailable');
    } finally {
      clearTimeout(timeout);
    }
  }
}