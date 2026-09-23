import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { requestToken } from "../common/keycloak-http";

export type { TokenResponse } from "../common/keycloak-http";

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

  private tokenUrl(): URL {
    return new URL(
      `/realms/${this.realm}/protocol/openid-connect/token`,
      this.baseUrl,
    );
  }
}
