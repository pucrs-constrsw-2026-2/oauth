import { Injectable } from '@nestjs/common';

import { KeycloakAdminClient } from '../common/keycloak-admin.client';
import { KeycloakSettingsService } from '../config';
import { roleConflict, roleNotFound } from './roles.exceptions';
import { KeycloakRole } from './role.types';

/**
 * Client-role operations. Token handling, timeouts and upstream failure
 * mapping live in the shared `KeycloakAdminClient`, which the users module
 * uses as well.
 */
@Injectable()
export class KeycloakAdminService {
  constructor(
    private readonly admin: KeycloakAdminClient,
    private readonly settings: KeycloakSettingsService,
  ) {}

  async createRole(role: Record<string, unknown>): Promise<KeycloakRole> {
    const clientId = await this.clientUuid();
    const response = await this.admin.request(`/clients/${clientId}/roles`, {
      method: 'POST',
      body: JSON.stringify(role),
    });

    if (response.status === 409) {
      throw roleConflict('A client role with this name already exists.');
    }
    await this.admin.expectSuccess(response, 'Could not create the client role.');

    const created = await this.findRoleByName(String(role.name));
    if (!created) {
      throw this.admin.upstream(502, 'Keycloak created the role but did not return it.');
    }
    return created;
  }

  async listRoles(): Promise<KeycloakRole[]> {
    const clientId = await this.clientUuid();
    const response = await this.admin.request(`/clients/${clientId}/roles`);
    await this.admin.expectSuccess(response, 'Could not list client roles.');
    const roles = await this.admin.json(response, 'Could not decode the client roles response.');
    if (!Array.isArray(roles)) {
      throw this.admin.upstream(502, 'Keycloak returned an invalid client roles response.');
    }
    return roles as KeycloakRole[];
  }

  async getRole(id: string): Promise<KeycloakRole> {
    const clientId = await this.clientUuid();
    const response = await this.admin.request(`/roles-by-id/${encodeURIComponent(id)}`);
    if (response.status === 404) {
      throw roleNotFound('Client role not found.');
    }
    await this.admin.expectSuccess(response, 'Could not retrieve the client role.');
    const role = await this.admin.json(response, 'Could not decode the client role response.');
    if (!isRole(role)) {
      throw this.admin.upstream(502, 'Keycloak returned an invalid client role response.');
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
    const response = await this.admin.request(
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
    await this.admin.expectSuccess(response, 'Could not update the client role.');
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
    const response = await this.admin.request(
      `/users/${encodeURIComponent(userId)}/role-mappings/clients/${clientId}`,
      { method: 'POST', body: JSON.stringify([role]) },
    );
    await this.admin.expectSuccess(response, 'Could not assign the client role.');
  }

  async unassignRole(userId: string, roleId: string): Promise<void> {
    const role = await this.resolveRole(roleId);
    await this.ensureUser(userId);
    const clientId = await this.clientUuid();
    const response = await this.admin.request(
      `/users/${encodeURIComponent(userId)}/role-mappings/clients/${clientId}`,
      { method: 'DELETE', body: JSON.stringify([role]) },
    );
    if (response.status !== 404) {
      await this.admin.expectSuccess(response, 'Could not unassign the client role.');
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
    const response = await this.admin.request(
      `/clients?clientId=${encodeURIComponent(this.settings.clientId)}`,
    );
    await this.admin.expectSuccess(response, 'Could not resolve the oauth client.');
    const clients = await this.admin.json(response, 'Could not decode the client list response.');
    if (!Array.isArray(clients)) {
      throw this.admin.upstream(502, 'Keycloak returned an invalid client list response.');
    }
    const client = clients.find((candidate) => candidate.clientId === this.settings.clientId);
    if (!client?.id) {
      throw roleNotFound(`Keycloak client "${this.settings.clientId}" was not found.`);
    }
    return client.id;
  }

  private async ensureUser(userId: string): Promise<void> {
    const response = await this.admin.request(`/users/${encodeURIComponent(userId)}`);
    if (response.status === 404) {
      throw roleNotFound('User not found.');
    }
    await this.admin.expectSuccess(response, 'Could not retrieve the user.');
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