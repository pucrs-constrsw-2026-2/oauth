import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { KeycloakDependencyError } from "../common/errors";
import { keycloakJson, requestToken } from "../common/keycloak-http";

/**
 * Subset of the Keycloak realm-role representation we rely on.
 */
export interface KeycloakRole {
  id: string;
  name: string;
  description?: string;
  composite?: boolean;
  clientRole?: boolean;
  containerId?: string;
  attributes?: Record<string, string[]>;
}

export interface RealmRoleRef {
  id: string;
  name: string;
}

/**
 * Thin client over the Keycloak Admin REST API for realm roles and
 * user role-mappings. Shares the timeout/error-mapping helpers with the login
 * client (`common/keycloak-http.ts`) and caches the admin token in memory.
 */
@Injectable()
export class KeycloakAdminClient {
  private readonly baseUrl: string;
  private readonly realm: string;
  private readonly timeoutMs: number;
  private readonly adminClientId: string;
  private readonly adminClientSecret: string;

  private cachedToken?: { accessToken: string; expiresAt: number };

  constructor(private readonly config: ConfigService) {
    this.baseUrl = this.config.getOrThrow<string>("KEYCLOAK_URL");
    this.realm = this.config.getOrThrow<string>("KEYCLOAK_REALM");
    this.timeoutMs = this.config.getOrThrow<number>("KEYCLOAK_TIMEOUT_MS");
    this.adminClientId = this.config.getOrThrow<string>(
      "KEYCLOAK_ADMIN_CLIENT_ID",
    );
    this.adminClientSecret = this.config.getOrThrow<string>(
      "KEYCLOAK_ADMIN_CLIENT_SECRET",
    );
  }

  async listRoles(): Promise<KeycloakRole[]> {
    const token = await this.token();
    // briefRepresentation=false so `attributes` (incl. our logical-delete flag)
    // come back — Keycloak's list endpoint omits them by default.
    return (
      (await keycloakJson<KeycloakRole[]>(
        this.adminUrl("/roles?briefRepresentation=false"),
        { token, timeoutMs: this.timeoutMs },
      )) ?? []
    );
  }

  async getRoleById(id: string): Promise<KeycloakRole> {
    const token = await this.token();
    const role = await keycloakJson<KeycloakRole>(
      this.adminUrl(`/roles-by-id/${encodeURIComponent(id)}`),
      { token, timeoutMs: this.timeoutMs },
    );
    if (!role) throw new KeycloakDependencyError("not_found", 404);
    return role;
  }

  async getRoleByName(name: string): Promise<KeycloakRole> {
    const token = await this.token();
    const role = await keycloakJson<KeycloakRole>(
      this.adminUrl(`/roles/${encodeURIComponent(name)}`),
      { token, timeoutMs: this.timeoutMs },
    );
    if (!role) throw new KeycloakDependencyError("not_found", 404);
    return role;
  }

  async createRole(role: {
    name: string;
    description?: string;
    attributes?: Record<string, string[]>;
  }): Promise<void> {
    const token = await this.token();
    await keycloakJson(this.adminUrl("/roles"), {
      method: "POST",
      token,
      body: role,
      timeoutMs: this.timeoutMs,
    });
  }

  async updateRoleById(
    id: string,
    role: Partial<KeycloakRole>,
  ): Promise<void> {
    const token = await this.token();
    await keycloakJson(this.adminUrl(`/roles-by-id/${encodeURIComponent(id)}`), {
      method: "PUT",
      token,
      body: role,
      timeoutMs: this.timeoutMs,
    });
  }

  async assignRealmRole(userId: string, role: RealmRoleRef): Promise<void> {
    const token = await this.token();
    await keycloakJson(this.userRealmMappingsUrl(userId), {
      method: "POST",
      token,
      body: [role],
      timeoutMs: this.timeoutMs,
    });
  }

  async removeRealmRole(userId: string, role: RealmRoleRef): Promise<void> {
    const token = await this.token();
    await keycloakJson(this.userRealmMappingsUrl(userId), {
      method: "DELETE",
      token,
      body: [role],
      timeoutMs: this.timeoutMs,
    });
  }

  private async token(): Promise<string> {
    const now = Date.now();
    if (this.cachedToken && this.cachedToken.expiresAt > now + 5000) {
      return this.cachedToken.accessToken;
    }
    const response = await requestToken(
      new URL(
        `/realms/${this.realm}/protocol/openid-connect/token`,
        this.baseUrl,
      ),
      new URLSearchParams({
        grant_type: "client_credentials",
        client_id: this.adminClientId,
        client_secret: this.adminClientSecret,
      }),
      this.timeoutMs,
    );
    this.cachedToken = {
      accessToken: response.access_token,
      expiresAt: now + (response.expires_in ?? 0) * 1000,
    };
    return response.access_token;
  }

  private adminUrl(path: string): URL {
    return new URL(`/admin/realms/${this.realm}${path}`, this.baseUrl);
  }

  private userRealmMappingsUrl(userId: string): URL {
    return this.adminUrl(
      `/users/${encodeURIComponent(userId)}/role-mappings/realm`,
    );
  }
}
