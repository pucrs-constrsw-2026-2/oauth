/**
 * Uniform error body every oauth API response must use (Story 3.1, CAP-4).
 * Epics 2-6 must reuse this shape instead of inventing their own.
 */
export interface OaError {
  readonly error_code: string;
  readonly error_description: string;
  readonly error_source: string;
  /** Chain of causes down to the root cause, oldest cause last. */
  readonly error_stack: Record<string, unknown>[];
}

/** `error_source` for every error this service produces. */
export const OA_ERROR_SOURCE = 'OAuthAPI';

/**
 * Brief-specific code family for failures that are local to this API (bad
 * structure, missing auth, etc.) and not relayed from a Keycloak response.
 * Documented in the README so Epics 4-6 do not invent `ERR_USER_*` siblings.
 */
export const OA_ERROR_CODE = {
  BAD_REQUEST: 'OA-400',
  UNAUTHORIZED: 'OA-401',
  FORBIDDEN: 'OA-403',
  NOT_FOUND: 'OA-404',
  CONFLICT: 'OA-409',
  INTERNAL: 'OA-500',
} as const;
