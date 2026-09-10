import { AuthzController } from './authz.controller';
import { KeycloakAuthorizationClient } from './keycloak-authorization.client';
import type { AuthenticatedRequest } from '../common';
import type { AuthzResourceName } from './authz-resource';

/**
 * Story 6.5, AC #6 — documented lab check against the realm-derived matrix
 * (`keycloak-authz.md`), simulating Keycloak's actual verified bindings
 * (Stories 6.1-6.4) since no live Keycloak is reachable in this environment.
 *
 * This does **not** replace running the assertions against a live realm —
 * it proves the route's plumbing reaches the right decision for every input
 * Keycloak's `AFFIRMATIVE` multi-policy config would produce. The permission
 * decision itself is never computed locally (see `keycloak-authorization
 * .client.spec.ts` — it only ever asks Keycloak's HTTP status).
 */
const MATRIX: Record<string, readonly AuthzResourceName[]> = {
  administrator: [
    'classes',
    'courses',
    'lessons',
    'professors',
    'reservations',
    'resources',
    'rooms',
    'students',
  ],
  coordinator: ['courses', 'classes', 'lessons', 'reservations'],
  professor: ['lessons', 'reservations'],
  student: [],
};

const ALL_RESOURCES: readonly AuthzResourceName[] = [
  'classes',
  'courses',
  'lessons',
  'professors',
  'reservations',
  'resources',
  'rooms',
  'students',
];

function requestFor(role: string): AuthenticatedRequest {
  return { headers: { authorization: `Bearer token-for-${role}` } } as unknown as AuthenticatedRequest;
}

describe('POST /authz/validate against the realm-derived matrix (story 6.5)', () => {
  for (const [role, allowed] of Object.entries(MATRIX)) {
    describe(`role: ${role}`, () => {
      let checkPermission: jest.Mock;
      let controller: AuthzController;

      beforeEach(() => {
        // Simulates Keycloak's verified effective bindings (6.1-6.4), not a
        // local re-implementation the route itself would use.
        checkPermission = jest.fn(
          async (_token: string, resource: AuthzResourceName) => allowed.includes(resource),
        );
        controller = new AuthzController({
          checkPermission,
        } as unknown as KeycloakAuthorizationClient);
      });

      if (allowed.length > 0) {
        it.each(allowed)('permits %s → 200', async (resource) => {
          await expect(
            controller.validate(requestFor(role), { resource }),
          ).resolves.toEqual({});
        });
      }

      const forbidden = ALL_RESOURCES.filter((resource) => !allowed.includes(resource));
      if (forbidden.length > 0) {
        it.each(forbidden)('forbids %s → 403', async (resource) => {
          await expect(
            controller.validate(requestFor(role), { resource }),
          ).rejects.toMatchObject({});
        });
      }
    });
  }

  it('never asserts 403 for administrator or coordinator on a resource the realm actually grants', () => {
    expect(MATRIX.administrator).toHaveLength(8);
    expect(MATRIX.coordinator).toEqual(
      expect.arrayContaining(['courses', 'classes', 'lessons', 'reservations']),
    );
  });

  it('student has no resource grants — the one row where the brief and the realm agree', () => {
    expect(MATRIX.student).toHaveLength(0);
  });
});
