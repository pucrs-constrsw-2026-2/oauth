import { Injectable } from '@nestjs/common';

import { KeycloakSettingsService } from '../config';
import {
  roleConflict,
  roleNotFound,
  roleUpstreamFailure,
} from './roles.exceptions';
import { KeycloakRole } from './role.types';

export const KEYCLOAK_REQUEST_TIMEOUT_MS = 5_000;

interface TokenResponse {
  access_token?: string;
}

@Injectable()
export class KeycloakAdminService {
  private readonly adminRealm = 'master';

  constructor(private readonly settings: KeycloakSettingsService) {}

  async createRole(role: Record<string, unknown>): Promise<KeycloakRole> {
    const clientId = await this.clientUuid();
    const response = await this.request(`/clients/${clientId}/roles`, {
      method: 'POST',
      body: JSON.stringify(role),
    });

    if (response.status === 409) {
      throw roleConflict('A client role with this name already exists.');
    }
    await this.expectSuccess(response, 'Could not create the client role.');

    const created = await this.findRoleByName(String(role.name));
    if (!created) {
      throw roleUpstreamFailure(502, 'Keycloak created the role but did not return it.');
    }
    return created;
  }

  async listRoles(): Promise<KeycloakRole[]> {
    const clientId = await this.clientUuid();
    const response = await this.request(`/clients/${clientId}/roles`);
    await this.expectSuccess(response, 'Could not list client roles.');
    const roles = await this.json(response, 'Could not decode the client roles response.');
    if (!Array.isArray(roles)) {
      throw roleUpstreamFailure(502, 'Keycloak returned an invalid client roles response.');
    }
    return roles as KeycloakRole[];
  }

  async getRole(id: string): Promise<KeycloakRole> {
    const clientId = await this.clientUuid();
    const response = await this.request(`/roles-by-id/${encodeURIComponent(id)}`);
    if (response.status === 404) {
      throw roleNotFound('Client role not found.');
    }
    await this.expectSuccess(response, 'Could not retrieve the client role.');
    const role = await this.json(response, 'Could not decode the client role response.');
    if (!isRole(role)) {
      throw roleUpstreamFailure(502, 'Keycloak returned an invalid client role response.');
    }
    if (!role.clientRole || role.containerId !== clientId) {
      throw roleNotFound('Client role not found.');
    }
    return role as KeycloakRole;
  }

  async updateRole(id: string, changes: Record<string, unknown>): Promise<void> {
    const current = await this.getRole(id);
    await this.putRole(current, {
      name: changes.name ?? current.name,
      description: changes.description ?? current.description,
      attributes: changes.attributes ?? current.attributes,
    });
  }

  async replaceRole(id: string, replacement: Record<string, unknown>): Promise<void> {
    const current = await this.getRole(id);
    await this.putRole(current, replacement);
  }

  private async putRole(
    current: KeycloakRole,
    changes: Record<string, unknown>,
  ): Promise<void> {
    const response = await this.request(
      `/clients/${await this.clientUuid()}/roles/${encodeURIComponent(current.name)}`,
      {
        method: 'PUT',
        body: JSON.stringify({
          id: current.id,
          name: changes.name,
          description: changes.description,
          attributes: changes.attributes,
        }),
      },
    );
    if (response.status === 404) {
      throw roleNotFound('Client role not found.');
    }
    if (response.status === 409) {
      throw roleConflict('A client role with this name already exists.');
    }
    await this.expectSuccess(response, 'Could not update the client role.');
  }

  async logicalDeleteRole(id: string): Promise<void> {
    const role = await this.getRole(id);
    const attributes = { ...(role.attributes ?? {}), inactive: ['true'] };
    await this.updateRole(id, { attributes });
  }

  async assignRole(userId: string, roleId: string): Promise<void> {
    const role = await this.resolveRole(roleId);
    await this.ensureUser(userId);
    const clientId = await this.clientUuid();
    const response = await this.request(
      `/users/${encodeURIComponent(userId)}/role-mappings/clients/${clientId}`,
      { method: 'POST', body: JSON.stringify([role]) },
    );
    await this.expectSuccess(response, 'Could not assign the client role.');
  }

  async unassignRole(userId: string, roleId: string): Promise<void> {
    const role = await this.resolveRole(roleId);
    await this.ensureUser(userId);
    const clientId = await this.clientUuid();
    const response = await this.request(
      `/users/${encodeURIComponent(userId)}/role-mappings/clients/${clientId}`,
      { method: 'DELETE', body: JSON.stringify([role]) },
    );
    if (response.status !== 404) {
      await this.expectSuccess(response, 'Could not unassign the client role.');
    }
  }

  async findRoleByName(name: string): Promise<KeycloakRole | null> {
    const roles = await this.listRoles();
    return roles.find((role) => role.name === name) ?? null;
  }

  private async resolveRole(identifier: string): Promise<KeycloakRole> {
    if (isUuid(identifier)) {
      return this.getRole(identifier);
    }
    const role = await this.findRoleByName(identifier);
    if (!role) {
      throw roleNotFound('Client role not found.');
    }
    return role;
  }

  private async clientUuid(): Promise<string> {
    const response = await this.request(
      `/clients?clientId=${encodeURIComponent(this.settings.clientId)}`,
    );
    await this.expectSuccess(response, 'Could not resolve the oauth client.');
    const clients = await this.json(response, 'Could not decode the client list response.');
    if (!Array.isArray(clients)) {
      throw roleUpstreamFailure(502, 'Keycloak returned an invalid client list response.');
    }
    const client = clients.find((candidate) => candidate.clientId === this.settings.clientId);
    if (!client?.id) {
      throw roleNotFound(`Keycloak client "${this.settings.clientId}" was not found.`);
    }
    return client.id;
  }

  private async ensureUser(userId: string): Promise<void> {
    const response = await this.request(`/users/${encodeURIComponent(userId)}`);
    if (response.status === 404) {
      throw roleNotFound('User not found.');
    }
    await this.expectSuccess(response, 'Could not retrieve the user.');
  }

  private async request(path: string, init: RequestInit = {}): Promise<Response> {
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
      throw roleUpstreamFailure(503, 'Keycloak Admin API is unavailable.');
    }
  }

  private async adminToken(): Promise<string> {
    if (!this.settings.adminUser || !this.settings.adminPassword) {
      throw roleUpstreamFailure(503, 'Keycloak admin credentials are not configured.');
    }
    const body = new URLSearchParams({
      client_id: 'admin-cli',
      grant_type: 'password',
      username: this.settings.adminUser,
      password: this.settings.adminPassword,
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
      throw roleUpstreamFailure(503, 'Keycloak authentication is unavailable.');
    }
    if (!response.ok) {
      throw roleUpstreamFailure(502, 'Keycloak admin authentication failed.');
    }
    const token = await this.json(response, 'Could not decode the admin token response.');
    if (!token.access_token) {
      throw roleUpstreamFailure(502, 'Keycloak did not return an admin access token.');
    }
    return token.access_token;
  }

  private async expectSuccess(response: Response, description: string): Promise<void> {
    if (!response.ok) {
      throw roleUpstreamFailure(
        response.status === 403 ? 403 : 502,
        description,
      );
    }
  }

  private async json(response: Response, description: string): Promise<any> {
    try {
      return await response.json();
    } catch {
      throw roleUpstreamFailure(502, description);
    }
  }
}

function isRole(value: unknown): value is KeycloakRole {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Record<string, unknown>).id === 'string' &&
    typeof (value as Record<string, unknown>).name === 'string'
  );
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}