/**
 * The eight Keycloak Authorization Services resources on client `oauth`
 * (Story 6.2). This is the closed set `POST /authz/validate` accepts — it is
 * not an implementation of `classes`/`courses`/etc. as domain services.
 */
export const AUTHZ_RESOURCE_NAMES = [
  'classes',
  'courses',
  'lessons',
  'professors',
  'reservations',
  'resources',
  'rooms',
  'students',
] as const;

export type AuthzResourceName = (typeof AUTHZ_RESOURCE_NAMES)[number];

export function isAuthzResourceName(value: unknown): value is AuthzResourceName {
  return (
    typeof value === 'string' &&
    (AUTHZ_RESOURCE_NAMES as readonly string[]).includes(value)
  );
}
