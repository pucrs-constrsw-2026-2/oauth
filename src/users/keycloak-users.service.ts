import { Injectable } from '@nestjs/common';

import { KeycloakAdminClient } from '../common';
import { KeycloakUser } from './user.types';
import { userConflict, userNotFound } from './users.exceptions';

@Injectable()
export class KeycloakUsersService {
  constructor(private readonly admin: KeycloakAdminClient) {}

  /**
   * Creates the user and returns the id Keycloak reports in `Location`.
   * Keycloak answers 201 with an empty body, so the header is the only place
   * the generated id appears.
   */
  async createUser(
    user: KeycloakUser,
    password: string,
  ): Promise<KeycloakUser> {
    const response = await this.admin.request('/users', {
      method: 'POST',
      body: JSON.stringify({
        ...user,
        enabled: true,
        emailVerified: false,
        credentials: [{ type: 'password', value: password, temporary: false }],
      }),
    });

    if (response.status === 409) {
      throw userConflict();
    }
    await this.admin.expectSuccess(response, 'Could not create the user.');

    const id = idFromLocation(response.headers.get('location'));
    if (!id) {
      throw this.admin.upstream(
        502,
        'Keycloak created the user but did not report its id.',
      );
    }

    return this.getUser(id);
  }

  /** Lists users, optionally restricted to an enabled state. */
  async listUsers(enabled?: boolean): Promise<KeycloakUser[]> {
    const query =
      enabled === undefined ? '' : `?enabled=${enabled ? 'true' : 'false'}`;
    const response = await this.admin.request(`/users${query}`);
    await this.admin.expectSuccess(response, 'Could not list users.');

    const users = await this.admin.json(
      response,
      'Could not decode the users response.',
    );
    if (!Array.isArray(users)) {
      throw this.admin.upstream(502, 'Keycloak returned an invalid users response.');
    }

    // Keycloak ignores `enabled` on some versions and setups, so the filter is
    // reapplied here rather than trusted upstream.
    return enabled === undefined
      ? (users as KeycloakUser[])
      : (users as KeycloakUser[]).filter((user) => user.enabled === enabled);
  }

  async getUser(id: string): Promise<KeycloakUser> {
    const response = await this.admin.request(
      `/users/${encodeURIComponent(id)}`,
    );

    if (response.status === 404) {
      throw userNotFound();
    }
    await this.admin.expectSuccess(response, 'Could not retrieve the user.');

    const user = await this.admin.json<KeycloakUser>(
      response,
      'Could not decode the user response.',
    );
    if (!user || typeof user.id !== 'string') {
      throw this.admin.upstream(502, 'Keycloak returned an invalid user response.');
    }

    return user;
  }

  /**
   * Merges `changes` onto the stored user. Keycloak's user update replaces the
   * representation, so omitted fields would otherwise be cleared.
   */
  async updateUser(id: string, changes: Partial<KeycloakUser>): Promise<void> {
    const current = await this.getUser(id);
    const response = await this.admin.request(
      `/users/${encodeURIComponent(id)}`,
      {
        method: 'PUT',
        body: JSON.stringify({
          firstName: changes.firstName ?? current.firstName,
          lastName: changes.lastName ?? current.lastName,
          enabled: changes.enabled ?? current.enabled,
        }),
      },
    );

    if (response.status === 404) {
      throw userNotFound();
    }
    if (response.status === 409) {
      throw userConflict();
    }
    await this.admin.expectSuccess(response, 'Could not update the user.');
  }

  async changePassword(id: string, password: string): Promise<void> {
    await this.getUser(id);

    const response = await this.admin.request(
      `/users/${encodeURIComponent(id)}/reset-password`,
      {
        method: 'PUT',
        body: JSON.stringify({
          type: 'password',
          value: password,
          temporary: false,
        }),
      },
    );

    if (response.status === 404) {
      throw userNotFound();
    }
    await this.admin.expectSuccess(response, 'Could not change the password.');
  }

  /**
   * Logical delete: the user is disabled, never removed. Already-disabled
   * users are accepted so the route stays idempotent.
   */
  async disableUser(id: string): Promise<void> {
    const current = await this.getUser(id);
    if (current.enabled === false) {
      return;
    }
    await this.updateUser(id, { enabled: false });
  }
}

/** Keycloak returns `.../users/{id}` in the Location header on create. */
export function idFromLocation(location: string | null): string | undefined {
  if (!location) {
    return undefined;
  }
  const id = location.split('/').filter(Boolean).pop();
  return id && id.length > 0 ? id : undefined;
}
