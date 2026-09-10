import type { Request } from 'express';

import { OaErrorMapper } from '../errors';

const SUPPORTED_CONTENT_TYPES = [
  'multipart/form-data',
  'application/x-www-form-urlencoded',
];

/**
 * `/login` and `/refresh` accept only form-data or urlencoded bodies — the
 * Keycloak README's JSON curl example is out of contract for this API.
 */
export function assertSupportedFormContentType(request: Request): void {
  const contentType = (request.headers['content-type'] ?? '').toLowerCase();
  const isSupported = SUPPORTED_CONTENT_TYPES.some((type) =>
    contentType.startsWith(type),
  );

  if (!isSupported) {
    throw OaErrorMapper.badRequest(
      'Expected multipart/form-data or application/x-www-form-urlencoded.',
      { contentType: contentType || undefined },
    );
  }
}

/** Reads a required, non-blank string field. Extra fields are simply ignored. */
export function requireFormField(body: unknown, field: string): string {
  const value = isRecord(body) ? body[field] : undefined;
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw OaErrorMapper.badRequest(`Field "${field}" is required.`, { field });
  }
  return value;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
