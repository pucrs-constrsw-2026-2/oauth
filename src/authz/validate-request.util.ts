import { AUTHZ_RESOURCE_NAMES, AuthzResourceName, isAuthzResourceName } from './authz-resource';
import { OaErrorMapper } from '../errors';

/**
 * Validates the `POST /authz/validate` body: `{ "resource": "<name>" }` where
 * `<name>` is one of the eight known resources. Anything else is a local
 * `OA-400`, per AC #5 of Story 6.5.
 */
export function requireAuthzResourceName(body: unknown): AuthzResourceName {
  if (!isRecord(body)) {
    throw OaErrorMapper.badRequest(
      'Request body must be a JSON object with a "resource" field.',
    );
  }

  const resource = body.resource;
  if (!isAuthzResourceName(resource)) {
    throw OaErrorMapper.badRequest(
      `"resource" must be one of: ${AUTHZ_RESOURCE_NAMES.join(', ')}.`,
      { resource },
    );
  }

  return resource;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
