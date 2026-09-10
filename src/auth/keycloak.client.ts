import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { KeycloakDependencyError } from "../common/errors";

export interface TokenResponse {
  token_type: string;
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  refresh_expires_in?: number;
}

@Injectable()
export class KeycloakClient {
  private readonly baseUrl: string;
  private readonly realm: string;
  private readonly timeoutMs: number;
  private readonly clientId: string;
  private readonly clientSecret: string;

  constructor(private readonly config: ConfigService) {
    this.baseUrl = this.config.getOrThrow<string>("KEYCLOAK_URL");
    this.realm = this.config.getOrThrow<string>("KEYCLOAK_REALM");
    this.timeoutMs = this.config.getOrThrow<number>("KEYCLOAK_TIMEOUT_MS");
    this.clientId = this.config.getOrThrow<string>("KEYCLOAK_CLIENT_ID");
    this.clientSecret = this.config.getOrThrow<string>(
      "KEYCLOAK_CLIENT_SECRET",
    );
  }

  async login(username: string, password: string): Promise<TokenResponse> {
    return this.requestToken(
      new URL(
        `/realms/${this.realm}/protocol/openid-connect/token`,
        this.baseUrl,
      ),
      new URLSearchParams({
        grant_type: "password",
        client_id: this.clientId,
        client_secret: this.clientSecret,
        username,
        password,
      }),
    );
  }

  async refresh(refreshToken: string): Promise<TokenResponse> {
    return this.requestToken(
      new URL(
        `/realms/${this.realm}/protocol/openid-connect/token`,
        this.baseUrl,
      ),
      new URLSearchParams({
        grant_type: "refresh_token",
        client_id: this.clientId,
        client_secret: this.clientSecret,
        refresh_token: refreshToken,
      }),
    );
  }

  private async requestToken(
    url: URL,
    body: URLSearchParams,
  ): Promise<TokenResponse> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body,
        signal: controller.signal,
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new KeycloakDependencyError(
          response.status === 401 ? "invalid_credentials" : "upstream_rejected",
          response.status,
        );
      }
      return payload as TokenResponse;
    } catch (error) {
      if (error instanceof KeycloakDependencyError) throw error;
      throw new KeycloakDependencyError("unavailable");
    } finally {
      clearTimeout(timeout);
    }
  }
}
