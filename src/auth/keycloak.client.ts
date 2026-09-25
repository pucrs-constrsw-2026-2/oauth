import { Injectable, Optional } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { KeycloakDependencyError } from "../common/errors";
import { keycloakJson, requestToken } from "../common/keycloak-http";

export type { TokenResponse } from "../common/keycloak-http";

export interface KeycloakUser {
  id: string;
  username: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
}

@Injectable()
export class KeycloakClient {
  private readonly baseUrl: string;
  private readonly realm: string;
  private readonly timeoutMs: number;
  private readonly clientId: string;
  private readonly clientSecret: string;

  constructor(@Optional() config?: ConfigService) {
    this.baseUrl =
      this.value(config, "KEYCLOAK_URL") ?? "http://localhost:8080";
    this.realm = this.value(config, "KEYCLOAK_REALM") ?? "closed-cras";
    this.timeoutMs = Number(this.value(config, "KEYCLOAK_TIMEOUT_MS") ?? 5000);
    this.clientId = this.value(config, "KEYCLOAK_CLIENT_ID") ?? "bff";
    this.clientSecret = this.value(config, "KEYCLOAK_CLIENT_SECRET") ?? "";
  }

  async login(username: string, password: string) {
    return requestToken(
      this.tokenUrl(),
      new URLSearchParams({
        grant_type: "password",
        client_id: this.clientId,
        client_secret: this.clientSecret,
        username,
        password,
      }),
      this.timeoutMs,
    );
  }

  async refresh(refreshToken: string) {
    return requestToken(
      this.tokenUrl(),
      new URLSearchParams({
        grant_type: "refresh_token",
        client_id: this.clientId,
        client_secret: this.clientSecret,
        refresh_token: refreshToken,
      }),
      this.timeoutMs,
    );
  }

  async listUsers(
    accessToken: string,
    enabled?: boolean,
  ): Promise<KeycloakUser[]> {
    const url = new URL(`/admin/realms/${this.realm}/users`, this.baseUrl);
    if (enabled !== undefined) url.searchParams.set("enabled", String(enabled));
    try {
      return (
        (await keycloakJson<KeycloakUser[]>(url, {
          token: accessToken,
          timeoutMs: this.timeoutMs,
        })) ?? []
      );
    } catch (error) {
      if (error instanceof KeycloakDependencyError) {
        throw new KeycloakDependencyError(
          "upstream_rejected",
          error.upstreamStatus,
        );
      }
      throw error;
    }
  }

  async getUser(accessToken: string, id: string): Promise<KeycloakUser> {
    let user: KeycloakUser | undefined;
    try {
      user = await keycloakJson<KeycloakUser>(
        new URL(
          `/admin/realms/${this.realm}/users/${encodeURIComponent(id)}`,
          this.baseUrl,
        ),
        { token: accessToken, timeoutMs: this.timeoutMs },
      );
    } catch (error) {
      if (error instanceof KeycloakDependencyError) {
        throw new KeycloakDependencyError(
          "upstream_rejected",
          error.upstreamStatus,
        );
      }
      throw error;
    }
    if (!user) throw new KeycloakDependencyError("not_found", 404);
    return user;
  }

  private tokenUrl(): URL {
    return new URL(
      `/realms/${this.realm}/protocol/openid-connect/token`,
      this.baseUrl,
    );
  }

  private value(
    config: ConfigService | undefined,
    key: string,
  ): string | undefined {
    if (config && typeof (config as ConfigService).getOrThrow === "function") {
      const value = config.getOrThrow<string | number>(key);
      return value === undefined ? undefined : String(value);
    }
    return process.env[key];
  }
}
